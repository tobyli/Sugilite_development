package edu.cmu.hcii.sugilite.ontology;

import edu.cmu.hcii.sugilite.Node;

/**
 * @author toby
 * @date 2/21/18
 * @time 3:05 PM
 */
public class OntologyQueryUtils {
    static public OntologyQuery getQueryWithClassAndPackageConstraints(OntologyQuery query, Node clickedNode){
        //de-serialize the query
        OntologyQuery parentQuery = new OntologyQuery(OntologyQuery.relationType.AND);
        parentQuery.addSubQuery(query);

        if (clickedNode.getClassName() != null) {
            OntologyQuery classQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            classQuery.addObject(new SugiliteEntity<>(-1, String.class, clickedNode.getClassName()));
            classQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
            parentQuery.addSubQuery(classQuery);
        }

        if (clickedNode.getPackageName() != null) {
            OntologyQuery packageQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            packageQuery.addObject(new SugiliteEntity<>(-1, String.class, clickedNode.getPackageName()));
            packageQuery.setQueryFunction(SugiliteRelation.HAS_PACKAGE_NAME);
            parentQuery.addSubQuery(packageQuery);
        }
        return parentQuery;
    }
}
