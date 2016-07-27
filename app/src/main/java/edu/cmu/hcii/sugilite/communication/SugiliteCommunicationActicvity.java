package edu.cmu.hcii.sugilite.communication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTrackingDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ui.VariableSetValueDialog;

public class SugiliteCommunicationActicvity extends AppCompatActivity {
    TextView messageType, scriptName;
    SugiliteScriptDao sugiliteScriptDao;
    SugiliteBlockJSONProcessor jsonProcessor;
    SugiliteData sugiliteData;
    SharedPreferences sharedPreferences;
    SugiliteTrackingDao sugiliteTrackingDao;
    Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sugilite_communication_acticvity);
        messageType = (TextView)findViewById(R.id.receive_message_textview);
        scriptName = (TextView)findViewById(R.id.receive_message_script_name);
        messageType.setText("TEST MESSAGE TYPE");
        String arg1 = "", arg2 = "";
        if (getIntent().getExtras() != null)
        {
            arg1 = getIntent().getStringExtra("messageType");
            messageType.setText(arg1);
            /*
            START_RECORDING, scriptName
            END_RECORDING, "NULL"
            RUN_SCRIPT, scriptName
            GET_SCRIPT, scriptName
            GET_SCRIPT_LIST, "NULL
             */

            arg2 = getIntent().getStringExtra("scriptName");
            scriptName.setText(arg2);

        }
        this.sugiliteScriptDao = new SugiliteScriptDao(this);
        this.sugiliteTrackingDao = new SugiliteTrackingDao(this);
        this.jsonProcessor = new SugiliteBlockJSONProcessor(this);
        this.sugiliteData = (SugiliteData)getApplication();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.context = this;
        handleRequest(arg1, arg2);

    }

    private void handleRequest(String messageType, final String scriptName){
        boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
        boolean trackingInProcess = sharedPreferences.getBoolean("tracking_in_process", false);
        switch (messageType){
            case "START_RECORDING":
                if(recordingInProcess) {
                    //the exception message below will be sent when there's already recording in process
                    //TODO: send exception message
                }
                else {
                    //NOTE: script name should be specified in msg.getData().getString("request");
                    if (scriptName != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("New Recording")
                                .setMessage("Now start recording new script " + scriptName)
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        sugiliteData.clearInstructionQueue();
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString("scriptName", scriptName);
                                        editor.putBoolean("recording_in_process", true);
                                        editor.commit();

                                        sugiliteData.initiateScript(scriptName + ".SugiliteScript");
                                        sugiliteData.initiatedExternally = true;

                                        try {
                                            sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        Toast.makeText(getApplicationContext(), "Recording new script " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();

                                        //go to home screen for recording
                                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                                        startMain.addCategory(Intent.CATEGORY_HOME);
                                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(startMain);
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        dialog.show();
                    }
                }
                break;
            case "END_RECORDING":
                if(recordingInProcess) {

                    SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                    prefEditor.putBoolean("recording_in_process", false);
                    prefEditor.commit();
                    if(sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null)
                        sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());

                    Toast.makeText(context, "recording ended", Toast.LENGTH_SHORT).show();
                    sendReturnValue("");
                }
                else {
                    //the exception message below will be sent when there's no recording in process
                    //TODO: send exception message
                }
                break;
            case "RUN_SCRIPT":
                if(recordingInProcess) {
                //TODO: send exception message
                }
                else {
                    SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName + ".SugiliteScript");
                    if(script == null)
                        //TODO: send exception message
                    sugiliteData.clearInstructionQueue();
                    final ServiceStatusManager serviceStatusManager = new ServiceStatusManager(context);

                    if(!serviceStatusManager.isRunning()){
                        //prompt the user if the accessiblity service is not active
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                        builder1.setTitle("Service not running")
                                .setMessage("The Sugilite accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        serviceStatusManager.promptEnabling();
                                        //do nothing
                                    }
                                });
                        AlertDialog dialog = builder1.create();
                        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        dialog.show();
                    }
                    else {
                        sugiliteData.stringVariableMap.putAll(script.variableNameDefaultValueMap);

                        //kill all the relevant packages
                        for (String packageName : script.relevantPackages) {
                            try {
                                Process sh = Runtime.getRuntime().exec("su", null, null);
                                OutputStream os = sh.getOutputStream();
                                os.write(("am force-stop " + packageName).getBytes("ASCII"));
                                os.flush();
                                os.close();
                                System.out.println(packageName);
                            } catch (Exception e) {
                                e.printStackTrace();
                                // do nothing, likely this exception is caused by non-rooted device
                            }
                        }
                        sugiliteData.runScript(script);
                        try {
                            Thread.sleep(VariableSetValueDialog.SCRIPT_DELAY);
                        } catch (Exception e) {
                            // do nothing
                        }
                        //go to home screen for running the automation
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        context.startActivity(startMain);
                    }
                }
                break;
            case "GET_SCRIPT":
                SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName + ".SugiliteScript");
                if(script != null)
                    sendReturnValue(jsonProcessor.scriptToJson(script));
                else
                    //the exception message below will be sent when can't find a script with provided name
                    //TODO: send exception message
                break;
            case "GET_SCRIPT_LIST":
                List<String> allNames = sugiliteScriptDao.getAllNames();
                List<String> retVal = new ArrayList<>();
                for(String name : allNames)
                    retVal.add(name.replace(".SugiliteScript", ""));
                sendReturnValue(new Gson().toJson(retVal));
                break;
            case "START_TRACKING":
                //commit preference change
                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                sugiliteData.initiateTracking(scriptName);
                prefEditor.putBoolean("tracking_in_process", true);
                prefEditor.commit();
                try {
                    sugiliteTrackingDao.save(sugiliteData.getTrackingHead());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sendReturnValue("");
                Toast.makeText(context, "tracking started", Toast.LENGTH_SHORT).show();
                break;
            case "END_TRACKING":
                if(trackingInProcess) {
                    SharedPreferences.Editor prefEditor2 = sharedPreferences.edit();
                    prefEditor2.putBoolean("tracking_in_process", false);
                    prefEditor2.commit();
                    Toast.makeText(context, "tracking ended", Toast.LENGTH_SHORT).show();
                    sendReturnValue("");
                }
                break;
            case "GET_TRACKING":
                SugiliteStartingBlock tracking = sugiliteTrackingDao.read(scriptName);
                if(tracking != null)
                    sendReturnValue(jsonProcessor.scriptToJson(tracking));
                else
                    //the exception message below will be sent when can't find a script with provided name
                    //TODO: send exception message
                break;
            case "GET_TRACKING_LIST":
                List<String> allTrackingNames = sugiliteTrackingDao.getAllNames();
                List<String> trackingRetVal = new ArrayList<>();
                for(String name : allTrackingNames)
                    trackingRetVal.add(name);
                sendReturnValue(new Gson().toJson(trackingRetVal));
                break;
            case "CLEAR_TRACKING_LIST":
                sugiliteTrackingDao.clear();
                sendReturnValue("");
                break;
        }
    }

    public boolean sendRecordingFinishedSignal(String scriptName){
        return true;
    }

    private void sendReturnValue(String retVal){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", retVal);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }
}
