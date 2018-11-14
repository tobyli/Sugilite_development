package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.util.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteBinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteTrinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteUnaryOperation;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:10 PM
 */
public class SugiliteOperationBlock extends SugiliteBlock implements Serializable{
    private SugiliteOperation operation;
    private SugiliteAvailableFeaturePack featurePack;
    //private SerializableOntologyQuery query;

    @Deprecated
    private UIElementMatchingFilter elementMatchingFilter;

    public boolean isSetAsABreakPoint = false;

    public SugiliteOperationBlock(){
        super();
        this.blockType = SugiliteBlock.REGULAR_OPERATION;
        this.setDescription("");
    }

    /*
    public void setQuery(SerializableOntologyQuery query) {
        this.query = query;
    }
    */

    public void setOperation(SugiliteOperation operation){
        this.operation = operation;
    }
    public void setFeaturePack(SugiliteAvailableFeaturePack featurePack){
        this.featurePack = featurePack;
    }

    /*
    public SerializableOntologyQuery getQuery() {
        return query;
    }
    */

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
                verb = "click";
                break;
            case SugiliteOperation.LONG_CLICK:
                verb = "long_click";
                break;
            case SugiliteOperation.SELECT:
                verb = "select";
                break;
            case SugiliteOperation.RETURN:
                verb = "return";
                break;
            case SugiliteOperation.LOAD_AS_VARIABLE:
                verb = "load_as_variable";
                break;
            case SugiliteOperation.READ_OUT:
                verb = "read_out";
                break;
            case SugiliteOperation.SET_TEXT:
                verb = "set_text";
                break;
            case SugiliteOperation.SPECIAL_GO_HOME:
                verb = "special_go_home";
                break;
            case SugiliteOperation.READOUT_CONST:
                verb = "readout_const";
                break;
            case SugiliteOperation.GET:
                verb = "get";
                break;
            case SugiliteOperation.RESOLVE_BOOLEXP:
                verb = "resolve_boolExp";
                break;
            case SugiliteOperation.RESOLVE_PROCEDURE:
                verb = "resolve_procedure";
                break;
            case SugiliteOperation.RESOLVE_VALUEQUERY:
                verb = "resolve_valueQuery";
                break;
        }

        if(operation instanceof SugiliteUnaryOperation){
            results = "(call " + verb + " " + addQuoteToTokenIfNeeded(((SugiliteUnaryOperation) operation).getParameter0().toString()) + ")";
        } else if (operation instanceof SugiliteBinaryOperation){
            results = "(call " + verb + " " + addQuoteToTokenIfNeeded(((SugiliteBinaryOperation) operation).getParameter0().toString()) + " " + addQuoteToTokenIfNeeded(((SugiliteBinaryOperation) operation).getParameter1().toString()) + ")";
        } else if (operation instanceof SugiliteTrinaryOperation){
            results = "(call " + verb + " " + addQuoteToTokenIfNeeded(((SugiliteTrinaryOperation) operation).getParameter0().toString()) + " " + addQuoteToTokenIfNeeded(((SugiliteTrinaryOperation) operation).getParameter1().toString()) + " " + addQuoteToTokenIfNeeded(((SugiliteTrinaryOperation) operation).getParameter2().toString()) + ")";
        } else {
            results = "(call " + verb + ")";
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
