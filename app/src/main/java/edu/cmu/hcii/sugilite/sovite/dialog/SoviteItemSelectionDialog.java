package edu.cmu.hcii.sugilite.sovite.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;


import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 3/2/20
 * @time 2:06 PM
 */
public class SoviteItemSelectionDialog extends SugiliteDialogManager {

    private LayoutInflater layoutInflater;

    private View dialogView;
    private ListView mainListView;
    private Dialog dialog;
    private SugiliteDialogSimpleState promptState;
    private List<Pair<String, Runnable>> listItemLabelRunnableList;
    private boolean toSkipIfOnlyOneItem = false;

    public SoviteItemSelectionDialog(Activity context, PumiceDialogManager pumiceDialogManager) {
        super(context, pumiceDialogManager.getSugiliteVoiceRecognitionListener());
        this.layoutInflater = LayoutInflater.from(context);
    }

    public SoviteItemSelectionDialog(Activity context, PumiceDialogManager pumiceDialogManager, List<Pair<String, Runnable>> listItemLabelRunnableList, String prompt, boolean toSkipIfOnlyOneItem) {
        this(context, pumiceDialogManager);
        initDialog(listItemLabelRunnableList, prompt, toSkipIfOnlyOneItem);
    }

    public void initDialog(List<Pair<String, Runnable>> listItemLabelRunnableList, String prompt, boolean toSkipIfOnlyOneItem) {
        this.promptState = new SugiliteDialogSimpleState("CHOOSE_BETWEEN_SCRIPT_DISAMBIGUATIONS", this, false);
        this.promptState.setPrompt(prompt);
        this.listItemLabelRunnableList = listItemLabelRunnableList;
        this.toSkipIfOnlyOneItem = toSkipIfOnlyOneItem;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);


        dialogView = layoutInflater.inflate(R.layout.dialog_choosing_parsing, null);
        mainListView = dialogView.findViewById(R.id.listview_query_candidates);


        String[] stringArray = new String[listItemLabelRunnableList.size()];
        Runnable[] runnableArray = new Runnable[listItemLabelRunnableList.size()];

        int i = 0;
        for (Pair<String, Runnable> pair : listItemLabelRunnableList) {
            stringArray[i] = pair.first;
            runnableArray[i] = pair.second;
            i++;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_2, stringArray) {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;
                if (null == convertView) {
                    row = layoutInflater.inflate(android.R.layout.simple_list_item_1, null);
                } else {
                    row = convertView;
                }
                TextView tv1 = (TextView) row.findViewById(android.R.id.text1);
                tv1.setText(Html.fromHtml(getItem(position)));
                //textViews.put(tv1, ontologyQueryArray[position]);
                return row;
            }

        };

        mainListView.setAdapter(adapter);
        //finished setting up the parse result candidate list
        builder.setView(dialogView);
        //on item click for query candidates
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //showConfirmation(sugiliteOperationBlockArray[position], featurePack, queryScoreList);
                runnableArray[position].run();
            }
        });

        dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                stopASRandTTS();
                onDestroy();
            }
        });
    }

    public void show() {
        if(dialog != null) {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(OVERLAY_TYPE);
            }
            dialog.show();
        }
        //initiate the dialog manager when the dialog is shown
        initDialogManager();
    }

    @Override
    public void initDialogManager() {
        //set current sate
        setCurrentState(promptState);
        initPrompt();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

}
