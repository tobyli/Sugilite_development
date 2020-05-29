package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.AutomatorUtil;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogUtteranceFilter;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;

import static android.content.Context.ACTIVITY_SERVICE;
import static edu.cmu.hcii.sugilite.Const.MUL_ZEROS;
import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.RECORDING_DARK_GRAY_COLOR;
import static edu.cmu.hcii.sugilite.Const.RECORDING_OFF_BUTTON_COLOR;
import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;

/**
 * @author toby
 * @date 7/15/16
 * @time 8:47 PM
 */


/**
 * dialog for running scripts with parameters
 */
public class VariableSetValueDialog extends SugiliteDialogManager implements AbstractSugiliteDialog {

    private Context context;
    private AlertDialog dialog;
    private Map<String, VariableValue> variableNameDefaultValueMap;
    private Map<String, Variable> variableNameVariableObjectMap;
    private Map<String, Set<VariableValue>> variableNameAlternativeValueMap;
    private Map<String, View> variableSelectionViewMap;
    private SharedPreferences sharedPreferences;
    private SugiliteStartingBlock startingBlock;
    private SugiliteData sugiliteData;
    private int state;
    private TextToSpeech tts;

    private PumiceDialogManager pumiceDialogManager;
    private Map<String, VariableValue> alreadyLoadedVariableMap;


    //adding speech for VLHCC DEMO
    private SugiliteDialogSimpleState askingForValueState = new SugiliteDialogSimpleState("ASKING_FOR_VARIABLE_VALUE", this, true);
    private SugiliteDialogSimpleState askingForValueConfirmationState = new SugiliteDialogSimpleState("ASKING_FOR_VARIABLE_VALUE_CONFIRMATION_VALUE", this, true);
    private EditText firstVariableEditText;
    private String firstVariableName;
    private String firstVariableDefaultValue;
    private ImageButton speakButton;
    private LinearLayout mainLayout;

    //whether this execution is for reconstructing the script
    private boolean isForReconstructing;

    public VariableSetValueDialog(final Context context, SugiliteData sugiliteData, SugiliteStartingBlock startingBlock, SharedPreferences sharedPreferences, int state, PumiceDialogManager pumiceDialogManager, boolean isForReconstructing) {
        //constructor for SugiliteDialogManager
        super(context, sugiliteData.getTTS());

        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.startingBlock = startingBlock;
        this.sugiliteData = sugiliteData;
        this.state = state;
        this.tts = sugiliteData.getTTS();
        this.pumiceDialogManager = pumiceDialogManager;
        this.isForReconstructing = isForReconstructing;
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_variable_set_value, null);
        LinearLayout mainLayout = (LinearLayout) dialogView.findViewById(R.id.layout_variable_set_value);
        this.mainLayout = mainLayout;

        variableNameDefaultValueMap = startingBlock.variableNameDefaultValueMap;
        variableNameAlternativeValueMap = startingBlock.variableNameAlternativeValueMap;
        variableNameVariableObjectMap = startingBlock.variableNameVariableObjectMap;
        variableSelectionViewMap = new HashMap<>();


