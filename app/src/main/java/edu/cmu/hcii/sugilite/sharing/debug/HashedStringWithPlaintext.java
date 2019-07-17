package edu.cmu.hcii.sugilite.sharing.debug;

import edu.cmu.hcii.sugilite.sharing.HashedString;

public class HashedStringWithPlaintext extends HashedString implements HasPlaintext {
    private String plaintext;

    public HashedStringWithPlaintext(String string) {
        super(string);
        this.plaintext = string;
    }

    @Override
    public String getPlaintext() {
        return plaintext;
    }
}
