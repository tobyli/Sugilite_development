package edu.cmu.hcii.sugilite;

import android.graphics.Color;
import android.os.Build;
import android.view.WindowManager;

import java.text.SimpleDateFormat;

/**
 * @author toby
 * @date 10/25/16
 * @time 3:55 PM
 */
public class Const {
    public static final int OVERLAY_TYPE = (Build.VERSION.SDK_INT >= 26) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

    //TRUE to save all the clickable items to the "app vocabulary" when recording
    public static final boolean BUILDING_VOCAB = false;

    //TRUE to enable broadcasting of accessiblity event (needed by HELPR system)
    public static final boolean BROADCASTING_ACCESSIBILITY_EVENT = false;

    //TRUE to enable root functions
    public static final boolean ROOT_ENABLED = true;

    //delay before running a script after clicking on OK in the VariableSetValueDialog
    public static final int SCRIPT_DELAY = 5000;

    //delay before executing each operation block in Automator
    public static final int DELAY = 1000;
    public static final int DEBUG_DELAY = 8000;

    //interval for error checking in accessibility service
    public static final int INTERVAL_ERROR_CHECKING_ACCESSIBILITY_SERVICE = 2000;

    //interval for refreshing ui snapshot
    public static final int INTERVAL_REFRESH_UI_SNAPSHOT = 1000;

    //interval for refreshing Sugilite icon
    public static final int INTERVAL_REFRESH_SUGILITE_ICON = 1000;


    public static final long THRESHOLD_FOR_START_SENDING_ACCESSIBILITY_EVENT = 500;

    //TRUE to save a list of all elements with text labels
    public static final boolean KEEP_ALL_TEXT_LABEL_LIST = false;

    //TRUE to save all available nodes in the feature pack
    public static final boolean KEEP_ALL_NODES_IN_THE_FEATURE_PACK = false;

    //TRUE to save all alternative clickables in the filter
    public static final boolean KEEP_ALL_ALTERNATIVES_IN_THE_FILTER = false;

    //TRUE to allow simultaneous verbal instruction + demonstration
    public static final boolean ENABLE_SIMULTANEOUS_INSTRUCTION_AND_DEMONSTRATION = false;

    //# of thread for parsing/annotating string entities in UISnapshot
    public static final int UI_SNAPSHOT_TEXT_PARSING_THREAD_COUNT = 5;


    //contain package names for launchers so they won't be killed
    public static final String[] HOME_SCREEN_PACKAGE_NAMES = {"com.google.android.googlequicksearchbox", "com.google.android.apps.nexuslauncher"};

    //package names to excluded for recording & tracking
    public static final String[] ACCESSIBILITY_SERVICE_EXCEPTED_PACKAGE_NAMES = {"edu.cmu.hcii.sugilite", "com.android.systemui", "edu.cmu.hcii.sugilitecommunicationtest", "edu.cmu.helpr"};
    //public static final String[] ACCESSIBILITY_SERVICE_EXCEPTED_PACKAGE_NAMES = {"edu.cmu.hcii.sugilite", "edu.cmu.hcii.sugilitecommunicationtest", "edu.cmu.helpr"};
    public static final String[] ACCESSIBILITY_SERVICE_TRACKING_EXCLUDED_PACKAGE_NAMES = {"edu.cmu.hcii.sugilitecommunicationtest", "edu.cmu.hcii.sugilite", "edu.cmu.helpr"};
    public static final String[] INPUT_METHOD_PACKAGE_NAMES = {"com.google.android.inputmethod.pinyin", "com.google.android.inputmethod.latin", "com.menny.android.anysoftkeyboard"};


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
            , SCRIPT_IDENTIFYING_FEATURE_COLOR = "#008400", SCRIPT_VIEW_ID_COLOR = "#1f71e2", SCRIPT_WITHIN_APP_COLOR = "#ff00ff", SCRIPT_LINK_COLOR = "#2e159f", SCRIPT_CONDITIONAL_COLOR = "#9333FF", SCRIPT_CONDITIONAL_COLOR_2 = "#6D3333", SCRIPT_CONDITIONAL_COLOR_3 = "#10b29a";

    public static int RECORDING_OVERLAY_COLOR = 0x20FFFF00;
    public static int RECORDING_OVERLAY_COLOR_STOP = 0x20FF0000;

    public static int PREVIEW_OVERLAY_COLOR = 0xB0000000;
    public static int MUL_ZEROS = 0xFF000000;
    public static int RECORDING_ON_BUTTON_COLOR = 0xFFFF3838;
    public static int RECORDING_OFF_BUTTON_COLOR = 0xFFDDDDDD;
    public static int RECORDING_SPEAKING_BUTTON_COLOR = 0xFF3784FF;
    public static int RECORDING_SPEAKING_ON_BACKGROUND_COLOR = 0xFFFFD3D3;
    public static int RECORDING_SPEAKING_OFF_BACKGROUND_COLOR = 0xFFFFFFFF;
    public static int RECORDING_WHITE_COLOR = 0xFFFFFFFF;
    public static int RECORDING_DARK_GRAY_COLOR = 0xFF424242;


    public static final int SEMI_TRANSPARENT_GRAY_BACKGROUND = Color.parseColor("#80000000");


    public static final String LOADING_MESSAGE = "Loading the script...\n", SAVING_MESSAGE = "Saving the script... \n";

    public static final String GET_CONDITION = "Please give your condition for the new fork. \n";
    public static final String CHECK_FOR_ELSE = "Would you like to do something if the condition is not fulfilled? \n";


    public static final int FILE_SCRIPT_DAO = 1, SQL_SCRIPT_DAO = 2;

    //switch between using SQL Dao and File Dao (SQL Dao has a max script size issue)
    public static final int DAO_TO_USE = FILE_SCRIPT_DAO;


    //date format
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd=HH_mm_ss-SSS");

    public static String boldify(String string){
        return "<b>" + string + "</b>";
    }

}
