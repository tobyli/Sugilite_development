package edu.cmu.hcii.sugilite.dao.db;

import android.provider.BaseColumns;

/**
 * Created by toby on 8/15/16.
 */
public class SugiliteAppVocabularyDBContract {
    public SugiliteAppVocabularyDBContract(){

    }
    public static abstract class SugiliteAppVocabularRecordEntry implements BaseColumns {
        public static final String TABLE_NAME = "sugilite_app_vocabulary";
        public static final String COLUMN_NAME_PACKAGE_NAME = "package_name";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_TEXT_TYPE = "text_type";
        public static final String COLUMN_NAME_PREVIOUS_CLICK_TEXT = "previous_click_text";
        public static final String COLUMN_NAME_PREVIOUS_CLICK_CONTENT_DESCRIPTION = "previous_click_content_description";
        public static final String COLUMN_NAME_PREVIOUS_CLICK_CHILD_TEXT = "previous_click_child_text";
        public static final String COLUMN_NAME_PREVIOUS_CLICK_CHILD_CONTENT_DESCRIPTION = "previous_click_child_content_description";

    }
}
