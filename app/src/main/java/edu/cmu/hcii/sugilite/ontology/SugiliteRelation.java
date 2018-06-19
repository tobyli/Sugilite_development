package edu.cmu.hcii.sugilite.ontology;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class SugiliteRelation implements Serializable {
    private static final long serialVersionUID = 1963030960748924140L;

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
    public static final SugiliteRelation HAS_TEXT = new SugiliteRelation(1, "hasText");
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

    //TODO: "has text label" relation that combines "has_text", "has_child_text" and "has_sibling_text"

    //added
    public static final SugiliteRelation IS_EDITABLE = new SugiliteRelation(12, "IS_EDITABLE");
    public static final SugiliteRelation IS_CLICKABLE = new SugiliteRelation(13, "IS_CLICKABLE");
    public static final SugiliteRelation IS_SCROLLABLE = new SugiliteRelation(14, "IS_SCROLLABLE");
    public static final SugiliteRelation IS_CHECKABLE = new SugiliteRelation(15, "IS_CHECKABLE");
    public static final SugiliteRelation IS_CHECKED = new SugiliteRelation(16, "IS_CHECKED");
    public static final SugiliteRelation IS_SELECTED = new SugiliteRelation(17, "IS_SELECTED");

    //adv
    public static final SugiliteRelation HAS_LIST_ORDER = new SugiliteRelation(18, "numeric_index");
    public static final SugiliteRelation HAS_PARENT_WITH_LIST_ORDER = new SugiliteRelation(19, "numeric_parent_index");
    public static final SugiliteRelation IS_A_LIST = new SugiliteRelation(20, "IS_A_LIST");

    //text parsing
    public static final SugiliteRelation CONTAINS_EMAIL_ADDRESS = new SugiliteRelation(21, "CONTAINS_EMAIL_ADDRESS");
    public static final SugiliteRelation CONTAINS_PHONE_NUMBER = new SugiliteRelation(22, "CONTAINS_PHONE_NUMBER");

    public static final SugiliteRelation CONTAINS_MONEY = new SugiliteRelation(23, "numeric_price");
    public static final SugiliteRelation CONTAINS_TIME = new SugiliteRelation(24, "numeric_time");
    public static final SugiliteRelation CONTAINS_DATE = new SugiliteRelation(25, "numeric_date");
    public static final SugiliteRelation CONTAINS_DURATION = new SugiliteRelation(26, "numeric_duration");
    public static final SugiliteRelation CONTAINS_LENGTH = new SugiliteRelation(27, "numeric_length");
    public static final SugiliteRelation CONTAINS_PERCENTAGE = new SugiliteRelation(28, "numeric_percentage");
    public static final SugiliteRelation CONTAINS_VOLUME = new SugiliteRelation(29, "numeric_volume");
    public static final SugiliteRelation CONTAINS_NUMBER = new SugiliteRelation(30, "numeric_number");
    public static final SugiliteRelation CONTAINS_TEMPERATURE = new SugiliteRelation(39,"numeric_temperature");///added later so relationID higher



    //spatial relations
    public static final SugiliteRelation CONTAINS = new SugiliteRelation(31, "CONTAINS");
    public static final SugiliteRelation RIGHT = new SugiliteRelation(32, "RIGHT");
    public static final SugiliteRelation LEFT = new SugiliteRelation(33, "LEFT");
    public static final SugiliteRelation ABOVE = new SugiliteRelation(34, "ABOVE");
    public static final SugiliteRelation BELOW = new SugiliteRelation(35, "BELOW");
    public static final SugiliteRelation NEAR = new SugiliteRelation(36, "NEAR");
    public static final SugiliteRelation NEXT_TO = new SugiliteRelation(37, "NEXT_TO");

    //Deprecated
    public static final SugiliteRelation IS = new SugiliteRelation(38, "is");



    public static final Map<String, SugiliteRelation> stringRelationMap;
    static {
        stringRelationMap = new HashMap<String, SugiliteRelation>();
        stringRelationMap.put("HAS_CLASS_NAME", HAS_CLASS_NAME);
        stringRelationMap.put("hasText", HAS_TEXT);
        stringRelationMap.put("HAS_PARENT", HAS_PARENT);
        stringRelationMap.put("HAS_VIEW_ID", HAS_VIEW_ID);
        stringRelationMap.put("HAS_CONTENT_DESCRIPTION", HAS_CONTENT_DESCRIPTION);
        stringRelationMap.put("HAS_SCREEN_LOCATION", HAS_SCREEN_LOCATION);
        stringRelationMap.put("HAS_PARENT_LOCATION", HAS_PARENT_LOCATION);
        stringRelationMap.put("HAS_PACKAGE_NAME", HAS_PACKAGE_NAME);
        stringRelationMap.put("HAS_CHILD", HAS_CHILD);

        stringRelationMap.put("HAS_CHILD_TEXT", HAS_CHILD_TEXT);
        stringRelationMap.put("HAS_SIBLING_TEXT", HAS_SIBLING_TEXT);
        stringRelationMap.put("HAS_SIBLING", HAS_SIBLING);

        stringRelationMap.put("IS_EDITABLE", IS_EDITABLE);
        stringRelationMap.put("IS_CLICKABLE", IS_CLICKABLE);
        stringRelationMap.put("IS_SCROLLABLE", IS_SCROLLABLE);
        stringRelationMap.put("IS_CHECKABLE", IS_CHECKABLE);
        stringRelationMap.put("IS_CHECKED", IS_CHECKED);
        stringRelationMap.put("IS_SELECTED", IS_SELECTED);

        stringRelationMap.put("numeric_index", HAS_LIST_ORDER);
        stringRelationMap.put("numeric_parent_index", HAS_PARENT_WITH_LIST_ORDER);
        stringRelationMap.put("IS_A_LIST", IS_A_LIST);

        stringRelationMap.put("CONTAINS_EMAIL_ADDRESS", CONTAINS_EMAIL_ADDRESS);
        stringRelationMap.put("CONTAINS_PHONE_NUMBER", CONTAINS_PHONE_NUMBER);

        stringRelationMap.put("numeric_price", CONTAINS_MONEY);
        stringRelationMap.put("numeric_time", CONTAINS_TIME);
        stringRelationMap.put("numeric_date", CONTAINS_DATE);
        stringRelationMap.put("numeric_duration", CONTAINS_DURATION);
        stringRelationMap.put("numeric_length", CONTAINS_LENGTH);
        stringRelationMap.put("numeric_percentage", CONTAINS_PERCENTAGE);
        stringRelationMap.put("numeric_volume", CONTAINS_VOLUME);
        stringRelationMap.put("numeric_number", CONTAINS_NUMBER);
        stringRelationMap.put("numeric_temperature", CONTAINS_TEMPERATURE);///


        stringRelationMap.put("CONTAINS", CONTAINS);
        stringRelationMap.put("RIGHT", RIGHT);
        stringRelationMap.put("LEFT", LEFT);
        stringRelationMap.put("ABOVE", ABOVE);
        stringRelationMap.put("BELOW", BELOW);
        stringRelationMap.put("NEAR", NEAR);
        stringRelationMap.put("NEXT_TO", NEXT_TO);

        stringRelationMap.put("is", IS);
    }

    @Override
    public String toString() {
        return relationName;
    }
}
