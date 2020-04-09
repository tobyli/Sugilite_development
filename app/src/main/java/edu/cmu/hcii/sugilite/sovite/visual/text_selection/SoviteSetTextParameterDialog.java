package edu.cmu.hcii.sugilite.sovite.visual.text_selection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.LightingColorFilter;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.text.WordUtils;


import javax.annotation.Nullable;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteVariableUpdateCallback;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteVisualVariableOnClickDialog;

import static edu.cmu.hcii.sugilite.Const.MUL_ZEROS;
import static edu.cmu.hcii.sugilite.Const.RECORDING_DARK_GRAY_COLOR;
import static edu.cmu.hcii.sugilite.Const.RECORDING_OFF_BUTTON_COLOR;

/**
 * @author toby
 * @date 4/5/20
 * @time 3:29 PM
 */
public class SoviteSetTextParameterDialog extends SugiliteDialogManager {
    private Context context;
    private VariableValue currentlySelectedVariableValue;
    private String originalResultString;
    private String userUtterance;
    private SugiliteGetProcedureOperation getProcedureOperation;
    private SoviteVariableUpdateCallback soviteVariableUpdateCallback;
    private View originalScreenshotView;

    private LinearLayout mainLinearLayout;
    private SoviteTextSelectionView soviteTextSelectionView;
    private EditText variableValueEditText;

    private AlertDialog dialog;
    private SugiliteDialogSimpleState askingForValueState = new SugiliteDialogSimpleState("ASKING_FOR_VARIABLE_VALUE", this, true);

    private TextToSpeech tts;
    private boolean toUpdateEvenNoChange;


    public SoviteSetTextParameterDialog(Context context, SugiliteData sugiliteData, VariableValue currentlySelectedVariableValue, String userUtterance, @Nullable SugiliteGetProcedureOperation getProcedureOperation, @Nullable SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable View originalScreenshotView, boolean toUpdateEvenNoChange) {
        super(context, sugiliteData.getTTS());
        this.context = context;
        this.currentlySelectedVariableValue = currentlySelectedVariableValue;
        this.originalResultString = currentlySelectedVariableValue.getVariableValue().toString();
        this.userUtterance = userUtterance;
        this.tts = sugiliteData.getTTS();

        this.getProcedureOperation = getProcedureOperation;
        this.soviteVariableUpdateCallback = soviteVariableUpdateCallback;
        this.originalScreenshotView = originalScreenshotView;
        this.toUpdateEvenNoChange = toUpdateEvenNoChange;
    }

    public void show() {
        initDialog();
        if (dialog != null) {
            dialog.show();
        }
    }

    private void initDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        mainLinearLayout = new LinearLayout(context);
        mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mainLinearLayout.setPadding(25, 35, 25, 25);

        soviteTextSelectionView = new SoviteTextSelectionView(context);
        variableValueEditText = new EditText(context);
        variableValueEditText.setText(originalResultString);

        //create the horizontal linearlayout for the sovite text selection
        LinearLayout soviteTextSelectionRowLinearLayout = new LinearLayout(context);
        soviteTextSelectionRowLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        soviteTextSelectionRowLinearLayout.setWeightSum(4);
        soviteTextSelectionView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 4));
        soviteTextSelectionRowLinearLayout.addView(soviteTextSelectionView);

        //create the horizontal linearlayout for the Edittext
        LinearLayout editTextRowLinearLayout = new LinearLayout(context);
        editTextRowLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        editTextRowLinearLayout.setWeightSum(4);
        TextView parametername = new TextView(context);
        parametername.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        parametername.setWidth(1);
        parametername.setText(currentlySelectedVariableValue.getVariableName());
        editTextRowLinearLayout.addView(parametername);

        variableValueEditText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, (float)2.35));
        editTextRowLinearLayout.addView(variableValueEditText);
        variableValueEditText.setTextIsSelectable(false);

        ImageButton speakButton = new ImageButton(context);
        speakButton.setLayoutParams(new LinearLayout.LayoutParams(0, PumiceDemonstrationUtil.dpToPx(48), (float)0.65));
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
        editTextRowLinearLayout.addView(speakButton);
        setSpeakButton(speakButton);

        mainLinearLayout.addView(soviteTextSelectionRowLinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainLinearLayout.addView(editTextRowLinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        if (userUtterance.contains(currentlySelectedVariableValue.getVariableValue().toString())) {
            soviteTextSelectionView.setUserUtterance(userUtterance, userUtterance.indexOf(originalResultString), userUtterance.indexOf(originalResultString) + originalResultString.length());
        } else {
            soviteTextSelectionView.setUserUtterance(userUtterance, 0, 0);
        }
        soviteTextSelectionView.setSoviteSelectionChangedListener(new SoviteTextSelectionView.SoviteSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int startIndex, int endIndex) {
                variableValueEditText.setText(soviteTextSelectionView.getText().toString().substring(startIndex, endIndex));
            }
        });

        builder.setTitle("Set the Parameter Value");
        builder.setView(mainLinearLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //update the parameter value
                String newVariableStringValue = variableValueEditText.getText().toString();
                SoviteVisualVariableOnClickDialog.updateVariableValue(getProcedureOperation, currentlySelectedVariableValue.getVariableName(), newVariableStringValue, currentlySelectedVariableValue.getVariableValue().toString(), soviteVariableUpdateCallback, originalScreenshotView, toUpdateEvenNoChange);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
                dialog.dismiss();
            }
        });

        dialog = builder.create();
    }

    @Override
    public void initDialogManager() {
        askingForValueState.setPrompt("");
        askingForValueState.setNoASRResultState(askingForValueState);
        //askingForValueState.addNextStateUtteranceFilter(askingForValueState, SugiliteDialogUtteranceFilter.getConstantFilter(true));
        askingForValueState.setOnInitiatedRunnable(new Runnable() {
            @Override
            public void run() {
                //do nothing on initiation
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
                            variableValueEditText.setText(WordUtils.capitalize(askingForValueState.getASRResult().get(0)));
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    refreshSpeakButtonStyle(speakButton);
                                }
                            }, 100);
                        }
                    });
                }
            }
        });
        setCurrentState(askingForValueState);
        initPrompt();
    }
}
