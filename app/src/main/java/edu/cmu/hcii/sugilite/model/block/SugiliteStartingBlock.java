package edu.cmu.hcii.sugilite.model.block;

import android.text.Html;
import android.text.SpannableString;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 6/13/16
 * @time 1:48 PM
 */
public class SugiliteStartingBlock extends SugiliteBlock implements Serializable {
    //the name of the script
    private String scriptName;

    //the name of packages (apps) involved in this script -- used for terminating all relevant apps before the execution
    public Set<String> relevantPackages;

    //persistent across launches, used to store the list of names for variables
    public Map<String, Variable> variableNameVariableObjectMap;
    public Map<String, VariableValue> variableNameDefaultValueMap;
    public Map<String, Set<VariableValue>> variableNameAlternativeValueMap;

    public SerializableUISnapshot uiSnapshotOnEnd;
    public File screenshotOnEnd;


    public SugiliteStartingBlock(){
        super();
        relevantPackages = new HashSet<>();
        variableNameVariableObjectMap = new HashMap<>();
        variableNameDefaultValueMap = new HashMap<>();
        variableNameAlternativeValueMap = new HashMap<>();
        this.blockType = SugiliteBlock.STARTING_BLOCK;
        try {
            this.setDescription(Html.fromHtml("<b>START SCRIPT</b>"));
        } catch (Exception e) {
            e.printStackTrace();
            this.setDescription("START SCRIPT");
        }
    }
    public SugiliteStartingBlock(String scriptName){
        super();
        relevantPackages = new HashSet<>();
        variableNameVariableObjectMap = new HashMap<>();
        variableNameDefaultValueMap = new HashMap<>();
        variableNameAlternativeValueMap = new HashMap<>();
        this.scriptName = scriptName;
        this.blockType = SugiliteBlock.STARTING_BLOCK;
        try {
            this.setDescription(Html.fromHtml("<b>START SCRIPT</b>"));
        } catch (Exception e) {
            e.printStackTrace();
            this.setDescription("START SCRIPT");
        }
    }

    public String getScriptName(){
        return scriptName;
    }
    public void setScriptName(String scriptName) {this.scriptName = scriptName;}


    /**
     * get the last block in the script
     * @return
     */
    //TODO: need to refactor to work better with conditionals
    public SugiliteBlock getTail(){
        SugiliteBlock currentBlock = this;
        while(true){
            if(currentBlock instanceof SugiliteStartingBlock){
                if(((SugiliteStartingBlock) currentBlock).getNextBlockToRun() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
            }
            else if(currentBlock instanceof SugiliteOperationBlock){
                if(((SugiliteOperationBlock) currentBlock).getNextBlockToRun() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlockToRun();
            }
            else if(currentBlock instanceof SugiliteSpecialOperationBlock){
                if(((SugiliteSpecialOperationBlock) currentBlock).getNextBlockToRun() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteSpecialOperationBlock) currentBlock).getNextBlockToRun();
            }
            else if(currentBlock instanceof SugiliteConditionBlock){
                if(((SugiliteConditionBlock) currentBlock).getNextBlockToRun() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteConditionBlock) currentBlock).getNextBlockToRun();
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

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("Start the script: \"%s\"", PumiceDemonstrationUtil.removeScriptExtension(scriptName));
    }
}
