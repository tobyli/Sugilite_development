package edu.cmu.hcii.sugilite.source_parsing;

import java.util.ArrayList;
import java.util.List;

/**
 * @author toby
 * @date 3/18/18
 * @time 11:20 PM
 */

/**
 * data structure to represent a node on an AST
 */
public class SugiliteScriptNode {
    SugiliteScriptNode parent;
    List<SugiliteScriptNode> children;
    String value;
    String scriptContent;

    public SugiliteScriptNode(){
        children = new ArrayList<>();
        scriptContent = "";
    }

    public void setParent(SugiliteScriptNode parent) {
        this.parent = parent;
    }

    public SugiliteScriptNode getParent() {
        return parent;
    }

    public void addChild(SugiliteScriptNode child){
        children.add(child);
    }

    public List<SugiliteScriptNode> getChildren() {
        return children;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent.trim();
    }

    public String getScriptContent() {
        return scriptContent;
    }
}
