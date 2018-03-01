package edu.cmu.hcii.sugilite.ontology;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.Arrays;
import edu.cmu.hcii.sugilite.Const;

/**
 * Created by Wanling Ding on 22/02/2018.
 */

public class OntologyDescriptionGenerator {
    Context context;
    PackageManager packageManager;


    public OntologyDescriptionGenerator(Context context){
        this.context = context;
        this.packageManager = context.getPackageManager();
    }

    public String getAppName(String packageName){
        ApplicationInfo ai;
        try {
            ai = packageManager.getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        final String applicationName = (String) (ai != null ? packageManager.getApplicationLabel(ai) : "(unknown)");
        return applicationName;
    }



    static public String setColor(String message, String color){
        return "<font color=\"" + color + "\"><b>" + message + "</b></font>";
    }

    public String formatting(SugiliteRelation sr, String[] os)
    {
        if (sr.equals(SugiliteRelation.HAS_SCREEN_LOCATION) || sr.equals(SugiliteRelation.HAS_PARENT_LOCATION))
            return DescriptionGenerator.descriptionMap.get(sr);

        else if (sr.equals(SugiliteRelation.HAS_TEXT) || sr.equals(SugiliteRelation.HAS_CONTENT_DESCRIPTION) || sr.equals(SugiliteRelation.HAS_CHILD_TEXT) || sr.equals(SugiliteRelation.HAS_SIBLING_TEXT))
            {
                return setColor(DescriptionGenerator.descriptionMap.get(sr) + '"' + os[0] + '"',"#ff00ff");
            }
        else if (sr.equals(SugiliteRelation.HAS_LIST_ORDER) || sr.equals(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)){
            if (os[0].equals("1"))
                os[0] = os[0] + "st";
            else if (os[0].equals("2"))
                os[0] = os[0] + "nd";
            else if (os[0].equals("3"))
                os[0] = os[0] + "rd";
            else
                os[0] = os[0] + "th";
            return String.format(DescriptionGenerator.descriptionMap.get(sr),os[0]);
        }
        return DescriptionGenerator.descriptionMap.get(sr) + os[0];
    }

    public String getDescriptionForOntologyQuery(SerializableOntologyQuery sq){
        OntologyQuery ontologyQuery = new OntologyQuery(sq);
        SugiliteRelation r = ontologyQuery.getR();
        if(ontologyQuery.getSubRelation() == OntologyQuery.relationType.nullR){
            // base case
            // this should have size 1 always, the array is only used in execution for when there's a query whose results are used as the objects of the next one
            SugiliteEntity[] objectArr = ontologyQuery.getObject().toArray(new SugiliteEntity[ontologyQuery.getObject().size()]);
            int objectSize = objectArr.length;
            String[] objectString = new String[objectSize];
            for(int i = 0; i < objectSize; i++){
                boolean flag = false;
                //System.out.println(objectArr[i].toString());
                for (String s: ObjectTranslation.objectMap.keySet())
                {
                    System.out.println("11111"+objectArr[i].toString());
                    if (objectArr[i].toString().equals(s))
                    {
                        System.out.println("22222"+true);
                        objectString[i] = ObjectTranslation.objectMap.get(s);
                        flag = true;
                    }
                }
                if (!flag) {
                    if (r.equals(SugiliteRelation.HAS_TEXT))
                        objectString[i] = setColor(objectArr[i].toString(),Const.SCRIPT_WITHIN_APP_COLOR);
                    else if (r.equals(SugiliteRelation.HAS_PACKAGE_NAME))
                        objectString[i] = getAppName(objectArr[i].toString());
                    else if (r.equals(SugiliteRelation.HAS_CLASS_NAME))
                        objectString[i] = "";
                    else
                        objectString[i] = objectArr[i].toString();
                }

            }
            return formatting(r,objectString);

        }

        OntologyQuery[] subQueryArray = ontologyQuery.getSubQueries().toArray(new OntologyQuery[ontologyQuery.getSubQueries().size()]);

        Arrays.sort(subQueryArray,RelationWeight.ontologyQueryComparator);

        if(ontologyQuery.getSubRelation() == OntologyQuery.relationType.AND || ontologyQuery.getSubRelation() == OntologyQuery.relationType.OR){
            int size = subQueryArray.length;
            String[] arr = new String[size];
            for(int i = 0; i < size; i++){
                SerializableOntologyQuery soq = new SerializableOntologyQuery(subQueryArray[i]);
                arr[i] = getDescriptionForOntologyQuery(soq);
                System.out.println("??????"+arr[i]);
            }


            if(ontologyQuery.getSubRelation() == OntologyQuery.relationType.AND){
                System.out.println("!!!!!!!" + TextUtils.join(" ", arr));
                return TextUtils.join(" ", arr);
            }

            else return TextUtils.join(" or ", arr);
        }

        // SubRelation == relationType.PREV
        SerializableOntologyQuery soq0 = new SerializableOntologyQuery(subQueryArray[0]);

        return r.getRelationName() + " " + getDescriptionForOntologyQuery(soq0);
    }

    public static void main(String[] args)
    {

        //System.out.println(test.getAppName("com.android.chrome"));
        //System.out.println(test.getAppNameFromPkgName(test.context,"com.android.chrome"));
    }

}
