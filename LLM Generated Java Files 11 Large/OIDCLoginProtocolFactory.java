package org.keycloak.protocol.oidc;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationSessionNote;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.DefaultClientScopes;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.AllowedWebOriginsProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AudienceResolveProtocolMapper;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.protocol.oidc.mappers.UserRealmRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ServicesLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

/**
 * SPI factory that registers the OpenID Connect login protocol, its catalog of
 * built-in protocol mappers, and provisions the default OIDC client scopes for
 * every new realm.
 */
public class OIDCLoginProtocolFactory extends AbstractLoginProtocolFactory {

    private static final Logger logger = Logger.getLogger(OIDCLoginProtocolFactory.class);

    // Claim / mapper id constants
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email verified";
    public static final String GIVEN_NAME = "given name";
    public static final String FAMILY_NAME = "family name";
    public static final String MIDDLE_NAME = "middle name";
    public static final String NICKNAME = "nickname";
    public static final String PROFILE_CLAIM = "profile";
    public static final String PICTURE = "picture";
    public static final String WEBSITE = "website";
    public static final String GENDER = "gender";
    public static final String BIRTHDATE = "birthdate";
    public static final String ZONEINFO = "zoneinfo";
    public static final String UPDATED_AT = "updated at";
    public static final String FULL_NAME = "full name";
    public static final String LOCALE = "locale";
    public static final String ADDRESS = "address";
    public static final String PHONE_NUMBER = "phone number";
    public static final String PHONE_NUMBER_VERIFIED = "phone number verified";
    public static final String REALM_ROLES = "realm roles";
    public static final String CLIENT_ROLES = "client roles";
    public static final String AUDIENCE_RESOLVE = "audience resolve";
    public static final String ALLOWED_WEB_ORIGINS = "allowed web origins";
    public static final String UPN = "upn";
    public static final String GROUPS = "groups";

    public static final String ROLES_SCOPE = "roles";
    public static final String WEB_ORIGINS_SCOPE = "web-origins";
    public static final String MICROPROFILE_JWT_SCOPE = "microprofile-jwt";

    public static final String PROFILE_SCOPE_CONSENT_TEXT = "${profileScopeConsentText}";
    public static final String EMAIL_SCOPE_CONSENT_TEXT = "${emailScopeConsentText}";
    public static final String ADDRESS_SCOPE_CONSENT_TEXT = "${addressScopeConsentText}";
    public static final String PHONE_SCOPE_CONSENT_TEXT = "${phoneScopeConsentText}";
    public static final String OFFLINE_ACCESS_SCOPE_CONSENT_TEXT = "${offlineAccessScopeConsentText}";
    public static final String ROLES_SCOPE_CONSENT_TEXT = "${rolesScopeConsentText}";

    static Map<String, ProtocolMapperModel> builtins = new HashMap<>();

