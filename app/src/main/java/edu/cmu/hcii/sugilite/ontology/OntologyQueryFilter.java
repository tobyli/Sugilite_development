package edu.cmu.hcii.sugilite.ontology;

import java.io.Serializable;

/**
 * @author toby
 * @date 3/22/18
 * @time 11:45 AM
 */
public class OntologyQueryFilter implements Serializable {
    public enum FilterType {
        ARG_MIN, ARG_MAX, CONTAINS
    }
    private FilterType filterType;
    private SugiliteRelation relation;
    public OntologyQueryFilter(FilterType filterType, SugiliteRelation relation){
        this.filterType = filterType;
        this.relation = relation;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public SugiliteRelation getRelation() {
        return relation;
    }

    public static OntologyQueryFilter deserialize (String s){
        String s1 = new String(s);
        if(s.startsWith("[") && s.endsWith("]") && s.length() > 1){
            s1 = s.substring(1, s.length() - 1);
        }
        String[] split = s1.split(" ");
        if(split.length == 2 && SugiliteRelation.stringRelationMap.containsKey(split[1])){
            SugiliteRelation relation = SugiliteRelation.stringRelationMap.get(split[1]);
            switch (split[0]){
                case "ARG_MAX":
                    return new OntologyQueryFilter(FilterType.ARG_MAX, relation);
                case "ARG_MIN":
                    return new OntologyQueryFilter(FilterType.ARG_MIN, relation);
                case "CONTAINS":
                    return new OntologyQueryFilter(FilterType.CONTAINS, relation);
                default:
                    return null;
            }

        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        switch (filterType){
            case ARG_MAX:
                return "[ARG_MAX " + relation.getRelationName() + "]";
            case ARG_MIN:
                return "[ARG_MIN " + relation.getRelationName() + "]";
            case CONTAINS:
                return "[CONTAINS " + relation.getRelationName() + "]";
        }
        return "";
    }
}
