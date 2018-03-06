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
    void resultAvailable(List<String> matches);
    void listeningStarted();
    void listeningEnded();
    void speakingStarted();
    void speakingEnded();

}
