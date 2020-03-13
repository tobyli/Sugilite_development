package edu.cmu.hcii.sugilite.sovite.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.Toast;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.pumice.ui.PumiceDialogActivity;
import edu.cmu.hcii.sugilite.sovite.ScriptVisualThumbnailManager;
import edu.cmu.hcii.sugilite.sovite.dialog.intent_handler.SoviteDemonstrateRelevantScreenIntentHandler;
import edu.cmu.hcii.sugilite.sovite.dialog.intent_handler.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;

/**
 * @author toby
 * @date 3/12/20
 * @time 10:06 AM
 */
public class SoviteDisambiguationDemonstrationDialog {
    private AlertDialog dialog;
    private Activity context;
    private SugiliteScriptDao sugiliteScriptDao;
    private SharedPreferences sharedPreferences;
    private SugiliteData sugiliteData;
    private ServiceStatusManager serviceStatusManager;
    private VerbalInstructionIconManager verbalInstructionIconManager;
    private ScriptVisualThumbnailManager scriptVisualThumbnailManager;
    private PumiceDialogManager pumiceDialogManager;
    private SoviteDemonstrateRelevantScreenIntentHandler soviteDemonstrateRelevantScreenIntentHandler;


    private String procedureKnowledgeName;
    private String userUtterance;
    private String appPackageName;
    private String appReadableName;
    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;

    public SoviteDisambiguationDemonstrationDialog(Activity context, PumiceDialogManager pumiceDialogManager, String procedureKnowledgeName, String userUtterance, String appPackageName, String appReadableName, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject, SoviteDemonstrateRelevantScreenIntentHandler soviteDemonstrateRelevantScreenIntentHandler){
        this.context = context;
        this.scriptVisualThumbnailManager = new ScriptVisualThumbnailManager(context);
        this.pumiceDialogManager = pumiceDialogManager;
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
        this.soviteDemonstrateRelevantScreenIntentHandler = soviteDemonstrateRelevantScreenIntentHandler;

        constructDialog();
    }

    private void constructDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage(String.format("Please demonstrate which screen in %s is more relevant to the task \"%s\". Click OK to continue.", appReadableName, userUtterance))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //String scriptName = "Procedure_" + procedureKnowledgeName; //determine the script name
                        String scriptName = "AppReference_" + userUtterance;
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
                                            SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName + ".SugiliteScript");
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

                        PumiceDemonstrationUtil.initiateDemonstration(context, serviceStatusManager, sharedPreferences, scriptName, sugiliteData, onFinishDemonstrationCallback, sugiliteScriptDao, verbalInstructionIconManager);
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

        //extract the last screen in the script
        Drawable lastScreenshot = scriptVisualThumbnailManager.getVisualThumbnailForScript(script, userUtterance);
        SerializableUISnapshot lastScreenUISnapshot = scriptVisualThumbnailManager.getLastAvailableUISnapshotInSubsequentScript(script, null);

        //prompt the user of the screen
        ImageView imageView = new ImageView(context);
        imageView.setImageDrawable(lastScreenshot);
        pumiceDialogManager.sendAgentMessage(String.format("I've learned about the relevant screen in %s", appReadableName), true, false);
        pumiceDialogManager.sendAgentViewMessage(imageView, String.format("Relevant screen in %s", appReadableName), false, false);

        //send out a packet to the embedding server




        //run the returnResultCallback when the result if ready
        //parentIntentHandler.callReturnValueCallback(new PumiceProceduralKnowledge(context, procedureKnowledgeName, procedureKnowledgeName, script));
    }

    //show the dialog
    public void show() {
        dialog.show();
    }
}