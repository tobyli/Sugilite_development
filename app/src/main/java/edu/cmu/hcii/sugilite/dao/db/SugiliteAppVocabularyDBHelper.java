package edu.cmu.hcii.sugilite.dao.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by toby on 8/15/16.
 */
public class SugiliteAppVocabularyDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "SugiliteAppVocabulary.db";
    private static final String BLOB_TYPE = " BLOB";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME + " (" +
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry._ID + " INTEGER PRIMARY KEY," +
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME + TEXT_TYPE + COMMA_SEP +
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT + TEXT_TYPE + COMMA_SEP +
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT_TYPE + TEXT_TYPE + COMMA_SEP +
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_TEXT + TEXT_TYPE + COMMA_SEP +
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CONTENT_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CHILD_TEXT + TEXT_TYPE + COMMA_SEP +
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CHILD_CONTENT_DESCRIPTION + TEXT_TYPE +

                    " )";

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME;

    public SugiliteAppVocabularyDBHelper (Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate (SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        System.out.println("ON CREATE Sugilite App Vocabulary DB");
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }
}