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
        if (query instanceof CombinedOntologyQuery) {
            ((CombinedOntologyQuery) query).flattenConj();
        }
        return query;
    }

    public static OntologyQuery removeIsAllRelation(OntologyQuery query){
        // TODO this might not work recursively
        if (query != null && query instanceof CombinedOntologyQuery) {
            CombinedOntologyQuery coq = (CombinedOntologyQuery)query;
            if(coq.getSubRelation().equals(CombinedOntologyQuery.RelationType.AND)  && coq.getSubQueries() != null) {
                Iterator<OntologyQuery> iterator = coq.getSubQueries().iterator();
                while (iterator.hasNext()) {
                    OntologyQuery subQuery = iterator.next();
                    if(subQuery != null && subQuery instanceof LeafOntologyQuery) {
                        LeafOntologyQuery loq = (LeafOntologyQuery)subQuery;
                        if (loq.getR() != null && loq.getR().equals(SugiliteRelation.IS)) {
                            iterator.remove();
                            break;
                        }
                    }
                    else{
                        //coq.addSubQuery(removeIsAllRelation(subQuery));
                        //iterator.remove();
                    }
                }

                if(coq.getSubQueries().size() == 1){
                    OntologyQuery subQuery = new ArrayList<>(coq.getSubQueries()).get(0);

                    // coq.setSubjectSet(subQuery.getSubjectSet());
                    // coq.setObjectSet(subQuery.getObjectSet());
                    // coq.setSubRelation(subQuery.getSubRelation());
                    // coq.setQueryFunction(subQuery.getR());
                    // coq.setSubQueries(subQuery.getSubQueries());
                    return subQuery.clone();
                }
            }

        }

        return query;
    }

    public static OntologyQuery getQueryWithClassAndPackageConstraints(OntologyQuery query, Node clickedNode, boolean toAddClassQuery, boolean toAddPackageQuery, boolean toAddClickableQuery){
        //TODO: glue
        toAddClickableQuery = false;

        //de-serialize the query
        LeafOntologyQuery classQuery = null;
        LeafOntologyQuery packageQuery = null;
        LeafOntologyQuery clickableQuery = null;

        //TODO: consolidate nested conj
        query = consolidateNestedConj(query);
        query = removeIsAllRelation(query);

        //construct classQuery and packageQuery
        if (clickedNode.getClassName() != null) {
            classQuery = new LeafOntologyQuery();
            classQuery.addObject(new SugiliteEntity<>(-1, String.class, clickedNode.getClassName()));
            classQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
        }

        if (clickedNode.getPackageName() != null) {
            packageQuery = new LeafOntologyQuery();
            packageQuery.addObject(new SugiliteEntity<>(-1, String.class, clickedNode.getPackageName()));
            packageQuery.setQueryFunction(SugiliteRelation.HAS_PACKAGE_NAME);
        }

        if (clickedNode.getClickable() == true) {
            clickableQuery = new LeafOntologyQuery();
            clickableQuery.addObject(new SugiliteEntity<>(-1, Boolean.class, true));
            clickableQuery.setQueryFunction(SugiliteRelation.IS_CLICKABLE);
        }

        if(query != null && query instanceof CombinedOntologyQuery) {
            CombinedOntologyQuery coq = (CombinedOntologyQuery)query;
            if (coq.getSubRelation().equals(CombinedOntologyQuery.RelationType.AND)  && coq.getSubQueries() != null) {

                for (OntologyQuery query1 : coq.getSubQueries()) {
                    if (query1 != null && query1 instanceof LeafOntologyQuery) {
                        LeafOntologyQuery loq = (LeafOntologyQuery)query1;
                        if (loq.getR() != null) {
                            if (loq.getR().equals(SugiliteRelation.HAS_CLASS_NAME)) {
                                toAddClassQuery = false;
                            }
                            if (loq.getR().equals(SugiliteRelation.HAS_PACKAGE_NAME)) {
                                toAddPackageQuery = false;
                            }
                            if (loq.getR().equals(SugiliteRelation.IS_CLICKABLE)) {
                                toAddClickableQuery = false;
                            }
                        }
                    }
                }

                //add classQuery and packageQuery directly to query if query is of AND type
                if (classQuery != null && toAddClassQuery) {
                    coq.addSubQuery(classQuery);
                }
                if (packageQuery != null && toAddPackageQuery) {
                    coq.addSubQuery(packageQuery);
                }
                if (clickableQuery != null && toAddClickableQuery) {
                    coq.addSubQuery(clickableQuery);
                }

                return coq;
            }
        }

        // ELSE
        //create a parent query of AND type
        CombinedOntologyQuery parentQuery = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);

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
        CombinedOntologyQuery result = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);
        result.addSubQuery(query1.clone());
        result.addSubQuery(query2.clone());
        result.flattenConj();

//        if(query1.getSubRelation().equals(OntologyQueryWithSubQueries.RelationType.AND)){
//            for(OntologyQuery subQuery : query1.getSubQueries()){
//                if(! addedQueryString.contains(subQuery.toString())){
//                    result.addSubQuery(OntologyQuery.deserialize(subQuery.toString()));
//                    addedQueryString.add(subQuery.toString());
//                }
//            }
//        }
//        else{
//            result.addSubQuery(OntologyQuery.deserialize(query1.toString()));
//            addedQueryString.add(query1.toString());
//        }
//
//        if(query2.getSubRelation().equals(OntologyQueryWithSubQueries.RelationType.AND)){
//            for(OntologyQuery subQuery : query2.getSubQueries()){
//                if(! addedQueryString.contains(subQuery.toString())){
//                    result.addSubQuery(OntologyQuery.deserialize(subQuery.toString()));
//                    addedQueryString.add(subQuery.toString());
//                }
//            }
//        }
//        else{
//            result.addSubQuery(OntologyQuery.deserialize(query2.toString()));
//            addedQueryString.add(query2.toString());
//        }
//
//        //TODO: fix the combination of ontology query
//        if(query1.getOntologyQueryFilter() != null){
//            result.setOntologyQueryFilter(query1.getOntologyQueryFilter());
//        }
//
//        if(query2.getOntologyQueryFilter() != null){
//            result.setOntologyQueryFilter(query2.getOntologyQueryFilter());
//        }

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
