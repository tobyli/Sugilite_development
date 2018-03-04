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

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
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


    static private String setColor(String message, String color) {
        return "<font color=\"" + color + "\"><b>" + message + "</b></font>";
    }

    public String formatting(SugiliteRelation sr, String[] os) {
        if (sr.equals(SugiliteRelation.HAS_SCREEN_LOCATION) || sr.equals(SugiliteRelation.HAS_PARENT_LOCATION))
            return DescriptionGenerator.descriptionMap.get(sr) + setColor("(" + os[0] + ")", Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);

        else if (sr.equals(SugiliteRelation.HAS_TEXT) ||
                sr.equals(SugiliteRelation.HAS_CONTENT_DESCRIPTION) ||
                sr.equals(SugiliteRelation.HAS_CHILD_TEXT) ||
                sr.equals(SugiliteRelation.HAS_SIBLING_TEXT) ||
                sr.equals(SugiliteRelation.HAS_VIEW_ID)) {
            return DescriptionGenerator.descriptionMap.get(sr) + setColor("\"" + os[0] + "\"", Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);
        }

        else if (sr.equals(SugiliteRelation.HAS_LIST_ORDER) || sr.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)) {
            if (os[0].equals("1"))
                os[0] = os[0] + "st";
            else if (os[0].equals("2"))
                os[0] = os[0] + "nd";
            else if (os[0].equals("3"))
                os[0] = os[0] + "rd";
            else
                os[0] = os[0] + "th";
            return String.format(DescriptionGenerator.descriptionMap.get(sr), setColor(os[0], Const.SCRIPT_IDENTIFYING_FEATURE_COLOR));
        }
        return DescriptionGenerator.descriptionMap.get(sr) + setColor(os[0], Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);
    }


    public String getDescriptionForOperation(SugiliteOperation operation, SerializableOntologyQuery sq){
        if(operation.getOperationType() == SugiliteOperation.CLICK){
            return getDescriptionForOperation(setColor("Click on ", Const.SCRIPT_ACTION_COLOR), sq);
        }
        else{
            return "NULL";
        }
    }

    public String getDescriptionForOperation(String verb, SerializableOntologyQuery sq){
        return verb + getDescriptionForOntologyQuery(sq);
    }

    /**
     * Get the natural language description for a SerializableOntologyQuery
     * @param sq
     * @return
     */
    public String getDescriptionForOntologyQuery(SerializableOntologyQuery sq) {
        OntologyQuery ontologyQuery = new OntologyQuery(sq);
        SugiliteRelation r = ontologyQuery.getR();
        if (ontologyQuery.getSubRelation() == OntologyQuery.relationType.nullR) {
            // base case
            // this should have size 1 always, the array is only used in execution for when there's a query whose results are used as the objects of the next one
            SugiliteEntity[] objectArr = ontologyQuery.getObject().toArray(new SugiliteEntity[ontologyQuery.getObject().size()]);
            String[] objectString = new String[1];
            boolean flag = false;
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
        OntologyQuery[] subQueryArray = ontologyQuery.getSubQueries().toArray(new OntologyQuery[ontologyQuery.getSubQueries().size()]);
        Arrays.sort(subQueryArray, RelationWeight.ontologyQueryComparator);


        //TODO: the use of "and" and "or" should be grammatically correct
        if (ontologyQuery.getSubRelation() == OntologyQuery.relationType.AND || ontologyQuery.getSubRelation() == OntologyQuery.relationType.OR) {
            int size = subQueryArray.length;
            String[] arr = new String[size];
            for (int i = 0; i < size; i++) {
                SerializableOntologyQuery soq = new SerializableOntologyQuery(subQueryArray[i]);
                arr[i] = getDescriptionForOntologyQuery(soq);
            }


            if (ontologyQuery.getSubRelation() == OntologyQuery.relationType.AND) {
                return StringUtils.join(arr, " ");
            } else return StringUtils.join(arr, " or ");
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