    static {
        builtins.put(USERNAME, UserPropertyMapper.createClaimMapper(USERNAME, "username",
                "preferred_username", "String", true, true, true));
        builtins.put(EMAIL, UserPropertyMapper.createClaimMapper(EMAIL, "email",
                "email", "String", true, true, true));
        builtins.put(GIVEN_NAME, UserPropertyMapper.createClaimMapper(GIVEN_NAME, "firstName",
                "given_name", "String", true, true, true));
        builtins.put(FAMILY_NAME, UserPropertyMapper.createClaimMapper(FAMILY_NAME, "lastName",
                "family_name", "String", true, true, true));
        builtins.put(FULL_NAME, FullNameMapper.createFullNameMapper(FULL_NAME, true, true, false));

        createUserAttributeMapper(MIDDLE_NAME, "middleName", "middle_name", "String");
        createUserAttributeMapper(NICKNAME, "nickname", "nickname", "String");
        createUserAttributeMapper(PROFILE_CLAIM, "profile", "profile", "String");
        createUserAttributeMapper(PICTURE, "picture", "picture", "String");
        createUserAttributeMapper(WEBSITE, "website", "website", "String");
        createUserAttributeMapper(GENDER, "gender", "gender", "String");
        createUserAttributeMapper(BIRTHDATE, "birthdate", "birthdate", "String");
        createUserAttributeMapper(ZONEINFO, "zoneinfo", "zoneinfo", "String");
        createUserAttributeMapper(LOCALE, "locale", "locale", "String");
        createUserAttributeMapper(UPDATED_AT, "updatedAt", "updated_at", "String");
        createUserAttributeMapper(PHONE_NUMBER, "phoneNumber", "phone_number", "String");
        createUserAttributeMapper(PHONE_NUMBER_VERIFIED, "phoneNumberVerified", "phone_number_verified", "boolean");
        createUserAttributeMapper(UPN, "username", "upn", "String");

        builtins.put(ADDRESS, AddressMapper.createAddressMapper());

        builtins.put(REALM_ROLES, UserRealmRoleMappingMapper.createRoleNameMapper(REALM_ROLES,
                IDToken.REALM_ACCESS_CLAIM + "." + "roles", true, true));
        builtins.put(CLIENT_ROLES, UserClientRoleMappingMapper.createClaimMapper(CLIENT_ROLES,
                IDToken.RESOURCE_ACCESS_CLAIM, null, null, true, true));
        builtins.put(GROUPS, UserRealmRoleMappingMapper.createRoleNameMapper(GROUPS, "groups", true, true));

        builtins.put(AUDIENCE_RESOLVE, AudienceResolveProtocolMapper.createClaimMapper(AUDIENCE_RESOLVE));
        builtins.put(ALLOWED_WEB_ORIGINS, AllowedWebOriginsProtocolMapper.createClaimMapper(ALLOWED_WEB_ORIGINS));

        builtins.put("gss delegation credential",
                UserSessionNoteMapper.createUserSessionNoteMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                        KerberosConstants.GSS_DELEGATION_CREDENTIAL, KerberosConstants.GSS_DELEGATION_CREDENTIAL));
        builtins.put("impersonator id",
                UserSessionNoteMapper.createUserSessionNoteMapper("impersonator id", IMPERSONATOR_ID, IMPERSONATOR_ID));
        builtins.put("impersonator username",
                UserSessionNoteMapper.createUserSessionNoteMapper("impersonator username", IMPERSONATOR_USERNAME, IMPERSONATOR_USERNAME));
    }

    private static void createUserAttributeMapper(String name, String userAttribute, String claimName, String type) {
        ProtocolMapperModel model = UserAttributeMapper.createClaimMapper(name, userAttribute, claimName, type,
                true, true, false, true);
        builtins.put(name, model);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OIDCLoginProtocol().setSession(session);
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    protected void createDefaultClientScopesImpl(RealmModel newRealm) {
        // profile scope
        ClientScopeModel profileScope = newRealm.addClientScope(OAuth2Constants.SCOPE_PROFILE);
        profileScope.setDescription("OpenID Connect built-in scope: profile");
        profileScope.setDisplayOnConsentScreen(true);
        profileScope.setConsentScreenText(PROFILE_SCOPE_CONSENT_TEXT);
        profileScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        for (String name : new String[]{USERNAME, FULL_NAME, FAMILY_NAME, GIVEN_NAME, MIDDLE_NAME, NICKNAME,
                PROFILE_CLAIM, PICTURE, WEBSITE, GENDER, BIRTHDATE, ZONEINFO, LOCALE, UPDATED_AT}) {
            profileScope.addProtocolMapper(builtins.get(name));
        }
        newRealm.addDefaultClientScope(profileScope, true);

        // email scope
        ClientScopeModel emailScope = newRealm.addClientScope(OAuth2Constants.SCOPE_EMAIL);
        emailScope.setDescription("OpenID Connect built-in scope: email");
        emailScope.setDisplayOnConsentScreen(true);
        emailScope.setConsentScreenText(EMAIL_SCOPE_CONSENT_TEXT);
        emailScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        emailScope.addProtocolMapper(builtins.get(EMAIL));
        newRealm.addDefaultClientScope(emailScope, true);

        // address scope
        ClientScopeModel addressScope = newRealm.addClientScope(ADDRESS);
        addressScope.setDescription("OpenID Connect built-in scope: address");
        addressScope.setDisplayOnConsentScreen(true);
        addressScope.setConsentScreenText(ADDRESS_SCOPE_CONSENT_TEXT);
        addressScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        addressScope.addProtocolMapper(builtins.get(ADDRESS));
        newRealm.addDefaultClientScope(addressScope, false);

        // phone scope
        ClientScopeModel phoneScope = newRealm.addClientScope("phone");
        phoneScope.setDescription("OpenID Connect built-in scope: phone");
        phoneScope.setDisplayOnConsentScreen(true);
        phoneScope.setConsentScreenText(PHONE_SCOPE_CONSENT_TEXT);
        phoneScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        phoneScope.addProtocolMapper(builtins.get(PHONE_NUMBER));
        phoneScope.addProtocolMapper(builtins.get(PHONE_NUMBER_VERIFIED));
        newRealm.addDefaultClientScope(phoneScope, false);

        // offline_access scope, wiring the pre-existing offline_access role if present
        if (KeycloakModelUtils.getClientScopeByName(newRealm, OAuth2Constants.OFFLINE_ACCESS) == null) {
            RoleModel offlineRole = newRealm.getRole(OAuth2Constants.OFFLINE_ACCESS);
            DefaultClientScopes.createOfflineAccessClientScope(newRealm, offlineRole);
        }

        addRolesClientScope(newRealm);
        addWebOriginsClientScope(newRealm);
        addMicroprofileJWTClientScope(newRealm);
    }

    public static ClientScopeModel addRolesClientScope(RealmModel newRealm) {
        ClientScopeModel rolesScope = KeycloakModelUtils.getClientScopeByName(newRealm, ROLES_SCOPE);
        if (rolesScope == null) {
            logger.debugf("Client scope '%s' not found. Creating", ROLES_SCOPE);

            rolesScope = newRealm.addClientScope(ROLES_SCOPE);
            rolesScope.setDescription("OpenID Connect scope for add roles in the tokens");
            rolesScope.setDisplayOnConsentScreen(true);
            rolesScope.setConsentScreenText(ROLES_SCOPE_CONSENT_TEXT);
            rolesScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            rolesScope.addProtocolMapper(builtins.get(REALM_ROLES));
            rolesScope.addProtocolMapper(builtins.get(CLIENT_ROLES));
            rolesScope.addProtocolMapper(builtins.get(AUDIENCE_RESOLVE));

            newRealm.addDefaultClientScope(rolesScope, true);
        }
        return rolesScope;
    }

    public static ClientScopeModel addWebOriginsClientScope(RealmModel newRealm) {
        ClientScopeModel webOriginsScope = KeycloakModelUtils.getClientScopeByName(newRealm, WEB_ORIGINS_SCOPE);
        if (webOriginsScope == null) {
            logger.debugf("Client scope '%s' not found. Creating", WEB_ORIGINS_SCOPE);

            webOriginsScope = newRealm.addClientScope(WEB_ORIGINS_SCOPE);
            webOriginsScope.setDescription("OpenID Connect scope for add allowed web origins to the access token");
            webOriginsScope.setDisplayOnConsentScreen(false);
            webOriginsScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            webOriginsScope.addProtocolMapper(builtins.get(ALLOWED_WEB_ORIGINS));

            newRealm.addDefaultClientScope(webOriginsScope, true);
        }
        return webOriginsScope;
    }

    public static ClientScopeModel addMicroprofileJWTClientScope(RealmModel newRealm) {
        ClientScopeModel mpJwtScope = KeycloakModelUtils.getClientScopeByName(newRealm, MICROPROFILE_JWT_SCOPE);
        if (mpJwtScope == null) {
            logger.debugf("Client scope '%s' not found. Creating", MICROPROFILE_JWT_SCOPE);

            mpJwtScope = newRealm.addClientScope(MICROPROFILE_JWT_SCOPE);
            mpJwtScope.setDescription("Microprofile - JWT built-in scope");
            mpJwtScope.setDisplayOnConsentScreen(false);
            mpJwtScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            mpJwtScope.addProtocolMapper(builtins.get(UPN));
            mpJwtScope.addProtocolMapper(builtins.get(GROUPS));

            newRealm.addDefaultClientScope(mpJwtScope, false);
        }
        return mpJwtScope;
    }

    @Override
    protected void addDefaults(ClientModel client) {
        // no-op: no protocol-specific defaults beyond setupClientDefaults
    }

    @Override
    public Object createProtocolEndpoint(RealmModel realm, EventBuilder event) {
        return new OIDCLoginProtocolService(realm, event);
    }

    @Override
    public String getId() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public void setupClientDefaults(ClientRepresentation rep, ClientModel newClient) {
        if (rep.getRootUrl() != null && (rep.getRedirectUris() == null || rep.getRedirectUris().isEmpty())) {
            String root = rep.getRootUrl();
            if (root.endsWith("/")) root = root.substring(0, root.length() - 1);
            newClient.addRedirectUri(root + "/*");

            Set<String> origins = new HashSet<>();
            String origin = UriUtils.getOrigin(root);
            origins.add(origin);
            newClient.setWebOrigins(origins);
        }

        if (rep.isPublicClient() == null) newClient.setPublicClient(true);
        if (rep.isBearerOnly() == null) newClient.setBearerOnly(false);
        if (rep.getAdminUrl() == null && rep.getRootUrl() != null) newClient.setManagementUrl(rep.getRootUrl());

        if (rep.isDirectGrantsOnly() != null) {
            ServicesLogger.LOGGER.usingDeprecatedDirectGrantsOnly();
            newClient.setStandardFlowEnabled(!rep.isDirectGrantsOnly());
            newClient.setDirectAccessGrantsEnabled(rep.isDirectGrantsOnly());
        } else {
            if (rep.isStandardFlowEnabled() == null) newClient.setStandardFlowEnabled(true);
            if (rep.isDirectAccessGrantsEnabled() == null) newClient.setDirectAccessGrantsEnabled(true);
        }

        if (rep.isImplicitFlowEnabled() == null) newClient.setImplicitFlowEnabled(false);
        if (rep.getAttributes() == null || !rep.getAttributes().containsKey("frontchannelLogout")) {
            newClient.setFrontchannelLogout(false);
        }
    }
}
