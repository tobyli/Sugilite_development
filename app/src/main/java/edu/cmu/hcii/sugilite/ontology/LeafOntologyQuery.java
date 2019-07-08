package edu.cmu.hcii.sugilite.ontology;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * Created by nancyli on 9/27/17.
 */

public class LeafOntologyQuery extends OntologyQuery {
    protected SugiliteRelation r = null;
    private transient BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> QueryFunction = null;
    private Set<SugiliteSerializableEntity> object = null;
    private Set<SugiliteSerializableEntity> subject = null;

    public LeafOntologyQuery(){

    }

    public LeafOntologyQuery(SerializableOntologyQuery sq) {
        setOntologyQueryFilter(sq.getOntologyQueryFilter());
        r = sq.getR();
        if(r != null){
            setQueryFunction(r);
        }
        Set<SugiliteSerializableEntity> so = sq.getObject();
        Set<SugiliteSerializableEntity> ss = sq.getSubject();
        if(so != null){
            object = new HashSet<>();
            for(SugiliteSerializableEntity se : so){
                object.add(se);
//                object.add(new SugiliteEntity(se));
            }
        }
    }

    public void setQueryFunction(BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> f, SugiliteRelation r){
        /*
        if(BuildConfig.DEBUG && !(SubRelation == RelationType.nullR)){
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

    public void setObject(Set<SugiliteSerializableEntity> o) {
        object = o;
    }

    public void addObject(SugiliteSerializableEntity se) {
        if (object == null) {
            object = new HashSet<>();
        }
        object.add(se);
    }

    public void setSubject(Set<SugiliteSerializableEntity> s) {
        subject = s;
    }

    public void addSubject(SugiliteSerializableEntity se) {
        if (subject == null) {
            subject = new HashSet<>();
        }
        subject.add(se);
    }

    @Deprecated
    public void setObjectSet(Set<SugiliteEntity> o){
        /*
        if(BuildConfig.DEBUG && !(SubRelation == RelationType.nullR)){
            throw new AssertionError();
        }
        */
        object = new HashSet<>();
        for (SugiliteEntity se : o) {
            object.add(new SugiliteSerializableEntity(se));
        }
    }

    @Deprecated
    public void addObject(SugiliteEntity o){
        /*
        if(BuildConfig.DEBUG && !(SubRelation == RelationType.nullR)){
            throw new AssertionError();
        }
        */
        if(object == null){
            object = new HashSet<>();
        }
        object.add(new SugiliteSerializableEntity((o)));
    }

    @Deprecated
    public void setSubjectSet(Set<SugiliteEntity> s){
        /*
        if(BuildConfig.DEBUG && !(SubRelation == RelationType.nullR)){
            throw new AssertionError();
        }
        */
        subject = new HashSet<>();
        for (SugiliteEntity se : s) {
            subject.add(new SugiliteSerializableEntity(se));
        }
    }

    @Deprecated
    public void addSubject(SugiliteEntity s){
        /*
        if(BuildConfig.DEBUG && !(SubRelation == RelationType.nullR)){
            throw new AssertionError();
        }
        */
        if(subject == null){
            subject = new HashSet<>();
        }
        subject.add(new SugiliteSerializableEntity((s)));
    }

    public BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> getQueryFunction() {return this.QueryFunction;}

    public Set<SugiliteEntity> getObject() {
        Set<SugiliteEntity> result = new HashSet<>();
        for (SugiliteSerializableEntity se : object) {
            result.add(new SugiliteEntity(se));
        }
        return result;
    }

    public Set<SugiliteEntity> getSubject() {
        Set<SugiliteEntity> result = new HashSet<>();
        for (SugiliteSerializableEntity se : subject) {
            result.add(new SugiliteEntity(se));
        }
        return result;
    }

    public LeafOntologyQuery clone() {
        LeafOntologyQuery q = new LeafOntologyQuery();
        q.setOntologyQueryFilter(getOntologyQueryFilter());
        if(r != null){
            q.setQueryFunction(r);
        }
        if(object != null){
            for(SugiliteSerializableEntity se : object){
                q.addObject(se.clone());
            }
        }
        if(subject != null){
            for(SugiliteSerializableEntity se : subject){
                q.addSubject(se.clone());
            }
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
    protected boolean overallQueryFunction(SugiliteEntity currNode, UISnapshot graph) {
        // base case, leaf node
        if(subject == null && object == null){
            // currNode can act as either subject or object
            Set<SugiliteTriple> subjectMap = graph.getSubjectTriplesMap().get(currNode.getEntityId());
            if(subjectMap != null) {
                for (SugiliteTriple t : subjectMap) {
                    // every triple with currNode as subject
                    if (QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, t.getObject()), graph)) {
                        return true;
                    }
                }
            }

            Set<SugiliteTriple> objectMap = graph.getObjectTriplesMap().get(currNode.getEntityId());
            if(objectMap != null) {
                for (SugiliteTriple t : objectMap) {
                    // every triple with currNode as object
                    if (QueryFunction.apply(new SubjectEntityObjectEntityPair(t.getSubject(), currNode), graph)) {
                        return true;
                    }
                }
            }
            return false;
        }

        boolean objectBool = false;
        boolean subjectBool = false;
        if(object != null){
            // TODO not create so many objects?
            for(SugiliteSerializableEntity o : object){
                try {
                    if (QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, new SugiliteEntity(o)), graph)) {
                        objectBool = true;
                        break;
                    }
                }
                catch (Exception e){
                    if(QueryFunction == null){
                        System.out.println("null QueryFunction");
                    }
                    if(graph == null){
                        System.out.println("null graph");
                    }
                    e.printStackTrace();
                }
            }
        }

        if(subject != null){
            for(SugiliteSerializableEntity s : subject){
                if(QueryFunction.apply(new SubjectEntityObjectEntityPair(new SugiliteEntity(s), currNode), graph)){
                    subjectBool = true;
                    break;
                }
            }
        }

        if(object != null && subject == null){
            // currNode can act as a subject to the given object
            return objectBool;
        }

        else if(subject != null && object == null){
            // currNode can act as an object to the given subject
            return subjectBool;
        }
        else{
            // both subject and object are not null
            return objectBool || subjectBool;
        }
    }

    @Override
    public String toString() {
        String baseQueryString = "";

        // base case
        // this should have size 1 always, the array is only used in execution for when there's a query whose results are used as the objects of the next one
        SugiliteSerializableEntity[] objectArr = object.toArray(new SugiliteSerializableEntity[object.size()]);
        baseQueryString = "(" + r.getRelationName() + " " + addQuoteToTokenIfNeeded(objectArr[0].toString()) + ")";

        //include the ontologyQueryFilter in the toString() method
        if(getOntologyQueryFilter() == null) {
            return baseQueryString;
        } else{
            return "(" + getOntologyQueryFilter().toString() + " " + baseQueryString + ")";
        }
    }

    public SugiliteRelation getR() {
        return r;
    }
}