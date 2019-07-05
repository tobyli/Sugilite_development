package edu.cmu.hcii.sugilite.ontology;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Created by nancyli on 9/27/17.
 */

public class CombinedOntologyQuery extends OntologyQuery {
    public enum RelationType {
        AND, OR, PREV
    }
    private RelationType subRelation;
    private Set<OntologyQuery> subQueries = null;
    private transient BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> QueryFunction = null;

    public CombinedOntologyQuery(){
        subQueries = new HashSet<OntologyQuery>();
    }

    public CombinedOntologyQuery(SerializableOntologyQuery sq) {
        subRelation = sq.getSubRelation();
        setOntologyQueryFilter(sq.getOntologyQueryFilter());
        r = sq.getR();
        if(getR() != null){
            setQueryFunction(getR());
        }
        subQueries = new HashSet<OntologyQuery>();
        Set<SerializableOntologyQuery> pSubq = sq.getSubQueries();
        for(SerializableOntologyQuery s : pSubq) {
            subQueries.add(new CombinedOntologyQuery(s));
        }
//        else{
//            Set<SugiliteSerializableEntity> so = sq.getObject();
//            Set<SugiliteSerializableEntity> ss = sq.getSubject();
//            if(so != null){
//                object = new HashSet<>();
//                for(SugiliteSerializableEntity se : so){
//                    object.add(new SugiliteEntity(se));
//                }
//            }
//        }
    }

    public CombinedOntologyQuery(RelationType r){
        this.subRelation = r;
        // there are sub-queries
        this.subQueries = new HashSet<OntologyQuery>();
        // NOTE this constructor might cause bugs with QueryFuction
    }

    public void addSubQuery(OntologyQuery sub){
        /*
        if(BuildConfig.DEBUG && !(subRelation != RelationType.nullR && subQueries != null)){
            throw new AssertionError();
        }
        */
        subQueries.add(sub);
    }

    public void setQueryFunction(BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> f, SugiliteRelation r){
        /*
        if(BuildConfig.DEBUG && !(subRelation == RelationType.nullR)){
            throw new AssertionError();
        }
        */
        QueryFunction = f;
        this.r = r;
    }

    public void setQueryFunction(SugiliteRelation relation){
        r = relation;
        QueryFunction = new BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean>() {
            @Override
            public Boolean apply(SubjectEntityObjectEntityPair pair, UISnapshot graph) {
                SugiliteEntity s = pair.getSubject();
                SugiliteEntity o = pair.getObject();
                SugiliteTriple newTriple = new SugiliteTriple(s, relation, o);
                Integer sID = s.getEntityId();
                Integer oID = o.getEntityId();
                if(sID != -1) {
                    Set<SugiliteTriple> subjectTriples = graph.getSubjectTriplesMap().get(sID);
                    if (subjectTriples == null) return false;
                    return subjectTriples.contains(newTriple);
                }
                else{
                    Set<SugiliteTriple> objectTriples = graph.getObjectTriplesMap().get(oID);
                    if (objectTriples == null) return false;
                    return objectTriples.contains(newTriple);
                }
            }
        };
    }

    public void setSubRelation(RelationType subRelation) {
        this.subRelation = subRelation;
    }

    public void setSubQueries(Set<OntologyQuery> subQueries) {
        /*
        if(BuildConfig.DEBUG && !(subRelation != RelationType.nullR)){
            throw new AssertionError();
        }
        */
        this.subQueries = subQueries;
    }

    public RelationType getSubRelation() {return this.subRelation;}

    public Set<OntologyQuery> getSubQueries() {return this.subQueries;}

    public BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> getQueryFunction() {return this.QueryFunction;}

    @Override
    public CombinedOntologyQuery clone() {
        CombinedOntologyQuery q = new CombinedOntologyQuery();
        q.setSubRelation(subRelation);
        q.setOntologyQueryFilter(getOntologyQueryFilter());
        if (getR() != null) {
            q.setQueryFunction(getR());
        }
        for (OntologyQuery subQ : subQueries) {
            q.addSubQuery(subQ.clone());
        }
        return q;
    }

