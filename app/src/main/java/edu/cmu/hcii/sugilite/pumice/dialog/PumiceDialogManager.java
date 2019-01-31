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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.hcii.sugilite.R;

import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceInitInstructionParsingHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceConditionalIntentHandler;

import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;

import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.ui.PumiceDialogActivity;
import edu.cmu.hcii.sugilite.pumice.ui.util.PumiceDialogUIHelper;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;

/**
 * @author toby
 * @date 10/9/18
 * @time 3:56 PM
 */
public class PumiceDialogManager{
    public enum Sender {AGENT, USER}
    public AppCompatActivity context;//made public from private
    private PumiceDialogView pumiceDialogView;
    private PumiceDialogUIHelper pumiceDialogUIHelper;
    private View speakButtonForCallback;
    private PumiceInitInstructionParsingHandler pumiceInitInstructionParsingHandler;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private SugiliteVerbalInstructionHTTPQueryManager httpQueryManager;
    private SharedPreferences sharedPreferences;
    private ExecutorService executorService;
    public SugiliteBlock tResult;
    public String check;
    public boolean addElse = false;
    public SugiliteBlock conditionBlock = null;

    private List<PumiceDialogState> stateHistoryList;

    //TODO: need to add a structure to represent undo

    //represents the current state of the dialog
    private PumiceDialogState pumiceDialogState;

    public PumiceDialogManager(AppCompatActivity context, String intentHandler){
        this.context = context;
        this.pumiceDialogView = new PumiceDialogView(context);
        this.pumiceDialogUIHelper = new PumiceDialogUIHelper(context);
        this.pumiceInitInstructionParsingHandler = new PumiceInitInstructionParsingHandler(context, this);
        this.stateHistoryList = new ArrayList<>();

        //set intent handler to be either conditional or default
        PumiceUtteranceIntentHandler pcih = null;
        if(intentHandler.equals("cond")) {
            pcih = new PumiceConditionalIntentHandler(this, context);
        }
        else {
            pcih = new PumiceDefaultUtteranceIntentHandler(this, context);
        }

        this.pumiceDialogState = new PumiceDialogState(pcih, new PumiceKnowledgeManager());
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.httpQueryManager = new SugiliteVerbalInstructionHTTPQueryManager(sharedPreferences);
        this.executorService = Executors.newCachedThreadPool();

        //** testing **
        this.pumiceDialogState.getPumiceKnowledgeManager().initForTesting();
    }
    public void sendUserMessage(String message){
        //send the user message with the current in use intent handler
        sendUserMessage(message, pumiceDialogState.getPumiceUtteranceIntentHandlerInUse());
    }

    /**
     * used when the user's message needs a different intent handler than the one currently in use
     * @param message
     * @param pumiceUtteranceIntentHandler
     */
    private void sendUserMessage(String message, PumiceUtteranceIntentHandler pumiceUtteranceIntentHandler){
        updateUtteranceIntentHandlerInANewState(pumiceUtteranceIntentHandler);
        // ** finished saving the current PumiceDialogState **

        PumiceUtterance utterance = new PumiceUtterance(Sender.USER, message, Calendar.getInstance().getTimeInMillis(), true,false);
        pumiceDialogState.getUtteranceHistory().add(utterance);
        pumiceDialogView.addMessage(utterance);

        //classify the intent of user message
        PumiceUtteranceIntentHandler.PumiceIntent intent = pumiceUtteranceIntentHandler.detectIntentFromUtterance(utterance);

        //handle the incoming user message based on the identified intent
        pumiceUtteranceIntentHandler.handleIntentWithUtterance(this, intent, utterance);
    }

    public void updateUtteranceIntentHandlerInANewState(PumiceUtteranceIntentHandler pumiceUtteranceIntentHandler){
        //save the current PumiceDialogState to the deque and get a new one
        stateHistoryList.add(pumiceDialogState);
        pumiceDialogState = pumiceDialogState.getDuplicateWithNewIntentHandler(context, pumiceUtteranceIntentHandler);
        if(stateHistoryList.size() >= 1){
            pumiceDialogState.setPreviousState(stateHistoryList.get(stateHistoryList.size() - 1));
        }
    }

    /**
     * send a message from the agent that contains a view -- add the alt-text to the utterance history
     * @param viewContent
     * @param altText
     * @param isSpokenMessage
     * @param requireUserResponse
     */
    public void sendAgentViewMessage(View viewContent, String altText, boolean isSpokenMessage, boolean requireUserResponse){
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                PumiceUtterance utterance = new PumiceUtterance(Sender.AGENT, "[CARD]" + altText, Calendar.getInstance().getTimeInMillis(), isSpokenMessage, requireUserResponse);
                pumiceDialogState.getUtteranceHistory().add(utterance);
                pumiceDialogView.addMessage(viewContent, Sender.AGENT);
                handleSpeakingAndUserResponse(altText, isSpokenMessage, requireUserResponse);
            }
        });
    }

    /**
     * send a message from the agent that contains a string -- add the alttext to the utterance history
     * @param message
     * @param isSpokenMessage
     * @param requireUserResponse
     */
    public void sendAgentMessage(String message, boolean isSpokenMessage, boolean requireUserResponse){
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                PumiceUtterance utterance = new PumiceUtterance(Sender.AGENT, message, Calendar.getInstance().getTimeInMillis(), isSpokenMessage, requireUserResponse);
                pumiceDialogState.getUtteranceHistory().add(utterance);
                pumiceDialogView.addMessage(utterance);
                handleSpeakingAndUserResponse(message, isSpokenMessage, requireUserResponse);
            }
        });
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
        if(isSpokenMessage && sugiliteVoiceRecognitionListener != null) {
            sugiliteVoiceRecognitionListener.speak(utterance, String.valueOf(Calendar.getInstance().getTimeInMillis()), new Runnable() {
                @Override
                public void run() {
                    if(requireUserResponse && speakButtonForCallback != null){
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

        public void setPumiceUtteranceIntentHandlerInUse(PumiceUtteranceIntentHandler pumiceUtteranceIntentHandlerInUse) {
            this.pumiceUtteranceIntentHandlerInUse = pumiceUtteranceIntentHandlerInUse;
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
            //PumiceKnowledgeManager newPumiceKnowledgeManager = gson.fromJson(gson.toJson(pumiceKnowledgeManager), PumiceKnowledgeManager.class);
            //TODO: duplicate the knowledge manager
            return new PumiceDialogState(newUtteranceHistory, intentHandler, pumiceKnowledgeManager);
        }
    }

    public void setPumiceUtteranceIntentHandlerInUse(PumiceUtteranceIntentHandler p) {
        this.pumiceDialogState.setPumiceUtteranceIntentHandlerInUse(p);
    }

    public SugiliteVerbalInstructionHTTPQueryManager getHttpQueryManager() {
        return httpQueryManager;
    }

    public PumiceInitInstructionParsingHandler getPumiceInitInstructionParsingHandler() {
        return pumiceInitInstructionParsingHandler;
    }

    public class GetTheIntentHandlingResultForTheNextUserInput implements Callable<Object> {
        @Override
        public Object call() throws Exception {
            return null;
        }
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void runOnMainThread(Runnable r) {
        context.runOnUiThread(r);
    }

    /*
    store top result from a parser query
     */
    public void settResult(SugiliteBlock tResult) {
        this.tResult = tResult;
    }

}
