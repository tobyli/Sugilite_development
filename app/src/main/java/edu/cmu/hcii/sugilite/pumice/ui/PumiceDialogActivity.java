package edu.cmu.hcii.sugilite.pumice.ui;

import android.graphics.LightingColorFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;

import java.util.List;
import java.util.Locale;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_pumice_dialog);
        this.getSupportActionBar().setTitle("Sugilite Bot");
        this.speakButton = (ImageButton) findViewById(R.id.button3);
        this.userTextBox = (EditText) findViewById(R.id.pumice_user_textbox);


        //initiate tts
        this.tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                pumiceDialogManager.sendAgentMessage("Hi I'm Sugilite bot! How are you doing today?", true, true);
            }
        });
        initiateDrawables();
        tts.setLanguage(Locale.US);
        //initiate sugiliteVoiceRecognitionListener
        this.sugiliteVoiceRecognitionListener = new SugiliteVoiceRecognitionListener(this, this, tts);
        bindDialogManager(new PumiceDialogManager(this));
    }


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
