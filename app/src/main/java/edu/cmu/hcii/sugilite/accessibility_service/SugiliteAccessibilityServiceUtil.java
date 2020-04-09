package edu.cmu.hcii.sugilite.accessibility_service;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.automation.AutomatorUtil;
import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.util.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;

/**
 * @author toby
 * @date 2/22/19
 * @time 11:28 PM
 */
class SugiliteAccessibilityServiceUtil {

    static SugiliteAvailableFeaturePack generateFeaturePack(AccessibilityEvent event, AccessibilityNodeInfo sourceNode, AccessibilityNodeInfo rootNode, File screenshot, HashSet<SerializableNodeInfo> availableAlternativeNodes, List<AccessibilityNodeInfo> preOrderTraverseSourceNode, List<AccessibilityNodeInfo> preOderTraverseRootNode, List<AccessibilityNodeInfo> preOrderTraverseSibNode, SerializableUISnapshot uiSnapshot) {
        SugiliteAvailableFeaturePack featurePack = new SugiliteAvailableFeaturePack();
        Rect boundsInParents = new Rect();
        Rect boundsInScreen = new Rect();
        AccessibilityNodeInfo parentNode = null;
        if (sourceNode != null) {
            sourceNode.getBoundsInParent(boundsInParents);
            sourceNode.getBoundsInScreen(boundsInScreen);
            parentNode = sourceNode.getParent();
        }

        //NOTE: NOT ONLY COUNTING THE IMMEDIATE CHILDREN NOW
        ArrayList<AccessibilityNodeInfo> childrenNodes = null;
        ArrayList<AccessibilityNodeInfo> siblingNodes = null;
        if (sourceNode != null && preOrderTraverseSourceNode != null) {
            childrenNodes = new ArrayList<>(preOrderTraverseSourceNode);
        } else {
            childrenNodes = new ArrayList<>();
        }
        if (sourceNode != null && preOrderTraverseSibNode != null) {
            siblingNodes = new ArrayList<>(preOrderTraverseSibNode);
        } else {
            siblingNodes = new ArrayList<>();
        }
        ArrayList<AccessibilityNodeInfo> allNodes = null;
        if (rootNode != null && preOderTraverseRootNode != null) {
            allNodes = new ArrayList<>(preOderTraverseRootNode);
        } else {
            allNodes = new ArrayList<>();
        }
        //TODO:AccessibilityNodeInfo is not serializable

        if (sourceNode == null || sourceNode.getPackageName() == null) {
            featurePack.packageName = "NULL";
        } else {
            featurePack.packageName = sourceNode.getPackageName().toString();
        }

        if (sourceNode == null || sourceNode.getClassName() == null) {
            featurePack.className = "NULL";
        } else {
            featurePack.className = sourceNode.getClassName().toString();
        }

        if (sourceNode == null || sourceNode.getText() == null) {
            featurePack.text = "NULL";
        } else {
            featurePack.text = sourceNode.getText().toString();
        }

        if (sourceNode == null || sourceNode.getContentDescription() == null) {
            featurePack.contentDescription = "NULL";
        } else {
            featurePack.contentDescription = sourceNode.getContentDescription().toString();
        }

        if (sourceNode == null || sourceNode.getViewIdResourceName() == null) {
            featurePack.viewId = "NULL";
        } else {
            featurePack.viewId = sourceNode.getViewIdResourceName();
        }

        featurePack.boundsInParent = boundsInParents.flattenToString();
        featurePack.boundsInScreen = boundsInScreen.flattenToString();
        featurePack.time = Calendar.getInstance().getTimeInMillis();
        featurePack.eventType = event.getEventType();
        featurePack.parentNode = new SerializableNodeInfo(parentNode);
        featurePack.childNodes = new AccessibilityNodeInfoList(childrenNodes).getSerializableList();
        // TODO: if it's slow, then use serializednode instead
        featurePack.siblingNodes = new AccessibilityNodeInfoList(siblingNodes).getSerializableList();
        featurePack.allNodes = new AccessibilityNodeInfoList(allNodes).getSerializableList();
        if (sourceNode != null) {
            featurePack.isEditable = sourceNode.isEditable();
        } else {
            featurePack.isEditable = false;
        }
        featurePack.screenshot = screenshot;
        if (availableAlternativeNodes != null) {
            featurePack.alternativeNodes = new HashSet<>(availableAlternativeNodes);
        } else {
            featurePack.alternativeNodes = new HashSet<>();
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            if (event.getBeforeText() == null) {
                featurePack.beforeText = "NULL";
            } else {
                featurePack.beforeText = event.getBeforeText().toString();
            }
        }

        featurePack.childTexts = new ArrayList<>();
        if (featurePack.childNodes != null) {
            for (SerializableNodeInfo node : featurePack.childNodes) {
                featurePack.childTexts.add(node.text);
            }
        }

        featurePack.serializableUISnapshot = uiSnapshot;
        for (SugiliteSerializableEntity<Node> node : uiSnapshot.getAccessibilityNodeInfoSugiliteEntityMap().values()) {
            if (node.getEntityValue() instanceof Node &&
                    node.getEntityValue().getClassName().equals(featurePack.className) &&
                    node.getEntityValue().getBoundsInScreen().equals(featurePack.boundsInScreen)) {
                featurePack.targetNodeEntity = node;
            }
        }

        return featurePack;
    }

