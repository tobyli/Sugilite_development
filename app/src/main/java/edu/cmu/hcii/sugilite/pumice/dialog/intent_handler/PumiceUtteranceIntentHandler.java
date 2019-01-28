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
    enum PumiceIntent {USER_INIT_INSTRUCTION, TEST_WEATHER, START_OVER, UNDO_STEP, SHOW_KNOWLEDGE, SHOW_RAW_KNOWLEDGE, BOOL_EXP_INSTRUCTION, DEFINE_VALUE_EXP, DEFINE_VALUE_DEMONSTRATION, DEFINE_PROCEDURE_EXP, DEFINE_PROCEDURE_DEMONSTATION, EXECUTION_POSITIVE, EXECUTION_NEGATIVE}

    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance);
    void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance);
    void setContext(Activity context);
}
