package edu.cmu.hcii.sugilite.sharing;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringAlternativeGenerator {
    public final static int ORIGINAL_TYPE = 0, PATTERN_MATCH_TYPE = 1, HASH_TYPE = 2;
    public static class StringAlternative implements Serializable {
        public String altText;
        public int priority;
        public int type;

        public StringAlternative(String altText, int priority, int type) {
            this.altText = altText;
            this.priority = priority;
            this.type = type;
        }

        public StringAlternative clone() {
            return new StringAlternative(altText, priority, type);
        }
    }

    public static Set<StringAlternative> generateAlternatives(String s) {
        Set<StringAlternative> alternatives = new HashSet<>();

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
                alternatives.add(new StringAlternative(sb.toString(), 2, PATTERN_MATCH_TYPE));
            }
        }

        // should match numbers punctuated by (optionally) decimal points
        Matcher m = Pattern.compile("\\d+(?:.\\d+)*").matcher(s);
        while (m.find()) {
            alternatives.add(new StringAlternative(s.substring(0, m.start()) + "<number>" + s.substring(m.end()), 1, PATTERN_MATCH_TYPE));
        }

        return alternatives;
    }

    public static void main(String[] args) {
        Set<StringAlternative> alternatives = generateAlternatives("Checkout $4.41");
        for (StringAlternative s : alternatives) {
            System.out.println(s.priority + ": " + s.altText);
        }
    }
}
