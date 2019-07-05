package edu.cmu.hcii.sugilite.ontology;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.Node;

/**
 * @author toby
 * @date 2/21/18
 * @time 3:05 PM
 */
public class OntologyQueryUtils {

    public static OntologyQuery getQueryWithClassAndPackageConstraints(OntologyQuery query, Node clickedNode){
        return getQueryWithClassAndPackageConstraints(query, clickedNode, true, true, true);
    }

    /**
     * consolidate nested conjs in queries (e.g. (conj xx (conj yy zz)) to (conj xx yy zz))
     * @param query
     * @return
     */
    public static OntologyQuery consolidateNestedConj(OntologyQuery query){
        if(query != null && query.getSubRelation().equals(OntologyQuery.relationType.AND)  && query.getSubQueries() != null) {
            Iterator<OntologyQuery> iterator = query.getSubQueries().iterator();
            Set<OntologyQuery> setToRemove = new HashSet<>();
            Set<OntologyQuery> setToAdd = new HashSet<>();
            while (iterator.hasNext()) {
                OntologyQuery subQuery = iterator.next();
                if(subQuery != null && subQuery.getSubRelation().equals(OntologyQuery.relationType.AND) && subQuery.getSubQueries() != null){
                    setToAdd.addAll(getBreakdownForNestedConj(subQuery));
                    setToRemove.add(subQuery);
                }
            }
            query.getSubQueries().removeAll(setToRemove);
            query.getSubQueries().addAll(setToAdd);
        }
        return query;
    }

    private static Set<OntologyQuery> getBreakdownForNestedConj(OntologyQuery query) {
        Set<OntologyQuery> results = new HashSet<>();
        if (query != null && query.getSubRelation().equals(OntologyQuery.relationType.AND) && query.getSubQueries() != null) {
            for(OntologyQuery subQuery : query.getSubQueries()){
                results.addAll(getBreakdownForNestedConj(subQuery));
            }
            return results;
        }

        else{
            results.add(query);
            return results;
        }
    }

    public static OntologyQuery removeIsAllRelation(OntologyQuery query){
        if(query != null && query.getSubRelation().equals(OntologyQuery.relationType.AND)  && query.getSubQueries() != null) {
            Iterator<OntologyQuery> iterator = query.getSubQueries().iterator();
            while (iterator.hasNext()) {
                OntologyQuery subQuery = iterator.next();
                if(subQuery != null && subQuery.getSubRelation().equals(OntologyQuery.relationType.nullR) && subQuery.getR() != null && subQuery.getR().equals(SugiliteRelation.IS)){
                    iterator.remove();
                    break;
                }
                else{
                    //query.addSubQuery(removeIsAllRelation(subQuery));
                    //iterator.remove();
                }
            }

            if(query.getSubQueries().size() == 1){
                OntologyQuery subQuery = new ArrayList<>(query.getSubQueries()).get(0);

                query.setSubject(subQuery.getSubject());
                query.setObject(subQuery.getObject());
                query.setSubRelation(subQuery.getSubRelation());
                query.setQueryFunction(subQuery.getR());
                query.setSubQueries(subQuery.getSubQueries());
            }
        }


        return query;
    }

    public static OntologyQuery getQueryWithClassAndPackageConstraints(OntologyQuery query, Node clickedNode, boolean toAddClassQuery, boolean toAddPackageQuery, boolean toAddClickableQuery){
        //TODO: glue
        toAddClickableQuery = false;

        //de-serialize the query
        OntologyQuery classQuery = null;
        OntologyQuery packageQuery = null;
        OntologyQuery clickableQuery = null;

        //TODO: consolidate nested conj
        query = consolidateNestedConj(query);
        query = removeIsAllRelation(query);

        //construct classQuery and packageQuery
        if (clickedNode.getClassName() != null) {
            classQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            classQuery.addObject(new SugiliteEntity<>(-1, String.class, clickedNode.getClassName()));
            classQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
        }

        if (clickedNode.getPackageName() != null) {
            packageQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            packageQuery.addObject(new SugiliteEntity<>(-1, String.class, clickedNode.getPackageName()));
            packageQuery.setQueryFunction(SugiliteRelation.HAS_PACKAGE_NAME);
        }

        if (clickedNode.getClickable() == true) {
            clickableQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            clickableQuery.addObject(new SugiliteEntity<>(-1, Boolean.class, true));
            clickableQuery.setQueryFunction(SugiliteRelation.IS_CLICKABLE);
        }

        if(query != null && query.getSubRelation().equals(OntologyQuery.relationType.AND)  && query.getSubQueries() != null){

            for(OntologyQuery query1 : query.getSubQueries()){
                if(query1 != null && query1.getR() != null){
                    if(query1.getR().equals(SugiliteRelation.HAS_CLASS_NAME)){
                        toAddClassQuery = false;
                    }
                    if(query1.getR().equals(SugiliteRelation.HAS_PACKAGE_NAME)){
                        toAddPackageQuery = false;
                    }
                    if(query1.getR().equals(SugiliteRelation.IS_CLICKABLE)){
                        toAddClickableQuery = false;
                    }
                }
            }

            //add classQuery and packageQuery directly to query if query is of AND type
            if(classQuery != null && toAddClassQuery) {
                query.addSubQuery(classQuery);
            }
            if(packageQuery != null && toAddPackageQuery) {
                query.addSubQuery(packageQuery);
            }
            if(clickableQuery != null && toAddClickableQuery) {
                query.addSubQuery(clickableQuery);
            }

            return query;
        }

        else {
            //create a parent query of AND type
            OntologyQuery parentQuery = new OntologyQuery(OntologyQuery.relationType.AND);

            if (query.getOntologyQueryFilter() != null) {
                parentQuery.setOntologyQueryFilter(query.getOntologyQueryFilter());
                query.setOntologyQueryFilter(null);
            }
            if(query != null) {
                parentQuery.addSubQuery(query);
            }
            if(classQuery != null && toAddClassQuery) {
                parentQuery.addSubQuery(classQuery);
            }
            if(packageQuery != null && toAddPackageQuery) {
                parentQuery.addSubQuery(packageQuery);
            }
            if(clickableQuery != null && toAddClickableQuery) {
                parentQuery.addSubQuery(clickableQuery);
            }

            return parentQuery;
        }
    }

