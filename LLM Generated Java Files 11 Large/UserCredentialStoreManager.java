package org.keycloak.credential;

import org.keycloak.common.util.reflections.Types;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.UserStorageProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Central runtime implementation of the {@link UserCredentialManager} SPI facade. Routes
 * every credential operation to the appropriate backing store or provider, respecting
 * legacy federation providers before falling back to the registered {@link CredentialProvider}
 * chain.
 */
public class UserCredentialStoreManager implements UserCredentialManager, OnUserCache {

    protected KeycloakSession session;

    public UserCredentialStoreManager(KeycloakSession session) {
        this.session = session;
    }

    protected UserCredentialStore getStoreForUser(UserModel user) {
        if (StorageId.isLocalStorage(user)) {
            return (UserCredentialStore) session.userLocalStorage();
        } else {
            return (UserCredentialStore) session.userFederatedStorage();
        }
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        getStoreForUser(user).updateCredential(realm, user, cred);
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        return getStoreForUser(user).createCredential(realm, user, cred);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        boolean removed = getStoreForUser(user).removeStoredCredential(realm, user, id);
        if (removed) {
            session.userCache().evict(realm, user);
        }
        return removed;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        return getStoreForUser(user).getStoredCredentialById(realm, user, id);
    }

    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user) {
        return getStoreForUser(user).getStoredCredentials(realm, user);
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type) {
        return getStoreForUser(user).getStoredCredentialsByType(realm, user, type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return getStoreForUser(user).getStoredCredentialByNameAndType(realm, user, name, type);
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId) {
        return getStoreForUser(user).moveCredentialTo(realm, user, id, newPreviousCredentialId);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput... inputs) {
        return isValid(realm, user, Arrays.asList(inputs));
    }

    @Override
    public CredentialModel createCredentialThroughProvider(RealmModel realm, UserModel user, CredentialModel model) {
        List<CredentialProvider> providers = getCredentialProviders(session, realm, CredentialProvider.class);
        for (CredentialProvider provider : providers) {
            if (provider.getType().equals(model.getType())) {
                return provider.createCredential(realm, user, provider.getCredentialFromModel(model));
            }
        }
        return null;
    }

    @Override
    public void updateCredentialLabel(RealmModel realm, UserModel user, String credentialId, String userLabel) {
        CredentialModel credential = getStoredCredentialById(realm, user, credentialId);
        credential.setUserLabel(userLabel);
        getStoreForUser(user).updateCredential(realm, user, credential);
        session.userCache().evict(realm, user);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, List<CredentialInput> inputs) {
        List<CredentialInput> toValidate = new LinkedList<>(inputs);

        if (!StorageId.isLocalStorage(user)) {
            StorageId storageId = new StorageId(user.getId());
            String providerId = storageId.getProviderId();
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider != null && provider instanceof CredentialInputValidator && UserStorageManager.isStorageProviderEnabled(realm, providerId)) {
                validate(realm, user, toValidate, (CredentialInputValidator) provider);
            }
        } else if (user.getFederationLink() != null) {
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
            if (provider != null && provider instanceof CredentialInputValidator && UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) {
                validate(realm, user, toValidate, (CredentialInputValidator) provider);
            }
        }

        if (!toValidate.isEmpty()) {
            List<CredentialInputValidator> validators = getCredentialProviders(session, realm, CredentialInputValidator.class);
            for (CredentialInputValidator validator : validators) {
                if (toValidate.isEmpty()) break;
                validate(realm, user, toValidate, validator);
            }
        }

        return toValidate.isEmpty();
    }

    private void validate(RealmModel realm, UserModel user, List<CredentialInput> toValidate, CredentialInputValidator validator) {
        Iterator<CredentialInput> it = toValidate.iterator();
        while (it.hasNext()) {
            CredentialInput input = it.next();
            if (validator.supportsCredentialType(input.getType()) && validator.isValid(realm, user, input)) {
                it.remove();
            }
        }
    }

    public static <T> List<T> getCredentialProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return session.getKeycloakSessionFactory().getProviderFactories(CredentialProvider.class).stream()
                .filter(f -> Types.supports(CredentialProviderFactory.class, f, ProviderFactory.class))
                .map(f -> (T) session.getProvider(CredentialProvider.class, f.getId()))
                .filter(type::isInstance)
                .collect(Collectors.toList());
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!StorageId.isLocalStorage(user)) {
            StorageId storageId = new StorageId(user.getId());
            String providerId = storageId.getProviderId();
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider != null && provider instanceof CredentialInputUpdater && UserStorageManager.isStorageProviderEnabled(realm, providerId)) {
                CredentialInputUpdater updater = (CredentialInputUpdater) provider;
                if (updater.supportsCredentialType(input.getType())) {
                    updater.updateCredential(realm, user, input);
                    return;
                }
            }
        } else if (user.getFederationLink() != null) {
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
            if (provider != null && provider instanceof CredentialInputUpdater && UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) {
                CredentialInputUpdater updater = (CredentialInputUpdater) provider;
                if (updater.supportsCredentialType(input.getType())) {
                    updater.updateCredential(realm, user, input);
                    return;
                }
            }
        }

        List<CredentialInputUpdater> updaters = getCredentialProviders(session, realm, CredentialInputUpdater.class);
        for (CredentialInputUpdater updater : updaters) {
            if (updater.supportsCredentialType(input.getType())) {
                updater.updateCredential(realm, user, input);
                return;
            }
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!StorageId.isLocalStorage(user)) {
            StorageId storageId = new StorageId(user.getId());
            String providerId = storageId.getProviderId();
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider != null && provider instanceof CredentialInputUpdater && UserStorageManager.isStorageProviderEnabled(realm, providerId)) {
                CredentialInputUpdater updater = (CredentialInputUpdater) provider;
                if (updater.supportsCredentialType(credentialType)) {
                    updater.disableCredentialType(realm, user, credentialType);
                }
            }
        } else if (user.getFederationLink() != null) {
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
            if (provider != null && provider instanceof CredentialInputUpdater && UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) {
                CredentialInputUpdater updater = (CredentialInputUpdater) provider;
                if (updater.supportsCredentialType(credentialType)) {
                    updater.disableCredentialType(realm, user, credentialType);
                }
            }
        }

        List<CredentialInputUpdater> updaters = getCredentialProviders(session, realm, CredentialInputUpdater.class);
        for (CredentialInputUpdater updater : updaters) {
            if (updater.supportsCredentialType(credentialType)) {
                updater.disableCredentialType(realm, user, credentialType);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        Set<String> types = new HashSet<>();

        if (!StorageId.isLocalStorage(user)) {
            StorageId storageId = new StorageId(user.getId());
            String providerId = storageId.getProviderId();
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider != null && provider instanceof CredentialInputUpdater && UserStorageManager.isStorageProviderEnabled(realm, providerId)) {
                types.addAll(((CredentialInputUpdater) provider).getDisableableCredentialTypes(realm, user));
            }
        } else if (user.getFederationLink() != null) {
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
            if (provider != null && provider instanceof CredentialInputUpdater && UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) {
                types.addAll(((CredentialInputUpdater) provider).getDisableableCredentialTypes(realm, user));
            }
        }

        List<CredentialInputUpdater> updaters = getCredentialProviders(session, realm, CredentialInputUpdater.class);
        for (CredentialInputUpdater updater : updaters) {
            types.addAll(updater.getDisableableCredentialTypes(realm, user));
        }

        return types.isEmpty() ? (Set<String>) Collections.EMPTY_SET : types;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String type) {
        if (!StorageId.isLocalStorage(user)) {
            StorageId storageId = new StorageId(user.getId());
            String providerId = storageId.getProviderId();
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, providerId);
            if (provider != null && provider instanceof CredentialInputValidator && UserStorageManager.isStorageProviderEnabled(realm, providerId)) {
                CredentialInputValidator validator = (CredentialInputValidator) provider;
                if (validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                    return true;
                }
            }
        } else if (user.getFederationLink() != null) {
            UserStorageProvider provider = UserStorageManager.getStorageProvider(session, realm, user.getFederationLink());
            if (provider != null && provider instanceof CredentialInputValidator && UserStorageManager.isStorageProviderEnabled(realm, user.getFederationLink())) {
                CredentialInputValidator validator = (CredentialInputValidator) provider;
                if (validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                    return true;
                }
            }
        }

        return isConfiguredLocally(realm, user, type);
    }

    @Override
    public boolean isConfiguredLocally(RealmModel realm, UserModel user, String type) {
        List<CredentialInputValidator> validators = getCredentialProviders(session, realm, CredentialInputValidator.class);
        for (CredentialInputValidator validator : validators) {
            if (validator.supportsCredentialType(type) && validator.isConfiguredFor(realm, user, type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CredentialValidationOutput authenticate(KeycloakSession session, RealmModel realm, CredentialInput input) {
        List<UserStorageProvider> storageProviders = UserStorageManager.getEnabledStorageProviders(session, realm, UserStorageProvider.class);
        for (UserStorageProvider provider : storageProviders) {
            if (provider instanceof CredentialAuthentication) {
                CredentialAuthentication auth = (CredentialAuthentication) provider;
                if (auth.supportsCredentialAuthenticationFor(input.getType())) {
                    CredentialValidationOutput output = auth.authenticate(realm, input);
                    if (output != null) {
                        return output;
                    }
                }
            }
        }

        List<CredentialAuthentication> authProviders = getCredentialProviders(session, realm, CredentialAuthentication.class);
        for (CredentialAuthentication auth : authProviders) {
            if (auth.supportsCredentialAuthenticationFor(input.getType())) {
                CredentialValidationOutput output = auth.authenticate(realm, input);
                if (output != null) {
                    return output;
                }
            }
        }

        return null;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        List<OnUserCache> providers = getCredentialProviders(session, realm, OnUserCache.class);
        for (OnUserCache validator : providers) {
            validator.onCache(realm, user, delegate);
        }
    }

    @Override
    public void close() {
    }
}
