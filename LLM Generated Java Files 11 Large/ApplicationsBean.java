package org.keycloak.forms.account.freemarker.model;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrderedModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.storage.StorageId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Freemarker view-model for the account console's "Applications" page.
 * Computes, for the current user, the list of client applications they have
 * a relationship with (available roles, granted consent, offline-token grants).
 */
public class ApplicationsBean {

    private final List<ApplicationEntry> applications = new LinkedList<>();

    public ApplicationsBean(KeycloakSession session, RealmModel realm, UserModel user) {
        Set<ClientModel> offlineClients = new UserSessionManager(session).findClientsWithOfflineToken(realm, user);

        for (ClientModel client : getApplications(session, realm, user)) {
            if (isAdminClient(client) &&
                    !AdminPermissions.realms(session, realm, user).isAdmin()) {
                continue;
            }

            // Compute the union of default + optional client scopes (plus the client itself)
            // to feed TokenManager.getAccess for a conservative "all potentially available roles" set.
            Set<ClientScopeModel> allClientScopes = new HashSet<>();
            allClientScopes.addAll(client.getClientScopes(true, true).values());
            allClientScopes.addAll(client.getClientScopes(false, true).values());
            allClientScopes.add(client);

            Set<RoleModel> availableRoles = TokenManager.getAccess(null, false, allClientScopes, client);

            if (availableRoles.isEmpty() && !client.isConsentRequired()) {
                continue;
            }

            List<RoleModel> realmRolesAvailable = new LinkedList<>();
            MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable = new MultivaluedHashMap<>();
            processRoles(availableRoles, realmRolesAvailable, resourceRolesAvailable);

            List<String> clientScopesGranted = new LinkedList<>();
            List<String> additionalGrants = new ArrayList<>();

            if (client.isConsentRequired()) {
                UserConsentModel consent = session.users().getConsentByClient(realm, user.getId(), client.getId());

                if (consent != null) {
                    List<ClientScopeModel> orderedScopes = new LinkedList<>(consent.getGrantedClientScopes());
                    clientScopesGranted = orderedScopes.stream()
                            .sorted(new OrderedModel.OrderedModelComparator<>())
                            .map(ClientScopeModel::getConsentScreenText)
                            .collect(Collectors.toList());
                }
            }

            if (offlineClients.contains(client)) {
                additionalGrants.add("${offlineToken}");
            }

            ApplicationEntry entry = new ApplicationEntry(session, realmRolesAvailable, resourceRolesAvailable,
                    client, clientScopesGranted, additionalGrants);
            applications.add(entry);
        }
    }

    public static boolean isAdminClient(ClientModel client) {
        return client.getClientId().equals(Constants.ADMIN_CLI_CLIENT_ID) ||
                client.getClientId().equals(Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    private Set<ClientModel> getApplications(KeycloakSession session, RealmModel realm, UserModel user) {
        Set<ClientModel> clients = new HashSet<>();
        for (ClientModel client : realm.getClients()) {
            if (!client.isBearerOnly()) {
                clients.add(client);
            }
        }
        for (UserConsentModel consent : session.users().getConsents(realm, user.getId())) {
            ClientModel client = consent.getClient();
            if (new StorageId(client.getId()).isLocal() || !client.isBearerOnly()) {
                clients.add(client);
            }
        }
        return clients;
    }

    private void processRoles(Set<RoleModel> inputRoles, List<RoleModel> realmRoles,
                               MultivaluedHashMap<String, ClientRoleEntry> clientRoles) {
        for (RoleModel role : inputRoles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoles.add(role);
            } else {
                ClientModel client = (ClientModel) role.getContainer();
                ClientRoleEntry entry = new ClientRoleEntry(client.getClientId(), client.getName(),
                        role.getName(), role.getDescription());
                clientRoles.add(client.getClientId(), entry);
            }
        }
    }

    public List<ApplicationEntry> getApplications() {
        return applications;
    }

    public static class ApplicationEntry {

        private final KeycloakSession session;
        private final List<RoleModel> realmRolesAvailable;
        private final MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable;
        private final ClientModel client;
        private final List<String> clientScopesGranted;
        private final List<String> additionalGrants;

        public ApplicationEntry(KeycloakSession session, List<RoleModel> realmRolesAvailable,
                                 MultivaluedHashMap<String, ClientRoleEntry> resourceRolesAvailable,
                                 ClientModel client, List<String> clientScopesGranted,
                                 List<String> additionalGrants) {
            this.session = session;
            this.realmRolesAvailable = realmRolesAvailable;
            this.resourceRolesAvailable = resourceRolesAvailable;
            this.client = client;
            this.clientScopesGranted = clientScopesGranted;
            this.additionalGrants = additionalGrants;
        }

        public List<RoleModel> getRealmRolesAvailable() {
            return realmRolesAvailable;
        }

        public MultivaluedHashMap<String, ClientRoleEntry> getResourceRolesAvailable() {
            return resourceRolesAvailable;
        }

        public List<String> getClientScopesGranted() {
            return clientScopesGranted;
        }

        public String getEffectiveUrl() {
            return ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), client.getBaseUrl());
        }

        public ClientModel getClient() {
            return client;
        }

        public List<String> getAdditionalGrants() {
            return additionalGrants;
        }
    }

    public static class ClientRoleEntry {

        private final String clientId;
        private final String clientName;
        private final String roleName;
        private final String roleDescription;

        public ClientRoleEntry(String clientId, String clientName, String roleName, String roleDescription) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.roleName = roleName;
            this.roleDescription = roleDescription;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getRoleName() {
            return roleName;
        }

        public String getRoleDescription() {
            return roleDescription;
        }
    }
}
