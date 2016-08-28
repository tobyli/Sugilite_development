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
    private static SugiliteAppVocabularyDBHelper sugiliteAppVocabularyDBHelper;
    private static SQLiteDatabase db;

    public SugiliteAppVocabularyDao(Context context){
        sugiliteAppVocabularyDBHelper = new SugiliteAppVocabularyDBHelper(context);
    }

    /**
     * save packageName/text pair into DB;
     * @param packageName
     * @param text
     * @return row id
     * @throws Exception
     */
    public long save(String packageName, String text, String textType, String previousClickText, String previousClickContentDescription, String previousClickChildText, String previousClickChildContentDescription) throws Exception{
        ContentValues values = new ContentValues();
        if(packageName == null || text == null){
            throw new Exception("null block");
        }
        //TODO: fix contains entry
        if(containsEntry(packageName, text, textType, previousClickText, previousClickContentDescription, previousClickChildText, previousClickChildContentDescription))
            return -1;
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME, packageName);
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT, text);
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT_TYPE, textType);
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_TEXT, previousClickText);
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CONTENT_DESCRIPTION, previousClickContentDescription);
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CHILD_TEXT, previousClickChildText);
        values.put(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CHILD_CONTENT_DESCRIPTION, previousClickChildContentDescription);

        long newRowId = -1;
        try {
            db = sugiliteAppVocabularyDBHelper.getWritableDatabase();
            newRowId = db.insert(
                    SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME,
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
     * get a set of all texts for a given packageName
     * @param packageName
     * @return
     * @throws Exception
     */
    public Set<String> getText(String packageName) throws Exception{
        String[] columnsToReturn = {SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT};
        String selection = SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME + " =?";
        String[] selectionArgs = {packageName};
        Set<String> retVal = new HashSet<>();
        try {
            db = sugiliteAppVocabularyDBHelper.getReadableDatabase();
            Cursor cursor = db.query(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME,
                    columnsToReturn, selection, selectionArgs, null, null, null);
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String text = cursor.getString(cursor.getColumnIndex(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT));
                retVal.add(text);
                cursor.moveToNext();
            }
            cursor.close();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return retVal;
    }

    /**
     * get a map of all texts for all packages
     * @return
     * @throws Exception
     */
    public Map<String, Set<String>> getTextsForAllPackages() throws Exception{
        Map<String, Set<String>> retVal = new HashMap<>();
        try {
            db = sugiliteAppVocabularyDBHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME + ";", null);
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String packageName = cursor.getString(cursor.getColumnIndex(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME));
                String text = cursor.getString(cursor.getColumnIndex(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT));
                if (retVal.containsKey(packageName)) {
                    retVal.get(packageName).add(text);
                } else {
                    Set<String> textSet = new HashSet<>();
                    textSet.add(text);
                    retVal.put(packageName, textSet);
                }
                cursor.moveToNext();
            }
            cursor.close();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return retVal;
    }

    /**
     * check if db contains a given (packageName, text)
     * @param packageName
     * @param text
     * @return
     * @throws Exception
     */
    public boolean containsEntry(String packageName, String text) throws Exception{
        String[] columnsToReturn = {SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT};
        String selection = SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME + " =? AND " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT + " =?";
        String[] selectionArgs = {packageName, text};
        boolean containsEntry = false;
        try {
            db = sugiliteAppVocabularyDBHelper.getReadableDatabase();
            Cursor cursor = db.query(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME,
                    columnsToReturn, selection, selectionArgs, null, null, null);
            if (cursor.getCount() > 0) {
                containsEntry = true;
            }
            else{
                containsEntry = false;
            }
            cursor.close();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return containsEntry;
    }

    /**
     * check if db contains a given (packageName, text, textType, previousClickText, previousClickContentDescription, previousClickChildText, previousClickChildContentDescription)
     * @param packageName
     * @param text
     * @param textType
     * @param previousClickText
     * @param previousClickContentDescription
     * @param previousClickChildText
     * @param previousClickChildContentDescription
     * @return
     * @throws Exception
     */
    public boolean containsEntry(String packageName, String text, String textType, String previousClickText, String previousClickContentDescription, String previousClickChildText, String previousClickChildContentDescription) throws Exception {
        String[] columnsToReturn = {SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT};
        String selection = SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PACKAGE_NAME + " =? AND " +
                SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT + " =? AND " +
                SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_TEXT_TYPE + " =? AND " +
                SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_TEXT + " =? AND " +
                SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CONTENT_DESCRIPTION + " =? AND " +
                SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CHILD_TEXT + " =? AND " +
                SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.COLUMN_NAME_PREVIOUS_CLICK_CHILD_CONTENT_DESCRIPTION + " =?";

        String[] selectionArgs = {packageName, text, textType, previousClickText, previousClickChildContentDescription, previousClickChildText, previousClickChildContentDescription};

        boolean containsEntry = false;
        try {
            db = sugiliteAppVocabularyDBHelper.getReadableDatabase();
            Cursor cursor = db.query(SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME,
                    columnsToReturn, selection, selectionArgs, null, null, null);
            if (cursor.getCount() > 0) {
                containsEntry = true;
            }
            else{
                containsEntry = false;
            }
            cursor.close();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return containsEntry;
    }

    /**
     * @return path of the ".db" file
     */
    public String getPath(){
        String path = "";
        try {
            db = sugiliteAppVocabularyDBHelper.getReadableDatabase();
            path = db.getPath();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return path;
    }

    /**
     * @return # of rows in DB
     */
    public long size(){
        long size = -1;
        try {
            db = sugiliteAppVocabularyDBHelper.getReadableDatabase();
            SQLiteStatement statement = db.compileStatement("SELECT COUNT (*) FROM " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME + ";");
            size = statement.simpleQueryForLong();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return size;
    }

    /**
     * Clear the DB
     * @return the number of rows deleted
     */

    public int clear(){
        int rowCount = -1;
        try {
            db = sugiliteAppVocabularyDBHelper.getWritableDatabase();
            SQLiteStatement statement = db.compileStatement("DELETE FROM " + SugiliteAppVocabularyDBContract.SugiliteAppVocabularRecordEntry.TABLE_NAME + ";");
            rowCount = statement.executeUpdateDelete();
            db.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return rowCount;
    }

}
