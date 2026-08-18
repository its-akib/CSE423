package org.keycloak.testsuite.auth.page.account;

import javax.ws.rs.core.UriBuilder;

public class Autheticator extends AccountManagement {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("totp");
    }
}
