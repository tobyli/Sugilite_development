package edu.cmu.hcii.sugilite.sovite.conversation_state;

import android.app.Activity;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;

/**
 * @author toby
 * @date 4/23/20
 * @time 2:04 PM
 */
public interface SoviteSerializableRecoverableIntentHanlder extends Serializable, PumiceUtteranceIntentHandler {
    void inflateFromDeserializedInstance(Activity context, PumiceDialogManager pumiceDialogManager, SugiliteData sugiliteData, PumiceDefaultUtteranceIntentHandler pumiceDefaultUtteranceIntentHandler);
}

