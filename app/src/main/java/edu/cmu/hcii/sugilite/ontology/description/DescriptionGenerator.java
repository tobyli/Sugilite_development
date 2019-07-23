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

    private static final HashMap<SugiliteRelation, String> descriptionMap;
    static {
        descriptionMap = new HashMap<SugiliteRelation,String>();
        descriptionMap.put(SugiliteRelation.HAS_CLASS_NAME,"");
        descriptionMap.put(SugiliteRelation.HAS_TEXT, setColor("has the text ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PARENT, setColor("is the parent of ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_VIEW_ID, setColor("has the view id " ,SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_CONTENT_DESCRIPTION, setColor("has the content ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_SCREEN_LOCATION, setColor("has the exact location ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PARENT_LOCATION, setColor("has the parent location ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PACKAGE_NAME,"in ");
        descriptionMap.put(SugiliteRelation.HAS_CHILD, setColor("is a child of ", SCRIPT_VIEW_ID_COLOR));

        descriptionMap.put(SugiliteRelation.HAS_CHILD_TEXT, setColor("has the text ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_SIBLING_TEXT,"");
        descriptionMap.put(SugiliteRelation.HAS_SIBLING, setColor("is a sibling of ", SCRIPT_VIEW_ID_COLOR));

        descriptionMap.put(SugiliteRelation.IS_EDITABLE,"is editable ");
        descriptionMap.put(SugiliteRelation.IS_CLICKABLE,"is clickable ");
        descriptionMap.put(SugiliteRelation.IS_SCROLLABLE,"is scrollable ");
        descriptionMap.put(SugiliteRelation.IS_CHECKABLE,"is checkable ");
        descriptionMap.put(SugiliteRelation.IS_CHECKED,"is checked ");
        descriptionMap.put(SugiliteRelation.IS_SELECTED,"is selected ");
        descriptionMap.put(SugiliteRelation.IS_FOCUSED,"is focused ");

        descriptionMap.put(SugiliteRelation.HAS_LIST_ORDER,"the %s item");
        descriptionMap.put(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER,"the %s item");
        descriptionMap.put(SugiliteRelation.IS_A_LIST,"is a list ");

        descriptionMap.put(SugiliteRelation.CONTAINS, setColor("contains ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.ABOVE, setColor("is above ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.BELOW, setColor("is below ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.NEAR, setColor("is near ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.NEXT_TO, setColor("is next to ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.RIGHT, setColor("is to the right of ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.LEFT, setColor("is to the left of ", SCRIPT_VIEW_ID_COLOR));


        descriptionMap.put(SugiliteRelation.CONTAINS_EMAIL_ADDRESS,"contains the email address ");
        descriptionMap.put(SugiliteRelation.CONTAINS_PHONE_NUMBER,"contains the phone number ");
        descriptionMap.put(SugiliteRelation.CONTAINS_MONEY,"contains the price ");
        descriptionMap.put(SugiliteRelation.CONTAINS_TIME,"contains the time ");
        descriptionMap.put(SugiliteRelation.CONTAINS_DATE,"contains the date ");
        descriptionMap.put(SugiliteRelation.CONTAINS_DURATION,"contains the duration ");
        descriptionMap.put(SugiliteRelation.CONTAINS_LENGTH,"contains the distance ");
        descriptionMap.put(SugiliteRelation.CONTAINS_PERCENTAGE,"contains the percentage ");
        descriptionMap.put(SugiliteRelation.CONTAINS_VOLUME,"contains the volume ");
        descriptionMap.put(SugiliteRelation.CONTAINS_NUMBER,"contains the number ");


    }

    public static String getDescription(SugiliteRelation r)
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