package edu.cmu.hcii.sugilite.ontology;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

import edu.cmu.hcii.sugilite.BuildConfig;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;

/**
 * Created by nancyli on 9/27/17.
 */

public class OntologyQuery {
    public enum relationType {
        nullR, AND, OR, PREV;
    }
    private relationType SubRelation;
    private Set<OntologyQuery> SubQueries = null;
    private BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> QueryFunction = null;
    private Set<SugiliteEntity> object = null;
    private Set<SugiliteEntity> subject = null;
    private SugiliteRelation r = null;

    public OntologyQuery(){

    }

    public OntologyQuery(SerializableOntologyQuery sq) {
        SubRelation = sq.getSubRelation();
        r = sq.getR();
        if(r != null){
            setQueryFunction(sq.getR());
        }
        if(SubRelation != relationType.nullR) {
            SubQueries = new HashSet<OntologyQuery>();
            Set<SerializableOntologyQuery> pSubq = sq.getSubQueries();
            for(SerializableOntologyQuery s : pSubq) {
                SubQueries.add(new OntologyQuery(s));
            }
        }
        else{
            Set<SugiliteSerializableEntity> so = sq.getObject();
            Set<SugiliteSerializableEntity> ss = sq.getSubject();
            if(so != null){
                object = new HashSet<SugiliteEntity>();
                for(SugiliteSerializableEntity se : so){
                    object.add(new SugiliteEntity(se));
                }
            }
        }
    }

    public OntologyQuery(relationType r){
        this.SubRelation = r;
        if(r != relationType.nullR){
            // there are sub-queries
            this.SubQueries = new HashSet<OntologyQuery>();
        }
    }

    public void addSubQuery(OntologyQuery sub){
        if(BuildConfig.DEBUG && !(SubRelation != relationType.nullR && SubQueries != null)){
            throw new AssertionError();
        }
        SubQueries.add(sub);
    }

