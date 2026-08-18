
package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;

import java.util.List;

/**
 * Integration test verifying Keycloak's realm import functionality: realm deletion,
 * verification of imported realm settings, and helper assertions for protocol
 * mappers and required credentials. Also configures the test realms used by the suite.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImportTest extends AbstractTestRealmKeycloakTest {

    @Test
    public void demoDelete() throws Exception {
        removeRealm("demo-delete");
    }

    @Test
    public void install2() throws Exception {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("demo");

            Assert.assertEquals(600, realm.getAccessCodeLifespan());
            Assert.assertEquals(900, realm.getAccessTokenLifespanForImplicitFlow());
            Assert.assertEquals(2592000, realm.getOfflineSessionIdleTimeout());

            List<RequiredCredentialModel> requiredCreds = realm.getRequiredCredentials();
            verifyRequiredCredentials(requiredCreds, "password");
        });
    }

    private static void verifyRequiredCredentials(List<RequiredCredentialModel> requiredCreds, String expectedType) {
        Assert.assertEquals(1, requiredCreds.size());
        Assert.assertEquals(expectedType, requiredCreds.get(0).getType());
    }

    private static void assertGssProtocolMapper(ProtocolMapperModel gssCredentialMapper) {
        Assert.assertEquals(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME, gssCredentialMapper.getName());
        Assert.assertEquals("openid-connect", gssCredentialMapper.getProtocol());
        Assert.assertEquals("oidc-usermodel-attribute-mapper", gssCredentialMapper.getProtocolMapper());
        Assert.assertEquals("true", gssCredentialMapper.getConfig().get("id.token.claim"));
        Assert.assertEquals("true", gssCredentialMapper.getConfig().get("access.token.claim"));
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealmParm) {
        log.info("Importing test realms for ImportTest");

        RealmRepresentation testRealm2 = loadJson(getClass().getResourceAsStream("/model/testrealm2.json"), RealmRepresentation.class);
        adminClient.realms().create(testRealm2);

        RealmRepresentation demoRealm = loadJson(getClass().getResourceAsStream("/model/testrealm-demo.json"), RealmRepresentation.class);
        demoRealm.setRealm("demo");
        demoRealm.setId("demo");
        adminClient.realms().create(demoRealm);
    }
}
