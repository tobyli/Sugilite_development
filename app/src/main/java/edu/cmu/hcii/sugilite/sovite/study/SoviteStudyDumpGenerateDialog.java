package edu.cmu.hcii.sugilite.sovite.study;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 3/9/20
 * @time 4:19 PM
 */
public class SoviteStudyDumpGenerateDialog {
    private Context context;
    private Dialog dialog;
    private View dialogView;
    private EditText dumpNameEditText;
    SoviteStudyDumpManager soviteStudyDumpManager;

    public SoviteStudyDumpGenerateDialog(Context context, SugiliteData sugiliteData, PumiceDialogManager pumiceDialogManager) {
        this.context = context;
        this.soviteStudyDumpManager = new SoviteStudyDumpManager(context, sugiliteData, pumiceDialogManager);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        this.dialogView = layoutInflater.inflate(R.layout.dialog_sovite_dump, null);
        this.dumpNameEditText = (EditText) dialogView.findViewById(R.id.edittext_dump_file_name);
        this.dumpNameEditText.setText(getDefaultName());
        AlertDialog.Builder builder = new AlertDialog.Builder(context);


        builder.setMessage("Specify the name for the dump file")
                .setView(dialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String dumpFileName = dumpNameEditText.getText().toString();
                        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.saving_sovite_dump_message);
                        progressDialog.show();
                        try {
                            soviteStudyDumpManager.saveSoviteStudyDumpPacketToFile(dumpFileName);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        progressDialog.dismiss();                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setTitle("New Script");

        dialog = builder.create();
    }

    public void show(){
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private String getDefaultName() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        String strDate = dateFormat.format(calendar.getTime());
        return "SoviteDump_" + strDate;
    }
}
