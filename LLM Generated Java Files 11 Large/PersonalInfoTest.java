package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;

/**
 * UI integration test for the account console's "Personal Info" page: editing/saving
 * username/email/first/last name, per-field validation, duplicate detection, and the
 * editUsernameAllowed realm toggle.
 */
public class PersonalInfoTest extends BaseAccountPageTest {

    @Page
    private PersonalInfoPage personalInfoPage;

    private UserRepresentation testUser2;

    @Before
    public void setTestUser() {
        testUser2 = new UserRepresentation();
        testUser2.setUsername("personalinfotestuser2");
        testUser2.setEmail("personalinfotestuser2@email.test");
        testUser2.setFirstName("personalinfotestuser2FirstName");
        testUser2.setLastName("personalinfotestuser2LastName");

        ApiUtil.removeUserByUsername(testRealm(), testUser2.getUsername());
    }

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return personalInfoPage;
    }

    @Test
    public void updateUserInfo() {
        setEditUsernameAllowed(true);

        assertTrue(personalInfoPage.valuesEqual(testUser));
        personalInfoPage.assertUsernameDisabled(false);
        personalInfoPage.assertSaveDisabled(true);

        personalInfoPage.setValues(testUser2);
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();

        personalInfoPage.navigateTo();
        assertTrue(personalInfoPage.valuesEqual(testUser2));

        // partial update: first/last name only
        testUser2.setFirstName("changedFirstName");
        testUser2.setLastName("changedLastName");
        personalInfoPage.setFirstName(testUser2.getFirstName());
        personalInfoPage.setLastName(testUser2.getLastName());
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();

        personalInfoPage.navigateTo();
        assertTrue(personalInfoPage.valuesEqual(testUser2));
    }

    @Test
    public void formValidationTest() {
        setEditUsernameAllowed(true);

        String originalUsername = testUser.getUsername();
        personalInfoPage.setUsername("");
        assertTrue(!personalInfoPage.assertUsernameValid(false));
        personalInfoPage.setUsername(originalUsername);
        assertTrue(personalInfoPage.assertUsernameValid(true));

        String originalEmail = testUser.getEmail();
        personalInfoPage.setEmail("");
        assertTrue(!personalInfoPage.assertEmailValid(false));
        personalInfoPage.setEmail(originalEmail);
        assertTrue(personalInfoPage.assertEmailValid(true));

        String originalFirstName = testUser.getFirstName();
        personalInfoPage.setFirstName("");
        assertTrue(!personalInfoPage.assertFirstNameValid(false));
        personalInfoPage.setFirstName(originalFirstName);
        assertTrue(personalInfoPage.assertFirstNameValid(true));

        String originalLastName = testUser.getLastName();
        personalInfoPage.setLastName("");
        assertTrue(!personalInfoPage.assertLastNameValid(false));
        personalInfoPage.setLastName(originalLastName);
        assertTrue(personalInfoPage.assertLastNameValid(true));

        ApiUtil.createUserWithAdminClient(testRealm(), testUser2);

        personalInfoPage.setUsername(testUser2.getUsername());
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertDanger();
        personalInfoPage.navigateTo();
        assertTrue(personalInfoPage.valuesEqual(testUser));

        personalInfoPage.setEmail(testUser2.getEmail());
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertDanger();
        personalInfoPage.navigateTo();
        assertTrue(personalInfoPage.valuesEqual(testUser));
    }

    @Test
    public void disabledEditUsername() {
        setEditUsernameAllowed(false);

        personalInfoPage.assertUsernameDisabled(true);

        String originalUsername = testUser2.getUsername();
        testUser2.setUsername(testUser.getUsername());

        personalInfoPage.setValues(testUser2);
        personalInfoPage.clickSave();
        personalInfoPage.alert().assertSuccess();

        testUser2.setUsername(originalUsername);

        personalInfoPage.navigateTo();
        assertTrue(personalInfoPage.valuesEqual(testUser2));
    }

    private void setEditUsernameAllowed(boolean value) {
        RealmRepresentation realmRep = testRealmResource().toRepresentation();
        realmRep.setEditUsernameAllowed(value);
        testRealmResource().update(realmRep);
        refreshPageAndWaitForLoad();
    }
}
