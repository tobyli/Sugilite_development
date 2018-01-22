package edu.cmu.hcii.sugilite.ontology.helper;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Node;

/**
 * @author toby
 * @date 1/15/18
 * @time 11:18 AM
 */
public class ListOrderResolver {

    public boolean isAList(Node node, Iterable<Node> children){
        String className = node.getClassName();
        Set<String> listClassNames = new HashSet<>();
        if(isOfListClass(className)){
            //the node is of a class name of lists
            int count = 0;
            for(Node childNode : children){
                count++;
                listClassNames.add(childNode.getClassName());
            }
            if(count > 1 && listClassNames.size() == 1){
                //System.out.println("Found list: " + node.getBoundsInScreen() + " " + node.getClassName() + " hasSize " + count);
                return true;
            }
        }
        return false;
    }


    @Deprecated
    public int getListOrder(Node node, Iterable<Node> children){
        //assume vertical list
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
            if(entry.getKey().getBoundsInScreen().equals(node.getBoundsInScreen())){
                return counter;
            }
        }
        return -1;
    }

    private boolean isOfListClass(String className){
        return (className.toLowerCase().contains("list") ||
                className.toLowerCase().contains("recycler") ||
                className.toLowerCase().contains("scroll") ||
                className.toLowerCase().contains("gridview") ||
                className.toLowerCase().contains("tabwidget"));
    }
}
