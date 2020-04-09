package edu.cmu.hcii.sugilite.ontology;

import edu.cmu.hcii.sugilite.model.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private Map<String, Set<SugiliteSerializableTriple>> subjectTriplesMap;
    private Map<String, Set<SugiliteSerializableTriple>> objectTriplesMap;
    private Map<String, Set<SugiliteSerializableTriple>> predicateTriplesMap;

    //indexes for entities and relations
    private Map<String, SugiliteSerializableEntity> sugiliteEntityIdSugiliteEntityMap;
    private Map<Integer, SugiliteRelation> sugiliteRelationIdSugiliteRelationMap;

    private transient int entityIdCounter;
    private transient Map<Node, SugiliteSerializableEntity<Node>> accessibilityNodeInfoSugiliteEntityMap;
    private transient Map<String, SugiliteSerializableEntity<String>> stringSugiliteEntityMap;
    private transient Map<Boolean, SugiliteSerializableEntity<Boolean>> booleanSugiliteEntityMap;

    private String activityName;
    private String packageName;

    public SerializableUISnapshot(String activityName, String packageName) {
        this.activityName = activityName;
        this.packageName = packageName;
        this.triples = new HashSet<>();
        this.subjectTriplesMap = new HashMap<>();
        this.objectTriplesMap = new HashMap<>();
        this.predicateTriplesMap = new HashMap<>();
        this.sugiliteEntityIdSugiliteEntityMap = new HashMap<>();
        this.accessibilityNodeInfoSugiliteEntityMap = new HashMap<>();
        this.stringSugiliteEntityMap = new HashMap<>();
        this.booleanSugiliteEntityMap = new HashMap<>();
        this.sugiliteRelationIdSugiliteRelationMap = new HashMap<>();
    }


    public SerializableUISnapshot(UISnapshot uiSnapshot) {
        this(uiSnapshot.getActivityName(), uiSnapshot.getPackageName());
        //populate triples
        if(uiSnapshot.getTriples() != null) {
            for (SugiliteTriple t : uiSnapshot.getTriples()) {
                triples.add(new SugiliteSerializableTriple(t));
            }
        }

        //populate subjectTriplesMap
        Map<Integer, Set<SugiliteTriple>> oldMap = uiSnapshot.getSubjectTriplesMap();
        for(Integer entityId : oldMap.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMap.get(entityId)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            subjectTriplesMap.put("@"+entityId, newSet);
        }

        //populate objectTriplesMap
        Map<Integer, Set<SugiliteTriple>> oldMapO = uiSnapshot.getObjectTriplesMap();
        for(Integer entityId : oldMapO.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMapO.get(entityId)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            objectTriplesMap.put("@"+entityId, newSet);
        }

        //populate sugiliteRelationIdSugiliteRelationMap
        sugiliteRelationIdSugiliteRelationMap.putAll(uiSnapshot.getSugiliteRelationIdSugiliteRelationMap());

        //populate predicateTriplesMap
        Map<Integer, Set<SugiliteTriple>> oldMapR = uiSnapshot.getPredicateTriplesMap();
        for(Integer relationId : oldMapR.keySet()) {
            Set<SugiliteSerializableTriple> newSet = new HashSet<>();
            for(SugiliteTriple t : oldMapR.get(relationId)) {
                newSet.add(new SugiliteSerializableTriple(t));
            }
            predicateTriplesMap.put(sugiliteRelationIdSugiliteRelationMap.get(relationId).getRelationName(), newSet);
        }

        //populate sugiliteEntityIdSugiliteEntityMap
        Map<Integer, SugiliteEntity> oldMapID = uiSnapshot.getSugiliteEntityIdSugiliteEntityMap();
        for(Integer i : oldMapID.keySet()) {
            sugiliteEntityIdSugiliteEntityMap.put("@"+i, new SugiliteSerializableEntity(oldMapID.get(i)));
        }

        //set entityIdCounter
        entityIdCounter = uiSnapshot.getEntityIdCounter();

        //populate accessibilityNodeInfoSugiliteEntityMap
        Map<Node, SugiliteEntity<Node>> oldMapANI = uiSnapshot.getNodeSugiliteEntityMap();
        for(Node ani : oldMapANI.keySet()) {
            SugiliteEntity<Node> oldEntity = oldMapANI.get(ani);
            accessibilityNodeInfoSugiliteEntityMap.put(ani,
                    new SugiliteSerializableEntity<>(oldEntity));
        }

        //populate stringSugiliteEntityMap
        Map<String, SugiliteEntity<String>> oldMapString = uiSnapshot.getStringSugiliteEntityMap();
        for(String s : oldMapString.keySet()) {
            stringSugiliteEntityMap.put(s, new SugiliteSerializableEntity<>(oldMapString.get(s)));
        }

        //populate booleanSugiliteEntityMap
        Map<Boolean, SugiliteEntity<Boolean>> oldMapBool = uiSnapshot.getBooleanSugiliteEntityMap();
        for(Boolean b : oldMapBool.keySet()) {
            booleanSugiliteEntityMap.put(b, new SugiliteSerializableEntity(oldMapBool.get(b)));
        }
    }

    public Map<String, SugiliteSerializableEntity> getSugiliteEntityIdSugiliteEntityMap() {
        return sugiliteEntityIdSugiliteEntityMap;
    }

    public List<List<String>> triplesToString(){
        List<List<String>> result = new ArrayList<>();

        for(SugiliteSerializableTriple triple : triples){
            List<String> tripleList = new ArrayList<>();
            if(triple.getSubjectId() != null) {
                tripleList.add(triple.getSubjectId());
            }
            if(triple.getPredicateStringValue() != null) {
                tripleList.add(triple.getPredicateStringValue());
            }
            if(triple.getObjectStringValue() != null) {
                tripleList.add(triple.getObjectStringValue());
            }
            if(tripleList.size() == 3){
                result.add(tripleList);
            }
        }
        return result;

    }

    public List<List<String>> triplesToStringWithFilter(SugiliteRelation ... relations){
        Set<String> relationsToFilter = new HashSet<>();
        for(SugiliteRelation relation : relations){
            relationsToFilter.add(relation.getRelationName());
        }
        List<List<String>> result = new ArrayList<>();

        for(SugiliteSerializableTriple triple : triples){
            if(relationsToFilter.contains(triple.getPredicateStringValue())){
                continue;
            }
            List<String> tripleList = new ArrayList<>();
            if(triple.getSubjectId() != null) {
                tripleList.add(triple.getSubjectId());
            }
            if(triple.getPredicateStringValue() != null) {
                tripleList.add(triple.getPredicateStringValue());
            }
            if(triple.getObjectStringValue() != null) {
                tripleList.add(triple.getObjectStringValue());
            }
            if(tripleList.size() == 3){
                result.add(tripleList);
            }
        }
        return result;
    }

    public Map<String, Set<SugiliteSerializableTriple>> getSubjectTriplesMap() {
        return subjectTriplesMap;
    }

    public Map<String, Set<SugiliteSerializableTriple>> getObjectTriplesMap() {
        return objectTriplesMap;
    }

    public Map<String, Set<SugiliteSerializableTriple>> getPredicateTriplesMap() {
        return predicateTriplesMap;
    }

    public Map<Integer, SugiliteRelation> getSugiliteRelationIdSugiliteRelationMap() {
        return sugiliteRelationIdSugiliteRelationMap;
    }

    public void setSubjectTriplesMap(Map<String, Set<SugiliteSerializableTriple>> subjectTriplesMap) {
        this.subjectTriplesMap = subjectTriplesMap;
    }

    public void setObjectTriplesMap(Map<String, Set<SugiliteSerializableTriple>> objectTriplesMap) {
        this.objectTriplesMap = objectTriplesMap;
    }

    public void setPredicateTriplesMap(Map<String, Set<SugiliteSerializableTriple>> predicateTriplesMap) {
        this.predicateTriplesMap = predicateTriplesMap;
    }

    public void setSugiliteRelationIdSugiliteRelationMap(Map<Integer, SugiliteRelation> sugiliteRelationIdSugiliteRelationMap) {
        this.sugiliteRelationIdSugiliteRelationMap = sugiliteRelationIdSugiliteRelationMap;
    }

    public void addTriple (SugiliteSerializableTriple sugiliteSerializableTriple){
        triples.add(sugiliteSerializableTriple);
    }

    public Map<Node, SugiliteSerializableEntity<Node>> getAccessibilityNodeInfoSugiliteEntityMap() {
        return accessibilityNodeInfoSugiliteEntityMap;
    }

    public Set<SugiliteSerializableTriple> getTriples() {
        return triples;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getPackageName() {
        return packageName;
    }
}
