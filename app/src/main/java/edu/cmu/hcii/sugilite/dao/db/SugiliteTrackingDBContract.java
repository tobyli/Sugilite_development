package edu.cmu.hcii.sugilite.dao.db;

import android.provider.BaseColumns;

/**
 * Created by toby on 7/25/16.
 */
public class SugiliteTrackingDBContract {
    public SugiliteTrackingDBContract(){

    }
    public static abstract class SugiliteTrackingRecordEntry implements BaseColumns {
        public static final String TABLE_NAME = "sugilite_tracking";
        public static final String COLUMN_NAME_SCRIPT_NAME = "script_name";
        public static final String COLUMN_NAME_ADDED_TIME = "added_time";
        public static final String COLUMN_NAME_SCRIPT_BODY = "script_body";

    }
}
