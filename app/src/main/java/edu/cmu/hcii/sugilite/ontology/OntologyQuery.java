package edu.cmu.hcii.sugilite.ontology;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collectors;

import edu.cmu.hcii.sugilite.BuildConfig;

/**
 * Created by nancyli on 9/27/17.
 */

public class OntologyQuery {
    public enum relationType {
        nullR, AND, OR, PREV
    }
    private relationType SubRelation;
    private Set<OntologyQuery> SubQueries = null;
    private BiFunction<SubjectEntityObjectEntityPair, UISnapshot, Boolean> QueryFunction = null;
    private Set<SugiliteEntity> object = null;
    private Set<SugiliteEntity> subject = null;
    private SugiliteRelation r = null;

    private OntologyQueryFilter ontologyQueryFilter = null;

    public OntologyQuery(){

    }

    public OntologyQuery(SerializableOntologyQuery sq) {
        SubRelation = sq.getSubRelation();
        ontologyQueryFilter = sq.getOntologyQueryFilter();
        r = sq.getR();
        if(r != null){
            setQueryFunction(sq.getR());
        }
        if(SubRelation != relationType.nullR) {
            SubQueries = new HashSet<>();
            Set<SerializableOntologyQuery> pSubq = sq.getSubQueries();
            for(SerializableOntologyQuery s : pSubq) {
                SubQueries.add(new OntologyQuery(s));
            }
        }
        else{
            Set<SugiliteSerializableEntity> so = sq.getObject();
            Set<SugiliteSerializableEntity> ss = sq.getSubject();
            if(so != null){
                object = new HashSet<>();
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

    public void setOntologyQueryFilter(OntologyQueryFilter ontologyQueryFilter) {
        this.ontologyQueryFilter = ontologyQueryFilter;
    }

    public OntologyQueryFilter getOntologyQueryFilter() {
        return ontologyQueryFilter;
    }

    /**
     * the query function used for determine whether a node matches the query
     * @param query
     * @param currNode
     * @param graph
     * @return
     */
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

        if(ontologyQueryFilter != null) {
            return ontologyQueryFilter.filter(results, graph);
        } else {
            return results;
        }
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

    private static OntologyQuery parseString(String s) {
        // example: (conj (IS_CLICKABLE true) (HAS_TEXT coffee))
        OntologyQuery q = new OntologyQuery();
        s = s.trim();
        if(s.startsWith("(") && s.endsWith(")") && s.length() > 1) {
            //remove the outmost parenthesis
            s = s.substring(1, s.length() - 1);
            s = s.trim();

            //process the possible OntologyQueryFilter
            if(s.startsWith("ARG_MIN") || s.startsWith("ARG_MAX") || s.startsWith("EXISTS")){
                //contains an OntologyQueryFilter -- need to process
                int filterEndIndex = s.indexOf(" ", s.indexOf(" ") + 1);
                String ontologyFilterString = s.substring(0, filterEndIndex);
                String queryString = s.substring(filterEndIndex + 1);
                OntologyQueryFilter filter = OntologyQueryFilter.deserialize(ontologyFilterString);
                OntologyQuery resultQuery = OntologyQuery.parseString(queryString);
                resultQuery.setOntologyQueryFilter(filter);
                return resultQuery;
            }

            // s: conj (IS_CLICKABLE true) (HAS_TEXT coffee)
            int spaceIndex = s.indexOf(' ');
            String firstWord = s.substring(0, spaceIndex);
            // firstWord: conj
            if (s.contains("(")) {
                // nested relation
                if (firstWord.equals("conj")) {
                    q.setSubRelation(relationType.AND);
                } else if (firstWord.equals("or")) {
                    q.setSubRelation(relationType.OR);
                } else {
                    q.setSubRelation(relationType.PREV);
                    q.setQueryFunction(SugiliteRelation.stringRelationMap.get(firstWord));
                }

                Set<OntologyQuery> subQ = new HashSet<OntologyQuery>();
                // walk through the string and parse in the next level query strings recursively
                int lastMatchIndex = spaceIndex + 1;
                int counter = 0;
                for (int i = spaceIndex + 1; i < s.length(); i++) {
                    if (s.charAt(i) == '(') counter++;
                    else if (s.charAt(i) == ')') counter--;

                    if (counter == 0) {
                        OntologyQuery sub_query = parseString(s.substring(lastMatchIndex, i + 1));
                        subQ.add(sub_query);
                        lastMatchIndex = i + 2;
                        i++;
                    }
                }
                q.setSubQueries(subQ);
            }

            else {
                // base case: simple relation
                // note: the object will never be an accessbility node info (since this is directly from user)
                String predicateString = firstWord;
                String objectString = s.substring(spaceIndex + 1, s.length());
                q.setSubRelation(relationType.nullR);

                q.setQueryFunction(SugiliteRelation.stringRelationMap.get(predicateString));
                Set<SugiliteEntity> oSet = new HashSet<SugiliteEntity>();
                if (objectString.equalsIgnoreCase("true")) {
                    SugiliteEntity<Boolean> o = new SugiliteEntity<Boolean>(-1, Boolean.class, true);
                    oSet.add(o);
                } else if (objectString.equalsIgnoreCase("false")) {
                    SugiliteEntity<Boolean> o = new SugiliteEntity<Boolean>(-1, Boolean.class, false);
                    oSet.add(o);
                } else {
                    SugiliteEntity<String> o = new SugiliteEntity<String>(-1, String.class, OntologyQueryUtils.removeQuoteSigns(objectString));
                    oSet.add(o);
                }
                q.setObject(oSet);
            }
            return q;
        }

        else {

            //malformed query
            return null;
        }
    }

    public static OntologyQuery deserialize(String queryString) {
        // example: (and (hasColor red) (isChecked true))
        return parseString(queryString);
    }

    public SugiliteRelation getR() {
        return r;
    }

    @Override
    public String toString() {
        String baseQueryString = "";

        if(SubRelation == relationType.nullR){
            // base case
            // this should have size 1 always, the array is only used in execution for when there's a query whose results are used as the objects of the next one
            SugiliteEntity[] objectArr = object.toArray(new SugiliteEntity[object.size()]);
            baseQueryString = "(" + r.getRelationName() + " " + objectArr[0].toString() + ")";
        }

        else {
            OntologyQuery[] subQueryArray = SubQueries.toArray(new OntologyQuery[SubQueries.size()]);
            if (SubRelation == relationType.AND || SubRelation == relationType.OR) {
                int size = subQueryArray.length;
                String[] arr = new String[size];
                for (int i = 0; i < size; i++) {
                    arr[i] = subQueryArray[i].toString();
                }
                String joined = Arrays.asList(arr).stream().collect(Collectors.joining(" "));
                if (SubRelation == relationType.AND) {
                    baseQueryString = "(conj " + joined + ")";
                } else {
                    baseQueryString = "(or " + joined + ")";
                }
            }
            else if (SubRelation == relationType.PREV){
                baseQueryString = "(" + r.getRelationName() + " " + subQueryArray[0].toString() + ")";
            }
        }

        //include the ontologyQueryFilter in the toString() method
        if(ontologyQueryFilter == null) {
            return baseQueryString;
        } else{
            return "(" + ontologyQueryFilter.toString() + " " + baseQueryString + ")";
        }
    }

}