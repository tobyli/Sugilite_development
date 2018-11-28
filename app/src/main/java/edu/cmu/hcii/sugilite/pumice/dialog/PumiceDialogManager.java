package edu.cmu.hcii.sugilite.pumice.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.support.v7.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceInitInstructionParsingHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceStartUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceConditionalIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.ui.PumiceDialogActivity;
import edu.cmu.hcii.sugilite.pumice.ui.util.PumiceDialogUIHelper;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

/**
 * @author toby
 * @date 10/9/18
 * @time 3:56 PM
 */
public class PumiceDialogManager implements SugiliteVerbalInstructionHTTPQueryInterface {
    public enum Sender {AGENT, USER}
    public AppCompatActivity context;//made public from private
    private PumiceDialogView pumiceDialogView;
    private PumiceDialogUIHelper pumiceDialogUIHelper;
    private View speakButtonForCallback;
    private PumiceInitInstructionParsingHandler pumiceInitInstructionParsingHandler;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private SugiliteVerbalInstructionHTTPQueryManager httpQueryManager;
    private SharedPreferences sharedPreferences;

    private List<PumiceDialogState> stateHistoryList;

    //TODO: need to add a structure to represent undo

    //represents the current state of the dialog
    private PumiceDialogState pumiceDialogState;

    public PumiceDialogManager(AppCompatActivity context){
        this.context = context;
        this.pumiceDialogView = new PumiceDialogView(context);
        this.pumiceDialogUIHelper = new PumiceDialogUIHelper(context);
        this.pumiceInitInstructionParsingHandler = new PumiceInitInstructionParsingHandler(context, this);
        this.stateHistoryList = new ArrayList<>();
        this.pumiceDialogState = new PumiceDialogState(new PumiceConditionalIntentHandler(context), new PumiceKnowledgeManager()); //new PumiceStartUtteranceIntentHandler(context)
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.httpQueryManager = new SugiliteVerbalInstructionHTTPQueryManager(this, sharedPreferences);

        //** testing **
        this.pumiceDialogState.getPumiceKnowledgeManager().initForTesting();
    }
    public void sendUserMessage(String message){
        //send the user message with the current in use intent handler
        sendUserMessage(message, pumiceDialogState.getPumiceUtteranceIntentHandlerInUse());
    }

    private void sendUserMessage(String message, PumiceUtteranceIntentHandler pumiceUtteranceIntentHandler){
        //save the current PumiceDialogState to the deque and get a new one
        stateHistoryList.add(pumiceDialogState);
        pumiceDialogState = pumiceDialogState.getDuplicateWithNewIntentHandler(context, pumiceUtteranceIntentHandler);
        if(stateHistoryList.size() >= 1){
            pumiceDialogState.setPreviousState(stateHistoryList.get(stateHistoryList.size() - 1));
        }
        // ** finished saving the current PumiceDialogState **

        PumiceUtterance utterance = new PumiceUtterance(Sender.USER, message, Calendar.getInstance().getTimeInMillis(), true,false);
        pumiceDialogState.getUtteranceHistory().add(utterance);
        pumiceDialogView.addMessage(utterance);

        //classify the intent of user message
        PumiceUtteranceIntentHandler.PumiceIntent intent = pumiceUtteranceIntentHandler.detectIntentFromUtterance(utterance);


        //handle the incoming user message based on the identified intent
        pumiceUtteranceIntentHandler.handleIntentWithUtterance(this, intent, utterance);

    }

    /**
     * send a message from the agent that contains a view -- add the alt-text to the utterance history
     * @param viewContent
     * @param altText
     * @param isSpokenMessage
     * @param requireUserResponse
     */
    public void sendAgentViewMessage(View viewContent, String altText, boolean isSpokenMessage, boolean requireUserResponse){
        PumiceUtterance utterance = new PumiceUtterance(Sender.AGENT, "[CARD]" + altText, Calendar.getInstance().getTimeInMillis(), isSpokenMessage, requireUserResponse);
        pumiceDialogState.getUtteranceHistory().add(utterance);
        pumiceDialogView.addMessage(viewContent, Sender.AGENT);

        handleSpeakingAndUserResponse(altText, isSpokenMessage, requireUserResponse);
    }

