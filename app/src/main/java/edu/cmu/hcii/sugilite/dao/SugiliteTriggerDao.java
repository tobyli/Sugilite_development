package edu.cmu.hcii.sugilite.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;


import java.util.ArrayList;
import java.util.List;


import edu.cmu.hcii.sugilite.dao.db.SugiliteTriggerDBContract;
import edu.cmu.hcii.sugilite.dao.db.SugiliteTriggerDBHelper;
import edu.cmu.hcii.sugilite.model.trigger.SugiliteTrigger;


/**
 * Created by toby on 1/14/17.
 */

public class SugiliteTriggerDao {
    private SugiliteTriggerDBHelper sugiliteTriggerDBHelper;
    private SQLiteDatabase db;


    public SugiliteTriggerDao(Context context){
        sugiliteTriggerDBHelper = new SugiliteTriggerDBHelper(context);
    }

    public long save(SugiliteTrigger sugiliteTrigger) throws Exception{
        ContentValues values = new ContentValues();
        if(sugiliteTrigger == null){
            throw new Exception("null block");
        }
        values.put(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_SCRIPT_NAME, sugiliteTrigger.getScriptName());
        values.put(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_APP, sugiliteTrigger.getAppPackageName());
        values.put(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_CONTENT, sugiliteTrigger.getContent());
        values.put(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_NAME, sugiliteTrigger.getName());
        values.put(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_TYPE, sugiliteTrigger.getType());

        long newRowId = -1;
        try {
            db = sugiliteTriggerDBHelper.getWritableDatabase();
            newRowId = db.insert(
                    SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME,
                    null,
                    values);
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return newRowId;
    }

    /**
     * @return # of rows in DB
     */
    public long size(){
        long size = -1;
        try {
            db = sugiliteTriggerDBHelper.getReadableDatabase();
            SQLiteStatement statement = db.compileStatement("select count (*) from " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME + ";");
            size = statement.simpleQueryForLong();
            db.close();

        }
        catch (Exception e){
            e.printStackTrace();
        }
        return size;
    }

    /**
     *
     * @return path of the ".db" file
     */
    public String getPath(){
        String path = "";
        try{
            db = sugiliteTriggerDBHelper.getReadableDatabase();
            path = db.getPath();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return path;
    }

    public SugiliteTrigger read(String triggerName){
        SugiliteTrigger trigger = null;
        try {
            db = sugiliteTriggerDBHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME + " WHERE " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_NAME + " = \'" + triggerName + "\';", null);
            if (cursor.getCount() == 0) {
                db.close();
                return null;
            }
            cursor.moveToFirst();
            String scriptName = cursor.getString(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_SCRIPT_NAME));
            String triggerApp = cursor.getString(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_APP));
            String content = cursor.getString(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_CONTENT));
            int type = cursor.getInt(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_TYPE));
            cursor.close();
            db.close();
            trigger = new SugiliteTrigger(triggerName, scriptName, content, triggerApp, type);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return trigger;
    }

    /**
     * Delete the row with trigger name = key from DB
     * @param key
     * @return the number of rows deleted
     */
    public int delete(String key){
        int rowCount = -1;
        try {
            db = sugiliteTriggerDBHelper.getWritableDatabase();
            SQLiteStatement statement = db.compileStatement("DELETE FROM " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME + " WHERE " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_NAME + " = \'" + key + "\';");
            rowCount = statement.executeUpdateDelete();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return rowCount;

    }

    /**
     * Clear the DB
     * @return the number of rows deleted
     */

    public int clear(){
        int rowCount = -1;
        try {
            db = sugiliteTriggerDBHelper.getWritableDatabase();
            SQLiteStatement statement = db.compileStatement("DELETE FROM " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME + ";");
            rowCount = statement.executeUpdateDelete();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return rowCount;
    }

    /**
     *
     * @return the list of names of all sugilite triggers
     */
    public List<String> getAllNames(){
        List<String> names = new ArrayList<>();
        try {
            db = sugiliteTriggerDBHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME + ";", null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_NAME));
                names.add(name);
                cursor.moveToNext();
            }
            cursor.close();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return names;
    }

    /**
     *
     * @return the list of all sugilite triggers
     */
    public List<SugiliteTrigger> getAllTriggers(){
        List<SugiliteTrigger> triggers = new ArrayList<>();
        try {
            db = sugiliteTriggerDBHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.TABLE_NAME + ";", null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String triggerName = cursor.getString(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_NAME));
                String scriptName = cursor.getString(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_SCRIPT_NAME));
                String triggerApp = cursor.getString(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_APP));
                String content = cursor.getString(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_CONTENT));
                int type = cursor.getInt(cursor.getColumnIndex(SugiliteTriggerDBContract.SugiliteTriggerRecordEntry.COLUMN_NAME_TRIGGER_TYPE));
                SugiliteTrigger trigger = new SugiliteTrigger(triggerName, scriptName, content, triggerApp, type);
                triggers.add(trigger);
                cursor.moveToNext();
            }
            cursor.close();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return triggers;
    }



}

