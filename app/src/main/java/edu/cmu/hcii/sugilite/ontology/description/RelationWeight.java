package edu.cmu.hcii.sugilite.ontology.description;

import java.util.*;

import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * Created by Wanling Ding on 15/02/2018.
 */

public class RelationWeight {
    public static final HashMap<SugiliteRelation, Integer> weightMap;

    static {
        weightMap = new HashMap<SugiliteRelation, Integer>();

        weightMap.put(SugiliteRelation.HAS_CLASS_NAME,0);
        weightMap.put(SugiliteRelation.HAS_LIST_ORDER,1);
        weightMap.put(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER,2);
        weightMap.put(SugiliteRelation.HAS_TEXT,3);
        weightMap.put(SugiliteRelation.HAS_CHILD_TEXT,4);
        weightMap.put(SugiliteRelation.HAS_SIBLING_TEXT,5);
        weightMap.put(SugiliteRelation.HAS_CONTENT_DESCRIPTION,6);

        weightMap.put(SugiliteRelation.HAS_SCREEN_LOCATION,7);
        weightMap.put(SugiliteRelation.HAS_PACKAGE_NAME,101);

        weightMap.put(SugiliteRelation.HAS_PARENT,8);
        weightMap.put(SugiliteRelation.HAS_VIEW_ID,9);
        weightMap.put(SugiliteRelation.HAS_PARENT_LOCATION,10);
        weightMap.put(SugiliteRelation.HAS_CHILD,11);
        weightMap.put(SugiliteRelation.HAS_SIBLING,12);


        weightMap.put(SugiliteRelation.IS_EDITABLE,13);
        weightMap.put(SugiliteRelation.IS_CLICKABLE,14);
        weightMap.put(SugiliteRelation.IS_SCROLLABLE,15);
        weightMap.put(SugiliteRelation.IS_CHECKABLE,16);
        weightMap.put(SugiliteRelation.IS_CHECKED,17);
        weightMap.put(SugiliteRelation.IS_SELECTED,18);


        weightMap.put(SugiliteRelation.IS_A_LIST,19);

        weightMap.put(SugiliteRelation.CONTAINS_EMAIL_ADDRESS,20);
        weightMap.put(SugiliteRelation.CONTAINS_PHONE_NUMBER,21);
        weightMap.put(SugiliteRelation.CONTAINS_MONEY,22);
        weightMap.put(SugiliteRelation.CONTAINS_TIME,23);
        weightMap.put(SugiliteRelation.CONTAINS_DATE,24);
        weightMap.put(SugiliteRelation.CONTAINS_DURATION,25);
    }

    public int getWeight(SugiliteRelation r)
    {
        return weightMap.get(r);
    }


    static Comparator<SugiliteRelation> sugiliteRelationComparator = new Comparator<SugiliteRelation>() {
        @Override
        public int compare(SugiliteRelation r1, SugiliteRelation r2) {
            try {
                if (weightMap.get(r1) < weightMap.get(r2))
                    return -1;
                else if (weightMap.get(r1) > weightMap.get(r2))
                    return 1;
                else
                    return 0;
            }
            catch (Exception e){
                e.printStackTrace();
                return 0;
            }
        }
    };

    static Comparator<OntologyQuery> ontologyQueryComparator = new Comparator<OntologyQuery>() {
        @Override
        public int compare(OntologyQuery q1, OntologyQuery q2) {
            SugiliteRelation qr1 = q1.getR();
            SugiliteRelation qr2 = q2.getR();
            return sugiliteRelationComparator.compare(qr1,qr2);
        }
    };

}
