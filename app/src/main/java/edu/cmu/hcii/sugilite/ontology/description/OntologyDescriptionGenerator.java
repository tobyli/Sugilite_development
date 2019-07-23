package edu.cmu.hcii.sugilite.ontology.description;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.*;
import edu.cmu.hcii.sugilite.ontology.HashedStringOntologyQuery;

/**
 * Created by Wanling Ding on 22/02/2018.
 */

public class OntologyDescriptionGenerator {
    Context context;
    PackageManager packageManager;


    public OntologyDescriptionGenerator(Context context) {
        this.context = context;
        if(context != null) {
            this.packageManager = context.getPackageManager();
        }
    }
    public static String getAppName(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        if(packageName.equals("com.android.launcher3") ||
                packageName.equals("com.google.android.googlequicksearchbox") ||
                packageName.equals("com.google.android.apps.nexuslauncher"))
            return "Home Screen";
        if(packageManager != null) {
            ApplicationInfo ai;
            try {
                ai = packageManager.getApplicationInfo(packageName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            final String applicationName = (String) (ai != null ? packageManager.getApplicationLabel(ai) : "(unknown)");
            return applicationName;
        }
        else {
            return packageName;
        }
    }

    // get the package name for the application
    private String getAppName(String packageName) {
        if (context != null) {
            return getAppName(context, packageName);
        } else {
            return packageName;
        }
    }

    // get a more viewable list order
    private static String numberToOrder(String number) {
        if (number.endsWith("1")) {
            if (number.endsWith("11"))
                number = number + "th";
            else
                number = number + "st";
        }
        else if (number.endsWith("2")) {
            if (number.endsWith("12"))
                number = number + "th";
            else
                number = number + "nd";
        }
        else if (number.endsWith("3")) {
            if (number.endsWith("13"))
                number = number + "th";
            else
                number = number + "rd";
        }
        else
            number = number + "th";
        return number;
    }


    public static String setColor(String message, String color) {
        return "<font color=\"" + color + "\"><b>" + message + "</b></font>";
    }

    // deals with some special cases to make description more readable
    private String formatting(SugiliteRelation sugiliteRelation, String[] objectString) {
        if(sugiliteRelation == null){
            return "NULL";
        }
        if (sugiliteRelation.equals(SugiliteRelation.HAS_SCREEN_LOCATION) || sugiliteRelation.equals(SugiliteRelation.HAS_PARENT_LOCATION))
            return DescriptionGenerator.getDescription(sugiliteRelation) + setColor("(" + objectString[0] + ")", Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);

        else if (sugiliteRelation.equals(SugiliteRelation.HAS_TEXT) ||
                sugiliteRelation.equals(SugiliteRelation.HAS_CONTENT_DESCRIPTION) ||
                sugiliteRelation.equals(SugiliteRelation.HAS_CHILD_TEXT) ||
                sugiliteRelation.equals(SugiliteRelation.HAS_SIBLING_TEXT) ||
                sugiliteRelation.equals(SugiliteRelation.HAS_VIEW_ID)) {
            return DescriptionGenerator.getDescription(sugiliteRelation) + setColor("\"" +  objectString[0] + "\"", Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);
        }

        else if (sugiliteRelation.equals(SugiliteRelation.HAS_LIST_ORDER) || sugiliteRelation.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)) {
            objectString[0] = numberToOrder(objectString[0]);
            return String.format(DescriptionGenerator.getDescription(sugiliteRelation), setColor(objectString[0], Const.SCRIPT_IDENTIFYING_FEATURE_COLOR));
        }

        return DescriptionGenerator.getDescription(sugiliteRelation) + setColor(objectString[0], Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);
    }


    public String getDescriptionForOperation(SugiliteOperation operation, OntologyQuery sq){
        //TODO: temporily disable because of crashes due to unable to handle filters
        //return sq.toString();
        String prefix = "";

        if(operation.getOperationType() == SugiliteOperation.CLICK){
            return prefix + getDescriptionForOperation(setColor("Click on ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else if(operation.getOperationType() == SugiliteOperation.READ_OUT){
            return prefix + getDescriptionForOperation(setColor("Read out ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else if(operation.getOperationType() == SugiliteOperation.SET_TEXT){
            return prefix + getDescriptionForOperation(setColor("Set text ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else if(operation.getOperationType() == SugiliteOperation.READOUT_CONST){
            return prefix + getDescriptionForOperation(setColor("Read out constant ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else if(operation.getOperationType() == SugiliteOperation.LOAD_AS_VARIABLE){
            System.out.println("HERE");
            String vari = ((SugiliteLoadVariableOperation) operation).getParameter1();
            String[] s = vari.split("(?=\\p{Upper})");
            vari = "";
            for (String x : s) {
                vari += x + " ";
            }
            return prefix + getDescriptionForOperation(setColor("Set value of ", Const.SCRIPT_ACTION_COLOR) + setColor(((SugiliteLoadVariableOperation) operation).getVariableName(), Const.SCRIPT_CONDITIONAL_COLOR_3) + setColor(" to the following: ", Const.SCRIPT_ACTION_COLOR) + "the " +  setColor(((SugiliteLoadVariableOperation) operation).getPropertyToSave(), Const.SCRIPT_CONDITIONAL_COLOR_3) + " property in ", sq);
        }
        else{
            //TODO: handle more types of operations ***
            System.err.println("can't handle operation " + operation.getOperationType());
            return null;
        }

    }

    // determines if the relation is about list order
    private boolean isListOrderRelation(SugiliteRelation sugiliteRelation)
    {
        if (sugiliteRelation!=null) {
            if (sugiliteRelation.equals(SugiliteRelation.HAS_LIST_ORDER) || sugiliteRelation.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)) {
                return true;
            }
        }
        return false;
    }

    private String getDescriptionForOperation(String verb, OntologyQuery q){
        return verb + getDescriptionForOntologyQuery(q, true);
    }

    // translates the filters
    private String translateFilter(OntologyQueryFilter filter)
    {
        SugiliteRelation filterRelation = null;
        String translation = "";
        filterRelation = filter.getRelation();
        if (isListOrderRelation(filterRelation)) {
            translation = String.format(DescriptionGenerator.getDescription(filterRelation), FilterTranslation.getFilterTranslation(filter));
        }
        else {
            translation = ("the " + FilterTranslation.getFilterTranslation(filter) + " " + DescriptionGenerator.getDescription(filterRelation)).replace("contains the ", "").trim();
        }
        return translation;
    }

    // separating "or" from "and"
    private String translationWithRelationshipOr(String[] descriptions, OntologyQuery[] queries, OntologyQueryFilter filter) {
        String result = "";
        int descriptionArrayLength = descriptions.length;
        String translatedFilter = "";
        boolean isListOrder = false;
        if(filter != null) {
            translatedFilter = translateFilter(filter);
            translatedFilter = setColor(translatedFilter, Const.SCRIPT_ACTION_PARAMETER_COLOR);
            if (isListOrderRelation(filter.getRelation()))
            {
                result = translatedFilter + " that ";
                isListOrder = true;
            }
        }
        if (!isListOrder) {
            result = "the item that ";
        }

        for (int i = 0; i < descriptionArrayLength-1; i++) {
            result += " " + descriptions[i] + " or ";
        }

        result += (" " + descriptions[descriptionArrayLength-1]);
        if (filter != null && !isListOrder)
            result += ", with " + translatedFilter;

        return result;
    }

    // translates the list order filter with class name
    // e.g. the first item --> the first button
    private String filterListOrderTranslationWithClassName(String result, String translation, String[] descriptions)
    {
        result = "";
        result += translation.replace("item", "");
        result += descriptions[0];
        return result;
    }

    // handles the special case for package name (no for loop)
    // package name always appears at the end and does not need "and"
    // e.g. in home screen
    private String packageNameSpecialCaseHandler(String[] descriptions, OntologyQueryFilter filter, String result, String translatedFilter, boolean isListOrder, SugiliteRelation lastRelation, boolean needsMoreChange)
    {
        int descriptionArrayLength = descriptions.length;
        if (lastRelation != null && lastRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
            if (!needsMoreChange) // all queries before package name are taken care of
            {
                result += " " + descriptions[descriptionArrayLength-1]; // just add package name relation
            }
            else
            {
                result += " that " + descriptions[descriptionArrayLength-2]; // the query before package name needs to be included
                result += " " + descriptions[descriptionArrayLength-1]; // and then add package name relation
            }
        }
        else { // the last relation is not package name, so there is no package name relation in the queries
            if (!needsMoreChange) // all queries before last query are taken care of
            {
                result += " that " + descriptions[descriptionArrayLength-1]; // add the last relation
            }
            else
            {
                result += " that " + descriptions[descriptionArrayLength-2]; // the query before last query needs to be included
                result += " and " + descriptions[descriptionArrayLength-1]; // and then add the last relation
            }
        }
        if (filter != null && !isListOrder)
            result += " with " + translatedFilter;
        return result;
    }

    // handles the special case for package name (with for loop)
    private String packageNameHandlerWithForLoop(String[] descriptions, OntologyQueryFilter filter, String result, String translatedFilter, boolean isListOrder, SugiliteRelation lastRelation, int startingNumber)
    {
        int descriptionArrayLength = descriptions.length;
        if (lastRelation != null && lastRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
            for (int i = startingNumber; i < descriptionArrayLength - 2; i++) {
                // startingNumber indicates the index of the description that should be followed with ","
                result += ", " + descriptions[i];
            }
            if (startingNumber < descriptionArrayLength -1)
                // only 2 descriptions, no "," needed
                result += " and " + descriptions[descriptionArrayLength - 2];
            result += " " + descriptions[descriptionArrayLength - 1];

        }
        else {
            for (int i = startingNumber; i < descriptionArrayLength - 1; i++) {
                result += ", " + descriptions[i];
            }
            result += " and " + descriptions[descriptionArrayLength - 1];

        }
        if (filter != null && !isListOrder)
            result += " with " + translatedFilter;
        return result;
    }

    private static SugiliteRelation getRForQuery(OntologyQuery query) {
        if (query instanceof LeafOntologyQuery) {
            return ((LeafOntologyQuery)query).getR();
        } else if (query instanceof HashedStringOntologyQuery) {
            return ((HashedStringOntologyQuery)query).getR();
        }
        return null;
    }

    private String translationWithRelationshipAnd(String[] descriptions, OntologyQuery[] queries, OntologyQueryFilter filter) {
        String result = "";
        int descriptionArrayLength = descriptions.length;
        int queryLength = queries.length;
        SugiliteRelation firstRelation = getRForQuery(queries[0]); // first relation
        SugiliteRelation lastRelation = getRForQuery(queries[queryLength-1]); // last relation
        SugiliteRelation filterRelation = null; // filter relation
        String translatedFilter = "";
        boolean isListOrder = false;
        if(filter != null) {
            // translate the filter
            filterRelation = filter.getRelation();
            translatedFilter = translateFilter(filter);
            translatedFilter = setColor(translatedFilter, Const.SCRIPT_ACTION_PARAMETER_COLOR);
        }
        // if there is class name, it should be the first
        if (firstRelation != null && firstRelation.equals(SugiliteRelation.HAS_CLASS_NAME)) {
            // if there is list order, it should be the second
            SugiliteRelation secondRelation = getRForQuery(queries[1]);
            // special case: class + list order
            if (secondRelation != null && isListOrderRelation(secondRelation)) {
                // e.g. the 1st item --> the 1st button
                result += descriptions[1].replace("item", "");
                result += descriptions[0];
                // only class and list order
                if (descriptionArrayLength == 2) {
                    if (filter != null) {
                        if (isListOrderRelation(filterRelation)) {
                            // e.g. the 1st button --> the first button
                            result = filterListOrderTranslationWithClassName(result, translatedFilter, descriptions);
                            isListOrder = true;
                        }
                        else
                            // e.g. the 1st button with the cheapest price
                            result += " with " + translatedFilter;
                    }
                }
                // there is other relation besides class name and list order
                else {
                    if (filter != null) {
                        if (isListOrderRelation(filterRelation)) {
                            result = filterListOrderTranslationWithClassName(result, translatedFilter, descriptions);
                            isListOrder = true;
                        }
                        // other filters are handled in packageName handler function
                    }
                    if (descriptionArrayLength == 3) {
                        result = packageNameSpecialCaseHandler(descriptions, filter, result, translatedFilter, isListOrder, lastRelation,false);
                    }
                    else if (descriptionArrayLength == 4) {
                        result = packageNameSpecialCaseHandler(descriptions, filter, result, translatedFilter, isListOrder, lastRelation,true);
                    }
                    // needs for loop
                    else {
                        result += " that ";
                        result += descriptions[2];
                        result = packageNameHandlerWithForLoop(descriptions, filter, result, translatedFilter, isListOrder, lastRelation,3);
                        // startingNumber indicates the index of the description that should be followed with ","
                        // in this case, e.g. the 1st button that has text hello, text abc, and text 123
                        // startingNumber should be 3 because "," follows "text hello", which is the 3rd in the description array
                    }
                }
            }
            // special case: only class, no list order
            else {
                if (filter != null) {
                    if (isListOrderRelation(filterRelation))
                    {
                        result = filterListOrderTranslationWithClassName(result, translatedFilter, descriptions);
                        isListOrder = true;
                    }
                    else {
                        result = String.format("the %s", descriptions[0]); // e.g. the button
                    }
                }
                else {
                    result = String.format("the %s", descriptions[0]);
                }

                if (descriptionArrayLength == 2) {
                    result = packageNameSpecialCaseHandler(descriptions,filter,result,translatedFilter,isListOrder,lastRelation,false);
                }
                else if (descriptionArrayLength == 3) {
                    result = packageNameSpecialCaseHandler(descriptions,filter,result,translatedFilter,isListOrder,lastRelation,true);
                }
                // needs for loop
                else {
                    result += " that ";
                    result += descriptions[1];
                    result = packageNameHandlerWithForLoop(descriptions,filter,result,translatedFilter,isListOrder,lastRelation,2);
                    // startingNumber indicates the index of the description that should be followed with ","
                    // in this case, e.g. the button that has text hello, text abc, and text 123
                    // startingNumber should be 2 because "," follows "text hello", which is the 2nd in the description array
                }
            }
        }

        // special case: only list order, no class
        else if (firstRelation != null && isListOrderRelation(firstRelation)) {
            result += descriptions[0];
            if (filter != null) {
                if (isListOrderRelation(filterRelation)) {
                    result = translatedFilter;
                    isListOrder = true;
                }
            }
            if (descriptionArrayLength==2)
            {
                result = packageNameSpecialCaseHandler(descriptions,filter,result,translatedFilter,isListOrder,lastRelation,false);
            }
            else if (descriptionArrayLength == 3) {
                result = packageNameSpecialCaseHandler(descriptions, filter, result, translatedFilter, isListOrder, lastRelation,true);
            }
            // needs for loop
            else {
                result += " that ";
                result += descriptions[1];
                result = packageNameHandlerWithForLoop(descriptions, filter, result, translatedFilter, isListOrder, lastRelation, 2);
                // startingNumber indicates the index of the description that should be followed with ","
                // in this case, e.g. the 1st item that has text hello, text abc, and text 123
                // startingNumber should be 2 because "," follows "text hello", which is the 2nd in the description array
            }
        }

        // general case
        else {

            if (filter != null)
            {
                if (isListOrderRelation(filterRelation))
                {
                    // e.g. the first item that has
                    result = translatedFilter + " that ";
                    isListOrder = true;
                }
                else {
                    result = "the item that ";
                }
            }
            else {
                result = "the item that ";
            }
            // e.g. the item that has text hello
            result += descriptions[0];
            result = packageNameHandlerWithForLoop(descriptions,filter,result,translatedFilter,isListOrder,lastRelation,1);
            // startingNumber indicates the index of the description that should be followed with ","
            // in this case, e.g. the item that has text hello, text abc, and text 123
            // startingNumber should be 1 because "," follows "text hello", which is the 1st in the description array

        }
        return result;
    }

    private String descriptionForSingleQuery(LeafOntologyQuery ontologyQuery) {
        String[] objectString = new String[1];
        SugiliteRelation sugiliteRelation = getRForQuery(ontologyQuery);
        SugiliteEntity[] objectArr = ontologyQuery.getObjectSet().toArray(new SugiliteEntity[ontologyQuery.getObjectSet().size()]);

        if (sugiliteRelation.equals(SugiliteRelation.HAS_CLASS_NAME)) {
            objectString[0] = ObjectTranslation.getTranslation(objectArr[0].toString());
        } else if  (sugiliteRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
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
        if (isListOrderRelation(filterRelation))  {
            result += translatedFilter;
            if (sugiliteRelation.equals(SugiliteRelation.HAS_CLASS_NAME)) {
                result = result.replace("item", descriptionForSingleQuery(ontologyQuery));
            }
            else if (sugiliteRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
                result += descriptionForSingleQuery(ontologyQuery);
            }
            else if (!(isListOrderRelation(sugiliteRelation))) {
                result += " that " + descriptionForSingleQuery(ontologyQuery);
            }
            return result;
        }
        if (isListOrderRelation(sugiliteRelation) || sugiliteRelation.equals(SugiliteRelation.HAS_CLASS_NAME))
        {
            result += descriptionForSingleQuery(ontologyQuery);
        }

        else if (sugiliteRelation.equals(SugiliteRelation.HAS_PACKAGE_NAME))
        {
            result += "item " + descriptionForSingleQuery(ontologyQuery);
        }

        else {
            result += "the item that " + descriptionForSingleQuery(ontologyQuery);
        }
        result += " with " + translatedFilter;
        return result;
    }

    private boolean isSpatialRelationship(SugiliteRelation sugiliteRelation)
    {
        if (sugiliteRelation.equals(SugiliteRelation.CONTAINS) || sugiliteRelation.equals(SugiliteRelation.RIGHT)
                || sugiliteRelation.equals(SugiliteRelation.LEFT) || sugiliteRelation.equals(SugiliteRelation.ABOVE)
                || sugiliteRelation.equals(SugiliteRelation.NEAR) || sugiliteRelation.equals(SugiliteRelation.NEXT_TO))
            return true;
        return false;
    }

    private boolean isParentChildSiblingPrev(SugiliteRelation sugiliteRelation)
    {
        if (sugiliteRelation.equals(SugiliteRelation.HAS_PARENT) || sugiliteRelation.equals(SugiliteRelation.HAS_CHILD) || sugiliteRelation.equals(SugiliteRelation.HAS_SIBLING))
            return true;
        return false;
    }

    //return string that starts with "has" or "is"
    private String translationWithRelationshipPrev(String[] descriptions, SugiliteRelation sugiliteRelation)
    {
        String result = "";
        result += DescriptionGenerator.getDescription(sugiliteRelation);

        result += descriptions[0];
        for (int i = 1; i<descriptions.length;i++) {
            result += " and " + descriptions[i];
        }
        return result;
    }

    /**
     * Get the natural language description for an OntologyQuery
     * @param ontologyQuery
     * @return
     */
    public String getDescriptionForOntologyQuery(OntologyQuery ontologyQuery, boolean isParentQuery) {
        String postfix = ""; // pretty sure this isn't used

        OntologyQueryFilter filter = ontologyQuery.getOntologyQueryFilter();

        if (ontologyQuery instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery)ontologyQuery;
            if (loq.getR().equals(SugiliteRelation.IS)) {
                return "";
            }
            if (filter == null) {
                if (((LeafOntologyQuery) ontologyQuery).getR().equals(SugiliteRelation.HAS_CLASS_NAME)) {
                    if (isParentQuery) {
                        return "the " + descriptionForSingleQuery((LeafOntologyQuery) ontologyQuery) + postfix;
                    } else {
                        return descriptionForSingleQuery((LeafOntologyQuery) ontologyQuery) + postfix;
                    }
                } else {
                    if (isParentQuery) {
                        return "the item that " + descriptionForSingleQuery((LeafOntologyQuery) ontologyQuery) + postfix;
                    } else {
                        return descriptionForSingleQuery((LeafOntologyQuery) ontologyQuery) + postfix;
                    }
                }
            }
            else {
                return descriptionForSingleQueryWithFilter((LeafOntologyQuery)ontologyQuery) + postfix;
            }
        } else if (ontologyQuery instanceof CombinedOntologyQuery) {
            CombinedOntologyQuery coq = (CombinedOntologyQuery)ontologyQuery;
            OntologyQuery[] subQueryArray = coq.getSubQueries().toArray(new OntologyQuery[coq.getSubQueries().size()]);

            //sort the queries based on the order that they should appear in the description
            Arrays.sort(subQueryArray, RelationWeight.ontologyQueryComparator);

            if (coq.getSubRelation() == CombinedOntologyQuery.RelationType.AND || coq.getSubRelation() == CombinedOntologyQuery.RelationType.OR || coq.getSubRelation() == CombinedOntologyQuery.RelationType.PREV) {
                int size = subQueryArray.length;

                //generate descriptions for subQueries
                String[] arr = new String[size];
                for (int i = 0; i < size; i++) {
                    arr[i] = getDescriptionForOntologyQuery(subQueryArray[i], false);
                }

                if (coq.getSubRelation() == CombinedOntologyQuery.RelationType.AND) {
                    System.out.println(postfix);
                    return translationWithRelationshipAnd(arr,subQueryArray, filter) + postfix;
                }
                else if (coq.getSubRelation() == CombinedOntologyQuery.RelationType.OR) {
                    return translationWithRelationshipOr(arr, subQueryArray, filter) + postfix;
                }

                else if (coq.getSubRelation() == CombinedOntologyQuery.RelationType.PREV) {
                    return translationWithRelationshipPrev(arr, coq.getR()) + postfix;
                }
            } else {
                throw new RuntimeException("Unsupported relation type: " + coq.getSubRelation().toString());
            }
        } else if (ontologyQuery instanceof HashedStringOntologyQuery) {
            // TODO not very convincing
            return setColor("secret text ", Const.SCRIPT_VIEW_ID_COLOR);
        } else {
            // oh boy
        }

        return "NULL";
    }

    public static void main(String[] args){
        OntologyDescriptionGenerator generator = new OntologyDescriptionGenerator(null);
        System.out.println("Enter a query:");
        while (true) {
            BufferedReader screenReader = new BufferedReader(new InputStreamReader(System.in));
            String input = "";
            System.out.print("> ");
            try {
                input = screenReader.readLine();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            try {
                OntologyQuery query = OntologyQuery.deserialize(input);
                String description = generator.getDescriptionForOperation("Click on ", query);
                //clean up the html tags
                description = description.replaceAll("\\<.*?\\>", "");
                System.out.println(description);
            }
            catch (Exception e){
                System.out.println("Failed to parse the query");
                e.printStackTrace();
            }

        }
    }





}
