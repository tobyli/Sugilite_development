package edu.cmu.hcii.sugilite.dao.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by toby on 1/14/17.
 */

public class SugiliteTriggerDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SugiliteTrigger.db";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME + " (" +
                    SugiliteTriggerDBContract.SugiliteTriggerRecordEntry._ID + " INTEGER PRIMARY KEY," +
                    SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_NAME + TEXT_TYPE + COMMA_SEP +
                    SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_TYPE + TEXT_TYPE + COMMA_SEP +
                    SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_SCRIPT_NAME + TEXT_TYPE + COMMA_SEP +
                    SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_APP + TEXT_TYPE + COMMA_SEP +
                    SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_CONTENT + TEXT_TYPE +
                    " )";

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME;

    public SugiliteTriggerDBHelper (Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate (SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        System.out.println("ON CREATE Sugilite Trigger DB");
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }
}
