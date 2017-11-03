package edu.cmu.hcii.sugilite.ontology;

import java.util.Objects;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class SugiliteRelation {
    private Integer relationId;
    private String relationName;

    public SugiliteRelation(Integer relationId, String relationName){
        this.relationId = relationId;
        this.relationName = relationName;
        //TODO: initiate a unique relationId
    }

    public Integer getRelationId() {
        return relationId;
    }

    public String getRelationName() {
        return relationName;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj instanceof SugiliteRelation){
            return ((SugiliteRelation) obj).relationId.equals(this.relationId);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.relationId);
    }

    //examples of relations
    public static final SugiliteRelation HAS_CLASS_NAME = new SugiliteRelation(0, "HAS_CLASS_NAME");
    public static final SugiliteRelation HAS_TEXT = new SugiliteRelation(1, "HAS_TEXT");
    public static final SugiliteRelation HAS_PARENT = new SugiliteRelation(2, "HAS_PARENT");
    public static final SugiliteRelation HAS_VIEW_ID = new SugiliteRelation(3, "HAS_VIEW_ID");
    public static final SugiliteRelation HAS_CONTENT_DESCRIPTION = new SugiliteRelation(4, "HAS_CONTENT_DESCRIPTION");
    public static final SugiliteRelation HAS_SCREEN_LOCATION = new SugiliteRelation(5, "HAS_SCREEN_LOCATION");
    public static final SugiliteRelation HAS_PARENT_LOCATION = new SugiliteRelation(6, "HAS_PARENT_LOCATION");
    public static final SugiliteRelation HAS_PACKAGE_NAME = new SugiliteRelation(7, "HAS_PACKAGE_NAME");
    public static final SugiliteRelation HAS_CHILD = new SugiliteRelation(8, "HAS_CHILD");


    //TODO: new relations to add for flattening the ontology
    public static final SugiliteRelation HAS_CHILD_TEXT = new SugiliteRelation(9, "HAS_CHILD_TEXT");
    public static final SugiliteRelation HAS_SIBLING_TEXT = new SugiliteRelation(11, "HAS_SIBLING_TEXT");
    public static final SugiliteRelation HAS_SIBLING = new SugiliteRelation(10, "HAS_SIBLING");

    //added
    public static final SugiliteRelation IS_EDITABLE = new SugiliteRelation(12, "IS_EDITABLE");
    public static final SugiliteRelation IS_CLICKABLE = new SugiliteRelation(13, "IS_CLICKABLE");
    public static final SugiliteRelation IS_SCROLLABLE = new SugiliteRelation(14, "IS_SCROLLABLE");
    public static final SugiliteRelation IS_CHECKABLE = new SugiliteRelation(15, "IS_CHECKABLE");
    public static final SugiliteRelation IS_CHECKED = new SugiliteRelation(16, "IS_CHECKED");
    public static final SugiliteRelation IS_SELECTED = new SugiliteRelation(17, "IS_SELECTED");



}
