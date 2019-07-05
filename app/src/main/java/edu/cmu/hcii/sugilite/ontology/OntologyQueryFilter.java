package edu.cmu.hcii.sugilite.ontology;

import android.util.Pair;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.Node;

/**
 * @author toby
 * @date 3/22/18
 * @time 11:45 AM
 */
public class OntologyQueryFilter implements Serializable {
    public enum FilterType {
        ARG_MIN, ARG_MAX, EXISTS
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

    /**
     * create an OntologyQueryFilter from a string
     * @param s
     * @return
     */
    public static OntologyQueryFilter deserialize (String s){
        String s1 = new String(s);
        String[] split = s1.split(" ");
        if(split.length == 2 && SugiliteRelation.stringRelationMap.containsKey(split[1])){
            SugiliteRelation relation = SugiliteRelation.stringRelationMap.get(split[1]);
            switch (split[0]){
                case "argmax":
                    return new OntologyQueryFilter(FilterType.ARG_MAX, relation);
                case "argmin":
                    return new OntologyQueryFilter(FilterType.ARG_MIN, relation);
                case "exists":
                    return new OntologyQueryFilter(FilterType.EXISTS, relation);
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
                return "argmax " + relation.getRelationName();
            case ARG_MIN:
                return "argmin " + relation.getRelationName();
            case EXISTS:
                return "exists " + relation.getRelationName();
        }
        return "";
    }

    /**
     * execute the filter on the result of an OntologyQuery to return a subset of SugiliteEntity
     * @param sugiliteEntities
     * @param uiSnapshot
     * @return
     */

    public Set<SugiliteEntity> filter(Set<SugiliteEntity> sugiliteEntities, UISnapshot uiSnapshot){
        Set<SugiliteEntity> results = new HashSet<>();
        if(sugiliteEntities.isEmpty()){
            return sugiliteEntities;
        }

        if (filterType == FilterType.EXISTS) {
            for (SugiliteEntity entity : sugiliteEntities) {
                Set<SugiliteTriple> allMatchedEntities = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(entity.getEntityId(), relation.getRelationId()));
                if(allMatchedEntities == null || allMatchedEntities.isEmpty()){
                    continue;
                }
                for(SugiliteTriple triple : allMatchedEntities){
                    if(triple != null && triple.getSubject() != null){
                        results.add(triple.getSubject());
                    }
                }
                return results;
            }
        }

        else if (filterType == FilterType.ARG_MAX || filterType == FilterType.ARG_MIN){
            List<Pair<SugiliteEntity, Comparable>> entityWithObjectValues = new ArrayList<>();

            for(SugiliteEntity entity : sugiliteEntities){


                //*** temporarily only consider clickable items in argmin/max
                if(entity.getEntityValue() instanceof Node){
                    if(((Node) entity.getEntityValue()).getClickable() == false){
                        continue;
                    }
                }
                //***

                Set<SugiliteTriple> allMatchedTriples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(entity.getEntityId(), relation.getRelationId()));
                if(allMatchedTriples == null || allMatchedTriples.isEmpty()){
                    continue;
                }
                for(SugiliteTriple matchedTriple : allMatchedTriples) {
                    SugiliteEntity object = matchedTriple.getObject();
                    if (object.getEntityValue() instanceof Comparable) {
                        entityWithObjectValues.add(new Pair<>(entity, (Comparable) object.getEntityValue()));
                    }
                }
            }

            //sort the entityWithObjectValues list
            Collections.sort(entityWithObjectValues, new Comparator<Pair<SugiliteEntity, Comparable>>() {
                @Override
                public int compare(Pair<SugiliteEntity, Comparable> o1, Pair<SugiliteEntity, Comparable> o2) {
                    try{
                        return Double.valueOf(o1.second.toString()).compareTo(Double.valueOf(o2.second.toString()));
                    }

                    catch (Exception e){
                        return o1.second.compareTo(o2.second);
                    }
                }
            });

            Set<SugiliteEntity> result = new HashSet<>();
            if(entityWithObjectValues.size() > 0) {
                if (filterType == FilterType.ARG_MIN) {
                    Comparable value = entityWithObjectValues.get(0).second;
                    for(Pair<SugiliteEntity, Comparable> pair : entityWithObjectValues){
                        if(pair.second.compareTo(value) == 0){
                            result.add(pair.first);
                        }
                    }
                } else if (filterType == FilterType.ARG_MAX) {
                    Comparable value = entityWithObjectValues.get(entityWithObjectValues.size() - 1).second;
                    for(Pair<SugiliteEntity, Comparable> pair : entityWithObjectValues){
                        if(pair.second.compareTo(value) == 0){
                            result.add(pair.first);
                        }
                    }
                }
            }
            return result;
        }

