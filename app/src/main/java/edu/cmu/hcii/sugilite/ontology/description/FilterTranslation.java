package edu.cmu.hcii.sugilite.ontology.description;

/**
 * Created by Bell on 29/03/2018.
 */

import java.util.*;

import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.OntologyQueryFilter;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

public class FilterTranslation {
    public static final HashMap<OntologyQueryFilter,String> filterMap;
    static {
        filterMap = new HashMap<OntologyQueryFilter,String>();
        OntologyQueryFilter.FilterType max = OntologyQueryFilter.FilterType.ARG_MAX;
        OntologyQueryFilter.FilterType min = OntologyQueryFilter.FilterType.ARG_MIN;

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.HAS_LIST_ORDER),"last");
        filterMap.put(new OntologyQueryFilter(min,SugiliteRelation.HAS_LIST_ORDER),"first");

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER),"last");
        filterMap.put(new OntologyQueryFilter(min,SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER),"first");

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.CONTAINS_MONEY),"most expensive");
        filterMap.put(new OntologyQueryFilter(min,SugiliteRelation.CONTAINS_MONEY),"cheapest");

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.CONTAINS_TIME),"latest");
        filterMap.put(new OntologyQueryFilter(min,SugiliteRelation.CONTAINS_TIME),"earliest");

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.CONTAINS_DATE),"latest");
        filterMap.put(new OntologyQueryFilter(min,SugiliteRelation.CONTAINS_DATE),"earliest");

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.CONTAINS_DURATION),"longest");
        filterMap.put(new OntologyQueryFilter(min,SugiliteRelation.CONTAINS_DURATION),"shortest");

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.CONTAINS_LENGTH),"longest");
        filterMap.put(new OntologyQueryFilter(min,SugiliteRelation.CONTAINS_LENGTH),"shortest");

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.CONTAINS_PERCENTAGE),"biggest");
        filterMap.put(new OntologyQueryFilter(min,SugiliteRelation.CONTAINS_PERCENTAGE),"smallest");

        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.CONTAINS_VOLUME),"biggest");
        filterMap.put(new OntologyQueryFilter(max,SugiliteRelation.CONTAINS_VOLUME),"smallest");
//        HashMap<OntologyQueryFilter.FilterType,String> list_order_filter = new HashMap<OntologyQueryFilter.FilterType,String>();
//        list_order_filter.put(OntologyQueryFilter.FilterType.ARG_MAX,"last");
//        list_order_filter.put(OntologyQueryFilter.FilterType.ARG_MIN,"first");
//        filterMap.put(SugiliteRelation.HAS_LIST_ORDER,list_order_filter);
//        filterMap.put(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER,list_order_filter);

//        HashMap<OntologyQueryFilter.FilterType,String> money_filter = new HashMap<OntologyQueryFilter.FilterType,String>();
//        money_filter.put(OntologyQueryFilter.FilterType.ARG_MAX,"most expensive");
//        money_filter.put(OntologyQueryFilter.FilterType.ARG_MIN,"cheapest");
//        filterMap.put(SugiliteRelation.CONTAINS_MONEY,money_filter);

//        HashMap<OntologyQueryFilter.FilterType,String> time_filter = new HashMap<OntologyQueryFilter.FilterType,String>();
//        time_filter.put(OntologyQueryFilter.FilterType.ARG_MAX,"latest");
//        time_filter.put(OntologyQueryFilter.FilterType.ARG_MIN,"earliest");
//        filterMap.put(SugiliteRelation.CONTAINS_TIME,time_filter);
//        filterMap.put(SugiliteRelation.CONTAINS_DATE,time_filter);

//        HashMap<OntologyQueryFilter.FilterType,String> length_filter = new HashMap<OntologyQueryFilter.FilterType,String>();
//        length_filter.put(OntologyQueryFilter.FilterType.ARG_MAX,"longest");
//        length_filter.put(OntologyQueryFilter.FilterType.ARG_MIN,"shortest");
//        filterMap.put(SugiliteRelation.CONTAINS_DURATION,length_filter);
//        filterMap.put(SugiliteRelation.CONTAINS_LENGTH,length_filter);

//        HashMap<OntologyQueryFilter.FilterType,String> size_filter = new HashMap<OntologyQueryFilter.FilterType,String>();
//        size_filter.put(OntologyQueryFilter.FilterType.ARG_MAX,"biggest");
//        size_filter.put(OntologyQueryFilter.FilterType.ARG_MIN,"smallest");
//        filterMap.put(SugiliteRelation.CONTAINS_PERCENTAGE,length_filter);
//        filterMap.put(SugiliteRelation.CONTAINS_VOLUME,length_filter);
    }

    public static String getFilterTranslation(OntologyQueryFilter f) {
        for (OntologyQueryFilter i:filterMap.keySet())
        {
            if (i.getFilterType().equals(f.getFilterType()) && i.getRelation().equals(f.getRelation())) {
                return filterMap.get(i);
            }
        }
        if (f.getFilterType().equals(OntologyQueryFilter.FilterType.ARG_MAX))
            return "maximum";
        else
            return "minimum";
    }

    public String toString() {
        String result = "";
        for (OntologyQueryFilter i:filterMap.keySet())
        {
            result += i.toString() + ": " + filterMap.get(i) + "\n";
        }
        return result;
    }

    public static void main(String[] args){
        FilterTranslation test = new FilterTranslation();
        //System.out.println(test);
        OntologyQueryFilter f = new OntologyQueryFilter(OntologyQueryFilter.FilterType.ARG_MIN,SugiliteRelation.CONTAINS_MONEY);
        System.out.println(getFilterTranslation(f));
    }
}
