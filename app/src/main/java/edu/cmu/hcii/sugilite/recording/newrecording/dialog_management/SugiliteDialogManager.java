package edu.cmu.hcii.sugilite.recording.newrecording.dialog_management;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Calendar;
import java.util.List;

import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

/**
 * @author toby
 * @date 2/20/18
 * @time 2:51 PM
 */
public abstract class SugiliteDialogManager implements SugiliteVoiceInterface {
    protected Context context;
    protected TextToSpeech tts;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private boolean isListening = false;
    private SugiliteDialogState currentState = null;

    public SugiliteDialogManager(Context context, TextToSpeech tts) {
        this.context = context;
        this.tts = tts;
        this.sugiliteVoiceRecognitionListener = new SugiliteVoiceRecognitionListener(context, this, tts);
    }

    public void initPrompt() {
        if (currentState.getPrompt() != null && currentState.getPromptOnPlayingDoneRunnable() != null) {
            speak(currentState.getPrompt(), currentState.getPromptOnPlayingDoneRunnable());
        }
    }

    /**
     * this class should initiate the dialog manager by creating the states, set the current state, and call initPrompt()
     */
    public abstract void initDialogManager();

    public void stopASRandTTS() {
        stopListening();
        sugiliteVoiceRecognitionListener.stopTTS();
    }

    /**
     * callback from ASR -> called when a set of ASR results is available
     * @param matches
     */
    @Override
    public void resultAvailable(List<String> matches) {
        //fill in the ASR results for the current state
        currentState.setASRResult(matches);

        //invoke the on switched away runnable of the current state
        if (currentState.getOnSwitchedAwayRunnable() != null) {
            currentState.getOnSwitchedAwayRunnable().run();
        }

        //switch state
        currentState = currentState.getNextState(matches);

        if (currentState != null) {
            //invoke the on initiated runnable of the next state
            if (currentState.getOnInitiatedRunnable() != null) {
                currentState.getOnInitiatedRunnable().run();
            }

            //play the prompt of the next state
            if (currentState.getPrompt() != null && currentState.getPromptOnPlayingDoneRunnable() != null) {
                speak(currentState.getPrompt(), currentState.getPromptOnPlayingDoneRunnable());
            }
        }
    }

    @Override
    public void listeningStarted() {
        isListening = true;
    }

    @Override
    public void listeningEnded() {
        isListening = false;
    }

    public boolean isListening() {
        return isListening;
    }

    public void startListening() {
        sugiliteVoiceRecognitionListener.startListening();
    }

    public void stopListening() {
        sugiliteVoiceRecognitionListener.stopListening();
        listeningEnded();
    }

    public void speak(String utterance, Runnable runnableOnDone) {
        sugiliteVoiceRecognitionListener.speak(utterance, String.valueOf(Calendar.getInstance().getTimeInMillis()), runnableOnDone);
    }

    public SugiliteDialogState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(SugiliteDialogState currentState) {
        this.currentState = currentState;
    }
}