        for (Map.Entry<String, VariableValue> entry : variableNameDefaultValueMap.entrySet()) {
            Variable variableObject = variableNameVariableObjectMap.get(entry.getKey());

            if (variableObject == null) {
                continue;
            }

            if (variableObject.getVariableType() == Variable.LOAD_RUNTIME) {
                // only ask the values for those that need to be loaded as a user input
                continue;
            }

            if (alreadyLoadedVariableMap != null && alreadyLoadedVariableMap.containsKey(entry.getKey())) {
                // skip variables that have been already loaded
                continue;
            }
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.setWeightSum(4);
            TextView variableName = new TextView(context);
            variableName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            variableName.setWidth(0);
            variableName.setText(entry.getKey());
            linearLayout.addView(variableName);

            //use a spinner if alternatives can be found
            if (variableNameAlternativeValueMap != null && variableNameAlternativeValueMap.containsKey(entry.getKey()) && variableNameAlternativeValueMap.get(entry.getKey()).size() >= 1) {
                //has alternative values stored
                Spinner alternativeValueSpinner = new Spinner(context);
                List<String> spinnerItemList = new ArrayList<>();
                if (entry.getValue().getVariableValue() instanceof String) {
                    spinnerItemList.add((String) entry.getValue().getVariableValue());
                }
                for (VariableValue alternative : variableNameAlternativeValueMap.get(entry.getKey())) {
                    if (alternative.getVariableValue().equals(entry.getValue().getVariableValue())) {
                        continue;
                    }
                    spinnerItemList.add(alternative.getVariableValue().toString());
                }
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, spinnerItemList);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                alternativeValueSpinner.setAdapter(spinnerAdapter);

                alternativeValueSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3));
                linearLayout.addView(alternativeValueSpinner);
                alternativeValueSpinner.setSelection(0);
                variableSelectionViewMap.put(entry.getKey(), alternativeValueSpinner);

            } else {
                //has no alternative values stored - show edit text to prompt the user to enter value
                EditText variableValueEditText = new EditText(context);
                variableValueEditText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 3));
                variableValueEditText.setWidth(0);
                /*
                this part save the state of the last variable setting
                if(variableNameVariableValueMap.containsKey(entry.getKey()) && variableNameVariableValueMap.get(entry.getKey()) instanceof StringVariable)
                    variableValue.setText(((StringVariable) variableNameVariableValueMap.get(entry.getKey())).getValue());
                */
                if (entry.getValue().getVariableValue() instanceof String) {
                    variableValueEditText.setText((String) entry.getValue().getVariableValue());
                }
                linearLayout.addView(variableValueEditText);
                variableSelectionViewMap.put(entry.getKey(), variableValueEditText);

                //set firstVariableName, firstVariableEditText and firstVariableDefaultValue for setting variables by speech
                if (firstVariableName == null && firstVariableEditText == null) {
                    firstVariableName = entry.getKey();
                    firstVariableDefaultValue = entry.getValue().getVariableValue().toString();
                    firstVariableEditText = variableValueEditText;
                }

            }
            mainLayout.addView(linearLayout);
        }

        builder.setView(dialogView)
                //.setTitle(Const.appNameUpperCase + " Parameter Settings")
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

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //stop ASR and TTS when the dialog is dismissed
                stopASRandTTS();
                onDestroy();
            }
        });
    }

    @Override
    public void show() {
        show(null, null);
    }

    public void show(SugiliteBlock afterExecutionOperation, Runnable afterExecutionRunnable) {
        initDialog();
        if (dialog != null) {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(OVERLAY_TYPE);
            }
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //check if all fields have been set
                    boolean allReady = true;

                    for (Map.Entry<String, View> entry : variableSelectionViewMap.entrySet()) {
                        if (entry.getValue() instanceof TextView) {
                            if (((TextView) entry.getValue()).getText().toString().length() < 1) {
                                allReady = false;
                                break;
                            }
                        }
                    }

                    if (allReady) {
                        //update all
                        for (Map.Entry<String, View> entry : variableSelectionViewMap.entrySet()) {
                            if (entry.getValue() instanceof TextView)
                                sugiliteData.variableNameVariableValueMap.put(entry.getKey(), new VariableValue<>(entry.getKey(), ((TextView) entry.getValue()).getText().toString()));
                            else if (entry.getValue() instanceof Spinner) {
                                sugiliteData.variableNameVariableValueMap.put(entry.getKey(), new VariableValue<>(entry.getKey(), ((Spinner) entry.getValue()).getSelectedItem().toString()));
                            }
                        }
                        executeScript(afterExecutionOperation, pumiceDialogManager, afterExecutionRunnable);
                        dialog.dismiss();
                    } else {
                        PumiceDemonstrationUtil.showSugiliteAlertDialog("Please complete all fields");
                    }
                }
            });
        }
        if (firstVariableEditText != null && firstVariableName != null) {
            initDialogManager();
            addSpeakButton();
            refreshSpeakButtonStyle(speakButton);
        }

    }

    private void addSpeakButton() {
        ImageButton speakButton = new ImageButton(context);
        if (firstVariableEditText.getParent() instanceof LinearLayout) {
            speakButton.setLayoutParams(new LinearLayout.LayoutParams(0, PumiceDemonstrationUtil.dpToPx(48), (float)0.75));
            firstVariableEditText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, (float)2.25));
            ((LinearLayout) firstVariableEditText.getParent()).addView(speakButton);
            this.speakButton = speakButton;
            speakButton.setElevation(PumiceDemonstrationUtil.dpToPx(2));
            speakButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
            speakButton.setCropToPadding(true);
            speakButton.getBackground().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_OFF_BUTTON_COLOR));
            speakButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // speak button
                    if (tts != null) {
                        if (isListening() || tts.isSpeaking()) {
                            stopASRandTTS();
                        } else {
                            initDialogManager();
                        }
                    }
                }
            });
            speakButton.setImageDrawable(notListeningDrawable);
            speakButton.getDrawable().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_DARK_GRAY_COLOR));
            setSpeakButton(speakButton);
        }
    }

    /**
     * @param afterExecutionOperation @nullable, this operation will be pushed into the queue after the execution
     *                                this is used for resume recording
     */
    public void executeScript(final SugiliteBlock afterExecutionOperation, PumiceDialogManager pumiceDialogManager, Runnable afterExecutionRunnable) {
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        //turn off the recording before executing
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.apply();
        //kill all the relevant packages
        for (String packageName : startingBlock.relevantPackages) {
            AutomatorUtil.killPackage(packageName, (ActivityManager) context.getSystemService(ACTIVITY_SERVICE));
        }

        sugiliteData.logUsageData(ScriptUsageLogManager.EXECUTE_SCRIPT, startingBlock.getScriptName());

        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(context, R.string.prepareing_script_execution_message);
        progressDialog.show();

        Runnable delayAndRunScript = new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                sugiliteData.runScript(startingBlock, afterExecutionOperation, afterExecutionRunnable, state, isForReconstructing);
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(delayAndRunScript, SCRIPT_DELAY);

        //load the pumice knowledge manager
        if (sugiliteData.pumiceDialogManager == null && pumiceDialogManager != null) {
            sugiliteData.pumiceDialogManager = pumiceDialogManager;
        } else {
            //TODO: need to be able to initiate a dialog manager here
        }


        System.out.println("start");
        //go to home screen for running the automation
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        if (!(context instanceof Activity)) {
            startMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(startMain);
    }

    public void setAlreadyLoadedVariableMap(Map<String, VariableValue> alreadyLoadedVariableMap) {
        this.alreadyLoadedVariableMap = alreadyLoadedVariableMap;
    }

    @Override
    public void initDialogManager() {
        //initiate the dialog manager
        askingForValueState.setPrompt(String.format("Do you want to use the parameter value \"%s\" for \"%s\", or you can say something else?", firstVariableDefaultValue, firstVariableName));

        //askingForValueState.setPrompt("What's the value for the parameter " + firstVariableName + "?");
        askingForValueState.setNoASRResultState(askingForValueState);
        askingForValueState.addNextStateUtteranceFilter(askingForValueConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));
        askingForValueState.setOnInitiatedRunnable(new Runnable() {
            @Override
            public void run() {
                if (!firstVariableEditText.getText().toString().equals(firstVariableDefaultValue)) {
                    SugiliteData.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            firstVariableEditText.setText("");
                        }
                    });
                }
            }
        });
        //set on switched away runnable - the verbal instruction state should set the value for the text box
        askingForValueState.setOnSwitchedAwayRunnable(new Runnable() {
            @Override
            public void run() {
                if (askingForValueState.getASRResult() != null && (!askingForValueState.getASRResult().isEmpty())) {
                    SugiliteData.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            firstVariableEditText.setText(capitalize(askingForValueState.getASRResult().get(0)));
                        }
                    });
                }
            }
        });

        askingForValueConfirmationState.setPrompt("Is this parameter value correct?");
        askingForValueConfirmationState.setNoASRResultState(askingForValueState);
        askingForValueConfirmationState.setUnmatchedState(askingForValueState);
        askingForValueConfirmationState.addNextStateUtteranceFilter(askingForValueState, SugiliteDialogUtteranceFilter.getSimpleContainingFilter("no", "nah"));
        askingForValueConfirmationState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("yes", "yeah"), new Runnable() {
            @Override
            public void run() {
                if (dialog != null || dialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            speak("Executing the task...", null);
                        }
                    }, 500);
                }
            }
        });

        //set current sate
        setCurrentState(askingForValueState);
        initPrompt();
    }

    private static String capitalize(String str) {
        return WordUtils.capitalize(str);
    }
}