    static List<AccessibilityNodeInfo> getAllNodesWithText(AccessibilityNodeInfo rootNode, List<AccessibilityNodeInfo> preOderTraverseRootNode) {
        List<AccessibilityNodeInfo> retList = new ArrayList<>();
        if (preOderTraverseRootNode == null) {
            return retList;
        }
        for (AccessibilityNodeInfo node : preOderTraverseRootNode) {
            if (node.getText() != null) {
                retList.add(node);
            }
        }
        return retList;
    }

    static HashSet<Map.Entry<String, String>> getAlternativeLabels(AccessibilityNodeInfo sourceNode, AccessibilityNodeInfo rootNode, List<AccessibilityNodeInfo> preOderTraverseRootNode, Set<String> exceptedPackages) {
        HashSet<Map.Entry<String, String>> retMap = new HashSet<>();
        if (preOderTraverseRootNode == null) {
            return retMap;
        }
        for (AccessibilityNodeInfo node : preOderTraverseRootNode) {
            if (exceptedPackages.contains(node.getPackageName().toString())) {
                continue;
            }
            if (!node.isClickable()) {
                continue;
            }
            if (!(sourceNode == null || (sourceNode.getClassName() == null && node.getClassName() == null) || (sourceNode.getClassName() != null && node.getClassName() != null && sourceNode.getClassName().toString().contentEquals(node.getClassName())))) {
                continue;
            }
            if (node.getText() != null) {
                retMap.add(new AbstractMap.SimpleEntry<>("Text", node.getText().toString()));
            }
            if (node.getContentDescription() != null) {
                retMap.add(new AbstractMap.SimpleEntry<>("ContentDescription", node.getContentDescription().toString()));
            }
            List<AccessibilityNodeInfo> childNodes = AutomatorUtil.preOrderTraverse(node);
            if (childNodes == null) {
                continue;
            }
            for (AccessibilityNodeInfo childNode : childNodes) {
                if (childNode == null) {
                    continue;
                }
                if (childNode.getText() != null) {
                    retMap.add(new AbstractMap.SimpleEntry<>("Child Text", childNode.getText().toString()));
                }
                if (childNode.getContentDescription() != null) {
                    retMap.add(new AbstractMap.SimpleEntry<>("Child ContentDescription", childNode.getContentDescription().toString()));
                }
            }
        }
        return retMap;
    }

    /**
     * get alternative nodes: anything that is clickable, of the same class type as the source node
     * and not in the excepted packages
     *
     * @param sourceNode
     * @param rootNode
     * @return
     */
    static HashSet<SerializableNodeInfo> getAvailableAlternativeNodes(AccessibilityNodeInfo sourceNode, AccessibilityNodeInfo rootNode, List<AccessibilityNodeInfo> preOderTraverseRootNode, Set<String> exceptedPackages) {
        HashSet<SerializableNodeInfo> retSet = new HashSet<>();
        if (preOderTraverseRootNode == null) {
            return retSet;
        }
        for (AccessibilityNodeInfo node : preOderTraverseRootNode) {
            if (exceptedPackages.contains(node.getPackageName().toString())) {
                continue;
            }
            if (sourceNode != null &&
                    node.getClassName() != null &&
                    sourceNode.getClassName() != null &&
                    (!sourceNode.getClassName().toString().equals(node.getClassName().toString()))) {
                continue;
            }
            if (!node.isClickable()) {
                continue;
            }
            SerializableNodeInfo nodeToAdd = new SerializableNodeInfo(node);
            retSet.add(nodeToAdd);
        }
        return retSet;
    }
}
