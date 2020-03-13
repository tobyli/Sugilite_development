package edu.cmu.hcii.sugilite.automation;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;

import static edu.cmu.hcii.sugilite.Const.HOME_SCREEN_PACKAGE_NAMES;

/**
 * @author toby
 * @date 3/20/18
 * @time 1:45 AM
 */
public class AutomatorUtil {
    /**
     * kill a package named packageName
     * @param packageName
     */
    static private Set<String> homeScreenPackageNameSet = new HashSet<>(Arrays.asList(HOME_SCREEN_PACKAGE_NAMES));

    static public void killPackage(String packageName, ActivityManager am){
        //don't kill the home screen
        if(homeScreenPackageNameSet.contains(packageName)) {
            return;
        }

        try {
            Process sh = Runtime.getRuntime().exec("su", null, null);
            OutputStream os = sh.getOutputStream();
            os.write(("am force-stop " + packageName).getBytes("ASCII"));
            os.flush();
            os.close();
            System.out.println("KILLING: " + packageName);
        } catch (Exception e) {
            System.out.println("FAILED TO KILL RELEVANT PACKAGES");
            e.printStackTrace();
            // do nothing, likely this exception is caused by running Sugilite on a non-rooted device
        }

    }

    public static List<AccessibilityNodeInfo> preOrderTraverseSiblings(AccessibilityNodeInfo node){
        if(node == null) return null;
        List<AccessibilityNodeInfo> siblingNodes = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo parent = node.getParent();
        if(parent == null) return siblingNodes;
        // adding parent for now
        siblingNodes.add(parent);
        Rect nodeRect = new Rect();
        Rect compRect = new Rect();
        node.getBoundsInScreen(nodeRect);
        int numSibling = parent.getChildCount();
        for (int i = 0; i < numSibling; i++){
            AccessibilityNodeInfo currSib = parent.getChild(i);
            if(currSib == null) continue;
            currSib.getBoundsInScreen(compRect);
            // checking bounding screen + name for equality
            if(currSib.getClassName().toString().equals(node.getClassName().toString()) &&
                    nodeRect.contains(compRect) && compRect.contains(nodeRect)) continue;
            siblingNodes.add(currSib);
        }

        List<AccessibilityNodeInfo> preOrderTraverseSibNode = new ArrayList<AccessibilityNodeInfo>();
        for (AccessibilityNodeInfo sib : siblingNodes) {
            // add all children of the sibling node
            preOrderTraverseSibNode.addAll(preOrderTraverse(sib));
        }
        return preOrderTraverseSibNode;
    }

    /**
     * traverse a tree from the root, and return all the notes in the tree
     * @param root
     * @return
     */
    public static List<AccessibilityNodeInfo> preOrderTraverse(AccessibilityNodeInfo root){
        if(root == null)
            return null;
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        list.add(root);
        int childCount = root.getChildCount();
        for(int i = 0; i < childCount; i ++){
            AccessibilityNodeInfo node = root.getChild(i);
            if(node != null)
                list.addAll(preOrderTraverse(node));
        }
        return list;
    }

    /**
     * return all nodes from a list of windows
     * @param windows
     * @return
     */
    public static List<AccessibilityNodeInfo> getAllNodesFromWindows(List<AccessibilityWindowInfo> windows){
        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        for(AccessibilityWindowInfo window : windows){
            AccessibilityNodeInfo rootNode = window.getRoot();
            if(rootNode != null) {
                allNodes.addAll(preOrderTraverse(rootNode));
            }
        }
        return allNodes;
    }

    @Deprecated
    private static boolean isChild(AccessibilityNodeInfo child, AccessibilityNodeInfo parent) {
        Rect childBox = new Rect();
        Rect compBox = new Rect();
        child.getBoundsInScreen(childBox);

        int numChildren = parent.getChildCount();
        for(int i = 0; i < numChildren; i++){
            AccessibilityNodeInfo c = parent.getChild(i);
            if(c == null) continue;
            c.getBoundsInScreen(compBox);
            if(child.getClassName().toString().equals(c.getClassName().toString()) &&
                    childBox.contains(compBox) && compBox.contains(childBox)){
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static AccessibilityNodeInfo customGetParent(AccessibilityNodeInfo child) {
        AccessibilityNodeInfo potentialParent = child.getParent();
        if(potentialParent == null) return null;
        if(isChild(child, potentialParent)) return potentialParent;

        // this is the wrong parent :(
        int numChildren = potentialParent.getChildCount();
        for(int i = 0 ; i < numChildren; i++){
            AccessibilityNodeInfo newPotentialParent = potentialParent.getChild(i);
            if(newPotentialParent == null) continue;
            if(isChild(child, newPotentialParent)) return newPotentialParent;
        }
        return null;
    }

    public static List<AccessibilityNodeInfo> getClickableList (List<AccessibilityNodeInfo> nodeInfos){
        List<AccessibilityNodeInfo> retList = new ArrayList<>();
        for(AccessibilityNodeInfo node : nodeInfos){
            if(node.isClickable())
                retList.add(node);
        }
        return retList;
    }

    public static String textVariableParse (String text, Set<String> variableSet, Map<String, Variable> variableValueMap){
        if(variableSet == null || variableValueMap == null)
            return text;
        String currentText = new String(text);
        for(Map.Entry<String, Variable> entry : variableValueMap.entrySet()){
            if(!variableSet.contains(entry.getKey()))
                continue;
            if(entry.getValue() instanceof StringVariable)
                currentText = currentText.replace("@" + entry.getKey(), ((StringVariable) entry.getValue()).getValue());
        }
        return currentText;
    }
}
