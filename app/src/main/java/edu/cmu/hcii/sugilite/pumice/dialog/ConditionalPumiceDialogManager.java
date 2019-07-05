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
    public SugiliteConditionBlock newBlock;
    private int newBlockIndex;
    private ScriptDetailActivity context;
    private boolean isThen;
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

    public void moveStep(String s) {
        int i = Integer.parseInt(s);//index of step that new step should go after; indices start at 1 and new step starts at index 1
        int j = newBlockIndex;
        SugiliteBlock storedCondBlock = null;
        boolean onIf = false;
        if(i == j-1) {
            return;
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
            SugiliteBlock iterNextBlock = iterBlock.getNextBlock();
            SugiliteBlock newNextBlock = newBlock.getNextBlock();
            SugiliteBlock newPrevBlock;
            if (j == 1) {
                newPrevBlock = context.getScript();
            } else {
                newPrevBlock = newBlock.getPreviousBlock();
            }

            int check = j + 1;
            if (i == check) {
                iterBlock.setPreviousBlock(newPrevBlock);
                newPrevBlock.setNextBlock(iterBlock);
            } else {
                if (newNextBlock != null) {
                    newNextBlock.setPreviousBlock(newPrevBlock);
                }
                newPrevBlock.setNextBlock(newNextBlock);
            }

            newBlock.setPreviousBlock(iterBlock);
            iterBlock.setNextBlock(newBlock);


            newBlock.setNextBlock(iterNextBlock);
            if (iterNextBlock != null) {
                iterNextBlock.setPreviousBlock(newBlock);
            }

            if (j < i) {
                newBlockIndex = i;
            } else {
                newBlockIndex = i + 1;
            }

            context.loadOperationList();
        }
    }

    public void chooseThenBlock(boolean then) {
        isThen = then;
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

    public void switchThenElse() {
        SugiliteBlock storedBlock = newBlock.getElseBlock();
        newBlock.setElseBlock(newBlock.getThenBlock());
        newBlock.setThenBlock(storedBlock);
        context.loadOperationList();
    }

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

    public void endTestRun() {
        SugiliteStartingBlock script = context.getScript();
        sugiliteData.clearInstructionQueue();
        Intent openMainActivity= new Intent(context,ScriptDetailActivity.class);
        openMainActivity.putExtra("scriptName", script.getScriptName());
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(openMainActivity);
    }

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

    public void endInteraction() {
        newBlock.inScope = false;
        context.loadOperationList();
    }

    public int getNewBlockIndex() {
        return newBlockIndex;
    }

    public SugiliteConditionBlock getNewBlock() { return newBlock; }

    public boolean getIsThen() {
        return isThen;
    }

}
