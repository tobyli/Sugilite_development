package edu.cmu.hcii.sugilite.ontology;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class UISnapshot {
    private Set<SugiliteTriple> triples;

    //indexes for triples
    private Map<SugiliteEntity, Set<SugiliteTriple>> subjectTriplesMap;
    private Map<SugiliteEntity, Set<SugiliteTriple>> objectTriplesMap;
    private Map<SugiliteRelation, Set<SugiliteTriple>> predicateTriplesMap;

    //indexes for entities and relations
    private Map<Integer, SugiliteEntity> sugiliteEntityIdSugiliteEntityMap;
    private Map<Integer, SugiliteRelation> sugiliteRelationIdSugiliteRelationMap;

    public UISnapshot(){
        //empty
    }

    public UISnapshot(AccessibilityEvent event){
        //TODO: contruct a UI snapshot from an event
    }

    public UISnapshot(AccessibilityNodeInfo rootNode){
        //TODO: contruct a UI snapshot from a rootNode
    }

    public void update(AccessibilityEvent event){
        // TODO: update the UI snapshot based on an event
    }

    public void update(AccessibilityNodeInfo rootNode){
        //TODO: update the UI snapshot basd on a rootNode
    }

    //add a triple to the UI snapshot
    public void addTriple(SugiliteTriple triple){
        triples.add(triple);

        //fill in the indexes for triples
        if(!subjectTriplesMap.containsKey(triple.getSubject())){
            subjectTriplesMap.put(triple.getSubject(), new HashSet<SugiliteTriple>());
        }
        if(!predicateTriplesMap.containsKey(triple.getPredicate())){
            predicateTriplesMap.put(triple.getPredicate(), new HashSet<SugiliteTriple>());
        }
        if(!objectTriplesMap.containsKey(triple.getObject())){
            objectTriplesMap.put(triple.getObject(), new HashSet<SugiliteTriple>());
        }

        subjectTriplesMap.get(triple.getSubject()).add(triple);
        predicateTriplesMap.get(triple.getPredicate()).add(triple);
        objectTriplesMap.get(triple.getObject()).add(triple);


        //fill in the two indexes for entities and relations
        if(!sugiliteEntityIdSugiliteEntityMap.containsKey(triple.getSubject().getEntityId())){
            sugiliteEntityIdSugiliteEntityMap.put(triple.getSubject().getEntityId(), triple.getSubject());
        }
        if(!sugiliteRelationIdSugiliteRelationMap.containsKey(triple.getPredicate().getRelationId())){
            sugiliteRelationIdSugiliteRelationMap.put(triple.getPredicate().getRelationId(), triple.getPredicate());
        }
        if(!sugiliteEntityIdSugiliteEntityMap.containsKey(triple.getObject().getEntityId())){
            sugiliteEntityIdSugiliteEntityMap.put(triple.getObject().getEntityId(), triple.getObject());
        }

    }

    public void removeTriple(SugiliteTriple triple){
        triples.remove(triple);

        if(subjectTriplesMap.get(triple.getSubject()) != null){
            subjectTriplesMap.get(triple.getSubject()).remove(triple);
        }

        if(predicateTriplesMap.get(triple.getPredicate()) != null){
            predicateTriplesMap.get(triple.getPredicate()).remove(triple);
        }

        if(objectTriplesMap.get(triple.getObject()) != null){
            objectTriplesMap.get(triple.getObject()).remove(triple);
        }

    }

    public Map<Integer, SugiliteEntity> getSugiliteEntityIdSugiliteEntityMap() {
        return sugiliteEntityIdSugiliteEntityMap;
    }

    public Map<Integer, SugiliteRelation> getSugiliteRelationIdSugiliteRelationMap() {
        return sugiliteRelationIdSugiliteRelationMap;
    }

    public Map<SugiliteEntity, Set<SugiliteTriple>> getSubjectTriplesMap() {
        return subjectTriplesMap;
    }

    public Map<SugiliteEntity, Set<SugiliteTriple>> getObjectTriplesMap() {
        return objectTriplesMap;
    }
}
