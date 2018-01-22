package edu.cmu.hcii.sugilite.ontology.helper;

import java.util.List;

import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
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

    public void parse(SugiliteEntity<String> stringEntity, UISnapshot uiSnapshot){
        List<SugiliteTextAnnotator.AnnotatingResult> results = sugiliteTextAnnotator.annotate(stringEntity.getEntityValue());
        for(SugiliteTextAnnotator.AnnotatingResult result : results){

            //insert the new triples for the relations in the annotating results
            String objectString = result.getMatchedString();
            if(result.getNumericValue() != null){
                objectString = String.valueOf(result.getNumericValue());
            }
            SugiliteRelation sugiliteRelation = result.getRelation();
            uiSnapshot.addEntityStringTriple(stringEntity, objectString, sugiliteRelation);
        }
    }




}
