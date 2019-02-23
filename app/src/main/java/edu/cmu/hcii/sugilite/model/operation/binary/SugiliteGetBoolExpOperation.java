package edu.cmu.hcii.sugilite.model.operation.binary;

import java.io.Serializable;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 3/21/18
 * @time 6:19 PM
 */

/**
 * the operation used for getting a Boolean expression from the KB
 */
public class SugiliteGetBoolExpOperation extends SugiliteGetOperation<Boolean> implements Serializable, SugiliteValue<Boolean> {
    public SugiliteGetBoolExpOperation(){
        super();
    }

    public SugiliteGetBoolExpOperation(String name){
        super(name, SugiliteGetOperation.BOOL_FUNCTION_NAME);
    }
    /**
     *
     * @return the Boolean result
     */
    @Override
    public Boolean evaluate(SugiliteData sugiliteData) {
        //retrieve the actual bool expression from the KB
        if (sugiliteData != null) {
            PumiceDialogManager dialogManager = sugiliteData.pumiceDialogManager;
            if (dialogManager != null) {
                PumiceKnowledgeManager knowledgeManager = dialogManager.getPumiceKnowledgeManager();
                List<PumiceBooleanExpKnowledge> booleanExpKnowledges = knowledgeManager.getPumiceBooleanExpKnowledges();
                for (PumiceBooleanExpKnowledge booleanExpKnowledge : booleanExpKnowledges){
                    //TODO: use a hashmap for faster retrieval
                    if(booleanExpKnowledge.getExpName() != null && booleanExpKnowledge.getExpName().equals(getName())){
                        return booleanExpKnowledge.evaluate(sugiliteData);
                    }
                }
            }
        }
        throw new RuntimeException("Failed to find the boolean exp knowledge");
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("the condition \"%s\" is true", getName());
    }
}
