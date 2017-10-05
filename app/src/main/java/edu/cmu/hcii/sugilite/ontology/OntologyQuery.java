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
    private SugiliteEntity object;
    private SugiliteEntity subject;

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
        //TODO: create a new BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> f so f is true when there exists (s, r, o) in the graph
    }

    public void setObject(SugiliteEntity o){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
        object = o;
    }

    public void setSubject(SugiliteEntity s){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
        subject = s;
    }

    public relationType getSubRelation(){return this.SubRelation;}

    public Set<OntologyQuery> getSubQueries(){return this.SubQueries;}

    public BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> getQueryFunction(){return this.QueryFunction;}

    public SugiliteEntity getObject(){return this.object;}

    public SugiliteEntity getSubject(){return this.subject;}

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
                for(SugiliteTriple t : graph.getSubjectTriplesMap().get(currNode)){
                    // every triple with currNode as subject
                    if(query.QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, t.getObject()), graph)){
                        return true;
                    }
                }
                for(SugiliteTriple t : graph.getObjectTriplesMap().get(currNode)){
                    // every triple with currNode as object
                    if(query.QueryFunction.apply(new SubjectEntityObjectEntityPair(t.getSubject(), currNode), graph)){
                        return true;
                    }
                }
                return false;
            }

            else if(query.object != null && query.subject == null){
                // currNode can act as a subject to the given object
                if(query.QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, query.object), graph)){
                    return true;
                }
                return false;
            }

            else if(query.subject != null && query.object == null){
                // currNode can act as an object to the given subject
                if(query.QueryFunction.apply(new SubjectEntityObjectEntityPair(query.subject, currNode), graph)){
                    return true;
                }
                return false;
            }
            else{
                // both subject and object are not null
                if(query.QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, query.object), graph) || query.QueryFunction.apply(new SubjectEntityObjectEntityPair(query.subject, currNode), graph)){
                    return true;
                }
                return false;
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
        private SugiliteEntity subject;
        private SugiliteEntity object;

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
