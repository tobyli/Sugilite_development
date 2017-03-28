package edu.cmu.hcii.sugilite.dao;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * Created by toby on 3/28/17.
 */

public class SugiliteScriptFileDao implements SugiliteScriptDao {
    private Context context;
    private File scriptDir;

    public SugiliteScriptFileDao(Context context){
        this.context = context;
        try {
            File rootDataDir = context.getFilesDir();
            File scriptDir = new File(rootDataDir.getPath() + "/scripts");
            if (!scriptDir.exists() || !scriptDir.isDirectory())
                scriptDir.mkdir();
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
    }

    public void save(SugiliteStartingBlock sugiliteBlock) throws Exception{
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

    public int size(){
        return 0;
    }
    public SugiliteStartingBlock read(String key){
        return null;
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
        return 0;
    }
    public List<String> getAllNames(){
        return null;
    }
    public List<SugiliteStartingBlock> getAllScripts(){
        return null;
    }
    public String getNextAvailableDefaultName(){
        return null;
    }
}
