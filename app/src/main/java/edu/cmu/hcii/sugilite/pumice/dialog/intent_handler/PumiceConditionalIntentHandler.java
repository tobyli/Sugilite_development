package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;

public class PumiceConditionalIntentHandler implements PumiceUtteranceIntentHandler {
    private transient Context context;
    Calendar calendar;
    private PumiceIntent lastIntent = null;
    private PumiceDialogManager.PumiceUtterance lastUtterance;
    public PumiceConditionalIntentHandler(Context context){
        this.context = context;
        this.calendar = Calendar.getInstance();
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String text = utterance.getContent().toLowerCase();
        if(lastIntent == PumiceIntent.ADD_CONDITIONAL_2 && text.contains("yes")) {
            return PumiceIntent.CHECKING_LOC;
        }
        if(lastIntent == PumiceIntent.ADD_TELL_ELSE && text.contains("yes")) {
            return PumiceIntent.SCRIPT_ADD_TELL_ELSE;
        }
        if(lastIntent == PumiceIntent.TELL_ELSE) {
            return PumiceIntent.ADD_TELL_ELSE;
        }
        if(text.contains("tell") && lastIntent == PumiceIntent.ADD_ELSE) {
            return PumiceIntent.TELL_ELSE;
        }
        if(text.contains("yes") && lastIntent == PumiceIntent.GET_SCOPE) {
            return PumiceIntent.ADD_ELSE;
        }
        if((text.contains("true") || text.contains("false")) && lastIntent == PumiceIntent.CHECKING_LOC) {
            return PumiceIntent.GET_SCOPE;
        }
        if((text.contains("before") || text.contains("after")) && lastIntent == PumiceIntent.ADD_TO_SCRIPT) {
            return PumiceIntent.ADD_CONDITIONAL_2;
        }
        if(text.contains("if") || text.contains("when") || text.contains("unless")) { //will need more
            return PumiceIntent.ADD_CONDITIONAL;
        }
        if(((text.contains("yes") || text.contains("no")) && lastIntent == PumiceIntent.SCRIPT_ADD_TELL_ELSE) || (text.contains("yes") || text.contains("no")) && lastIntent == PumiceIntent.CHECKING_LOC && lastUtterance.getContent().contains("yes")) {
            return PumiceIntent.RUN_THROUGH;
        }
        if(((text.contains("yes") || text.contains("no")) && (lastIntent == PumiceIntent.ADD_TO_SCRIPT) || (lastIntent == PumiceIntent.MOVE_STEP) && text.contains("yes"))) {
            return PumiceIntent.CHECKING_LOC;
        }
        if(((text.contains("no") && lastIntent == PumiceIntent.ADD_CONDITIONAL_2) || (lastIntent == PumiceIntent.CHECKING_LOC) && lastUtterance.getContent().contains("no")) || (lastIntent == PumiceIntent.MOVE_STEP && !lastUtterance.getContent().matches(".*\\d+.*")) || (lastIntent == PumiceIntent.MOVE_STEP && text.contains("no"))) {
            return PumiceIntent.MOVE_STEP;
        }
        if((text.contains("yes")) || text.contains("no")) {
            return PumiceIntent.ADD_TO_SCRIPT;
        }
        else {
            return PumiceIntent.USER_INIT_INSTRUCTION;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {

        switch(pumiceIntent) {
            case SCRIPT_ADD_TELL_ELSE:
                lastIntent = PumiceIntent.SCRIPT_ADD_TELL_ELSE;
                lastUtterance = utterance;
                SugiliteBlock block = ((SugiliteConditionBlock) dialogManager.tResult).getIfBlock();
                ((SugiliteConditionBlock) dialogManager.conditionBlock).setElseBlock(block);
                block.setPreviousBlock(null);
                block.setParentBlock(dialogManager.conditionBlock);
                block.setNextBlock(null);
                ((ScriptDetailActivity) dialogManager.context).loadOperationList();
                dialogManager.sendAgentMessage("Would you like to run through the task to make sure the check and steps happen correctly?", true, true);
                dialogManager.addElse = false;
                break;
            case ADD_TELL_ELSE:
                lastIntent = PumiceIntent.ADD_TELL_ELSE;
                lastUtterance = utterance;
                dialogManager.addElse = true;
                dialogManager.conditionBlock = dialogManager.tResult;
                PumiceInstructionPacket pumiceInstructionPacket4 = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), "if it is cold" + utterance.getContent());
                dialogManager.sendAgentMessage("Let's make sure I understood what you said...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket4.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket4);
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(pumiceInstructionPacket4.toString());
                break;
            case TELL_ELSE:
                lastIntent = PumiceIntent.TELL_ELSE;
                lastUtterance = utterance;
                dialogManager.sendAgentMessage("Ok, what should I do if" + dialogManager.check + "is false?",true,true);
                break;
            case ADD_ELSE:
                lastIntent = PumiceIntent.ADD_ELSE;
                lastUtterance = utterance;
                dialogManager.sendAgentMessage("Ok, I need you to explain to me what to do if" + dialogManager.check + "is false. Would you like to show me or tell me what to do?",true,true);
                break;
            case GET_SCOPE:
                lastIntent = PumiceIntent.GET_SCOPE;
                lastUtterance = utterance;
                if(utterance.getContent().contains("not true") || utterance.getContent().contains("false")) {
                    //((SugiliteConditionBlock) dialogManager.tResult).setElseBlock(((SugiliteConditionBlock) dialogManager.tResult).getIfBlock());
                    //((SugiliteConditionBlock) dialogManager.tResult).setIfBlock(null);
                    //dialogManager.sendAgentMessage("Ok, would you like to show or tell me what to do if it is " +  + "?",true,true);
                }
                else {
                    ((SugiliteConditionBlock) dialogManager.tResult).setIfBlock(dialogManager.tResult.getNextBlock());
                    dialogManager.tResult.getNextBlock().setParentBlock(dialogManager.tResult);
                    dialogManager.tResult.setNextBlock(null);
                    ((ScriptDetailActivity) dialogManager.context).loadOperationList();
                    dialogManager.sendAgentMessage("Ok, would you like to do something if " + dialogManager.check + "is false?",true,true);
                }
                break;
            case ADD_TO_SCRIPT:
                lastIntent = PumiceIntent.ADD_TO_SCRIPT;
                lastUtterance = utterance;
                if(utterance.getContent().contains("yes")) {
                    ((ScriptDetailActivity) dialogManager.context).determineConditionalLoc(dialogManager.tResult);
                }
                else {
                    //
                }
                break;
            case ADD_CONDITIONAL_2:
                lastIntent = PumiceIntent.ADD_CONDITIONAL_2;
                lastUtterance = utterance;
                boolean after = false;
                if(utterance.getContent().contains("after")) {
                    after = true;
                }
                ((ScriptDetailActivity) dialogManager.context).determineConditionalLoc2(after);
                break;
            case ADD_CONDITIONAL:
                lastIntent = PumiceIntent.ADD_CONDITIONAL;
                lastUtterance = utterance;
                dialogManager.justChecking = true;
                PumiceInstructionPacket pumiceInstructionPacket2 = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), utterance.getContent());
                dialogManager.sendAgentMessage("Let's make sure I understood what you said...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket2.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket2);
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(pumiceInstructionPacket2.toString());
                break;
            case CHECKING_LOC:
                lastIntent = PumiceIntent.CHECKING_LOC;
                lastUtterance = utterance;
                if(utterance.getContent().contains("yes")) {
                    if((((ScriptDetailActivity) context).newBlockIndex+1) != ((ScriptDetailActivity) context).lastBlockIndex) {
                        dialogManager.sendAgentMessage("Ok, do you want me to perform steps " + (((ScriptDetailActivity) context).newBlockIndex + 1) + " through " + ((ScriptDetailActivity) context).lastBlockIndex + " if " + dialogManager.check + "is true or if " + dialogManager.check + "is false?", true, true);
                    }
                    else {
                        dialogManager.sendAgentMessage("Ok, do you want me to perform step " + (((ScriptDetailActivity) context).newBlockIndex + 1) + " if " + dialogManager.check + "is true or false?", true, true);
                    }
                }
                else {
                    dialogManager.sendAgentMessage("Ok, after which step should I perform the check?",true,true);
                }
                break;
            case RUN_THROUGH:
                lastIntent = PumiceIntent.RUN_THROUGH;
                lastUtterance = utterance;
                if(utterance.getContent().contains("yes")) {
                    ((ScriptDetailActivity) dialogManager.context).testRun();
                    //dialogManager.sendAgentMessage("The test run is finished. Did everything happen correctly?",true,true);
                }
                else {
                    dialogManager.sendAgentMessage("Ok, you're all set.",true,false);
                }
                break;
            case MOVE_STEP:
                lastIntent = PumiceIntent.MOVE_STEP;
                lastUtterance = utterance;
                if(!utterance.getContent().matches(".*\\d+.*")) {//need to account for if say number that isn't step
                    dialogManager.sendAgentMessage("Please say the number of the step after which the check should happen.",true,true);
                }
                else {
                    ((ScriptDetailActivity) dialogManager.context).moveStep(utterance.getContent());
                    dialogManager.sendAgentMessage("Ok, does it look like the check happens at the right time?",true,true);
                }
                break;
            case USER_INIT_INSTRUCTION:
                lastIntent = PumiceIntent.USER_INIT_INSTRUCTION;
                lastUtterance = utterance;
                dialogManager.sendAgentMessage("I have received your instruction: " + utterance.getContent(), true, false);
                PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), utterance.getContent());
                dialogManager.sendAgentMessage("Sending out the server query below...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket);
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

}
