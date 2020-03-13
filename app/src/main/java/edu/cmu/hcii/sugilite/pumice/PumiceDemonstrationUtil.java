package edu.cmu.hcii.sugilite.pumice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationHelper;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteAndroidAPIVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteGoogleCloudVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 1/7/19
 * @time 2:44 PM
 */
public class PumiceDemonstrationUtil {
    /**
     * initiate a demonstration recording -> need to call endRecording() when the recording ends
     * @param context
     * @param serviceStatusManager
     * @param sharedPreferences
     * @param scriptName
     * @param sugiliteData
     * @param afterRecordingCallback
     * @param sugiliteScriptDao
     * @param verbalInstructionIconManager
     */
    public static void initiateDemonstration(Context context, ServiceStatusManager serviceStatusManager, SharedPreferences sharedPreferences, String scriptName, SugiliteData sugiliteData, Runnable afterRecordingCallback, SugiliteScriptDao sugiliteScriptDao, VerbalInstructionIconManager verbalInstructionIconManager){
        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessibility service is not active
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
        } else {
            //start demonstration
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("scriptName", scriptName);
            editor.putBoolean("recording_in_process", true);
            editor.commit();

            //set the system state
            sugiliteData.setCurrentSystemState(SugiliteData.RECORDING_STATE);


            //set the active script to the newly created script
            sugiliteData.initiateScriptRecording(scriptName + ".SugiliteScript", afterRecordingCallback); //add the end recording callback
            sugiliteData.initiatedExternally = false;

            //save the newly created script to DB
            try {
                sugiliteScriptDao.save(sugiliteData.getScriptHead());
                sugiliteScriptDao.commitSave(null);
            }
            catch (Exception e){
                e.printStackTrace();
            }

            //send the phone back to the home screen
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(startMain);

            //turn on the cat overlay to prepare for demonstration
            if(verbalInstructionIconManager != null){
                verbalInstructionIconManager.turnOnCatOverlay();
            }
        }
    }

    /**
     * execute a script  --> check the service status and the variable values before doing so
     * @param activityContext
     * @param serviceStatusManager
     * @param script
     * @param sugiliteData
     * @param sharedPreferences
     * @param dialogManager
     */
    public static void executeScript(Activity activityContext, ServiceStatusManager serviceStatusManager, SugiliteStartingBlock script, SugiliteData sugiliteData, SharedPreferences sharedPreferences, boolean isForReconstructing, @Nullable PumiceDialogManager dialogManager, @Nullable SugiliteBlock afterExecutionOperation, @Nullable Runnable afterExecutionRunnable){
        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessiblity service is not active
            activityContext.runOnUiThread(() -> {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(activityContext);
                builder1.setTitle("Service not running")
                        .setMessage("The Sugilite accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            serviceStatusManager.promptEnabling();
                            //do nothing
                        }).show();
            });
        }
        else {
            //check if pumice dialog manager is available, create a new one if needed
            if (dialogManager == null) {
                if (sugiliteData.pumiceDialogManager != null) {
                    dialogManager = sugiliteData.pumiceDialogManager;
                } else {
                    dialogManager = new PumiceDialogManager(activityContext, true);
                    SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener = null;
                    TextToSpeech tts = sugiliteData.getTTS();
                    if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.ANDROID) {
                        sugiliteVoiceRecognitionListener = new SugiliteAndroidAPIVoiceRecognitionListener(activityContext, null, tts);
                    } else if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.GOOGLE_CLOUD) {
                        sugiliteVoiceRecognitionListener = new SugiliteGoogleCloudVoiceRecognitionListener(activityContext, null, tts);
                    }
                    dialogManager.setSugiliteVoiceRecognitionListener(sugiliteVoiceRecognitionListener);
                    sugiliteData.pumiceDialogManager = dialogManager;
                }
            }

            final PumiceDialogManager finalDialogManager = dialogManager;

            activityContext.runOnUiThread(() -> {
                VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(activityContext, sugiliteData, script, sharedPreferences, SugiliteData.EXECUTION_STATE, finalDialogManager, isForReconstructing);
                if(script.variableNameDefaultValueMap.size() > 0) {

                    //has variable
                    sugiliteData.stringVariableMap.putAll(script.variableNameDefaultValueMap);
                    boolean needUserInput = false;

                    //check if any of the variables needs user input
                    for(Map.Entry<String, Variable> entry : script.variableNameDefaultValueMap.entrySet()){
                        if(entry.getValue().type == Variable.USER_INPUT){
                            needUserInput = true;
                            break;
                        }
                    }
                    if(needUserInput) {
                        //show the dialog to obtain user input
                        variableSetValueDialog.show();
                    }
                    else {
                        variableSetValueDialog.executeScript(afterExecutionOperation, finalDialogManager, afterExecutionRunnable);
                    }
                }
                else{
                    //execute the script without showing the dialog
                    variableSetValueDialog.executeScript(afterExecutionOperation, finalDialogManager, afterExecutionRunnable);
                }
            });
        }
    }

    /**
     * end the current recording
     * @param context
     * @param sugiliteData
     * @param sharedPreferences
     * @param sugiliteScriptDao
     */
    public static void endRecording(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences, SugiliteScriptDao sugiliteScriptDao) {
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        SugiliteBlockJSONProcessor jsonProcessor = new SugiliteBlockJSONProcessor(context);


        //end recording
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.apply();

        new Thread(new Runnable() {
            @Override
            public void run() {
                //commit the script through the sugiliteScriptDao
                try {
                    if (sugiliteScriptDao != null) {
                        sugiliteScriptDao.commitSave(new Runnable() {
                            @Override
                            public void run() {
                                //invoke the callback
                                if (sugiliteData.initiatedExternally && sugiliteData.getScriptHead() != null) {
                                    //return the recording to the external caller
                                    sugiliteData.communicationController.sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());
                                    sugiliteData.sendCallbackMsg(SugiliteCommunicationHelper.FINISHED_RECORDING, jsonProcessor.scriptToJson(sugiliteData.getScriptHead()), sugiliteData.callbackString);
                                }

                                //call the after recording callback
                                if (sugiliteData.getScriptHead() != null && sugiliteData.afterRecordingCallback != null){
                                    //call the endRecordingCallback
                                    Runnable r = sugiliteData.afterRecordingCallback;
                                    sugiliteData.afterRecordingCallback = null;
                                    r.run();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();





        //turn off the recording overlay if any
        if(sugiliteData.verbalInstructionIconManager != null){
            sugiliteData.verbalInstructionIconManager.turnOffCatOverlay();
        }


        sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
        PumiceDemonstrationUtil.showSugiliteToast("end recording", Toast.LENGTH_SHORT);
    }

    public static void showSugiliteToast(String text, int length) {
        SugiliteData.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SugiliteData.getAppContext(), text, length).show();

            }
        });
    }

    public static void showSugiliteAlertDialog(String content) {
        SugiliteData.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(SugiliteData.getAppContext()).setMessage(content).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
                dialog.getWindow().setType(OVERLAY_TYPE);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }
        });
    }

    public static String removeScriptExtension (String scriptName) {
        if (scriptName.endsWith(".SugiliteScript")) {
            return scriptName.replace(".SugiliteScript", "");
        } else {
            return scriptName;
        }
    }

    public static String addScriptExtension (String scriptName) {
        if (scriptName.endsWith(".SugiliteScript")) {
            return scriptName;
        } else {
            return scriptName + ".SugiliteScript";
        }
    }

    public static String boldify(String string){
        return "<b>" + string + "</b>";
    }
}
