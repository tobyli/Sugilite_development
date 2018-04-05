package edu.cmu.hcii.sugilite.ontology.description;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static edu.cmu.hcii.sugilite.Const.SCRIPT_VIEW_ID_COLOR;
import static edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator.setColor;

/**
 * Created by Wanling Ding on 01/02/2018.
 */

public class DescriptionGenerator {

    public static final HashMap<SugiliteRelation, String> descriptionMap;
    static {
        descriptionMap = new HashMap<SugiliteRelation,String>();
        descriptionMap.put(SugiliteRelation.HAS_CLASS_NAME,"");
        descriptionMap.put(SugiliteRelation.HAS_TEXT, setColor("text ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PARENT, setColor("parent ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_VIEW_ID, setColor("view id " ,SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_CONTENT_DESCRIPTION, setColor("content ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_SCREEN_LOCATION, setColor("the exact location ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PARENT_LOCATION, setColor("the parent location ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PACKAGE_NAME,"in ");
        descriptionMap.put(SugiliteRelation.HAS_CHILD, setColor("child ", SCRIPT_VIEW_ID_COLOR));

        descriptionMap.put(SugiliteRelation.HAS_CHILD_TEXT, setColor("child text ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_SIBLING_TEXT,"");
        descriptionMap.put(SugiliteRelation.HAS_SIBLING, setColor("sibling ", SCRIPT_VIEW_ID_COLOR));

        descriptionMap.put(SugiliteRelation.IS_EDITABLE,"is editable ");
        descriptionMap.put(SugiliteRelation.IS_CLICKABLE,"is clickable ");
        descriptionMap.put(SugiliteRelation.IS_SCROLLABLE,"is scrollable ");
        descriptionMap.put(SugiliteRelation.IS_CHECKABLE,"is checkable ");
        descriptionMap.put(SugiliteRelation.IS_CHECKED,"is checked ");
        descriptionMap.put(SugiliteRelation.IS_SELECTED,"is selected ");

        descriptionMap.put(SugiliteRelation.HAS_LIST_ORDER,"the %s item");
        descriptionMap.put(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER,"the %s item");
        descriptionMap.put(SugiliteRelation.IS_A_LIST,"is a list ");

        descriptionMap.put(SugiliteRelation.CONTAINS_EMAIL_ADDRESS,"email address ");
        descriptionMap.put(SugiliteRelation.CONTAINS_PHONE_NUMBER,"phone number ");
        descriptionMap.put(SugiliteRelation.CONTAINS_MONEY,"price ");
        descriptionMap.put(SugiliteRelation.CONTAINS_TIME,"time ");
        descriptionMap.put(SugiliteRelation.CONTAINS_DATE,"date ");
        descriptionMap.put(SugiliteRelation.CONTAINS_DURATION,"duration ");
        descriptionMap.put(SugiliteRelation.CONTAINS_LENGTH,"distance ");
        descriptionMap.put(SugiliteRelation.CONTAINS_PERCENTAGE,"percentage ");
        descriptionMap.put(SugiliteRelation.CONTAINS_VOLUME,"volume ");


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




}