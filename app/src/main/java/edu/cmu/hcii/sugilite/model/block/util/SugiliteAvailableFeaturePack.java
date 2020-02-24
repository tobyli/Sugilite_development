package edu.cmu.hcii.sugilite.model.block.util;

import android.view.accessibility.AccessibilityEvent;

import java.io.File;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteTriple;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;

/**
 * Created by toby on 6/28/16.
 */


/**
 * this class is used for storing a serializable copy of all the features extracted from an AccessibilityEvent
 */
public class SugiliteAvailableFeaturePack implements Serializable{
    public SugiliteAvailableFeaturePack(){
        //do nothing
    }

    public SugiliteAvailableFeaturePack(SugiliteEntity<Node> nodeEntity, UISnapshot uiSnapshot, File screenshot){
        Node node = nodeEntity.getEntityValue();
        if(node.getPackageName() != null) {
            this.packageName = new String(node.getPackageName());
        }
        if(node.getClassName() != null) {
            this.className = new String(node.getClassName());
        }
        if(node.getText() != null) {
            this.text = new String(node.getText());
        }
        if(node.getContentDescription() != null) {
            this.contentDescription = new String(node.getContentDescription());
        }
        if(node.getViewId() != null) {
            this.viewId = new String(node.getViewId());
        }
        if(node.getBoundsInParent() != null) {
            this.boundsInParent = new String(node.getBoundsInParent());
        }
        if(node.getBoundsInScreen() != null) {
            this.boundsInScreen = new String(node.getBoundsInScreen());
        }
        this.isEditable = node.getEditable();
        //TODO: fix timestamp
        this.time = -1;
        this.eventType = AccessibilityEvent.TYPE_VIEW_CLICKED;
        this.screenshot = screenshot;

        this.parentNode = null;
        this.childNodes = new ArrayList<>();
        this.allNodes = new ArrayList<>();
        this.alternativeChildTextList = new HashSet<>();
        this.alternativeTextList = new HashSet<>();

        this.serializableUISnapshot = new SerializableUISnapshot(uiSnapshot);
        this.targetNodeEntity = new SugiliteSerializableEntity<>(nodeEntity);

        this.childTexts = new ArrayList<>();
        if(uiSnapshot.getNodeSugiliteEntityMap().containsKey(node)) {
            Integer subjectId = uiSnapshot.getNodeSugiliteEntityMap().get(node).getEntityId();
            Set<SugiliteTriple> triples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(subjectId, SugiliteRelation.HAS_CHILD_TEXT.getRelationId()));
            if(triples != null){
                for(SugiliteTriple triple : triples){
                    if(triple.getObject() != null && triple.getObject().getEntityValue() instanceof String){
                        childTexts.add((String)triple.getObject().getEntityValue());
                    }
                }
            }
        }

    }

    public SugiliteAvailableFeaturePack(SugiliteAvailableFeaturePack featurePack){
        this.packageName = new String(featurePack.packageName);
        this.className = new String(featurePack.className);
        this.text = new String(featurePack.text);
        this.contentDescription = new String(featurePack.contentDescription);
        this.viewId = new String(featurePack.viewId);
        this.boundsInParent = new String(featurePack.boundsInParent);
        this.boundsInScreen = new String(featurePack.boundsInScreen);
        this.isEditable = featurePack.isEditable;
        this.time = featurePack.time;
        this.eventType = featurePack.eventType;
        this.screenshot = featurePack.screenshot;
        this.serializableUISnapshot = featurePack.serializableUISnapshot;
        this.targetNodeEntity = featurePack.targetNodeEntity;

        if(Const.KEEP_ALL_NODES_IN_THE_FEATURE_PACK) {
            this.parentNode = featurePack.parentNode;
            this.childNodes = new ArrayList<>(featurePack.childNodes);
            this.allNodes = new ArrayList<>(featurePack.allNodes);
            this.childTexts = new ArrayList<>(featurePack.childTexts);
        }
        else{
            this.parentNode = null;
            this.childNodes = new ArrayList<>();
            this.allNodes = new ArrayList<>();
        }

        if(Const.KEEP_ALL_TEXT_LABEL_LIST) {
            if (featurePack.alternativeChildTextList != null)
                this.alternativeChildTextList = new HashSet<>(featurePack.alternativeChildTextList);
            else
                this.alternativeChildTextList = new HashSet<>();
            if (featurePack.alternativeTextList != null)
                this.alternativeTextList = new HashSet<>(featurePack.alternativeTextList);
            else
                this.alternativeTextList = new HashSet<>();
        }
        else{
            this.alternativeChildTextList = new HashSet<>();
            this.alternativeTextList = new HashSet<>();
        }
    }
    public String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    public boolean isEditable;
    public long time;
    public int eventType;
    public File screenshot;
    public SerializableNodeInfo parentNode;
    /**
     * allNodes: all nodes present (from traversing the root view)
     * childNodes: all child nodes of the source nodes (from traversing the source node)
     * siblingNodes: all sibling nodes of the source node and their children
     */
    public ArrayList<SerializableNodeInfo> childNodes, allNodes, siblingNodes;
    public List<String> childTexts;


    /**
     * from SugiliteAccessibilityService.getAvailableAlternativeNodes()
     */
    public Set<SerializableNodeInfo> alternativeNodes;

    public Set<String> alternativeTextList;
    public Set<String> alternativeChildTextList;

    //for VIEW_TEXT_CHANGED events only
    public String beforeText, afterText;

    public SerializableUISnapshot serializableUISnapshot;
    public SugiliteSerializableEntity<Node> targetNodeEntity;
}
