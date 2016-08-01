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
        String messageTypeString = "", arg1 = "", arg2 = "";
        if (getIntent().getExtras() != null)
        {
            messageTypeString = getIntent().getStringExtra("messageType");
            messageType.setText(messageTypeString);
            /*

            messageType, arg1, arg2
            --------------------------
            START_RECORDING, scriptName, callbackString (callbackString gets called when finish recording OR at EXCEPTION)
            END_RECORDING, "NULL", callbackString (... gets called with status: SUCCESS or EXCEPTION)
            RUN_SCRIPT, scriptName, callbackString (... when finish executing or EXCEPTION)
            RUN_JSON, JSON, callbackString (callbackString gets called when finish executing or EXCEPTION)
            //TODO: send call back when finish executing
            ADD_JSON_AS_SCRIPT, JSON, "NULL" //return value returned as activity result instead
            GET_SCRIPT, scriptName, "NULL" //return value returned as activity result instead
            GET_SCRIPT_LIST, "NULL, "NULL" //return value returned as activity result instead



            START_TRACKING, trackingName, callbackString
            END_TRACKING, "NULL", callbackString
            GET_TRACKING, trackingName, "NULL"
            GET_TRACKING_LIST, "NULL", "NULL"
            CLEAR_TRACKING_LIST, "NULL", "NULL"
            */

            arg1 = getIntent().getStringExtra("arg1");
            arg2 = getIntent().getStringExtra("arg2");
            scriptName.setText(arg1);

        }
        this.sugiliteScriptDao = new SugiliteScriptDao(this);
        this.sugiliteTrackingDao = new SugiliteTrackingDao(this);
        this.jsonProcessor = new SugiliteBlockJSONProcessor(this);
        this.sugiliteData = (SugiliteData)getApplication();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.context = this;
        handleRequest(messageTypeString, arg1, arg2);

    }

    private void handleRequest(String messageType, final String arg1, final String arg2){
        boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
        boolean trackingInProcess = sharedPreferences.getBoolean("tracking_in_process", false);
        switch (messageType){
            //
            case "START_RECORDING":
                //arg1 = scriptName, arg2 = callbackString
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(recordingInProcess) {
                    //the exception message below will be sent when there's already recording in process
                    sugiliteData.sendCallbackMsg("START_RECORDING_EXCEPTION", "recording already in process", arg2);
                    finish();
                }
                else {
                    if (arg1 != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("New Recording")
                                .setMessage("Now start recording new script " + arg1)
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
                                        editor.putString("scriptName", arg1);
                                        editor.putBoolean("recording_in_process", true);
                                        editor.commit();

                                        sugiliteData.initiateScript(arg1 + ".SugiliteScript");
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
                //arg1 = "NULL", arg2 = callbackString
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(recordingInProcess) {

                    SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                    prefEditor.putBoolean("recording_in_process", false);
                    prefEditor.commit();
                    if(sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null)
                        sugiliteData.sendCallbackMsg("FINISHED_RECORDING", sugiliteData.getScriptHead().getScriptName(), arg2);

                    Toast.makeText(context, "recording ended", Toast.LENGTH_SHORT).show();
                    sendReturnValue("");
                }
                else {
                    //the exception message below will be sent when there's no recording in process
                    sugiliteData.sendCallbackMsg("END_RECORDING_EXCEPTION", "no recording in process", arg2);
                    finish();
                }
                break;
            case "RUN_SCRIPT":
                //arg1 = scriptName, arg2 = callbackString
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(recordingInProcess) {
                    sugiliteData.sendCallbackMsg("RUN_SCRIPT_EXCEPTION", "recording already in process", arg2);
                    finish();
                }
                else {
                    SugiliteStartingBlock script = sugiliteScriptDao.read(arg1 + ".SugiliteScript");

                    if(script == null) {
                        sugiliteData.sendCallbackMsg("RUN_SCRIPT_EXCEPTION", "null script", arg2);
                        finish();
                    }
                    else {
                        runScript(script);
                    }
                }
                break;
            case "RUN_JSON":
                //arg1 = JSON, arg2 = callbackString
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(arg1 != null){
                    try{
                        SugiliteStartingBlock script = jsonProcessor.jsonToScript(arg1);
                        runScript(script);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        sugiliteData.sendCallbackMsg("RUN_JSON_EXCEPTION", "error in json parsing", arg2);
                        finish();
                    }
                }
                else {
                    sugiliteData.sendCallbackMsg("RUN_JSON_EXCEPTION", "null json", arg2);
                    finish();
                }
                break;
            case "ADD_JSON_AS_SCRIPT":
                //arg1 = JSON, arg2 = "NULL"
                if(arg1 != null){
                    try{
                        SugiliteStartingBlock script = jsonProcessor.jsonToScript(arg1);
                        sugiliteScriptDao.save(script);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        sugiliteData.sendCallbackMsg("ADD_JSON_AS_SCRIPT_EXCEPTION", "error in json parsing", arg2);
                    }

                }
                else {
                    sugiliteData.sendCallbackMsg("ADD_JSON_AS_SCRIPT", "null json", arg2);
                }
                break;
            case "GET_SCRIPT":
                //arg1 = scriptName, arg2 = "NULL"
                SugiliteStartingBlock script = sugiliteScriptDao.read(arg1 + ".SugiliteScript");
                if(script != null)
                    sendReturnValue(jsonProcessor.scriptToJson(script));
                else
                    //the exception message below will be sent when can't find a script with provided name
                    //TODO: send exception message
                break;
            case "GET_SCRIPT_LIST":
                //arg1 = scriptName, arg2 = "NULL"
                List<String> allNames = sugiliteScriptDao.getAllNames();
                List<String> retVal = new ArrayList<>();
                for(String name : allNames)
                    retVal.add(name.replace(".SugiliteScript", ""));
                sendReturnValue(new Gson().toJson(retVal));
                break;




            //*******
            case "START_TRACKING":
                //commit preference change
                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                sugiliteData.initiateTracking(arg1);
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
                SugiliteStartingBlock tracking = sugiliteTrackingDao.read(arg1);
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

    private void runScript(SugiliteStartingBlock script){
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

    private void sendReturnValue(String retVal){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", retVal);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }
}
