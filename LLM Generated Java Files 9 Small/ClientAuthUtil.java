package org.keycloak.authentication.authenticators.client;

import org.keycloak.representations.idm.OAuth2ErrorRepresentation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ClientAuthUtil {

    public static Response errorResponse(int status, String error, String errorDescription) {
        OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(error, errorDescription);
        return Response.status(status)
                .entity(errorRep)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
