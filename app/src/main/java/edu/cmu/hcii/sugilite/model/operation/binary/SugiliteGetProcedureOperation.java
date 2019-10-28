package edu.cmu.hcii.sugilite.model.operation.binary;

import android.support.annotation.Nullable;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dao.PumiceKnowledgeDao;
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
        PumiceKnowledgeDao pumiceKnowledgeDao = new PumiceKnowledgeDao(SugiliteData.getAppContext(), sugiliteData);
        try {
            PumiceKnowledgeManager pumiceKnowledgeManager = pumiceKnowledgeDao.getPumiceKnowledgeOrANewInstanceIfNotAvailable(true);
            if (pumiceKnowledgeManager != null) {
                for (PumiceProceduralKnowledge proceduralKnowledge : pumiceKnowledgeManager.getPumiceProceduralKnowledges()) {
                    if (getName().equals(proceduralKnowledge.getProcedureName())) {
                        //found;
                        return proceduralKnowledge.getTargetScriptName(pumiceKnowledgeManager);
                    }
                }
            } else {
                throw new RuntimeException("null knowledge manager!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //TODO: retrive the knowledge manager when sugiliteData.pumiceDialogManager is null
            PumiceDemonstrationUtil.showSugiliteAlertDialog("Error when finding the procedure knowledge: " + getName());
            return null;
        }
        PumiceDemonstrationUtil.showSugiliteAlertDialog("Error when finding the procedure knowledge: " + getName());
        return null;
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("perform the action \"%s\"", getName());
    }

}
