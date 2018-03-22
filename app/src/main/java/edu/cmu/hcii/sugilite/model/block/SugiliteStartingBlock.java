package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.operation.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.operation.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 6/13/16
 * @time 1:48 PM
 */
public class SugiliteStartingBlock extends SugiliteBlock implements Serializable {
    private String scriptName;
    public Set<String> relevantPackages;

    //persistent across launches, used to store the list of names for variables
    public Map<String, Variable> variableNameDefaultValueMap;
    public Map<String, Set<String>> variableNameAlternativeValueMap;

    public SugiliteStartingBlock(){
        super();
        relevantPackages = new HashSet<>();
        variableNameDefaultValueMap = new HashMap<>();
        variableNameAlternativeValueMap = new HashMap<>();
        this.blockType = SugiliteBlock.STARTING_BLOCK;
        this.setDescription("<b>START SCRIPT</b>");
    }
    public SugiliteStartingBlock(String scriptName){
        super();
        relevantPackages = new HashSet<>();
        variableNameDefaultValueMap = new HashMap<>();
        variableNameAlternativeValueMap = new HashMap<>();
        this.scriptName = scriptName;
        this.blockType = SugiliteBlock.STARTING_BLOCK;
        this.setDescription("<b>START SCRIPT</b>");
    }

    public String getScriptName(){
        return scriptName;
    }
    public void setScriptName(String scriptName) {this.scriptName = scriptName;}


    public SugiliteBlock getTail(){
        SugiliteBlock currentBlock = this;
        while(true){
            if(currentBlock instanceof SugiliteStartingBlock){
                if(((SugiliteStartingBlock) currentBlock).getNextBlock() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlock();
            }
            else if(currentBlock instanceof SugiliteOperationBlock){
                if(((SugiliteOperationBlock) currentBlock).getNextBlock() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlock();
            }
            else if(currentBlock instanceof SugiliteSpecialOperationBlock){
                if(((SugiliteSpecialOperationBlock) currentBlock).getNextBlock() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteSpecialOperationBlock) currentBlock).getNextBlock();
            }
            else if(currentBlock instanceof SugiliteErrorHandlingForkBlock){
                if(((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock();
            }
        }
    }

    @Override
    public String toString() {
        return "(SUGILITE_START " + addQuoteToTokenIfNeeded(scriptName) + ")";
    }
}
