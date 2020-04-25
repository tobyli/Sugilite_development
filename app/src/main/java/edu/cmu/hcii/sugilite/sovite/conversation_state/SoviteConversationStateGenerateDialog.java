package edu.cmu.hcii.sugilite.sovite.conversation_state;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.sovite.study.SoviteStudyDumpManager;
import edu.cmu.hcii.sugilite.sovite.study.SoviteStudyDumpPacket;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;

/**
 * @author toby
 * @date 3/9/20
 * @time 4:19 PM
 */
public class SoviteConversationStateGenerateDialog {
    private Context context;
    private Dialog dialog;
    private View dialogView;
    private EditText dumpNameEditText;
    private SoviteConversationState soviteConversationState;
    private File conversationStateDir;


    public SoviteConversationStateGenerateDialog(Context context, SoviteConversationState soviteConversationState) {
        this.context = context;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        this.dialogView = layoutInflater.inflate(R.layout.dialog_sovite_dump, null);
        this.dumpNameEditText = (EditText) dialogView.findViewById(R.id.edittext_dump_file_name);
        this.dumpNameEditText.setText(getDefaultName());
        this.soviteConversationState = soviteConversationState;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        try {
            File rootDataDir = context.getFilesDir();
            conversationStateDir = new File(rootDataDir.getPath() + "/sovite_conversation_state_dump");
            if (!conversationStateDir.exists() || !conversationStateDir.isDirectory())
                conversationStateDir.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }

        builder.setMessage("Specify the name for the saved conversation state file")
                .setView(dialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String dumpFileName = dumpNameEditText.getText().toString();
                        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.saving_sovite_dump_message);
                        progressDialog.show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    File path = null;
                                    soviteConversationState.setName(dumpFileName);
                                    path = saveSoviteConversationStateToFile(soviteConversationState, dumpFileName);
                                    if (path != null) {
                                        PumiceDemonstrationUtil.showSugiliteAlertDialog("The Sovite dump file has been saved to " + path.getCanonicalPath() + ".");
                                    }
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                                progressDialog.dismiss();
                            }
                        }).start();

                    }
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = dateFormat.format(calendar.getTime());
        return "SoviteConversationState_" + strDate;
    }

    // save file
    private File saveSoviteConversationStateToFile(SoviteConversationState soviteConversationState, String packetFileName) throws Exception {
        File f = new File(conversationStateDir.getPath() + "/" + packetFileName + ".sovitedump");
        ObjectOutputStream oos = null;
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(f);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(soviteConversationState);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
            //TODO: error handling
        } finally {
            if (oos != null)
                oos.close();
            if (fout != null)
                fout.close();
        }
        return f;

    }
}
