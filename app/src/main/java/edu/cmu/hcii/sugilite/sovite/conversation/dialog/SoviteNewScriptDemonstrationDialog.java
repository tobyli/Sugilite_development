package edu.cmu.hcii.sugilite.sovite.conversation.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.pumice.ui.PumiceDialogActivity;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.sovite.conversation.intent_handler.SoviteDemonstrateRelevantScreenIntentHandler;
import edu.cmu.hcii.sugilite.sovite.conversation.intent_handler.SoviteScriptMatchedFromScreenIntentHandler;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.SugiliteData.RECORDING_STATE;

/**
 * @author toby
 * @date 3/12/20
 * @time 10:06 AM
 */
public class SoviteNewScriptDemonstrationDialog {
    private AlertDialog dialog;
    private Activity context;
    private SugiliteScriptDao sugiliteScriptDao;
    private SharedPreferences sharedPreferences;
    private SugiliteData sugiliteData;
    private ServiceStatusManager serviceStatusManager;
    private VerbalInstructionIconManager verbalInstructionIconManager;
    private PumiceDialogManager pumiceDialogManager;
    private SoviteScriptMatchedFromScreenIntentHandler soviteScriptMatchedFromScreenIntentHandler;

    private SugiliteStartingBlock appReferenceScript;
    private String procedureKnowledgeName;
    private String userUtterance;
    private String appPackageName;
    private String appReadableName;
    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;

    public SoviteNewScriptDemonstrationDialog(Activity context, PumiceDialogManager pumiceDialogManager, SugiliteStartingBlock appReferenceScript, String procedureKnowledgeName, String userUtterance, String appPackageName, String appReadableName, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject, SoviteScriptMatchedFromScreenIntentHandler soviteScriptMatchedFromScreenIntentHandler){
        this.context = context;
        this.pumiceDialogManager = pumiceDialogManager;
        this.appReferenceScript = appReferenceScript;
        this.procedureKnowledgeName = procedureKnowledgeName;
        this.appPackageName = appPackageName;
        this.appReadableName = appReadableName;
        this.userUtterance = userUtterance;
        this.sharedPreferences = pumiceDialogManager.getSharedPreferences();
        this.sugiliteData = pumiceDialogManager.getSugiliteData();
        this.verbalInstructionIconManager = sugiliteData.verbalInstructionIconManager;
        this.serviceStatusManager = pumiceDialogManager.getServiceStatusManager();
        this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        this.returnValueCallbackObject = returnValueCallbackObject;
        this.soviteScriptMatchedFromScreenIntentHandler = soviteScriptMatchedFromScreenIntentHandler;

        constructDialog();
    }

    private void constructDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage(String.format("Please teach me how to perform the task \"%s\" in %s. Click OK to continue.", userUtterance, appReadableName))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //String scriptName = "Procedure_" + procedureKnowledgeName; //determine the script name
                        String scriptName = "Procedure_" + userUtterance;
                        //create a callback to be called when the recording ends
                        Runnable onFinishDemonstrationCallback = new Runnable() {
                            @Override
                            public void run() {
                                SugiliteData.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //turn off the overlay
                                        if(verbalInstructionIconManager != null){
                                            verbalInstructionIconManager.turnOffCatOverlay();
                                        }

                                        //get the result script
                                        try {
                                            SugiliteStartingBlock script = sugiliteScriptDao.read(PumiceDemonstrationUtil.addScriptExtension(scriptName));
                                            if (script != null) {
                                                onDemonstrationReady(script);
                                            } else {
                                                throw new RuntimeException("can't find the script!");
                                            }
                                        } catch (Exception e){
                                            throw new RuntimeException("failed to read the script!");
                                        }
                                    }
                                });
                            }
                        };

                        //PumiceDemonstrationUtil.initiateDemonstration(context, serviceStatusManager, sharedPreferences, scriptName, sugiliteData, onFinishDemonstrationCallback, sugiliteScriptDao, verbalInstructionIconManager);

                        //resume the app screen
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appPackageName);
                        if (launchIntent != null) {
                            context.startActivity(launchIntent);
                        } else {
                            PumiceDemonstrationUtil.showSugiliteToast("There is no package available in android", Toast.LENGTH_SHORT);
                        }

                        //resume the recording from the AppReference script
                        sugiliteData.initiatedExternally = false;
                        sugiliteData.setScriptHead(appReferenceScript);
                        sugiliteData.setCurrentScriptBlock(appReferenceScript.getTail());
                        try {
                            sugiliteScriptDao.delete(appReferenceScript.getScriptName());
                            appReferenceScript.setScriptName(PumiceDemonstrationUtil.addScriptExtension(scriptName));
                            sugiliteScriptDao.save(appReferenceScript);
                            sugiliteScriptDao.commitSave(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("You can start demonstrating now.");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.out.println("Turning on recording - resuming");
                                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                                prefEditor.putBoolean("recording_in_process", true);
                                prefEditor.commit();
                                sugiliteData.verbalInstructionIconManager.turnOnCatOverlay();
                                sugiliteData.setCurrentSystemState(RECORDING_STATE);
                                sugiliteData.afterRecordingCallback = onFinishDemonstrationCallback;
                            }
                        });
                        Dialog resumeRecordingDialog = builder.create();
                        if (resumeRecordingDialog.getWindow() != null) {
                            resumeRecordingDialog.getWindow().setType(OVERLAY_TYPE);
                        }

                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            //1 sec delay to start recording -> to avoid catching operations from automation execution
                            @Override
                            public void run() {
                                resumeRecordingDialog.show();
                                pumiceDialogManager.sendAgentMessage("You can start demonstrating now.", true, false);

                            }
                        }, 200);


                    }
                });
        dialog = dialogBuilder.create();
    }

    //called when the demonstration is ready
    public void onDemonstrationReady(SugiliteStartingBlock script){
        //resume the Sugilite agent activity
        Intent resumeActivity = new Intent(context, PumiceDialogActivity.class);
        resumeActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivityIfNeeded(resumeActivity, 0);

        PumiceDemonstrationUtil.showSugiliteToast("Demonstration Ready!", Toast.LENGTH_SHORT);
        soviteScriptMatchedFromScreenIntentHandler.onDemonstrationReady(script);
    }

    //show the dialog
    public void show() {
        dialog.show();
    }
}