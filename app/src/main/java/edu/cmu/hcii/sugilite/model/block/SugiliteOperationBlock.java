package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.operation.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteReadoutOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:10 PM
 */
public class SugiliteOperationBlock extends SugiliteBlock implements Serializable{
    private SugiliteBlock nextBlock;
    private SugiliteOperation operation;
    private SugiliteAvailableFeaturePack featurePack;
    private SerializableOntologyQuery query;

    @Deprecated
    private UIElementMatchingFilter elementMatchingFilter;

    public boolean isSetAsABreakPoint = false;

    public SugiliteOperationBlock(){
        super();
        this.blockType = SugiliteBlock.REGULAR_OPERATION;
        this.setDescription("");
    }
    public void setNextBlock(SugiliteBlock block){
        this.nextBlock = block;
    }
    public void setQuery(SerializableOntologyQuery query) {
        this.query = query;
    }
    public void setOperation(SugiliteOperation operation){
        this.operation = operation;
    }
    public void setFeaturePack(SugiliteAvailableFeaturePack featurePack){
        this.featurePack = featurePack;
    }

    public SugiliteBlock getNextBlock(){
        return nextBlock;
    }
    public SerializableOntologyQuery getQuery() {
        return query;
    }
    public SugiliteOperation getOperation(){
        return operation;
    }
    public SugiliteAvailableFeaturePack getFeaturePack(){
        return featurePack;
    }

    public void delete(){
        SugiliteBlock previousBlock = getPreviousBlock();
        if(previousBlock instanceof SugiliteStartingBlock)
            ((SugiliteStartingBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteOperationBlock)
            ((SugiliteOperationBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteSpecialOperationBlock)
            ((SugiliteSpecialOperationBlock) previousBlock).setNextBlock(null);
    }

    @Override
    public String toString() {
        //TODO: get the string for the block
        String results = "";
        String verb = "";
        switch (operation.getOperationType()){
            case SugiliteOperation.CLICK:
                verb = "CLICK";
                results = "(" + verb + " " + query.toString() + ")";
                break;
            case SugiliteOperation.LONG_CLICK:
                verb = "LONG_CLICK";
                results = "(" + verb + " " + query.toString() + ")";
                break;
            case SugiliteOperation.CLEAR_TEXT:
                verb = "CLEAR_TEXT";
                results = "(" + verb + " " + query.toString() + ")";
                break;
            case SugiliteOperation.CHECK:
                verb = "CHECK";
                results = "(" + verb + " " + query.toString() + ")";
                break;
            case SugiliteOperation.UNCHECK:
                verb = "UNCHECK";
                results = "(" + verb + " " + query.toString() + ")";
                break;
            case SugiliteOperation.SELECT:
                verb = "SELECT";
                results = "(" + verb + " " + query.toString() + ")";
                break;
            case SugiliteOperation.RETURN:
                verb = "RETURN";
                results = "(" + verb + " " + query.toString() + ")";
                break;
            case SugiliteOperation.LOAD_AS_VARIABLE:
                verb = "LOAD_AS_VARIABLE";
                SugiliteLoadVariableOperation loadVariableOperation = (SugiliteLoadVariableOperation)operation;
                results = "(" + verb + " " + query.toString() + " " + loadVariableOperation.getPropertyToSave() + " " + loadVariableOperation.getVariableName() + ")";
                break;
            case SugiliteOperation.READ_OUT:
                verb = "READ_OUT";
                SugiliteReadoutOperation readoutOperation = (SugiliteReadoutOperation)operation;
                results = "(" + verb + " " + query.toString() + " " + readoutOperation.getPropertyToReadout() + ")";
                break;
            case SugiliteOperation.SET_TEXT:
                verb = "SET_TEXT";
                SugiliteSetTextOperation setTextOperation = (SugiliteSetTextOperation)operation;
                results = "(" + verb + " " + query.toString() + " " + setTextOperation.getText() + ")";
                break;
            case SugiliteOperation.SPECIAL_GO_HOME:
                verb = "SPECIAL_GO_HOME";
                results = "(" + verb + ")";
                break;
        }
        return results;
    }

    @Deprecated
    public void setElementMatchingFilter(UIElementMatchingFilter filter){
        this.elementMatchingFilter = filter;
    }
    @Deprecated
    public UIElementMatchingFilter getElementMatchingFilter(){
        return elementMatchingFilter;
    }
}
