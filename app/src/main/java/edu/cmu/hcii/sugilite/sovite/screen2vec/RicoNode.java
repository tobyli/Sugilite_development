package edu.cmu.hcii.sugilite.sovite.screen2vec;

import android.graphics.Rect;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;
import edu.cmu.hcii.sugilite.ontology.SugiliteTriple;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;

/**
 * @author toby
 * @date 8/18/20
 * @time 10:13 PM
 */
public class RicoNode implements Serializable {


    boolean clickable;
    boolean long_clickable;
    boolean enabled;
    boolean focused;
    boolean selected;

    List<Integer> bounds;
    List<Integer> rel_bounds;
    List<String> content_desc;
    List<String> ancestors;

    String className;
    String text;
    String resource_id;
    String packageName;
    List<RicoNode> children;

    //not used
    boolean scrollable_horizontal;
    boolean scrollable_vertical;
    boolean draw;
    boolean focusable;
    boolean adapter_view;
    boolean abs_pos;
    boolean visible_to_user;

    String pressed;
    String visibility;
    String pointer;


    /**
     * create a RicoNode from a SugiliteEntity<Node> and the corresponding UISnapshot
     * @param nodeEntity
     * @param uiSnapshot
     * @return
     */
    public static RicoNode fromSugiliteNode (SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot, Set<Integer> processedNodes) {
        processedNodes.add(nodeEntity.getEntityId());

        RicoNode ricoNode = new RicoNode();
        Node node = nodeEntity.getEntityValue();
        ricoNode.clickable = node.getClickable();
        ricoNode.focused = node.getFocused();
        ricoNode.long_clickable = node.getLongClickable();
        ricoNode.enabled = node.getEnabled();
        ricoNode.selected = node.getSelected();

        Rect boundsInScreenRect = Rect.unflattenFromString(node.getBoundsInScreen());
        ricoNode.bounds = new ArrayList<>();
        ricoNode.bounds.add(boundsInScreenRect.left);
        ricoNode.bounds.add(boundsInScreenRect.top);
        ricoNode.bounds.add(boundsInScreenRect.right);
        ricoNode.bounds.add(boundsInScreenRect.bottom);

        Rect boundsInParentRect = Rect.unflattenFromString(node.getBoundsInParent());
        ricoNode.rel_bounds = new ArrayList<>();
        ricoNode.rel_bounds.add(boundsInParentRect.left);
        ricoNode.rel_bounds.add(boundsInParentRect.top);
        ricoNode.rel_bounds.add(boundsInParentRect.right);
        ricoNode.rel_bounds.add(boundsInParentRect.bottom);

        ricoNode.content_desc = new ArrayList<>();
        if (node.getContentDescription() != null) {
            ricoNode.content_desc.add(node.getContentDescription());
        }
        ricoNode.ancestors = new ArrayList<>();
        ricoNode.className = node.getClassName();
        if (node.getText() != null) {
            ricoNode.text = node.getText();
        }
        if (node.getViewId() != null) {
            ricoNode.resource_id = node.getViewId();
        }
        if (node.getPackageName() != null) {
            ricoNode.packageName = node.getPackageName();
        }
        ricoNode.children = new ArrayList<>();

        //get all the children
        Set<SugiliteSerializableTriple> tripleSet = uiSnapshot.getObjectTriplesMap().get("@" + String.valueOf(nodeEntity.getEntityId()));

        for (SugiliteSerializableTriple triple : tripleSet) {
            if (triple.getPredicateStringValue().equals(SugiliteRelation.HAS_PARENT.getRelationName())) {
                SugiliteSerializableEntity<Node> childNodeEntity = uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().get(triple.getSubjectId());
                if (! processedNodes.contains(childNodeEntity.getEntityId())) {
                    ricoNode.children.add(RicoNode.fromSugiliteNode(childNodeEntity, uiSnapshot, processedNodes));
                } else {
                    Log.i(RicoNode.class.getName(), String.format("Found a loop at node %d!", nodeEntity.getEntityId()));
                }
            }
        }
        return ricoNode;
    }
}
