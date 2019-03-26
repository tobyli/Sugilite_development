package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.content.Context;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;

/**
 * @author toby
 * @date 10/26/18
 * @time 1:33 PM
 */
public interface PumiceUtteranceIntentHandler {
    enum PumiceIntent {USER_INIT_INSTRUCTION, TEST_WEATHER, START_OVER, UNDO_STEP, SHOW_KNOWLEDGE, SHOW_RAW_KNOWLEDGE, DEFINE_BOOL_EXP, DEFINE_VALUE_EXP, BOOL_EXP_INSTRUCTION, DEFINE_VALUE_DEMONSTRATION, DEFINE_PROCEDURE_EXP, DEFINE_PROCEDURE_DEMONSTATION, EXECUTION_POSITIVE, EXECUTION_NEGATIVE, ADD_CONDITIONAL0, ADD_CONDITIONAL_Y, ADD_CONDITIONAL_N, ADD_CONDITIONAL_X, ADD_TO_SCRIPT_X, ADD_TO_SCRIPT_N, ADD_TO_SCRIPT_Y, CHECKING_LOC_Y, CHECKING_LOC_N, CHECKING_LOC_X, MOVE_STEP, MOVE_STEP_X, GET_THEN_BLOCK, GET_THEN_BLOCK_X}//RUN_THROUGH, GET_SCOPE, ADD_ELSE, TELL_ELSE, ADD_TELL_ELSE, SCRIPT_ADD_TELL_ELSE, FIX_SCRIPT, FIX_SCRIPT2, FIX_SCOPE, RUN_THROUGH2, TELL_IF, ADD_TELL_IF, SCRIPT_ADD_TELL_IF}

    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance);
    void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance);
    void setContext(Activity context);
}
