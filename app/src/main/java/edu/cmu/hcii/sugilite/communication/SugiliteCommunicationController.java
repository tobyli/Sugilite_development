package edu.cmu.hcii.sugilite.communication;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTrackingDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ui.VariableSetValueDialog;

/**
 * Created by oscarr on 7/7/16.
 */
public class SugiliteCommunicationController {
    private final String TAG = SugiliteCommunicationController.class.getName();
    private Messenger sender = null; //used to make an RPC invocation
    private Messenger receiver = null; //invocation replies are processed by this Messenger (Middleware)
    private boolean isBound = false;
    SugiliteScriptDao sugiliteScriptDao;
    SugiliteTrackingDao sugiliteTrackingDao;
    SugiliteBlockJSONProcessor jsonProcessor;
    SugiliteData sugiliteData;
    private ServiceConnection connection; //receives callbacks from bind and unbind invocations
    private Context context; //NOTE: application context
    private final int REGISTER = 1;
    private final int UNREGISTER = 2;
    private final int RESPONSE = 3;
    //TODO: implement tracking
    private final int START_TRACKING = 4;
    private final int STOP_TRACKING = 5;
    private final int GET_ALL_TRACKINGS = 6;
    private final int GET_TRACKING = 7;
    private final int RUN = 9;
    private final int RESPONSE_EXCEPTION = 10;
    private final int START_RECORDING = 11;
    private final int STOP_RECORDING = 12;
    private final int GET_ALL_SCRIPTS = 13;
    private final int GET_SCRIPT = 14;
    private final int APP_TRACKER_ID = 1001;
    private SharedPreferences sharedPreferences;
    //TODO: add start recording/stop recording

    public SugiliteCommunicationController(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences) {
        this.connection = new RemoteServiceConnection();
        this.receiver = new Messenger(new IncomingHandler());
        this.context = context.getApplicationContext();
        this.sugiliteScriptDao = new SugiliteScriptDao(context);
        this.sugiliteTrackingDao = new SugiliteTrackingDao(context);
        this.jsonProcessor = new SugiliteBlockJSONProcessor(context);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
    }

    /**
     *
     * @return true if connection is good, false otherwise
     */
    public boolean checkConnectionStatus(){
        if(connection == null)
            return false;
        return isBound;
    }

    //start() is called when SugiliteAccessibilityService is created
    public void start(){
        //TODO: change the service name here
        //System.out.println("BIND SERVICE FOR CONTEXT " + context);
        Intent intent = createExplicitFromImplicitIntent( context,
                new Intent( "com.yahoo.inmind.services.generic.control.ExternalAppCommService" ) );
        if(intent != null) {
            context.bindService(intent, this.connection, Context.BIND_AUTO_CREATE);
        }
    }

    //stop() is called when SugiliteAccessibilityService is destroyed
    public void stop() {
        if (this.isBound) {
            context.unbindService(connection);
            this.isBound = false;
        }
    }

    //register() is called when SugiliteAccessibilityService is created
    public boolean register(){
        System.out.println("REGISTER");
        return sendMessage(REGISTER, 0, null);
    }

    //unregister() is called when SugiliteAccessibilityService is destroyed
    public boolean unregister(){
        return sendMessage(UNREGISTER, 0, null);
    }


