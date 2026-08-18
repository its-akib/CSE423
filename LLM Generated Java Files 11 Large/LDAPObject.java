package org.keycloak.storage.ldap.idm.model;

import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory representation of a single LDAP directory entry, used throughout
 * Keycloak's LDAP user federation storage provider.
 */
public class LDAPObject {

    private static final Logger logger = Logger.getLogger(LDAPObject.class);

    private String uuid;
    private LDAPDn dn;
    private String rdnAttributeName;

    private final List<String> objectClasses = new LinkedList<>();
    private final List<String> readOnlyAttributeNames = new LinkedList<>();
    private final Map<String, Set<String>> attributes = new HashMap<>();
    private final Map<String, Set<String>> lowerCasedAttributes = new HashMap<>();
    private final Map<String, Integer> rangedAttributes = new HashMap<>();

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public LDAPDn getDn() {
        return dn;
    }

    public void setDn(LDAPDn dn) {
        this.dn = dn;
    }

    public List<String> getObjectClasses() {
        return objectClasses;
    }

    public void setObjectClasses(Collection<String> objectClasses) {
        this.objectClasses.clear();
        this.objectClasses.addAll(objectClasses);
    }

    public List<String> getReadOnlyAttributeNames() {
        return readOnlyAttributeNames;
    }

    public void addReadOnlyAttributeName(String readOnlyAttribute) {
        readOnlyAttributeNames.add(readOnlyAttribute.toLowerCase());
    }

    public void removeReadOnlyAttributeName(String readOnlyAttribute) {
        readOnlyAttributeNames.remove(readOnlyAttribute.toLowerCase());
    }

    public String getRdnAttributeName() {
        return rdnAttributeName;
    }

    public void setRdnAttributeName(String rdnAttributeName) {
        this.rdnAttributeName = rdnAttributeName;
    }

    public void setSingleAttribute(String attributeName, String value) {
        Set<String> asSet = new LinkedHashSet<>();
        asSet.add(value);
        setAttribute(attributeName, asSet);
    }

    public void setAttribute(String attributeName, Set<String> values) {
        attributes.put(attributeName, values);
        lowerCasedAttributes.put(attributeName.toLowerCase(), values);
    }

    public String getAttributeAsString(String name) {
        Set<String> l = lowerCasedAttributes.get(name.toLowerCase());
        if (l == null || l.isEmpty()) {
            return null;
        } else {
            if (l.size() != 1) {
                logger.warnf("More attribute values found for attribute '%s' of LDAP object '%s'. All values: %s",
                        name, dn, l);
            }
            return l.iterator().next();
        }
    }

    public Set<String> getAttributeAsSet(String name) {
        Set<String> l = lowerCasedAttributes.get(name.toLowerCase());
        return l == null ? null : new LinkedHashSet<>(l);
    }

    public boolean isRangeComplete(String attributeName) {
        return !rangedAttributes.containsKey(attributeName.toLowerCase());
    }

    public int getCurrentRange(String attributeName) {
        Integer range = rangedAttributes.get(attributeName.toLowerCase());
        return range == null ? -1 : range;
    }

    public boolean isRangeCompleteForAllAttributes() {
        return rangedAttributes.isEmpty();
    }

    public void addRangedAttribute(String attributeName, int currentRangeMax) {
        Integer existing = rangedAttributes.get(attributeName.toLowerCase());
        if (existing == null || currentRangeMax > existing) {
            rangedAttributes.put(attributeName.toLowerCase(), currentRangeMax);
        }
    }

    public void populateRangedAttribute(LDAPObject sourcePage, String attributeName) {
        String lower = attributeName.toLowerCase();
        Set<String> existingValues = attributes.get(attributeName);
        Set<String> newValues = sourcePage.getAttributeAsSet(attributeName);
        if (existingValues != null && newValues != null) {
            existingValues.addAll(newValues);
        }

        if (sourcePage.isRangeComplete(attributeName)) {
            rangedAttributes.remove(lower);
        } else {
            addRangedAttribute(attributeName, sourcePage.getCurrentRange(attributeName));
        }
    }

    public Map<String, Set<String>> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(getClass().isInstance(obj))) return false;
        LDAPObject other = (LDAPObject) obj;
        return getUuid() != null && getUuid().equals(other.getUuid());
    }

    @Override
    public int hashCode() {
        return getUuid() == null ? super.hashCode() : getUuid().hashCode();
    }

    @Override
    public String toString() {
        return String.format("LDAPObject [ dn: %s, uuid: %s, attributes: %s, readOnlyAttributeNames: %s, rangedAttributes: %s ]",
                dn, uuid, attributes, readOnlyAttributeNames, rangedAttributes);
    }
}
