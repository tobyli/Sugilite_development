package edu.cmu.hcii.sugilite.communication;

        import android.app.AlertDialog;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.preference.PreferenceManager;
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
        import edu.cmu.hcii.sugilite.model.variable.StringVariable;

        import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;
        import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * This is the activty used for communicating with external apps through the Android Intent Mechanism
 */

public class SugiliteCommunicationHelper {

    // received information
    Context receivedContext;
    Intent receivedIntent;
    SugiliteData sugiliteData;

    // extracted request parameters
    String requestedMessageTypeString="";
    int messageTypeInt = 0;
    String arg1 = "", arg2 = "";

    // classes to help process the request
    SugiliteScriptDao sugiliteScriptDao;
    SugiliteBlockJSONProcessor jsonProcessor;
    SharedPreferences sharedPreferences;
    SugiliteTrackingDao sugiliteTrackingDao;
    SugiliteAppVocabularyDao vocabularyDao;
    Gson gson;

    // results
    Intent resultIntent; // in case any result needs to be returned, this intent contains it. otherwise, this is null. and null is returned.

    public SugiliteCommunicationHelper(Context receivedContext, Intent receivedIntent, SugiliteData sugiliteData)
    {
        this.receivedContext = receivedContext;
        this.receivedIntent = receivedIntent;
        this.sugiliteData = sugiliteData;

        gson = new Gson();

        // extract parameters from request, and prepare for processing
        if (receivedIntent.getExtras() != null)
        {
            requestedMessageTypeString = receivedIntent.getStringExtra("messageType");
            switch (requestedMessageTypeString){

                case "START_RECORDING":
                    messageTypeInt = Const.START_RECORDING;
                    break;
                case "END_RECORDING":
                    messageTypeInt = Const.STOP_RECORDING;
                    break;
                case "GET_SCRIPT":
                    messageTypeInt = Const.GET_RECORDING_SCRIPT;
                    break;
                case "GET_SCRIPT_LIST":
                    messageTypeInt = Const.GET_ALL_RECORDING_SCRIPTS;
                    break;
                case "ADD_JSON_AS_SCRIPT":
                    messageTypeInt = Const.ADD_JSON_AS_SCRIPT;
                    break;

                case "RUN_SCRIPT":
                    messageTypeInt = Const.RUN_SCRIPT;
                    break;
                case "RUN_JSON":
                    messageTypeInt = Const.RUN_JSON;
                    break;
                case "RUN_SCRIPT_WITH_PARAMETERS":
                    messageTypeInt = Const.RUN_SCRIPT_WITH_PARAMETERS;
                    break;

                case "END_TRACKING":
                    messageTypeInt = Const.STOP_TRACKING;
                    break;
                case "START_TRACKING":
                    messageTypeInt = Const.START_TRACKING;
                    break;
                case "GET_TRACKING": // added ...
                    messageTypeInt = Const.GET_TRACKING_SCRIPT;
                    break;
                case "GET_TRACKING_LIST":
                    messageTypeInt = Const.GET_ALL_TRACKING_SCRIPTS;
                    break;
                case "CLEAR_TRACKING_LIST":
                    messageTypeInt = Const.CLEAR_TRACKING_LIST;
                    break;
            }
            /*
            messageType, arg1, arg2
            --------------------------
            START_RECORDING, scriptName, callbackString (callbackString gets called when finish recording OR at EXCEPTION)
            STOP_RECORDING, "NULL", callbackString (... gets called with status: SUCCESS or EXCEPTION)
            RUN_SCRIPT, scriptName, callbackString (... when finish executing or EXCEPTION)
            RUN_JSON, JSON, callbackString (callbackString gets called when finish executing or EXCEPTION)
            //TODO: send call back when finish executing
            ADD_JSON_AS_SCRIPT, JSON, "NULL" //return value returned as activity result instead
            GET_RECORDING_SCRIPT, scriptName, "NULL" //return value returned as activity result instead
            GET_SCRIPT_LIST, "NULL, "NULL" //return value returned as activity result instead
            START_TRACKING, trackingName, callbackString
            END_TRACKING, "NULL", callbackString
            GET_TRACKING_SCRIPT, trackingName, "NULL"
            GET_TRACKING_LIST, "NULL", "NULL"
            CLEAR_TRACKING_LIST, "NULL", "NULL"
            GET_PACKAGE_VOCAB, "NULL", NULL" //return value returned as activity result instead
            */

            arg1 = receivedIntent.getStringExtra("arg1");
            arg2 = receivedIntent.getStringExtra("arg2");
        }

        // data structures for processing the request
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            sugiliteScriptDao = new SugiliteScriptSQLDao(this.receivedContext);
        else
            sugiliteScriptDao = new SugiliteScriptFileDao(this.receivedContext, sugiliteData);
        this.sugiliteTrackingDao = new SugiliteTrackingDao(receivedContext);
        this.vocabularyDao = new SugiliteAppVocabularyDao(receivedContext);
        this.jsonProcessor = new SugiliteBlockJSONProcessor(receivedContext);
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(receivedContext);
        //this.receivedContext = this;

        //handleRequest(messageTypeInt, arg1, arg2);

    }