    public void setQueryFunction(BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> f, SugiliteRelation r){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
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

    public void setObject(Set<SugiliteEntity> o){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
        object = o;
    }

    public void addObject(SugiliteEntity o){
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
        if(object == null){
            object = new HashSet<>();
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
        if(BuildConfig.DEBUG && !(SubRelation == relationType.nullR)){
            throw new AssertionError();
        }
        if(subject == null){
            subject = new HashSet<>();
        }
        subject.add(s);
    }

    public void setSubRelation(relationType subRelation) {
        SubRelation = subRelation;
    }

    public void setSubQueries(Set<OntologyQuery> subQueries) {
        if(BuildConfig.DEBUG && !(SubRelation != relationType.nullR)){
            throw new AssertionError();
        }
        SubQueries = subQueries;
    }

    public relationType getSubRelation() {return this.SubRelation;}

    public Set<OntologyQuery> getSubQueries() {return this.SubQueries;}

    public BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> getQueryFunction() {return this.QueryFunction;}

    public Set<SugiliteEntity> getObject() {return this.object;}

    public Set<SugiliteEntity> getSubject() {return this.subject;}

    public boolean checkValidQuery() {
        if(!(SubRelation == relationType.PREV && SubQueries != null && object == null && subject == null && QueryFunction != null && r != null && SubQueries.size() == 1)){
            return false;
        }
        if(!((SubRelation == relationType.AND || SubRelation == relationType.OR) && SubQueries != null && object == null && subject == null && QueryFunction == null && r == null)){
            return false;
        }
        if(!(SubRelation == relationType.nullR && SubQueries == null && QueryFunction != null && r != null)){
            return false;
        }
        return true;
    }

    private boolean OverallQueryFunction(OntologyQuery query, SugiliteEntity currNode, UISnapshot graph) {
        if(query.SubRelation == relationType.nullR){
            // base case, leaf node
            if(query.subject == null && query.object == null){
                // currNode can act as either subject or object
                Set<SugiliteTriple> subjectMap = graph.getSubjectTriplesMap().get(currNode.getEntityId());
                if(subjectMap != null) {
                    for (SugiliteTriple t : subjectMap) {
                        // every triple with currNode as subject
                        if (query.QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, t.getObject()), graph)) {
                            return true;
                        }
                    }
                }

                Set<SugiliteTriple> objectMap = graph.getObjectTriplesMap().get(currNode.getEntityId());
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
                for(SugiliteEntity o : query.object){
                    try {
                        if (query.QueryFunction.apply(new SubjectEntityObjectEntityPair(currNode, o), graph)) {
                            objectBool = true;
                            break;
                        }
                    }
                    catch (Exception e){
                        if(query == null){
                            System.out.println("null query");
                        }
                        if(query.QueryFunction == null){
                            System.out.println("null QueryFunction");
                        }
                        if(graph == null){
                            System.out.println("null graph");
                        }
                        e.printStackTrace();
                    }
                }
            }

            if(query.subject != null){
                for(SugiliteEntity s : query.subject){
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
            else if(query.SubRelation == relationType.OR){
                for(OntologyQuery q : query.SubQueries){
                    if(OverallQueryFunction(q, currNode, graph)){
                        return true;
                    }
                }
                return false;
            }
            else{
                // query.SubRelation == relationType.PREV
                // only one subquery
                OntologyQuery prevQ = query.SubQueries.toArray(new OntologyQuery[query.SubQueries.size()])[0];
                Set<SugiliteEntity> prevResult = prevQ.executeOn(graph);
                query.setSubQueries(null);
                query.setSubRelation(relationType.nullR);
                query.setObject(prevResult);
                return OverallQueryFunction(query, currNode, graph);
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

    private static OntologyQuery parseString(String s, OntologyQuery q) {
        // example: (conj (IS_CLICKABLE true) (HAS_TEXT coffee))
        int len = s.length();
        if(BuildConfig.DEBUG && !(s.charAt(0) == '(' && s.charAt(len-1) == ')')){
            //throw new AssertionError();
        }
        s = s.substring(1, len-1);
        // s: conj (IS_CLICKABLE true) (HAS_TEXT coffee)
        len = s.length();

        int spaceIndex = s.indexOf(' ');
        String firstWord = s.substring(0, spaceIndex);
        // firstWord: conj

        if(s.contains("(")) {
            // nested relation
            if(firstWord.equals("conj")) {
                q.setSubRelation(relationType.AND);
            }
            else if(firstWord.equals("or")){
                q.setSubRelation(relationType.OR);
            }
            else{
                q.setSubRelation(relationType.PREV);
                q.setQueryFunction(SugiliteRelation.stringRelationMap.get(firstWord));
            }

            Set<OntologyQuery> subQ = new HashSet<OntologyQuery>();
            // walk through the string and parse in the next level query strings recursively
            int lastMatchIndex = spaceIndex + 1;
            int counter = 0;
            for(int i = spaceIndex + 1; i < len; i++) {
                if(s.charAt(i) == '(') counter++;
                else if(s.charAt(i) == ')') counter--;

                if(counter == 0) {
                    OntologyQuery sub_query = new OntologyQuery();
                    sub_query = parseString(s.substring(lastMatchIndex, i+1), sub_query);
                    subQ.add(sub_query);
                    lastMatchIndex = i+2;
                    i++;
                }
            }
            q.setSubQueries(subQ);
        }
        else {
            // base case: simple relation
            // note: the object will never be an accessbility node info (since this is directly from user)
            String predicateString = firstWord;
            String objectString = s.substring(spaceIndex+1, len);
            q.setSubRelation(relationType.nullR);

            q.setQueryFunction(SugiliteRelation.stringRelationMap.get(predicateString));
            Set<SugiliteEntity> oSet = new HashSet<SugiliteEntity>();
            if(objectString.equalsIgnoreCase("true")){
                SugiliteEntity<Boolean> o = new SugiliteEntity<Boolean>(-1, Boolean.class, true);
                oSet.add(o);
            }
            else if(objectString.equalsIgnoreCase("false")){
                SugiliteEntity<Boolean> o = new SugiliteEntity<Boolean>(-1, Boolean.class, false);
                oSet.add(o);
            }
            else {
                SugiliteEntity<String> o = new SugiliteEntity<String>(-1, String.class, cleanString(objectString));
                oSet.add(o);
            }
            q.setObject(oSet);
        }
        return q;
    }

    private static String cleanString(String string){
        if(string.startsWith("\"") && string.endsWith("\"")){
            return string.substring(1, string.length() - 1);
        } else{
            return string;
        }
    }

    public static OntologyQuery deserialize(String queryString) {
        // example: (and (hasColor red) (isChecked true))
        return parseString(queryString, new OntologyQuery());
    }

    public SugiliteRelation getR() {
        return r;
    }

    @Override
    public String toString() {
        if(SubRelation == relationType.nullR){
            // base case
            // this should have size 1 always, the array is only used in execution for when there's a query whose results are used as the objects of the next one
            SugiliteEntity[] objectArr = object.toArray(new SugiliteEntity[object.size()]);
            return "(" + r.getRelationName() + " " + objectArr[0].toString() + ")";
        }
        OntologyQuery[] subQueryArray = SubQueries.toArray(new OntologyQuery[SubQueries.size()]);
        if(SubRelation == relationType.AND || SubRelation == relationType.OR){
            int size = subQueryArray.length;
            String[] arr = new String[size];
            for(int i = 0; i < size; i++){
                arr[i] = subQueryArray[i].toString();
            }
            if(SubRelation == relationType.AND) return "(conj " + TextUtils.join(" ", arr) + ")";
            else return "(or " + TextUtils.join(" ", arr) + ")";
        }

        // SubRelation == relationType.PREV

        return "(" + r.getRelationName() + " " + subQueryArray[0].toString() + ")";
    }



}