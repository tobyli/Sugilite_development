package edu.cmu.hcii.sugilite.recording.newrecording.dialog_management;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author toby
 * @date 2/20/18
 * @time 4:17 PM
 */
public class SugiliteDialogSimpleState implements SugiliteDialogState {

    private String prompt;
    private Map<SugiliteDialogState, SugiliteDialogUtteranceFilter> nextStateUtteranceFilterMap;
    private Map<Runnable, SugiliteDialogUtteranceFilter> exitRunnableUtteranceFilterMap;
    private SugiliteDialogState unmatchedState;
    private SugiliteDialogState noASRResultState;
    private SugiliteDialogManager dialogManager;
    private Runnable onSwitchedAwayRunnable;
    private Runnable onInitiatedRunnable;
    private String name;
    private List<String> asrResult;

    public SugiliteDialogSimpleState(String name, SugiliteDialogManager dialogManager) {
        nextStateUtteranceFilterMap = new HashMap<>();
        exitRunnableUtteranceFilterMap = new HashMap<>();
        this.name = name;
        this.dialogManager = dialogManager;
    }

    public SugiliteDialogState getNextState(List<String> utterances) {
        if (utterances == null || utterances.isEmpty()) {
            //the error handling state when no asr result is available
            return noASRResultState;
        }
        String utterance = utterances.get(0);

        for (Map.Entry<Runnable, SugiliteDialogUtteranceFilter> entry : exitRunnableUtteranceFilterMap.entrySet()) {
            if (entry.getValue().checkIfMatch(utterance)) {
                //exit points of the state
                entry.getKey().run();
                return null;
            }
        }

        for (Map.Entry<SugiliteDialogState, SugiliteDialogUtteranceFilter> entry : nextStateUtteranceFilterMap.entrySet()) {
            if (entry.getValue().checkIfMatch(utterance)) {
                //matched
                return entry.getKey();
            }
        }
        //the error handling state when the asr result doesn't match anything
        return unmatchedState;
    }

    public void addNextStateUtteranceFilter(SugiliteDialogState nextState, SugiliteDialogUtteranceFilter utteranceFilter) {
        nextStateUtteranceFilterMap.put(nextState, utteranceFilter);
    }

    public void addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter utteranceFilter, Runnable exitRunnable) {
        exitRunnableUtteranceFilterMap.put(exitRunnable, utteranceFilter);
    }

    @Override
    public Runnable getPromptOnPlayingDoneRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                dialogManager.startListening();
            }
        };
    }

    @Override
    public Runnable getOnInitiatedRunnable() {
        return onInitiatedRunnable;
    }

    public void setOnInitiatedRunnable(Runnable onInitiatedRunnable) {
        this.onInitiatedRunnable = onInitiatedRunnable;
    }

    @Override
    public List<String> getASRResult() {
        return asrResult;
    }

    @Override
    public void setASRResult(List<String> asrResult) {
        this.asrResult = asrResult;
    }

    @Override
    public Runnable getOnSwitchedAwayRunnable() {
        return onSwitchedAwayRunnable;
    }

    public void setOnSwitchedAwayRunnable(Runnable onSwitchedAwayRunnable) {
        this.onSwitchedAwayRunnable = onSwitchedAwayRunnable;
    }

    @Override
    public void setDialogManager(SugiliteDialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public SugiliteDialogState getNoASRResultState() {
        return noASRResultState;
    }

    public void setNoASRResultState(SugiliteDialogState noASRResultState) {
        this.noASRResultState = noASRResultState;
    }

    public SugiliteDialogState getUnmatchedState() {
        return unmatchedState;
    }

    public void setUnmatchedState(SugiliteDialogState unmatchedState) {
        this.unmatchedState = unmatchedState;
    }


}