    public static boolean isSameNode(Node a, Node b) {
        if (a.getClassName() != null && (!a.getClassName().equals(b.getClassName()))) {
            return false;
        }
        if (a.getPackageName() != null && (!a.getPackageName().equals(b.getPackageName()))) {
            return false;
        }
        if (a.getBoundsInScreen() != null && (!a.getBoundsInScreen().equals(b.getBoundsInScreen()))) {
            return false;
        }
        return true;
    }

    public static OntologyQuery combineTwoQueries(OntologyQuery query1, OntologyQuery query2){
        Set<String> addedQueryString = new HashSet<>();
        if(query1.toString().equals(query2.toString())){
            return query1;
        }
        OntologyQuery result = new OntologyQuery(OntologyQuery.relationType.AND);

        if(query1.getSubRelation().equals(OntologyQuery.relationType.AND)){
            for(OntologyQuery subQuery : query1.getSubQueries()){
                if(! addedQueryString.contains(subQuery.toString())){
                    result.addSubQuery(OntologyQuery.deserialize(subQuery.toString()));
                    addedQueryString.add(subQuery.toString());
                }
            }
        }
        else{
            result.addSubQuery(OntologyQuery.deserialize(query1.toString()));
            addedQueryString.add(query1.toString());
        }

        if(query2.getSubRelation().equals(OntologyQuery.relationType.AND)){
            for(OntologyQuery subQuery : query2.getSubQueries()){
                if(! addedQueryString.contains(subQuery.toString())){
                    result.addSubQuery(OntologyQuery.deserialize(subQuery.toString()));
                    addedQueryString.add(subQuery.toString());
                }
            }
        }
        else{
            result.addSubQuery(OntologyQuery.deserialize(query2.toString()));
            addedQueryString.add(query2.toString());
        }

        //TODO: fix the combination of ontology query
        if(query1.getOntologyQueryFilter() != null){
            result.setOntologyQueryFilter(query1.getOntologyQueryFilter());
        }

        if(query2.getOntologyQueryFilter() != null){
            result.setOntologyQueryFilter(query2.getOntologyQueryFilter());
        }

        return result;
    }

    public static String removeQuoteSigns(String string){
        if(string.startsWith("\"") && string.endsWith("\"")){
            return string.substring(1, string.length() - 1);
        } else{
            return string;
        }
    }

    public static Set<SugiliteEntity<String>> getAllStringEntitiesWithHasTextAndHasChildTextRelations(SugiliteEntity entity, UISnapshot uiSnapshot){
        Set<SugiliteEntity<String>> allStringEntities = new HashSet<>();
        Set<SugiliteTriple> hasTextRelationTriples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(entity.getEntityId(), SugiliteRelation.HAS_TEXT.getRelationId()));
        Set<SugiliteTriple> hasChildTextRelationTriples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(entity.getEntityId(), SugiliteRelation.HAS_CHILD_TEXT.getRelationId()));
        if(hasTextRelationTriples != null) {
            for (SugiliteTriple triple : hasTextRelationTriples) {
                if (triple.getObject() != null && triple.getObject().getEntityValue() instanceof String) {
                    allStringEntities.add(triple.getObject());
                }
            }
        }
        if(hasChildTextRelationTriples != null) {
            for (SugiliteTriple triple : hasChildTextRelationTriples) {
                if (triple.getObject() != null && triple.getObject().getEntityValue() instanceof String) {
                    allStringEntities.add(triple.getObject());
                }
            }
        }
        return allStringEntities;
    }
}
