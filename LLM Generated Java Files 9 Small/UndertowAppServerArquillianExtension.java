package org.keycloak.testsuite.arquillian.undertow;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.keycloak.testsuite.arquillian.undertow.container.UndertowDeploymentArchiveProcessor;

public class UndertowAppServerArquillianExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, UndertowAppServer.class)
                .service(ApplicationArchiveProcessor.class, UndertowDeploymentArchiveProcessor.class);
    }
}
