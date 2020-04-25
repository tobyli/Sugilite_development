package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.content.Context;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;

/**
 * @author toby
 * @date 10/26/18
 * @time 1:33 PM
 */
public interface PumiceUtteranceIntentHandler {
    enum PumiceIntent {
        //PumiceDefaultUtteranceIntentHandler
        USER_INIT_INSTRUCTION,
        TEST_WEATHER,
        START_OVER,
        UNDO_STEP,
        LIST_KNOWLEDGE,
        LIST_KNOWLEDGE_IN_RAW_FORM,

        //PumiceConditionalIntentHandler
        DEFINE_BOOL_EXPRESSION_INSTRUCTION,
        DEFINE_VALUE_EXPLANATION,
        DEFINE_VALUE_DEMONSTRATION,
        DEFINE_PROCEDURE_EXPLANATION,
        DEFINE_PROCEDURE_DEMONSTATION,

        //PumiceScriptExecutingConfirmationIntentHandler, PumiceBooleanExpKnowledgeGeneralizationIntentHandler
        EXECUTION_CONFIRM_POSITIVE,
        EXECUTION_CONFIRM_NEGATIVE,

        //PumiceAskIfNeedElseStatementHandler, PumiceParsingResultNoResolveConfirmationHandler, PumiceParsingResultWithResolveFnConfirmationHandler
        PARSE_CONFIRM_POSITIVE,
        PARSE_CONFIRM_NEGATIVE,

        //SoviteIntentClassificationErrorIntentHandler
        APP_REFERENCE,

        // unrecognized
        UNRECOGNIZED,

        // deprecated
        DEFINE_BOOL_EXP,
        ADD_CONDITIONAL,
        ADD_CONDITIONAL_2,
        ADD_TO_SCRIPT,
        CHECKING_LOC,
        CHECKING_LOC0,
        RUN_THROUGH,
        MOVE_STEP,
        GET_SCOPE,
        ADD_ELSE,
        TELL_ELSE,
        ADD_TELL_ELSE,
        SCRIPT_ADD_TELL_ELSE,
        FIX_SCRIPT,
        FIX_SCRIPT2,
        FIX_SCOPE,
        RUN_THROUGH2,
        CHECKING_LOC2,
        TELL_IF,
        ADD_TELL_IF,
        SCRIPT_ADD_TELL_IF

    }
    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    PumiceIntent detectIntentFromUtterance(PumiceUtterance utterance);
    void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceUtterance utterance);
    void setContext(Activity context);
    void sendPromptForTheIntentHandler();
}
