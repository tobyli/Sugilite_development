package edu.cmu.hcii.sugilite.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.google.gson.Gson;

import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.cmu.hcii.sugilite.dao.db.SugiliteScriptDbContract;
import edu.cmu.hcii.sugilite.dao.db.SugiliteScriptDBHelper;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * @author toby
 * @date 6/15/16
 * @time 4:04 PM
 */
public class SugiliteScriptDao {
    private SugiliteScriptDBHelper sugiliteScriptDBHelper;
    private Gson gson = new Gson();
    SQLiteDatabase db;

    public SugiliteScriptDao(Context context){
        sugiliteScriptDBHelper = new SugiliteScriptDBHelper(context);
        db = sugiliteScriptDBHelper.getWritableDatabase();
    }

    /**
     * save sugiliteBlock into the db (note: no duplicated script name allowed, new ones will replace old ones with the same name)
     * @param sugiliteBlock
     * @return row id
     * @throws Exception
     */
    public long save(SugiliteStartingBlock sugiliteBlock) throws Exception{
        Calendar c = Calendar.getInstance();
        ContentValues values = new ContentValues();
        if(sugiliteBlock == null || sugiliteBlock.getScriptName() == null){
            throw new Exception("null block");
        }
        delete(sugiliteBlock.getScriptName());
        values.put(SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_SCRIPT_NAME, sugiliteBlock.getScriptName());
        values.put(SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_SCRIPT_BODY, SerializationUtils.serialize(sugiliteBlock));
        values.put(SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_ADDED_TIME, c.getTimeInMillis());
        long newRowId;
        newRowId = db.insert(
                SugiliteScriptDbContract.SugiliteScriptRecordEntry.TABLE_NAME,
                null,
                values);
        return newRowId;
    }

    /**
     *
     * @return # of rows in DB
     */
    public long size(){
        SQLiteStatement statement = db.compileStatement("select count (*) from " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.TABLE_NAME + ";");
        long size = statement.simpleQueryForLong();
        return size;
    }

    /**
     *
     * @return path of the ".db" file
     */
    public String getPath(){
        String path = db.getPath();
        return path;
    }

    /**
     *
     * @param key
     * @return the script with name = key, null if there's no such script
     */
    public SugiliteStartingBlock read(String key){
        Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.TABLE_NAME + " WHERE " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_SCRIPT_NAME + " = \'" + key + "\';", null);
        if(cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();
        byte[] blob = cursor.getBlob(cursor.getColumnIndex(SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_SCRIPT_BODY));
        SugiliteStartingBlock block = (SugiliteStartingBlock)SerializationUtils.deserialize(blob);
        cursor.close();
        return block;
    }

    public SugiliteStartingBlock read(long id){
        Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.TABLE_NAME + " WHERE " + SugiliteScriptDbContract.SugiliteScriptRecordEntry._ID + " = \'" + id + "\';", null);
        if(cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();
        byte[] blob = cursor.getBlob(cursor.getColumnIndex(SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_SCRIPT_BODY));
        SugiliteStartingBlock block = (SugiliteStartingBlock)SerializationUtils.deserialize(blob);
        cursor.close();
        return block;
    }

    /**
     * Delete the row with script name = key from DB
     * @param key
     * @return the number of rows deleted
     */
    public int delete(String key){
        SQLiteStatement statement = db.compileStatement("DELETE FROM " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.TABLE_NAME + " WHERE " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_SCRIPT_NAME + " = \'" + key + "\';");
        int rowCount = statement.executeUpdateDelete();
        return rowCount;
    }

    /**
     * Clear the DB
     * @return the number of rows deleted
     */

    public int clear(){
        SQLiteStatement statement = db.compileStatement("DELETE FROM " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.TABLE_NAME + ";");
        int rowCount = statement.executeUpdateDelete();
        return rowCount;
    }

    /**
     *
     * @return the list of all script names in DB
     */
    public List<String> getAllNames(){
        Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.TABLE_NAME + ";", null);
        cursor.moveToFirst();
        List<String> names = new ArrayList<>();
        while (!cursor.isAfterLast()){
            String name = cursor.getString(cursor.getColumnIndex(SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_SCRIPT_NAME));
            names.add(name);
            cursor.moveToNext();
        }
        cursor.close();
        return names;
    }

    /**
     *
     * @return the list of all scripts in DB
     */
    public List<SugiliteStartingBlock> getAllScripts(){
        Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteScriptDbContract.SugiliteScriptRecordEntry.TABLE_NAME + ";", null);
        cursor.moveToFirst();
        List<SugiliteStartingBlock> scripts = new ArrayList<>();
        while (!cursor.isAfterLast()){
            byte[] blob = cursor.getBlob(cursor.getColumnIndex(SugiliteScriptDbContract.SugiliteScriptRecordEntry.COLUMN_NAME_SCRIPT_BODY));
            SugiliteStartingBlock block = (SugiliteStartingBlock)SerializationUtils.deserialize(blob);
            scripts.add(block);
            cursor.moveToNext();
        }
        cursor.close();
        return scripts;
    }

    /**
     *
     * @return the next available default name "Untitled Script N" with the smallest N
     */
    public String getNextAvailableDefaultName(){
        int i = 1;
        String prefix = "Untitled Script ";
        List<String> allNames = getAllNames();
        while(true){
            String scriptName = prefix + String.valueOf(i);
            if(allNames.contains(scriptName + ".SugiliteScript")){
                i++;
                continue;
            }
            else
                return scriptName;
        }
    }




}
