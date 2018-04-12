package edu.cmu.hcii.sugilite.ontology;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.Node;

/**
 * @author toby
 * @date 2/21/18
 * @time 3:05 PM
 */
public class OntologyQueryUtils {

    public static OntologyQuery getQueryWithClassAndPackageConstraints(OntologyQuery query, Node clickedNode){
        return getQueryWithClassAndPackageConstraints(query, clickedNode, true, true);
    }
    public static OntologyQuery getQueryWithClassAndPackageConstraints(OntologyQuery query, Node clickedNode, boolean toAddClassQuery, boolean toAddPackageQuery){
        //de-serialize the query
        OntologyQuery classQuery = null;
        OntologyQuery packageQuery = null;

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

        if(query != null && query.getSubRelation() == OntologyQuery.relationType.AND && query.getSubQueries() != null){
            for(OntologyQuery query1 : query.getSubQueries()){
                if(query1 != null){
                    if(query1.getR().equals(SugiliteRelation.HAS_CLASS_NAME)){
                        toAddClassQuery = false;
                    }
                    if(query1.getR().equals(SugiliteRelation.HAS_PACKAGE_NAME)){
                        toAddPackageQuery = false;
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
