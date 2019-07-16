package edu.cmu.hcii.sugilite.ontology;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.automation.AutomatorUtil;
import edu.cmu.hcii.sugilite.ontology.helper.ListOrderResolver;
import edu.cmu.hcii.sugilite.ontology.helper.TextStringParseHelper;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteNodeAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextParentAnnotator;

import static edu.cmu.hcii.sugilite.Const.UI_SNAPSHOT_TEXT_PARSING_THREAD_COUNT;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class UISnapshot {
    private final Set<SugiliteTriple> triples;

    //indexes for triples
    private final Map<Integer, Set<SugiliteTriple>> subjectTriplesMap;
    private final Map<Integer, Set<SugiliteTriple>> objectTriplesMap;
    private final Map<Integer, Set<SugiliteTriple>> predicateTriplesMap;

    private final transient Map<Map.Entry<Integer, Integer>, Set<SugiliteTriple>> subjectPredicateTriplesMap;

    //indexes for entities and relations
    private Map<Integer, SugiliteEntity> sugiliteEntityIdSugiliteEntityMap;
    private Map<Integer, SugiliteRelation> sugiliteRelationIdSugiliteRelationMap;


    private transient int entityIdCounter;
    private final transient Map<Node, SugiliteEntity<Node>> nodeSugiliteEntityMap;
    private final transient Map<String, SugiliteEntity<String>> stringSugiliteEntityMap;
    private final transient Map<Double, SugiliteEntity<Double>> doubleSugiliteEntityMap;
    private final transient Map<Boolean, SugiliteEntity<Boolean>> booleanSugiliteEntityMap;
    private final transient Map<Node, AccessibilityNodeInfo> nodeAccessibilityNodeInfoMap;
    private final transient UISnapshot uiSnapshot;

    private boolean stringEntitiesAreAnnotated = false;

    protected static final String TAG = UISnapshot.class.getSimpleName();


    //construct an empty UI snapshot
    public UISnapshot(){
        //empty
        uiSnapshot = this;
        triples = new HashSet<>();
        subjectTriplesMap = new HashMap<>();
        objectTriplesMap = new HashMap<>();
        predicateTriplesMap = new HashMap<>();
        subjectPredicateTriplesMap = new HashMap<>();
        sugiliteEntityIdSugiliteEntityMap = new HashMap<>();
        sugiliteRelationIdSugiliteRelationMap = new HashMap<>();
        nodeSugiliteEntityMap = new HashMap<>();
        stringSugiliteEntityMap = new HashMap<>();
        doubleSugiliteEntityMap = new HashMap<>();
        booleanSugiliteEntityMap = new HashMap<>();
        nodeAccessibilityNodeInfoMap = new HashMap<>();
        entityIdCounter = 0;
    }

    //construct a UISnapshot for a list of windows
    public UISnapshot(List<AccessibilityWindowInfo> windows, boolean toConstructNodeAccessibilityNodeInfoMap, SugiliteTextParentAnnotator sugiliteTextParentAnnotator, boolean toAnnotateStringEntities, String activityName) {
        this();
        List<Node> allNodes = new ArrayList<>();
        for(AccessibilityWindowInfo window : windows){
            AccessibilityNodeInfo rootNode = window.getRoot();
            if(rootNode != null) {
                allNodes.addAll(preOrderNodeTraverseWithZIndex(rootNode, toConstructNodeAccessibilityNodeInfoMap, window.getLayer(), new ArrayList<>(), activityName));
            }
        }
        long startTime = System.currentTimeMillis();
        constructFromListOfNodes(allNodes, toAnnotateStringEntities);
        long stopTime = System.currentTimeMillis();
        Log.i(TAG, String.format("Constructed from List of %d Nodes! -- Takes %s ms", allNodes.size(), String.valueOf(stopTime - startTime)));
    }

    //construct a UISnapshot from a rootNode
    public UISnapshot(AccessibilityNodeInfo rootNode, boolean toConstructNodeAccessibilityNodeInfoMap, SugiliteTextParentAnnotator sugiliteTextParentAnnotator, boolean toAnnotateStringEntities, String activityName) {
        this();
        List<AccessibilityNodeInfo> allOldNodes = AutomatorUtil.preOrderTraverse(rootNode);
        List<Node> allNodes = new ArrayList<>();
        if(allOldNodes != null) {
            for (AccessibilityNodeInfo oldNode : allOldNodes) {
                Node node = new Node(oldNode, activityName);
                if (node.getPackageName() != null && (node.getPackageName().contains("com.android.systemui") || node.getPackageName().contains("sugilite"))) {
                    continue;
                }
                allNodes.add(node);
                if (toConstructNodeAccessibilityNodeInfoMap) {
                    nodeAccessibilityNodeInfoMap.put(node, oldNode);
                }
            }
        }
        constructFromListOfNodes(allNodes, toAnnotateStringEntities);
    }

    public void annotateStringEntitiesIfNeeded(){
        //create a multi-thread executor for parsing strings
        ExecutorService executor = Executors.newFixedThreadPool(UI_SNAPSHOT_TEXT_PARSING_THREAD_COUNT);
        SugiliteTextParentAnnotator sugiliteTextParentAnnotator = SugiliteTextParentAnnotator.getInstance();

        if (! stringEntitiesAreAnnotated) {
            long startTime = System.currentTimeMillis();
            //parse the string entities
            TextStringParseHelper textStringParseHelper = new TextStringParseHelper(sugiliteTextParentAnnotator);

            //use the tempEntities to avoid concurrentModification in the map
            Set<SugiliteEntity<String>> tempEntities = new HashSet<>();

            for(Map.Entry<String, SugiliteEntity<String>> entry : stringSugiliteEntityMap.entrySet()){
                tempEntities.add(entry.getValue());
            }

            //use Callable and invokeAll() to wait for the finish of all threads
            List<Callable<Boolean>> todos = new ArrayList<>();

            for(SugiliteEntity<String> entity : tempEntities){
                Callable<Boolean> callable = new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        textStringParseHelper.parseAndAddNewRelations(entity, uiSnapshot);
                        return true;
                    }
                };
                todos.add(callable);
            }
            try {
                List<Future<Boolean>> results = executor.invokeAll(todos);
                stringEntitiesAreAnnotated = true;
                long stopTime = System.currentTimeMillis();
                Log.i(TAG, String.format("Parsed strings for %d entities! -- Takes %s ms", results.size(), String.valueOf(stopTime - startTime)));

            } catch (InterruptedException e){
                e.printStackTrace();;
            } finally {
                executor.shutdown();
            }

        }
    }

    /**
     * contruct a UI snapshot from a list of all nodes
     * @param allNodes
     * @param toAnnotateStringEntities
     */
    private void constructFromListOfNodes(List<Node> allNodes, boolean toAnnotateStringEntities){

        if(allNodes != null){
            for(Node node : allNodes) {
                if(node.getPackageName() != null && (node.getPackageName().contains("com.android.systemui") || node.getPackageName().contains("sugilite"))){
                    continue;
                }

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


                if (node.getClassName() != null) {
                    //class
                    String className = node.getClassName();
                    addEntityStringTriple(currentEntity, className, SugiliteRelation.HAS_CLASS_NAME);
                }

                if (node.getText() != null) {
                    //text
                    String text = node.getText();
                    addEntityStringTriple(currentEntity, text, SugiliteRelation.HAS_TEXT);
                }

                if (node.getViewIdResourceName() != null) {
                    //view id
                    String viewId = node.getViewIdResourceName();
                    addEntityStringTriple(currentEntity, viewId, SugiliteRelation.HAS_VIEW_ID);
                }

                if (node.getPackageName() != null) {
                    //package name
                    String packageName = node.getPackageName();
                    addEntityStringTriple(currentEntity, packageName, SugiliteRelation.HAS_PACKAGE_NAME);
                }

                if (node.getActivityName() != null) {
                    //activity name
                    String activityName = node.getActivityName();
                    addEntityStringTriple(currentEntity, activityName, SugiliteRelation.HAS_ACTIVITY_NAME);
                }

                if (node.getContentDescription() != null) {
                    //content description
                    String contentDescription = node.getContentDescription();
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

                //isFocused
                addEntityBooleanTriple(currentEntity, node.getFocused(), SugiliteRelation.IS_FOCUSED);


                //screen location
                addEntityStringTriple(currentEntity, node.getBoundsInScreen(), SugiliteRelation.HAS_SCREEN_LOCATION);

                //parent location
                addEntityStringTriple(currentEntity, node.getBoundsInParent(), SugiliteRelation.HAS_PARENT_LOCATION);


                //*** temporarily disable HAS_PARENT and HAS_CHILD relations ***


                //has_parent relation
                if (node.getParent() != null) {
                    //parent
                    Node parentNode = node.getParent();
                    SugiliteEntity<Node> parentEntity = null;
                    if(nodeSugiliteEntityMap.containsKey(parentNode)) {
                        parentEntity = nodeSugiliteEntityMap.get(parentNode);
                        SugiliteTriple triple1 = new SugiliteTriple(parentEntity, SugiliteRelation.HAS_CHILD, currentEntity);
                        addTriple(triple1);
                        SugiliteTriple triple2 = new SugiliteTriple(currentEntity, SugiliteRelation.HAS_PARENT, parentEntity);
                        addTriple(triple2);
                    }
                    else {
                        SugiliteEntity<Node> newEntity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, parentNode);
                        parentEntity = newEntity;
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

            for(Map.Entry<Node, SugiliteEntity<Node>> entry : nodeSugiliteEntityMap.entrySet()){
                SugiliteEntity currentEntity = entry.getValue();
                // TODO: add order in list info
                ListOrderResolver listOrderResolver = new ListOrderResolver();
                Set<SugiliteTriple> triples = subjectPredicateTriplesMap.get(new AbstractMap.SimpleEntry<>(currentEntity.getEntityId(), SugiliteRelation.HAS_CHILD.getRelationId()));
                Set<Node> childNodes = new HashSet<>();
                if(triples != null) {
                    for (SugiliteTriple triple : triples) {
                        Node child = (Node)triple.getObject().getEntityValue();
                        Rect rect = Rect.unflattenFromString(child.getBoundsInScreen());
                        int size = rect.width() * rect.height();
                        if (size > 0) {
                            childNodes.add(child);
                        }

                    }

                    if (listOrderResolver.isAList(entry.getKey(), childNodes)) {
                        addEntityBooleanTriple(currentEntity, true, SugiliteRelation.IS_A_LIST);
                        addOrderForChildren(childNodes);
                    }
                }
            }

            if (toAnnotateStringEntities) {
                this.annotateStringEntitiesIfNeeded();
            }
            //*** temporarily disable geometric relations ***

            //parse node entities

            /*
            Display display = context.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;

            Set<SugiliteEntity<Node>> nodeEntities = new HashSet<>();

            for(Map.Entry<Node, SugiliteEntity<Node>> entry : nodeSugiliteEntityMap.entrySet()){
                nodeEntities.add(entry.getValue());
            }

            SugiliteNodeAnnotator annotator = SugiliteNodeAnnotator.getInstance();
            for (SugiliteNodeAnnotator.NodeAnnotatingResult res : annotator.annotate(nodeEntities)) {
                this.addEntityNodeTriple(res.getSubjectSet(), res.getObjectSet(), res.getRelation());
            }
            */


        }

    }

    private void addOrderForChildren(Iterable<Node> children){
        //add list order for list items
        List<Map.Entry<Node, Integer>> childNodeYValueList = new ArrayList<>();
        for(Node childNode : children){
            childNodeYValueList.add(new AbstractMap.SimpleEntry<>(childNode, Integer.valueOf(childNode.getBoundsInScreen().split(" ")[1])));
        }
        childNodeYValueList.sort(new Comparator<Map.Entry<Node, Integer>>() {
            @Override
            public int compare(Map.Entry<Node, Integer> o1, Map.Entry<Node, Integer> o2) {
                return o1.getValue() - o2.getValue();
            }
        });
        int counter = 0;
        for(Map.Entry<Node, Integer> entry : childNodeYValueList){
            counter ++;
            Node childNode = entry.getKey();

            //addEntityStringTriple(nodeSugiliteEntityMap.get(childNode), String.valueOf(counter), SugiliteRelation.HAS_LIST_ORDER);
            addEntityNumericTriple(nodeSugiliteEntityMap.get(childNode), Double.valueOf(counter), SugiliteRelation.HAS_LIST_ORDER);

            SugiliteEntity<Node> childEntity = nodeSugiliteEntityMap.get(childNode);
            if(childEntity != null){
                for(SugiliteEntity<Node> entity : getAllChildEntities(childEntity, new HashSet<>())){
                    //addEntityStringTriple(entity, String.valueOf(counter), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER);
                    addEntityNumericTriple(entity, Double.valueOf(counter), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER);
                }
            }
        }
    }


    private Set<SugiliteEntity<Node>> getAllChildEntities(SugiliteEntity<Node> node, Set<SugiliteEntity<Node>> coveredNodes){
        Set<SugiliteEntity<Node>> results = new HashSet<>();
        Set<SugiliteTriple> triples = subjectPredicateTriplesMap.get(new AbstractMap.SimpleEntry<>(node.getEntityId(), SugiliteRelation.HAS_CHILD.getRelationId()));
        if(triples != null) {
            for (SugiliteTriple triple : triples) {
                if (triple.getObject().getEntityValue() instanceof Node && (!results.contains(triple.getObject()))) {
                    if(coveredNodes.contains(triple.getObject())){
                        continue;
                    }
                    results.add(triple.getObject());
                    coveredNodes.add(triple.getObject());
                    results.addAll(getAllChildEntities(triple.getObject(), coveredNodes));
                }
            }
        }
        coveredNodes.addAll(results);
        return results;
    }

    private static String cleanUpString(String string){
        return string.replace("(", "").replace(")", "").replace("\"", "");
    }


    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<String>, SugiliteRelation) triple
     * @param currentEntity
     * @param string
     * @param relation
     */
    public void addEntityStringTriple(SugiliteEntity currentEntity, String string, SugiliteRelation relation){
        //class
        SugiliteEntity<String> objectEntity = null;

        //clean up the string
        string = cleanUpString(string);

        if (stringSugiliteEntityMap.containsKey(string)) {
            objectEntity = stringSugiliteEntityMap.get(string);
        } else {
            //create a new entity for the class name
            SugiliteEntity<String> entity = new SugiliteEntity<>(entityIdCounter++, String.class, string);
            synchronized (stringSugiliteEntityMap) {
                stringSugiliteEntityMap.put(string, entity);
            }
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        triple.setObjectStringValue(string);
        synchronized (this) {
            addTriple(triple);
        }
    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<Double>, SugiliteRelation) triple
     * @param currentEntity
     * @param numeric
     * @param relation
     */
    private void addEntityNumericTriple(SugiliteEntity currentEntity, Double numeric, SugiliteRelation relation){
        //class
        SugiliteEntity<Double> objectEntity = null;

        if (doubleSugiliteEntityMap.containsKey(numeric)) {
            objectEntity = doubleSugiliteEntityMap.get(numeric);
        } else {
            //create a new entity for the class name
            SugiliteEntity<Double> entity = new SugiliteEntity<>(entityIdCounter++, Double.class, numeric);
            doubleSugiliteEntityMap.put(numeric, entity);
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        triple.setObjectStringValue(numeric.toString());
        addTriple(triple);
    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<Boolean>, SugiliteRelation) triple
     * @param currentEntity
     * @param bool
     * @param relation
     */
    private void addEntityBooleanTriple(SugiliteEntity currentEntity, Boolean bool, SugiliteRelation relation){
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
    private void addTriple(SugiliteTriple triple){
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

        if(!subjectPredicateTriplesMap.containsKey(new AbstractMap.SimpleEntry<>(triple.getSubject().getEntityId(), triple.getPredicate().getRelationId()))){
            subjectPredicateTriplesMap.put(new AbstractMap.SimpleEntry<>(triple.getSubject().getEntityId(), triple.getPredicate().getRelationId()), new HashSet<>());
        }

        subjectTriplesMap.get(triple.getSubject().getEntityId()).add(triple);
        predicateTriplesMap.get(triple.getPredicate().getRelationId()).add(triple);
        objectTriplesMap.get(triple.getObject().getEntityId()).add(triple);
        subjectPredicateTriplesMap.get(new AbstractMap.SimpleEntry<>(triple.getSubject().getEntityId(), triple.getPredicate().getRelationId())).add(triple);

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

    public Map<Map.Entry<Integer, Integer>, Set<SugiliteTriple>> getSubjectPredicateTriplesMap() {
        return subjectPredicateTriplesMap;
    }

    /**
     *
     * @param parent parent node
     * @param toConstructNodeAccessibilityNodeInfoMap whether to populate nodeAccessibilityNodeInfoMap while traversing
     * @param windowZIndex the z index of the window
     * @param parentNodeZIndexSequence the z index sequence of the parent node
     * @return
     */
    private List<Node> preOrderNodeTraverseWithZIndex(AccessibilityNodeInfo parent, boolean toConstructNodeAccessibilityNodeInfoMap, Integer windowZIndex, List<Integer> parentNodeZIndexSequence, String activityName){
        //fill in the Z-index for nodes recursively
        if(parent == null) {
            return null;
        }
        List<Node> list = new ArrayList<>();
        Node node = new Node(parent, windowZIndex, parentNodeZIndexSequence, activityName);
        if(toConstructNodeAccessibilityNodeInfoMap){
            nodeAccessibilityNodeInfoMap.put(node, parent);
        }
        list.add(node);
        int childCount = parent.getChildCount();
        for(int i = 0; i < childCount; i ++){
            AccessibilityNodeInfo childNode = parent.getChild(i);
            if(childNode != null) {
                list.addAll(preOrderNodeTraverseWithZIndex(childNode, toConstructNodeAccessibilityNodeInfoMap, windowZIndex, node.getNodeZIndexSequence(), activityName));
            }
        }
        return list;
    }

}
