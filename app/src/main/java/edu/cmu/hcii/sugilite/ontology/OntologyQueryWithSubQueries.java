package edu.cmu.hcii.sugilite.ontology;

import java.util.Set;

public abstract class OntologyQueryWithSubQueries extends OntologyQuery {
    public abstract Set<OntologyQuery> getSubQueries();

    /**
     * Get the same query but with different subqueries.
     * @param newSubQueries
     * @return
     */
    public abstract OntologyQueryWithSubQueries cloneWithTheseSubQueries(Set<OntologyQuery> newSubQueries);
}
