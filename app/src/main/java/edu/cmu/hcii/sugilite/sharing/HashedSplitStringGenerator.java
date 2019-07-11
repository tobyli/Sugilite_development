package edu.cmu.hcii.sugilite.sharing;

import java.util.ArrayList;

public class HashedSplitStringGenerator {
    public HashedSplitStringGenerator() {
    }

    public HashedSplitString generate(String s) {
        // TODO tokenize/heuristically remove PII
        return new HashedSplitString(new HashedString(s), new ArrayList<>(0));
    }
}
