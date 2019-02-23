package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation;

import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveProcedureOperation;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;

/**
 * @author toby
 * @date 2/19/19
 * @time 2:21 PM
 */
public class PumiceParsingResultDescriptionGenerator {
    private SugiliteScriptParser sugiliteScriptParser;

    public PumiceParsingResultDescriptionGenerator(){
        this.sugiliteScriptParser = new SugiliteScriptParser();

    }
    public String generateForConditionBlock(String formula) {
        SugiliteStartingBlock script = null;
        script = sugiliteScriptParser.parseBlockFromString(formula);
        if (script.getNextBlock() instanceof SugiliteConditionBlock){
            return script.getNextBlock().getPumiceUserReadableDecription();
        } else {
            throw new RuntimeException("wrong type of formula! expecting a conditional block");
        }
    }

    public String generateForOperationBlock(String formula) {
        SugiliteStartingBlock script = null;
        script = sugiliteScriptParser.parseBlockFromString(formula);
        if (script.getNextBlock() instanceof SugiliteOperationBlock){
            return script.getNextBlock().getPumiceUserReadableDecription();
        } else {
            SugiliteValue sugiliteValue = sugiliteScriptParser.parseSugiliteValueFromString(formula);
            //resolve the unknown concepts in the value instruction
            if (sugiliteValue instanceof SugiliteGetProcedureOperation) {
                return ((SugiliteGetProcedureOperation) sugiliteValue).getPumiceUserReadableDecription();
            }
            if (sugiliteValue instanceof SugiliteResolveProcedureOperation) {
                return ((SugiliteResolveProcedureOperation) sugiliteValue).getPumiceUserReadableDecription();
            }
            else {
                throw new RuntimeException("wrong type of formula! expecting an operation");
            }
        }
    }


    public String generateForValue(String formula) {
        SugiliteValue sugiliteValue = sugiliteScriptParser.parseSugiliteValueFromString(formula);
        return sugiliteValue.getReadableDescription();
    }

    public String generateForBoolExp(String formula) {
        SugiliteBooleanExpressionNew booleanExpression = sugiliteScriptParser.parseBooleanExpressionFromString(formula);
        return booleanExpression.getReadableDescription();
    }


}
