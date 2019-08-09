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
    enum PumiceIntent {UNRECOGNIZED, PARSE_CONFIRM_POSITIVE, PARSE_CONFIRM_NEGATIVE, USER_INIT_INSTRUCTION, TEST_WEATHER, START_OVER, UNDO_STEP, SHOW_KNOWLEDGE, SHOW_RAW_KNOWLEDGE, DEFINE_BOOL_EXP, DEFINE_VALUE_EXP, BOOL_EXP_INSTRUCTION, DEFINE_VALUE_DEMONSTRATION, DEFINE_PROCEDURE_EXP, DEFINE_PROCEDURE_DEMONSTATION, EXECUTION_POSITIVE, EXECUTION_NEGATIVE, ADD_CONDITIONAL_0, ADD_CONDITIONAL_Y, ADD_CONDITIONAL_N, ADD_CONDITIONAL_X, ADD_TO_SCRIPT_X, ADD_TO_SCRIPT_N, ADD_TO_SCRIPT_Y, CHECKING_LOC_Y, CHECKING_LOC_N, CHECKING_LOC_X, CHECKING_LOC_IDK, MOVE_STEP, MOVE_STEP_X, GET_THEN_BLOCK, GET_THEN_BLOCK_X, GET_THEN_BLOCK_IDK, WANT_ELSE_BLOCK, PROBLEM_1, PROBLEM_1_X, SOLUTION_1_N, SOLUTION_1_X, SOLUTION_1_IDK, SOLUTION_1_Y, SOLUTION_2_Y, SOLUTION_2_N, SOLUTION_2_X, MOVE_STEP_2, MOVE_STEP_2_X, GET_ELSE_BLOCK_Y, GET_ELSE_BLOCK_X, DONE_1, EXPLAIN_ELSE_BLOCK, DEMONSTRATE_ELSE_BLOCK, CHECKING_ELSE_X, CHECKING_ELSE, PROBLEM_2_X, WANT_CHECK_CONDITION, CHECK_CONDITION_X, CHECK_CONDITION, PROBLEM_3_X, CHANGE_CHECK, CHANGE_CHECK_X, DONE_2}

    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance);
    void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance);
    void setContext(Activity context);
    void sendPromptForTheIntentHandler();
}