    /**
     * the query function used for determine whether a node matches the query
     * @param currNode
     * @param graph
     * @return
     */
    @Override
    protected boolean OverallQueryFunction(SugiliteEntity currNode, UISnapshot graph) {
        if(subRelation == RelationType.AND){
            for(OntologyQuery q : subQueries){
                if(!q.OverallQueryFunction(currNode, graph)){
                    return false;
                }
            }
            return true;
        }
        else if(subRelation == RelationType.OR){
            for(OntologyQuery q : subQueries){
                if(q.OverallQueryFunction(currNode, graph)){
                    return true;
                }
            }
            return false;
        }
        else{
            // subRelation == RelationType.PREV
            // only one subquery
            // TODO not sure whats going on here
            OntologyQuery prevQ = subQueries.toArray(new OntologyQuery[subQueries.size()])[0];
            // Set<SugiliteEntity> prevResult = prevQ.executeOn(graph);
            // setSubQueries(null);
            // setSubRelation(RelationType.nullR);
            // setObjectSet(prevResult);
            return prevQ.OverallQueryFunction(currNode, graph);
        }
    }

    @Override
    public String toString() {
        String baseQueryString = "";

        OntologyQuery[] subQueryArray = subQueries.toArray(new OntologyQuery[subQueries.size()]);
        if (subRelation == RelationType.AND || subRelation == RelationType.OR) {
            int size = subQueryArray.length;
            String[] arr = new String[size];
            for (int i = 0; i < size; i++) {
                arr[i] = subQueryArray[i].toString();
            }
            String joined = Arrays.asList(arr).stream().collect(Collectors.joining(" "));
            if (subRelation == RelationType.AND) {
                baseQueryString = "(conj " + joined + ")";
            } else {
                baseQueryString = "(or " + joined + ")";
            }
        }
        else if (subRelation == RelationType.PREV){
            baseQueryString = "(" + getR().getRelationName() + " " + subQueryArray[0].toString() + ")";
        }

        //include the ontologyQueryFilter in the toString() method
        if(getOntologyQueryFilter() == null) {
            return baseQueryString;
        } else{
            return "(" + getOntologyQueryFilter().toString() + " " + baseQueryString + ")";
        }
    }

    public void flattenConj() {
        if (getSubRelation().equals(CombinedOntologyQuery.RelationType.AND) && getSubQueries() != null) {
            Iterator<OntologyQuery> iterator = getSubQueries().iterator();
            Set<OntologyQuery> setToRemove = new HashSet<>();
            Set<OntologyQuery> setToAdd = new HashSet<>();
            while (iterator.hasNext()) {
                OntologyQuery subQuery = iterator.next();
                if (subQuery != null && subQuery instanceof CombinedOntologyQuery) {
                    CombinedOntologyQuery subCoq = (CombinedOntologyQuery)subQuery;
                    if (subCoq.getSubRelation().equals(CombinedOntologyQuery.RelationType.AND) && subCoq.getSubQueries() != null) {
                        setToAdd.addAll(getBreakdownForNestedConj(subCoq));
                        setToRemove.add(subQuery);
                    }
                }
            }
            getSubQueries().removeAll(setToRemove);
            getSubQueries().addAll(setToAdd);
        }
    }

    private static Set<OntologyQuery> getBreakdownForNestedConj(OntologyQuery query) {
        Set<OntologyQuery> results = new HashSet<>();
        if (query != null && query instanceof CombinedOntologyQuery) {
            CombinedOntologyQuery coq = (CombinedOntologyQuery) query;
            if (coq.getSubRelation().equals(CombinedOntologyQuery.RelationType.AND) && coq.getSubQueries() != null) {
                for (OntologyQuery subQuery : coq.getSubQueries()) {
                    results.addAll(getBreakdownForNestedConj(subQuery));
                }
            } else {
                results.add(query);
            }
        } else {
            results.add(query);
        }
        return results;
    }

}