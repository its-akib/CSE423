package org.keycloak.forms.account.freemarker.model;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.util.ResolveRelative;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Freemarker view-model backing the account console's User-Managed Access
 * "My Resources" page.
 */
public class AuthorizationBean {

    private final UserModel user;
    private final AuthorizationProvider authorization;
    private final UriInfo uriInfo;

    private ResourceBean resource;
    private List<ResourceBean> resources;
    private Collection<ResourceBean> userSharedResources;
    private Collection<ResourceBean> requestsWaitingPermission;
    private Collection<ResourceBean> resourcesWaitingOthersApproval;

    public AuthorizationBean(KeycloakSession session, UserModel user, UriInfo uriInfo) {
        this.user = user;
        this.uriInfo = uriInfo;
        this.authorization = session.getProvider(AuthorizationProvider.class);

        String resourceId = uriInfo.getPathParameters().getFirst("resource_id");
        if (resourceId != null) {
            Resource resource = authorization.getStoreFactory().getResourceStore().findById(resourceId, null);
            if (resource == null || !resource.getOwner().equals(user.getId())) {
                throw new RuntimeException("Resource with id [" + resourceId + "] does not belong to user [" + user.getId() + "]");
            }
        }
    }

    public Collection<ResourceBean> getResourcesWaitingOthersApproval() {
        if (resourcesWaitingOthersApproval == null) {
            Map<String, String> filters = new HashMap<>();
            filters.put(PermissionTicket.OWNER, user.getId());
            filters.put(PermissionTicket.REQUESTER, "");
            filters.put(PermissionTicket.GRANTED, "false");
            resourcesWaitingOthersApproval = toResourceRepresentation(findPermissions(filters));
        }
        return resourcesWaitingOthersApproval;
    }

    public Collection<ResourceBean> getResourcesWaitingApproval() {
        if (requestsWaitingPermission == null) {
            Map<String, String> filters = new HashMap<>();
            filters.put(PermissionTicket.REQUESTER, user.getId());
            filters.put(PermissionTicket.GRANTED, "false");
            requestsWaitingPermission = toResourceRepresentation(findPermissions(filters));
        }
        return requestsWaitingPermission;
    }

    public List<ResourceBean> getResources() {
        if (resources == null) {
            resources = authorization.getStoreFactory().getResourceStore().findByOwner(user.getId(), null).stream()
                    .filter(resource -> resource.getOwner().equals(user.getId()))
                    .map(ResourceBean::new)
                    .collect(Collectors.toList());
        }
        return resources;
    }

    public Collection<ResourceBean> getSharedResources() {
        if (userSharedResources == null) {
            Map<String, String> filters = new HashMap<>();
            filters.put(PermissionTicket.REQUESTER, user.getId());
            filters.put(PermissionTicket.GRANTED, "true");
            userSharedResources = toResourceRepresentation(
                    authorization.getStoreFactory().getPermissionTicketStore().find(filters, null, -1, -1));
        }
        return userSharedResources;
    }

    public ResourceBean getResource() {
        if (resource == null) {
            String resourceId = uriInfo.getPathParameters().getFirst("resource_id");
            resource = getResource(resourceId);
        }
        return resource;
    }

    private ResourceBean getResource(String id) {
        Resource resource = authorization.getStoreFactory().getResourceStore().findById(id, null);
        return new ResourceBean(resource);
    }

    private Collection<ResourceBean> toResourceRepresentation(List<PermissionTicket> tickets) {
        Map<String, ResourceBean> resources = new HashMap<>();

        for (PermissionTicket ticket : tickets) {
            Resource resource = ticket.getResource();

            if (!resource.isOwnerManagedAccess()) {
                continue;
            }

            ResourceBean resourceBean = resources.computeIfAbsent(resource.getId(), id -> new ResourceBean(resource));
            resourceBean.addPermission(ticket);
        }

        return resources.values();
    }

    private Collection<RequesterBean> toPermissionRepresentation(List<PermissionTicket> permissionRequests) {
        Map<String, RequesterBean> requesters = new HashMap<>();

        for (PermissionTicket ticket : permissionRequests) {
            RequesterBean requesterBean = requesters.computeIfAbsent(ticket.getRequester(),
                    id -> new RequesterBean(ticket, authorization));
            requesterBean.addScope(new PermissionScopeBean(ticket));
        }

        return requesters.values();
    }

    private List<PermissionTicket> findPermissions(Map<String, String> filters) {
        PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();
        return ticketStore.find(filters, null, -1, -1);
    }

    public static class RequesterBean {

        private final UserModel requester;
        private final List<PermissionScopeBean> scopes = new ArrayList<>();
        private final Date createdDate;
        private Date grantedDate;
        private final boolean granted;

