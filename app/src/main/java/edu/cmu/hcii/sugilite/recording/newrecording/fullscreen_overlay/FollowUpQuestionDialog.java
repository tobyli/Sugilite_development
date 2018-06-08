package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.graphics.PixelFormat;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import com.google.gson.Gson;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.OntologyQueryUtils;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogUtteranceFilter;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerQuery;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResults;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;

import static edu.cmu.hcii.sugilite.Const.MUL_ZEROS;
import static edu.cmu.hcii.sugilite.Const.RECORDING_DARK_GRAY_COLOR;
import static edu.cmu.hcii.sugilite.Const.RECORDING_OFF_BUTTON_COLOR;
import static edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay.RecordingAmbiguousPopupDialog.CHECK_FOR_GROUNDING_MATCH;

/**
 * @author toby
 * @date 2/23/18
 * @time 2:55 PM
 */

/**
 * dialog class used for maintaining follow-up quersions with users in case of ambiguities after the user has given a verbal instruction
 */
public class FollowUpQuestionDialog extends SugiliteDialogManager implements SugiliteVerbalInstructionHTTPQueryInterface {
    private ImageButton speakButton;
    private View dialogView;
    private TextView currentQueryTextView;
    private Runnable clickRunnable;
    private LayoutInflater layoutInflater;
    private EditText verbalInstructionEditText;
    private UISnapshot uiSnapshot;
    private SerializableUISnapshot serializableUISnapshot;
    private SugiliteVerbalInstructionHTTPQueryManager sugiliteVerbalInstructionHTTPQueryManager;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private SugiliteEntity<Node> actualClickedNode;
    private List<Node> matchedNodes;
    private List<Node> previousMatchedNodes;
    private SugiliteAvailableFeaturePack featurePack;
    private List<Pair<SerializableOntologyQuery, Double>> queryScoreList;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private SugiliteData sugiliteData;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;
    private SugiliteFullScreenOverlayFactory sugiliteFullScreenOverlayFactory;
    private DisplayMetrics displayMetrics;
    private WindowManager windowManager;
    private NavigationBarUtil navigationBarUtil;
    private VerbalInstructionIconManager verbalInstructionIconManager;
    private View previewOverlay;
    private FollowUpQuestionDialog followUpQuestionDialog;
    private TextView textPromptTextView;

    private int errorCount = 0;


    private Handler handler;

    private SugiliteDialogSimpleState askingForVerbalInstructionFollowUpState = new SugiliteDialogSimpleState("ASKING_FOR_VERBAL_INSTRUCTION", this);
    private SugiliteDialogSimpleState askingForInstructionConfirmationState = new SugiliteDialogSimpleState("ASKING_FOR_INSTRUCTION_CONFIRMATION", this);
    private SugiliteDialogSimpleState emptyResultState = new SugiliteDialogSimpleState("EMPTY_RESULT_STATE", this);
    private SugiliteDialogSimpleState resultWontMatchState = new SugiliteDialogSimpleState("RESULT_WONT_MATCH_STATE", this);

    //variable for saving the animation offset when the dialog is hidden
    private float prevXOffset = 0, prevYOffset = 0;

    private boolean flagDismissedByCollapsing = false;

    //number of matched nodes for current query
    private int numberOfMatchedNodes = -1;

    private OntologyQuery previousQuery;
    private OntologyQuery currentQuery;

    private AlertDialog dialog;
    private Dialog progressDialog;

