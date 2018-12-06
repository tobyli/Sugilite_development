package edu.cmu.hcii.sugilite.pumice.dialog;

import android.content.Context;

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
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveValueQueryOperation;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUserExplainBoolExpIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUserExplainProcedureIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUserExplainValueIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;

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
    private SugiliteStartingBlock currentScript;
    private SugiliteScriptParser sugiliteScriptParser;
    private ExecutorService es;
    private PumiceDialogManager pumiceDialogManager;

    //locks used to notify() when a new intent has been handled by handlers that return a new knowledge object as the result
    public PumiceBooleanExpKnowledge resolveBoolExpLock = new PumiceBooleanExpKnowledge();
    public PumiceValueQueryKnowledge resolveValueLock = new PumiceValueQueryKnowledge();
    public PumiceProceduralKnowledge resolveProcedureLock = new PumiceProceduralKnowledge();

    private Context context;

    public PumiceInitInstructionParsingHandler(Context context, PumiceDialogManager pumiceDialogManager){
        this.context = context;
        this.sugiliteScriptParser = new SugiliteScriptParser();
        this.pumiceDialogManager = pumiceDialogManager;
        this.es = Executors.newCachedThreadPool();
    }

    /**
     * called externally to resolve all "resolve_" type function calls in the semantic parsing result
     * @param serverResult
     */
    public void parseFromNewInitInstruction(String serverResult){
        try {
            if(serverResult.length() > 0) {
                SugiliteStartingBlock script = sugiliteScriptParser.parseBlockFromString(serverResult);
                currentScript = script;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        //resolve the unknown concepts in the current script
        try {
            resolveScript(currentScript);
        } catch (Exception e){
            e.printStackTrace();
        }

        //done
        pumiceDialogManager.sendAgentMessage("I've finished resolving all concepts in the script", true, false);
        printCurrentScript();
    }


    /**
     * go through a block, recursively resolve unknown concepts in that block and ALL subsequent blocks
     * @param block
     */
    private void resolveScript(SugiliteBlock block) throws ExecutionException, InterruptedException{
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
                    ResolveOperationTask task = new ResolveOperationTask(context, pumiceDialogManager, (SugiliteResolveBoolExpOperation) booleanExpressionNew.getBoolOperation());
                    Future<SugiliteOperation> result = es.submit(task);
                    SugiliteOperation newOperation = result.get();
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
            resolveScript(((SugiliteConditionBlock) block).getIfBlock());
            resolveScript(((SugiliteConditionBlock) block).getElseBlock());
            resolveScript(block.getNextBlock());
        } else if (block instanceof SugiliteOperationBlock){
            if (((SugiliteOperationBlock) block).getOperation() instanceof SugiliteResolveProcedureOperation){
                ResolveOperationTask task = new ResolveOperationTask(context, pumiceDialogManager, ((SugiliteOperationBlock) block).getOperation());
                Future<SugiliteOperation> result = es.submit(task);
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
            resolveScript(block.getNextBlock());
        }
    }

    private class ResolveOperationTask implements Callable<SugiliteOperation> {
        /**
         * async task used for resolving unknown concepts in a "resolve" type of operation
         * @param operation
         */
        SugiliteOperation operation;
        PumiceDialogManager pumiceDialogManager;
        Context context;
        ResolveOperationTask(Context context, PumiceDialogManager pumiceDialogManager, SugiliteOperation operation){
            this.context = context;
            this.pumiceDialogManager = pumiceDialogManager;
            this.operation = operation;
        }
        @Override
        public SugiliteOperation call() throws Exception {
            if (operation instanceof SugiliteResolveProcedureOperation){
                String procedureUtterance = ((SugiliteResolveProcedureOperation) operation).getParameter0();
                pumiceDialogManager.sendAgentMessage("How do I " + procedureUtterance + "?" + " You can explain, or say \"demonstrate\" to demonstrate", true, true);
                //TODO: resolve -- user response - actually learn the procedure

                //update the dialog manager with a new intent handler
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceUserExplainProcedureIntentHandler(context));

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
                PumiceProceduralKnowledge proceduralKnowledge = resolveProcedureLock;
                proceduralKnowledge.setProcedureName(procedureUtterance);
                proceduralKnowledge.setUtterance(procedureUtterance);

                pumiceDialogManager.getPumiceKnowledgeManager().addPumiceProceduralKnowledge(proceduralKnowledge);
                pumiceDialogManager.sendAgentMessage("OK, I learned how to " + procedureUtterance + ".", true, false);
                return new SugiliteGetOperation<Void>(procedureUtterance, SugiliteGetOperation.PROCEDURE_NAME);
            }

            else if (operation instanceof SugiliteResolveValueQueryOperation){
                String valueUtterance = ((SugiliteResolveValueQueryOperation) operation).getParameter0();
                pumiceDialogManager.sendAgentMessage("How do I find out the value for " + valueUtterance + "?" + " You can explain, or say \"demonstrate\" to demonstrate", true, true);

                //update the dialog manager with a new intent handler
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceUserExplainValueIntentHandler(context));


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
                pumiceDialogManager.sendAgentMessage("OK, I learned how to find out the value for " + valueUtterance + ".", true, false);
                return new SugiliteGetOperation<Number>(valueUtterance, VALUE_QUERY_NAME);
            }

            else if (operation instanceof SugiliteResolveBoolExpOperation){
                String boolUtterance = ((SugiliteResolveBoolExpOperation) operation).getParameter0();
                pumiceDialogManager.sendAgentMessage("How do I tell whether " + boolUtterance + "?", true, true);
                //TODO: resolve -- user response - actually learn the boolean exp

                //update the dialog manager with a new intent handler
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceUserExplainBoolExpIntentHandler(context));

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

                pumiceDialogManager.getPumiceKnowledgeManager().addPumiceBooleanExpKnowledge(booleanExpKnowledge);
                pumiceDialogManager.sendAgentMessage("OK, I learned how to tell whether " + boolUtterance + ".", true, false);
                return new SugiliteGetOperation<Boolean>(boolUtterance, VALUE_QUERY_NAME);
            }

            throw new RuntimeException("wrong type of operation for resolving");
        }
    }

    private void printCurrentScript(){
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
        if (getOperation.getType().equals(VALUE_QUERY_NAME)){
            pumiceDialogManager.sendAgentMessage("I already know how to find out the value for " + getOperation.getName() + ".", true, false);
        } else if (getOperation.getType().equals(BOOL_FUNCTION_NAME)){
            pumiceDialogManager.sendAgentMessage("I already know how to tell whether " + getOperation.getName() + ".", true, false);
        } else if (getOperation.getType().equals(PROCEDURE_NAME)){
            pumiceDialogManager.sendAgentMessage("I already know how to " + getOperation.getName() + ".", true, false);
        }
    }


}
