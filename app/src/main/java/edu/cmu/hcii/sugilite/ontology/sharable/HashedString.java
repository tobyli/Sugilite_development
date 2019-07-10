package edu.cmu.hcii.sugilite.ontology.sharable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashedString implements Serializable {

    private final byte[] hash;

    public HashedString(String string) {
        this.hash = hash(string);
    }

    public HashedString(byte[] hash) {
        this.hash = hash;
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

    public String toString() {
        return String.format("%064x", new BigInteger(hash));
    }

    public static HashedString fromEncodedString(String encodedString) {
        return new HashedString(new BigInteger(encodedString, 16).toByteArray());
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

    public static void main(String[] args) {
        System.out.println("Hashing \"Starbucks\":");
        HashedString hashed = new HashedString("Starbucks");
        System.out.println(hashed);
        System.out.println("Reinterpreting output:");
        HashedString interpreted = fromEncodedString(hashed.toString());
        System.out.println(interpreted);
        System.out.println("Equals \"Starbucks\"? (want true)");
        System.out.println(interpreted.equals("Starbucks"));
        System.out.println(interpreted.equals(new HashedString("Starbucks")));
        System.out.println("Equals \"Caribou\"? (want false)");
        System.out.println(interpreted.equals("Caribou"));
        System.out.println(interpreted.equals(new HashedString("Caribou")));
    }
}
