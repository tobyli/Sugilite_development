package edu.cmu.hcii.sugilite.ontology.description;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.*;
import edu.cmu.hcii.sugilite.ontology.HashedStringOntologyQuery;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.sharing.SugiliteSharingScriptPreparer;

import static edu.cmu.hcii.sugilite.sharing.SugiliteSharingScriptPreparer.POTENTIALLY_PRIVATE_RELATIONS;

/**
 * Created by Wanling Ding on 22/02/2018.
 */

public class OntologyDescriptionGenerator {

    public static String getAppName(String packageName) {
        PackageManager packageManager = null;
        if (SugiliteData.getAppContext() != null) {
            packageManager = SugiliteData.getAppContext().getPackageManager();
        }

        if (packageName.equals("com.android.launcher3") ||
                packageName.equals("com.google.android.googlequicksearchbox") ||
                packageName.equals("com.google.android.apps.nexuslauncher"))
            return "Home Screen";
        if (packageManager != null) {
            ApplicationInfo ai;
            try {
                ai = packageManager.getApplicationInfo(packageName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            final String applicationName = (String) (ai != null ? packageManager.getApplicationLabel(ai) : "(unknown)");
            return applicationName;
        } else {
            return packageName;
        }
    }

    public Spanned getDescriptionForOntologyQuery(OntologyQuery ontologyQuery, boolean isParentQuery) {
        return getDescriptionForOntologyQuery(ontologyQuery, isParentQuery, false);
    }

    /**
     * Get the natural language description for an OntologyQuery
     *
     * @param ontologyQuery
     * @return
     */
    public Spanned getDescriptionForOntologyQuery(OntologyQuery ontologyQuery, boolean isParentQuery, boolean addClickableSpansForPrivacy) {
        String postfix = ""; // pretty sure this isn't used

        OntologyQueryFilter filter = ontologyQuery.getOntologyQueryFilter();

        //process LeafOntologyQuery
        if (ontologyQuery instanceof LeafOntologyQuery) {
            SpannableString result = new SpannableString("");
            LeafOntologyQuery loq = (LeafOntologyQuery) ontologyQuery;
            if (loq.getR().equals(SugiliteRelation.IS)) {
                result = new SpannableString("");
            }
            if (filter == null) {
                if (((LeafOntologyQuery) ontologyQuery).getR().equals(SugiliteRelation.HAS_CLASS_NAME)) {
                    if (isParentQuery) {
                        result = new SpannableString(TextUtils.concat("the", descriptionForSingleQuery((LeafOntologyQuery) ontologyQuery), postfix));
                    } else {
                        result = new SpannableString(TextUtils.concat(descriptionForSingleQuery((LeafOntologyQuery) ontologyQuery), postfix));
                    }
                } else {
                    if (isParentQuery) {
                        result = new SpannableString(TextUtils.concat("the item that ", descriptionForSingleQuery((LeafOntologyQuery) ontologyQuery), postfix));
                    } else {
                        result = new SpannableString(TextUtils.concat(descriptionForSingleQuery((LeafOntologyQuery) ontologyQuery), postfix));
                    }
                }
            } else {
                result = new SpannableString(TextUtils.concat(descriptionForSingleQueryWithFilter((LeafOntologyQuery) ontologyQuery), postfix));
            }

            //process privacy
            if (addClickableSpansForPrivacy && Arrays.stream(POTENTIALLY_PRIVATE_RELATIONS).anyMatch(loq.getR()::equals)) {
                result.setSpan(new UnhashedOntologyQueryClickableSpan(), 0, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return result;


            //process CombinedOntologyQuery
        } else if (ontologyQuery instanceof CombinedOntologyQuery) {
            CombinedOntologyQuery coq = (CombinedOntologyQuery) ontologyQuery;
            OntologyQuery[] subQueryArray = coq.getSubQueries().toArray(new OntologyQuery[coq.getSubQueries().size()]);

            //sort the queries based on the order that they should appear in the description
            Arrays.sort(subQueryArray, RelationWeight.ontologyQueryComparator);

            switch (coq.getSubRelation()) {
                case AND:
                    break;
                case OR:
                    break;
                case PREV:
                    break;
            }

            if (coq.getSubRelation() == CombinedOntologyQuery.RelationType.AND || coq.getSubRelation() == CombinedOntologyQuery.RelationType.OR || coq.getSubRelation() == CombinedOntologyQuery.RelationType.PREV) {
                int size = subQueryArray.length;

                //generate descriptions for subQueries
                Spanned[] arr = new Spanned[size];
                for (int i = 0; i < size; i++) {
                    arr[i] = getDescriptionForOntologyQuery(subQueryArray[i], false, addClickableSpansForPrivacy);
                }

                if (coq.getSubRelation() == CombinedOntologyQuery.RelationType.AND) {
                    return (Spanned) TextUtils.concat(translationWithRelationshipAnd(arr, subQueryArray, filter), postfix);
                } else if (coq.getSubRelation() == CombinedOntologyQuery.RelationType.OR) {
                    return (Spanned) TextUtils.concat(translationWithRelationshipOr(arr, subQueryArray, filter), postfix);
                } else if (coq.getSubRelation() == CombinedOntologyQuery.RelationType.PREV) {
                    return (Spanned) TextUtils.concat(translationWithRelationshipPrev(arr, coq.getR()), postfix);
                }
            } else {
                throw new RuntimeException("Unsupported relation type: " + coq.getSubRelation().toString());
            }
        } else if (ontologyQuery instanceof HashedStringOntologyQuery) {
            SpannableString spannableString = new SpannableString(getColoredSpannedTextFromMessage("has unknown text ", Const.SCRIPT_VIEW_ID_COLOR));
            if (addClickableSpansForPrivacy) {
                spannableString.setSpan(new HashedOntologyQueryClickableSpan(), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannableString;
        } else if (ontologyQuery instanceof PlaceholderOntologyQuery) {
            return (Spanned) TextUtils.concat(getColoredSpannedTextFromMessage("(temporary) ", Const.SCRIPT_PLACEHOLDER_COLOR), getDescriptionForOntologyQuery(((PlaceholderOntologyQuery) ontologyQuery).getInnerQuery(), true, addClickableSpansForPrivacy));
        } else {
            // oh boy
        }

        return new SpannableString("NULL");
    }

    public class HashedOntologyQueryClickableSpan extends ClickableSpan {
        @Override
        public void onClick(View widget) {
            Spanned s = (Spanned) ((TextView) widget).getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);
            PumiceDemonstrationUtil.showSugiliteToast("CLICKED! " + s.toString().substring(start, end) , Toast.LENGTH_SHORT);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.bgColor = SugiliteData.getAppContext().getResources().getColor(android.R.color.holo_red_dark);
            ds.setColor(SugiliteData.getAppContext().getResources().getColor(android.R.color.white));
            ds.setUnderlineText(true);
        }
    }

    public class UnhashedOntologyQueryClickableSpan extends ClickableSpan {
        @Override
        public void onClick(View widget) {
            Spanned s = (Spanned) ((TextView) widget).getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);
            PumiceDemonstrationUtil.showSugiliteToast("CLICKED! " + s.toString().substring(start, end) , Toast.LENGTH_SHORT);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.bgColor = SugiliteData.getAppContext().getResources().getColor(android.R.color.holo_blue_dark);
            ds.setColor(SugiliteData.getAppContext().getResources().getColor(android.R.color.white));
            ds.setUnderlineText(true);
        }
    }

    /**
     * Get the natural language description for a SugiliteOperation
     *
     * @param operation
     * @param sq
     * @return
     */
    public Spanned getSpannedDescriptionForOperation(SugiliteOperation operation, OntologyQuery sq, boolean addClickableSpansForPrivacy) {
        //TODO: temporily disable because of crashes due to unable to handle filters
        //return sq.toString();
        String prefix = "";

        if (operation.getOperationType() == SugiliteOperation.CLICK) {
            return (Spanned) TextUtils.concat(prefix, getDescriptionForOperation(getColoredSpannedTextFromMessage("Click on ", Const.SCRIPT_ACTION_COLOR), sq, addClickableSpansForPrivacy));
        } else if (operation.getOperationType() == SugiliteOperation.LONG_CLICK) {
            return (Spanned) TextUtils.concat(prefix, getDescriptionForOperation(getColoredSpannedTextFromMessage("Long click on ", Const.SCRIPT_ACTION_COLOR), sq, addClickableSpansForPrivacy));
        } else if (operation.getOperationType() == SugiliteOperation.READ_OUT) {
            return (Spanned) TextUtils.concat(prefix, getDescriptionForOperation(getColoredSpannedTextFromMessage("Read out ", Const.SCRIPT_ACTION_COLOR), sq, addClickableSpansForPrivacy));
        } else if (operation.getOperationType() == SugiliteOperation.SET_TEXT) {
            return (Spanned) TextUtils.concat(prefix, getDescriptionForOperation(getColoredSpannedTextFromMessage("Set text ", Const.SCRIPT_ACTION_COLOR), sq, addClickableSpansForPrivacy));
        } else if (operation.getOperationType() == SugiliteOperation.READOUT_CONST) {
            return (Spanned) TextUtils.concat(prefix, getDescriptionForOperation(getColoredSpannedTextFromMessage("Read out constant ", Const.SCRIPT_ACTION_COLOR), sq, addClickableSpansForPrivacy));
        } else if (operation.getOperationType() == SugiliteOperation.LOAD_AS_VARIABLE) {
            System.out.println("HERE");
            String vari = ((SugiliteLoadVariableOperation) operation).getParameter1();
            String[] s = vari.split("(?=\\p{Upper})");
            StringBuilder variables = new StringBuilder();
            for (String x : s) {
                variables.append(x);
                variables.append(" ");
            }
            return (Spanned) TextUtils.concat(prefix,
                    getDescriptionForOperation((Spanned) TextUtils.concat(getColoredSpannedTextFromMessage("Set value of ", Const.SCRIPT_ACTION_COLOR), getColoredSpannedTextFromMessage(((SugiliteLoadVariableOperation) operation).getVariableName(), Const.SCRIPT_CONDITIONAL_COLOR_3), getColoredSpannedTextFromMessage(" to the following: ", Const.SCRIPT_ACTION_COLOR), "the ", getColoredSpannedTextFromMessage(((SugiliteLoadVariableOperation) operation).getPropertyToSave(), Const.SCRIPT_CONDITIONAL_COLOR_3), " property in "), sq, addClickableSpansForPrivacy));
        } else {
            //TODO: handle more types of operations ***
            System.err.println("can't handle operation " + operation.getOperationType());
            return null;
        }

    }

    public Spanned getSpannedDescriptionForOperation(SugiliteOperation operation, OntologyQuery sq) {
        return getSpannedDescriptionForOperation(operation, sq, false);
    }

        // get a more viewable list order
    private static String numberToOrder(String number) {
        if (number.endsWith("1")) {
            if (number.endsWith("11"))
                number = number + "th";
            else
                number = number + "st";
        } else if (number.endsWith("2")) {
            if (number.endsWith("12"))
                number = number + "th";
            else
                number = number + "nd";
        } else if (number.endsWith("3")) {
            if (number.endsWith("13"))
                number = number + "th";
            else
                number = number + "rd";
        } else
            number = number + "th";
        return number;
    }


    static Spanned getColoredSpannedTextFromMessage(String message, String color) {
        return Html.fromHtml(String.format("<font color=\"%s\"><b>%s</b></font>", color, message));
    }


    private static SugiliteRelation getRForQuery(OntologyQuery query) {
        if (query instanceof LeafOntologyQuery) {
            return ((LeafOntologyQuery) query).getR();
        } else if (query instanceof HashedStringOntologyQuery) {
            return ((HashedStringOntologyQuery) query).getR();
        }
        return null;
    }


    // deals with some special cases to make description more readable
    private Spanned formatting(SugiliteRelation sugiliteRelation, String[] objectString) {
        if (sugiliteRelation == null) {
            return new SpannableString("NULL");
        }
        if (sugiliteRelation.equals(SugiliteRelation.HAS_SCREEN_LOCATION) || sugiliteRelation.equals(SugiliteRelation.HAS_PARENT_LOCATION))
            return (Spanned) TextUtils.concat(SugiliteRelationDescriptionGenerator.getDescription(sugiliteRelation), getColoredSpannedTextFromMessage("(" + objectString[0] + ")", Const.SCRIPT_IDENTIFYING_FEATURE_COLOR));

        else if (sugiliteRelation.equals(SugiliteRelation.HAS_TEXT) ||
                sugiliteRelation.equals(SugiliteRelation.HAS_CONTENT_DESCRIPTION) ||
                sugiliteRelation.equals(SugiliteRelation.HAS_CHILD_TEXT) ||
                sugiliteRelation.equals(SugiliteRelation.HAS_SIBLING_TEXT) ||
                sugiliteRelation.equals(SugiliteRelation.HAS_VIEW_ID)) {
            return (Spanned) TextUtils.concat(SugiliteRelationDescriptionGenerator.getDescription(sugiliteRelation), getColoredSpannedTextFromMessage("\"" + objectString[0] + "\"", Const.SCRIPT_IDENTIFYING_FEATURE_COLOR));
        } else if (sugiliteRelation.equals(SugiliteRelation.HAS_LIST_ORDER) || sugiliteRelation.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)) {
            objectString[0] = numberToOrder(objectString[0]);
            return new SpannableString(String.format(SugiliteRelationDescriptionGenerator.getDescription(sugiliteRelation).toString(), getColoredSpannedTextFromMessage(objectString[0], Const.SCRIPT_IDENTIFYING_FEATURE_COLOR)));
        }

        return (Spanned) TextUtils.concat(SugiliteRelationDescriptionGenerator.getDescription(sugiliteRelation), getColoredSpannedTextFromMessage(objectString[0], Const.SCRIPT_IDENTIFYING_FEATURE_COLOR));
    }

    // determines if the relation is about list order
    private boolean isListOrderRelation(SugiliteRelation sugiliteRelation) {
        if (sugiliteRelation != null) {
            if (sugiliteRelation.equals(SugiliteRelation.HAS_LIST_ORDER) || sugiliteRelation.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)) {
                return true;
            }
        }
        return false;
    }

    private Spanned getDescriptionForOperation(Spanned verb, OntologyQuery q, boolean addClickableSpansForPrivacy) {
        return (Spanned) TextUtils.concat(verb, getDescriptionForOntologyQuery(q, true, addClickableSpansForPrivacy));
    }

    // translates the filters
    private String translateFilter(OntologyQueryFilter filter) {
        SugiliteRelation filterRelation = null;
        String translation = "";
        filterRelation = filter.getRelation();
        if (isListOrderRelation(filterRelation)) {
            translation = String.format(SugiliteRelationDescriptionGenerator.getDescription(filterRelation).toString(), FilterTranslation.getFilterTranslation(filter));
        } else {
            translation = ("the " + FilterTranslation.getFilterTranslation(filter) + " " + SugiliteRelationDescriptionGenerator.getDescription(filterRelation)).replace("contains the ", "").trim();
        }
        return translation;
    }

    // separating "or" from "and"
    private SpannableStringBuilder translationWithRelationshipOr(Spanned[] descriptions, OntologyQuery[] queries, OntologyQueryFilter filter) {
        SpannableStringBuilder result = new SpannableStringBuilder("");
        int descriptionArrayLength = descriptions.length;
        String translatedFilter = "";
        Spanned spannedTranslatedFilter = new SpannableString("");
        boolean isListOrder = false;
        if (filter != null) {
            translatedFilter = translateFilter(filter);
            spannedTranslatedFilter = getColoredSpannedTextFromMessage(translatedFilter, Const.SCRIPT_ACTION_PARAMETER_COLOR);
            if (isListOrderRelation(filter.getRelation())) {
                result.append(spannedTranslatedFilter);
                result.append(" that ");
                isListOrder = true;
            }
        }
        if (!isListOrder) {
            result.append("the item that ");
        }

        for (int i = 0; i < descriptionArrayLength - 1; i++) {
            result.append(" ");
            result.append(descriptions[i]);
            result.append(" or ");
        }
        result.append(" ");
        result.append(descriptions[descriptionArrayLength - 1]);
        if (filter != null && !isListOrder) {
            result.append(", with ");
            result.append(spannedTranslatedFilter);
        }
        return result;
    }

    // translates the list order filter with class name
    // e.g. the first item --> the first button
    private void filterListOrderTranslationWithClassName(SpannableStringBuilder result, Spanned translation, Spanned[] descriptions) {
        result.clear();
        result.append(translation.subSequence(0, translation.length() - 4));
        result.append(descriptions[0]);
    }

    // handles the special case for package name (no for loop)
    // package name always appears at the end and does not need "and"
    // e.g. in home screen
    private void handlePackageNameSpecialCase(Spanned[] descriptions, OntologyQueryFilter filter, SpannableStringBuilder result, Spanned translatedFilter, boolean isListOrder, SugiliteRelation lastRelation, boolean needsMoreChange) {
        int descriptionArrayLength = descriptions.length;
        if (lastRelation != null && lastRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
            if (!needsMoreChange) // all queries before package name are taken care of
            {
                result.append(" ");
                result.append(descriptions[descriptionArrayLength - 1]);
            } else {
                result.append(" that ");
                result.append(descriptions[descriptionArrayLength - 2]); // the query before package name needs to be included
                result.append(" ");
                result.append(descriptions[descriptionArrayLength - 1]); // and then add package name relation

            }
        } else { // the last relation is not package name, so there is no package name relation in the queries
            if (!needsMoreChange) // all queries before last query are taken care of
            {
                result.append(" that ");
                result.append(descriptions[descriptionArrayLength - 1]); // add the last relation
            } else {
                result.append(" that ");
                result.append(descriptions[descriptionArrayLength - 2]);// the query before last query needs to be included
                result.append(" and ");
                result.append(descriptions[descriptionArrayLength - 1]); // and then add the last relation
            }
        }
        if (filter != null && !isListOrder) {
            result.append(" with ");
            result.append(translatedFilter);
        }
    }

    // handles the special case for package name (with for loop)
    private void handlePackageNameWithForLoop(Spanned[] descriptions, OntologyQueryFilter filter, SpannableStringBuilder result, Spanned translatedFilter, boolean isListOrder, SugiliteRelation lastRelation, int startingNumber) {
        int descriptionArrayLength = descriptions.length;
        if (lastRelation != null && lastRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
            for (int i = startingNumber; i < descriptionArrayLength - 2; i++) {
                // startingNumber indicates the index of the description that should be followed with ","
                result.append(", ");
                result.append(descriptions[i]);
            }
            if (startingNumber < descriptionArrayLength - 1) {
                // only 2 descriptions, no "," needed
                result.append(" and ");
                result.append(descriptions[descriptionArrayLength - 2]);
            }
            result.append(" ");
            result.append(descriptions[descriptionArrayLength - 1]);

        } else {
            for (int i = startingNumber; i < descriptionArrayLength - 1; i++) {
                result.append(", ");
                result.append(descriptions[i]);
            }
            result.append(" and ");
            result.append(descriptions[descriptionArrayLength - 1]);

        }
        if (filter != null && !isListOrder) {
            result.append(" with ");
            result.append(translatedFilter);
        }
    }

    /*
    private Spanned getDescrptionForCombinedOntologyQueryWithAndRelation (OntologyQuery[] queries, OntologyQueryFilter filter) {
        String result = "";
        String filterClause = "";
        SugiliteRelation filterRelation = null; // filter relation
        boolean isListOrder = false;

        //get the filterClause
        if (filter != null) {
            filterRelation = filter.getRelation();
            filterClause = getColoredHTMLFromMessage(translateFilter(filter), Const.SCRIPT_ACTION_PARAMETER_COLOR);
        }


        if (queries[0] != null && getRForQuery(queries[0]).equals(SugiliteRelation.HAS_CLASS_NAME)) {
            //the first relation is a HAS_CLASS_NAME relation
            if (queries[1] != null && isListOrderRelation(getRForQuery(queries[1]))) {
                //special case: the first relation is a HAS_CLASS_NAME relation and the second relation is list order
                String itemDescription = getDescriptionForOntologyQuery(queries[1], false).toString().replace("item", getDescriptionForOntologyQuery(queries[0], false).toString());
                if (queries.length == 2) {
                    if (filter != null) {
                        if (isListOrderRelation(filterRelation)) {
                            result = filterListOrderTranslationWithClassName(result, filterClause, queries);
                            isListOrder = true;
                        }
                        else {
                            result = itemDescription + " with " + filterClause;
                        }
                    }
                }
            }
        }

    }
    */

    private Spanned translationWithRelationshipAnd(Spanned[] descriptions, OntologyQuery[] queries, OntologyQueryFilter filter) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        int descriptionArrayLength = descriptions.length;
        int queryLength = queries.length;
        SugiliteRelation firstRelation = getRForQuery(queries[0]); // first relation
        SugiliteRelation lastRelation = getRForQuery(queries[queryLength - 1]); // last relation
        SugiliteRelation filterRelation = null; // filter relation
        String translatedFilter = "";
        Spanned spannedTranslatedFilter = new SpannableString("");
        boolean isListOrder = false;
        if (filter != null) {
            // translate the filter
            filterRelation = filter.getRelation();
            translatedFilter = translateFilter(filter);
            spannedTranslatedFilter = getColoredSpannedTextFromMessage(translatedFilter, Const.SCRIPT_ACTION_PARAMETER_COLOR);
        }
        // if there is class name, it should be the first
        if (firstRelation != null && firstRelation.equals(SugiliteRelation.HAS_CLASS_NAME)) {
            // if there is list order, it should be the second
            SugiliteRelation secondRelation = getRForQuery(queries[1]);
            // special case: class + list order
            if (secondRelation != null && isListOrderRelation(secondRelation)) {
                // e.g. the 1st item --> the 1st button
                result.append(descriptions[1].toString().replace("item", ""));
                result.append(descriptions[0]);
                // only class and list order
                if (descriptionArrayLength == 2) {
                    if (filter != null) {
                        if (isListOrderRelation(filterRelation)) {
                            // e.g. the 1st button --> the first button
                            filterListOrderTranslationWithClassName(result, spannedTranslatedFilter, descriptions);
                            isListOrder = true;
                        } else
                            // e.g. the 1st button with the cheapest price
                            result.append(" with ");
                            result.append(spannedTranslatedFilter);
                    }
                }
                // there is other relation besides class name and list order
                else {
                    if (filter != null) {
                        if (isListOrderRelation(filterRelation)) {
                            filterListOrderTranslationWithClassName(result, spannedTranslatedFilter, descriptions);
                            isListOrder = true;
                        }
                        // other filters are handled in packageName handler function
                    }
                    if (descriptionArrayLength == 3) {
                        handlePackageNameSpecialCase(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, false);
                    } else if (descriptionArrayLength == 4) {
                        handlePackageNameSpecialCase(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, true);
                    }
                    // needs for loop
                    else {
                        result.append(" that ");
                        result.append(descriptions[2]);
                        handlePackageNameWithForLoop(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, 3);
                        // startingNumber indicates the index of the description that should be followed with ","
                        // in this case, e.g. the 1st button that has text hello, text abc, and text 123
                        // startingNumber should be 3 because "," follows "text hello", which is the 3rd in the description array
                    }
                }
            }
            // special case: only class, no list order
            else {
                if (filter != null) {
                    if (isListOrderRelation(filterRelation)) {
                        filterListOrderTranslationWithClassName(result, spannedTranslatedFilter, descriptions);
                        isListOrder = true;
                    } else {
                        result.clear();
                        result.append("the ");
                        result.append(descriptions[0]);// e.g. the button
                    }
                } else {
                    result.clear();
                    result.append("the ");
                    result.append(descriptions[0]);
                }

                if (descriptionArrayLength == 2) {
                    handlePackageNameSpecialCase(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, false);
                } else if (descriptionArrayLength == 3) {
                    handlePackageNameSpecialCase(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, true);
                }
                // needs for loop
                else {
                    result.append(" that ");
                    result.append(descriptions[1]);
                    handlePackageNameWithForLoop(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, 2);
                    // startingNumber indicates the index of the description that should be followed with ","
                    // in this case, e.g. the button that has text hello, text abc, and text 123
                    // startingNumber should be 2 because "," follows "text hello", which is the 2nd in the description array
                }
            }
        }

        // special case: only list order, no class
        else if (firstRelation != null && isListOrderRelation(firstRelation)) {
            result.append(descriptions[0]);
            if (filter != null) {
                if (isListOrderRelation(filterRelation)) {
                    result.clear();
                    result.append(spannedTranslatedFilter);
                    isListOrder = true;
                }
            }
            if (descriptionArrayLength == 2) {
                handlePackageNameSpecialCase(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, false);
            } else if (descriptionArrayLength == 3) {
                handlePackageNameSpecialCase(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, true);
            }
            // needs for loop
            else {
                result.append(" that ");
                result.append(descriptions[1]);
                handlePackageNameWithForLoop(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, 2);
                // startingNumber indicates the index of the description that should be followed with ","
                // in this case, e.g. the 1st item that has text hello, text abc, and text 123
                // startingNumber should be 2 because "," follows "text hello", which is the 2nd in the description array
            }
        }

        // general case
        else {

            if (filter != null) {
                if (isListOrderRelation(filterRelation)) {
                    // e.g. the first item that has
                    result.clear();
                    result.append(spannedTranslatedFilter);
                    result.append(" that ");
                    isListOrder = true;
                } else {
                    result.clear();
                    result.append(spannedTranslatedFilter);
                    result.append("the item that ");
                }
            } else {
                result.clear();
                result.append(spannedTranslatedFilter);
                result.append("the item that ");
            }
            // e.g. the item that has text hello
            result.append(descriptions[0]);
            handlePackageNameWithForLoop(descriptions, filter, result, spannedTranslatedFilter, isListOrder, lastRelation, 1);
            // startingNumber indicates the index of the description that should be followed with ","
            // in this case, e.g. the item that has text hello, text abc, and text 123
            // startingNumber should be 1 because "," follows "text hello", which is the 1st in the description array

        }
        return result;
    }

    private Spanned descriptionForSingleQuery(LeafOntologyQuery ontologyQuery) {
        String[] objectString = new String[1];
        SugiliteRelation sugiliteRelation = getRForQuery(ontologyQuery);
        SugiliteEntity[] objectArr = ontologyQuery.getObjectSet().toArray(new SugiliteEntity[ontologyQuery.getObjectSet().size()]);

        if (sugiliteRelation.equals(SugiliteRelation.HAS_CLASS_NAME)) {
            objectString[0] = ObjectTranslation.getTranslation(objectArr[0].toString());
        } else if (sugiliteRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
            objectString[0] = getAppName(objectArr[0].toString());
        } else {
            objectString[0] = objectArr[0].toString();
        }
        return formatting(sugiliteRelation, objectString);
    }

    private String descriptionForSingleQueryWithFilter(LeafOntologyQuery ontologyQuery) {
        OntologyQueryFilter filter = ontologyQuery.getOntologyQueryFilter();
        SugiliteRelation filterRelation = filter.getRelation();
        String result = "";
        SugiliteRelation sugiliteRelation = getRForQuery(ontologyQuery);
        String translatedFilter = translateFilter(filter);
        if (isListOrderRelation(filterRelation)) {
            result += translatedFilter;
            if (sugiliteRelation.equals(SugiliteRelation.HAS_CLASS_NAME)) {
                result = result.replace("item", descriptionForSingleQuery(ontologyQuery));
            } else if (sugiliteRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
                result += descriptionForSingleQuery(ontologyQuery);
            } else if (!(isListOrderRelation(sugiliteRelation))) {
                result += " that " + descriptionForSingleQuery(ontologyQuery);
            }
            return result;
        }
        if (isListOrderRelation(sugiliteRelation) || sugiliteRelation.equals(SugiliteRelation.HAS_CLASS_NAME)) {
            result += descriptionForSingleQuery(ontologyQuery);
        } else if (sugiliteRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
            result += "item " + descriptionForSingleQuery(ontologyQuery);
        } else {
            result += "the item that " + descriptionForSingleQuery(ontologyQuery);
        }
        result += " with " + translatedFilter;
        return result;
    }

    private boolean isSpatialRelationship(SugiliteRelation sugiliteRelation) {
        if (sugiliteRelation.equals(SugiliteRelation.CONTAINS) || sugiliteRelation.equals(SugiliteRelation.RIGHT)
                || sugiliteRelation.equals(SugiliteRelation.LEFT) || sugiliteRelation.equals(SugiliteRelation.ABOVE)
                || sugiliteRelation.equals(SugiliteRelation.NEAR) || sugiliteRelation.equals(SugiliteRelation.NEXT_TO))
            return true;
        return false;
    }

    private boolean isParentChildSiblingPrev(SugiliteRelation sugiliteRelation) {
        if (sugiliteRelation.equals(SugiliteRelation.HAS_PARENT) || sugiliteRelation.equals(SugiliteRelation.HAS_CHILD) || sugiliteRelation.equals(SugiliteRelation.HAS_SIBLING))
            return true;
        return false;
    }

    //return string that starts with "has" or "is"
    private SpannableStringBuilder translationWithRelationshipPrev(Spanned[] descriptions, SugiliteRelation sugiliteRelation) {
        SpannableStringBuilder result = new SpannableStringBuilder("");
        result.append(SugiliteRelationDescriptionGenerator.getDescription(sugiliteRelation));
        result.append(descriptions[0]);

        for (int i = 1; i < descriptions.length; i++) {
            result.append(" and ");
            result.append(descriptions[i]);
        }
        return result;
    }

    public static void main(String[] args) {
        OntologyDescriptionGenerator generator = new OntologyDescriptionGenerator();
        System.out.println("Enter a query:");
        while (true) {
            BufferedReader screenReader = new BufferedReader(new InputStreamReader(System.in));
            String input = "";
            System.out.print("> ");
            try {
                input = screenReader.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                OntologyQuery query = OntologyQuery.deserialize(input);
                String description = generator.getDescriptionForOperation(new SpannableString("Click on "), query, false).toString();
                //clean up the html tags
                description = description.replaceAll("\\<.*?\\>", "");
                System.out.println(description);
            } catch (Exception e) {
                System.out.println("Failed to parse the query");
                e.printStackTrace();
            }

        }
    }

}
