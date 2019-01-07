package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;

/**
 * @author toby
 * @date 10/26/18
 * @time 1:33 PM
 */
public interface PumiceUtteranceIntentHandler {
    enum PumiceIntent {USER_INIT_INSTRUCTION, TEST_WEATHER, START_OVER, UNDO_STEP, SHOW_KNOWLEDGE, SHOW_RAW_KNOWLEDGE, DEFINE_BOOL_EXP, DEFINE_VALUE_EXP, DEFINE_VALUE_DEMONSTRATION, DEFINE_PROCEDURE_EXP, DEFINE_PROCEDURE_DEMONSTATION,ADD_CONDITIONAL, ADD_CONDITIONAL_2, ADD_TO_SCRIPT, CHECKING_LOC, CHECKING_LOC0, RUN_THROUGH, MOVE_STEP, GET_SCOPE, ADD_ELSE, TELL_ELSE, ADD_TELL_ELSE, SCRIPT_ADD_TELL_ELSE, FIX_SCRIPT, FIX_SCRIPT2, FIX_SCOPE, RUN_THROUGH2, CHECKING_LOC2, TELL_IF, ADD_TELL_IF, SCRIPT_ADD_TELL_IF}


    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance);
    void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance);
    void setContext(Context context);
    void handleServerResponse(PumiceDialogManager dialogManager, int responseCode, String result);

}