        public RequesterBean(PermissionTicket ticket, AuthorizationProvider authorization) {
            this.requester = authorization.getKeycloakSession().users()
                    .getUserById(ticket.getRequester(), authorization.getRealm());
            this.createdDate = Time.toDate(ticket.getCreatedTimestamp());
            this.granted = ticket.isGranted();
            if (granted) {
                this.grantedDate = Time.toDate(ticket.getGrantedTimestamp());
            }
        }

        public void addScope(PermissionScopeBean scope) {
            scopes.add(scope);
        }

        public UserModel getRequester() {
            return requester;
        }

        public List<PermissionScopeBean> getScopes() {
            return scopes;
        }

        public Date getCreatedDate() {
            return createdDate;
        }

        public Date getGrantedDate() {
            return grantedDate;
        }

        public boolean isGranted() {
            return granted;
        }
    }

    public static class PermissionScopeBean {

        private final PermissionTicket ticket;

        public PermissionScopeBean(PermissionTicket ticket) {
            this.ticket = ticket;
        }

        public Scope getScope() {
            return ticket.getScope();
        }

        public boolean isGranted() {
            return ticket.isGranted();
        }
    }

    public class ResourceBean {

        private final Resource resource;
        private ResourceServerBean resourceServer;
        private final Map<String, RequesterBean> permissions = new HashMap<>();

        public ResourceBean(Resource resource) {
            this.resource = resource;
        }

        public void addPermission(PermissionTicket ticket) {
            RequesterBean requesterBean = permissions.computeIfAbsent(ticket.getRequester(),
                    id -> new RequesterBean(ticket, authorization));
            requesterBean.addScope(new PermissionScopeBean(ticket));
        }

        public String getId() {
            return resource.getId();
        }

        public String getName() {
            return resource.getName();
        }

        public String getIconUri() {
            return resource.getIconUri();
        }

        public List<ScopeRepresentation> getScopes() {
            return resource.getScopes().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        }

        public Collection<RequesterBean> getShares() {
            Map<String, String> filters = new HashMap<>();
            filters.put(PermissionTicket.RESOURCE, resource.getId());
            filters.put(PermissionTicket.GRANTED, "true");
            return toPermissionRepresentation(findPermissions(filters));
        }

        public UserModel getOwner() {
            return authorization.getKeycloakSession().users().getUserById(resource.getOwner(), authorization.getRealm());
        }

        public ResourceServerBean getResourceServer() {
            if (resourceServer == null) {
                ClientModel client = authorization.getRealm().getClientById(resource.getResourceServer().getId());
                resourceServer = new ResourceServerBean(client);
            }
            return resourceServer;
        }

        public List<ManagedPermissionBean> getPolicies() {
            List<Policy> policies = authorization.getStoreFactory().getPolicyStore()
                    .findByResourceServer(resource.getResourceServer().getId());

            if (policies == null) {
                return Collections.emptyList();
            }

            return policies.stream()
                    .filter(policy -> policy.getResources().contains(resource))
                    .map(ManagedPermissionBean::new)
                    .collect(Collectors.toList());
        }
    }

    public class ResourceServerBean {

        private final ClientModel clientModel;

        public ResourceServerBean(ClientModel clientModel) {
            this.clientModel = clientModel;
        }

        public String getName() {
            String name = clientModel.getName();
            return name != null ? name : clientModel.getClientId();
        }

        public String getClientId() {
            return clientModel.getClientId();
        }

        public String getRedirectUri() {
            Set<String> redirectUris = clientModel.getRedirectUris();
            return redirectUris == null || redirectUris.isEmpty() ? null : redirectUris.iterator().next();
        }

        public String getBaseUri() {
            return ResolveRelative.resolveRelativeUri(authorization.getKeycloakSession(),
                    clientModel.getRootUrl(), clientModel.getBaseUrl());
        }
    }

    public class ManagedPermissionBean {

        private final Policy policy;
        private List<ManagedPermissionBean> associatedPolicies;

        public ManagedPermissionBean(Policy policy) {
            this.policy = policy;
        }

        public String getId() {
            return policy.getId();
        }

        public String getName() {
            return policy.getName();
        }

        public String getDescription() {
            return policy.getDescription();
        }

        public Set<Scope> getScopes() {
            return policy.getScopes();
        }

        public List<ManagedPermissionBean> getPolicies() {
            if (associatedPolicies == null) {
                associatedPolicies = policy.getAssociatedPolicies().stream()
                        .map(ManagedPermissionBean::new)
                        .collect(Collectors.toList());
            }
            return associatedPolicies;
        }
    }
}
