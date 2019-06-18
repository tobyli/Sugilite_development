package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogUtteranceFilter;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;

import static edu.cmu.hcii.sugilite.Const.MUL_ZEROS;
import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.RECORDING_DARK_GRAY_COLOR;
import static edu.cmu.hcii.sugilite.Const.RECORDING_OFF_BUTTON_COLOR;

/**
 * @author toby
 * @date 8/3/16
 * @time 6:14 PM
 */

/**
 * Dialog used for creating a new script -> asking the user to give the script a name and set the system into the recording state
 */
public class NewScriptDialog extends SugiliteDialogManager implements AbstractSugiliteDialog {
    private Context context;
    private SugiliteScriptDao sugiliteScriptDao;
    private ServiceStatusManager serviceStatusManager;
    private SharedPreferences sharedPreferences;
    private SugiliteData sugiliteData;
    private AlertDialog dialog;
    private TextToSpeech tts;
    private SugiliteDialogSimpleState askingForScriptNameState = new SugiliteDialogSimpleState("ASKING_FOR_SCRIPT_NAME", this);
    private SugiliteDialogSimpleState askingForScriptNameConfirmationState = new SugiliteDialogSimpleState("ASKING_FOR_SCRIPT_NAME_CONFIRMATION", this);
    private VerbalInstructionIconManager verbalInstructionIconManager;
    private View dialogView;

    private ImageButton mySpeakButton;
    private EditText scriptNameEditText;

    public NewScriptDialog(Context context, LayoutInflater layoutInflater, SugiliteScriptDao sugiliteScriptDao, ServiceStatusManager serviceStatusManager,
                           SharedPreferences sharedPreferences, SugiliteData sugiliteData, boolean isSystemAlert, final Dialog.OnClickListener positiveCallback, final Dialog.OnClickListener negativeCallback){
        super(context, sugiliteData.getTTS());
        this.tts = sugiliteData.getTTS();
        this.context = context;
        this.sugiliteScriptDao = sugiliteScriptDao;
        this.serviceStatusManager = serviceStatusManager;
        this.sharedPreferences = sharedPreferences;
        this.sugiliteData = sugiliteData;
        this.verbalInstructionIconManager = sugiliteData.verbalInstructionIconManager;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        sugiliteData.clearInstructionQueue();

        dialogView = layoutInflater.inflate(R.layout.dialog_new_script, null);
        scriptNameEditText = (EditText) dialogView.findViewById(R.id.edittext_instruction_content);
        scriptNameEditText.setText(sugiliteScriptDao.getNextAvailableDefaultName());

        //initiate the speak button
        mySpeakButton = (ImageButton) dialogView.findViewById(R.id.button_verbal_instruction_talk);
        mySpeakButton.getBackground().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_OFF_BUTTON_COLOR));
        mySpeakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // speak button
                if(tts != null) {
                    if (isListening() || tts.isSpeaking()) {
                        stopASRandTTS();
                    } else {
                        initDialogManager();
                    }
                }
            }
        });
        mySpeakButton.setImageDrawable(notListeningDrawable);
        mySpeakButton.getDrawable().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_DARK_GRAY_COLOR));
        setSpeakButton(mySpeakButton);


        builder.setMessage("Specify the name for your new script")
                .setView(dialogView)
                .setPositiveButton("Start Recording", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String scriptName = scriptNameEditText.getText().toString() + ".SugiliteScript";
                        PumiceDemonstrationUtil.initiateDemonstration(context, serviceStatusManager, sharedPreferences, scriptName, sugiliteData, null, sugiliteScriptDao, verbalInstructionIconManager);

                        //Toast.makeText(v.getContext(), "Changed script name to " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();
                        if(positiveCallback != null) {
                            positiveCallback.onClick(dialog, 0);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(negativeCallback != null)
                            negativeCallback.onClick(dialog, 0);
                        dialog.dismiss();
                    }
                })
                .setTitle("New Script");

        dialog = builder.create();

        if(dialog.getWindow() != null) {
            if (isSystemAlert) {
                dialog.getWindow().setType(OVERLAY_TYPE);
            }
        }


        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if(tts != null) {
                    if (isListening() || tts.isSpeaking()) {
                        stopASRandTTS();
                    }
                }
            }
        });


        scriptNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE)
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
        });
    }
    public void show(){
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        initDialogManager();
        refreshSpeakButtonStyle(mySpeakButton);
    }

    @Override
    public void initDialogManager() {
        askingForScriptNameState.setPrompt("What's the name for the script?");
        askingForScriptNameState.setNoASRResultState(askingForScriptNameState);
        askingForScriptNameState.addNextStateUtteranceFilter(askingForScriptNameConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));
        askingForScriptNameState.setOnInitiatedRunnable(new Runnable() {
            @Override
            public void run() {
                if(! scriptNameEditText.getText().toString().startsWith("Untitled")) {
                    scriptNameEditText.setText("");
                }
            }
        });
        //set on switched away runnable - the verbal instruction state should set the value for the text box
        askingForScriptNameState.setOnSwitchedAwayRunnable(new Runnable() {
            @Override
            public void run() {
                if (askingForScriptNameState.getASRResult() != null && (!askingForScriptNameState.getASRResult().isEmpty())) {
                    scriptNameEditText.setText(askingForScriptNameState.getASRResult().get(0));
                }
            }
        });


        askingForScriptNameConfirmationState.setPrompt("Is this script name correct?");
        askingForScriptNameConfirmationState.setNoASRResultState(askingForScriptNameState);
        askingForScriptNameConfirmationState.setUnmatchedState(askingForScriptNameState);
        askingForScriptNameConfirmationState.addNextStateUtteranceFilter(askingForScriptNameState, SugiliteDialogUtteranceFilter.getSimpleContainingFilter("no", "nah"));
        askingForScriptNameConfirmationState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("yes", "yeah"), new Runnable() {
            @Override
            public void run() {
                if(dialog != null || dialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    speak("Please start demonstrating the task.", null);
                    if(verbalInstructionIconManager != null){
                        verbalInstructionIconManager.turnOnCatOverlay();
                    }

                }
            }
        });

        //set current sate
        setCurrentState(askingForScriptNameState);
        initPrompt();
    }



}
