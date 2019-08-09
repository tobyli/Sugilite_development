package edu.cmu.hcii.sugilite.pumice.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceConditionalIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;
import android.content.Intent;

public class ConditionalPumiceDialogManager extends PumiceDialogManager {
    public SugiliteConditionBlock newBlock; //condition block
    private int newBlockIndex; //index of condition block
    private ScriptDetailActivity context;
    private boolean isThen; //whether or not added then block or else block first
    public boolean addCheck = false; //whether or not a check is being added to the script (i.e., whether or not this dialog manager is getting used)
    public boolean checkingTask = false; //whether or not testing versus demonstrating
    public boolean elseStatementDem = false; //whether or not performing else statement demonstration
    public boolean lastCheck = false; //whether or not running last test of conditional

    public ConditionalPumiceDialogManager(ScriptDetailActivity context) {
        super(context);
        this.context = context;
        setPumiceDialogState(new PumiceDialogState(new PumiceConditionalIntentHandler(this, context,null), new PumiceKnowledgeManager()));
        setPumiceInitInstructionParsingHandler(new PumiceConditionalInstructionParsingHandler(context, this, sugiliteData));
    }

    /*add the condition to the top of the script*/
    public void determineConditionalLoc() {
        SugiliteBooleanExpressionNew sben = ((PumiceConditionalInstructionParsingHandler) getPumiceInitInstructionParsingHandler()).getBoolExp();
        SugiliteConditionBlock scb = new SugiliteConditionBlock(null, null, null);
        scb.setSugiliteBooleanExpressionNew(sben);
        newBlock = scb;
        newBlockIndex = 1;
        newBlock.inScope = true;
        SugiliteStartingBlock script = context.getScript();
        SugiliteBlock holdBlock = script.getNextBlock();
        script.setNextBlock(newBlock);
        newBlock.setPreviousBlock(script);
        newBlock.setNextBlock(holdBlock);
        holdBlock.setPreviousBlock(newBlock);
        context.loadOperationList();
    }

    /*move condition to step location that user wants
    *
   *@param s: String given by user indicating index of step that new step should go after
    * */
    public void moveStep(String s) {
        int i = Integer.parseInt(s);//index of step that condition should go after; indices start at 1 and condition originally starts at index 1
        int j = newBlockIndex;
        SugiliteBlock storedCondBlock = null;
        boolean onIf = false; //true if iterBlock is in the then or else block of the condition block
        if(i == j-1) {
            return; //do nothing because user is saying condition should go where it already is
        }
        if(i != j) {
            SugiliteBlock iterBlock = context.getScript();
            int count = 0;
            while (count < i) {
                if (iterBlock instanceof SugiliteStartingBlock)
                    iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof SugiliteOperationBlock)
                    iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                    iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof SugiliteConditionBlock) {
                    storedCondBlock = iterBlock;
                    SugiliteConditionBlock condBlock = ((SugiliteConditionBlock) iterBlock);
                    iterBlock = condBlock.getThenBlock();
                    if(iterBlock != null) {
                        onIf = true;
                    }
                    else if(condBlock.getElseBlock() != null) {
                        iterBlock = condBlock.getElseBlock();
                    }
                    else {
                        iterBlock = condBlock.getNextBlock();
                    }
                }
                else if (iterBlock == null && onIf) {
                    iterBlock = ((SugiliteConditionBlock) storedCondBlock).getElseBlock();
                    if(iterBlock == null) {
                        iterBlock = storedCondBlock.getNextBlock();
                    }
                }
                else
                    new Exception("unsupported block type").printStackTrace();
                count++;
            }

