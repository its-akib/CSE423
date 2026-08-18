
package org.keycloak.subsystem.saml.as7;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

import java.util.HashMap;

/**
 * Shared static attribute definitions describing a SAML keystore configuration
 * within the AS7/WildFly SAML subsystem management model.
 */
abstract class KeyStoreDefinition {

    static final SimpleAttributeDefinition RESOURCE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.RESOURCE, ModelType.STRING)
                    .setAllowExpression(true)
                    .setXmlName(Constants.XML.RESOURCE)
                    .build();

    static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(Constants.Model.PASSWORD, ModelType.STRING)
                    .setAllowExpression(true)
                    .setXmlName(Constants.XML.PASSWORD)
                    .build();

    static final SimpleAttributeDefinition FILE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.FILE, ModelType.STRING)
                    .setAllowExpression(true)
                    .setXmlName(Constants.XML.FILE)
                    .build();

    static final SimpleAttributeDefinition TYPE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.TYPE, ModelType.STRING)
                    .setAllowExpression(true)
                    .setXmlName(Constants.XML.TYPE)
                    .build();

    static final SimpleAttributeDefinition ALIAS =
            new SimpleAttributeDefinitionBuilder(Constants.Model.ALIAS, ModelType.STRING)
                    .setAllowExpression(true)
                    .setXmlName(Constants.XML.ALIAS)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            RESOURCE, PASSWORD, FILE, TYPE, ALIAS
    };

    static final SimpleAttributeDefinition[] ALL_ATTRIBUTES = {
            RESOURCE, PASSWORD, FILE, TYPE, ALIAS,
            KeyStorePrivateKeyDefinition.ALIAS,
            KeyStorePrivateKeyDefinition.PASSWORD,
            KeyStoreCertificateDefinition.ALIAS
    };

    private static final HashMap<String, SimpleAttributeDefinition> ATTRIBUTE_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition def : ATTRIBUTES) {
            ATTRIBUTE_MAP.put(def.getXmlName(), def);
        }
    }

    static SimpleAttributeDefinition lookup(String xmlName) {
        return ATTRIBUTE_MAP.get(xmlName);
    }
}
