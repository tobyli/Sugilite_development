package edu.cmu.hcii.sugilite.sharing.debug;

import edu.cmu.hcii.sugilite.sharing.HashedSubString;

public class HashedSubStringWithPlaintext extends HashedSubString implements HasPlaintext {
    private String plaintext;

    public HashedSubStringWithPlaintext(String string, int priority) {
        super(string, priority);
        this.plaintext = string;
    }

    @Override
    public String getPlaintext() {
        return plaintext;
    }
}