    /**
     * send a message from the agent that contains a string -- add the alttext to the utterance history
     * @param message
     * @param isSpokenMessage
     * @param requireUserResponse
     */
    public void sendAgentMessage(String message, boolean isSpokenMessage, boolean requireUserResponse){
        System.out.println("SEND");
        PumiceUtterance utterance = new PumiceUtterance(Sender.AGENT, message, Calendar.getInstance().getTimeInMillis(), isSpokenMessage, requireUserResponse);
        pumiceDialogState.getUtteranceHistory().add(utterance);
        pumiceDialogView.addMessage(utterance);

        handleSpeakingAndUserResponse(message, isSpokenMessage, requireUserResponse);
    }
    public void revertToLastState(){
        if(pumiceDialogState.getPreviousState() != null && pumiceDialogState.getPreviousState().getPreviousState() != null) {
            revertToState(pumiceDialogState.getPreviousState().getPreviousState());
        } else {
            sendAgentMessage("Can't undo, already at the start of the conversation", true, false);
        }
    }

    /**
     * revert to a state by creating a DUPLICATE of the state and add it to the end of the state list
     * @param state
     */
    public void revertToState(PumiceDialogState state){
        if(pumiceDialogState.getUtteranceHistory().size() >= 1) {
            sendAgentMessage("Going back to a previous state...", true, false);
            this.pumiceDialogState = state.getDuplicateWithNewIntentHandler(context, state.getPumiceUtteranceIntentHandlerInUse());
            this.pumiceDialogState.setPreviousState(state.getPreviousState());
            PumiceUtterance lastUtterance = pumiceDialogState.getUtteranceHistory().get(pumiceDialogState.getUtteranceHistory().size() - 1);
            if (lastUtterance != null && lastUtterance.getSender().equals(Sender.AGENT)) {
                pumiceDialogState.getUtteranceHistory().remove(pumiceDialogState.getUtteranceHistory().size() - 1);
                sendAgentMessage(lastUtterance.getContent(), lastUtterance.isSpoken(), lastUtterance.isRequireUserResponse());
            }
        } else {
            sendAgentMessage("Can't undo, already at the start of the conversation", true, false);
        }
    }

    public void startOverState(){
        if(stateHistoryList.get(0) != null){
            revertToState(stateHistoryList.get(0));
        }
    }

    public PumiceKnowledgeManager getPumiceKnowledgeManager() {
        return pumiceDialogState.getPumiceKnowledgeManager();
    }

