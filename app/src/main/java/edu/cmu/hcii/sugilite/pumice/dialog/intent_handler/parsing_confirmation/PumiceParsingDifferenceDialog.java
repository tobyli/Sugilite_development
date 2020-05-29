package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Html;
import android.util.Pair;
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

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.dialog.AbstractSugiliteDialog;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 3/5/19
 * @time 10:25 AM
 */
public class PumiceParsingDifferenceDialog<T> implements AbstractSugiliteDialog {
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private AlertDialog dialog;
    private String prompt;

    public PumiceParsingDifferenceDialog(Activity context, PumiceDialogManager pumiceDialogManager, List<Pair<T, String>> candidateCandidateNameList, String prompt, Runnable runnableForRetry, PumiceParsingDifferenceProcessor.ParsingWrapper<T> returnObject) {
        this.context = context;
        this.pumiceDialogManager = pumiceDialogManager;
        this.prompt = prompt;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        List<String> candidateNameList = new ArrayList<>();
        Map<String, T> candidateNameCandidateMap = new HashMap<>();

        for(Pair<T, String> candidateCandidateNamePair : candidateCandidateNameList){
            //TODO: use readable descriptions instead
            candidateNameList.add(candidateCandidateNamePair.second);
            candidateNameCandidateMap.put(candidateCandidateNamePair.second, candidateCandidateNamePair.first);
        }

        //initiate a linearlayout to hold the list view
        LinearLayout parentLinearLayout = new LinearLayout(context);
        parentLinearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView promptTextView = new TextView(context);
        promptTextView.setText(Html.fromHtml(String.format("<b>%s<br><br>You can also choose \"None of the above\" to give a different instruction.<b>", prompt)));
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
                    T selectedCandidate = candidateNameCandidateMap.get(selectedCandidateName);
                    dialog.dismiss();
                    //update selected Candidate;
                    synchronized (returnObject) {
                        returnObject.setObject(selectedCandidate);
                        returnObject.notify();
                    }

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
                .setNegativeButton("None of the above", null);

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                PumiceDemonstrationUtil.showSugiliteToast(context.getString(R.string.choose_parsing_prompt), Toast.LENGTH_SHORT);

                                pumiceDialogManager.sendAgentMessage(context.getString(R.string.choose_parsing_prompt), true, false);
                            }
                        });
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                pumiceDialogManager.sendAgentMessage(context.getString(R.string.try_again), true, false);
                                dialog.dismiss();
                                synchronized (returnObject) {
                                    returnObject.setObject(null);
                                    returnObject.notify();
                                }
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
        });

    }

    @Override
    public void show(){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.getWindow().setType(OVERLAY_TYPE);
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }
        });
        pumiceDialogManager.sendAgentMessage(prompt, true, false);

    }

}
