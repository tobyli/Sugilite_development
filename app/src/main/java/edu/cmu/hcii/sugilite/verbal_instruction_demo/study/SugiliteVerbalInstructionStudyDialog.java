package edu.cmu.hcii.sugilite.verbal_instruction_demo.study;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

/**
 * @author toby
 * @date 12/9/17
 * @time 11:11 PM
 */
public class SugiliteVerbalInstructionStudyDialog implements SugiliteVoiceInterface {
    private SerializableUISnapshot serializableUISnapshot;
    private SugiliteSerializableEntity serializableEntity;
    private SugiliteStudyHandler sugiliteStudyHandler;
    private Context context;
    private EditText instructionTextbox;
    private AlertDialog dialog;
    private ImageButton speakButton;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private LinearLayout mainLayout;
    private String path, fileName;
    public boolean isListening = false;
    public boolean isSpeaking = false;


    public SugiliteVerbalInstructionStudyDialog(SerializableUISnapshot serializableUISnapshot, SugiliteSerializableEntity serializableEntity, SugiliteStudyHandler sugiliteStudyHandler, Context context, LayoutInflater inflater, SugiliteVoiceRecognitionListener voiceRecognitionListener, String path, String fileName){
        this.serializableUISnapshot = serializableUISnapshot;
        this.serializableEntity = serializableEntity;
        this.sugiliteStudyHandler = sugiliteStudyHandler;
        this.context = context;
        this.sugiliteVoiceRecognitionListener = voiceRecognitionListener;
        this.path = path;
        this.fileName = fileName;
        voiceRecognitionListener.setSugiliteVoiceInterface(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = inflater.inflate(R.layout.dialog_send_server_query, null);
        mainLayout = (LinearLayout)dialogView.findViewById(R.id.layout_send_server_query);
        instructionTextbox = (EditText)dialogView.findViewById(R.id.edittext_instruction_content);
        speakButton = (ImageButton)dialogView.findViewById(R.id.button_verbal_instruction_talk);
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // speak button
                if(isListening) {
                    sugiliteVoiceRecognitionListener.stopListening();
                }

                else {
                    sugiliteVoiceRecognitionListener.startListening();
                }
            }
        });
        builder.setView(dialogView)
                .setTitle(Const.appNameUpperCase + " Study Data Collection")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        dialog = builder.create();
        dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(isListening) {
                    sugiliteVoiceRecognitionListener.stopListening();
                }
            }
        });
    }

    public void show(){
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        dialog.show();
        speakButton.performClick();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            //set the on click listener for the ok button
            @Override
            public void onClick(View v)
            {
                String userInput = instructionTextbox.getText().toString();
                SugiliteStudyPacket packet = new SugiliteStudyPacket(serializableUISnapshot, serializableEntity, userInput, SugiliteStudyPacket.TYPE_CLICK, "");
                sugiliteStudyHandler.savePacket(packet, path, fileName);
                dialog.dismiss();

            }
        });
    }



    @Override
    public void listeningStarted() {
        isListening = true;
    }

    @Override
    public void listeningEnded() {
        isListening = false;
    }

    @Override
    public void speakingStarted() {
        isSpeaking = true;
    }

    @Override
    public void speakingEnded() {
        isSpeaking = false;
    }

    @Override
    public void resultAvailable(List<String> matches) {
        if(matches.size() > 0) {
            instructionTextbox.setText(matches.get(0));
        }
    }






}