    private void handleSpeakingAndUserResponse(String utterance, boolean isSpokenMessage, boolean requireUserResponse){
        System.out.println("HANDLE");
        if(isSpokenMessage && sugiliteVoiceRecognitionListener != null) {
            sugiliteVoiceRecognitionListener.speak(utterance, String.valueOf(Calendar.getInstance().getTimeInMillis()), new Runnable() {
                @Override
                public void run() {
                    if(requireUserResponse && speakButtonForCallback != null){
                        System.out.println("HITHER");
                        speakButtonForCallback.callOnClick();
                        pumiceDialogView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //replace this line to scroll up or down
                                speakButtonForCallback.callOnClick();
                            }
                        }, 500L);
                    }
                }
            });
        } else {
            if(requireUserResponse && speakButtonForCallback != null){
                pumiceDialogView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //replace this line to scroll up or down
                        speakButtonForCallback.callOnClick();
                    }
                }, 500L);
            }
        }

    }

    public PumiceDialogView getPumiceDialogView() {
        return pumiceDialogView;
    }

    public void setSpeakButtonForCallback(View speakButtonForCallback) {
        this.speakButtonForCallback = speakButtonForCallback;
    }

    public void setSugiliteVoiceRecognitionListener(SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener) {
        this.sugiliteVoiceRecognitionListener = sugiliteVoiceRecognitionListener;
    }

    public class PumiceUtterance {
        private Sender sender;
        private String content;
        private long timeStamp;
        private boolean requireUserResponse;
        private boolean isSpoken;

        public String getContent() {
            return content;
        }

        public Sender getSender() {
            return sender;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public boolean isRequireUserResponse() {
            return requireUserResponse;
        }

        public boolean isSpoken() {
            return isSpoken;
        }

        //if set to true, this utterance will trigger a recording of user input

        public PumiceUtterance(Sender sender, String content, long timeStamp, boolean isSpoken, boolean requireUserResponse){
            this.sender = sender;
            this.content = content;
            this.timeStamp = timeStamp;
            this.isSpoken = isSpoken;
            this.requireUserResponse = requireUserResponse;
        }

    }

    public class PumiceDialogView extends LinearLayout {
        public PumiceDialogView(Context context){
            super(context);
            this.setOrientation(VERTICAL);
            //this.setGravity(Gravity.BOTTOM);
        }

        public void addMessage(PumiceUtterance utterance){
            View view = pumiceDialogUIHelper.getDialogLayout(utterance);
            this.addView(view);
            ScrollView mScrollView = (ScrollView) context.findViewById(R.id.pumice_dialog_scrollLayout);
            if(mScrollView != null) {
                mScrollView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //replace this line to scroll up or down
                        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                }, 1000L);
            }
        }

        public void addMessage(View contentView, Sender sender){
            View view = pumiceDialogUIHelper.getDialogLayout(contentView, sender);
            this.addView(view);
            ScrollView mScrollView = (ScrollView) context.findViewById(R.id.pumice_dialog_scrollLayout);
            if(mScrollView != null) {
                mScrollView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //replace this line to scroll up or down
                        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                }, 1000L);
            }
        }

        public void clearMessage(){
            this.removeAllViews();
        }
    }

    public class PumiceDialogState {
        private List<PumiceUtterance> utteranceHistory;
        private transient PumiceUtteranceIntentHandler pumiceUtteranceIntentHandlerInUse;
        private PumiceKnowledgeManager pumiceKnowledgeManager;
        private PumiceDialogState previousState;


        public PumiceDialogState (List<PumiceUtterance> utteranceHistory, PumiceUtteranceIntentHandler pumiceUtteranceIntentHandler, PumiceKnowledgeManager pumiceKnowledgeManager){
            this.utteranceHistory = utteranceHistory;
            this.pumiceUtteranceIntentHandlerInUse = pumiceUtteranceIntentHandler;
            this.pumiceKnowledgeManager = pumiceKnowledgeManager;
        }

        public PumiceDialogState(PumiceUtteranceIntentHandler pumiceUtteranceIntentHandler, PumiceKnowledgeManager pumiceKnowledgeManager){
            this.utteranceHistory = new ArrayList<>();
            this.pumiceUtteranceIntentHandlerInUse = pumiceUtteranceIntentHandler;
            this.pumiceKnowledgeManager = pumiceKnowledgeManager;
        }

        public PumiceKnowledgeManager getPumiceKnowledgeManager() {
            return pumiceKnowledgeManager;
        }

        public List<PumiceUtterance> getUtteranceHistory() {
            return utteranceHistory;
        }

        public PumiceUtteranceIntentHandler getPumiceUtteranceIntentHandlerInUse() {
            return pumiceUtteranceIntentHandlerInUse;
        }

        public void setPreviousState(PumiceDialogState previousState) {
            this.previousState = previousState;
        }

        public PumiceDialogState getPreviousState() {
            return previousState;
        }

        PumiceDialogState getDuplicateWithNewIntentHandler(Context context, PumiceUtteranceIntentHandler intentHandler){
            Gson gson = new Gson();
            List<PumiceUtterance> newUtteranceHistory =  new ArrayList<>(utteranceHistory);
            PumiceKnowledgeManager newPumiceKnowledgeManager = gson.fromJson(gson.toJson(pumiceKnowledgeManager), PumiceKnowledgeManager.class);
            return new PumiceDialogState(newUtteranceHistory, intentHandler, newPumiceKnowledgeManager);
        }
    }

    public SugiliteVerbalInstructionHTTPQueryManager getHttpQueryManager() {
        return httpQueryManager;
    }

    @Override
    public void runOnMainThread(Runnable r) {
        context.runOnUiThread(r);

    }

    @Override
    public void resultReceived(int responseCode, String result) {
        //TODO: handle server response from the semantic parsing server
        Gson gson = new Gson();
        try {
            PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
            if (resultPacket.utteranceType != null) {
                switch (PumiceUtteranceIntentHandler.PumiceIntent.valueOf(resultPacket.utteranceType)) {
                    case USER_INIT_INSTRUCTION:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            if (topResult.formula != null) {
                                sendAgentMessage("Received the parsing result from the server: ", true, false);
                                sendAgentMessage(topResult.formula, false, false);
                                pumiceInitInstructionParsingHandler.parseFromNewInitInstruction(topResult.formula);
                            }
                        }
                        break;
                    default:
                        sendAgentMessage("Can't read from the server response", true, false);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
