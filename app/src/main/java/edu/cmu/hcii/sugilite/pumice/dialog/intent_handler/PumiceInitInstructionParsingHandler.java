package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveValueQueryOperation;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;

import static edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation.BOOL_FUNCTION_NAME;
import static edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation.PROCEDURE_NAME;
import static edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation.VALUE_QUERY_NAME;

/**
 * @author toby
 * @date 11/14/18
 * @time 11:33 PM
 */

/**
 * this class handles the top-down parsing for resolving unknown concepts in scripts by replacing 'resolve' functions with 'get' functions and adding new entries into the knowledge graph
 */
public class PumiceInitInstructionParsingHandler {
    public SugiliteStartingBlock currentScript;//was private
    private SugiliteScriptParser sugiliteScriptParser;
    private PumiceDialogManager pumiceDialogManager;
    Context context;
    public PumiceInitInstructionParsingHandler(Context context, PumiceDialogManager pumiceDialogManager){
        this.context = context;
        this.sugiliteScriptParser = new SugiliteScriptParser();
        this.pumiceDialogManager = pumiceDialogManager;

    }
    public void parseFromNewInitInstruction(String serverResult){
        System.out.println("ASDF");
        System.out.println(serverResult);
        currentScript = null;
        try {
            if(serverResult.length() > 0 && serverResult.contains("resolve_")) {
                SugiliteStartingBlock script = sugiliteScriptParser.parseBlockFromString(serverResult);
                currentScript = script;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        //resolve the unknown concepts in the current script
        resolveScript(currentScript);
        pumiceDialogManager.settResult(currentScript.getNextBlock());
        System.out.println("ASDF2");
        System.out.println(serverResult);
        System.out.println(currentScript);
        System.out.println(currentScript.getNextBlock());

        //done
        if(context instanceof ScriptDetailActivity) {
            pumiceDialogManager.sendAgentMessage("I've finished resolving all concepts you mentioned", true, false);
        }
        else {
            pumiceDialogManager.sendAgentMessage("I've finished resolving all concepts in the script", true, false);
        }
        printCurrentScript();
    }

    private void printCurrentScript(){
        if(context instanceof ScriptDetailActivity && !pumiceDialogManager.addElse) {
            pumiceDialogManager.sendAgentMessage("I understood to check if" + pumiceDialogManager.check, true, false);
            pumiceDialogManager.sendAgentMessage("Should I add this new check to the script?",true,true);
        }
        else if(context instanceof ScriptDetailActivity) {
            pumiceDialogManager.sendAgentMessage("What I understood to do is " + ((SugiliteConditionBlock) currentScript.getNextBlock()).getIfBlock().toString(), true, false);
            pumiceDialogManager.sendAgentMessage("Should I add this to the script?",true,true);
        }
        else {
            pumiceDialogManager.sendAgentMessage("Below is the current script after concept resolution: ", true, false);
            pumiceDialogManager.sendAgentMessage(sugiliteScriptParser.scriptToString(currentScript), false, false);
            pumiceDialogManager.sendAgentMessage("Below is the updated list of existing knowledge: \n\n" + pumiceDialogManager.getPumiceKnowledgeManager().getKnowledgeInString(), true, false);
        }
    }

    /**
     * go through a block, resolve unknown concepts in that block and ALL subsequent blocks
     * @param block
     */
    private void resolveScript(SugiliteBlock block){
        if (block == null){
            //at the end of the script
            return;
        }
        if (block instanceof SugiliteStartingBlock){
            resolveScript(block.getNextBlock());
        } else if (block instanceof SugiliteConditionBlock){
            //resolve for the conditional expression
            SugiliteBooleanExpressionNew booleanExpressionNew = ((SugiliteConditionBlock) block).getSugiliteBooleanExpressionNew();
            if(booleanExpressionNew.getBoolOperation() != null){
                if(booleanExpressionNew.getBoolOperation() instanceof SugiliteResolveBoolExpOperation) {
                    //if the booleanExpression uses a resolve_boolExp call
                    SugiliteOperation newOperation = resolveOperation((SugiliteResolveBoolExpOperation) booleanExpressionNew.getBoolOperation());
                    //replace
                    if (newOperation instanceof SugiliteGetOperation && ((SugiliteGetOperation) newOperation).evaluate() instanceof Boolean) {
                        booleanExpressionNew.setBoolOperation((SugiliteGetOperation) newOperation);
                    }
                } else if (booleanExpressionNew.getBoolOperation() instanceof SugiliteGetOperation){
                    //if the booleanExpression uses a get call
                    handleExistingGetFunctionInScript((SugiliteGetOperation)booleanExpressionNew.getBoolOperation());
                }
            } else {
                if (booleanExpressionNew.getArg0() instanceof SugiliteResolveValueQueryOperation){
                    //if the booleanExpression uses a resolve_valueQuery call for arg0
                    SugiliteOperation newOperation = resolveOperation((SugiliteResolveValueQueryOperation)booleanExpressionNew.getArg0());
                    //replace
                    if (newOperation instanceof SugiliteGetOperation){
                        booleanExpressionNew.setArg0((SugiliteGetOperation)newOperation);
                    }
                } else if (booleanExpressionNew.getArg0() instanceof SugiliteGetOperation){
                    //if the booleanExpression uses a get call
                    handleExistingGetFunctionInScript((SugiliteGetOperation)booleanExpressionNew.getArg0());
                }
                if (booleanExpressionNew.getArg1() instanceof SugiliteResolveValueQueryOperation){
                    //if the booleanExpression uses a resolve_valueQuery call for arg1
                    SugiliteOperation newOperation = resolveOperation((SugiliteResolveValueQueryOperation)booleanExpressionNew.getArg1());
                    //replace
                    if (newOperation instanceof SugiliteGetOperation){
                        booleanExpressionNew.setArg1((SugiliteGetOperation)newOperation);
                    }
                } else if (booleanExpressionNew.getArg1() instanceof SugiliteGetOperation){
                    //if the booleanExpression uses a get call
                    handleExistingGetFunctionInScript((SugiliteGetOperation)booleanExpressionNew.getArg1());
                }
            }
            resolveScript(((SugiliteConditionBlock) block).getIfBlock());
            resolveScript(((SugiliteConditionBlock) block).getElseBlock());
            resolveScript(block.getNextBlock());
        } else if (block instanceof SugiliteOperationBlock){
            if (((SugiliteOperationBlock) block).getOperation() instanceof SugiliteResolveProcedureOperation){
                SugiliteOperation newOperation = resolveOperation(((SugiliteOperationBlock) block).getOperation());
                //replace
                if (newOperation instanceof SugiliteGetOperation){
                    ((SugiliteOperationBlock) block).setOperation(newOperation);
                }
            } else if (((SugiliteOperationBlock) block).getOperation() instanceof SugiliteGetOperation){
                //if the booleanExpression uses a get call
                handleExistingGetFunctionInScript((SugiliteGetOperation)((SugiliteOperationBlock) block).getOperation());
            }
            //TODO: handle resolving when resolve_valueQuery() used in operation parameters
            resolveScript(block.getNextBlock());
        }
    }
    private void handleExistingGetFunctionInScript(SugiliteGetOperation getOperation){
        if (getOperation.getType().equals(VALUE_QUERY_NAME)){
            pumiceDialogManager.sendAgentMessage("I already know how to find out the value for " + getOperation.getName() + ".", true, false);
        } else if (getOperation.getType().equals(BOOL_FUNCTION_NAME)){
            pumiceDialogManager.check = getOperation.getName();
            pumiceDialogManager.sendAgentMessage("I already know how to tell whether " + getOperation.getName() + ".", true, false);
        } else if (getOperation.getType().equals(PROCEDURE_NAME)){
            pumiceDialogManager.sendAgentMessage("I already know how to " + getOperation.getName() + ".", true, false);
        }
    }

    /**
     * resolve unknown concepts in a "resolve" type of operation
     * @param operation
     */
    private SugiliteOperation resolveOperation(SugiliteOperation operation){
        if (operation instanceof SugiliteResolveProcedureOperation){
            String procedureUtterance = ((SugiliteResolveProcedureOperation) operation).getParameter0();
            if(!pumiceDialogManager.justChecking) {
                pumiceDialogManager.sendAgentMessage("How do I " + procedureUtterance + "?", true, false);
            }
            //TODO: resolve -- user response

            //for testing purpose
            List<String> appList = new ArrayList<>();
            appList.add("Test App");
            PumiceProceduralKnowledge proceduralKnowledge = new PumiceProceduralKnowledge(procedureUtterance, procedureUtterance, appList);

            pumiceDialogManager.getPumiceKnowledgeManager().addPumiceProceduralKnowledge(proceduralKnowledge);
            if(!pumiceDialogManager.justChecking) {
                pumiceDialogManager.sendAgentMessage("OK, I learned how to " + procedureUtterance + ".", true, false);
            }
            return new SugiliteGetOperation<Void>(procedureUtterance, SugiliteGetOperation.PROCEDURE_NAME);
        }

        else if (operation instanceof SugiliteResolveValueQueryOperation) {
            String valueUtterance = ((SugiliteResolveValueQueryOperation) operation).getParameter0();
            if (!pumiceDialogManager.justChecking) {
                pumiceDialogManager.sendAgentMessage("How do I find out the value for " + valueUtterance + "?", true, false);
            }
            //TODO: resolve -- user response
            PumiceValueQueryKnowledge valueQueryKnowledge = new PumiceValueQueryKnowledge(valueUtterance, PumiceValueQueryKnowledge.ValueType.STRING);
            pumiceDialogManager.getPumiceKnowledgeManager().addPumiceValueQueryKnowledge(valueQueryKnowledge);
            if(!pumiceDialogManager.justChecking) {
                pumiceDialogManager.sendAgentMessage("OK, I learned how to find out the value for " + valueUtterance + ".", true, false);
            }
            return new SugiliteGetOperation<Number>(valueUtterance, VALUE_QUERY_NAME);
        }

        else if (operation instanceof SugiliteResolveBoolExpOperation){
            String boolUtterance = ((SugiliteResolveBoolExpOperation) operation).getParameter0();
            pumiceDialogManager.check = boolUtterance;
            if(!pumiceDialogManager.addElse) {
                pumiceDialogManager.sendAgentMessage("How do I tell whether " + boolUtterance + "?", true, false);
            }
            //TODO: resolve -- user response
            PumiceBooleanExpKnowledge booleanExpKnowledge = new PumiceBooleanExpKnowledge(boolUtterance, boolUtterance, null, null, null);
            pumiceDialogManager.getPumiceKnowledgeManager().addPumiceBooleanExpKnowledge(booleanExpKnowledge);
            if(!pumiceDialogManager.addElse) {
                pumiceDialogManager.sendAgentMessage("OK, I learned how to tell whether " + boolUtterance + ".", true, false);
            }
            return new SugiliteGetOperation<Boolean>(boolUtterance, VALUE_QUERY_NAME);
        }

        throw new RuntimeException("wrong type of operation for resolving");
    }

}
