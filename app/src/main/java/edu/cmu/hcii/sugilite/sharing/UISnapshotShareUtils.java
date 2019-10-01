package edu.cmu.hcii.sugilite.sharing;

import android.util.Log;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;
import edu.cmu.hcii.sugilite.sharing.model.StringInContext;

import java.util.Arrays;
import java.util.Map;

import static edu.cmu.hcii.sugilite.sharing.SugiliteSharingScriptPreparer.POTENTIALLY_PRIVATE_RELATIONS;

public class UISnapshotShareUtils {
    public static void prepareSnapshotForSharing(SerializableUISnapshot snapshot, Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements) {
        // for some reason the predicatetriplesmap seems to not be correct
        // for (SugiliteRelation relation : Const.POTENTIALLY_PRIVATE_RELATIONS) {
        //     Set<SugiliteSerializableTriple> triples = snapshot.getPredicateTriplesMap().get(relation);
        //     if (triples != null) {
        //         for (SugiliteSerializableTriple triple : triples) {
        //             StringInContext key = new StringInContext(snapshot.getActivityName(), snapshot.getPackageName(), triple.getObjectStringValue());
        //             StringAlternativeGenerator.StringAlternative alt = replacements.get(key);
        //             if (alt == null) {
        //                 // TODO add HashedSplitString edge
        //             } else if (alt.priority == 0) {
        //                 continue;
        //             } else {
        //                 // TODO add StringAlternative edge
        //             }

        //             // delete existing
        //             // TODO
        //         }
        //     } else {
        //         Log.i("UISnapshotShareUtils", "No triple predicate map found for " + relation.getRelationName());
        //     }
        // }
        for (SugiliteSerializableTriple triple : snapshot.getTriples()) {
            if (triple != null && triple.getPredicate() != null && Arrays.stream(POTENTIALLY_PRIVATE_RELATIONS).anyMatch(triple.getPredicate()::equals)) {
                StringInContext key = new StringInContext(snapshot.getActivityName(), snapshot.getPackageName(), triple.getObjectStringValue());
                StringAlternativeGenerator.StringAlternative alt = replacements.get(key);
                if (alt == null) {
                    // TODO add HashedSplitString edge
                    Log.v("UISnapshotShareUtils", "should hash and remove \"" + triple.getObjectStringValue() + "\"");
                } else if (alt.priority == 0) {
                    Log.v("UISnapshotShareUtils", "should keep \"" + triple.getObjectStringValue() + "\"");
                    continue;
                } else {
                    Log.v("UISnapshotShareUtils", "should replace \"" + triple.getObjectStringValue() + "\" with \"" + alt.altText + "\"");
                    // TODO add StringAlternative edge
                }

                // delete existing
                // TODO
            } else {
                Log.v("UISnapshotShareUtils", "null something");
            }
        }
    }
}
