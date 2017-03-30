package edu.cmu.hcii.sugilite.communication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteAppVocabularyDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTrackingDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * This is the controller used for communicating with InMind Middleware
 *
 * Created by oscarr on 7/7/16.
 */
public class SugiliteCommunicationController {
    private static SugiliteCommunicationController instance;
    private final String TAG = SugiliteCommunicationController.class.getName();
    SugiliteScriptDao sugiliteScriptDao;
    SugiliteTrackingDao sugiliteTrackingDao;
    SugiliteAppVocabularyDao vocabularyDao;
    SugiliteBlockJSONProcessor jsonProcessor;
    SugiliteData sugiliteData;
    Activity activity;
    private Context context; //NOTE: application receivedContext
    private String message;
    private SharedPreferences sharedPreferences;
    private List<SugiliteMessageListener> subscribers;
    //TODO: CHANGE SugiliteData.currentSystemStatus for external actions

    private SugiliteCommunicationController( Context context, SugiliteData sugiliteData, Activity activity ) {
        this(context, sugiliteData, PreferenceManager.getDefaultSharedPreferences(context) );
        this.activity = activity;
    }

    private SugiliteCommunicationController( Context context, Activity activity ) {
        this(context, new SugiliteData(), PreferenceManager.getDefaultSharedPreferences(context) );
        this.activity = activity;
    }

    private SugiliteCommunicationController(Context context, SugiliteData sugiliteData,
                                            SharedPreferences sharedPreferences) {
        this.context = context.getApplicationContext();
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            sugiliteScriptDao = new SugiliteScriptSQLDao(this.context);
        else
            sugiliteScriptDao = new SugiliteScriptFileDao(this.context, sugiliteData);
        this.vocabularyDao = new SugiliteAppVocabularyDao(this.context);
        this.sugiliteTrackingDao = new SugiliteTrackingDao(this.context);
        this.jsonProcessor = new SugiliteBlockJSONProcessor(this.context);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.subscribers = new ArrayList<>();
    }


    public static SugiliteCommunicationController getInstance(){
        return instance;
    }

    public static SugiliteCommunicationController getInstance(Context context, SugiliteData sugiliteData,
                                                       SharedPreferences sharedPreferences) {
        if( instance == null ){
            instance = new SugiliteCommunicationController(context, sugiliteData, sharedPreferences);
        }
        return instance;
    }

    public static SugiliteCommunicationController getInstance(Context context, Activity activity){
        if( instance == null ){
            instance = new SugiliteCommunicationController(context, activity);
        }
        return instance;
    }

    public static SugiliteCommunicationController getInstance(Context context, SugiliteData sugiliteData,
                                                              Activity activity){
        if( instance == null ){
            instance = new SugiliteCommunicationController(context, sugiliteData, activity);
        }
        return instance;
    }

    public void setActivity(Activity activity) {
        if( this.activity == null || ( this.activity != null && activity != null ) ) {
            this.activity = activity;
        }
    }

    public boolean isTrackingInProcess() {
        return sharedPreferences.getBoolean("tracking_in_process", false);
    }

    public boolean isRecordingInProcess() {
        return sharedPreferences.getBoolean("recording_in_process", false);
    }

    /**
     * This method is called from outside (i.e., from Middleware)
     * @param subcriber
     */
    public void addSubscriber(SugiliteMessageListener subcriber){
        subscribers.add( subcriber );
    }

    public boolean removeSubscriber(SugiliteMessageListener subcriber){
        return subscribers.remove( subcriber );
    }

    public String processMultipurposeRequest(String json) {
        Log.d(TAG, "Request received: processMultipurposeRequest");
        //TODO: process json object
        return "After processing json: " + json;
    }

