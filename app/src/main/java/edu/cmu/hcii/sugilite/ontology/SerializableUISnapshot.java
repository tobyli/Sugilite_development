package edu.cmu.hcii.sugilite.ontology;

import android.view.accessibility.AccessibilityNodeInfo;

import edu.cmu.hcii.sugilite.Node;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author nancy
 * @date 10/25/17
 * @time 2:14 PM
 */
public class SerializableUISnapshot implements Serializable {
    private Set<SugiliteSerializableTriple> triples;

    //indexes for triples
    private Map<SugiliteSerializableEntity, Set<SugiliteSerializableTriple>> subjectTriplesMap;
    private Map<SugiliteSerializableEntity, Set<SugiliteSerializableTriple>> objectTriplesMap;
    private Map<SugiliteRelation, Set<SugiliteSerializableTriple>> predicateTriplesMap;

    //indexes for entities and relations
    private Map<Integer, SugiliteSerializableEntity> sugiliteEntityIdSugiliteEntityMap;
    private Map<Integer, SugiliteRelation> sugiliteRelationIdSugiliteRelationMap;

    int entityIdCounter;
    private Map<Node, SugiliteSerializableEntity<Node>> accessibilityNodeInfoSugiliteEntityMap;
    private Map<String, SugiliteSerializableEntity<String>> stringSugiliteEntityMap;

    public SerializableUISnapshot(UISnapshot uiSnapshot) {
        if(uiSnapshot.getTriples() != null) {
            triples = new HashSet<>();
            for (SugiliteTriple t : uiSnapshot.getTriples()) {
                triples.add(new SugiliteSerializableTriple(t));
            }
        }

        subjectTriplesMap = new HashMap<>();
        Map<SugiliteEntity, Set<SugiliteTriple>> oldMap = uiSnapshot.getSubjectTriplesMap();
        for(SugiliteEntity e : oldMap.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMap.get(e)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            subjectTriplesMap.put(new SugiliteSerializableEntity(e), newSet);
        }

        objectTriplesMap = new HashMap<>();
        Map<SugiliteEntity, Set<SugiliteTriple>> oldMapO = uiSnapshot.getObjectTriplesMap();
        for(SugiliteEntity e : oldMapO.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMapO.get(e)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            objectTriplesMap.put(new SugiliteSerializableEntity(e), newSet);
        }

        predicateTriplesMap = new HashMap<>();
        Map<SugiliteRelation, Set<SugiliteTriple>> oldMapR = uiSnapshot.getPredicateTriplesMap();
        for(SugiliteRelation r : oldMapR.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMapR.get(r)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            predicateTriplesMap.put(r, newSet);
        }

        sugiliteEntityIdSugiliteEntityMap = new HashMap<>();
        Map<Integer, SugiliteEntity> oldMapID = uiSnapshot.getSugiliteEntityIdSugiliteEntityMap();
        for(Integer i : oldMapID.keySet()) {
            sugiliteEntityIdSugiliteEntityMap.put(i, new SugiliteSerializableEntity(oldMapID.get(i)));
        }

        sugiliteRelationIdSugiliteRelationMap = uiSnapshot.getSugiliteRelationIdSugiliteRelationMap();

        entityIdCounter = uiSnapshot.getEntityIdCounter();

        accessibilityNodeInfoSugiliteEntityMap = new HashMap<>();
        Map<AccessibilityNodeInfo, SugiliteEntity<AccessibilityNodeInfo>> oldMapANI = uiSnapshot.getAccessibilityNodeInfoSugiliteEntityMap();
        for(AccessibilityNodeInfo ani : oldMapANI.keySet()) {
            SugiliteEntity<AccessibilityNodeInfo> oldEntity = oldMapANI.get(ani);
            accessibilityNodeInfoSugiliteEntityMap.put(new Node(ani),
                    new SugiliteSerializableEntity<>(oldMapANI.get(ani)));
        }

        stringSugiliteEntityMap = new HashMap<>();
        Map<String, SugiliteEntity<String>> oldMapString = uiSnapshot.getStringSugiliteEntityMap();
        for(String s : oldMapString.keySet()) {
            stringSugiliteEntityMap.put(s, new SugiliteSerializableEntity<>(oldMapString.get(s)));
        }
    }
}
