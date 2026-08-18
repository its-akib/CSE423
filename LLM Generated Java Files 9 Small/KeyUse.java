package org.keycloak.crypto;

public enum KeyUse {

    SIG("sig"),
    ENC("enc");

    private String specName;

    KeyUse(String specName) {
        this.specName = specName;
    }

    public String getSpecName() {
        return specName;
    }
}
