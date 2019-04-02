package edu.cmu.hcii.sugilite.model.operation.binary;

import android.support.annotation.Nullable;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 3/21/18
 * @time 6:19 PM
 */

/**
 * the operation used for getting a procedure from the KB
 */
public class SugiliteGetProcedureOperation extends SugiliteGetOperation<String> implements Serializable, SugiliteValue<String> {
    public SugiliteGetProcedureOperation(){
        super();
    }

    public SugiliteGetProcedureOperation(String name){
        super(name, SugiliteGetOperation.PROCEDURE_NAME);
    }

    /**
     * @return the subscript name
     */
    @Override
    public String evaluate(@Nullable SugiliteData sugiliteData) {
        PumiceDialogManager pumiceDialogManager = sugiliteData.pumiceDialogManager;
        if (pumiceDialogManager != null) {
            PumiceKnowledgeManager pumiceKnowledgeManager = pumiceDialogManager.getPumiceKnowledgeManager();
            if (pumiceKnowledgeManager != null) {
                for (PumiceProceduralKnowledge proceduralKnowledge : pumiceKnowledgeManager.getPumiceProceduralKnowledges()){
                    if (getName().equals(proceduralKnowledge.getProcedureName())){
                        //found;
                        return proceduralKnowledge.getTargetScriptName(pumiceKnowledgeManager);
                    }
                }
            } else {
                throw new RuntimeException("null knowledge manager!");
            }
        } else {
            throw new RuntimeException("null dialog manager!");
        }
        throw new RuntimeException("can't find the target procedure knowledge!");
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("perform the action \"%s\"", getName());
    }

}