    public FollowUpQuestionDialog(Context context, TextToSpeech tts, OntologyQuery initialQuery, UISnapshot uiSnapshot, SugiliteEntity<Node> actualClickedNode, List<Node> matchedNodes, SugiliteAvailableFeaturePack featurePack, List<Pair<SerializableOntologyQuery, Double>> queryScoreList, SugiliteBlockBuildingHelper blockBuildingHelper, LayoutInflater layoutInflater, Runnable clickRunnable, SugiliteData sugiliteData, SharedPreferences sharedPreferences, int errorCount){
        super(context, tts);
        this.previousQuery = null;
        this.currentQuery = initialQuery;
        this.clickRunnable = clickRunnable;
        this.layoutInflater = layoutInflater;
        this.uiSnapshot = uiSnapshot;
        this.serializableUISnapshot = new SerializableUISnapshot(uiSnapshot);
        this.sugiliteVerbalInstructionHTTPQueryManager = new SugiliteVerbalInstructionHTTPQueryManager(this, sharedPreferences);
        this.sharedPreferences = sharedPreferences;
        this.actualClickedNode = actualClickedNode;
        this.featurePack = featurePack;
        this.queryScoreList = queryScoreList;
        this.blockBuildingHelper = blockBuildingHelper;
        this.sugiliteData = sugiliteData;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator(context);
        this.gson = new Gson();
        this.sugiliteFullScreenOverlayFactory = new SugiliteFullScreenOverlayFactory(context);
        this.navigationBarUtil = new NavigationBarUtil();
        this.handler = new Handler();
        this.followUpQuestionDialog = this;
        this.errorCount = errorCount;

        if(matchedNodes != null) {
            this.matchedNodes = matchedNodes;
        } else {
            this.matchedNodes = new ArrayList<>();
        }

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        if (context instanceof SugiliteAccessibilityService) {
            verbalInstructionIconManager = ((SugiliteAccessibilityService) context).getVerbalInstructionIconManager();
            if (verbalInstructionIconManager != null){
                verbalInstructionIconManager.registerFollowUpQuestionDialog(this);
            }
        }

        //build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        dialogView = layoutInflater.inflate(R.layout.dialog_followup_popup_spoken, null);
        currentQueryTextView = (TextView) dialogView.findViewById(R.id.text_current_query_content);
        textPromptTextView = (TextView) dialogView.findViewById(R.id.text_prompt);
        verbalInstructionEditText = (EditText) dialogView.findViewById(R.id.edittext_instruction_content);

        refreshPreviewTextView();

        //initiate the speak button
        speakButton = (ImageButton) dialogView.findViewById(R.id.button_verbal_instruction_talk);
        speakButton.getBackground().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_OFF_BUTTON_COLOR));
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // speak button
                if (isListening() || tts.isSpeaking()) {
                    stopASRandTTS();
                } else {
                    initDialogManager();
                }
            }
        });
        speakButton.setImageDrawable(notListeningDrawable);
        speakButton.getDrawable().setColorFilter(new LightingColorFilter(MUL_ZEROS, RECORDING_DARK_GRAY_COLOR));

        builder.setView(dialogView);

        //set the buttons
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendInstructionButtonOnClick();

            }
        }).setNeutralButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                backButtonOnClick();
            }
        }).setNegativeButton("View", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
            }
        });

        dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                stopASRandTTS();
                if(!flagDismissedByCollapsing){
                    if(handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    }
                    if(verbalInstructionIconManager != null){
                        verbalInstructionIconManager.registerFollowUpQuestionDialog(null);
                    }
                } else {
                    if(verbalInstructionIconManager != null){
                        verbalInstructionIconManager.registerFollowUpQuestionDialog(followUpQuestionDialog);
                    }
                }
                flagDismissedByCollapsing = false;
            }
        });
    }

    public void show(){
        if(dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        dialog.show();

        //clear the handler
        if(handler != null){
            handler.removeCallbacksAndMessages(null);
        }

        dialog.getButton(Dialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewButtonOnClick();
            }
        });

        //initiate the dialog manager when the dialog is shown
        initDialogManager();
        refreshSpeakButtonStyle(speakButton);
    }

    private void sendInstructionButtonOnClick() {
        //send the instruction out to the server for semantic parsing
        if (verbalInstructionEditText != null) {
            String userInput = verbalInstructionEditText.getText().toString();
            //send out the ASR result
            String className = null;
            if(actualClickedNode != null && actualClickedNode.getEntityValue() != null){
                className = actualClickedNode.getEntityValue().getClassName();
            }

            VerbalInstructionServerQuery query = new VerbalInstructionServerQuery(userInput, serializableUISnapshot.triplesToStringWithFilter(SugiliteRelation.HAS_PARENT, SugiliteRelation.HAS_CHILD, SugiliteRelation.HAS_CONTENT_DESCRIPTION), className);
            //send the query
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        sugiliteVerbalInstructionHTTPQueryManager.sendQueryRequest(query);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.start();

            //show loading popup
            dialog.dismiss();
            showProgressDialog();
        }
    }

    /**
     * for viewing the clicked and matched nodes
     */
    private void viewButtonOnClick(){
        //display the highlight overlay
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        int real_y = 0;
        int statusBarHeight = navigationBarUtil.getStatusBarHeight(context);
        real_y -= statusBarHeight;

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = real_y;
        layoutParams.width = displayMetrics.widthPixels;
        layoutParams.height = displayMetrics.heightPixels;

        previewOverlay = sugiliteFullScreenOverlayFactory.getOverlayWithHighlightedBoundingBoxes(displayMetrics, actualClickedNode.getEntityValue(), matchedNodes);

        collapse();

        windowManager.addView(previewOverlay, layoutParams);

        //refresh the cat icon so it stays on top of the overlay
        verbalInstructionIconManager.removeStatusIcon();
        verbalInstructionIconManager.addStatusIcon();

        //set the exit animation
    }

    private void backButtonOnClick(){
        if(previousQuery != null) {
            //go back to the followup question dialog with the previous query
            FollowUpQuestionDialog followUpQuestionDialog = new FollowUpQuestionDialog(context, tts, previousQuery, uiSnapshot, actualClickedNode, previousMatchedNodes, featurePack, queryScoreList, blockBuildingHelper, layoutInflater, clickRunnable, sugiliteData, sharedPreferences, 0);
            dialog.dismiss();
            if(previousMatchedNodes != null && previousMatchedNodes.size() > 1) {
                followUpQuestionDialog.setNumberOfMatchedNodes(previousMatchedNodes.size());
            } else {
                followUpQuestionDialog.setNumberOfMatchedNodes(-1);
            }
            followUpQuestionDialog.show();
        }
        else{
            //go back to the original ambiguous popup dialog
            dialog.dismiss();
            RecordingAmbiguousPopupDialog ambiguousPopupDialog = new RecordingAmbiguousPopupDialog(context, queryScoreList, featurePack, blockBuildingHelper, layoutInflater, clickRunnable, uiSnapshot, actualClickedNode, sugiliteData, sharedPreferences, tts, 0);
            ambiguousPopupDialog.show();
        }
    }

    private void showProgressDialog() {
        progressDialog = new AlertDialog.Builder(context).setMessage("Processing the query ...").create();
        if(progressDialog.getWindow() != null) {
            progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    /**
     * refresh the current query text view so that it reflects currentQuery
     */
    private void refreshPreviewTextView(){
        //TODO: show the source code temporarily
        String html = ontologyDescriptionGenerator.getDescriptionForOntologyQuery(new SerializableOntologyQuery(currentQuery));
        Toast.makeText(context, currentQuery.toString(), Toast.LENGTH_SHORT).show();
        //String html = SugiliteBlockBuildingHelper.stripSerializableOntologyQuery(new SerializableOntologyQuery(currentQuery)).toString();
        currentQueryTextView.setText(Html.fromHtml(html));
    }

    /**
     * called externally to update numberOfMatchedNodes
     * @param numberOfMatchedNodes
     */
    public void setNumberOfMatchedNodes(int numberOfMatchedNodes) {
        this.numberOfMatchedNodes = numberOfMatchedNodes;
        if(numberOfMatchedNodes > 0) {
            askingForVerbalInstructionFollowUpState.setPrompt(context.getString(R.string.disambiguation_followup_prompt, numberOfMatchedNodes));
            textPromptTextView.setText(context.getString(R.string.disambiguation_followup_prompt, numberOfMatchedNodes));
        }
        else{
            //when the numberOfMatchedNodes < 0 (i.e. the dialog was generated through the back button)
            askingForVerbalInstructionFollowUpState.setPrompt(context.getString(R.string.disambiguation_followup_no_count_prompt));
            textPromptTextView.setText(context.getString(R.string.disambiguation_followup_no_count_prompt));
        }
    }

    @Override
    public void initDialogManager() {
        //initiate the dialog states
        //set the prompt
        emptyResultState.setPrompt(context.getString(R.string.disambiguation_error));
        resultWontMatchState.setPrompt(context.getString(R.string.disambiguation_result_wont_match));

        if(numberOfMatchedNodes > 0) {
            askingForVerbalInstructionFollowUpState.setPrompt(context.getString(R.string.disambiguation_followup_prompt, numberOfMatchedNodes));
        }
        else{
            //when the numberOfMatchedNodes < 0 (i.e. the dialog was generated through the back button)
            askingForVerbalInstructionFollowUpState.setPrompt(context.getString(R.string.disambiguation_followup_no_count_prompt));
        }

        //set on switched away runnable - the verbal instruction state should set the value for the text box
        askingForVerbalInstructionFollowUpState.setOnSwitchedAwayRunnable(new Runnable() {
            @Override
            public void run() {
                if (askingForVerbalInstructionFollowUpState.getASRResult() != null && (!askingForVerbalInstructionFollowUpState.getASRResult().isEmpty())) {
                    verbalInstructionEditText.setText(askingForVerbalInstructionFollowUpState.getASRResult().get(0));
                }
            }
        });
        emptyResultState.setOnSwitchedAwayRunnable(new Runnable() {
            @Override
            public void run() {
                if (emptyResultState.getASRResult() != null && (!emptyResultState.getASRResult().isEmpty())) {
                    verbalInstructionEditText.setText(emptyResultState.getASRResult().get(0));
                }
            }
        });

        resultWontMatchState.setOnSwitchedAwayRunnable(new Runnable() {
            @Override
            public void run() {
                if (resultWontMatchState.getASRResult() != null && (!resultWontMatchState.getASRResult().isEmpty())) {
                    verbalInstructionEditText.setText(resultWontMatchState.getASRResult().get(0));
                }
            }
        });
        askingForVerbalInstructionFollowUpState.setOnInitiatedRunnable(new Runnable() {
            @Override
            public void run() {
                //clear the edittext
                verbalInstructionEditText.setText("");
            }
        });

        resultWontMatchState.setOnInitiatedRunnable(new Runnable() {
            @Override
            public void run() {
                //clear the edittext
                verbalInstructionEditText.setText("");

                errorCount ++;

                if(errorCount > 1){
                    //set prompt to the error one
                    resultWontMatchState.setPrompt(context.getString(R.string.disambiguation_result_wont_match_repeated));
                    //textPrompt.setText(Html.fromHtml(boldify(context.getString(R.string.disambiguation_result_wont_match_repeated))));
                }

                else{
                    resultWontMatchState.setPrompt(context.getString(R.string.disambiguation_result_wont_match));
                    //textPrompt.setText(Html.fromHtml(boldify(context.getString(R.string.disambiguation_result_wont_match))));
                }

            }
        });

        emptyResultState.setOnInitiatedRunnable(new Runnable() {
            @Override
            public void run() {
                //clear the edittext
                verbalInstructionEditText.setText("");

                errorCount ++;

                if(errorCount > 1){
                    //set prompt to the error one
                    emptyResultState.setPrompt(context.getString(R.string.disambiguation_error_repeated));
                    //textPrompt.setText(Html.fromHtml(boldify(context.getString(R.string.disambiguation_error_repeated))));
                }

                else {
                    emptyResultState.setPrompt(context.getString(R.string.disambiguation_error));
                    //textPrompt.setText(Html.fromHtml(boldify(context.getString(R.string.disambiguation_error))));
                }
            }
        });

        //set on initiate runnable - the instruction confirmation state should use the content in the text box as the prompt
        askingForInstructionConfirmationState.setOnInitiatedRunnable(new Runnable() {
            @Override
            public void run() {
                //askingForInstructionConfirmationState.setPrompt(context.getString(R.string.disambiguation_confirm, verbalInstructionEditText.getText()));
                askingForInstructionConfirmationState.setPrompt(context.getString(R.string.disambiguation_confirm));
            }
        });

        //link the states
        askingForVerbalInstructionFollowUpState.setNoASRResultState(askingForVerbalInstructionFollowUpState);
        askingForVerbalInstructionFollowUpState.setUnmatchedState(askingForVerbalInstructionFollowUpState);
        askingForVerbalInstructionFollowUpState.addNextStateUtteranceFilter(askingForInstructionConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));

        emptyResultState.setNoASRResultState(askingForVerbalInstructionFollowUpState);
        emptyResultState.setUnmatchedState(askingForVerbalInstructionFollowUpState);
        emptyResultState.addNextStateUtteranceFilter(askingForInstructionConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));

        resultWontMatchState.setNoASRResultState(askingForVerbalInstructionFollowUpState);
        resultWontMatchState.setUnmatchedState(askingForVerbalInstructionFollowUpState);
        resultWontMatchState.addNextStateUtteranceFilter(askingForInstructionConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));

        askingForInstructionConfirmationState.setNoASRResultState(askingForInstructionConfirmationState);
        askingForInstructionConfirmationState.setUnmatchedState(askingForInstructionConfirmationState);
        askingForInstructionConfirmationState.addNextStateUtteranceFilter(askingForVerbalInstructionFollowUpState, SugiliteDialogUtteranceFilter.getSimpleContainingFilter("no", "nah"));

        //set exit runnables
        askingForVerbalInstructionFollowUpState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("skip"), new Runnable() {
            @Override
            public void run() {
                viewButtonOnClick();
            }
        });
        askingForVerbalInstructionFollowUpState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("cancel"), new Runnable() {
            @Override
            public void run() {
                dialog.cancel();
            }
        });
        askingForInstructionConfirmationState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("yes", "yeah"), new Runnable() {
            @Override
            public void run() {
                sendInstructionButtonOnClick();
            }
        });


        //set current sate
        setCurrentState(askingForVerbalInstructionFollowUpState);
        initPrompt();
    }

    @Override
    public void listeningStarted() {
        super.listeningStarted();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void listeningEnded() {
        super.listeningEnded();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void speakingStarted() {
        super.speakingStarted();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void speakingEnded() {
        super.speakingEnded();
        refreshSpeakButtonStyle(speakButton);
    }

    public boolean isShowing(){
        return dialog != null && dialog.isShowing();
    }

    public void uncollapse(){
        if(dialog != null && (!dialog.isShowing())) {
            //disable the prompt after uncollapsing
            askingForVerbalInstructionFollowUpState.setPrompt("");
            //clear the handler
            if(handler != null){
                handler.removeCallbacksAndMessages(null);
            }

            dialog.show();
            if (previewOverlay != null) {
                try {
                    windowManager.removeViewImmediate(previewOverlay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //show the on dialog
            if(dialog.getWindow() != null) {
                final View decorView = dialog
                        .getWindow()
                        .getDecorView();
                ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(decorView,
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 0.0f, 1.0f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.0f, 1.0f),
                        PropertyValuesHolder.ofFloat(View.TRANSLATION_X, prevXOffset, 0),
                        PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, prevYOffset, 0),
                        PropertyValuesHolder.ofFloat(View.ALPHA, 0.5f, 1.0f));

                scaleDown.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setCurrentState(askingForVerbalInstructionFollowUpState);
                        initPrompt();
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                scaleDown.setDuration(1000);
                scaleDown.start();
            }
        }
    }

    private void collapse(){
        if(dialog.getWindow() != null) {
            final View decorView = dialog
                    .getWindow()
                    .getDecorView();

            int icon_x = displayMetrics.widthPixels;
            int icon_y = 400;
            int center_x = displayMetrics.widthPixels / 2;
            int center_y = displayMetrics.heightPixels / 2;
            if (verbalInstructionIconManager != null) {
                icon_x = verbalInstructionIconManager.getCurrent_x();
                icon_y = verbalInstructionIconManager.getCurrent_y();
            }

            float x_offset = icon_x - center_x;
            float y_offset = icon_y - center_y;
            prevXOffset = x_offset;
            prevYOffset = y_offset;

            //show the off dialog
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(decorView,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 0.0f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 0.0f),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0, x_offset),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0, y_offset),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.5f));
            scaleDown.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    flagDismissedByCollapsing = true;
                    dialog.dismiss();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            uncollapse();
                        }
                    }, 6000);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    stopASRandTTS();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            scaleDown.setDuration(1000);
            scaleDown.start();
        }
    }

    @Override
    public void resultReceived(int responseCode, String result) {
        //dismiss the progress dialog
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        //update currentQuery based on the result received
        VerbalInstructionServerResults results = gson.fromJson(result, VerbalInstructionServerResults.class);

        if (results.getQueries() == null || results.getQueries().isEmpty()) {
            //error in parsing the server reply
            Toast.makeText(context, String.valueOf(responseCode) + ": Can't parse the verbal instruction", Toast.LENGTH_SHORT).show();
            dialog.show();
            setCurrentState(emptyResultState);
            initPrompt();
            return;
        }

        //print for debug purpose
        for (VerbalInstructionServerResults.VerbalInstructionResult verbalInstructionResult : results.getQueries()) {
            System.out.println(gson.toJson(verbalInstructionResult));
        }

        //find matches
        List<Map.Entry<OntologyQuery, List<Node>>> matchingQueriesMatchedNodesList = new ArrayList<>();

        for (VerbalInstructionServerResults.VerbalInstructionResult verbalInstructionResult : results.getQueries()) {
            boolean matched = false;
            List<Node> filteredNodes = new ArrayList<>();
            //Map<Node, Integer> filteredNodeNodeIdMap = new HashMap<>();

            //construct the query, run the query, and compare the result against the actually clicked on node

            String queryFormula = verbalInstructionResult.getFormula();
            OntologyQuery resolvedQuery = OntologyQueryUtils.getQueryWithClassAndPackageConstraints(OntologyQuery.deserialize(queryFormula), actualClickedNode.getEntityValue(), false, true, true);
            OntologyQuery combinedQuery = OntologyQueryUtils.combineTwoQueries(currentQuery, resolvedQuery);
            OntologyQuery queryClone = OntologyQuery.deserialize(combinedQuery.toString());

            //TODO: fix the bug in query.executeOn -- it should not change the query
            Set<SugiliteEntity> queryResults =  queryClone.executeOn(uiSnapshot);

            for(SugiliteEntity entity : queryResults){
                if(entity.getType().equals(Node.class)){
                    Node node = (Node) entity.getEntityValue();
                    if (node.getClickable()) {
                        filteredNodes.add(node);
                        //filteredNodeNodeIdMap.put(node, entity.getEntityId());
                    }
                    if (OntologyQueryUtils.isSameNode(actualClickedNode.getEntityValue(), node)) {
                        matched = true;
                    }
                    if(!CHECK_FOR_GROUNDING_MATCH){
                        matched = true;
                    }
                }
            }

            if (filteredNodes.size() > 0 && matched) {
                //matched, add the result to the list
                matchingQueriesMatchedNodesList.add(new AbstractMap.SimpleEntry<>(combinedQuery, filteredNodes));
            }
        }
        if(false) {
            //don't sort the results -- keep the original order from the parser
            matchingQueriesMatchedNodesList.sort(new Comparator<Map.Entry<OntologyQuery, List<Node>>>() {
                @Override
                public int compare(Map.Entry<OntologyQuery, List<Node>> o1, Map.Entry<OntologyQuery, List<Node>> o2) {
                    return o1.getValue().size() - o2.getValue().size();
                /*
                if(o1.getValue().size() != o2.getValue().size()){
                    return o1.getValue().size() - o2.getValue().size();
                }
                else{
                    return o1.getKey().toString().length() - o2.getKey().toString().length();
                }*/
                }
            });
        }

        //sort the list by the size of matched node and length, and see if the top result has filteredNodes.size() = 1
        if (matchingQueriesMatchedNodesList.size() > 0) {
            OntologyQuery query = matchingQueriesMatchedNodesList.get(0).getKey();

            if(matchingQueriesMatchedNodesList.get(0).getValue().size() > 1) {
                //need to prompt for further generalization
                Toast.makeText(context, "Matched " + matchingQueriesMatchedNodesList.get(0).getValue().size() + " Nodes, Need further disambiguation", Toast.LENGTH_SHORT).show();

                //update the query
                previousQuery = currentQuery;
                currentQuery = query;

                //update the matchedNodes
                previousMatchedNodes = matchedNodes;
                matchedNodes = matchingQueriesMatchedNodesList.get(0).getValue();

                dialog.show();
                refreshPreviewTextView();
                setNumberOfMatchedNodes(matchingQueriesMatchedNodesList.get(0).getValue().size());
                setCurrentState(askingForVerbalInstructionFollowUpState);
                initPrompt();
            }
            else {
                //save the block and show a confirmation dialog for the block
                Toast.makeText(context, query.toString(), Toast.LENGTH_SHORT).show();
                System.out.println("Result Query: " + query.toString());
                //construct the block from the query formula

                SerializableOntologyQuery serializableOntologyQuery = new SerializableOntologyQuery(query);

                SugiliteOperationBlock block = blockBuildingHelper.getOperationBlockFromQuery(serializableOntologyQuery, SugiliteOperation.CLICK, featurePack);
                showConfirmationDialog(block, featurePack, queryScoreList, clickRunnable);
                dialog.dismiss();
            }

        } else {
            //can't match, show the dialog and switch to result won't match state
            dialog.show();
            setCurrentState(resultWontMatchState);
            initPrompt();
        }
    }

    private void showConfirmationDialog(SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack, List<Pair<SerializableOntologyQuery, Double>> queryScoreList, Runnable clickRunnable) {
        SugiliteRecordingConfirmationDialog sugiliteRecordingConfirmationDialog = new SugiliteRecordingConfirmationDialog(context, block, featurePack, queryScoreList, clickRunnable, blockBuildingHelper, layoutInflater, uiSnapshot, actualClickedNode, sugiliteData, sharedPreferences, tts);
        sugiliteRecordingConfirmationDialog.show();
    }

    @Override
    public void runOnMainThread(Runnable r) {
        try {
            if (context instanceof SugiliteAccessibilityService) {
                ((SugiliteAccessibilityService) context).runOnUiThread(r);
            } else {
                throw new Exception("no access to ui thread");
            }
        } catch (Exception e) {
            //do nothing
            e.printStackTrace();
        }
    }
}
