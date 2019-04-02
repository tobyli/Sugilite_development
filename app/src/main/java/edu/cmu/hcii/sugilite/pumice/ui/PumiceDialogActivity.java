package edu.cmu.hcii.sugilite.pumice.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;
import edu.cmu.hcii.sugilite.study.StudyConst;
import edu.cmu.hcii.sugilite.ui.SettingsActivity;
import edu.cmu.hcii.sugilite.ui.dialog.AddTriggerDialog;
import edu.cmu.hcii.sugilite.ui.main.FragmentScriptListTab;
import edu.cmu.hcii.sugilite.ui.main.FragmentTriggerListTab;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

import static edu.cmu.hcii.sugilite.Const.MUL_ZEROS;
import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_pumice_dialog);
        this.getSupportActionBar().setTitle("PUMICE");
        this.speakButton = (ImageButton) findViewById(R.id.button3);
        this.userTextBox = (EditText) findViewById(R.id.pumice_user_textbox);


        //initiate tts
        this.tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                pumiceDialogManager.sendAgentMessage("Hi I'm Sugilite bot! How can I help you?", true, true);
            }
        });
        initiateDrawables();
        tts.setLanguage(Locale.US);
        //initiate sugiliteVoiceRecognitionListener
        this.sugiliteVoiceRecognitionListener = new SugiliteVoiceRecognitionListener(this, this, tts);
        bindDialogManager(new PumiceDialogManager(this));
    }

    /**
     * binds a dialog manager to the activity
     * @param pumiceDialogManager
     */
    private void bindDialogManager(PumiceDialogManager pumiceDialogManager){
        this.pumiceDialogManager = pumiceDialogManager;
        //bind to the view produced by the dialog manager
        ScrollView scrollView = (ScrollView) findViewById(R.id.pumice_dialog_scrollLayout);
        System.out.println("HERE");
        System.out.println(pumiceDialogManager.getPumiceDialogView());
        if(pumiceDialogManager.getPumiceDialogView() != null) {
            scrollView.addView(pumiceDialogManager.getPumiceDialogView());
        }
        pumiceDialogManager.setSpeakButtonForCallback(speakButton);
        pumiceDialogManager.setSugiliteVoiceRecognitionListener(sugiliteVoiceRecognitionListener);
    }

    public void pumiceSendButtonOnClick (View view) {
        // speak button
        //clear the text
        if (userTextBox != null){
            userTextBox.setText("");
        }
        if (isListening) {
            sugiliteVoiceRecognitionListener.stopListening();
        }

        else {
            sugiliteVoiceRecognitionListener.startListening();
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
            pumiceDialogManager.sendAgentMessage("Below are the current knowledge...", true, false);
            pumiceDialogManager.sendAgentMessage(pumiceDialogManager.getPumiceKnowledgeManager().getKnowledgeInString(), false, false);
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

        /*
        if (id == R.id.start_over_pumice) {
            stopTTSandASR();
            pumiceDialogManager.startOverState();
            //pumiceDialogManager.sendAgentMessage("Reverted to the last state", true, false);
            return true;
        }
        */

        return super.onOptionsItemSelected(item);
    }

    public void stopTTSandASR() {
        if (isListening) {
            sugiliteVoiceRecognitionListener.stopListening();
            listeningEnded();
        }
        if (tts != null && tts.isSpeaking()) {
            sugiliteVoiceRecognitionListener.stopTTS();
        }
    }



    @Override
    public void listeningStarted() {
        isListening = true;
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void listeningEnded() {
        isListening = false;
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void speakingStarted() {
        isSpeaking = true;
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void speakingEnded() {
        isSpeaking = false;
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void resultAvailable(List<String> matches) {
        if(matches.size() > 0) {
            userTextBox.setText(matches.get(0));
            String userTextBoxContent = userTextBox.getText().toString();
            if(pumiceDialogManager != null){
                pumiceDialogManager.sendUserMessage(userTextBoxContent);
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
        if(tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }
}
