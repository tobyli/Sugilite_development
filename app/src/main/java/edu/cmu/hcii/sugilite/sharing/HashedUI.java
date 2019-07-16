package edu.cmu.hcii.sugilite.sharing;

import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;

import java.util.*;

public class HashedUI {

    private static final SugiliteRelation[] USABLE_RELATIONS = {
            SugiliteRelation.HAS_TEXT,
            SugiliteRelation.HAS_CHILD_TEXT
    };

    public final String packageName;
    public final String activityName;
    public final HashedString packageUserHash;
    public final List<HashedSplitString> hashedTexts;

    // note: storing activityname and packagename may be redundant
    public HashedUI(String packageName, String activityName, SerializableUISnapshot snapshot, String androidID, HashedSplitStringGenerator generator) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.packageUserHash = new HashedString(packageName + androidID);
        this.hashedTexts = new ArrayList<>();

        // use set to ensure we don't save duplicate strings
        Set<String> childStrings = new HashSet<String>();

        for (SugiliteSerializableTriple triple : snapshot.getTriples()) {
            // if we can hash this kind of relation
            if (Arrays.stream(USABLE_RELATIONS).anyMatch(triple.getPredicate()::equals)) {
                childStrings.add(triple.getObjectStringValue());
            }
        }

        for (String s : childStrings) {
            hashedTexts.add(generator.generate(s));
        }
    }
}
