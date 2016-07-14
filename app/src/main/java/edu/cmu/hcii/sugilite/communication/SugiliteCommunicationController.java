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

import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.communication.json.SugiliteBlockJSON;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * Created by oscarr on 7/7/16.
 */
public class SugiliteCommunicationController {
    private final String TAG = SugiliteCommunicationController.class.getName();
    private Messenger sender = null; //used to make an RPC invocation
    private Messenger receiver = null; //invocation replies are processed by this Messenger (Middleware)
    private boolean isBound = false;
    SugiliteScriptDao sugiliteScriptDao;
    SugiliteBlockJSONProcessor jsonProcessor;
    SugiliteData sugiliteData;
    private ServiceConnection connection; //receives callbacks from bind and unbind invocations
    private Context context;
    private final int REGISTER = 1;
    private final int UNREGISTER = 2;
    private final int RESPONSE = 3;
    private final int START_TRACKING = 4;
    private final int STOP_TRACKING = 5;
    private final int GET_ALL_SCRIPTS = 6;
    private final int GET_SCRIPT = 7;
    private final int RESPONSE_EXCEPTION = 8;
    private final int APP_TRACKER_ID = 1001;
    private SharedPreferences sharedPreferences;



    public SugiliteCommunicationController(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences) {
        this.connection = new RemoteServiceConnection();
        this.receiver = new Messenger(new IncomingHandler());
        this.context = context.getApplicationContext();
        this.sugiliteScriptDao = new SugiliteScriptDao(context);
        this.jsonProcessor = new SugiliteBlockJSONProcessor(context);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
    }

    public void start(){
        Intent intent = createExplicitFromImplicitIntent( context,
                new Intent( "com.yahoo.inmind.services.generic.control.ExternalAppCommService" ) );
        context.bindService(intent, this.connection, Context.BIND_AUTO_CREATE);
    }

    public void stop(){
        if (this.isBound) {
            context.unbindService(connection);
            this.isBound = false;
        }
    }

    public boolean register(){
        return sendMessage(REGISTER, 0, null);
    }

    public boolean unregister(){
        return sendMessage(UNREGISTER, 0, null);
    }

    public boolean sendAllScripts(){
        return sendMessage( RESPONSE, GET_ALL_SCRIPTS, new Gson().toJson(sugiliteScriptDao.getAllNames()));
    }

    public boolean sendScript(String scriptName){
        // you should send back the script which name is "scriptName"... now, we are using a dummy
        SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName + ".SugiliteScript");
        if(script != null)
            return sendMessage(RESPONSE, GET_SCRIPT, jsonProcessor.scriptToJson(script));
        else
            return sendMessage(RESPONSE_EXCEPTION, GET_SCRIPT, "Can't find a script with provided name");
    }

    public boolean sendRecordingFinishedSignal(String scriptName){
        return sendMessage( RESPONSE, START_TRACKING, "FINISHED" + scriptName);
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
            switch(msg.what) {
                case START_TRACKING:
                    //TODO: start app tracking service
                    if(recordingInProcess) {
                        SugiliteCommunicationController.this.sendMessage(RESPONSE_EXCEPTION, START_TRACKING, "Already recording in process, can't start");
                    }
                    else {
                        final String scriptName = msg.getData().getString("request");
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
                                            sugiliteScriptDao.save((SugiliteStartingBlock) sugiliteData.getScriptHead());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        Toast.makeText(context, "Recording new script " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        dialog.show();
                    }
                    Log.d( TAG, "Start Tracking");
                    break;
                case STOP_TRACKING:
                    //TODO: stop app tracking service
                    if(recordingInProcess) {
                        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                        prefEditor.putBoolean("recording_in_process", false);
                        prefEditor.commit();
                        if(sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null)
                            sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());
                        Toast.makeText(context, "end recording", Toast.LENGTH_SHORT).show();
                        if (msg.arg1 == 1) { // send back tracking log (script)? false == 0, true == 1.
                            //TODO: replace getDummyScript by the corresponding script
                            SugiliteStartingBlock script = sugiliteData.getScriptHead();
                            if (script != null)
                                SugiliteCommunicationController.this.sendMessage(RESPONSE, GET_SCRIPT, jsonProcessor.scriptToJson(script));
                        }
                    }
                    else {
                        SugiliteCommunicationController.this.sendMessage(RESPONSE_EXCEPTION, STOP_TRACKING, "No recording in progress, can't stop");
                    }
                    break;
                case GET_ALL_SCRIPTS:
                    sendAllScripts();
                    break;
                case GET_SCRIPT:
                    sendScript( msg.getData().getString("request") );
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



}
