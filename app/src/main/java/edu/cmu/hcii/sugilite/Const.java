package edu.cmu.hcii.sugilite;

import android.graphics.Color;
import android.os.Build;
import android.view.WindowManager;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

import java.text.SimpleDateFormat;

/**
 * @author toby
 * @date 10/25/16
 * @time 3:55 PM
 */
public class Const {

    public enum SpeechRecognitionType  {ANDROID, GOOGLE_CLOUD};
    //public static final SpeechRecognitionType SELECTED_SPEECH_RECOGNITION_TYPE = SpeechRecognitionType.ANDROID;
    public static final SpeechRecognitionType SELECTED_SPEECH_RECOGNITION_TYPE = SpeechRecognitionType.GOOGLE_CLOUD;

    //OVERLAY_TYPE should be either TYPE_APPLICATION_OVERLAY (sdk level >= 26) or TYPE_PHONE for maximized compatibility
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

    //switch between using SQL Dao and File Dao (SQL Dao has a max script size issue)
    public static final int FILE_SCRIPT_DAO = 1, SQL_SCRIPT_DAO = 2;
    public static final int DAO_TO_USE = FILE_SCRIPT_DAO;

    public static final boolean ENABLE_DAO_READING_CACHE = false;

    //contain package names for launchers so they won't be killed
    public static final String[] HOME_SCREEN_PACKAGE_NAMES = {"com.google.android.googlequicksearchbox", "com.google.android.apps.nexuslauncher"};

    //package names to excluded for recording & tracking
    public static final String[] ACCESSIBILITY_SERVICE_EXCEPTED_PACKAGE_NAMES = {"edu.cmu.hcii.sugilite", "com.android.systemui", "edu.cmu.hcii.sugilitecommunicationtest", "edu.cmu.helpr"};
    public static final String[] ACCESSIBILITY_SERVICE_TRACKING_EXCLUDED_PACKAGE_NAMES = {"edu.cmu.hcii.sugilitecommunicationtest", "edu.cmu.hcii.sugilite", "edu.cmu.helpr"};
    public static final String[] INPUT_METHOD_PACKAGE_NAMES = {"com.google.android.inputmethod.pinyin", "com.google.android.inputmethod.latin", "com.menny.android.anysoftkeyboard"};

    //App name to display
    public static final String appName = "Sovite";
    public static final String appNameUpperCase = appName.toUpperCase();

    //colors
    public static final String SCRIPT_ACTION_COLOR = "#ffa500";
    public static final String SCRIPT_ACTION_PARAMETER_COLOR = "#bc002f";
    public static final String SCRIPT_TARGET_TYPE_COLOR = "#36a095";
    public static final String SCRIPT_IDENTIFYING_FEATURE_COLOR = "#008400";
    public static final String SCRIPT_VIEW_ID_COLOR = "#1f71e2";
    public static final String SCRIPT_WITHIN_APP_COLOR = "#ff00ff";
    public static final String SCRIPT_LINK_COLOR = "#2e159f";
    public static final String SCRIPT_CONDITIONAL_COLOR = "#9333FF";
    public static final String SCRIPT_CONDITIONAL_COLOR_2 = "#6D3333";
    public static final String SCRIPT_CONDITIONAL_COLOR_3 = "#10b29a";
    public static final String SCRIPT_PLACEHOLDER_COLOR = "#999999";

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

    //date format
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd=HH_mm_ss-SSS");
    public static final SimpleDateFormat dateFormat_simple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String[] INIT_INSTRUCTION_CONTEXT_WORDS = {"if", "when", "whenever", "order", "cold", "check", "bus", "schedule", "hotel", "room", "at least", "greater", "less", "below", "above", "cheaper", "colder", "cooler", "price", "temperature", "order"};
    public static final String[] DEMONSTRATION_CONTEXT_WORDS = {"demonstrate"};
    public static final String[] CONFIRM_CONTEXT_WORDS = {"yes", "no", "correct", "incorrect", "wrong"};
    public static final String[] COMPARISON_CONTEXT_WORDS = {"at least", "greater", "less", "below", "above", "cheaper", "colder", "cooler", "price", "temperature"};

    public static final String[] UI_UPLOAD_PACKAGE_BLACKLIST = {"com.google.android.inputmethod.pinyin", "com.google.android.inputmethod.latin", "com.menny.android.anysoftkeyboard"};


}
