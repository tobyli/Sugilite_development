package edu.cmu.hcii.sugilite.dao.db;

import android.provider.BaseColumns;

/**
 * @author toby
 * @date 6/15/16
 * @time 4:24 PM
 */
public class SugiliteScriptDbContract {
    public SugiliteScriptDbContract(){

    }
    public static abstract class SugiliteScriptRecordEntry implements BaseColumns{
        public static final String TABLE_NAME = "sugilite_script";
        public static final String COLUMN_NAME_SCRIPT_NAME = "script_name";
        public static final String COLUMN_NAME_ADDED_TIME = "added_time";
        public static final String COLUMN_NAME_SCRIPT_BODY = "script_body";

    }
}
