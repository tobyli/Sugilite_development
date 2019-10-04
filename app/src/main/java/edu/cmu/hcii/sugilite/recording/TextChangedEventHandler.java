package edu.cmu.hcii.sugilite.recording;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;

import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;

/**
 * @author toby
 * @date 7/23/17
 * @time 10:35 PM
 */

/**
 * this handler should receive VIEW_TEXT_CHANGED events and keep tracks of text entry sessions
 */
public class TextChangedEventHandler {
    /**
     * 1. text_changed events should be sent here instead of to RecordingPopUpDialog
     * 2. keep an update of the current text_change event
     * 3. ignore click events on the text box
     * 4. commit the change when receives an event on an element other than the textbox
     */
    private SugiliteData sugiliteData;
    private Context context;
    private SharedPreferences sharedPreferences;

    private SugiliteAvailableFeaturePack aggregatedFeaturePack;
    private Set<Map.Entry<String, String>> lastAvailableAlternatives;
    private android.os.Handler uiThreadHandler;
    protected static final String TAG = TextChangedEventHandler.class.getSimpleName();


    public TextChangedEventHandler(SugiliteData sugiliteData, Context context, SharedPreferences sharedPreferences, android.os.Handler uiThreadHandler){
        this.sugiliteData = sugiliteData;
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.uiThreadHandler = uiThreadHandler;
    }

    public void handle(SugiliteAvailableFeaturePack featurePack, Set<Map.Entry<String, String>> availableAlternatives){
        //handle the VIEW_TEXT_CHANGED event
        if(featurePack == null)
            return;
        if(belongsToTheSameSession(aggregatedFeaturePack, featurePack)){
            //same session
            featurePack.afterText = featurePack.text;
            if(aggregatedFeaturePack != null){
                featurePack.beforeText = aggregatedFeaturePack.beforeText;
                //the aggregated feature pack should inherit the bounds from the first event (so it won't be effected by the changing size of the textbox because of the input
                featurePack.boundsInParent = aggregatedFeaturePack.boundsInParent;
                featurePack.boundsInScreen = aggregatedFeaturePack.boundsInScreen;
                featurePack.text = featurePack.beforeText;
            }
            aggregatedFeaturePack = featurePack;
            lastAvailableAlternatives = availableAlternatives;
        }
        else if(featurePack != null){
            //handle (and consume) the aggregatedFeaturePack and start the new one
            System.out.println("flush from an unmatched text changed event");
            Runnable flush = new Runnable() {
                @Override
                public void run() {
                    flush();
                }
            };
            uiThreadHandler.post(flush);

            featurePack.afterText = featurePack.text;
            if(featurePack.beforeText != null) {
                featurePack.text = featurePack.beforeText;
            }
            aggregatedFeaturePack = featurePack;
            lastAvailableAlternatives = availableAlternatives;
        }

    }

    public void flush(){
        //TODO: show a recording popup for the text entry
        System.out.println("text changed event flushed");
        if(aggregatedFeaturePack != null) {
            System.out.println("text changed event flushed successfully");
            //show the recording popup only after an text entry session has concluded
            if (aggregatedFeaturePack.text == null) {
                //dirty fix
                aggregatedFeaturePack.text = "NULL";
            }
            RecordingPopUpDialog recordingPopUpDialog = new RecordingPopUpDialog(sugiliteData, context, aggregatedFeaturePack, sharedPreferences, RecordingPopUpDialog.TRIGGERED_BY_NEW_EVENT, lastAvailableAlternatives);
            sugiliteData.recordingPopupDialogQueue.add(recordingPopUpDialog);
            if (!sugiliteData.recordingPopupDialogQueue.isEmpty() && sugiliteData.hasRecordingPopupActive == false) {
                sugiliteData.hasRecordingPopupActive = true;
                Log.i(TAG, "FLUSH-TAG1");
                sugiliteData.recordingPopupDialogQueue.poll().show();
                Log.i(TAG, "FLUSH-TAG2");
            }
            aggregatedFeaturePack = null;
        }
    }

    private boolean belongsToTheSameSession(SugiliteAvailableFeaturePack earlierSession, SugiliteAvailableFeaturePack laterSession){
        if(earlierSession == null) {
            return true;
        }
        if(earlierSession.packageName != null && laterSession.packageName != null &&
                (!earlierSession.packageName.equals(laterSession.packageName))) {
            return false;
        }
        if(earlierSession.viewId != null &&
                (!earlierSession.viewId.equals(laterSession.viewId))) {
            return false;
        }
        if(earlierSession.className != null &&
                !earlierSession.className.equals(laterSession.className)) {
            return false;
        }

        return true;
    }
}
