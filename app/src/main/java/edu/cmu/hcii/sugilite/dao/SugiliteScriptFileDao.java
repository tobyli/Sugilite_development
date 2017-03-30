package edu.cmu.hcii.sugilite.dao;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * Created by toby on 3/28/17.
 */

public class SugiliteScriptFileDao implements SugiliteScriptDao {
    private Context context;
    private File scriptDir;
    private Map<String, SugiliteStartingBlock> savingCache;
    private Map<String, SugiliteStartingBlock> readingCache;



    public SugiliteScriptFileDao(Context context, SugiliteData sugiliteData){
        this.context = context;

        //NOTE: this is the one centralized buffer in SugiliteData
        savingCache = sugiliteData.sugiliteFileScriptDaoSavingCache;
        readingCache = sugiliteData.sugiliteFileScriptDaoReadingCache;

        try {
            File rootDataDir = context.getFilesDir();
            scriptDir = new File(rootDataDir.getPath() + "/scripts");
            if (!scriptDir.exists() || !scriptDir.isDirectory())
                scriptDir.mkdir();
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
    }

    /**
     * commit the changes in savingBuffer
     *
     * should be called whenever:
     * end recording (in the duck menu, in the recording popup and in the settings)
     * finish editing an existing script
     *
     * @throws Exception
     */
    public void commitSave() throws Exception{
        for(Map.Entry<String, SugiliteStartingBlock> entry : savingCache.entrySet()){
            commitSaveForASingleScript(entry.getValue());
        }
        savingCache.clear();
        System.out.println("COMMIT SAVE: SAVED " + savingCache.size() + " SCRIPTS");
    }

    public void commitSaveForASingleScript(SugiliteStartingBlock sugiliteBlock) throws Exception{
        String scriptName = sugiliteBlock.getScriptName();
        ObjectOutputStream oos = null;
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(scriptDir.getPath() + "/" + scriptName);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(sugiliteBlock);
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
        finally {
            if(oos != null)
                oos.close();
            if(fout != null)
                fout.close();
        }
    }

    public void save(SugiliteStartingBlock sugiliteBlock) throws Exception{
        savingCache.put(sugiliteBlock.getScriptName(), sugiliteBlock);
    }

    public int size(){
        int count = 0;
        try {
            for (File file : scriptDir.listFiles()){
                if(file.getName().contains(".SugiliteScript"))
                    count ++;
            }
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
        return count;
    }

    public SugiliteStartingBlock read(String key) throws Exception{
        if(savingCache.containsKey(key)){
            //the script is in the saving cache, read it out from the saving cache, and updates the reading cache
            SugiliteStartingBlock block = savingCache.get(key);
            readingCache.put(key, block);
            return block;
        }
        else if(readingCache.containsKey(key)){
            //read the script out from the reading cache
            return readingCache.get(key);
        }
        else {
            //read the script out from the file, and put it into the cache
            FileInputStream fin = null;
            ObjectInputStream ois = null;
            SugiliteStartingBlock block = null;
            try {
                fin = new FileInputStream(scriptDir.getPath() + "/" + key);
                ois = new ObjectInputStream(fin);
                block = (SugiliteStartingBlock) ois.readObject();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
                //TODO: error handling
            } finally {
                if (fin != null)
                    fin.close();
                if (ois != null)
                    ois.close();
            }
            readingCache.put(key, block);
            return block;
        }
    }
    public int delete(String key){
        try {
            File file = new File(scriptDir.getPath() + "/" + key);
            if (file.delete()) {
                return 1;
            } else {
                return 0;
            }
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
    }
    public int clear(){
        int count = 0;
        try {
            for (File file : scriptDir.listFiles()){
                if(file.getName().contains(".SugiliteScript")) {
                    file.delete();
                    count ++;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
        return count;
    }
    public List<String> getAllNames(){
        List<String> names = new ArrayList<>();
        try {
            for (File file : scriptDir.listFiles()){
                if(file.getName().contains(".SugiliteScript")) {
                    names.add(file.getName());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
        return names;
    }

    public List<SugiliteStartingBlock> getAllScripts() throws Exception{
        List<SugiliteStartingBlock> scripts = new ArrayList<>();
        for(String name : getAllNames()){
            try{
                SugiliteStartingBlock script = read(name);
                scripts.add(script);
            }
            catch (Exception e){
                e.printStackTrace();
                continue;
            }
        }
        return scripts;
    }

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
