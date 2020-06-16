package edu.cmu.hcii.sugilite.model.operation.binary;

import java.io.Serializable;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dao.PumiceKnowledgeDao;
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

    public PumiceBooleanExpKnowledge retrieveBoolExpKnowledge (SugiliteData sugiliteData) {
        //retrieve the actual bool expression from the KB
        if (sugiliteData != null) {
            try {
                PumiceKnowledgeDao pumiceKnowledgeDao = new PumiceKnowledgeDao(SugiliteData.getAppContext(), sugiliteData);
                PumiceKnowledgeManager knowledgeManager = pumiceKnowledgeDao.getPumiceKnowledgeOrANewInstanceIfNotAvailable(false, true);
                List<PumiceBooleanExpKnowledge> booleanExpKnowledges = knowledgeManager.getPumiceBooleanExpKnowledges();
                for (PumiceBooleanExpKnowledge booleanExpKnowledge : booleanExpKnowledges) {
                    //TODO: use a hashmap for faster retrieval
                    if (booleanExpKnowledge.getExpName() != null && booleanExpKnowledge.getExpName().equals(getName())) {
                        return booleanExpKnowledge;
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
                //TODO: retrive the knowledge manager when sugiliteData.pumiceDialogManager is null
                PumiceDemonstrationUtil.showSugiliteAlertDialog("Error when finding the boolean exp knowledge: " + getName());
                return null;
            }
        }
        PumiceDemonstrationUtil.showSugiliteAlertDialog("Failed to find the boolean exp knowledge: " + getName());
        return null;
    }

    /**
     *
     * @return the Boolean result
     */
    @Override
    public Boolean evaluate(SugiliteData sugiliteData) {
        //retrieve the actual bool expression from the KB
        PumiceBooleanExpKnowledge booleanExpKnowledge = retrieveBoolExpKnowledge(sugiliteData);
        if (booleanExpKnowledge != null) {
            return booleanExpKnowledge.evaluate(sugiliteData);
        } else {
            PumiceDemonstrationUtil.showSugiliteAlertDialog("Failed to evaluate the Boolean expression: " + getName() + "\n\nReturning the default false value ...");
            return false;
        }
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("the condition \"%s\" is true", getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof SugiliteValue) {
            return this.toString().equals(obj.toString());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
