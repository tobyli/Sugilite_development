package edu.cmu.hcii.sugilite.ontology;


import java.util.*;

/**
 * Created by Wanling Ding on 01/02/2018.
 */

public class DescriptionGenerator {

    public static final HashMap<SugiliteRelation, String> descriptionMap;
    static {
        descriptionMap = new HashMap<SugiliteRelation,String>();
        descriptionMap.put(SugiliteRelation.HAS_CLASS_NAME,"");
        descriptionMap.put(SugiliteRelation.HAS_TEXT,"");
        descriptionMap.put(SugiliteRelation.HAS_CHILD_TEXT,"");
        descriptionMap.put(SugiliteRelation.HAS_SIBLING_TEXT,"");
        descriptionMap.put(SugiliteRelation.HAS_CONTENT_DESCRIPTION,"");
        descriptionMap.put(SugiliteRelation.HAS_SCREEN_LOCATION,"the exact location");
        descriptionMap.put(SugiliteRelation.HAS_PARENT_LOCATION,"the exact location ");
        descriptionMap.put(SugiliteRelation.HAS_PACKAGE_NAME,"in ");

        descriptionMap.put(SugiliteRelation.HAS_LIST_ORDER,"the %s item");
        descriptionMap.put(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER,"the item inside the %s item");

        descriptionMap.put(SugiliteRelation.HAS_PARENT,"has parent ");
        descriptionMap.put(SugiliteRelation.HAS_CHILD,"has child ");
        descriptionMap.put(SugiliteRelation.HAS_SIBLING,"has sibling ");
        descriptionMap.put(SugiliteRelation.HAS_VIEW_ID,"has view id ");


        descriptionMap.put(SugiliteRelation.IS_EDITABLE,"can be edited ");
        descriptionMap.put(SugiliteRelation.IS_CLICKABLE,"can be clicked ");
        descriptionMap.put(SugiliteRelation.IS_SCROLLABLE,"can be scrolled ");
        descriptionMap.put(SugiliteRelation.IS_CHECKABLE,"can be checked ");
        descriptionMap.put(SugiliteRelation.IS_CHECKED,"is checked ");

        descriptionMap.put(SugiliteRelation.IS_A_LIST,"is a list ");
        descriptionMap.put(SugiliteRelation.CONTAINS_EMAIL_ADDRESS,"email address ");
        descriptionMap.put(SugiliteRelation.CONTAINS_PHONE_NUMBER,"phone number ");
        descriptionMap.put(SugiliteRelation.CONTAINS_MONEY,"money ");
        descriptionMap.put(SugiliteRelation.CONTAINS_TIME,"time ");
        descriptionMap.put(SugiliteRelation.CONTAINS_DATE,"date ");
        descriptionMap.put(SugiliteRelation.CONTAINS_DURATION,"duration ");


    }

    public String getDescription(SugiliteRelation r)
    {
        return descriptionMap.get(r);
    }

    public String toString()
    {
        String result = "";
        for (SugiliteRelation r: descriptionMap.keySet())
        {
            result += r.getRelationName() + " " + descriptionMap.get(r) + "\n";
        }
        return result;
    }

    public static void main(String[] args)
    {
//        DescriptionGenerator test = new DescriptionGenerator();
//        //System.out.println(test.getDescription(SugiliteRelation.CONTAINS_DATE));
//        System.out.println(test);
        String s = "the %s item";
        String d = String.format(s,"1");
        System.out.println(s+d);
    }

}