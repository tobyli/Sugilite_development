package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;

/**
 * @author toby
 * @date 10/26/18
 * @time 1:33 PM
 */
public interface PumiceUtteranceIntentHandler {
<<<<<<< HEAD
<<<<<<< HEAD
    enum PumiceIntent {INIT_INSTRUCTION, TEST_WEATHER, START_OVER, UNDO_STEP, SHOW_KNOWLEDGE, SHOW_RAW_KNOWLEDGE, ADD_CONDITIONAL}
=======
    enum PumiceIntent {USER_INIT_INSTRUCTION, TEST_WEATHER, START_OVER, UNDO_STEP, SHOW_KNOWLEDGE, SHOW_RAW_KNOWLEDGE}
>>>>>>> ca4093a365953dac63660eb9db6e3d62abf3f3d6
=======
    enum PumiceIntent {USER_INIT_INSTRUCTION, TEST_WEATHER, START_OVER, UNDO_STEP, SHOW_KNOWLEDGE, SHOW_RAW_KNOWLEDGE}
>>>>>>> ca4093a365953dac63660eb9db6e3d62abf3f3d6


    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance);
    void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance);
    void setContext(Context context);

}
