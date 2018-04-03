package edu.cmu.hcii.sugilite.ontology.description;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.Html;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.*;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.OntologyQueryFilter;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

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

    private String getAppName(String packageName) {
        if(packageName.equals("com.android.launcher3") ||
                packageName.equals("com.google.android.googlequicksearchbox"))
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

    private String numberToOrder(String num) {
        if (num.endsWith("1")) {
            if (num.endsWith("11"))
                num = num + "th";
            else
                num = num + "st";
        }
        else if (num.endsWith("2")) {
            if (num.endsWith("12"))
                num = num + "th";
            else
                num = num + "nd";
        }
        else if (num.endsWith("3")) {
            if (num.endsWith("13"))
                num = num + "th";
            else
                num = num + "rd";
        }
        else
            num = num + "th";
        return num;
    }


    static private String setColor(String message, String color) {
        return "<font color=\"" + color + "\"><b>" + message + "</b></font>";
    }

    private String formatting(SugiliteRelation sr, String[] os) {
        if (sr.equals(SugiliteRelation.HAS_SCREEN_LOCATION) || sr.equals(SugiliteRelation.HAS_PARENT_LOCATION))
            return DescriptionGenerator.descriptionMap.get(sr) + setColor("(" + os[0] + ")", Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);

        else if (sr.equals(SugiliteRelation.HAS_TEXT) ||
                sr.equals(SugiliteRelation.HAS_CONTENT_DESCRIPTION) ||
                sr.equals(SugiliteRelation.HAS_CHILD_TEXT) ||
                sr.equals(SugiliteRelation.HAS_SIBLING_TEXT) ||
                sr.equals(SugiliteRelation.HAS_VIEW_ID)) {
            return DescriptionGenerator.descriptionMap.get(sr) + setColor("\"" +  os[0] + "\"", Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);
        }

        else if (sr.equals(SugiliteRelation.HAS_LIST_ORDER) || sr.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)) {
            os[0] = numberToOrder(os[0]);
            return String.format(DescriptionGenerator.descriptionMap.get(sr), setColor(os[0], Const.SCRIPT_IDENTIFYING_FEATURE_COLOR));
        }
        return DescriptionGenerator.descriptionMap.get(sr) + setColor(os[0], Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);
    }


    public String getDescriptionForOperation(SugiliteOperation operation, SerializableOntologyQuery sq){
        //TODO: temporily disable because of crashes due to unable to handle filters
        return sq.toString();
        /*
        if(operation.getOperationType() == SugiliteOperation.CLICK){
            return getDescriptionForOperation(setColor("Click on ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else if(operation.getOperationType() == SugiliteOperation.READ_OUT){
            return getDescriptionForOperation(setColor("Read out ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else if(operation.getOperationType() == SugiliteOperation.SET_TEXT){
            return getDescriptionForOperation(setColor("Set text ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else if(operation.getOperationType() == SugiliteOperation.READOUT_CONST){
            return getDescriptionForOperation(setColor("Read out constant ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else if(operation.getOperationType() == SugiliteOperation.LOAD_AS_VARIABLE){
            return getDescriptionForOperation(setColor("Set variable to the following: ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else{
            //TODO: handle more types of operations ***
            return null;
        }
        */
    }

    private String getDescriptionForOperation(String verb, SerializableOntologyQuery sq){
        return verb + getDescriptionForOntologyQuery(sq);
    }

    private String translationWithRelationshipOr(String[] args, OntologyQuery[] queries, OntologyQueryFilter f) {
        String result = "";
        int l = args.length;
        int ql = queries.length;
        SugiliteRelation r = queries[0].getR();
        SugiliteRelation fr = null;
        String translatedFilter = "";
        if(f != null) {
            fr = f.getRelation();
            if (fr.equals(SugiliteRelation.HAS_LIST_ORDER) || fr.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER))
                translatedFilter = String.format(DescriptionGenerator.descriptionMap.get(fr),FilterTranslation.getFilterTranslation(f));
            else
                translatedFilter = ("the "+FilterTranslation.getFilterTranslation(f)+" "+DescriptionGenerator.descriptionMap.get(fr)).trim();
        }
        String conjunction = "has ";
        result = "the item that ";
        if (f != null)
            result += conjunction + translatedFilter + " or ";
        for (int i = 0; i < l-1; i++) {
            if (queries[i].getR().equals(SugiliteRelation.HAS_CLASS_NAME)||queries[i].getR().equals(SugiliteRelation.HAS_LIST_ORDER)||queries[i].getR().equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER))
                conjunction = "is ";
            else
                conjunction = "has ";
            result += conjunction + args[i] + " or ";
        }
        if (queries[l-1].getR().equals(SugiliteRelation.HAS_PACKAGE_NAME))
            conjunction = "is ";
        else
            conjunction = "has ";
        result += conjunction+args[l-1];

        return result;
    }


    private String translationWithRelationshipAnd(String[] args, OntologyQuery[] queries, OntologyQueryFilter f) {
        String result = "";
        int l = args.length;
        int ql = queries.length;
        SugiliteRelation r = queries[0].getR();
        //System.out.println(f);
        SugiliteRelation fr = null;
        String translatedFilter = "";
        if(f != null) {
            fr = f.getRelation();
            if (fr.equals(SugiliteRelation.HAS_LIST_ORDER) || fr.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER))
                translatedFilter = String.format(DescriptionGenerator.descriptionMap.get(fr),FilterTranslation.getFilterTranslation(f));
            else
                translatedFilter = ("the "+FilterTranslation.getFilterTranslation(f)+" "+DescriptionGenerator.descriptionMap.get(fr)).trim();
        }
        if (r.equals(SugiliteRelation.HAS_CLASS_NAME)) {
            SugiliteRelation r2 = queries[1].getR();
            SugiliteRelation r3 = queries[ql-1].getR();

            // get the description of the object; e.g. the 1st button
            if (r2.equals(SugiliteRelation.HAS_LIST_ORDER) || r2.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER))
            {
                result += args[1].replace("item","");
                result += args[0];
                // the 1st button --> the first button
                if (l==2) {
                    if (f != null) {
                        //System.out.println(result);
                        if (fr.equals(SugiliteRelation.HAS_LIST_ORDER) || fr.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER))
                        {
                            //System.out.println(FilterTranslation.getFilterTranslation(f));
                            result += " that is " + translatedFilter;
                            //System.out.println(result);
                        }
                        else
                            result += " that has " + translatedFilter;
                    }
                }

                else if (l==3)
                {
                    // the 1st button that has [filter] and/or something
                    if (f!=null) {
                        result += " that has " + translatedFilter;
                        if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                            result += " " + args[2];
                        else
                            result += " and " + args[2];
                    }
                    // the 1st button that has something
                    else {
                        if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                            result += " " + args[2];
                        else
                            result += " that has " + args[2];
                    }
                }

                else if (l > 3) {
                    result += " that has ";
                    if (f!=null)
                        result += translatedFilter+", ";
                    result += args[2];
                    if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                    {
                        for (int i = 3; i < l-2; i++) {
                            result += ", " + args[i];
                        }
                        result += " and " + args[l-2];
                        result += " " + args[l-1];
                    }
                    else
                    {
                        for (int i = 3; i < l - 1; i++) {
                            result += ", " + args[i];
                        }
                        result += " and " + args[l-1];
                    }
                }
            }
            else{
                result = String.format("the %s", args[0]);
                // the button that has something
                if (l==2) {
                    if (f!=null) {
                        result += " that has " + translatedFilter;
                        if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                            result += " " + args[1];
                        else
                            result += " and " + args[1];
                    }
                    else
                    {
                        if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                            result += " " + args[1];
                        else
                            result += " that has " + args[1];
                    }
                }
                else if (l == 3) {
                    if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                    {
                        if (f!=null) {
                            result += " that has " + translatedFilter;
                            result += " and " + args[1];
                        }
                        else {
                            result += " that has " + args[1];
                        }
                        result += " " + args[2];
                    }
                    else {
                        if (f!=null)
                            result += " that has " + translatedFilter + ", " + args[1] + " and " + args[2];
                        else
                            result += " that has " + args[1] + " and " + args[2];
                    }
                }
                else if (l > 3) {
                    result += " that has ";
                    if (f!=null)
                        result += translatedFilter+", ";
                    result += args[1];
                    if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                    {
                        for (int i = 2; i < l-2; i++) {
                            result += ", " + args[i];
                        }
                        result += " and " + args[l-2];
                        result += " " + args[l-1];
                    }
                    else
                    {
                        for (int i = 2; i < l - 1; i++) {
                            result += ", " + args[i];
                        }
                        result += " and " + args[l-1];
                    }
                }
            }

        }

        else if (r.equals(SugiliteRelation.HAS_LIST_ORDER) || r.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER))
        {
            result += args[0];
            if (l==2)
            {
                if (f!=null) {
                    if (fr.equals(SugiliteRelation.HAS_LIST_ORDER) || fr.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER))
                    {
                        result += " that is " + translatedFilter;
                        result += " and " + "has " + args[1];
                    }
                    else {
                        result += " that has " + translatedFilter;
                        result += " and " + args[1];
                    }
                }
                else
                    result += " that has " + args[1];

            }
            SugiliteRelation r3 = queries[ql-1].getR();
            if (l == 3) {
                if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
                    result += " that has ";
                    if (f != null)
                        result += translatedFilter+" and ";
                    result += args[1];
                    result += " " + args[2];
                }
                else {
                    result += " that has ";
                    if (f != null)
                        result += translatedFilter+", ";
                    result += args[1];
                    result += " and " + args[2];
                }
            }
            if (l > 3) {
                result += " that has ";
                if (f != null)
                    result += translatedFilter+", ";
                result += args[1];
                if (r3.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                {
                    for (int i = 2; i < l-2; i++) {
                        result += ", " + args[i];
                    }
                    result += " and " + args[l-2];
                    result += " " + args[l-1];
                }
                else
                {
                    for (int i = 2; i < l - 1; i++) {
                        result += ", " + args[i];
                    }
                    result += " and " + args[l - 1];
                }
            }
        }

        else {
            result = "the item that has ";
            if (f != null)
                result += translatedFilter+", ";
            result += args[0];
            if (l > 2) {
                for (int i = 1; i < l-1; i++) {
                    result += ", " + args[i];
                }
                result += " and " + args[l-1];
            }
            else
                result += " and " + args[1];
        }
        return result;
    }

    private String descriptionForSingleQuery(OntologyQuery q) {
        SugiliteEntity[] objectArr = q.getObject().toArray(new SugiliteEntity[q.getObject().size()]);
        String[] objectString = new String[1];
        SugiliteRelation r = q.getR();
        if(r.equals(SugiliteRelation.HAS_CLASS_NAME)) {
            objectString[0] = ObjectTranslation.getTranslation(objectArr[0].toString());
        }
        else {
            if (r.equals(SugiliteRelation.HAS_TEXT) || r.equals(SugiliteRelation.HAS_CONTENT_DESCRIPTION) || r.equals(SugiliteRelation.HAS_CHILD_TEXT) || r.equals(SugiliteRelation.HAS_SIBLING_TEXT))
                objectString[0] = objectArr[0].toString();
            else if (r.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                objectString[0] = getAppName(objectArr[0].toString());
            else if (r.equals(SugiliteRelation.HAS_CLASS_NAME))
                objectString[0] = "";
            else
                objectString[0] = objectArr[0].toString();
        }
        return formatting(r, objectString);
    }

    public String descriptionForSingleQueryWithFilter(OntologyQuery q) {
        OntologyQueryFilter f = q.getOntologyQueryFilter();
        SugiliteRelation fr = f.getRelation();
        String result = "";
        SugiliteRelation r = q.getR();
        if (f.getRelation().equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER) || f.getRelation().equals(SugiliteRelation.HAS_LIST_ORDER))
        {
            result += String.format(DescriptionGenerator.descriptionMap.get(fr),FilterTranslation.getFilterTranslation(f));
            if (!(r.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER) || r.equals(SugiliteRelation.HAS_LIST_ORDER)))
                result += " that has "+descriptionForSingleQuery(q);
            return result;
        }
        String translatedFilter = "the "+FilterTranslation.getFilterTranslation(f)+" "+DescriptionGenerator.descriptionMap.get(fr);
        if (r.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER) || r.equals(SugiliteRelation.HAS_LIST_ORDER))
        {
            result += descriptionForSingleQuery(q);
            result += " that has "+translatedFilter;
        }
        else {
            result += "the item that has " + descriptionForSingleQuery(q);
            result += " and has "+translatedFilter;
        }
        return result;
    }

    /**
     * Get the natural language description for a SerializableOntologyQuery
     * @param sq
     * @return
     */
    public String getDescriptionForOntologyQuery(SerializableOntologyQuery sq) {
        OntologyQuery ontologyQuery = new OntologyQuery(sq);
        SugiliteRelation r = ontologyQuery.getR();
        OntologyQueryFilter f = ontologyQuery.getOntologyQueryFilter();
        if (ontologyQuery.getSubRelation() == OntologyQuery.relationType.nullR) {
            // base case
            // this should have size 1 always, the array is only used in execution for when there's a query whose results are used as the objects of the next one
            if (f == null)
                return descriptionForSingleQuery(ontologyQuery);
            else
                return descriptionForSingleQueryWithFilter(ontologyQuery);

        }
        OntologyQuery[] subQueryArray = ontologyQuery.getSubQueries().toArray(new OntologyQuery[ontologyQuery.getSubQueries().size()]);
        Arrays.sort(subQueryArray, RelationWeight.ontologyQueryComparator);


        //TODO: the use of "and" and "or" should be grammatically correct
        if (ontologyQuery.getSubRelation() == OntologyQuery.relationType.AND || ontologyQuery.getSubRelation() == OntologyQuery.relationType.OR || ontologyQuery.getSubRelation() == OntologyQuery.relationType.PREV) {
            int size = subQueryArray.length;
            String[] arr = new String[size];
            for (int i = 0; i < size; i++) {
                //SerializableOntologyQuery soq = new SerializableOntologyQuery(subQueryArray[i]);
                //arr[i] = getDescriptionForOntologyQuery(soq);
                arr[i] = descriptionForSingleQuery(subQueryArray[i]);
            }


            if (ontologyQuery.getSubRelation() == OntologyQuery.relationType.AND) {
                //return StringUtils.join(arr, " ");
                return translationWithRelationshipAnd(arr,subQueryArray, f);
            }
            else if (ontologyQuery.getSubRelation() == OntologyQuery.relationType.OR)
                return translationWithRelationshipOr(arr,subQueryArray,f);
            else {
                String res = "the item that has ";
                res += DescriptionGenerator.descriptionMap.get(r);
                res += "that has " + arr[0];
                for (int i = 1; i<arr.length;i++) {
                    res += " and " + arr[i];
                }
                return res;
            }
        }

        // SubRelation == relationType.PREV
        SerializableOntologyQuery soq0 = new SerializableOntologyQuery(subQueryArray[0]);

        return r.getRelationName() + " " + getDescriptionForOntologyQuery(soq0);
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
                SerializableOntologyQuery query = new SerializableOntologyQuery(OntologyQuery.deserialize(input));
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
