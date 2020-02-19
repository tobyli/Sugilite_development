package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.dialog.AbstractSugiliteDialog;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 2/18/19
 * @time 7:09 PM
 */
public class PumiceChooseParsingDialogNew implements AbstractSugiliteDialog {

    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private PumiceSemanticParsingResultPacket resultPacket;
    private Runnable runnableForRetry;
    private PumiceParsingResultWithResolveFnConfirmationHandler.ConfirmedParseRunnable runnableForConfirmedParse;
    private PumiceParsingResultDescriptionGenerator pumiceParsingResultDescriptionGenerator;
    private PumiceParsingDifferenceProcessor pumiceParsingDifferenceProcessor;
    private List<String> candidateFormulas;

    private AlertDialog dialog;

    private int failureCount;

    final private static int MAX_LIST_SIZE = 4;

    public PumiceChooseParsingDialogNew(Activity context, PumiceDialogManager pumiceDialogManager, PumiceSemanticParsingResultPacket resultPacket, Runnable runnableForRetry, PumiceParsingResultWithResolveFnConfirmationHandler.ConfirmedParseRunnable runnableForConfirmedParse, int failureCount) {
        this.context = context;
        this.pumiceDialogManager = pumiceDialogManager;
        this.resultPacket = resultPacket;
        this.runnableForRetry = runnableForRetry;
        this.runnableForConfirmedParse = runnableForConfirmedParse;
        this.pumiceParsingResultDescriptionGenerator = new PumiceParsingResultDescriptionGenerator();
        this.pumiceParsingDifferenceProcessor = new PumiceParsingDifferenceProcessor(context, pumiceDialogManager);
        this.failureCount = failureCount;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);


        List<String> candidateNameList = new ArrayList<>();
        Map<String, String> candidateNameFormulaMap = new HashMap<>();
        candidateFormulas = new ArrayList<>();

        for(PumiceSemanticParsingResultPacket.QueryGroundingPair queryGroundingPair : resultPacket.queries){
            //TODO: use readable descriptions instead
            candidateNameList.add(getDescriptionForFormula(queryGroundingPair.formula, resultPacket.utteranceType));
            candidateNameFormulaMap.put(getDescriptionForFormula(queryGroundingPair.formula, resultPacket.utteranceType), queryGroundingPair.formula);
            candidateFormulas.add(queryGroundingPair.formula);
        }

        if (candidateNameList.size() > MAX_LIST_SIZE) {
            candidateNameList = candidateNameList.subList(0, MAX_LIST_SIZE);
        }

        //initiate a linearlayout to hold the list view
        LinearLayout parentLinearLayout = new LinearLayout(context);
        parentLinearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView promptTextView = new TextView(context);
        promptTextView.setText(Html.fromHtml("<b>Please choose the correct one that reflects your intention, or choose \"Try Again\" to give a different instruction.<b>"));
        //promptTextView.setPadding(10 ,10, 10,10);
        promptTextView.setTextColor(Color.BLACK);
        promptTextView.setTextSize(18);

        //initiate a list view
        ListView candidateListView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, candidateNameList);
        candidateListView.setAdapter(adapter);

        candidateListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(view instanceof TextView){
                    String selectedCandidateName = ((TextView) view).getText().toString();
                    String selectedCandidateFormula = candidateNameFormulaMap.get(selectedCandidateName);
                    runnableForConfirmedParse.run(selectedCandidateFormula);
                    dialog.dismiss();
                }
            }
        });


        parentLinearLayout.addView(promptTextView);
        parentLinearLayout.addView(candidateListView);


        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)promptTextView.getLayoutParams();
        float marginDp = 20f;
        int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marginDp, context.getResources().getDisplayMetrics());
        params.setMargins(marginPx, marginPx, marginPx, marginPx); //substitute parameters for left, top, right, bottom
        promptTextView.setLayoutParams(params);
        //parentLinearLayout.setPadding(10, 10, 10, 10);


        builder.setView(parentLinearLayout)
                .setPositiveButton("OK", null)
                .setNegativeButton("Try Again", null);


        dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PumiceDemonstrationUtil.showSugiliteToast("Please first choose the correct one that reflects your intention, or choose \"Try Again\" to give a different instruction.", Toast.LENGTH_SHORT);
                        pumiceDialogManager.sendAgentMessage("Please choose the correct one that reflects your intention, or choose \"Try Again\" to give a different instruction.", true, false);
                    }
                });
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pumiceDialogManager.sendAgentMessage("OK. Let's try again. Please try to explain in a way that I can tell from mobile apps on the phone. ", true, false);
                        runnableForRetry.run();
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                pumiceDialogManager.stopTalking();
            }
        });

    }

    @Override
    public void show(){
        dialog.getWindow().setType(OVERLAY_TYPE);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (resultPacket.utteranceType.equals("BOOL_EXP_INSTRUCTION") && candidateFormulas.size() > 0) {
            //use the PumiceParsingDifferenceProcessor to process the candidates instead
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String resultFormula = pumiceParsingDifferenceProcessor.handleBoolParsing(candidateFormulas.subList(0, MAX_LIST_SIZE), runnableForRetry);
                    if (resultFormula != null) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                runnableForConfirmedParse.run(resultFormula);
                            }
                        });
                    } else {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                runnableForRetry.run();
                            }
                        });
                    }
                }
            });

        } else {
            dialog.show();
            //voice prompt
            pumiceDialogManager.sendAgentMessage("Please choose the correct one that reflects your intention, or choose \"Try Again\" to give a different instruction.", true, false);
        }



    }

    private String getDescriptionForFormula(String formula, String utteranceType) {
        switch (utteranceType) {
            case "USER_INIT_INSTRUCTION":
                return pumiceParsingResultDescriptionGenerator.generateForConditionBlock(formula);
            case "BOOL_EXP_INSTRUCTION":
                return pumiceParsingResultDescriptionGenerator.generateForBoolExp(formula);
            case "OPERATION_INSTRUCTION":
                return pumiceParsingResultDescriptionGenerator.generateForOperationBlock(formula);
            case "VALUE_INSTRUCTION":
                return pumiceParsingResultDescriptionGenerator.generateForValue(formula);
            default:
                throw new RuntimeException("unexpected packet type!");

        }
    }
}
