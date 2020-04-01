package edu.cmu.hcii.sugilite.ontology.description;


import android.text.SpannableString;
import android.text.Spanned;

import java.util.*;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import static edu.cmu.hcii.sugilite.Const.SCRIPT_VIEW_ID_COLOR;
import static edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator.getColoredSpannedTextFromMessage;

/**
 * Created by Wanling Ding on 01/02/2018.
 */

public class SugiliteRelationDescriptionGenerator {

    private static final HashMap<SugiliteRelation, Spanned> descriptionMap;

    static {
        descriptionMap = new HashMap<SugiliteRelation, Spanned>();
        descriptionMap.put(SugiliteRelation.HAS_CLASS_NAME, new SpannableString(""));
        descriptionMap.put(SugiliteRelation.HAS_TEXT, getColoredSpannedTextFromMessage("has the text ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PARENT, getColoredSpannedTextFromMessage("is the parent of ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_VIEW_ID, getColoredSpannedTextFromMessage("has the view id ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_CONTENT_DESCRIPTION, getColoredSpannedTextFromMessage("has the content ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_SCREEN_LOCATION, getColoredSpannedTextFromMessage("has the exact location ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PARENT_LOCATION, getColoredSpannedTextFromMessage("has the parent location ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_PACKAGE_NAME, new SpannableString("in "));
        descriptionMap.put(SugiliteRelation.HAS_ACTIVITY_NAME, new SpannableString("on ")); // TODO maybe change this
        descriptionMap.put(SugiliteRelation.HAS_CHILD, getColoredSpannedTextFromMessage("is a child of ", SCRIPT_VIEW_ID_COLOR));

        descriptionMap.put(SugiliteRelation.HAS_CHILD_TEXT, getColoredSpannedTextFromMessage("has the text ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.HAS_SIBLING_TEXT, new SpannableString(""));
        descriptionMap.put(SugiliteRelation.HAS_SIBLING, getColoredSpannedTextFromMessage("is a sibling of ", SCRIPT_VIEW_ID_COLOR));

        descriptionMap.put(SugiliteRelation.IS_EDITABLE, new SpannableString("is editable "));
        descriptionMap.put(SugiliteRelation.IS_CLICKABLE, new SpannableString("is clickable "));
        descriptionMap.put(SugiliteRelation.IS_SCROLLABLE, new SpannableString("is scrollable "));
        descriptionMap.put(SugiliteRelation.IS_CHECKABLE, new SpannableString("is checkable "));
        descriptionMap.put(SugiliteRelation.IS_CHECKED, new SpannableString("is checked "));
        descriptionMap.put(SugiliteRelation.IS_SELECTED, new SpannableString("is selected "));
        descriptionMap.put(SugiliteRelation.IS_FOCUSED, new SpannableString("is focused "));

        descriptionMap.put(SugiliteRelation.HAS_LIST_ORDER, new SpannableString("the %s item"));
        descriptionMap.put(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER, new SpannableString("the %s item"));
        descriptionMap.put(SugiliteRelation.IS_A_LIST, new SpannableString("is a list "));

        descriptionMap.put(SugiliteRelation.CONTAINS, getColoredSpannedTextFromMessage("contains ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.ABOVE, getColoredSpannedTextFromMessage("is above ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.BELOW, getColoredSpannedTextFromMessage("is below ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.NEAR, getColoredSpannedTextFromMessage("is near ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.NEXT_TO, getColoredSpannedTextFromMessage("is next to ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.RIGHT, getColoredSpannedTextFromMessage("is to the right of ", SCRIPT_VIEW_ID_COLOR));
        descriptionMap.put(SugiliteRelation.LEFT, getColoredSpannedTextFromMessage("is to the left of ", SCRIPT_VIEW_ID_COLOR));


        descriptionMap.put(SugiliteRelation.CONTAINS_EMAIL_ADDRESS, new SpannableString("contains the email address "));
        descriptionMap.put(SugiliteRelation.CONTAINS_PHONE_NUMBER, new SpannableString("contains the phone number "));
        descriptionMap.put(SugiliteRelation.CONTAINS_MONEY, new SpannableString("contains the price "));
        descriptionMap.put(SugiliteRelation.CONTAINS_TIME, new SpannableString("contains the time "));
        descriptionMap.put(SugiliteRelation.CONTAINS_DATE, new SpannableString("contains the date "));
        descriptionMap.put(SugiliteRelation.CONTAINS_DURATION, new SpannableString("contains the duration "));
        descriptionMap.put(SugiliteRelation.CONTAINS_LENGTH, new SpannableString("contains the distance "));
        descriptionMap.put(SugiliteRelation.CONTAINS_PERCENTAGE, new SpannableString("contains the percentage "));
        descriptionMap.put(SugiliteRelation.CONTAINS_VOLUME, new SpannableString("contains the volume "));
        descriptionMap.put(SugiliteRelation.CONTAINS_NUMBER, new SpannableString("contains the number "));


    }

    public static Spanned getDescriptionOfSugiliteRelation(SugiliteRelation r) {
        return descriptionMap.get(r);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (SugiliteRelation r : descriptionMap.keySet()) {
            result.append(r.getRelationName() + " " + descriptionMap.get(r) + "\n");
        }
        return result.toString();
    }


}