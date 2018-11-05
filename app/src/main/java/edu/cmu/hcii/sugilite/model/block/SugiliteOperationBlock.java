package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.block.operation.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.util.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteBinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteTrinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:10 PM
 */
public class SugiliteOperationBlock extends SugiliteBlock implements Serializable{
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
    public void setQuery(SerializableOntologyQuery query) {
        this.query = query;
    }
    public void setOperation(SugiliteOperation operation){
        this.operation = operation;
    }
    public void setFeaturePack(SugiliteAvailableFeaturePack featurePack){
        this.featurePack = featurePack;
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
        if(previousBlock instanceof SugiliteConditionBlock)
            ((SugiliteConditionBlock) previousBlock).setNextBlock(null);
    }

    @Override
    public String toString() {
        //TODO: get the string for the block
        String results = "";
        String verb = "";
        switch (operation.getOperationType()){
            case SugiliteOperation.CLICK:
                verb = "CLICK";
                break;
            case SugiliteOperation.LONG_CLICK:
                verb = "LONG_CLICK";
                break;
            case SugiliteOperation.SELECT:
                verb = "SELECT";
                break;
            case SugiliteOperation.RETURN:
                verb = "RETURN";
                break;
            case SugiliteOperation.LOAD_AS_VARIABLE:
                verb = "LOAD_AS_VARIABLE";
                break;
            case SugiliteOperation.READ_OUT:
                verb = "READ_OUT";
                break;
            case SugiliteOperation.SET_TEXT:
                verb = "SET_TEXT";
                break;
            case SugiliteOperation.SPECIAL_GO_HOME:
                verb = "SPECIAL_GO_HOME";
                break;
            case SugiliteOperation.READOUT_CONST:
                verb = "READOUT_CONST";
                break;
        }

        //for handling when query == null (e.g. READOUT_CONST operations)
        String queryString = "";
        if(query != null){
            queryString += " ";
            queryString += query.toString();
        }

        if(operation instanceof SugiliteUnaryOperation){
            results = "(" + verb + queryString + ")";
        } else if (operation instanceof SugiliteBinaryOperation){
            results = "(" + verb + " " + addQuoteToTokenIfNeeded(((SugiliteBinaryOperation) operation).getParameter1()) + queryString + ")";
        } else if (operation instanceof SugiliteTrinaryOperation){
            results = "(" + verb + " " + addQuoteToTokenIfNeeded(((SugiliteTrinaryOperation) operation).getParameter1()) + " " + addQuoteToTokenIfNeeded(((SugiliteTrinaryOperation) operation).getParameter2()) + queryString + ")";
        } else {
            results = "(" + verb + ")";
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
