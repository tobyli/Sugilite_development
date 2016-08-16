package edu.cmu.hcii.sugilite.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.dao.db.SugiliteAppVocabularyDBContract;
import edu.cmu.hcii.sugilite.dao.db.SugiliteAppVocabularyDBHelper;
import edu.cmu.hcii.sugilite.dao.db.SugiliteScriptDbContract;


/**
 * Created by toby on 8/15/16.
 */
public class SugiliteAppVocabularyDao {
    private SugiliteAppVocabularyDBHelper sugiliteAppVocabularyDBHelper;
    private SQLiteDatabase db;

    public SugiliteAppVocabularyDao(Context context){
        sugiliteAppVocabularyDBHelper = new SugiliteAppVocabularyDBHelper(context);
        db = sugiliteAppVocabularyDBHelper.getWritableDatabase();
    }

    /**
     * save packageName/text pair into DB;
     * @param packageName
     * @param text
     * @return row id
     * @throws Exception
     */
    public long save(String packageName, String text) throws Exception{
        ContentValues values = new ContentValues();
        if(packageName == null || text == null){
            throw new Exception("null block");
        }
        if(containsEntry(packageName, text))
            return -1;
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME, packageName);
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT, text);
        long newRowId;
        newRowId = db.insert(
                SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME,
                null,
                values);
        return newRowId;
    }

    public Set<String> getText(String packageName) throws Exception{
        Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME + " WHERE " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME + " = \'" + packageName + "\';", null);
        if(cursor.getCount() == 0) {
            return null;
        }
        Set<String> retVal = new HashSet<>();
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            String text = cursor.getString(cursor.getColumnIndex(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT));
            retVal.add(text);
            cursor.moveToNext();
        }
        cursor.close();
        return retVal;
    }

    public Map<String, Set<String>> getTextsForAllPackages() throws Exception{
        Cursor cursor = db.rawQuery("SELECT * FROM *;", null);
        if(cursor.getCount() == 0) {
            return null;
        }
        Map<String, Set<String>> retVal = new HashMap<>();
        while(!cursor.isAfterLast()){
            String packageName = cursor.getString(cursor.getColumnIndex(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME));
            String text = cursor.getString(cursor.getColumnIndex(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT));
            if(retVal.containsKey(packageName)){
                retVal.get(packageName).add(text);
            }
            else{
                Set<String> textSet = new HashSet<>();
                textSet.add(text);
                retVal.put(packageName, textSet);
            }
            cursor.moveToNext();
        }
        cursor.close();
        return retVal;
    }

    public boolean containsEntry(String packageName, String text) throws Exception{
        Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME + " WHERE " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME + " = \'" + packageName + "\' AND " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT + "= \'" + text +  "\';", null);
        if(cursor.getCount() == 0) {
            return false;
        }
        return true;
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
     * @return # of rows in DB
     */
    public long size(){
        SQLiteStatement statement = db.compileStatement("select count (*) from " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME + ";");
        long size = statement.simpleQueryForLong();
        return size;
    }

    /**
     * Clear the DB
     * @return the number of rows deleted
     */

    public int clear(){
        SQLiteStatement statement = db.compileStatement("DELETE FROM " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME + ";");
        int rowCount = statement.executeUpdateDelete();
        return rowCount;
    }

}
