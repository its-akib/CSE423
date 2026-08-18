package org.keycloak.testsuite.client;

import org.junit.After;
import org.junit.Before;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Shared fixture/scaffolding for every test that exercises the Client Registration
 * REST/service API. Concrete subclasses inherit the realm bootstrap and the
 * authenticated-client helper operations defined here.
 */
public abstract class AbstractClientRegistrationTest extends AbstractKeycloakTest {

    public static final String REALM_NAME = "test";

    ClientRegistration reg;

    @Before
    public void before() throws Exception {
        reg = ClientRegistration.create()
                .url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", REALM_NAME)
                .build();
    }

    @After
    public void after() throws Exception {
        reg.close();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealm = new RealmRepresentation();
        testRealm.setId(REALM_NAME);
        testRealm.setRealm(REALM_NAME);
        testRealm.setEnabled(true);

        LinkedList<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        credentials.add(password);

        UserRepresentation manageClients = new UserRepresentation();
        manageClients.setUsername("manage-clients");
        manageClients.setEnabled(true);
        manageClients.setCredentials(credentials);
        manageClients.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID,
                Collections.singletonList(AdminRoles.MANAGE_CLIENTS)));

        UserRepresentation createClients = new UserRepresentation();
        createClients.setUsername("create-clients");
        createClients.setEnabled(true);
        createClients.setCredentials(credentials);
        createClients.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID,
                Collections.singletonList(AdminRoles.CREATE_CLIENT)));

        UserRepresentation noAccess = new UserRepresentation();
        noAccess.setUsername("no-access");
        noAccess.setEnabled(true);
        noAccess.setCredentials(credentials);

        UserRepresentation testUser = new UserRepresentation();
        testUser.setUsername("test-user");
        testUser.setEnabled(true);
        testUser.setCredentials(credentials);

        testRealm.setUsers(java.util.Arrays.asList(manageClients, createClients, noAccess, testUser));

        testRealms.add(testRealm);
    }

    public ClientRepresentation createClient(ClientRepresentation client) throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation result = reg.create(client);
        reg.auth(null);
        return result;
    }

    public ClientRepresentation getClient(String uuid) {
        try {
            return adminClient.realm(REALM_NAME).clients().get(uuid).toRepresentation();
        } catch (NotFoundException e) {
            return null;
        }
    }

    void authCreateClients() {
        String token = getToken("create-clients", "password");
        reg.auth(Auth.token(token));
    }

    void authManageClients() {
        String token = getToken("manage-clients", "password");
        reg.auth(Auth.token(token));
    }

    void authNoAccess() {
        String token = getToken("no-access", "password");
        reg.auth(Auth.token(token));
    }

    private String getToken(String username, String password) {
        try {
            return oauth.doGrantAccessTokenRequest(REALM_NAME, username, password, null, Constants.ADMIN_CLI_CLIENT_ID, null)
                    .getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
