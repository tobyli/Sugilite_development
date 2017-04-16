package edu.cmu.hcii.sugilite;

import android.graphics.Color;

/**
 * @author toby
 * @date 10/25/16
 * @time 3:55 PM
 */
public class Const {
    //TRUE to save all the clickable items to the "app vocabulary" when recording
    public static final boolean BUILDING_VOCAB = false;

    //TRUE to enable broadcasting of accessiblity event (needed by HELPR system)
    public static final boolean BROADCASTING_ACCESSIBILITY_EVENT = true;

    //delay before running a script after clicking on OK in the VariableSetValueDialog
    public static final int SCRIPT_DELAY = 5000;

    //delay before executing each operation block in Automator
    public static final int DELAY = 4000;
    public static final int DEBUG_DELAY = 8000;

    public static final long THRESHOLD_FOR_START_SENDING_ACCESSIBILITY_EVENT = 7000;

    //TRUE to save a list of all elements with text labels
    public static final boolean KEEP_ALL_TEXT_LABEL_LIST = false;

    //TRUE to save all available nodes in the feature pack
    public static final boolean KEEP_ALL_NODES_IN_THE_FEATURE_PACK = false;

    //TRUE to save all alternative clickables in the filter
    public static final boolean KEEP_ALL_ALTERNATIVES_IN_THE_FILTER = false;


    //contain package names for launchers so they won't be killed
    public static final String[] HOME_SCREEN_PACKAGE_NAMES = {"com.google.android.googlequicksearchbox", "com.google.android.apps.nexuslauncher"};

    //package names to excluded for recording & tracking
    public static final String[] ACCESSIBILITY_SERVICE_EXCEPTED_PACKAGE_NAMES = {"edu.cmu.hcii.sugilite", "com.android.systemui", "com.google.android.inputmethod.pinyin", "edu.cmu.hcii.sugilitecommunicationtest", "edu.cmu.helpr"};
    public static final String[] ACCESSIBILITY_SERVICE_TRACKING_EXCLUDED_PACKAGE_NAMES = {"edu.cmu.hcii.sugilitecommunicationtest", "edu.cmu.hcii.sugilite", "edu.cmu.helpr"};


    //App name to display
    public static final String appName = "Sugilite";
    public static final String appNameUpperCase = "SUGILITE";

    public static final int ID_APP_TRACKER = 1001;
    public static final String APP_TRACKER = "APP_TRACKER";
    public static final String SCRIPT_NAME = "SCRIPT_NAME";
    public static final String CALLBACK_STRING = "CALLBACK_STRING";
    public static final String SCRIPT = "SCRIPT";
    public static final String PACKAGE_NAME = "PACKAGE_NAME";
    public static final String JSON_STRING = "JSON_STRING";
    public static final String SHOULD_SEND_CALLBACK ="SHOULD_SEND_CALLBACK";

    //const code for communicating with InMind Middleware
    public static final int REGISTER = 1;
    public static final int UNREGISTER = 2;
    public static final int RESPONSE = 3;
    public static final int START_TRACKING = 4;
    public static final int STOP_TRACKING = 5;
    public static final int GET_ALL_TRACKING_SCRIPTS = 6;
    public static final int GET_TRACKING_SCRIPT = 7;
    public static final int APP_TRACKER_EXCEPTION = 8;
    public static final int RUN = 9;
    public static final int RESPONSE_EXCEPTION = 10;
    public static final int START_RECORDING = 11;
    public static final int STOP_RECORDING = 12;
    public static final int GET_ALL_RECORDING_SCRIPTS = 13;
    public static final int GET_RECORDING_SCRIPT = 14;
    public static final int ACCESSIBILITY_EVENT = 15;
    public static final int RUN_SCRIPT = 16;
    public static final int END_RECORDING_EXCEPTION = 17;
    public static final int START_RECORDING_EXCEPTION = 18;
    public static final int FINISHED_RECORDING = 19;
    public static final int RUN_SCRIPT_EXCEPTION = 20;
    public static final int RUN_JSON = 21;
    public static final int RUN_JSON_EXCEPTION = 22;
    public static final int ADD_JSON_AS_SCRIPT = 23;
    public static final int ADD_JSON_AS_SCRIPT_EXCEPTION = 24;
    public static final int CLEAR_TRACKING_LIST = 25;
    public static final int GET_ALL_PACKAGE_VOCAB = 26;
    public static final int GET_PACKAGE_VOCAB = 27;
    public static final int MULTIPURPOSE_REQUEST = 28;
    public static final int RUN_SCRIPT_WITH_PARAMETERS = 29;


    //colors
    public static final String SCRIPT_ACTION_COLOR = "#ffa500", SCRIPT_ACTION_PARAMETER_COLOR = "#bc002f", SCRIPT_TARGET_TYPE_COLOR = "#36a095"
            , SCRIPT_IDENTIFYING_FEATURE_COLOR = "#008400", SCRIPT_VIEW_ID_COLOR = "#800080", SCRIPT_WITHIN_APP_COLOR = "#ff00ff", SCRIPT_LINK_COLOR = "#2e159f";

    public static final int SEMI_TRANSPARENT_GRAY_BACKGROUND = Color.parseColor("#80000000");


    public static final String LOADING_MESSAGE = "Loading the script...\n", SAVING_MESSAGE = "Saving the script... \n";


    public static final int FILE_SCRIPT_DAO = 1, SQL_SCRIPT_DAO = 2;

    //switch between using SQL Dao and File Dao (SQL Dao has a max script size issue)
    public static final int DAO_TO_USE = FILE_SCRIPT_DAO;
}
