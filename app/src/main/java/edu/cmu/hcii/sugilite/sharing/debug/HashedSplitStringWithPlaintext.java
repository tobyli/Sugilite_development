package edu.cmu.hcii.sugilite.sharing.debug;

import edu.cmu.hcii.sugilite.sharing.model.HashedSplitString;
import edu.cmu.hcii.sugilite.sharing.model.HashedSubString;

import java.util.List;

public class HashedSplitStringWithPlaintext extends HashedSplitString implements HasPlaintext {
    private String plaintext;

    public HashedSplitStringWithPlaintext(HashedStringWithPlaintext preferred, List<HashedSubString> alternatives) {
        super(preferred, alternatives);
        plaintext = preferred.getPlaintext();
    }

    @Override
    public String getPlaintext() {
        return plaintext;
    }
}
