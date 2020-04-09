package edu.cmu.hcii.sugilite.pumice.ui;

import android.app.Activity;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;

import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.sovite.conversation.dialog.SoviteKnowledgeManagementDialog;
import edu.cmu.hcii.sugilite.sovite.study.SoviteStudyDumpGenerateDialog;
import edu.cmu.hcii.sugilite.sovite.study.SoviteStudyDumpLoadDialog;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteAndroidAPIVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteGoogleCloudVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

import static edu.cmu.hcii.sugilite.Const.MUL_ZEROS;
import static edu.cmu.hcii.sugilite.Const.RECORDING_DARK_GRAY_COLOR;
import static edu.cmu.hcii.sugilite.Const.RECORDING_OFF_BUTTON_COLOR;
import static edu.cmu.hcii.sugilite.Const.RECORDING_ON_BUTTON_COLOR;
import static edu.cmu.hcii.sugilite.Const.RECORDING_SPEAKING_BUTTON_COLOR;
import static edu.cmu.hcii.sugilite.Const.RECORDING_WHITE_COLOR;

public class PumiceDialogActivity extends AppCompatActivity implements SugiliteVoiceInterface {
    private PumiceDialogManager pumiceDialogManager;
    private EditText userTextBox;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private TextToSpeech tts;
    protected AnimationDrawable speakingDrawable;
    protected AnimationDrawable listeningDrawable;
    protected Drawable notListeningDrawable;
    public boolean isListening = false;
    public boolean isSpeaking = false;
    private ImageButton speakButton;
    private Runnable speakingEndedRunnable;
    private boolean runSpeakingEndedRunnable = false;
    private Activity context;
    private SugiliteData sugiliteData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_pumice_dialog);
        this.getSupportActionBar().setTitle(Const.appNameUpperCase);
        this.context = this;
        this.speakButton = (ImageButton) findViewById(R.id.button3);
        this.userTextBox = (EditText) findViewById(R.id.pumice_user_textbox);

        //initiate tts

        this.sugiliteData = (SugiliteData) getApplication();
        this.tts = sugiliteData.getTTS();
        initiateDrawables();


        //initiate sugiliteVoiceRecognitionListener
        if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.ANDROID) {
            this.sugiliteVoiceRecognitionListener = new SugiliteAndroidAPIVoiceRecognitionListener(this, this, tts);
        } else if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.GOOGLE_CLOUD) {
            this.sugiliteVoiceRecognitionListener = new SugiliteGoogleCloudVoiceRecognitionListener(this, this, tts);
        }

        init();
    }

    private void init() {
        bindDialogManager(new PumiceDialogManager(context, true));
        //send the first prompt from the default intent handler in the dialog manager
        pumiceDialogManager.callSendPromptForTheIntentHandlerForCurrentIntentHandler();
    }

    /**
     * clear the user's input textbox on the GUI
     */
    public void clearUserTextBox(){
        if (userTextBox != null){
            userTextBox.setText("");
        }
    }

    /**
     * binds a dialog manager to the activity
     * @param pumiceDialogManager
     */
    private void bindDialogManager(PumiceDialogManager pumiceDialogManager){
        this.pumiceDialogManager = pumiceDialogManager;
        //bind to the view produced by the dialog manager
        ScrollView scrollView = (ScrollView) findViewById(R.id.pumice_dialog_scrollLayout);
        if(pumiceDialogManager.getPumiceDialogView() != null) {
            scrollView.addView(pumiceDialogManager.getPumiceDialogView());
        }
        pumiceDialogManager.setSpeakButtonForCallback(speakButton);
        pumiceDialogManager.setSugiliteVoiceRecognitionListener(sugiliteVoiceRecognitionListener);
    }

    public void pumiceSendButtonOnClick (View view) {
        // speak button
        //clear the text
        clearUserTextBox();

        if (isListening) {
            sugiliteVoiceRecognitionListener.stopListening();
        } else {
            sugiliteVoiceRecognitionListener.startListening();
        }
        if (isSpeaking) {
            sugiliteVoiceRecognitionListener.stopTTS();
        }

        /*
        if(userTextBox != null) {
            String userTextBoxContent = userTextBox.getText().toString();
            if(pumiceDialogManager != null){
                pumiceDialogManager.sendUserMessage(userTextBoxContent);
            }
        }
        */
    }
    /**
     * refresh the color and icon for the speak button
     * @param speakButton
     */
    protected void refreshSpeakButtonStyle(ImageButton speakButton){
        if(speakButton != null) {
            if (isSpeaking) {
                speakButton.setImageDrawable(speakingDrawable);
                speakButton.getBackground().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_SPEAKING_BUTTON_COLOR));
                speakButton.getDrawable().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_WHITE_COLOR));
                speakingDrawable.start();
                //dialog.getWindow().getDecorView().getBackground().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_SPEAKING_ON_BACKGROUND_COLOR));
            } else {
                if (isListening) {
                    speakButton.setImageDrawable(listeningDrawable);
                    speakButton.getBackground().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_ON_BUTTON_COLOR));
                    speakButton.getDrawable().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_WHITE_COLOR));
                } else {
                    speakButton.setImageDrawable(notListeningDrawable);
                    speakButton.getBackground().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_OFF_BUTTON_COLOR));
                    speakButton.getDrawable().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_DARK_GRAY_COLOR));
                }
            }
        }
    }

    void initiateDrawables(){
        //initiate the drawables for the icons on the speak button
        speakingDrawable = new AnimationDrawable();
        speakingDrawable.addFrame(this.getDrawable(R.mipmap.ic_speaker0), 500);
        speakingDrawable.addFrame(this.getDrawable(R.mipmap.ic_speaker1), 500);
        speakingDrawable.addFrame(this.getDrawable(R.mipmap.ic_speaker2), 500);
        speakingDrawable.addFrame(this.getDrawable(R.mipmap.ic_speaker3), 500);

        speakingDrawable.setOneShot(false);
        speakingDrawable.start();

        listeningDrawable = new AnimationDrawable();
        listeningDrawable.addFrame(this.getDrawable(R.mipmap.ic_tap_to_talk_0), 500);
        listeningDrawable.addFrame(this.getDrawable(R.mipmap.ic_tap_to_talk_1), 500);
        listeningDrawable.addFrame(this.getDrawable(R.mipmap.ic_tap_to_talk_2), 500);

        listeningDrawable.setOneShot(false);
        listeningDrawable.start();

        notListeningDrawable = this.getDrawable(R.mipmap.ic_tap_to_talk_0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pumice_dialog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.show_knowledge) {
            stopTTSandASR();
            SoviteKnowledgeManagementDialog soviteKnowledgeManagementDialog = new SoviteKnowledgeManagementDialog(context, pumiceDialogManager.getPumiceKnowledgeManager(), pumiceDialogManager, sugiliteData);
            soviteKnowledgeManagementDialog.show();
            /*
            pumiceDialogManager.sendAgentMessage("Below are the current knowledge...", true, false);
            pumiceDialogManager.sendAgentMessage(pumiceDialogManager.getPumiceKnowledgeManager().getKnowledgeInString(), false, false);
            */
            return true;
        }
        if (id == R.id.clear_knowledge) {
            stopTTSandASR();
            pumiceDialogManager.clearPumiceKnowledgeAndSaveToDao();
            pumiceDialogManager.sendAgentMessage("Current knowledge have been cleared", true, false);
            return true;
        }


        if (id == R.id.undo_pumice) {
            stopTTSandASR();
            pumiceDialogManager.revertToLastState();
            //pumiceDialogManager.sendAgentMessage("Reverted to the last state", true, false);
            return true;
        }


        if (id == R.id.start_over_pumice) {
            stopTTSandASR();
            pumiceDialogManager.startOverState();
            //pumiceDialogManager.sendAgentMessage("Reverted to the last state", true, false);
            return true;
        }

        if (id == R.id.dump_packet) {
            stopTTSandASR();
            SoviteStudyDumpGenerateDialog soviteStudyDumpGenerateDialog = new SoviteStudyDumpGenerateDialog(context, sugiliteData, pumiceDialogManager);
            soviteStudyDumpGenerateDialog.show();
            return true;
        }

        if (id == R.id.load_packet) {
            stopTTSandASR();
            //TODO: load a previously stored packet
            SoviteStudyDumpLoadDialog soviteStudyDumpLoadDialog = new SoviteStudyDumpLoadDialog(context, sugiliteData, pumiceDialogManager);
            soviteStudyDumpLoadDialog.show();
        }


        return super.onOptionsItemSelected(item);
    }

    public void stopTTSandASR() {
        if (isListening) {
            sugiliteVoiceRecognitionListener.stopListening();
            listeningEndedCallback();
        }
        if (tts != null && tts.isSpeaking()) {
            sugiliteVoiceRecognitionListener.stopTTS();
        }
    }



    @Override
    public void listeningStartedCallback() {
        isListening = true;
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void listeningEndedCallback() {
        isListening = false;
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void speakingStartedCallback() {
        isSpeaking = true;
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void speakingEndedCallback() {
        isSpeaking = false;
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void resultAvailableCallback(List<String> matches, boolean isFinal) {
        if(matches.size() > 0) {
            userTextBox.setText(matches.get(0));
            if (isFinal) {
                String userTextBoxContent = userTextBox.getText().toString();
                if (pumiceDialogManager != null) {
                    pumiceDialogManager.sendUserMessage(userTextBoxContent);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        if (tts != null) {
            tts.stop();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (sugiliteVoiceRecognitionListener != null) {
            sugiliteVoiceRecognitionListener.stopAllAndEndASRService();
        }
        super.onDestroy();
    }
}
