package edu.cmu.hcii.sugilite.verbal_instruction_demo.speech;

import java.util.List;

/**
 * @author toby
 * @date 12/10/17
 * @time 12:35 AM
 */
public interface SugiliteVoiceInterface {
    /**
     * callback when the ASR result is available
     * @param matches
     */
    void resultAvailableCallback(List<String> matches, boolean isFinal);
    void listeningStartedCallback();
    void listeningEndedCallback();
    void speakingStartedCallback();
    void speakingEndedCallback();

}
