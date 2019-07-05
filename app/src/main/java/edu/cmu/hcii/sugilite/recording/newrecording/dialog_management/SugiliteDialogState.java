package edu.cmu.hcii.sugilite.recording.newrecording.dialog_management;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * @author toby
 * @date 2/20/18
 * @time 4:11 PM
 */
public interface SugiliteDialogState {

    /**
     * get the name of the state
     *
     * @return the name of the state
     */
    String getName();

    /**
     * get the prompt of the state
     *
     * @return the prompt of this state
     */
    @Nullable
    String getPrompt();

    /**
     * the runnable to run after the prompt has done playing
     *
     * @return
     */
    @Nullable
    Runnable getPromptOnPlayingDoneRunnable();

    /**
     * the runnable to run at the beginning of a state
     *
     * @return
     */
    @Nullable
    Runnable getOnInitiatedRunnable();

    /**
     * the runnable to run at the end of a state
     *
     * @return
     */
    @Nullable
    Runnable getOnSwitchedAwayRunnable();

    /**
     * get the ASR result for the state
     *
     * @return
     */
    @Nullable
    List<String> getASRResult();

    /**
     * called by the dialog manager to set the ASR result for the state
     *
     * @param asrResult
     */
    void setASRResult(List<String> asrResult);

    /**
     * set the dialog manager for the state
     *
     * @param dialogManager
     */
    void setDialogManager(SugiliteDialogManager dialogManager);

    /**
     * @param utterances the utterances received
     * @return the next state to move to after receiving this utterance, return null if exit
     */
    @Nullable
    SugiliteDialogState getNextState(List<String> utterances);
}
