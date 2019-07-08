package edu.cmu.hcii.sugilite.ontology.sharable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashedString implements Serializable {

    private final byte[] hash;

    public HashedString(String string) {
        this.hash = hash(string);
    }

    @Override
    public boolean equals(Object obj) {

        byte[] hash2;

        if (obj instanceof HashedString) {
            hash2 = ((HashedString)obj).hash;
        } else if (obj instanceof String) {
            hash2 = hash((String)obj);
        } else {
            return false;
        }

        if (hash.length != hash2.length) return false;
        for (int i = 0; i < hash.length; i++) {
            if (hash[i] != hash2[i]) return false;
        }

        return true;
    }

    public static byte[] hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}