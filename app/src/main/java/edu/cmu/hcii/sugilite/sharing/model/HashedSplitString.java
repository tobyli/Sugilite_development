package edu.cmu.hcii.sugilite.sharing.model;

import java.util.List;

public class HashedSplitString {
    private final HashedString preferred;
    private final List<HashedSubString> alternatives;

    public HashedSplitString(HashedString preferred, List<HashedSubString> alternatives) {
        this.preferred = preferred;
        this.alternatives = alternatives;
    }

    public HashedString getPreferred() {
        return preferred;
    }

    public List<HashedSubString> getAlternatives() {
        return alternatives;
    }
}
