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
    private Map<Integer, Set<SugiliteSerializableTriple>> subjectTriplesMap;
    private Map<Integer, Set<SugiliteSerializableTriple>> objectTriplesMap;
    private Map<Integer, Set<SugiliteSerializableTriple>> predicateTriplesMap;

    //indexes for entities and relations
    private Map<Integer, SugiliteSerializableEntity> sugiliteEntityIdSugiliteEntityMap;
    private Map<Integer, SugiliteRelation> sugiliteRelationIdSugiliteRelationMap;

    private transient int entityIdCounter;
    private transient Map<Node, SugiliteSerializableEntity<Node>> accessibilityNodeInfoSugiliteEntityMap;
    private transient Map<String, SugiliteSerializableEntity<String>> stringSugiliteEntityMap;
    private transient Map<Boolean, SugiliteSerializableEntity<Boolean>> booleanSugiliteEntityMap;

    public SerializableUISnapshot(UISnapshot uiSnapshot) {
        if(uiSnapshot.getTriples() != null) {
            triples = new HashSet<>();
            for (SugiliteTriple t : uiSnapshot.getTriples()) {
                triples.add(new SugiliteSerializableTriple(t));
            }
        }

        subjectTriplesMap = new HashMap<>();
        Map<Integer, Set<SugiliteTriple>> oldMap = uiSnapshot.getSubjectTriplesMap();
        for(Integer entityId : oldMap.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMap.get(entityId)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            subjectTriplesMap.put(entityId, newSet);
        }

        objectTriplesMap = new HashMap<>();
        Map<Integer, Set<SugiliteTriple>> oldMapO = uiSnapshot.getObjectTriplesMap();
        for(Integer entityId : oldMapO.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMapO.get(entityId)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            objectTriplesMap.put(entityId, newSet);
        }

        predicateTriplesMap = new HashMap<>();
        Map<Integer, Set<SugiliteTriple>> oldMapR = uiSnapshot.getPredicateTriplesMap();
        for(Integer relationId : oldMapR.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMapR.get(relationId)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            predicateTriplesMap.put(relationId, newSet);
        }

        //fill in the sugiliteEntityIdSugiliteEntityMap
        sugiliteEntityIdSugiliteEntityMap = new HashMap<>();
        Map<Integer, SugiliteEntity> oldMapID = uiSnapshot.getSugiliteEntityIdSugiliteEntityMap();
        for(Integer i : oldMapID.keySet()) {
            sugiliteEntityIdSugiliteEntityMap.put(i, new SugiliteSerializableEntity(oldMapID.get(i)));
        }

        sugiliteRelationIdSugiliteRelationMap = uiSnapshot.getSugiliteRelationIdSugiliteRelationMap();

        entityIdCounter = uiSnapshot.getEntityIdCounter();

        accessibilityNodeInfoSugiliteEntityMap = new HashMap<>();
        Map<Node, SugiliteEntity<Node>> oldMapANI = uiSnapshot.getNodeSugiliteEntityMap();
        for(Node ani : oldMapANI.keySet()) {
            SugiliteEntity<Node> oldEntity = oldMapANI.get(ani);
            accessibilityNodeInfoSugiliteEntityMap.put(ani,
                    new SugiliteSerializableEntity<>(oldEntity));
        }

        stringSugiliteEntityMap = new HashMap<>();
        Map<String, SugiliteEntity<String>> oldMapString = uiSnapshot.getStringSugiliteEntityMap();
        for(String s : oldMapString.keySet()) {
            stringSugiliteEntityMap.put(s, new SugiliteSerializableEntity<>(oldMapString.get(s)));
        }

        booleanSugiliteEntityMap = new HashMap<>();
        Map<Boolean, SugiliteEntity<Boolean>> oldMapBool = uiSnapshot.getBooleanSugiliteEntityMap();
        for(Boolean b : oldMapBool.keySet()) {
            booleanSugiliteEntityMap.put(b, new SugiliteSerializableEntity(oldMapBool.get(b)));
        }
    }
}
