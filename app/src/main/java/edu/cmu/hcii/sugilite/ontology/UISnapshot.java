package edu.cmu.hcii.sugilite.ontology;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.automation.Automator;

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

    int entityIdCounter;
    private Map<AccessibilityNodeInfo, SugiliteEntity<AccessibilityNodeInfo>> accessibilityNodeInfoSugiliteEntityMap;
    private Map<String, SugiliteEntity<String>> stringSugiliteEntityMap;

    public UISnapshot(){
        //empty
        triples = new HashSet<>();
        subjectTriplesMap = new HashMap<>();
        objectTriplesMap = new HashMap<>();
        predicateTriplesMap = new HashMap<>();
        sugiliteEntityIdSugiliteEntityMap = new HashMap<>();
        sugiliteRelationIdSugiliteRelationMap = new HashMap<>();
        accessibilityNodeInfoSugiliteEntityMap = new HashMap<>();
        stringSugiliteEntityMap = new HashMap<>();
        entityIdCounter = 0;
    }

    public UISnapshot(AccessibilityEvent event){
        //TODO: contruct a UI snapshot from an event
        //get the rootNode from event and pass into to the below function


    }

    public UISnapshot(AccessibilityNodeInfo rootNode){
        //TODO: contruct a UI snapshot from a rootNode
        this();
        List<AccessibilityNodeInfo> allNodes = Automator.preOrderTraverse(rootNode);
        Map<String, SugiliteEntity<String>> stringSugiliteEntityMap = new HashMap<>();
        if(allNodes != null){
            for(AccessibilityNodeInfo node : allNodes) {

                //get the corresponding entity for the node
                SugiliteEntity<AccessibilityNodeInfo> currentEntity = null;
                if (accessibilityNodeInfoSugiliteEntityMap.containsKey(node)) {
                    currentEntity = accessibilityNodeInfoSugiliteEntityMap.get(node);
                } else {
                    //create a new entity for the node
                    SugiliteEntity<AccessibilityNodeInfo> entity = new SugiliteEntity<>(entityIdCounter++, AccessibilityNodeInfo.class, node);
                    accessibilityNodeInfoSugiliteEntityMap.put(node, entity);
                    currentEntity = entity;
                }

                //start to construct the relationship

                if (node.getClassName() != null) {
                    //class
                    String className = node.getClassName().toString();
                    addEntityStringTriple(currentEntity, className, SugiliteRelation.HAS_CLASS_NAME);
                }

                if (node.getText() != null) {
                    //text
                    String text = node.getText().toString();
                    addEntityStringTriple(currentEntity, text, SugiliteRelation.HAS_TEXT);
                }

                if (node.getPackageName() != null) {
                    //package name
                    String packageName = node.getPackageName().toString();
                    addEntityStringTriple(currentEntity, packageName, SugiliteRelation.HAS_PACKAGE_NAME);
                }

                if (node.getContentDescription() != null) {
                    //content description
                    String contentDescription = node.getContentDescription().toString();
                    addEntityStringTriple(currentEntity, contentDescription, SugiliteRelation.HAS_CONTENT_DESCRIPTION);
                }

                //screen location
                Rect screenLocationRect = new Rect();
                node.getBoundsInScreen(screenLocationRect);
                addEntityStringTriple(currentEntity, screenLocationRect.flattenToString(), SugiliteRelation.HAS_SCREEN_LOCATION);

                //parent location
                Rect parentLocationRect = new Rect();
                node.getBoundsInParent(parentLocationRect);
                addEntityStringTriple(currentEntity, parentLocationRect.flattenToString(), SugiliteRelation.HAS_PARENT_LOCATION);

                //test the has_parent relation
                if (node.getParent() != null) {
                    //parent
                    AccessibilityNodeInfo parentNode = node.getParent();
                    SugiliteEntity<AccessibilityNodeInfo> objectEntity = null;

                    if (accessibilityNodeInfoSugiliteEntityMap.containsKey(parentNode)) {
                        objectEntity = accessibilityNodeInfoSugiliteEntityMap.get(parentNode);
                    } else {
                        //create a new entity for the class name
                        SugiliteEntity<AccessibilityNodeInfo> entity = new SugiliteEntity<>(entityIdCounter++, AccessibilityNodeInfo.class, parentNode);
                        accessibilityNodeInfoSugiliteEntityMap.put(parentNode, entity);
                        objectEntity = entity;
                    }

                    SugiliteTriple triple = new SugiliteTriple(currentEntity, SugiliteRelation.HAS_PARENT, objectEntity);
                    addTriple(triple);
                }
            }
        }

    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<String>, SugiliteRelation) triple
     * @param currentEntity
     * @param string
     * @param relation
     */
    void addEntityStringTriple(SugiliteEntity currentEntity, String string, SugiliteRelation relation){
        //class
        SugiliteEntity<String> objectEntity = null;

        if (stringSugiliteEntityMap.containsKey(string)) {
            objectEntity = stringSugiliteEntityMap.get(string);
        } else {
            //create a new entity for the class name
            SugiliteEntity<String> entity = new SugiliteEntity<>(entityIdCounter++, String.class, string);
            stringSugiliteEntityMap.put(string, entity);
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        addTriple(triple);
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

    public Set<SugiliteTriple> getTriples() {
        return triples;
    }

    public Map<SugiliteRelation, Set<SugiliteTriple>> getPredicateTriplesMap() {
        return predicateTriplesMap;
    }

    public Integer getEntityIdCounter() {
        return entityIdCounter;
    }

    public Map<AccessibilityNodeInfo, SugiliteEntity<AccessibilityNodeInfo>> getAccessibilityNodeInfoSugiliteEntityMap() {
        return accessibilityNodeInfoSugiliteEntityMap;
    }

    public Map<String, SugiliteEntity<String>> getStringSugiliteEntityMap() {
        return stringSugiliteEntityMap;
    }


}
