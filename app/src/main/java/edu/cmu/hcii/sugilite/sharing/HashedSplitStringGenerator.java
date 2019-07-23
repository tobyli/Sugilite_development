package edu.cmu.hcii.sugilite.sharing;

import edu.cmu.hcii.sugilite.sharing.debug.HashedSplitStringWithPlaintext;
import edu.cmu.hcii.sugilite.sharing.debug.HashedStringWithPlaintext;
import edu.cmu.hcii.sugilite.sharing.debug.HashedSubStringWithPlaintext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HashedSplitStringGenerator {
    public static HashedSplitString generate(String s) {
        Set<StringAlternativeGenerator.StringAlternative> alts = StringAlternativeGenerator.generateAlternatives(s);

        List<HashedSubString> substrings = new ArrayList<>(alts.size());
        for (StringAlternativeGenerator.StringAlternative alt : alts) {
            substrings.add(new HashedSubStringWithPlaintext(alt.altText, alt.priority));
        }
        return new HashedSplitStringWithPlaintext(new HashedStringWithPlaintext(s), substrings);
    }
}
