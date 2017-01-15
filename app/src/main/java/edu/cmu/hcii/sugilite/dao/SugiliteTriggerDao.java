package edu.cmu.hcii.sugilite.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import edu.cmu.hcii.sugilite.dao.db.SugiliteTrackingDBContract;
import edu.cmu.hcii.sugilite.dao.db.SugiliteTriggerDBContract;
import edu.cmu.hcii.sugilite.dao.db.SugiliteTriggerDBHelper;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * Created by toby on 1/14/17.
 */

public class SugiliteTriggerDao {
    private SugiliteTriggerDBHelper sugiliteTriggerDBHelper;
    private SQLiteDatabase db;


    public SugiliteTriggerDao(Context context){
        sugiliteTriggerDBHelper = new SugiliteTriggerDBHelper(context);
    }

    public long save(/*a trigger*/) throws Exception{
        return 0;
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

    //read(triggerName)

    /**
     * Delete the row with trigger name = key from DB
     * @param key
     * @return the number of rows deleted
     */
    public int delete(String key){

        return 0;
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

    //getAllNames

    //getAllTriggers



}

