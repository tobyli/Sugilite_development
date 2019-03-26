package edu.cmu.hcii.sugilite.pumice.dialog;

import android.app.Activity;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetValueOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveValueQueryOperation;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceScriptExecutingConfirmationIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUserExplainBoolExpIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUserExplainProcedureIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUserExplainValueIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;


/**
 * @author toby
 * @date 11/14/18
 * @time 11:33 PM
 */

/**
 * this class handles the top-down parsing for resolving unknown concepts in scripts by replacing 'resolve' functions with 'get' functions and adding new entries into the knowledge graph
 */
public class PumiceInitInstructionParsingHandler {
    private SugiliteScriptParser sugiliteScriptParser;
    private ExecutorService es;
    private PumiceDialogManager pumiceDialogManager;

    private Activity context;

    public PumiceInitInstructionParsingHandler(Activity context, PumiceDialogManager pumiceDialogManager){
        this.context = context;
        this.sugiliteScriptParser = new SugiliteScriptParser();
        this.pumiceDialogManager = pumiceDialogManager;
        this.es = Executors.newCachedThreadPool();
    }

    /**
     * called externally to resolve all "resolve_" type function calls in the semantic parsing result
     * @param serverResultFormula
     */
    public void parseFromNewInitInstruction(String serverResultFormula, String userUtterance){
        SugiliteStartingBlock script = null;
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
        pumiceDialogManager.sendAgentMessage("I've finished resolving all concepts in the script", true, false);
        printScript(script);

        //ask if want to execute
        pumiceDialogManager.sendAgentMessage("I've understood your instruction: " + "\"" + userUtterance + "\"." + " Do you want me to execute it now?", true, true);
        pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceScriptExecutingConfirmationIntentHandler(context, script));

    }

	/*private void printCurrentScript() {
        if(!pumiceDialogManager.addElse) {
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceConditionalIntentHandler(pumiceDialogManager,context));
            pumiceDialogManager.sendAgentMessage("I understood to check if" + pumiceDialogManager.check, true, false);
            pumiceDialogManager.sendAgentMessage("Should I add this new check to the script?",true,true);
        }
        else {
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceDialogManager.getPcih());
            pumiceDialogManager.sendAgentMessage("What I understood to do is " + ((SugiliteConditionBlock) currentScript.getNextBlock()).getIfBlock().toString(), true, false);
            pumiceDialogManager.sendAgentMessage("Should I add this to the script?",true,true);
        }
*/
    protected void resolveBoolExpKnowledge (SugiliteBooleanExpressionNew booleanExpressionNew) throws ExecutionException, InterruptedException{
        if(booleanExpressionNew.getBoolOperation() != null){
            //this boolean expression is either a SugiliteResolveBoolExpOperation or a SugiliteGetOperation
            if(booleanExpressionNew.getBoolOperation() instanceof SugiliteResolveBoolExpOperation) {
                //if the booleanExpression uses a resolve_boolExp call
                ResolveOperationTask task = new ResolveOperationTask(context, pumiceDialogManager, (SugiliteResolveBoolExpOperation) booleanExpressionNew.getBoolOperation());
                Future<SugiliteOperation> result = es.submit(task);
                SugiliteOperation newOperation = result.get();

                //replace
                if (newOperation instanceof SugiliteGetBoolExpOperation) {
                    booleanExpressionNew.setBoolOperation((SugiliteGetOperation) newOperation);
                }

            } else if (booleanExpressionNew.getBoolOperation() instanceof SugiliteGetOperation){
                //if the booleanExpression uses a get call
                handleExistingGetFunctionInScript((SugiliteGetOperation)booleanExpressionNew.getBoolOperation());
            }
        } else {
            if (booleanExpressionNew.getArg0() instanceof SugiliteResolveValueQueryOperation){
                //if the booleanExpression uses a resolve_valueQuery call for arg0
                ResolveOperationTask task = new ResolveOperationTask(context, pumiceDialogManager, (SugiliteResolveValueQueryOperation)booleanExpressionNew.getArg0());
                Future<SugiliteOperation> result = es.submit(task);
                SugiliteOperation newOperation = result.get();

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
                ResolveOperationTask task = new ResolveOperationTask(context, pumiceDialogManager, (SugiliteResolveValueQueryOperation)booleanExpressionNew.getArg1());
                Future<SugiliteOperation> result = es.submit(task);
                SugiliteOperation newOperation = result.get();

                //replace
                if (newOperation instanceof SugiliteGetOperation){
                    booleanExpressionNew.setArg1((SugiliteGetOperation)newOperation);
                }
            } else if (booleanExpressionNew.getArg1() instanceof SugiliteGetOperation){
                //if the booleanExpression uses a get call
                handleExistingGetFunctionInScript((SugiliteGetOperation)booleanExpressionNew.getArg1());
            }
        }
    }


    /**
     * go through a block, recursively resolve unknown concepts in that block and ALL subsequent blocks
     * @param block
     */
    protected void resolveBlock(SugiliteBlock block) throws ExecutionException, InterruptedException{
        System.out.println("RESOLVE");
        System.out.println("block: " + block);
        if (block == null){
            //at the end of the script
            return;
        }
        if (block instanceof SugiliteStartingBlock){
            resolveBlock(block.getNextBlock());
        } else if (block instanceof SugiliteConditionBlock){
            //resolve for the conditional expression
            SugiliteBooleanExpressionNew booleanExpressionNew = ((SugiliteConditionBlock) block).getSugiliteBooleanExpressionNew();
            resolveBoolExpKnowledge(booleanExpressionNew);

            resolveBlock(((SugiliteConditionBlock) block).getThenBlock());
            resolveBlock(((SugiliteConditionBlock) block).getElseBlock());
            resolveBlock(block.getNextBlock());
        } else if (block instanceof SugiliteOperationBlock){
            if ((((SugiliteOperationBlock) block).getOperation() instanceof SugiliteResolveProcedureOperation) || (((SugiliteOperationBlock) block).getOperation() instanceof SugiliteResolveBoolExpOperation)){

                //the task used for resolving a SugiliteOperationBlock on a separate thread
                ResolveOperationTask task = new ResolveOperationTask(context, pumiceDialogManager, ((SugiliteOperationBlock) block).getOperation());
                Future<SugiliteOperation> result = es.submit(task);
                //TODO: deal with the stackoverflow error thrown by GSON - 190123
                SugiliteOperation newOperation = result.get();
                //replace
                if (newOperation instanceof SugiliteGetOperation){
                    ((SugiliteOperationBlock) block).setOperation(newOperation);
                }
            } else if (((SugiliteOperationBlock) block).getOperation() instanceof SugiliteGetOperation){
                //if the booleanExpression uses a get call
                handleExistingGetFunctionInScript((SugiliteGetOperation)((SugiliteOperationBlock) block).getOperation());
            }
            //TODO: handle resolving when resolve_valueQuery() used in operation parameters
            resolveBlock(block.getNextBlock());
        }
    }

    private class ResolveOperationTask implements Callable<SugiliteOperation> {
        /**
         * async task used for resolving unknown concepts in a "resolve" type of operation
         * @param operation
         */
        SugiliteOperation operation;
        PumiceDialogManager pumiceDialogManager;
        Activity context;
        ResolveOperationTask(Activity context, PumiceDialogManager pumiceDialogManager, SugiliteOperation operation){
            this.context = context;
            this.pumiceDialogManager = pumiceDialogManager;
            this.operation = operation;
        }
        @Override
        public SugiliteOperation call() throws Exception {
            System.out.println("RESOLVEOPERATIONTASK");
            if (operation instanceof SugiliteResolveProcedureOperation){
                String procedureUtterance = ((SugiliteResolveProcedureOperation) operation).getParameter0();
                pumiceDialogManager.sendAgentMessage("How do I " + procedureUtterance + "?" + " You can explain, or say \"demonstrate\" to demonstrate", true, true);
                //TODO: resolve -- user response - actually learn the procedure

                //locks used to notify() when a new intent has been handled by handlers that return a new knowledge object as the result
                PumiceProceduralKnowledge resolveProcedureLock = new PumiceProceduralKnowledge();

                //update the dialog manager with a new intent handler
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceUserExplainProcedureIntentHandler(pumiceDialogManager, context, resolveProcedureLock, procedureUtterance));

                //wait for the user to explain the bool exp
                synchronized (resolveProcedureLock) {
                    try {
                        System.out.println("waiting for the user to explain the procedure");
                        resolveProcedureLock.wait();
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                System.out.println("Worker thread got server parsing result for procedure parsing " + resolveProcedureLock);

                //get the resolved procedure knowledge back
                PumiceProceduralKnowledge proceduralKnowledge = resolveProcedureLock;

                pumiceDialogManager.getPumiceKnowledgeManager().addPumiceProceduralKnowledge(proceduralKnowledge);
                if(pumiceDialogManager.getContext() instanceof ScriptDetailActivity) {
                    ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("OK, I learned " + proceduralKnowledge.getProcedureDescription().toLowerCase() + ".");
                }
                pumiceDialogManager.sendAgentMessage("OK, I learned " + proceduralKnowledge.getProcedureDescription().toLowerCase() + ".", true, false);

                //replace the orignal "resolve" call with a new "get" call
                return new SugiliteGetProcedureOperation(procedureUtterance);
            }

            else if (operation instanceof SugiliteResolveValueQueryOperation){
                String valueUtterance = ((SugiliteResolveValueQueryOperation) operation).getParameter0();
                if(pumiceDialogManager.getContext() instanceof ScriptDetailActivity) {
                    ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("How do I find out the value for " + valueUtterance + "?" + " You can explain, or say \"demonstrate\" to demonstrate");
                }
                pumiceDialogManager.sendAgentMessage("How do I find out the value for " + valueUtterance + "?" + " You can explain, or say \"demonstrate\" to demonstrate", true, true);

                //locks used to notify() when a new intent has been handled by handlers that return a new knowledge object as the result
                PumiceValueQueryKnowledge resolveValueLock = new PumiceValueQueryKnowledge();

                //update the dialog manager with a new intent handler
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceUserExplainValueIntentHandler(pumiceDialogManager, context, resolveValueLock, valueUtterance));


                //wait for the user to explain the bool exp
                synchronized (resolveValueLock) {
                    try {
                        System.out.println("waiting for the user to explain the value");
                        resolveValueLock.wait();
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                System.out.println("Worker thread got server parsing result for value parsing " + resolveValueLock);
                PumiceValueQueryKnowledge valueQueryKnowledge = resolveValueLock;
                valueQueryKnowledge.setValueName(valueUtterance);

                //PumiceValueQueryKnowledge valueQueryKnowledge = new PumiceValueQueryKnowledge(valueUtterance, PumiceValueQueryKnowledge.ValueType.STRING);
                pumiceDialogManager.getPumiceKnowledgeManager().addPumiceValueQueryKnowledge(valueQueryKnowledge);
                if(pumiceDialogManager.getContext() instanceof ScriptDetailActivity) {
                    ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("OK, I learned how to find out the value for " + valueUtterance + ".");
                }
                pumiceDialogManager.sendAgentMessage("OK, I learned how to find out the value for " + valueUtterance + ".", true, false);
                return new SugiliteGetValueOperation<Number>(valueUtterance);
            }

            else if (operation instanceof SugiliteResolveBoolExpOperation){
                String boolUtterance = ((SugiliteResolveBoolExpOperation) operation).getParameter0();
                if(pumiceDialogManager.getContext() instanceof ScriptDetailActivity) {
                    ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("How do I tell whether " + boolUtterance + "?");
                }
                pumiceDialogManager.sendAgentMessage("How do I tell whether " + boolUtterance + "?", true, true);
                //TODO: resolve -- user response - actually learn the boolean exp

                //locks used to notify() when a new intent has been handled by handlers that return a new knowledge object as the result
                PumiceBooleanExpKnowledge resolveBoolExpLock = new PumiceBooleanExpKnowledge();
                //update the dialog manager with a new intent handler
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceUserExplainBoolExpIntentHandler(pumiceDialogManager, context, resolveBoolExpLock, boolUtterance));

                //wait for the user to explain the bool exp
                synchronized (resolveBoolExpLock) {
                    try {
                        System.out.println("waiting for the user to explain the boolean exp");
                        resolveBoolExpLock.wait();
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                System.out.println("Worker thread got server parsing result for bool exp parsing " + resolveBoolExpLock);
                PumiceBooleanExpKnowledge booleanExpKnowledge = resolveBoolExpLock;
                booleanExpKnowledge.setExpName(boolUtterance);

                //add the learned knowledge to the knowledge manager
                //replace the original "resolve" statement with a "get" statement to retrieve the boolean exp from the knowledge manager
                pumiceDialogManager.getPumiceKnowledgeManager().addPumiceBooleanExpKnowledge(booleanExpKnowledge);
                if(pumiceDialogManager.getContext() instanceof ScriptDetailActivity) {
                    ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("OK, I learned how to tell whether " + boolUtterance + ".");
                }
                pumiceDialogManager.sendAgentMessage("OK, I learned how to tell whether " + boolUtterance + ".", true, false);
                return new SugiliteGetBoolExpOperation(boolUtterance);
            }

            throw new RuntimeException("wrong type of operation for resolving");
        }
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
            return booleanExpKnowledge;
        }
    }

    public void printScript(SugiliteStartingBlock currentScript){
        pumiceDialogManager.sendAgentMessage("Below is the current script after concept resolution: ", true, false);
        pumiceDialogManager.sendAgentMessage(sugiliteScriptParser.scriptToString(currentScript), false, false);
        pumiceDialogManager.sendAgentMessage("Below is the updated list of existing knowledge...", true, false);
        pumiceDialogManager.sendAgentMessage(pumiceDialogManager.getPumiceKnowledgeManager().getKnowledgeInString(), false, false);
    }

    /**
     * method called when process a "get" operation encountered in the parsing process
     * @param getOperation
     */
    private void handleExistingGetFunctionInScript(SugiliteGetOperation getOperation){
        if (getOperation instanceof SugiliteGetValueOperation){
            if(pumiceDialogManager.getContext() instanceof ScriptDetailActivity) {
                ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("I already know how to find out the value for " + getOperation.getName() + ".");
            }
            pumiceDialogManager.sendAgentMessage("I already know how to find out the value for " + getOperation.getName() + ".", true, false);
        } else if (getOperation instanceof SugiliteGetBoolExpOperation){
            if(pumiceDialogManager.getContext() instanceof ScriptDetailActivity) {
                ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("I already know how to tell whether " + getOperation.getName() + ".");
            }
            pumiceDialogManager.sendAgentMessage("I already know how to tell whether " + getOperation.getName() + ".", true, false);
        } else if (getOperation instanceof SugiliteGetProcedureOperation){
            if(pumiceDialogManager.getContext() instanceof ScriptDetailActivity) {
                ((ScriptDetailActivity) pumiceDialogManager.getContext()).addSnackbar("I already know how to " + getOperation.getName() + ".");
            }
            pumiceDialogManager.sendAgentMessage("I already know how to " + getOperation.getName() + ".", true, false);
        }
    }


}
