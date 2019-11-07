package edu.cmu.hcii.sugilite.sharing;

import edu.cmu.hcii.sugilite.ontology.HashedStringLeafOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.LeafOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;

/**
 * @author toby
 * @date 10/28/19
 * @time 4:29 PM
 */
public class PrivateNonPrivateLeafOntologyQueryPairWrapper extends OntologyQuery {
    private OntologyQuery privateQuery;
    private LeafOntologyQuery nonPrivateQuery;

    public enum QueryInUse {PRIVATE, NONPRIVATE}
    QueryInUse queryInUse;


    public PrivateNonPrivateLeafOntologyQueryPairWrapper(OntologyQuery privateQuery, LeafOntologyQuery nonPrivateQuery, QueryInUse queryInUse) {
        this.privateQuery = privateQuery;
        this.nonPrivateQuery = nonPrivateQuery;
        this.queryInUse = queryInUse;
    }

    public OntologyQuery getQueryInUse(){
        if (queryInUse == QueryInUse.PRIVATE) {
            return privateQuery;
        } else {
            return nonPrivateQuery;
        }
    }

    public void setQueryInUse(QueryInUse queryInUse) {
        this.queryInUse = queryInUse;
    }

    public void flip() {
        if (this.queryInUse == QueryInUse.PRIVATE) {
            this.queryInUse = QueryInUse.NONPRIVATE;
        } else {
            this.queryInUse = QueryInUse.PRIVATE;
        }
    }

    @Override
    public OntologyQuery clone() {
        return new PrivateNonPrivateLeafOntologyQueryPairWrapper(privateQuery, nonPrivateQuery, queryInUse);
    }

    @Override
    public String toString() {
        return getQueryInUse().toString();
    }

    @Override
    public boolean overallQueryFunction(SugiliteEntity currNode, UISnapshot graph) {
        return getQueryInUse().overallQueryFunction(currNode, graph);
    }
}