        return sugiliteEntities;
    }

    /*
    public Set<SugiliteEntity> filter(Set<SugiliteEntity> sugiliteEntities, UISnapshot uiSnapshot){
        Set<SugiliteEntity> results = new HashSet<>();
        if(sugiliteEntities.isEmpty()){
            return sugiliteEntities;
        }

        if (filterType == FilterType.EXISTS) {
            for (SugiliteEntity entity : sugiliteEntities) {
                //get all the string entities that have either hasText or HAS_CHILD_TEXT relation with the entity
                Set<SugiliteEntity<String>> allStringEntities = OntologyQueryUtils.getAllStringEntitiesWithHasTextAndHasChildTextRelations(entity, uiSnapshot);

                //search in all string entities
                for(SugiliteEntity<String> stringEntity : allStringEntities) {
                    //check whether the entity contains the relation
                    Set<SugiliteTriple> filteredTriples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(stringEntity.getEntityId(), relation.getRelationId()));
                    if (filteredTriples != null && filteredTriples.size() > 0) {
                        results.add(entity);
                    }
                }
                return results;
            }
        }

        else if (filterType == FilterType.ARG_MAX || filterType == FilterType.ARG_MIN){
            List<Pair<SugiliteEntity, Comparable>> entityWithObjectValues = new ArrayList<>();

            for(SugiliteEntity entity : sugiliteEntities){
                //get all the string entities that have either hasText or HAS_CHILD_TEXT relation with the entity
                Set<SugiliteEntity<String>> allStringEntities = OntologyQueryUtils.getAllStringEntitiesWithHasTextAndHasChildTextRelations(entity, uiSnapshot);

                //search in all string entities
                for(SugiliteEntity<String> stringEntity : allStringEntities) {
                    Set<SugiliteTriple> filteredTriples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(stringEntity.getEntityId(), relation.getRelationId()));
                    if (filteredTriples != null) {
                        for (SugiliteTriple filteredTriple : filteredTriples) {
                            SugiliteEntity object = filteredTriple.getObject();
                            if (object.getEntityValue() instanceof Comparable) {
                                entityWithObjectValues.add(new Pair<>(entity, (Comparable) object.getEntityValue()));
                            }
                        }
                    }
                }
            }

            //sort the entityWithObjectValues list
            Collections.sort(entityWithObjectValues, new Comparator<Pair<SugiliteEntity, Comparable>>() {
                @Override
                public int compare(Pair<SugiliteEntity, Comparable> o1, Pair<SugiliteEntity, Comparable> o2) {
                    try{
                        return Double.valueOf(o1.second.toString()).compareTo(Double.valueOf(o2.second.toString()));
                    }

                    catch (Exception e){
                        return o1.second.compareTo(o2.second);
                    }
                }
            });

            Set<SugiliteEntity> result = new HashSet<>();
            if(entityWithObjectValues.size() > 0) {
                if (filterType == FilterType.ARG_MIN) {
                    Comparable value = entityWithObjectValues.get(0).second;
                    for(Pair<SugiliteEntity, Comparable> pair : entityWithObjectValues){
                        if(pair.second.compareTo(value) == 0){
                            result.add(pair.first);
                        }
                    }
                } else if (filterType == FilterType.ARG_MAX) {
                    Comparable value = entityWithObjectValues.get(entityWithObjectValues.size() - 1).second;
                    for(Pair<SugiliteEntity, Comparable> pair : entityWithObjectValues){
                        if(pair.second.compareTo(value) == 0){
                            result.add(pair.first);
                        }
                    }
                }
            }
            return result;
        }

        return sugiliteEntities;
    }
    */
}
