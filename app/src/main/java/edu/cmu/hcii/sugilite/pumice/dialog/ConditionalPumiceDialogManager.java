package edu.cmu.hcii.sugilite.pumice.dialog;

import android.app.Activity;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceConditionalIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSpecialOperationBlock;

public class ConditionalPumiceDialogManager extends PumiceDialogManager {
    private SugiliteConditionBlock newBlock;
    private int newBlockIndex;
    private ScriptDetailActivity context;



    public ConditionalPumiceDialogManager(ScriptDetailActivity context) {
        super(context);
        this.context = context;
        setPumiceDialogState(new PumiceDialogState(new PumiceConditionalIntentHandler(this, context,null), new PumiceKnowledgeManager()));
        setPumiceInitInstructionParsingHandler(new PumiceConditionalInstructionParsingHandler(context, this));
    }

    public void determineConditionalLoc(SugiliteConditionBlock sb) {
        newBlock = sb;
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
        if(then) {
            newBlock.setThenBlock(newBlock.getNextBlock());
            newBlock.setElseBlock(null);
        }
        else {
            newBlock.setThenBlock(null);
            newBlock.setElseBlock(newBlock.getNextBlock());
        }
        newBlock.setNextBlock(null);
        context.loadOperationList();
    }

    public int getNewBlockIndex() {
        return newBlockIndex;
    }

}
