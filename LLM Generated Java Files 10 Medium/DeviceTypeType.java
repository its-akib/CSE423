
package org.keycloak.dom.saml.v2.ac.classes;

/**
 * <p>Java class for DeviceTypeType.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;simpleType name="DeviceTypeType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN"&gt;
 *     &lt;enumeration value="hardware"/&gt;
 *     &lt;enumeration value="software"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 */
public enum DeviceTypeType {

    HARDWARE("hardware"),
    SOFTWARE("software");

    private final String value;

    DeviceTypeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DeviceTypeType fromValue(String v) {
        for (DeviceTypeType c : DeviceTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
