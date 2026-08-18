package org.keycloak.jose.jwe;

import org.keycloak.jose.jwe.enc.JWEEncryptionProvider;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

/**
 * Mutable, fluent holder for the cryptographic keys and content-encryption-key (CEK)
 * material needed while encrypting or decrypting a JWE token.
 */
public class JWEKeyStorage {

    public enum KeyUse {
        ENCRYPTION,
        SIGNATURE
    }

    private Key encryptionKey;
    private Key decryptionKey;
    private byte[] cekBytes;
    private final Map<KeyUse, Key> decodedCEK = new HashMap<>();
    private JWEEncryptionProvider encryptionProvider;

    public Key getEncryptionKey() {
        return encryptionKey;
    }

    public JWEKeyStorage setEncryptionKey(Key encryptionKey) {
        this.encryptionKey = encryptionKey;
        return this;
    }

    public Key getDecryptionKey() {
        return decryptionKey;
    }

    public JWEKeyStorage setDecryptionKey(Key decryptionKey) {
        this.decryptionKey = decryptionKey;
        return this;
    }

    public void setCEKBytes(byte[] cekBytes) {
        this.cekBytes = cekBytes;
    }

    public byte[] getCekBytes() {
        if (cekBytes == null) {
            cekBytes = encryptionProvider.serializeCEK(this);
        }
        return cekBytes;
    }

    public JWEKeyStorage setCEKKey(Key key, KeyUse keyUse) {
        decodedCEK.put(keyUse, key);
        return this;
    }

    public Key getCEKKey(KeyUse keyUse, boolean generateIfNotPresent) {
        Key key = decodedCEK.get(keyUse);
        if (key == null) {
            if (encryptionProvider == null) {
                throw new IllegalStateException("encryptionProvider needs to be set");
            }
            if (generateIfNotPresent && cekBytes == null) {
                generateCekBytes();
            }
            encryptionProvider.deserializeCEK(this);
            key = decodedCEK.get(keyUse);
        }
        return key;
    }

    private void generateCekBytes() {
        int cekLength = encryptionProvider.getExpectedCEKLength();
        cekBytes = JWEUtils.generateSecret(cekLength);
    }

    public void setEncryptionProvider(JWEEncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
    }
}
