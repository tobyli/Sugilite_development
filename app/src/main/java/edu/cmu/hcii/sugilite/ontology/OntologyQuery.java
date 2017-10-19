package edu.cmu.hcii.sugilite.ontology;

import android.app.DownloadManager;

import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

import edu.cmu.hcii.sugilite.BuildConfig;

/**
 * Created by nancyli on 9/27/17.
 */

public class OntologyQuery {
    public enum relationType {
        nullR, AND, OR;
    }
    private relationType SubRelation;
    private Set<OntologyQuery> SubQueries;
    private BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> QueryFunction = null;
    private Set<SugiliteEntity> object;
    private Set<SugiliteEntity> subject;

    public OntologyQuery(relationType r){
        this.SubRelation = r;
        if(r == relationType.nullR){
            // base case query
            this.SubQueries = null;
        }
        else{
            // there are sub-queries
            this.SubQueries = new HashSet<OntologyQuery>();
            this.object = null;
            this.subject = null;
        }
    }

    public void addSubQuery(OntologyQuery sub){
        if(BuildConfig.DEBUG && !(SubRelation != relationType.nullR && SubQueries != null)){
            throw new AssertionError();
        }
        SubQueries.add(sub);
    }

    public void setQueryFunction(BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> f){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
        QueryFunction = f;
    }

    public void setQueryFunction(SugiliteRelation relation){
        QueryFunction = new BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean>() {
            @Override
            public Boolean apply(SubjectEntityObjectEntityPair pair, UISnapshot graph) {
                SugiliteEntity s = pair.getSubject();
                SugiliteEntity o = pair.getObject();
                SugiliteTriple newTriple = new SugiliteTriple(s, relation, o);
                Set<SugiliteTriple> subjectTriples = graph.getSubjectTriplesMap().get(s);
                if(subjectTriples == null) return false;
                return subjectTriples.contains(newTriple);
            }
        };
    }

    public void setObject(Set<SugiliteEntity> o){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
        object = o;
    }

    public void addObject(SugiliteEntity o){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR && object != null)){
            throw new AssertionError();
        }
        object.add(o);
    }

    public void setSubject(Set<SugiliteEntity> s){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
        subject = s;
    }

    public void addSubject(SugiliteEntity s){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR && subject != null)){
            throw new AssertionError();
        }
        subject.add(s);
    }

    public relationType getSubRelation() {return this.SubRelation;}

    public Set<OntologyQuery> getSubQueries() {return this.SubQueries;}

    public BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> getQueryFunction() {return this.QueryFunction;}

    public Set<SugiliteEntity> getObject() {return this.object;}

    public Set<SugiliteEntity> getSubject() {return this.subject;}

    public boolean checkValidQuery() {
        if(!(SubRelation != relationType.nullR && SubQueries != null && object == null && subject == null && QueryFunction == null)){
            return false;
        }
        if(!(SubRelation == relationType.nullR && SubQueries == null && QueryFunction != null)){
            return false;
        }
        return true;
    }

    private boolean OverallQueryFunction(OntologyQuery query, SugiliteEntity currNode, UISnapshot graph) {
        if(query.SubRelation == relationType.nullR){
            // base case
            if(query.subject == null && query.object == null){
                // currNode can act as either subject or object
                Set<SugiliteTriple> subjectMap = graph.getSubjectTriplesMap().get(currNode);
                if(subjectMap != null) {
                    for (SugiliteTriple t : subjectMap) {
                        // every triple with currNode as subject
                        if (query.QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, t.getObject()), graph)) {
                            return true;
                        }
                    }
                }

                Set<SugiliteTriple> objectMap = graph.getObjectTriplesMap().get(currNode);
                if(objectMap != null) {
                    for (SugiliteTriple t : objectMap) {
                        // every triple with currNode as object
                        if (query.QueryFunction.apply(new SubjectEntityObjectEntityPair(t.getSubject(), currNode), graph)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            boolean objectBool = false;
            boolean subjectBool = false;
            if(query.object != null){
                for(SugiliteEntity o : object){
                    if(query.QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, o), graph)){
                        objectBool = true;
                        break;
                    }
                }
            }

            if(query.subject != null){
                for(SugiliteEntity s : subject){
                    if(query.QueryFunction.apply(new SubjectEntityObjectEntityPair(s, currNode), graph)){
                        subjectBool = true;
                        break;
                    }
                }
            }

            if(query.object != null && query.subject == null){
                // currNode can act as a subject to the given object
                return objectBool;
            }

            else if(query.subject != null && query.object == null){
                // currNode can act as an object to the given subject
                return subjectBool;
            }
            else{
                // both subject and object are not null
                return objectBool || subjectBool;
            }
        }
        else{
            if(query.SubRelation == relationType.AND){
                for(OntologyQuery q : query.SubQueries){
                    if(!OverallQueryFunction(q, currNode, graph)){
                        return false;
                    }
                }
                return true;
            }
            else{
                // query.SubRelation == relationType.OR
                for(OntologyQuery q : query.SubQueries){
                    if(OverallQueryFunction(q, currNode, graph)){
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public Set<SugiliteEntity> executeOn(UISnapshot graph){
        Set<SugiliteEntity> results = new HashSet<SugiliteEntity>();
        // for each node in the graph, follow the if statements in notes
        // if it matches query, then add to results set
        for(SugiliteEntity s : graph.getSugiliteEntityIdSugiliteEntityMap().values()) {
            if(OverallQueryFunction(this, s, graph)){
                results.add(s);
            }
        }
        return results;
    }

    class SubjectEntityObjectEntityPair{
        private SugiliteEntity subject = null;
        private SugiliteEntity object = null;

        public SubjectEntityObjectEntityPair(SugiliteEntity subject, SugiliteEntity object){
            this.subject = subject;
            this.object = object;
        }

        public SugiliteEntity getSubject() {
            return subject;
        }

        public SugiliteEntity getObject() {
            return object;
        }

        public void setObject(SugiliteEntity object) {
            this.object = object;
        }

        public void setSubject(SugiliteEntity subject) {
            this.subject = subject;
        }
    }
}
