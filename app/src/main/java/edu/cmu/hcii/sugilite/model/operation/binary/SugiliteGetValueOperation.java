package edu.cmu.hcii.sugilite.model.operation.binary;

import java.io.Serializable;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;

/**
 * @author toby
 * @date 3/21/18
 * @time 6:19 PM
 */

/**
 * the operation used for getting a value from the KB
 */
public class SugiliteGetValueOperation<T> extends SugiliteGetOperation<T> implements Serializable, SugiliteValue<T> {
    public SugiliteGetValueOperation(){
        super();
    }

    public SugiliteGetValueOperation(String name){
        super(name, SugiliteGetOperation.VALUE_QUERY_NAME);
    }

    /**
     *
     * @return the value
     */
    @Override
    public T evaluate(SugiliteData sugiliteData) {
        //retrieve the actual bool expression from the KB
        if (sugiliteData != null) {
            PumiceDialogManager dialogManager = sugiliteData.pumiceDialogManager;
            if (dialogManager != null) {
                PumiceKnowledgeManager knowledgeManager = dialogManager.getPumiceKnowledgeManager();
                List<PumiceValueQueryKnowledge> valueQueryKnowledges = knowledgeManager.getPumiceValueQueryKnowledges();
                for (PumiceValueQueryKnowledge valueQueryKnowledge : valueQueryKnowledges){
                    //TODO: use a hashmap for faster retrieval
                    if(valueQueryKnowledge.getValueName() != null && valueQueryKnowledge.getValueName().equals(getName())){
                        return (T)valueQueryKnowledge.evaluate(sugiliteData);
                    }
                }
            }
        }
        throw new RuntimeException("Failed to find the boolean exp knowledge");
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("the value of \"%s\"", getName());
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
