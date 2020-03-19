package edu.cmu.hcii.sugilite.pumice.kb.generalization;

import android.app.Activity;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetBoolExpOperation;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceInitInstructionParsingHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUserExplainValueIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;

/**
 * @author toby
 * @date 3/6/19
 * @time 3:33 PM
 */
public class PumiceBooleanExpKnowledgeGeneralizationTask implements Callable<SugiliteOperation> {
    private PumiceDialogManager dialogManager;
    private Activity context;
    private SugiliteData sugiliteData;

    private SugiliteGetBoolExpOperation getBoolExpOperation;
    private String newProcedureScenario;

    private ExecutorService executorService;

    public PumiceBooleanExpKnowledgeGeneralizationTask(Activity context, PumiceDialogManager dialogManager, SugiliteData sugiliteData, ExecutorService executorService,  SugiliteGetBoolExpOperation getBoolExpOperation, String newProcedureScenario) {
        this.context = context;
        this.dialogManager = dialogManager;
        this.sugiliteData = sugiliteData;
        this.executorService = executorService;

        this.getBoolExpOperation = getBoolExpOperation;
        this.newProcedureScenario = newProcedureScenario;
    }



    /**
     * handle a SugiliteGetBoolExpOperation encountered in the query resolution -> check if the result PumiceBooleanExpKnowledge needs to be generalized
     */
    public SugiliteOperation call() {
        PumiceBooleanExpKnowledge matchedBooleanExpKnowledge = getBoolExpOperation.retrieveBoolExpKnowledge(sugiliteData);
        //check if the current scenario is known
        Map<String, SugiliteValue> scenarioArg1Map = matchedBooleanExpKnowledge.getScenarioArg1Map();
        if (scenarioArg1Map.containsKey(newProcedureScenario)) {
            //known scenario
            matchedBooleanExpKnowledge.setArg1(scenarioArg1Map.get(newProcedureScenario));
            PumiceInitInstructionParsingHandler.handleExistingGetFunctionInScript(dialogManager, (SugiliteGetBoolExpOperation) getBoolExpOperation);
            return getBoolExpOperation;
        } else {
            //new scenario -- need to ask
            //TODO: need a new dialog handler to ask if the new scenario should be matched to any of the old scenarios
            //figure out the new arg1
            SugiliteValue oldScenarioArg1Value = null;
            for (Map.Entry<String,SugiliteValue> entry : matchedBooleanExpKnowledge.getScenarioArg1Map().entrySet()) {
                //use the first one as the default
                oldScenarioArg1Value = entry.getValue();
                break;
            }
            final SugiliteValue finalOldScenarioArg1Value = oldScenarioArg1Value;

            //TODO: put a lock here

            PumiceBooleanExpKnowledgeGeneralizationIntentHandler pumiceBooleanExpKnowledgeGeneralizationIntentHandler = new PumiceBooleanExpKnowledgeGeneralizationIntentHandler(dialogManager, context, sugiliteData, matchedBooleanExpKnowledge, newProcedureScenario,
                    new Runnable() {
                        @Override
                        public void run() {
                            //positive runnable
                            //use the oldScenarioArg1Value
                            if (finalOldScenarioArg1Value != null) {
                                matchedBooleanExpKnowledge.getScenarioArg1Map().put(newProcedureScenario, finalOldScenarioArg1Value);
                                matchedBooleanExpKnowledge.setArg1(finalOldScenarioArg1Value);
                            } else {
                                throw new RuntimeException("finalOldScenarioArg1Value is null!");
                            }
                            synchronized (getBoolExpOperation) {
                                getBoolExpOperation.notify();
                            }
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            //negative runnable
                            SugiliteValue newArg1 = queryNewArg1(matchedBooleanExpKnowledge, newProcedureScenario);
                            if (newArg1 != null) {
                                matchedBooleanExpKnowledge.getScenarioArg1Map().put(newProcedureScenario, newArg1);
                                matchedBooleanExpKnowledge.setArg1(newArg1);
                            } else {
                                throw new RuntimeException("new Arg1 is null!");
                            }
                            synchronized (getBoolExpOperation) {
                                getBoolExpOperation.notify();
                            }
                        }
                    }, executorService);

            dialogManager.updateUtteranceIntentHandlerInANewState(pumiceBooleanExpKnowledgeGeneralizationIntentHandler);
            pumiceBooleanExpKnowledgeGeneralizationIntentHandler.sendPromptForTheIntentHandler();
            synchronized (getBoolExpOperation) {
                try {
                    System.out.println("waiting for getBoolExpOperation");
                    getBoolExpOperation.wait();
                } catch (Exception e){

                }
                System.out.println("finished waiting for getBoolExpOperation");
            }

            if (matchedBooleanExpKnowledge.getScenarioArg1Map().containsKey(newProcedureScenario)) {
                dialogManager.sendAgentMessage(String.format("OK, I learned that the threshold for \"%s\" is %s for determining whether to %s.", matchedBooleanExpKnowledge.getExpName(), matchedBooleanExpKnowledge.getScenarioArg1Map().get(newProcedureScenario).getReadableDescription(), newProcedureScenario), true, false);
            }
            return getBoolExpOperation;
        }
    }

    private SugiliteValue queryNewArg1(PumiceBooleanExpKnowledge matchedBooleanExpKnowledge, String newProcedureScenario){
        //locks used to notify() when a new intent has been handled by handlers that return a new knowledge object as the result
        PumiceValueQueryKnowledge resolveValueLock = new PumiceValueQueryKnowledge();
        String valueUtterance = String.format("new threshold for \"%s\"", matchedBooleanExpKnowledge.getExpName());

        //update the dialog manager with a new intent handler
        PumiceUserExplainValueIntentHandler pumiceUserExplainValueIntentHandler = new PumiceUserExplainValueIntentHandler(dialogManager, context, sugiliteData, resolveValueLock, valueUtterance, null);
        dialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainValueIntentHandler);
        dialogManager.sendAgentMessage(String.format("What's the new threshold for determining whether %s to %s? You can explain, or say \"demonstrate\" to demonstrate", matchedBooleanExpKnowledge.getExpName(), newProcedureScenario), true, true);
        //pumiceUserExplainValueIntentHandler.sendPromptForTheIntentHandler();

        //wait for the user to explain the bool exp
        synchronized (resolveValueLock) {
            try {
                System.out.println("waiting for the user to explain the value");
                resolveValueLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        System.out.println("Worker thread got server parsing result for value parsing " + resolveValueLock);
        PumiceValueQueryKnowledge valueQueryKnowledge = resolveValueLock;
        valueQueryKnowledge.setValueName(valueUtterance);

        //NOT adding the new knowledge to the knowledge manager
        return valueQueryKnowledge.getSugiliteValue();
    }
}
