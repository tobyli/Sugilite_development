package edu.cmu.hcii.sugilite.sovite.conversation_state;

import java.io.Serializable;
import java.util.List;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;

/**
 * @author toby
 * @date 4/23/20
 * @time 2:00 PM
 */
public class SoviteConversationState implements Serializable {
    private String name;
    private SoviteSerializableRecoverableIntentHanlder soviteSerializableRecoverableIntentHanlder;
    private List<PumiceUtterance> utteranceHistory;

    public SoviteConversationState() {

    }

    public SoviteConversationState(String name) {
        this.name = name;
    }

    public void setSoviteSerializableRecoverableIntentHanlder(SoviteSerializableRecoverableIntentHanlder soviteSerializableRecoverableIntentHanlder) {
        this.soviteSerializableRecoverableIntentHanlder = soviteSerializableRecoverableIntentHanlder;
    }

    public void setUtteranceHistory(List<PumiceUtterance> utteranceHistory) {
        this.utteranceHistory = utteranceHistory;
    }

    public SoviteSerializableRecoverableIntentHanlder getSoviteSerializableRecoverableIntentHanlder() {
        return soviteSerializableRecoverableIntentHanlder;
    }

    public List<PumiceUtterance> getUtteranceHistory() {
        return utteranceHistory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
