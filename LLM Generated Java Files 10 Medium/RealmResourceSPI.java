
package org.keycloak.services.resource;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * SPI that allows to define additional (unknown) sub-resources for the RESTful API of Realms.
 * This is an internal-only SPI.
 */
public class RealmResourceSPI implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "realm-restapi-extension";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RealmResourceProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RealmResourceProviderFactory.class;
    }
}
