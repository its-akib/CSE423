package org.keycloak.subsystem.server.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.keycloak.subsystem.server.attributes.ModulesListAttributeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WildFly management resource definition for the Keycloak subsystem's &lt;theme&gt;
 * configuration element.
 */
public class ThemeResourceDefinition extends SimpleResourceDefinition {

    public static final String TAG_NAME = "theme";
    protected static final String RESOURCE_NAME = "defaults";

    protected static final SimpleAttributeDefinition STATIC_MAX_AGE =
            new SimpleAttributeDefinitionBuilder("staticMaxAge", ModelType.LONG, true)
                    .setXmlName("staticMaxAge")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(2592000))
                    .setRestartAllServices()
                    .build();

    protected static final SimpleAttributeDefinition CACHE_THEMES =
            new SimpleAttributeDefinitionBuilder("cacheThemes", ModelType.BOOLEAN, true)
                    .setXmlName("cacheThemes")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(true))
                    .setRestartAllServices()
                    .build();

    protected static final SimpleAttributeDefinition CACHE_TEMPLATES =
            new SimpleAttributeDefinitionBuilder("cacheTemplates", ModelType.BOOLEAN, true)
                    .setXmlName("cacheTemplates")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(true))
                    .setRestartAllServices()
                    .build();

    protected static final SimpleAttributeDefinition WELCOME_THEME =
            new SimpleAttributeDefinitionBuilder("welcomeTheme", ModelType.STRING, true)
                    .setXmlName("welcomeTheme")
                    .setAllowExpression(true)
                    .setRestartAllServices()
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT =
            new SimpleAttributeDefinitionBuilder("default", ModelType.STRING, true)
                    .setXmlName("default")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode("keycloak"))
                    .setRestartAllServices()
                    .build();

    protected static final SimpleAttributeDefinition DIR =
            new SimpleAttributeDefinitionBuilder("dir", ModelType.STRING, true)
                    .setXmlName("dir")
                    .setAllowExpression(true)
                    .setRestartAllServices()
                    .build();

    protected static final StringListAttributeDefinition MODULES =
            new ModulesListAttributeBuilder().build();

    protected static final List<AttributeDefinition> ALL_ATTRIBUTES = new ArrayList<>();

    static {
        ALL_ATTRIBUTES.add(STATIC_MAX_AGE);
        ALL_ATTRIBUTES.add(CACHE_THEMES);
        ALL_ATTRIBUTES.add(CACHE_TEMPLATES);
        ALL_ATTRIBUTES.add(WELCOME_THEME);
        ALL_ATTRIBUTES.add(DEFAULT);
        ALL_ATTRIBUTES.add(DIR);
        ALL_ATTRIBUTES.add(MODULES);
    }

    private static final Map<String, AttributeDefinition> DEFINITION_LOOKUP = new HashMap<>();

    static {
        for (AttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }

    protected static final ReloadRequiredWriteAttributeHandler WRITE_ATTR_HANDLER =
            new ReloadRequiredWriteAttributeHandler(ALL_ATTRIBUTES);

    protected ThemeResourceDefinition() {
        super(PathElement.pathElement(TAG_NAME),
                KeycloakExtension.getResourceDescriptionResolver(TAG_NAME),
                ThemeResourceAddHandler.INSTANCE,
                ThemeResourceRemoveHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attr : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, WRITE_ATTR_HANDLER);
        }
    }

    public static SimpleAttributeDefinition lookup(String name) {
        return (SimpleAttributeDefinition) DEFINITION_LOOKUP.get(name);
    }
}