            if(iterBlock == null) {
                context.addSnackbar("The step number you gave does not exist.");
                sendAgentMessage("The step number you gave does not exist.",true,false);
            }
            else { //store blocks necessary for reconfiguring blocks according to condition block's new position
                SugiliteBlock iterNextBlock = iterBlock.getNextBlock();
                SugiliteBlock newNextBlock;
                boolean then = false; //true if condition block has a then block
                boolean thenOrElse = true; //true if condition block has a then or else block
                if (newBlock.getThenBlock() != null) {
                    then = true;
                    newNextBlock = newBlock.getThenBlock();
                }
                else if (newBlock.getElseBlock() != null) {
                    newNextBlock = newBlock.getElseBlock();
                }
                else {
                    thenOrElse = false;
                    newNextBlock = newBlock.getNextBlock();
                }

                SugiliteBlock newPrevBlock;
                if(j == 1) {
                    newPrevBlock = context.getScript();
                }
                else {
                    newPrevBlock = newBlock.getPreviousBlock();
                }

                //reconfigure blocks according to condition block's new position
                if(thenOrElse) {
                    newNextBlock.setPreviousBlock(newPrevBlock);
                    newPrevBlock.setNextBlock(newNextBlock);
                    if(then) {
                        newBlock.setThenBlock(iterNextBlock);
                    }
                    else {
                        newBlock.setElseBlock(iterNextBlock);
                    }
                }
                else {
                    newNextBlock.setPreviousBlock(newPrevBlock);
                    newPrevBlock.setNextBlock(newNextBlock);
                    newBlock.setNextBlock(iterNextBlock);
                    iterNextBlock.setPreviousBlock(newBlock);
                }
                newBlock.setPreviousBlock(iterBlock);
                iterBlock.setNextBlock(newBlock);

                //update index of condition block
                if(j < i) {
                    newBlockIndex = i;
                }
                else {
                    newBlockIndex = i + 1;
                }
            }
            context.loadOperationList();
        }
    }

    /*switch boolean value of SugiliteData.java's variable check1 indicating whether currently checking then block or else block*/
    public void changeCheck1() {
        boolean c = sugiliteData.getCheck1();
        sugiliteData.setCheck1(!c);
    }

    /*make blocks underneath newly added condition the then block or else block
    *
    * @param then: whether setting blocks below the newly added condition to be the then block or else block; true if setting to then block
    * */
    public void chooseThenBlock(boolean then) {
        isThen = then;
        sugiliteData.setCheck1(isThen);
        SugiliteBlock storedBlock = newBlock.getNextBlock();
        if(then) {
            storedBlock.setPreviousBlock(null);
            newBlock.setThenBlock(storedBlock);
            newBlock.setElseBlock(null);
            newBlock.setNextBlock(null);
        }
        else {
            newBlock.setThenBlock(null);
            newBlock.setNextBlock(null);
            storedBlock.setPreviousBlock(null);
            newBlock.setElseBlock(storedBlock);
        }
        context.loadOperationList();
    }

    /*switch the then and else blocks*/
    public void switchThenElse() {
        SugiliteBlock storedElse = newBlock.getElseBlock();
        SugiliteBlock elseNext = null;
        if(storedElse != null) {
            elseNext = storedElse.getNextBlock();
        }
        newBlock.setElseBlock(newBlock.getThenBlock());
        if(newBlock.getThenBlock() != null && newBlock.getElseBlock() != null) {
            newBlock.getElseBlock().setNextBlock(newBlock.getThenBlock().getNextBlock());
        }
        newBlock.setThenBlock(storedElse);
        if(newBlock.getThenBlock() != null) {
            newBlock.getThenBlock().setNextBlock(elseNext);
        }
        context.loadOperationList();
    }

    /*test run the then block, else block, or full condition block
    *
    * @param last: true if testing full condition block
    * */
    public void testRun(boolean last) {
        if(last) {
            sugiliteData.last = true;
        }
        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessiblity service is not active
            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
            builder1.setTitle("Service not running")
                    .setMessage("The Sugilite accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            serviceStatusManager.promptEnabling();
                            //do nothing
                        }
                    }).show();
        }
        else {
            VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(context, context.getLayoutInflater(), sugiliteData, context.getScript(), sharedPreferences, SugiliteData.REGULAR_DEBUG_STATE, this);

            //execute the script without showing the dialog
            variableSetValueDialog.executeScript(null, this, null);
        }
    }

    /*end test run and return to script*/
    public void endTestRun() {
        SugiliteStartingBlock script = context.getScript();
        sugiliteData.clearInstructionQueue();
        Intent openMainActivity= new Intent(context,ScriptDetailActivity.class);
        openMainActivity.putExtra("scriptName", script.getScriptName());
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(openMainActivity);
    }

    /*change the condition*/
    public void changeCheck() {
        SugiliteBlock iterBlock = context.getScript();
        while (iterBlock != null) {
            if (iterBlock instanceof SugiliteStartingBlock)
                iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlock();
            else if (iterBlock instanceof SugiliteOperationBlock)
                iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlock();
            else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlock();
            else if (iterBlock instanceof SugiliteConditionBlock) {
                break;
            }
            else
                new Exception("unsupported block type").printStackTrace();
        }

        SugiliteBooleanExpressionNew sben = ((PumiceConditionalInstructionParsingHandler) getPumiceInitInstructionParsingHandler()).getBoolExp();
        SugiliteConditionBlock scb = new SugiliteConditionBlock(null, null, null);
        scb.setSugiliteBooleanExpressionNew(sben);
        SugiliteBlock holdBlock = iterBlock.getNextBlock();
        SugiliteBlock holdBlock2 = iterBlock.getPreviousBlock();
        holdBlock2.setNextBlock(scb);
        scb.setPreviousBlock(holdBlock2);
        scb.setNextBlock(holdBlock);
        holdBlock.setPreviousBlock(scb);
        context.loadOperationList();
    }

    /*end the interaction for adding a condition to the script*/
    public void endInteraction() {
        newBlock.inScope = false;
        context.loadOperationList();
    }

    public int getNewBlockIndex() {
        return newBlockIndex;
    }

    public SugiliteConditionBlock getNewBlock() {
        return newBlock;
    }

    public boolean getIsThen() {
        return isThen;
    }

}
