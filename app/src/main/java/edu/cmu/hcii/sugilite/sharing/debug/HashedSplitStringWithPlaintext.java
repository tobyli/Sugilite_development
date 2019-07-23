package edu.cmu.hcii.sugilite.sharing.debug;

import edu.cmu.hcii.sugilite.sharing.HashedSplitString;
import edu.cmu.hcii.sugilite.sharing.HashedString;
import edu.cmu.hcii.sugilite.sharing.HashedSubString;

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
