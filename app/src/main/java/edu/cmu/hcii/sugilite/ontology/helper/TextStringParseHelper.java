package edu.cmu.hcii.sugilite.ontology.helper;

import org.w3c.dom.Entity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Node;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteTriple;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.EmailAddressAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.PhoneNumberAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextAnnotator;

/**
 * @author toby
 * @date 1/17/18
 * @time 8:54 PM
 */
public class TextStringParseHelper {
    private SugiliteTextAnnotator sugiliteTextAnnotator;
    public TextStringParseHelper(SugiliteTextAnnotator sugiliteTextAnnotator){
        this.sugiliteTextAnnotator = sugiliteTextAnnotator;
    }

    public void parseAndAddNewRelations(SugiliteEntity<String> stringEntity, UISnapshot uiSnapshot){
        List<SugiliteTextAnnotator.AnnotatingResult> results = sugiliteTextAnnotator.annotate(stringEntity.getEntityValue());
        for(SugiliteTextAnnotator.AnnotatingResult result : results){

            //insert the new triples for the relations in the annotating results
            String objectString = result.getMatchedString();
            if(result.getNumericValue() != null){
                objectString = String.valueOf(result.getNumericValue());
            }
            SugiliteRelation sugiliteRelation = result.getRelation();

            //TODO: add triples for all parents of the string Entity
            Set<SugiliteEntity> parentNodeEntities = new HashSet<>();
            Map<Integer, Set<SugiliteTriple>> tripleMap = uiSnapshot.getObjectTriplesMap();
            if(tripleMap != null){
                Set<SugiliteTriple> allTriplesWithMatchedObject = tripleMap.get(stringEntity.getEntityId());
                if(allTriplesWithMatchedObject != null){
                    for(SugiliteTriple triple : allTriplesWithMatchedObject){
                        if(triple.getPredicate().equals(SugiliteRelation.HAS_CHILD_TEXT) || triple.getPredicate().equals(SugiliteRelation.HAS_TEXT)){
                            if(triple.getSubject().getEntityValue() instanceof Node){
                                parentNodeEntities.add(triple.getSubject());
                            }
                        }
                    }
                }
            }
            for(SugiliteEntity entity : parentNodeEntities) {
                uiSnapshot.addEntityStringTriple(entity, objectString, sugiliteRelation);
            }
        }
    }




}
