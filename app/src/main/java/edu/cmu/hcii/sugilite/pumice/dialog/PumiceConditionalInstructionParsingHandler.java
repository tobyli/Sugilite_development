package edu.cmu.hcii.sugilite.pumice.dialog;

import android.app.Activity;

import java.util.concurrent.ExecutorService;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceScriptExecutingConfirmationIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceConditionalIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.SugiliteData;

public class PumiceConditionalInstructionParsingHandler extends PumiceInitInstructionParsingHandler {
    private SugiliteScriptParser sugiliteScriptParser;
    private ConditionalPumiceDialogManager pumiceDialogManager;
    private SugiliteStartingBlock script;
    private SugiliteBooleanExpressionNew boolExp;
    private Activity context;

    public PumiceConditionalInstructionParsingHandler(Activity context, ConditionalPumiceDialogManager pumiceDialogManager, SugiliteData sugilitedata){
        super(context,pumiceDialogManager, sugilitedata);
        this.context = context;
        this.pumiceDialogManager = pumiceDialogManager;
        this.sugiliteScriptParser = new SugiliteScriptParser();
    }

    /**
     * called externally to resolve all "resolve_" type function calls in the semantic parsing result
     * @param serverResultFormula
     */
    public void parseFromNewInitInstruction(String serverResultFormula, String userUtterance){
        script = null;
        try {
            if(serverResultFormula.length() > 0) {
                script = sugiliteScriptParser.parseBlockFromString(serverResultFormula);
            } else {
                throw new RuntimeException("empty server result!");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        //resolve the unknown concepts in the current script
        try {
            if(script != null) {
                resolveBlock(script);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        //done
        ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("I've finished resolving all concepts in the script");
        printScript(script);

        pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceConditionalIntentHandler(pumiceDialogManager,(ScriptDetailActivity) context,null));
    }


    public PumiceBooleanExpKnowledge parseFromBoolExpInstruction(String serverFormula, String userUtterance, String parentKnowledgeName){
        System.out.println("RECEIVED bool exp formula: " + serverFormula);

        if(serverFormula.length() == 0) {
            throw new RuntimeException("empty server result!");
        }

        else {
            SugiliteBooleanExpressionNew booleanExpression = sugiliteScriptParser.parseBooleanExpressionFromString(serverFormula);
            //resolve the unknown concepts in the boolean expression
            try {
                resolveBoolExpKnowledge(booleanExpression);
            } catch (Exception e) {
                e.printStackTrace();
            }

            PumiceBooleanExpKnowledge booleanExpKnowledge = new PumiceBooleanExpKnowledge(parentKnowledgeName, userUtterance, booleanExpression);
            boolExp = booleanExpression;
            ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("Ok, should I add the check for whether or not " + boolExp.getReadableDescription() + " to the script?");
            pumiceDialogManager.sendAgentMessage("Ok, should I add the check for whether or not " + booleanExpression.getReadableDescription() + " to the script?",true,true);
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceConditionalIntentHandler(pumiceDialogManager,(ScriptDetailActivity) context, PumiceUtteranceIntentHandler.PumiceIntent.ADD_CONDITIONAL_Y));

            return booleanExpKnowledge;
        }
    }

    public void printScript(SugiliteStartingBlock currentScript) {
        pumiceDialogManager.sendAgentMessage("What I understood to do is " + ((SugiliteConditionBlock) currentScript.getNextBlock()).getThenBlock().toString(), true, false);
        pumiceDialogManager.sendAgentMessage("Should I add this to the script?",true,true);
    }

    public SugiliteStartingBlock getScript() {
        return script;
    }

    public SugiliteBooleanExpressionNew getBoolExp() {
        return boolExp;
    }

}