    public Intent handleRequest(){
        boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
        boolean trackingInProcess = sharedPreferences.getBoolean("tracking_in_process", false);
        switch (messageTypeInt){


            ////// recording scripts related


            case Const.START_RECORDING:
                //arg1 = scriptName, arg2 = callbackString
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(recordingInProcess) {
                    //the exception message below will be sent when there's already recording in process
                    sugiliteData.sendCallbackMsg(Const.START_RECORDING_EXCEPTION, "recording already in process", arg2);
                    return(resultIntent);
                }
                else {
                    if (arg1 != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(receivedContext);
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
                                        sugiliteData.setCurrentSystemState(SugiliteData.RECORDING_STATE);

                                        try {
                                            sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        Toast.makeText(receivedContext.getApplicationContext(), "Recording new script " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();

                                        //go to home screen for recording
                                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                                        startMain.addCategory(Intent.CATEGORY_HOME);
                                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        receivedContext.startActivity(startMain);
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        dialog.show();
                    }
                }
                break;

            case Const.STOP_RECORDING:
                //arg1 = "NULL", arg2 = callbackString
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(recordingInProcess) {

                    SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                    prefEditor.putBoolean("recording_in_process", false);
                    prefEditor.commit();
                    if(sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null)
                        sugiliteData.sendCallbackMsg(Const.FINISHED_RECORDING, jsonProcessor.scriptToJson(sugiliteData.getScriptHead()), arg2);

                    Toast.makeText(receivedContext, "recording ended", Toast.LENGTH_SHORT).show();
                    sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                    setReturnValue("");
                }
                else {
                    //the exception message below will be sent when there's no recording in process
                    sugiliteData.sendCallbackMsg(Const.END_RECORDING_EXCEPTION, "no recording in process", arg2);
                    return(resultIntent);
                }
                break;

            case Const.GET_RECORDING_SCRIPT:
                //arg1 = scriptName, arg2 = "NULL"
            {
                SugiliteStartingBlock script = null;
                try {
                    script = sugiliteScriptDao.read(arg1 + ".SugiliteScript");
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                if(script != null) {
                    setReturnValue(jsonProcessor.scriptToJson(script));
                    return(resultIntent);
                }
                // else
                //the exception message below will be sent when can't find a script with provided name
                //TODO: send exception message
                break;
            }

            case Const.GET_ALL_RECORDING_SCRIPTS:
                //arg1 = scriptName, arg2 = "NULL"
                List<String> allNames = new ArrayList<>();
                try {
                    allNames = sugiliteScriptDao.getAllNames();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                List<String> retVal = new ArrayList<>();
                for(String name : allNames)
                    retVal.add(name.replace(".SugiliteScript", ""));

                setReturnValue(new Gson().toJson(retVal));
                return(resultIntent);

            case Const.ADD_JSON_AS_SCRIPT:
                //arg1 = JSON, arg2 = "NULL"
                if(arg1 != null){
                    try{
                        SugiliteStartingBlock script = jsonProcessor.jsonToScript(arg1);
                        if(!script.getScriptName().contains(".SugiliteScript"))
                            script.setScriptName(script.getScriptName() + ".SugiliteScript");
                        sugiliteScriptDao.save(script);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        sugiliteData.sendCallbackMsg(Const.ADD_JSON_AS_SCRIPT_EXCEPTION, "error in json parsing", arg2);
                    }

                }
                else {
                    sugiliteData.sendCallbackMsg(Const.ADD_JSON_AS_SCRIPT, "null json", arg2);
                }
                break;


            /////// running scripts


            //TODO: add run script with parameter
            //TODO: get a script and its parameter & alternative value lists
            case Const.RUN_SCRIPT:
                //arg1 = scriptName, arg2 = callbackString
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(recordingInProcess) {
                    sugiliteData.sendCallbackMsg(Const.RUN_SCRIPT_EXCEPTION, "recording already in process", arg2);
                    return(resultIntent);
                }
                else {
                    //run the script
                    SugiliteStartingBlock script = null;
                    try {
                        script = sugiliteScriptDao.read(arg1 + ".SugiliteScript");
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                    if(script == null) {
                        sugiliteData.sendCallbackMsg(Const.RUN_SCRIPT_EXCEPTION, "null script", arg2);
                        return(resultIntent);
                    }
                    else {
                        runScript(script, null);
                    }
                }
                break;

            case Const.RUN_SCRIPT_WITH_PARAMETERS:
                /**
                 * arg1 = JSON in the format:
                 * {
                 *  scriptName: SCRIPT_NAME,
                 *  variables:[
                 *      {name: PARAMETER_NAME1, value: PARAMETER_VALUE1},
                 *      {name: PARAMETER_NAME2, value: PARAMETER_VALUE2},
                 *      ...
                 *   ]
                 * }
                 *
                 * arg2 = callbackString
                 */
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(recordingInProcess) {
                    sugiliteData.sendCallbackMsg(Const.RUN_SCRIPT_EXCEPTION, "recording already in process", arg2);
                    return(resultIntent);
                }
                else {
                    //run the script
                    try {
                        RunScriptWithParametersWrapper parametersWrapper = gson.fromJson(arg1, RunScriptWithParametersWrapper.class);
                        SugiliteStartingBlock script = sugiliteScriptDao.read(parametersWrapper.scriptName + ".SugiliteScript");

                        if (script == null) {
                            sugiliteData.sendCallbackMsg(Const.RUN_SCRIPT_EXCEPTION, "null script", arg2);
                            return(resultIntent);
                        } else {
                            runScript(script, parametersWrapper.variables);
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;

            case Const.RUN_JSON:
                //arg1 = JSON, arg2 = callbackString
                if(arg2 != null)
                    sugiliteData.callbackString = new String(arg2);
                if(arg1 != null){
                    try{
                        SugiliteStartingBlock script = jsonProcessor.jsonToScript(arg1);
                        runScript(script, null);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        sugiliteData.sendCallbackMsg(Const.RUN_JSON_EXCEPTION, "error in json parsing", arg2);
                        return(resultIntent);
                    }
                }
                else {
                    sugiliteData.sendCallbackMsg(Const.RUN_JSON_EXCEPTION, "null json", arg2);
                    return(resultIntent);
                }
                break;


            ////// tracking


            case Const.START_TRACKING:
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
                setReturnValue("");
                Toast.makeText(receivedContext, "tracking started", Toast.LENGTH_SHORT).show();
                break;

            case Const.STOP_TRACKING:
                if(trackingInProcess) {
                    SharedPreferences.Editor prefEditor2 = sharedPreferences.edit();
                    prefEditor2.putBoolean("tracking_in_process", false);
                    prefEditor2.commit();
                    Toast.makeText(receivedContext, "tracking ended", Toast.LENGTH_SHORT).show();
                    setReturnValue("");
                }
                break;

            case Const.GET_TRACKING_SCRIPT:
                SugiliteStartingBlock tracking = sugiliteTrackingDao.read(arg1);
                if(tracking != null){
                    setReturnValue(jsonProcessor.scriptToJson(tracking));
                    return(resultIntent);
                }
                // else
                //the exception message below will be sent when can't find a script with provided name
                //TODO: send exception message
                break;

            case Const.GET_ALL_TRACKING_SCRIPTS:
                List<String> allTrackingNames = sugiliteTrackingDao.getAllNames();
                List<String> trackingRetVal = new ArrayList<>();
                for(String name : allTrackingNames)
                    trackingRetVal.add(name);
                setReturnValue(new Gson().toJson(trackingRetVal));
                return(resultIntent);

            case Const.CLEAR_TRACKING_LIST:
                sugiliteTrackingDao.clear();
                setReturnValue("");
                break;


            ////// misc


            case Const.GET_ALL_PACKAGE_VOCAB:
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
                    setReturnValue(retVal2);
                }
                else{
                    setReturnValue("NULL");
                }
                break;

            case Const.GET_PACKAGE_VOCAB:
                Set<String> vocabSet = null;
                if(arg1 != null) {
                    try {
                        vocabSet = vocabularyDao.getText(arg1);
                        setReturnValue(gson.toJson(vocabSet));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else {
                    //TODO: send exception
                }
        }

        return(null);
    }

    private void runScript(SugiliteStartingBlock script, List<VariableWrapper> variables){

        sugiliteData.clearInstructionQueue();
        final ServiceStatusManager serviceStatusManager = ServiceStatusManager.getInstance(receivedContext);

        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessiblity service is not active
            AlertDialog.Builder builder1 = new AlertDialog.Builder(receivedContext);
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

            if(variables != null){
                //put in the values for the variables
                for(VariableWrapper variable: variables){
                    if(sugiliteData.stringVariableMap.containsKey(variable.name)){
                        sugiliteData.stringVariableMap.put(variable.name, new StringVariable(variable.name, variable.value));
                    }
                }
            }

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
                Thread.sleep(SCRIPT_DELAY);
            } catch (Exception e) {
                // do nothing
            }
            //go to home screen for running the automation
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            receivedContext.startActivity(startMain);
        }

    }

    private void setReturnValue(String retVal){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", retVal);
        returnIntent.putExtra("messageType", requestedMessageTypeString);

        this.resultIntent = returnIntent;
    }

    class RunScriptWithParametersWrapper {
        public String scriptName;
        public List<VariableWrapper> variables;
        public RunScriptWithParametersWrapper(String scriptName, List<VariableWrapper> variables){
            this.scriptName = scriptName;
            this.variables = variables;
        }
    }

    class VariableWrapper {
        public String name;
        public String value;
        public VariableWrapper(String name, String value){
            this.name = name;
            this.value = value;
        }
    }
}