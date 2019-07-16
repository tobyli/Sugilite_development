package edu.cmu.hcii.sugilite.ontology;

import java.util.LinkedHashSet;
import java.util.Set;

public class PlaceholderOntologyQuery extends OntologyQueryWithSubQueries {
    private OntologyQuery innerQuery;

    public PlaceholderOntologyQuery(OntologyQuery innerQuery) {
        this.innerQuery = innerQuery;
    }

    @Override
    protected boolean overallQueryFunction(SugiliteEntity currNode, UISnapshot graph) {
        return innerQuery.overallQueryFunction(currNode, graph);
    }

    @Override
    public String toString() {
        return "(placeholder " + innerQuery.toString() + ")";
    }

    public OntologyQuery getInnerQuery() {
        return innerQuery;
    }

    public void setInnerQuery(OntologyQuery innerQuery) {
        this.innerQuery = innerQuery;
    }

    @Override
    public OntologyQuery clone() {
        return new PlaceholderOntologyQuery(innerQuery.clone());
    }

    @Override
    public Set<OntologyQuery> getSubQueries() {
        Set<OntologyQuery> queries = new LinkedHashSet<>();
        queries.add(innerQuery);
        return queries;
    }
}
