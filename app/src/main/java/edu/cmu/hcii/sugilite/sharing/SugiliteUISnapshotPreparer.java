package edu.cmu.hcii.sugilite.sharing;

import android.content.Context;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;
import edu.cmu.hcii.sugilite.sharing.model.HashedString;
import edu.cmu.hcii.sugilite.sharing.model.StringInContext;
import edu.cmu.hcii.sugilite.sharing.model.StringInContextWithIndexAndPriority;

import static edu.cmu.hcii.sugilite.sharing.SugiliteSharingScriptPreparer.getReplacementsFromStringInContextSet;

/**
 * @author toby
 * @date 10/3/19
 * @time 3:44 PM
 */
public class SugiliteUISnapshotPreparer {
    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;


    public SugiliteUISnapshotPreparer(Context context) {
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(context);
    }

    public SerializableUISnapshot prepareUISnapshot(SerializableUISnapshot uiSnapshot) throws Exception {
        // 1. get all StringInContext in the UI snapshot
        Set<StringInContext> originalQueryStrings = getStringsFromUISnapshot(uiSnapshot);

        Map<StringInContext, StringAlternativeGenerator.StringAlternative> replacements = getReplacementsFromStringInContextSet(originalQueryStrings, sugiliteScriptSharingHTTPQueryManager);
        Map<String, String> plainReplacements = new HashMap<>();
        replacements.forEach((string, alternative) -> plainReplacements.put(string.getText(), alternative.altText));
        //4. REPLACE HAS_TEXT and HAS_CHILD_TEXT with hashed equivalents in triples
        SerializableUISnapshot newUISnapshot = new SerializableUISnapshot(uiSnapshot.getActivityName(), uiSnapshot.getPackageName());

        for (SugiliteSerializableTriple triple : uiSnapshot.getTriples()) {
            SugiliteSerializableTriple tripleToAdd = triple;
            if (triple.getPredicateStringValue().equals(SugiliteRelation.HAS_TEXT.getRelationName()) || triple.getPredicateStringValue().equals(SugiliteRelation.HAS_CHILD_TEXT.getRelationName())) {
                tripleToAdd = new SugiliteSerializableTriple(triple.getSubjectId(), triple.getObjectId(), plainReplacements.get(triple.getObjectStringValue()), triple.getPredicateStringValue());
            }

            //populate triples
            newUISnapshot.getTriples().add(tripleToAdd);

            //populate subjectTriplesMap
            if (!newUISnapshot.getSubjectTriplesMap().containsKey(tripleToAdd.getSubjectId())) {
                newUISnapshot.getSubjectTriplesMap().put(tripleToAdd.getSubjectId(), new HashSet<>());
            }
            newUISnapshot.getSubjectTriplesMap().get(tripleToAdd.getSubjectId()).add(tripleToAdd);

            //populate objectTriplesMap
            if (!newUISnapshot.getObjectTriplesMap().containsKey(tripleToAdd.getObjectId())) {
                newUISnapshot.getObjectTriplesMap().put(tripleToAdd.getObjectId(), new HashSet<>());
            }
            newUISnapshot.getObjectTriplesMap().get(tripleToAdd.getObjectId()).add(tripleToAdd);

            //populate predicateTriplesMap
            if (!newUISnapshot.getPredicateTriplesMap().containsKey(tripleToAdd.getPredicateStringValue())) {
                newUISnapshot.getPredicateTriplesMap().put(tripleToAdd.getPredicateStringValue(), new HashSet<>());
            }
            newUISnapshot.getPredicateTriplesMap().get(tripleToAdd.getPredicateStringValue()).add(tripleToAdd);

            //populate sugiliteRelationIdSugiliteRelationMap
            newUISnapshot.getSugiliteRelationIdSugiliteRelationMap().putAll(uiSnapshot.getSugiliteRelationIdSugiliteRelationMap());
        }

        return newUISnapshot;
    }

    /**
     * return a set of all strings and their contexts from a SerializableUISnapshot
     * @param uiSnapshot
     * @return
     */
    private static Set<StringInContext> getStringsFromUISnapshot(SerializableUISnapshot uiSnapshot) {
        Set<StringInContext> result = new HashSet<>();
        Set<SugiliteSerializableTriple> triplesToHandle = new HashSet<>();
        triplesToHandle.addAll(uiSnapshot.getPredicateTriplesMap().get(SugiliteRelation.HAS_TEXT.getRelationName()));
        triplesToHandle.addAll(uiSnapshot.getPredicateTriplesMap().get(SugiliteRelation.HAS_CHILD_TEXT.getRelationName()));

        for (SugiliteSerializableTriple triple : triplesToHandle) {
            result.add(new StringInContext(uiSnapshot.getActivityName(), uiSnapshot.getPackageName(), triple.getObjectStringValue()));
        }

        return result;
    }

}



