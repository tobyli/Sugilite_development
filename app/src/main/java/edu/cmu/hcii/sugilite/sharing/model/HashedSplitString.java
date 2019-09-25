package edu.cmu.hcii.sugilite.sharing.model;

import java.util.List;

public class HashedSplitString {
    public HashedString preferred;
    public List<HashedSubString> alternatives;

    public HashedSplitString(HashedString preferred, List<HashedSubString> alternatives) {
        this.preferred = preferred;
        this.alternatives = alternatives;
    }
}
