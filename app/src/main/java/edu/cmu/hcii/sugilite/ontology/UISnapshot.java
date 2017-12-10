package edu.cmu.hcii.sugilite.ontology;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Node;
import edu.cmu.hcii.sugilite.automation.Automator;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class UISnapshot {
    private Set<SugiliteTriple> triples;

    //indexes for triples
    private Map<Integer, Set<SugiliteTriple>> subjectTriplesMap;
    private Map<Integer, Set<SugiliteTriple>> objectTriplesMap;
    private Map<Integer, Set<SugiliteTriple>> predicateTriplesMap;

    //indexes for entities and relations
    private Map<Integer, SugiliteEntity> sugiliteEntityIdSugiliteEntityMap;
    private Map<Integer, SugiliteRelation> sugiliteRelationIdSugiliteRelationMap;


    private transient int entityIdCounter;
    private transient Map<Node, SugiliteEntity<Node>> nodeSugiliteEntityMap;
    private transient Map<String, SugiliteEntity<String>> stringSugiliteEntityMap;
    private transient Map<Boolean, SugiliteEntity<Boolean>> booleanSugiliteEntityMap;
    private transient Map<Node, AccessibilityNodeInfo> nodeAccessibilityNodeInfoMap;


    public UISnapshot(){
        //empty
        triples = new HashSet<>();
        subjectTriplesMap = new HashMap<>();
        objectTriplesMap = new HashMap<>();
        predicateTriplesMap = new HashMap<>();
        sugiliteEntityIdSugiliteEntityMap = new HashMap<>();
        sugiliteRelationIdSugiliteRelationMap = new HashMap<>();
        nodeSugiliteEntityMap = new HashMap<>();
        stringSugiliteEntityMap = new HashMap<>();
        booleanSugiliteEntityMap = new HashMap<>();
        nodeAccessibilityNodeInfoMap = new HashMap<>();
        entityIdCounter = 0;
    }

    public UISnapshot(AccessibilityEvent event){
        //TODO: contruct a UI snapshot from an event
        //get the rootNode from event and pass into to the below function


    }

    public UISnapshot(AccessibilityNodeInfo rootNode, boolean toConstructNodeAccessibilityNodeInfoMap){
        //TODO: contruct a UI snapshot from a rootNode
        this();
        List<AccessibilityNodeInfo> allNodes = Automator.preOrderTraverse(rootNode);
        if(allNodes != null){
            for(AccessibilityNodeInfo oldNode : allNodes) {
                Node node = new Node(oldNode);


                //get the corresponding entity for the node
                SugiliteEntity<Node> currentEntity = null;
                if (nodeSugiliteEntityMap.containsKey(node)) {
                    currentEntity = nodeSugiliteEntityMap.get(node);
                } else {
                    //create a new entity for the node
                    SugiliteEntity<Node> entity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, node);
                    nodeSugiliteEntityMap.put(node, entity);
                    currentEntity = entity;
                }

                //start to construct the relationship
                if(toConstructNodeAccessibilityNodeInfoMap) {
                    nodeAccessibilityNodeInfoMap.put(node, oldNode);
                }

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

                if (node.getViewIdResourceName() != null) {
                    //view id
                    String viewId = node.getViewIdResourceName();
                    addEntityStringTriple(currentEntity, viewId, SugiliteRelation.HAS_VIEW_ID);
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


                //isClickable
                addEntityBooleanTriple(currentEntity, node.getClickable(), SugiliteRelation.IS_CLICKABLE);

                //isEditable
                addEntityBooleanTriple(currentEntity, node.getEditable(), SugiliteRelation.IS_EDITABLE);

                //isScrollable
                addEntityBooleanTriple(currentEntity, node.getScrollable(), SugiliteRelation.IS_SCROLLABLE);

                //isCheckable
                addEntityBooleanTriple(currentEntity, node.getCheckable(), SugiliteRelation.IS_CHECKABLE);

                //isChecked
                addEntityBooleanTriple(currentEntity, node.getChecked(), SugiliteRelation.IS_CHECKED);

                //isSelected
                addEntityBooleanTriple(currentEntity, node.getSelected(), SugiliteRelation.IS_SELECTED);


                //screen location
                addEntityStringTriple(currentEntity, node.getBoundsInScreen(), SugiliteRelation.HAS_SCREEN_LOCATION);

                //parent location
                addEntityStringTriple(currentEntity, node.getBoundsInParent(), SugiliteRelation.HAS_PARENT_LOCATION);

                //has_parent relation
                if (node.getParent() != null) {
                    //parent
                    Node parentNode = node.getParent();
                    if(nodeSugiliteEntityMap.containsKey(parentNode)) {
                        SugiliteTriple triple1 = new SugiliteTriple(nodeSugiliteEntityMap.get(parentNode), SugiliteRelation.HAS_CHILD, currentEntity);
                        addTriple(triple1);
                        SugiliteTriple triple2 = new SugiliteTriple(currentEntity, SugiliteRelation.HAS_PARENT, nodeSugiliteEntityMap.get(parentNode));
                        addTriple(triple2);
                    }
                    else {
                        SugiliteEntity<Node> newEntity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, parentNode);
                        nodeSugiliteEntityMap.put(parentNode, newEntity);
                        SugiliteTriple triple1 = new SugiliteTriple(newEntity, SugiliteRelation.HAS_CHILD, currentEntity);
                        addTriple(triple1);
                        SugiliteTriple triple2 = new SugiliteTriple(currentEntity, SugiliteRelation.HAS_PARENT, newEntity);
                        addTriple(triple2);
                    }
                }

                //has_child_text relation
                if (node.getParent() != null && node.getText() != null){
                    String text = node.getText();
                    Set<Node> parentNodes = new HashSet<>();
                    Node currentParent = node;
                    while(currentParent.getParent() != null){
                        currentParent = currentParent.getParent();
                        parentNodes.add(currentParent);
                    }
                    for(Node parentNode : parentNodes){
                        if(nodeSugiliteEntityMap.containsKey(parentNode)) {
                            addEntityStringTriple(nodeSugiliteEntityMap.get(parentNode), text, SugiliteRelation.HAS_CHILD_TEXT);
                        }
                        else {
                            SugiliteEntity<Node> newEntity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, parentNode);
                            nodeSugiliteEntityMap.put(parentNode, newEntity);
                            addEntityStringTriple(newEntity, text, SugiliteRelation.HAS_CHILD_TEXT);
                        }
                    }
                }

                // TODO: add sibling text info
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
        triple.setObjectStringValue(string);
        addTriple(triple);
    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<Boolean>, SugiliteRelation) triple
     * @param currentEntity
     * @param bool
     * @param relation
     */
    void addEntityBooleanTriple(SugiliteEntity currentEntity, Boolean bool, SugiliteRelation relation){
        //class
        SugiliteEntity<Boolean> objectEntity = null;

        if (booleanSugiliteEntityMap.containsKey(bool)) {
            objectEntity = booleanSugiliteEntityMap.get(bool);
        } else {
            //create a new entity for the class name
            SugiliteEntity<Boolean> entity = new SugiliteEntity<>(entityIdCounter++, Boolean.class, bool);
            booleanSugiliteEntityMap.put(bool, entity);
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        triple.setObjectStringValue(bool.toString());
        addTriple(triple);
    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<Node>, SugiliteRelation) triple
     * @param currentEntity
     * @param node
     * @param relation
     */
    void addEntityNodeTriple(SugiliteEntity currentEntity, Node node, SugiliteRelation relation){
        //class
        SugiliteEntity<Node> objectEntity = null;

        if (nodeSugiliteEntityMap.containsKey(node)) {
            objectEntity = nodeSugiliteEntityMap.get(node);
        } else {
            //create a new entity for the class name
            SugiliteEntity<Node> entity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, node);
            nodeSugiliteEntityMap.put(node, entity);
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
        if(!subjectTriplesMap.containsKey(triple.getSubject().getEntityId())){
            subjectTriplesMap.put(triple.getSubject().getEntityId(), new HashSet<>());
        }
        if(!predicateTriplesMap.containsKey(triple.getPredicate().getRelationId())){
            predicateTriplesMap.put(triple.getPredicate().getRelationId(), new HashSet<>());
        }
        if(!objectTriplesMap.containsKey(triple.getObject().getEntityId())){
            objectTriplesMap.put(triple.getObject().getEntityId(), new HashSet<>());
        }

        subjectTriplesMap.get(triple.getSubject().getEntityId()).add(triple);
        predicateTriplesMap.get(triple.getPredicate().getRelationId()).add(triple);
        objectTriplesMap.get(triple.getObject().getEntityId()).add(triple);


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

        if(subjectTriplesMap.get(triple.getSubject().getEntityId()) != null){
            subjectTriplesMap.get(triple.getSubject().getEntityId()).remove(triple);
        }

        if(predicateTriplesMap.get(triple.getPredicate().getRelationId()) != null){
            predicateTriplesMap.get(triple.getPredicate().getRelationId()).remove(triple);
        }

        if(objectTriplesMap.get(triple.getObject().getEntityId()) != null){
            objectTriplesMap.get(triple.getObject().getEntityId()).remove(triple);
        }

    }

    public Map<Integer, SugiliteEntity> getSugiliteEntityIdSugiliteEntityMap() {
        return sugiliteEntityIdSugiliteEntityMap;
    }

    public Map<Integer, SugiliteRelation> getSugiliteRelationIdSugiliteRelationMap() {
        return sugiliteRelationIdSugiliteRelationMap;
    }

    public Map<Integer, Set<SugiliteTriple>> getSubjectTriplesMap() {
        return subjectTriplesMap;
    }

    public Map<Integer, Set<SugiliteTriple>> getObjectTriplesMap() {
        return objectTriplesMap;
    }

    public Set<SugiliteTriple> getTriples() {
        return triples;
    }

    public Map<Integer, Set<SugiliteTriple>> getPredicateTriplesMap() {
        return predicateTriplesMap;
    }

    public Integer getEntityIdCounter() {
        return entityIdCounter;
    }

    public Map<Node, SugiliteEntity<Node>> getNodeSugiliteEntityMap() {
        return nodeSugiliteEntityMap;
    }

    public Map<String, SugiliteEntity<String>> getStringSugiliteEntityMap() {
        return stringSugiliteEntityMap;
    }

    public Map<Boolean, SugiliteEntity<Boolean>> getBooleanSugiliteEntityMap() {
        return booleanSugiliteEntityMap;
    }

    public Map<Node, AccessibilityNodeInfo> getNodeAccessibilityNodeInfoMap() {
        return nodeAccessibilityNodeInfoMap;
    }


}
