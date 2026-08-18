
package org.keycloak.storage.ldap.idm.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link LDAPDn} covering DN construction/parsing, RDN prepending,
 * string escaping, descendant-of relationship checks, and empty RDN handling.
 */
public class LDAPDnTest {

    @Test
    public void testDn() throws Exception {
        LDAPDn dn = LDAPDn.fromString("uid=john,ou=people,dc=example,dc=com");

        Assert.assertEquals("uid=john,ou=people,dc=example,dc=com", dn.toString());

        LDAPDn dn2 = LDAPDn.fromString("uid=john,ou=people,dc=example,dc=com");
        Assert.assertEquals(dn, dn2);

        LDAPDn parent = dn.getParentDn();
        Assert.assertEquals("ou=people,dc=example,dc=com", parent.toString());

        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("DC=Example,DC=Com")));
        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("  dc=example , dc=com ")));
        Assert.assertFalse(dn.isDescendantOf(dn));

        Assert.assertEquals("uid", dn.getFirstRdnAttrName());
        Assert.assertEquals("john", dn.getFirstRdnAttrValue());

        LDAPDn dn3 = LDAPDn.fromString("ou=people,dc=example,dc=com");
        dn3.addFirst("cn", "John, Doe+Admin\\Test");
        Assert.assertEquals("cn", dn3.getFirstRdnAttrName());
        Assert.assertEquals("John, Doe+Admin\\Test", dn3.getFirstRdnAttrValue());
    }

    @Test
    public void testEmptyRDN() throws Exception {
        LDAPDn dn = LDAPDn.fromString("dc=example,dc=com");
        dn.addFirst("cn", "");

        Assert.assertEquals("cn=,dc=example,dc=com", dn.toString());
        Assert.assertEquals("cn", dn.getFirstRdnAttrName());
        Assert.assertEquals("", dn.getFirstRdnAttrValue());

        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("DC=Example,DC=Com")));
        Assert.assertFalse(dn.isDescendantOf(LDAPDn.fromString("ou=other,dc=example,dc=com")));
    }

    @Test
    public void testCorrectEscape() throws Exception {
        LDAPDn dn = LDAPDn.fromString("dc=example,dc=com");
        dn.addFirst("cn", "Jos\u00e9 Ram\u00edrez, Jr. ");

        String serialized = dn.toString();
        Assert.assertEquals("cn=Jos\u00e9 Ram\u00edrez\\, Jr.\\ ,dc=example,dc=com", serialized);

        LDAPDn roundTrip = LDAPDn.fromString(serialized);
        Assert.assertEquals("Jos\u00e9 Ram\u00edrez, Jr. ", roundTrip.getFirstRdnAttrValue());
    }
}
