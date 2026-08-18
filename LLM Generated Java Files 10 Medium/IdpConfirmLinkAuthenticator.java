
package org.keycloak.authentication.authenticators.broker;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Authentication-flow step handling the case where a federated login attempt maps
 * to an already-existing local user. Presents a confirmation page (link the accounts,
 * update profile, or fail) and drives the resulting flow action.
 */
public class IdpConfirmLinkAuthenticator extends AbstractIdpAuthenticator {

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx,
                                     BrokeredIdentityContext brokerContext) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String existingUserInfoNote = authSession.getAuthNote(EXISTING_USER_INFO);

        if (existingUserInfoNote == null) {
            ServicesLogger.LOGGER.noDuplicationDetected();
            context.attempted();
            return;
        }

        ExistingUserInfo duplication = ExistingUserInfo.deserialize(existingUserInfoNote);

        Response challenge = context.form()
                .setStatus(Response.Status.OK)
                .setAttribute(LoginFormsProvider.IDENTITY_PROVIDER_BROKER_CONTEXT, serializedCtx)
                .setError(Messages.FEDERATED_IDENTITY_CONFIRM_LINK_MESSAGE, duplication.getDuplicateAttributeName())
                .createIdpLinkConfirmLinkPage();

        context.challenge(challenge);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx,
                               BrokeredIdentityContext brokerContext) {
        MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
        String submitAction = formParams.getFirst("submitAction");

        if (submitAction != null && submitAction.equals("updateProfile")) {
            context.resetFlow(() -> {
                serializedCtx.saveToAuthenticationSession(context.getAuthenticationSession(), BROKERED_CONTEXT_NOTE);
                context.getAuthenticationSession().setAuthNote(ENFORCE_UPDATE_PROFILE, "true");
            });
        } else if (submitAction != null && submitAction.equals("linkAccount")) {
            context.success();
        } else {
            throw new AuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }
}
