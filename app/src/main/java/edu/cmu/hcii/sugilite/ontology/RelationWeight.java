package edu.cmu.hcii.sugilite.ontology;

import java.util.*;

/**
 * Created by Wanling Ding on 15/02/2018.
 */

public class RelationWeight {
    public static final HashMap<SugiliteRelation, Integer> weightMap;

    static {
        weightMap = new HashMap<SugiliteRelation, Integer>();

        weightMap.put(SugiliteRelation.HAS_CLASS_NAME,0);
        weightMap.put(SugiliteRelation.HAS_TEXT,1);
        weightMap.put(SugiliteRelation.HAS_CHILD_TEXT,1);
        weightMap.put(SugiliteRelation.HAS_SIBLING_TEXT,1);
        weightMap.put(SugiliteRelation.HAS_CONTENT_DESCRIPTION,1);

        weightMap.put(SugiliteRelation.HAS_SCREEN_LOCATION,2);
        weightMap.put(SugiliteRelation.HAS_PACKAGE_NAME,101);
        weightMap.put(SugiliteRelation.HAS_LIST_ORDER,1);
        weightMap.put(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER,1);


        weightMap.put(SugiliteRelation.HAS_PARENT,100);
        weightMap.put(SugiliteRelation.HAS_VIEW_ID,100);
        weightMap.put(SugiliteRelation.HAS_PARENT_LOCATION,100);
        weightMap.put(SugiliteRelation.HAS_CHILD,100);
        weightMap.put(SugiliteRelation.HAS_SIBLING,100);


        weightMap.put(SugiliteRelation.IS_EDITABLE,100);
        weightMap.put(SugiliteRelation.IS_CLICKABLE,100);
        weightMap.put(SugiliteRelation.IS_SCROLLABLE,100);
        weightMap.put(SugiliteRelation.IS_CHECKABLE,100);
        weightMap.put(SugiliteRelation.IS_CHECKED,100);
        weightMap.put(SugiliteRelation.IS_SELECTED,100);


        weightMap.put(SugiliteRelation.IS_A_LIST,100);

        weightMap.put(SugiliteRelation.CONTAINS_EMAIL_ADDRESS,100);
        weightMap.put(SugiliteRelation.CONTAINS_PHONE_NUMBER,100);
        weightMap.put(SugiliteRelation.CONTAINS_MONEY,100);
        weightMap.put(SugiliteRelation.CONTAINS_TIME,100);
        weightMap.put(SugiliteRelation.CONTAINS_DATE,100);
        weightMap.put(SugiliteRelation.CONTAINS_DURATION,100);
    }

    public int getWeight(SugiliteRelation r)
    {
        return weightMap.get(r);
    }


    static Comparator<SugiliteRelation> sugiliteRelationComparator = new Comparator<SugiliteRelation>() {
        @Override
        public int compare(SugiliteRelation r1, SugiliteRelation r2) {
            if (weightMap.get(r1) < weightMap.get(r2))
                return -1;
            else if (weightMap.get(r1) > weightMap.get(r2))
                return 1;
            else
                return 0;        }
    };

    static Comparator<OntologyQuery> ontologyQueryComparator = new Comparator<OntologyQuery>() {
        @Override
        public int compare(OntologyQuery q1, OntologyQuery q2) {
            SugiliteRelation qr1 = q1.getR();
            SugiliteRelation qr2 = q2.getR();
            return sugiliteRelationComparator.compare(qr1,qr2);
        }
    };


    public String toString()
    {
        String result = "";
        for (SugiliteRelation r: weightMap.keySet())
        {
            result += r.getRelationName() + " " + weightMap.get(r) + "\n";
        }
        return result;
    }

    public static void main(String[] args)
    {
        RelationWeight test = new RelationWeight();
        System.out.println(test);
        //System.out.println(compare(SugiliteRelation.HAS_CLASS_NAME,SugiliteRelation.HAS_PACKAGE_NAME));
        SugiliteRelation[] testlist = new SugiliteRelation[3];
        testlist[0] = SugiliteRelation.HAS_TEXT;
        testlist[1] = SugiliteRelation.HAS_PACKAGE_NAME;
        testlist[2] = SugiliteRelation.HAS_CLASS_NAME;
        for (SugiliteRelation sr:testlist)
            System.out.print(sr.getRelationName()+" ");
        System.out.println();
        Arrays.sort(testlist, sugiliteRelationComparator);
        for (SugiliteRelation sr:testlist)
            System.out.print(sr.getRelationName()+" ");
    }
}