    public Set<String> getPackageVocab(String packageName) {
        Log.d(TAG, "Request received: getPackageVocab");
        Set<String> vocabSet = null;
        if(packageName != null) {
            try {
                vocabSet = vocabularyDao.getText(packageName);
                Gson gson = new Gson();
                sendMessage(Const.RESPONSE, Const.GET_PACKAGE_VOCAB, gson.toJson(vocabSet));
                return vocabSet;
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        else {
            //TODO: send exception
        }
        return null;
    }

    public String getAllPackageVocab() {
        Log.d(TAG, "Request received: getAllPackageVocab");
        Map<String, Set<String>> appVocabMap =  null;
        try {
            appVocabMap = vocabularyDao.getTextsForAllPackages();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        if(appVocabMap != null && appVocabMap.size() > 0){
            String retVal2 = "";
            for(Map.Entry<String, Set<String>> entry : appVocabMap.entrySet()){
                for(String text : entry.getValue()){
                    retVal2 += entry.getKey() + ": " + text + "\n";
                }
            }
            sendMessage(Const.RESPONSE, Const.GET_ALL_PACKAGE_VOCAB, retVal2);
            return retVal2;
        }
        else{
            sendMessage(Const.RESPONSE, Const.GET_ALL_PACKAGE_VOCAB, "NULL");
        }
        return null;
    }

    public void clearTrackingList() {
        Log.d(TAG, "Request received: clearTrackingList");
        sugiliteTrackingDao.clear();
        sendMessage(Const.RESPONSE, Const.CLEAR_TRACKING_LIST, "");
    }


    public boolean sendAllScripts(){
        Log.d(TAG, "Sending All Recording Scripts");
        return sendMessage( Const.RESPONSE, Const.GET_ALL_RECORDING_SCRIPTS, jsonProcessor
                .scriptsToJson( getRecordingScripts() ));
    }

    public List<SugiliteStartingBlock> getRecordingScripts(){
        Log.d(TAG, "Request received: getRecordingScripts");
        List<String> allNames = new ArrayList<>();
        try {
            allNames = sugiliteScriptDao.getAllNames();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        List<SugiliteStartingBlock> startingBlocks = new ArrayList<>();
        for(String name : allNames) {
            try {
                startingBlocks.add(sugiliteScriptDao.read(name));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return startingBlocks;
    }

    public boolean sendAllTrackings(){
        Log.d(TAG, "Sending All Tracking Scripts");
        return sendMessage( Const.RESPONSE, Const.GET_ALL_TRACKING_SCRIPTS, jsonProcessor
                .scriptsToJson( getTrackingScripts() ));
    }

    public List<SugiliteStartingBlock> getTrackingScripts(){
        Log.d(TAG, "Request received: getTrackingScripts");
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
        return startingBlocks;
    }

    public boolean sendScript(String scriptName){
        Log.d(TAG, "Sending Recording Script");
        // you should send back the script which name is "scriptName"... now, we are using a dummy
        SugiliteStartingBlock script = getRecordingScript(scriptName);
        if(script != null) {
            return sendMessage(Const.RESPONSE, Const.GET_RECORDING_SCRIPT, jsonProcessor.scriptToJson(script));
        }else {
            //the exception message below will be sent when can't find a script with provided name
            return sendMessage(Const.RESPONSE_EXCEPTION, Const.GET_RECORDING_SCRIPT,
                    "Can't find a script with provided name");
        }
    }

    public SugiliteStartingBlock getRecordingScript(String scriptName){
        Log.d(TAG, "Request received: getRecordingScript");
        try {
            return sugiliteScriptDao.read(scriptName + ".SugiliteScript");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public boolean sendTracking(String trackingName){
        Log.d(TAG, "Sending Tracking Script");
        // you should send back the script which name is "scriptName"... now, we are using a dummy
        SugiliteStartingBlock tracking = getTrackingScript( trackingName );
        if(tracking != null)
            return sendMessage(Const.RESPONSE, Const.GET_TRACKING_SCRIPT, jsonProcessor.scriptToJson(tracking));
        else
            //the exception message below will be sent when can't find a script with provided name
            return sendMessage(Const.RESPONSE_EXCEPTION, Const.GET_TRACKING_SCRIPT,
                    "Can't find a tracking with provided name");
    }

    public SugiliteStartingBlock getTrackingScript(String scriptName){
        Log.d(TAG, "Request received: getTrackingScript");
        return sugiliteTrackingDao.read(scriptName);
    }

    //the below message will be sent when a externally initiated script has finished recording
    public boolean sendRecordingFinishedSignal(String scriptName){
        return sendMessage(Const.RESPONSE, Const.STOP_RECORDING, "FINISHED RECORDING " + scriptName);
    }

    public boolean sendExecutionFinishedSignal(String scriptName){
        return sendMessage(Const.RESPONSE, Const.RUN, "FINISHED EXECUTING " + scriptName);
    }


    public boolean sendMessage(int messageType, int arg2, String obj){
        processSubscribers( arg2, obj);
        return true;
    }

    private void processSubscribers(int messageType, String obj) {
        for(SugiliteMessageListener subscriber : subscribers ){
            subscriber.onReceiveMessage( messageType, obj);
        }
    }

    //TODO: modify the "scriptName" field to use a JSON instead
    /*
    {
        "scriptName" : [SCRIPT_NAME],
        "variableValues" : {
        [VARIABLE_NAME_1] : [VARIABLE_VALUE_1],
        [VARIABLE_NAME_2] : [VARIABLE_VALUE_2],
        ...
        [VARIABLE_NAME_N] : [VARIABLE_VALUE_N]
        }
    }
    */
    public String startRecording(Boolean sendCallback, String callbackString, final String scriptName,
                                 final Boolean shouldUseToast) {
        Log.d(TAG, "Request received: startRecording");
        if( isRecordingInProcess() ) {
            //the exception message below will be sent when there's already recording in process
            message = "Already recording in progress, can't start";
            SugiliteCommunicationController.this.sendMessage(Const.RESPONSE_EXCEPTION,
                    Const.START_RECORDING, message);
            if( sendCallback ){
                sugiliteData.sendCallbackMsg(Const.START_RECORDING_EXCEPTION,
                        "recording already in process", callbackString);
            }
        }
        else {
            //NOTE: script name should be specified in msg.getData().getString("request");
            if (scriptName != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        message = "Now start recording new script " + scriptName;
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("New Recording")
                                .setMessage(message)
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
                                            sugiliteScriptDao.commitSave();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        if( shouldUseToast ) {
                                            Toast.makeText(context, "Recording new script " +
                                                            sharedPreferences.getString("scriptName", "NULL"),
                                                    Toast.LENGTH_SHORT).show();
                                        }

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
                        Log.d(TAG, "Start Recording");
                    }
                });
            }else{
                message = "Script name is null";
            }
        }
        return message;
    }

    public SugiliteStartingBlock stopRecording(Boolean sendCallback, String callbackString,
                                               int sendTracking, Boolean shouldUseToast) {
        Log.d(TAG, "Request received: stopRecording");
        boolean recordingInProcess = isRecordingInProcess();
        if(recordingInProcess) {
            SharedPreferences.Editor prefEditor = sharedPreferences.edit();
            prefEditor.putBoolean("recording_in_process", false);
            prefEditor.commit();
            if(sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null) {
                sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());
                if (sendCallback) {
                    sugiliteData.sendCallbackMsg(Const.FINISHED_RECORDING, jsonProcessor
                            .scriptToJson(sugiliteData.getScriptHead()), callbackString);
                }
            }
            if( shouldUseToast ) {
                Toast.makeText(context, "end recording", Toast.LENGTH_SHORT).show();
            }
            if (sendTracking == 1) {
                // send back tracking log (script)? false == 0, true == 1.
                SugiliteStartingBlock script = sugiliteData.getScriptHead();
                if (script != null)
                    SugiliteCommunicationController.this.sendMessage(Const.RESPONSE,
                            Const.GET_RECORDING_SCRIPT, jsonProcessor.scriptToJson(script));
                return script;
            }
        }
        else {
            //the exception message below will be sent when there's no recording in process
            SugiliteCommunicationController.this.sendMessage(Const.RESPONSE_EXCEPTION,
                    Const.STOP_RECORDING, "No recording in progress, can't stop");
            if( sendCallback ){
                sugiliteData.sendCallbackMsg(Const.END_RECORDING_EXCEPTION,
                        "no recording in process", callbackString);
            }
        }
        return null;
    }

    public String startTracking(String trackingName, Boolean shouldUseToast) {
        Log.d(TAG, "Request received: startTracking");
        if( !isTrackingInProcess() ){
            //commit preference change
            SharedPreferences.Editor prefEditor = sharedPreferences.edit();
            sugiliteData.initiateTracking(trackingName);
            prefEditor.putBoolean("tracking_in_process", true);
            prefEditor.commit();
            try {
                sugiliteTrackingDao.save(sugiliteData.getTrackingHead());
                message = "Start Tracking";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            message = "Already tracking in progress, can't start";
        }
        if( shouldUseToast ) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, message);
        return message;
    }

    public SugiliteStartingBlock stopTracking( int sendTracking, Boolean shouldUseToast) {
        Log.d(TAG, "Request received: stopTracking");
        if( isTrackingInProcess() ){
            SharedPreferences.Editor prefEditor2 = sharedPreferences.edit();
            prefEditor2.putBoolean("tracking_in_process", false);
            prefEditor2.commit();
            if( shouldUseToast ) {
                Toast.makeText(context, "end tracking", Toast.LENGTH_SHORT).show();
            }
            if (sendTracking == 1) {
                // send back tracking log (script)? false == 0, true == 1.
                SugiliteStartingBlock tracking = sugiliteData.getTrackingHead();
                if (tracking != null)
                    SugiliteCommunicationController.this.sendMessage(Const.RESPONSE,
                            Const.GET_TRACKING_SCRIPT, jsonProcessor.scriptToJson(tracking));
                return tracking;
            }
        }
        else {
            //the exception message below will be sent when there's no recording in process
            SugiliteCommunicationController.this.sendMessage(Const.RESPONSE_EXCEPTION,
                    Const.STOP_TRACKING, "No tracking in progress, can't stop");
        }
        return null;
    }

    public SugiliteStartingBlock addJsonAsScript(String json, Boolean sendCallback, String callbackString){
        Log.d(TAG, "Request received: addJsonAsScript");
        if(json != null){
            try{
                SugiliteStartingBlock script = jsonProcessor.jsonToScript(json);
                Log.d("mtemp",script.toString());
                Log.d("mtemp",script.getNextBlock().getDescription());
                if(!script.getScriptName().contains(".SugiliteScript"))
                    script.setScriptName(script.getScriptName() + ".SugiliteScript");
                sugiliteScriptDao.save(script);
                sugiliteScriptDao.commitSave();
                return script;
            }
            catch (Exception e){
                e.printStackTrace();
                sugiliteData.sendCallbackMsg(Const.ADD_JSON_AS_SCRIPT_EXCEPTION,
                        "error in json parsing", callbackString);
            }
        }
        else if( sendCallback ){
            sugiliteData.sendCallbackMsg(Const.ADD_JSON_AS_SCRIPT, "null json", callbackString);
        }
        return null;
    }

    public SugiliteStartingBlock runJson(String jsonScript, Boolean sendCallback, String callbackString) {
        Log.d(TAG, "Request received: runJson");
        if(jsonScript != null){
            try {
                Log.d("my_tag", jsonScript);
                SugiliteStartingBlock script = jsonProcessor.jsonToScript(jsonScript);
                runScript(script);
                return script;
            }
            catch (Exception e){
                e.printStackTrace();
                sugiliteData.sendCallbackMsg(Const.RUN_JSON_EXCEPTION,
                        "error in json parsing", callbackString);
            }
        }
        else if( sendCallback ){
            sugiliteData.sendCallbackMsg(Const.RUN_JSON_EXCEPTION, "null json", callbackString);
        }

        return null;
    }

    public SugiliteStartingBlock runScript(String scriptName, Boolean sendCallback, String callbackString) {
        Log.d(TAG, "Request received: runScript");
        boolean recordingInProcess = isRecordingInProcess();
        if(recordingInProcess) {
            SugiliteCommunicationController.this.sendMessage(Const.RESPONSE_EXCEPTION,
                    Const.RUN, "Already recording in progress, can't run");
            if( sendCallback ){
                sugiliteData.sendCallbackMsg(Const.RUN_SCRIPT_EXCEPTION,
                        "recording already in process", callbackString);
            }
        }
        else {
            SugiliteStartingBlock script = null;
            try {
                script = sugiliteScriptDao.read(scriptName +
                        ".SugiliteScript");
            }
            catch (Exception e){
                e.printStackTrace();
            }
            if(script == null) {
                SugiliteCommunicationController.this.sendMessage(Const.RESPONSE_EXCEPTION,
                        Const.RUN, "Can't find the script");
                if( sendCallback ){
                    sugiliteData.sendCallbackMsg(Const.RUN_SCRIPT_EXCEPTION,
                            "null script", callbackString);
                }
            }
            sugiliteData.clearInstructionQueue();
            final ServiceStatusManager serviceStatusManager = ServiceStatusManager.getInstance(context);

            if(!serviceStatusManager.isRunning()){
                //prompt the user if the accessiblity service is not active
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                        builder1.setTitle("Service not running")
                                .setMessage("The Sugilite accessiblity service is not enabled. " +
                                        "Please enable the service in the phone settings before recording.")
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
                });
            }
            else if( script != null ){
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
                sugiliteData.runScript(script, null, SugiliteData.EXECUTION_STATE);
                try {
                    Thread.sleep( Const.SCRIPT_DELAY);
                } catch (Exception e) {
                    // do nothing
                }
                //go to home screen for running the automation
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                context.startActivity(startMain);
            }
            return script;
        }
        return null;
    }


    private void runScript(SugiliteStartingBlock script){
        Log.d(TAG, "Request received: runScript");
        sugiliteData.clearInstructionQueue();
        final ServiceStatusManager serviceStatusManager = ServiceStatusManager.getInstance(context);
        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessiblity service is not active
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                    builder1.setTitle("Service not running")
                            .setMessage("The Sugilite accessiblity service is not enabled. " +
                                    "Please enable the service in the phone settings before recording.")
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
            });
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
            sugiliteData.runScript(script, false, SugiliteData.EXECUTION_STATE);
            try {
                Thread.sleep( Const.SCRIPT_DELAY);
            } catch (Exception e) {
                // do nothing
            }
            //go to home screen for running the automation
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startMain);
        }
    }
}
