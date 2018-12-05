package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;

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
        if((text.contains("before") || text.contains("after")) && lastIntent == PumiceIntent.ADD_CONDITIONAL) {
            return PumiceIntent.ADD_CONDITIONAL_2;
        }
        if(text.contains("if") || text.contains("when") || text.contains("unless")) { //will need more
            return PumiceIntent.ADD_CONDITIONAL;
        }
        if((text.contains("yes") || text.contains("no")) && lastIntent == PumiceIntent.CHECKING_LOC && lastUtterance.getContent().contains("yes")) {
            return PumiceIntent.RUN_THROUGH;
        }
        if(((text.contains("yes") || text.contains("no")) && (lastIntent == PumiceIntent.ADD_TO_SCRIPT) || (lastIntent == PumiceIntent.MOVE_STEP) && text.contains("yes"))) {
            return PumiceIntent.CHECKING_LOC;
        }
        if((lastIntent == PumiceIntent.CHECKING_LOC && lastUtterance.getContent().contains("no")) || (lastIntent == PumiceIntent.MOVE_STEP && !lastUtterance.getContent().matches(".*\\d+.*")) || (lastIntent == PumiceIntent.MOVE_STEP && text.contains("no"))) {
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
            case ADD_TO_SCRIPT:
                lastIntent = PumiceIntent.ADD_TO_SCRIPT;
                lastUtterance = utterance;
                if(utterance.getContent().contains("yes")) {
                    ((ScriptDetailActivity) dialogManager.context).determineConditionalLoc(dialogManager.tResult);
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
                    dialogManager.sendAgentMessage("Great, would you like me to run through the task to make sure the new step works correctly?",true,true);
                }
                else {
                    dialogManager.sendAgentMessage("Ok, after which step should the new step happen?",true,true);
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
                    dialogManager.sendAgentMessage("Please say the number of the step after which the new step should happen.",true,true);
                }
                else {
                    ((ScriptDetailActivity) dialogManager.context).moveStep(utterance.getContent());
                    dialogManager.sendAgentMessage("Ok, does it look like the new step happens at the right time?",true,true);
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
