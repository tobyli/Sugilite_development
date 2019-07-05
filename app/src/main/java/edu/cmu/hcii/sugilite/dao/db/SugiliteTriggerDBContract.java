package edu.cmu.hcii.sugilite.dao.db;

import android.provider.BaseColumns;

/**
 * Created by toby on 1/14/17.
 */

public class SugiliteTriggerDBContract {

    public SugiliteTriggerDBContract(){

    }

    public static abstract class SugiliteTriggerRecordEntry implements BaseColumns {
        public static final String TABLE_NAME = "sugilite_trigger";
        public static final String COLUMN_NAME_TRIGGER_NAME = "trigger_name";
        public static final String COLUMN_NAME_SCRIPT_NAME = "script_name";
        public static final String COLUMN_NAME_TRIGGER_TYPE = "trigger_type";
        public static final String COLUMN_NAME_TRIGGER_APP = "trigger_app";
        public static final String COLUMN_NAME_TRIGGER_CONTENT = "trigger_content";
    }
}
