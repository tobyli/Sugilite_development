package edu.cmu.hcii.sugilite.sharing;

import edu.cmu.hcii.sugilite.sharing.debug.HashedSplitStringWithPlaintext;
import edu.cmu.hcii.sugilite.sharing.debug.HashedStringWithPlaintext;
import edu.cmu.hcii.sugilite.sharing.debug.HashedSubStringWithPlaintext;

import java.util.ArrayList;

public class HashedSplitStringGenerator {
    public HashedSplitStringGenerator() {
    }

    public HashedSplitString generate(String s) {
        ArrayList<HashedSubString> substrings = new ArrayList<>();
        String[] pieces = s.split("\\s");
        if (pieces.length >= 2 && pieces.length <= 4) {
            for (int skip = 0; skip < pieces.length; skip++) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < pieces.length; i++) {
                    if (skip == i) {
                        sb.append("<pii>");
                        sb.append(' ');
                    } else {
                        sb.append(pieces[i]);
                        sb.append(' ');
                    }
                }
                substrings.add(new HashedSubStringWithPlaintext(sb.toString(), 1));
            }
        }
        return new HashedSplitStringWithPlaintext(new HashedStringWithPlaintext(s), substrings);
    }
}