    public boolean sendAllScripts(){
        List<String> allNames = sugiliteScriptDao.getAllNames();
        List<SugiliteStartingBlock> startingBlocks = new ArrayList<>();
        for(String name : allNames) {
            try {
                startingBlocks.add(sugiliteScriptDao.read(name));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return sendMessage( RESPONSE, GET_ALL_SCRIPTS, jsonProcessor.scriptsToJson(startingBlocks));
    }

    public boolean sendAllTrackings(){
        List<String> allNames = sugiliteTrackingDao.getAllNames();
        List<SugiliteStartingBlock> startingBlocks = new ArrayList<>();
        for(String name : allNames) {
            try {
                startingBlocks.add(sugiliteTrackingDao.read(name));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return sendMessage( RESPONSE, GET_ALL_TRACKINGS, jsonProcessor.scriptsToJson(startingBlocks));
    }

    public boolean sendScript(String scriptName){
        // you should send back the script which name is "scriptName"... now, we are using a dummy
        SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName + ".SugiliteScript");
        if(script != null)
            return sendMessage(RESPONSE, GET_SCRIPT, jsonProcessor.scriptToJson(script));
        else
            //the exception message below will be sent when can't find a script with provided name
            return sendMessage(RESPONSE_EXCEPTION, GET_SCRIPT, "Can't find a script with provided name");
    }

    public boolean sendTracking(String trackingName){
        // you should send back the script which name is "scriptName"... now, we are using a dummy
        SugiliteStartingBlock tracking = sugiliteTrackingDao.read(trackingName);
        if(tracking != null)
            return sendMessage(RESPONSE, GET_TRACKING, jsonProcessor.scriptToJson(tracking));
        else
            //the exception message below will be sent when can't find a script with provided name
            return sendMessage(RESPONSE_EXCEPTION, GET_TRACKING, "Can't find a tracking with provided name");
    }

    //the below message will be sent when a externally initiated script has finished recording
    public boolean sendRecordingFinishedSignal(String scriptName){
        return sendMessage(RESPONSE, STOP_RECORDING, "FINISHED RECORDING " + scriptName);
    }

    public boolean sendExecutionFinishedSignal(String scriptName){
        return sendMessage(RESPONSE, RUN, "FINISHED EXECUTING " + scriptName);
    }


    private boolean sendMessage(int messageType, int arg2, String obj){
        if (isBound) {
            Message message = Message.obtain(null, messageType, APP_TRACKER_ID, 0);
            try {
                message.replyTo = receiver;
                message.arg2 = arg2;
                if( obj != null ){
                    Bundle bundle = new Bundle();
                    bundle.putString( "response", obj );
                    message.setData( bundle );
                }
                sender.send(message);
                return true;
            } catch (RemoteException rme) {
                return false;
            }
        } else {
            return false;
        }
    }


    private class RemoteServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName component, IBinder binder) {
            sender = new Messenger(binder);
            isBound = true;
            register();
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            sender = null;
            isBound = false;
        }
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
            boolean trackingInProcess = sharedPreferences.getBoolean("tracking_in_process", false);
            switch(msg.what) {
                case START_RECORDING:
                    if(recordingInProcess) {
                        //the exception message below will be sent when there's already recording in process
                        SugiliteCommunicationController.this.sendMessage(RESPONSE_EXCEPTION, START_RECORDING, "Already recording in progress, can't start");
                    }
                    else {
                        //NOTE: script name should be specified in msg.getData().getString("request");
                        final String scriptName = msg.getData().getString("request");
                        if (scriptName != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

                                            Toast.makeText(context, "Recording new script " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();

                                            //go to home screen for recording
                                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                                            startMain.addCategory(Intent.CATEGORY_HOME);
                                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            context.startActivity(startMain);
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            dialog.show();
                        }
                        Log.d(TAG, "Start Recording");
                        break;
                    }
                case STOP_RECORDING:
                    if(recordingInProcess) {

                        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                        prefEditor.putBoolean("recording_in_process", false);
                        prefEditor.commit();
                        if(sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null)
                            sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());

                        Toast.makeText(context, "end recording", Toast.LENGTH_SHORT).show();
                        if (msg.arg1 == 1) {
                        // send back tracking log (script)? false == 0, true == 1.
                            SugiliteStartingBlock script = sugiliteData.getScriptHead();
                            if (script != null)
                                SugiliteCommunicationController.this.sendMessage(RESPONSE, GET_SCRIPT, jsonProcessor.scriptToJson(script));
                        }
                    }
                    else {
                        //the exception message below will be sent when there's no recording in process
                        SugiliteCommunicationController.this.sendMessage(RESPONSE_EXCEPTION, STOP_RECORDING, "No recording in progress, can't stop");
                    }
                    break;

                case START_TRACKING:
                    final String trackingName = msg.getData().getString("request");
                    //commit preference change
                    SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                    sugiliteData.initiateTracking(trackingName);
                    prefEditor.putBoolean("tracking_in_process", true);
                    prefEditor.commit();
                    try {
                        sugiliteTrackingDao.save(sugiliteData.getTrackingHead());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(context, "start tracking", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Start Tracking");
                    break;

                case STOP_TRACKING:
                    if(trackingInProcess){
                        SharedPreferences.Editor prefEditor2 = sharedPreferences.edit();
                        prefEditor2.putBoolean("tracking_in_process", false);
                        prefEditor2.commit();
                        Toast.makeText(context, "end tracking", Toast.LENGTH_SHORT).show();
                        if (msg.arg1 == 1) {
                            // send back tracking log (script)? false == 0, true == 1.
                            SugiliteStartingBlock tracking = sugiliteData.getTrackingHead();
                            if (tracking != null)
                                SugiliteCommunicationController.this.sendMessage(RESPONSE, GET_SCRIPT, jsonProcessor.scriptToJson(tracking));
                        }
                    }
                    else {
                        //the exception message below will be sent when there's no recording in process
                        SugiliteCommunicationController.this.sendMessage(RESPONSE_EXCEPTION, STOP_TRACKING, "No tracking in progress, can't stop");
                    }
                    break;

                case GET_ALL_SCRIPTS:
                    sendAllScripts();
                    break;

                case GET_SCRIPT:
                    sendScript( msg.getData().getString("request") );
                    break;

                case GET_ALL_TRACKINGS:
                    sendAllTrackings();
                    break;

                case GET_TRACKING:
                    sendTracking(msg.getData().getString("request"));
                    break;


                case RUN:
                    final String scriptName = msg.getData().getString("request");
                    if(recordingInProcess) {
                        SugiliteCommunicationController.this.sendMessage(RESPONSE_EXCEPTION, RUN, "Already recording in progress, can't run");
                    }
                    else {
                        SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName + ".SugiliteScript");
                        if(script == null)
                            SugiliteCommunicationController.this.sendMessage(RESPONSE_EXCEPTION, RUN, "Can't find the script");
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

                default:
                    Log.e( TAG, "Message not supported!");
                    break;
            }
        }
    }


    public Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }


    //TODO: add run methods

}
