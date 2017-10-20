package edu.cmu.hcii.sugilite.recording;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;

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
    private LayoutInflater lastLayoutInflator;
    private Set<Map.Entry<String, String>> lastAvailableAlternatives;

    public TextChangedEventHandler(SugiliteData sugiliteData, Context context, SharedPreferences sharedPreferences){
        this.sugiliteData = sugiliteData;
        this.context = context;
        this.sharedPreferences = sharedPreferences;
    }

    public void handle(SugiliteAvailableFeaturePack featurePack, Set<Map.Entry<String, String>> availableAlternatives, LayoutInflater layoutInflater, android.os.Handler uiThreadHandler){
        //handle the VIEW_TEXT_CHANGED event
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
            lastLayoutInflator = layoutInflater;
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
            lastLayoutInflator = layoutInflater;
            lastAvailableAlternatives = availableAlternatives;
        }

    }

    public void flush(){
        //TODO: show a recording popup for the text entry
        System.out.println("text changed event flushed");
        if(aggregatedFeaturePack != null) {
            Toast.makeText(context, "Text changed event flushed", Toast.LENGTH_SHORT).show();
            System.out.println("text changed event flushed successfully");
            //show the recording popup only after an text entry session has concluded
            RecordingPopUpDialog recordingPopUpDialog = new RecordingPopUpDialog(sugiliteData, context, aggregatedFeaturePack, sharedPreferences, lastLayoutInflator, RecordingPopUpDialog.TRIGGERED_BY_NEW_EVENT, lastAvailableAlternatives);
            sugiliteData.recordingPopupDialogQueue.add(recordingPopUpDialog);
            if (!sugiliteData.recordingPopupDialogQueue.isEmpty() && sugiliteData.hasRecordingPopupActive == false) {
                sugiliteData.hasRecordingPopupActive = true;
                sugiliteData.recordingPopupDialogQueue.poll().show();
            }
            aggregatedFeaturePack = null;
        }
    }

    private boolean belongsToTheSameSession(SugiliteAvailableFeaturePack earlierSession, SugiliteAvailableFeaturePack laterSession){
        if(earlierSession == null || laterSession == null)
            return false;

        if(earlierSession.packageName != null && laterSession.packageName != null &&
                (!earlierSession.packageName.equals(laterSession.packageName)))
            return false;
        if(earlierSession.viewId != null &&
                (!earlierSession.viewId.equals(laterSession.viewId)))
            return false;
        if(!earlierSession.className.equals(laterSession.className))
            return false;

        return true;
    }
}
