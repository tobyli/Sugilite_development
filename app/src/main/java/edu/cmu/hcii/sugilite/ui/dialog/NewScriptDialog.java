package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;

/**
 * @author toby
 * @date 8/3/16
 * @time 6:14 PM
 */
public class NewScriptDialog extends AbstractSugiliteDialog {
    private AlertDialog dialog;
    public NewScriptDialog(final Context context, final SugiliteScriptDao sugiliteScriptDao, final ServiceStatusManager serviceStatusManager,
                           final SharedPreferences sharedPreferences, final SugiliteData sugiliteData, boolean isSystemAlert, final Dialog.OnClickListener positiveCallback, final Dialog.OnClickListener negativeCallback){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        sugiliteData.clearInstructionQueue();
        final EditText scriptName = new EditText(context);
        scriptName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        scriptName.setSingleLine(true);
        scriptName.setText(sugiliteScriptDao.getNextAvailableDefaultName());
        scriptName.setSelectAllOnFocus(true);
        builder.setMessage("Specify the name for your new script")
                .setView(scriptName)
                .setPositiveButton("Start Recording", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!serviceStatusManager.isRunning()){
                            //prompt the user if the accessiblity service is not active
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                            builder1.setTitle("Service not running")
                                    .setMessage("The " + Const.appNameUpperCase + " accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            serviceStatusManager.promptEnabling();
                                            //do nothing
                                        }
                                    }).show();
                        }
                        else if (scriptName != null && scriptName.getText().toString().length() > 0) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("scriptName", scriptName.getText().toString());
                            editor.putBoolean("recording_in_process", true);
                            editor.commit();

                            //set the system state
                            sugiliteData.setCurrentSystemState(SugiliteData.RECORDING_STATE);

                            //set the active script to the newly created script
                            sugiliteData.initiateScript(scriptName.getText().toString() + ".SugiliteScript");
                            sugiliteData.initiatedExternally = false;
                            //save the newly created script to DB
                            try {
                                sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                sugiliteScriptDao.commitSave();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            //Toast.makeText(v.getContext(), "Changed script name to " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();
                            if(positiveCallback != null)
                                positiveCallback.onClick(dialog, 0);
                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                            startMain.addCategory(Intent.CATEGORY_HOME);
                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(startMain);
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
        if(isSystemAlert)
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        scriptName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
    }
}
