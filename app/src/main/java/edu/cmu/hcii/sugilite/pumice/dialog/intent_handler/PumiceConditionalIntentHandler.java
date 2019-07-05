package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.support.design.widget.Snackbar;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceConditionalInstructionParsingHandler;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.pumice.dialog.ConditionalPumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.else_statement.PumiceUserExplainElseStatementIntentHandler;


public class PumiceConditionalIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private ScriptDetailActivity sourceActivity;
    private ConditionalPumiceDialogManager dialogManager;
    private Context context;
    private ExecutorService es;
    private Calendar calendar;
    private PumiceIntent lastIntent; //store last PumiceIntent
    private String check; //store check String given by user
    private boolean then; //whether or not initial condition given is for then block or else block
    private String boolExp; //store boolean expression
    private boolean changeCheck; //whether or not changing check as opposed to creating one for first time

    public PumiceConditionalIntentHandler(ConditionalPumiceDialogManager dialogManager, ScriptDetailActivity sourceActivity, PumiceIntent lastIntent){
        this.dialogManager = dialogManager;
        this.context = sourceActivity;
        this.calendar = Calendar.getInstance();
        this.es = Executors.newCachedThreadPool();
        this.sourceActivity = sourceActivity;
        this.lastIntent = lastIntent;
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String text = utterance.getContent().toLowerCase();
        System.out.println("lastIntent: " + lastIntent);
        System.out.println("text: " + text);
        boolean yes = text.contains("yes") || text.contains("yeah") || text.contains("yep") || text.contains("yup");
        boolean no = text.contains("no") || text.contains("nope") || text.contains("nah");
        boolean idk = text.contains("don't know") || text.contains("not sure") || text.contains("do not know");
        if(lastIntent == PumiceIntent.PROBLEM_3 && yes) {
            changeCheck = true;
            return PumiceIntent.ADD_CONDITIONAL_N;
        }
        if(lastIntent == PumiceIntent.CHECK_CONDITION && yes) {
            return PumiceIntent.PROBLEM_3;
        }
        if(lastIntent == PumiceIntent.CHECK_CONDITION && no) {
            return PumiceIntent.DONE_2;
        }
        if(lastIntent == PumiceIntent.CHECK_CONDITION) {
            return PumiceIntent.PROBLEM_3_X;
        }
        if(lastIntent == PumiceIntent.WANT_CHECK_CONDITION && yes) {
            return PumiceIntent.CHECK_CONDITION;
        }
        if(lastIntent == PumiceIntent.WANT_CHECK_CONDITION && no) {
            return PumiceIntent.DONE_2;
        }
        if(lastIntent == PumiceIntent.WANT_CHECK_CONDITION) {
            return PumiceIntent.CHECK_CONDITION_X;
        }
        if(lastIntent == PumiceIntent.CHECKING_ELSE && no) {
            return PumiceIntent.WANT_CHECK_CONDITION;
        }
        if(lastIntent == PumiceIntent.CHECKING_ELSE && yes) {
            return PumiceIntent.GET_ELSE_BLOCK_Y;
        }
        if(lastIntent == PumiceIntent.CHECKING_ELSE) {
            return PumiceIntent.PROBLEM_2_X;
        }
        if((lastIntent == PumiceIntent.GET_ELSE_BLOCK_Y || lastIntent == PumiceIntent.CHECKING_ELSE_X) && (yes || idk)) {
            return PumiceIntent.CHECKING_ELSE;
        }
        if((lastIntent == PumiceIntent.GET_ELSE_BLOCK_Y || lastIntent == PumiceIntent.CHECKING_ELSE_X) && no) {
            return PumiceIntent.GET_ELSE_BLOCK_Y;
        }
        if(lastIntent == PumiceIntent.GET_ELSE_BLOCK_Y || lastIntent == PumiceIntent.CHECKING_ELSE_X) {
            return PumiceIntent.CHECKING_ELSE_X;
        }
        if(((lastIntent == PumiceIntent.WANT_ELSE_BLOCK) || (lastIntent == PumiceIntent.GET_ELSE_BLOCK_X)) && yes) {
            return PumiceIntent.GET_ELSE_BLOCK_Y;
        }
        if(((lastIntent == PumiceIntent.WANT_ELSE_BLOCK) || (lastIntent == PumiceIntent.GET_ELSE_BLOCK_X)) && idk) {
            return PumiceIntent.GET_ELSE_BLOCK_X;
        }
        if(lastIntent == PumiceIntent.WANT_ELSE_BLOCK && no) {
            return PumiceIntent.WANT_CHECK_CONDITION;
        }
        if((lastIntent == PumiceIntent.GET_THEN_BLOCK || lastIntent == PumiceIntent.PROBLEM_1_X || lastIntent == PumiceIntent.GET_THEN_BLOCK_IDK) && no) {
            return PumiceIntent.WANT_ELSE_BLOCK;
        }
        if((lastIntent == PumiceIntent.SOLUTION_2_Y || lastIntent == PumiceIntent.MOVE_STEP_2_X) && text.matches(".*\\d+.*")) {
            return PumiceIntent.MOVE_STEP_2;
        }
        if(lastIntent == PumiceIntent.SOLUTION_2_Y || lastIntent == PumiceIntent.MOVE_STEP_2_X) {
            return PumiceIntent.MOVE_STEP_2_X;
        }
        if((lastIntent == PumiceIntent.SOLUTION_1_N || lastIntent == PumiceIntent.SOLUTION_2_X) && yes) {
            return PumiceIntent.SOLUTION_2_Y;
        }
        if((lastIntent == PumiceIntent.SOLUTION_1_N || lastIntent == PumiceIntent.SOLUTION_2_X) && (no || idk)) {
            return PumiceIntent.SOLUTION_2_N;
        }
        if(lastIntent == PumiceIntent.SOLUTION_1_N || lastIntent == PumiceIntent.SOLUTION_2_X) {
            return PumiceIntent.SOLUTION_2_X;
        }
        if(lastIntent == PumiceIntent.SOLUTION_1_IDK) {
            return PumiceIntent.PROBLEM_1;
        }
        if((lastIntent == PumiceIntent.PROBLEM_1 || lastIntent == PumiceIntent.SOLUTION_1_X || lastIntent == PumiceIntent.SOLUTION_1_Y || lastIntent == PumiceIntent.SOLUTION_2_N) && idk) {
            return PumiceIntent.SOLUTION_1_IDK;
        }
        if((lastIntent == PumiceIntent.PROBLEM_1 || lastIntent == PumiceIntent.SOLUTION_1_X || lastIntent == PumiceIntent.SOLUTION_1_Y || lastIntent == PumiceIntent.SOLUTION_2_N) && no) {
            return PumiceIntent.SOLUTION_1_N;
        }
        if((lastIntent == PumiceIntent.PROBLEM_1 || lastIntent == PumiceIntent.SOLUTION_1_X || lastIntent == PumiceIntent.SOLUTION_1_Y || lastIntent == PumiceIntent.SOLUTION_2_N) && yes) {
            return PumiceIntent.SOLUTION_1_Y;
        }
        if(lastIntent == PumiceIntent.PROBLEM_1 || lastIntent == PumiceIntent.SOLUTION_1_X || lastIntent == PumiceIntent.SOLUTION_1_Y || lastIntent == PumiceIntent.SOLUTION_2_N) {
            return PumiceIntent.SOLUTION_1_X;
        }
        if((lastIntent == PumiceIntent.GET_THEN_BLOCK || lastIntent == PumiceIntent.PROBLEM_1_X) && yes) {
            return PumiceIntent.PROBLEM_1;
        }
        if(lastIntent == PumiceIntent.GET_THEN_BLOCK) {
            return PumiceIntent.PROBLEM_1_X;
        }
        if((lastIntent == PumiceIntent.CHECKING_LOC_Y || lastIntent == PumiceIntent.CHECKING_LOC_IDK || lastIntent == PumiceIntent.GET_THEN_BLOCK_X) && (text.contains("true") || text.contains("false"))) {
            return PumiceIntent.GET_THEN_BLOCK;
        }
        if((lastIntent == PumiceIntent.CHECKING_LOC_Y || lastIntent == PumiceIntent.CHECKING_LOC_IDK || lastIntent == PumiceIntent.GET_THEN_BLOCK_X || lastIntent == PumiceIntent.GET_THEN_BLOCK_IDK) && idk) {
            return PumiceIntent.GET_THEN_BLOCK_IDK;
        }
        if(lastIntent == PumiceIntent.CHECKING_LOC_Y) {
            return PumiceIntent.GET_THEN_BLOCK_X;
        }
        if((lastIntent == PumiceIntent.CHECKING_LOC_N || lastIntent == PumiceIntent.MOVE_STEP_X) && text.matches(".*\\d+.*")) {
            return PumiceIntent.MOVE_STEP;
        }
        if(lastIntent == PumiceIntent.CHECKING_LOC_N) {
            return PumiceIntent.MOVE_STEP_X;
        }
        if((lastIntent == PumiceIntent.ADD_TO_SCRIPT_Y || lastIntent == PumiceIntent.CHECKING_LOC_X || lastIntent == PumiceIntent.MOVE_STEP || lastIntent == PumiceIntent.CHECKING_LOC_IDK) && idk) {
            return PumiceIntent.CHECKING_LOC_IDK;
        }
        if((lastIntent == PumiceIntent.ADD_TO_SCRIPT_Y || lastIntent == PumiceIntent.CHECKING_LOC_X || lastIntent == PumiceIntent.MOVE_STEP) && no) {
            return PumiceIntent.CHECKING_LOC_N;
        }
        if((lastIntent == PumiceIntent.ADD_TO_SCRIPT_Y || lastIntent == PumiceIntent.CHECKING_LOC_X || lastIntent == PumiceIntent.MOVE_STEP) && yes) {
            return PumiceIntent.CHECKING_LOC_Y;
        }
        if((lastIntent == PumiceIntent.ADD_TO_SCRIPT_Y || lastIntent == PumiceIntent.CHECKING_LOC_X)) {
            return PumiceIntent.CHECKING_LOC_X;
        }
        if((lastIntent == PumiceIntent.ADD_CONDITIONAL_Y || lastIntent == PumiceIntent.ADD_CONDITIONAL_X) && no) {
            return PumiceIntent.ADD_TO_SCRIPT_N;
        }
        if(lastIntent == PumiceIntent.ADD_CONDITIONAL_Y && yes) {
            return PumiceIntent.ADD_TO_SCRIPT_Y;
        }
        if(lastIntent == PumiceIntent.ADD_CONDITIONAL_Y) {
            return PumiceIntent.ADD_TO_SCRIPT_X;
        }
        if((lastIntent == PumiceIntent.ADD_CONDITIONAL_0 || lastIntent == PumiceIntent.ADD_CONDITIONAL_X) && no) {
            return PumiceIntent.ADD_CONDITIONAL_N;
        }
        if((lastIntent == PumiceIntent.ADD_CONDITIONAL_0 || lastIntent == PumiceIntent.ADD_CONDITIONAL_X) && yes) {
            return PumiceIntent.ADD_CONDITIONAL_Y;
        }
        if(lastIntent == PumiceIntent.ADD_CONDITIONAL_0 || lastIntent == PumiceIntent.ADD_CONDITIONAL_X) {
            return PumiceIntent.ADD_CONDITIONAL_X;
        }
        if(lastIntent == null || lastIntent == PumiceIntent.ADD_CONDITIONAL_N || lastIntent == PumiceIntent.ADD_TO_SCRIPT_N) {
            return PumiceIntent.ADD_CONDITIONAL_0;
        }
        else {
            return PumiceIntent.USER_INIT_INSTRUCTION;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        switch(pumiceIntent) {
            case DONE_2:
                ((ConditionalPumiceDialogManager) dialogManager).addCheck = false;
                ((ConditionalPumiceDialogManager) dialogManager).checkingTask = false;
                lastIntent = PumiceIntent.DONE_2;
                ((ConditionalPumiceDialogManager) dialogManager).endTestRun();
                ((ConditionalPumiceDialogManager) dialogManager).endInteraction();
                sourceActivity.addSnackbar("Ok great, you're all set.");
                dialogManager.sendAgentMessage("Ok great, you're all set.",true,false);
                break;
            case CHANGE_CHECK:
                lastIntent = PumiceIntent.CHANGE_CHECK;
                ((ConditionalPumiceDialogManager) dialogManager).changeCheck();
                break;
            case PROBLEM_3:
                lastIntent = PumiceIntent.PROBLEM_3;
                ((ConditionalPumiceDialogManager) dialogManager).endTestRun();
                boolExp = ((PumiceConditionalInstructionParsingHandler) dialogManager.getPumiceInitInstructionParsingHandler()).getBoolExp().getReadableDescription();
                sourceActivity.addSnackbar("I understand there is a problem with the check. Would you like to change what you are checking?");
                dialogManager.sendAgentMessage("I understand there is a problem with the check. Would you like to change what you are checking?",true,true);
                break;
            case PROBLEM_3_X:
                lastIntent = PumiceIntent.PROBLEM_3_X;
                ((ConditionalPumiceDialogManager) dialogManager).endTestRun();
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if there was a problem with the task or 'no' if there was no problem with the task.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if there was a problem with the task or 'no' if there was no problem with the task.",true,true);
                break;
            case CHECK_CONDITION:
                lastIntent = PumiceIntent.CHECK_CONDITION;
                ((ConditionalPumiceDialogManager) dialogManager).checkingTask = true;
                ((ConditionalPumiceDialogManager) dialogManager).lastCheck = true;
                dialogManager.waitingForPause = true;
                sourceActivity.addSnackbar("Ok, let’s run through the task to make sure the steps happen correctly for the current situation. Let me know if anything goes wrong by saying 'pause.'");
                dialogManager.sendAgentMessage("Ok, let’s run through the task to make sure the steps happen correctly for the current situation. Let me know if anything goes wrong by saying 'pause.'",true,true);
                ((ConditionalPumiceDialogManager) dialogManager).testRun(true);
                break;
            case CHECK_CONDITION_X:
                lastIntent = PumiceIntent.CHECK_CONDITION_X;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if you would like to run through the task for the current situation or 'no' if you would not like to run through the task for the current situation.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if you would like to run through the task for the current situation or 'no' if you would not like to run through the task for the current situation.",true,true);
                break;
            case WANT_CHECK_CONDITION:
                lastIntent = PumiceIntent.WANT_CHECK_CONDITION;
                ((ConditionalPumiceDialogManager) dialogManager).endTestRun();
                sourceActivity.addSnackbar("Ok great, would you like to run through the task to test the check for the current situation?");
                dialogManager.sendAgentMessage("Ok great, would you like to run through the task to test the check for the current situation?",true,true);
                break;
            case PROBLEM_2_X:
                lastIntent = PumiceIntent.PROBLEM_2_X;
                ((ConditionalPumiceDialogManager) dialogManager).endTestRun();
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if there was a problem with the task or 'no' if there was no problem with the task.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if there was a problem with the task or 'no' if there was no problem with the task.",true,true);
                break;
            case CHECKING_ELSE:
                lastIntent = PumiceIntent.CHECKING_ELSE;
                then = ((ConditionalPumiceDialogManager) dialogManager).getIsThen();
                boolExp = ((PumiceConditionalInstructionParsingHandler) dialogManager.getPumiceInitInstructionParsingHandler()).getBoolExp().getReadableDescription();
                ((ConditionalPumiceDialogManager) dialogManager).checkingTask = true;
                dialogManager.waitingForPause = true;
                sourceActivity.addSnackbar("Ok, let’s run through the task to make sure the steps happen correctly when " + boolExp + " is " + !then + ". Let me know if anything goes wrong by saying 'pause.'");
                dialogManager.sendAgentMessage("Ok, let’s run through the task to make sure the steps happen correctly when " + boolExp + " is " + !then + ". Let me know if anything goes wrong by saying 'pause.'",true,true);
                ((ConditionalPumiceDialogManager) dialogManager).testRun(false);
                break;
            case CHECKING_ELSE_X:
                lastIntent = PumiceIntent.CHECKING_ELSE_X;
                then = ((ConditionalPumiceDialogManager) dialogManager).getIsThen();
                boolExp = ((PumiceConditionalInstructionParsingHandler) dialogManager.getPumiceInitInstructionParsingHandler()).getBoolExp().getReadableDescription();
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if the steps that happen when " + boolExp + " is " + !then + " seem correct or 'no' if the steps that happen when " + boolExp + " is " + !then + " do not seem correct.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if it looks like the check happens at the right time or 'no' if it does not.", true, true);
                break;
            case DONE_1:
                ((ConditionalPumiceDialogManager) dialogManager).addCheck = false;
                ((ConditionalPumiceDialogManager) dialogManager).checkingTask = false;
                lastIntent = PumiceIntent.DONE_1;
                ((ConditionalPumiceDialogManager) dialogManager).endInteraction();
                sourceActivity.addSnackbar("Ok great, you're all set.");
                dialogManager.sendAgentMessage("Ok great, you're all set.",true,false);
                break;
            case GET_ELSE_BLOCK_Y:
                lastIntent = PumiceIntent.GET_ELSE_BLOCK_Y;
                ((ConditionalPumiceDialogManager) dialogManager).checkingTask = false;
                sourceActivity.addSnackbar("Ok, I need you to tell me what to do when " + boolExp + " is " + !then + ". You can explain, or say 'demonstrate' to demonstrate.");
                dialogManager.sendAgentMessage("Ok, I need you to tell me what to do when " + boolExp + " is " + !then + ". You can explain, or say 'demonstrate' to demonstrate.",true,true);
                dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceUserExplainElseStatementIntentHandler(dialogManager,sourceActivity,((ConditionalPumiceDialogManager) dialogManager).getNewBlock(),((PumiceConditionalInstructionParsingHandler) dialogManager.getPumiceInitInstructionParsingHandler()).getBoolExp().getReadableDescription()));
                break;
            case GET_ELSE_BLOCK_X:
                lastIntent = PumiceIntent.GET_ELSE_BLOCK_X;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if you would like to do something when " + boolExp + " is false or 'no' if you do not want to do something when " + boolExp + " is false.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if you would like to do something when " + boolExp + " is false or 'no' if you do not want to do something when " + boolExp + " is false.",true,true);
                break;
            case MOVE_STEP_2:
                lastIntent = PumiceIntent.MOVE_STEP_2;
                ((ConditionalPumiceDialogManager) dialogManager).moveStep(utterance.getContent());
                sourceActivity.addSnackbar("Ok, does it look like the check happens at the right time?");
                dialogManager.sendAgentMessage("Ok, does it look like the check happens at the right time?",true,true);
                break;
            case MOVE_STEP_2_X:
                lastIntent = PumiceIntent.MOVE_STEP_2_X;
                sourceActivity.addSnackbar("Please say the number of the step after which the check should happen.");
                dialogManager.sendAgentMessage("Please say the number of the step after which the check should happen.",true,true);
                break;
            case SOLUTION_2_N:
                lastIntent = PumiceIntent.SOLUTION_2_N;
                sourceActivity.addSnackbar("Ok, let’s run through the task again so you can see if the problem has to do with what happens when " + boolExp + " is " + then + " or with when the check happens.");
                dialogManager.sendAgentMessage("Ok, let’s run through the task again so you can see if the problem has to do with what happens when " + boolExp + " is " + then + " or with when the check happens.",true,true);
                ((ConditionalPumiceDialogManager) dialogManager).testRun(false);
                break;
            case SOLUTION_2_X:
                lastIntent = PumiceIntent.SOLUTION_2_X;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if the check happens at the wrong time or 'no' if the check happens at the right time.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if the check happens at the wrong time or 'no' if the check happens at the right time.",true,true);
                break;
            case SOLUTION_2_Y:
                lastIntent = PumiceIntent.SOLUTION_2_Y;
                sourceActivity.addSnackbar("Ok, after which step should I perform the check?");
                dialogManager.sendAgentMessage("Ok, after which step should I perform the check?",true,true);
                break;
            case SOLUTION_1_IDK:
                lastIntent = PumiceIntent.SOLUTION_1_IDK;
                sourceActivity.addSnackbar("Ok, let’s run through the task again so you can see if what happens when " + boolExp + " is " + then + " should actually happen when " + boolExp + " is " + !then + " instead.");
                dialogManager.sendAgentMessage("Ok, let’s run through the task again so you can see if what happens when " + boolExp + " is " + then + " should actually happen when " + boolExp + " is " + !then + " instead.",true,true);
                ((ConditionalPumiceDialogManager) dialogManager).testRun(false);
                break;
            case SOLUTION_1_X:
                lastIntent = PumiceIntent.SOLUTION_1_X;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if what happened when" + boolExp + " was" + then + " should have happened when" + boolExp + " was " + !then + " instead or 'no' if not.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if what happened when" + boolExp + " was" + then + " should have happened when" + boolExp + " was " + !then + " instead or 'no' if not.",true,true);
                break;
            case SOLUTION_1_N:
                lastIntent = PumiceIntent.SOLUTION_1_N;
                sourceActivity.addSnackbar("Ok, does the check happen at the wrong time?");
                dialogManager.sendAgentMessage("Ok, does the check happen at the wrong time?",true,true);
                break;
            case SOLUTION_1_Y:
                lastIntent = PumiceIntent.SOLUTION_1_Y;
                ((ConditionalPumiceDialogManager) dialogManager).switchThenElse();
                dialogManager.waitingForPause = true;
                sourceActivity.addSnackbar("Ok, I've switched what happens when " + boolExp + " is " + then + " and when " + boolExp + " is " + !then + ". Let's run through the task to make sure the check happens correctly. Let me know if anything goes wrong by saying 'pause.'");
                dialogManager.sendAgentMessage("Ok, I've switched what happens when " + boolExp + " is " + then + " and when " + boolExp + " is " + !then + ". Let's run through the task to make sure the check happens correctly. Let me know if anything goes wrong by saying 'pause.'",true,true);
                ((ConditionalPumiceDialogManager) dialogManager).testRun(false);
                break;
            case PROBLEM_1:
                lastIntent = PumiceIntent.PROBLEM_1;
                ((ConditionalPumiceDialogManager) dialogManager).endTestRun();
                boolExp = ((PumiceConditionalInstructionParsingHandler) dialogManager.getPumiceInitInstructionParsingHandler()).getBoolExp().getReadableDescription();
                sourceActivity.addSnackbar("I understand there is a problem. Should what happened when " + boolExp + " was " + then + " have happened when " + boolExp + " was " + !then + " instead?");
                dialogManager.sendAgentMessage("I understand there is a problem. Should what happened when " + boolExp + " was " + then + " have happened when " + boolExp + " was " + !then + " instead?",true,true);
                break;
            case PROBLEM_1_X:
                lastIntent = PumiceIntent.PROBLEM_1_X;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if there was a problem with the task or 'no' if there was no problem with the task.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if there was a problem with the task or 'no' if there was no problem with the task.",true,true);
                break;
            case WANT_ELSE_BLOCK:
                lastIntent = PumiceIntent.WANT_ELSE_BLOCK;
                boolExp = ((PumiceConditionalInstructionParsingHandler) dialogManager.getPumiceInitInstructionParsingHandler()).getBoolExp().getReadableDescription();
                ((ConditionalPumiceDialogManager) dialogManager).endTestRun();
                sourceActivity.addSnackbar("Ok, would you like to do something when " + boolExp + " is " + !then + "?");
                dialogManager.sendAgentMessage("Ok, would you like to do something when " + boolExp + " is " + !then + "?",true,true);
                break;
            case GET_THEN_BLOCK:
                lastIntent = PumiceIntent.GET_THEN_BLOCK;
                then = false;
                if(utterance.getContent().contains("true")) {
                    then = true;
                }
                ((ConditionalPumiceDialogManager) dialogManager).chooseThenBlock(then);
                ((ConditionalPumiceDialogManager) dialogManager).checkingTask = true;
                dialogManager.waitingForPause = true;
                sourceActivity.addSnackbar("Ok, let's run through the task to make sure the check happens correctly. Let me know if anything goes wrong by saying 'pause.'");
                dialogManager.sendAgentMessage("Ok, let’s run through the task to make sure the check happens correctly. Let me know if anything goes wrong by saying 'pause.'",true,true);
                ((ConditionalPumiceDialogManager) dialogManager).testRun(false);
                break;
            case GET_THEN_BLOCK_X:
                lastIntent = PumiceIntent.GET_THEN_BLOCK_X;
                int stepNum1 = ((ConditionalPumiceDialogManager) dialogManager).getNewBlockIndex()+1;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'true' if you want step " + stepNum1 + " to happen when " + boolExp + " is true or 'false' if you want step " + stepNum1 + " to happen when " + boolExp + " is false.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'true' if you want step " + stepNum1 + "to happen when " + boolExp + " is true or 'false' if you want step " + stepNum1 + " to happen when " + boolExp + " is false.",true,true);
                break;
            case GET_THEN_BLOCK_IDK:
                lastIntent = PumiceIntent.GET_THEN_BLOCK_IDK;
                ((ConditionalPumiceDialogManager) dialogManager).chooseThenBlock(true);
                int stepNum4 = ((ConditionalPumiceDialogManager) dialogManager).getNewBlockIndex()+1;
                sourceActivity.addSnackbar("That's ok, we'll figure it out. For now, I will assume that you want step " + stepNum4 + " to happen when " + boolExp + " is true.");
                dialogManager.sendAgentMessage("That's ok, we'll figure it out. For now, I will assume that you want step " + stepNum4 + " to happen when " + boolExp + " is true.",true,true);
                break;
            case MOVE_STEP:
                lastIntent = PumiceIntent.MOVE_STEP;
                ((ConditionalPumiceDialogManager) dialogManager).moveStep(utterance.getContent());
                sourceActivity.addSnackbar("Ok, does it look like the check happens at the right time?");
                dialogManager.sendAgentMessage("Ok, does it look like the check happens at the right time?",true,true);
                break;
            case MOVE_STEP_X:
                lastIntent = PumiceIntent.MOVE_STEP_X;
                sourceActivity.addSnackbar("Please say the number of the step after which the check should happen.");
                dialogManager.sendAgentMessage("Please say the number of the step after which the check should happen.",true,true);
                break;
            case CHECKING_LOC_IDK:
                lastIntent = PumiceIntent.CHECKING_LOC_IDK;
                int stepNum3 = ((ConditionalPumiceDialogManager) dialogManager).getNewBlockIndex()+1;
                boolExp = ((PumiceConditionalInstructionParsingHandler) dialogManager.getPumiceInitInstructionParsingHandler()).getBoolExp().getReadableDescription();
                sourceActivity.addSnackbar("That's ok, we'll figure it out. For now, I will assume that the check happens at the right time. Do you think you want step " + stepNum3 + "to happen when " + boolExp + " is true or when " + boolExp + " is false?");
                dialogManager.sendAgentMessage("That's ok, we'll figure it out. For now, I will assume that the check happens at the right time. Do you think you want step " + stepNum3 + "to happen when " + boolExp + " is true or when " + boolExp + " is false?",true,true);
                break;
            case CHECKING_LOC_N:
                lastIntent = PumiceIntent.CHECKING_LOC_N;
                sourceActivity.addSnackbar("Ok, after which step should I perform the check?");
                dialogManager.sendAgentMessage("Ok, after which step should I perform the check?",true,true);
                break;
            case CHECKING_LOC_Y:
                lastIntent = PumiceIntent.CHECKING_LOC_Y;
                int stepNum2 = ((ConditionalPumiceDialogManager) dialogManager).getNewBlockIndex()+1;
                boolExp = ((PumiceConditionalInstructionParsingHandler) dialogManager.getPumiceInitInstructionParsingHandler()).getBoolExp().getReadableDescription();
                sourceActivity.addSnackbar("Ok, do you want step " + stepNum2 + " to happen when it's true that " + boolExp + " or when it's false that " + boolExp + "?");
                dialogManager.sendAgentMessage("Ok, do you want step " + stepNum2 + " to happen when it's true that " + boolExp + " or when it's false that " + boolExp + "?",true,true);
                break;
            case CHECKING_LOC_X:
                lastIntent = PumiceIntent.CHECKING_LOC_X;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if it looks like the check happens at the right time or 'no' if it does not.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if it looks like the check happens at the right time or 'no' if it does not.", true, true);
                break;
            case ADD_TO_SCRIPT_N:
                lastIntent = PumiceIntent.ADD_TO_SCRIPT_N;
                dialogManager.sendAgentMessage("Ok, let's try this again. Please try explaining your check in a different way.", true, true);
                sourceActivity.addSnackbar("You are adding a check to do different steps in different cases. Please say something like 'check if it's cold' or 'check if the current time is before 5pm.'");
                dialogManager.sendAgentMessage("You are adding a check to do different steps in different cases. Please say something like 'check if it's cold' or 'check if the current time is before 5pm.'", true, true);
                break;
            case ADD_TO_SCRIPT_Y:
                lastIntent = PumiceIntent.ADD_TO_SCRIPT_Y;
                ((ConditionalPumiceDialogManager) dialogManager).determineConditionalLoc();
                sourceActivity.addSnackbar("Ok, does it look like the check happens at the right time?");
                dialogManager.sendAgentMessage("Ok, does it look like the check happens at the right time?",true,true);
                break;
            case ADD_TO_SCRIPT_X:
                lastIntent = PumiceIntent.ADD_TO_SCRIPT_X;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if you want me to add the check to the script or 'no' if you do not.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if you want me to add the check to the script or 'no' if you do not.",true,true);
                break;
            case ADD_CONDITIONAL_N:
                lastIntent = PumiceIntent.ADD_CONDITIONAL_N;
                sourceActivity.addCheck();
                break;
            case ADD_CONDITIONAL_Y:
                lastIntent = PumiceIntent.ADD_CONDITIONAL_Y;
                PumiceInstructionPacket pumiceInstructionPacket2 = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.BOOL_EXP_INSTRUCTION, calendar.getTimeInMillis(), check, "");
                dialogManager.sendAgentMessage("Let's make sure I understand what you want to do...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket2.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket2, this);
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(pumiceInstructionPacket2.toString());
                break;
            case ADD_CONDITIONAL_X:
                lastIntent = PumiceIntent.ADD_CONDITIONAL_X;
                sourceActivity.addSnackbar("I’m sorry, I didn’t understand what you said. Please say 'yes' if you want to check if " + check + "or 'no' if you do not.");
                dialogManager.sendAgentMessage("I’m sorry, I didn’t understand what you said. Please say 'yes' if you want to check if " + check + "or 'no' if you do not.", true, true);
                break;
            case ADD_CONDITIONAL_0:
                lastIntent = PumiceIntent.ADD_CONDITIONAL_0;
                String[] removeIf2 = utterance.getContent().split(" ",3);
                check = removeIf2[2];
                sourceActivity.addSnackbar("You want to check if " + check + ". Is that correct?");
                dialogManager.sendAgentMessage("You want to check if " + check + ". Is that correct?", true, true);
                break;
            case USER_INIT_INSTRUCTION:
                lastIntent = PumiceIntent.USER_INIT_INSTRUCTION;
                sourceActivity.addSnackbar("I have received your instruction: " + utterance.getContent());
                dialogManager.sendAgentMessage("I have received your instruction: " + utterance.getContent(), true, false);
                PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), utterance.getContent(), "");
                sourceActivity.addSnackbar("Sending out the server query...");
                dialogManager.sendAgentMessage("Sending out the server query...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(pumiceInstructionPacket.toString());
                break;
            default:
                dialogManager.sendAgentMessage("I don't understand this intent", true, false);
                break;
        }
    }


    /*@Override
    public void handleServerResponse(PumiceDialogManager dialogManager, int responseCode, String result) {
        //TODO: handle server response
        //notify the thread for resolving unknown bool exp that the intent has been fulfilled


    }*/

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
        //TODO: handle server response from the semantic parsing server
        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new ExclusionStrategy()
                {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f)
                    {
                        return f.getAnnotation(SkipPumiceJSONSerialization.class) != null;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz)
                    {
                        return false;
                    }
                })
                .create();
        try {
            PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
            resultPacket.cleanFormula();
            if (resultPacket.utteranceType != null) {
                switch (PumiceUtteranceIntentHandler.PumiceIntent.valueOf(resultPacket.utteranceType)) {
                    case USER_INIT_INSTRUCTION:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            if (topResult.formula != null) {
                                //dialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                dialogManager.sendAgentMessage(topResult.formula, false, false);
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
					//pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula);
                                        dialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula, resultPacket.userUtterance);
                                    }
                                };
                                //do the parse on a new thread so it doesn't block the conversational I/O
                                es.submit(r);
                            }
                        }
                        break;
                    case BOOL_EXP_INSTRUCTION:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            System.out.println("top: " + topResult.formula);
                            System.out.println("utter: " + resultPacket.userUtterance);
                            if (topResult.formula != null) {
                                //dialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                dialogManager.sendAgentMessage(topResult.formula, false, false);
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        dialogManager.getPumiceInitInstructionParsingHandler().parseFromBoolExpInstruction(topResult.formula, resultPacket.userUtterance,null);
                                    }
                                };
                                //do the parse on a new thread so it doesn't block the conversational I/O
                                es.submit(r);
                            }
                        }
                        break;
                    case DEFINE_VALUE_EXP:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            if (topResult.formula != null) {
                                //dialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                dialogManager.sendAgentMessage(topResult.formula, false, false);
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        dialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula, resultPacket.userUtterance);
                                    }
                                };
                                //do the parse on a new thread so it doesn't block the conversational I/O
                                es.submit(r);
                            }
                        }
                        break;
                    case DEFINE_PROCEDURE_DEMONSTATION:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            System.out.println("top: " + topResult.formula);
                            System.out.println("utter: " + resultPacket.userUtterance);
                            if (topResult.formula != null) {
                                //dialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                dialogManager.sendAgentMessage(topResult.formula, false, false);
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        dialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula, resultPacket.userUtterance);
                                    }
                                };
                                //do the parse on a new thread so it doesn't block the conversational I/O
                                es.submit(r);
                            }
                        }
                        break;
                    case DEFINE_PROCEDURE_EXP:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            System.out.println("top: " + topResult.formula);
                            System.out.println("utter: " + resultPacket.userUtterance);
                            if (topResult.formula != null) {
                                //dialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                dialogManager.sendAgentMessage(topResult.formula, false, false);
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        dialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula, resultPacket.userUtterance);
                                    }
                                };
                                //do the parse on a new thread so it doesn't block the conversational I/O
                                es.submit(r);
                            }
                        }
                        break;
                    default:
                        dialogManager.sendAgentMessage("Can't read from the server response", true, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLastIntent(PumiceIntent pi) {
        lastIntent = pi;
    }

    public PumiceIntent getLastIntent() {
        return lastIntent;
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        //TODO: implement
    }

    @Override
    public void runOnMainThread(Runnable r) {
	//pumiceDialogManager.runOnMainThread(r);
        sourceActivity.runOnUiThread(r);
    }
}

