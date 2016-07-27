package edu.cmu.hcii.sugilite.dao.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by toby on 7/25/16.
 */
public class SugiliteTrackingDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SugiliteTracking.db";
    private static final String BLOB_TYPE = " BLOB";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SugiliteTrackingDBContract.SugiliteTrackingRecordEntry.TABLE_NAME + " (" +
                    SugiliteTrackingDBContract.SugiliteTrackingRecordEntry._ID + " INTEGER PRIMARY KEY," +
                    SugiliteTrackingDBContract.SugiliteTrackingRecordEntry.COLUMN_NAME_SCRIPT_NAME + TEXT_TYPE + COMMA_SEP +
                    SugiliteTrackingDBContract.SugiliteTrackingRecordEntry.COLUMN_NAME_ADDED_TIME + INTEGER_TYPE + COMMA_SEP +
                    SugiliteTrackingDBContract.SugiliteTrackingRecordEntry.COLUMN_NAME_SCRIPT_BODY + BLOB_TYPE +
                    " )";

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SugiliteTrackingDBContract.SugiliteTrackingRecordEntry.TABLE_NAME;

    public SugiliteTrackingDBHelper (Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate (SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        System.out.println("ON CREATE Sugilite Tracking DB");
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }
}
