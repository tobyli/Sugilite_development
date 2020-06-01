package edu.cmu.hcii.sugilite.dao;

import android.app.AlertDialog;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.OperationBlockDescriptionRegenerator;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSpecialOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetValueOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteReadoutOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteClickOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteLaunchAppOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteLongClickOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteReadoutConstOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveValueQueryOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteSelectOperation;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;

import static edu.cmu.hcii.sugilite.Const.ENABLE_DAO_READING_CACHE;
import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * Created by toby on 3/28/17.
 */

public class SugiliteScriptFileDao implements SugiliteScriptDao {
    private Context context;
    private File scriptDir;
    private Map<String, SugiliteStartingBlock> savingCache;
    private Map<String, SugiliteStartingBlock> readingCache;
    private Gson gson;


    private OntologyDescriptionGenerator ontologyDescriptionGenerator;


    public SugiliteScriptFileDao(Context context, SugiliteData sugiliteData){
        this.context = context;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();


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

    private void invalidateCache() {
        savingCache.clear();
        readingCache.clear();
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
    public void commitSave(Runnable runOnDone) throws Exception{
        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.saving_script_message);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int number = savingCache.size();
                for(Map.Entry<String, SugiliteStartingBlock> entry : savingCache.entrySet()){
                    try {
                        commitSaveForASingleScript(entry.getValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                invalidateCache();
                System.out.println("COMMIT SAVE: SAVED " + number + " SCRIPTS");
                progressDialog.dismiss();
                if (runOnDone != null) {
                    runOnDone.run();
                }
            }
        }).start();
    }


    private void commitSaveForASingleScript(SugiliteStartingBlock sugiliteBlock) throws Exception{
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
        if(savingCache.containsKey(key) && ENABLE_DAO_READING_CACHE){
            //the script is in the saving cache, read it out from the saving cache, and updates the reading cache
            SugiliteStartingBlock block = savingCache.get(key);
            readingCache.put(key, block);
            return block;
        }
        else if(readingCache.containsKey(key) && ENABLE_DAO_READING_CACHE){
            //read the script out from the reading cache
            return readingCache.get(key);
        }
        else {
            //show the progress dialog when reading from the file system
            SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.loading_file_message);
            progressDialog.show();


            //read the script out from the file, and put it into the cache

            FileInputStream fin = null;
            ObjectInputStream ois = null;
            SugiliteStartingBlock sugiliteStartingBlock = null;
            try {
                fin = new FileInputStream(scriptDir.getPath() + "/" + key);
                ois = new ObjectInputStream(new BufferedInputStream(fin));
                sugiliteStartingBlock = (SugiliteStartingBlock) ois.readObject();
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

            setParentBlockForNextBlocks(sugiliteStartingBlock);
            setPreviousBlockForNextBlocks(sugiliteStartingBlock);

            readingCache.put(key, sugiliteStartingBlock);
            OperationBlockDescriptionRegenerator.regenerateScriptDescriptions(sugiliteStartingBlock, ontologyDescriptionGenerator);
            progressDialog.dismiss();
            return sugiliteStartingBlock;
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
        finally {
            invalidateCache();
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
        finally {
            invalidateCache();
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
            if(allNames.contains(PumiceDemonstrationUtil.addScriptExtension(scriptName))){
                i++;
                continue;
            }
            else
                return scriptName;
        }
    }

    public String getDir(){
        return scriptDir.getPath();
    }

    public String getScriptPath(String name){
        return scriptDir.getPath() + "/" + name;
    }

    private void setPreviousBlockForNextBlocks (SugiliteBlock currentBlock) {
        if (currentBlock.getNextBlock() != null) {
            currentBlock.getNextBlock().setPreviousBlock(currentBlock);
            setPreviousBlockForNextBlocks(currentBlock.getNextBlock());
        }
        if (currentBlock instanceof SugiliteConditionBlock) {
            if (((SugiliteConditionBlock) currentBlock).getThenBlock() != null) {
                setPreviousBlockForNextBlocks(((SugiliteConditionBlock) currentBlock).getThenBlock());
            }
            if (((SugiliteConditionBlock) currentBlock).getElseBlock() != null) {
                setPreviousBlockForNextBlocks(((SugiliteConditionBlock) currentBlock).getElseBlock());
            }
        }
    }

    private void setParentBlockForNextBlocks (SugiliteBlock currentBlock) {
        if (currentBlock instanceof SugiliteConditionBlock) {
            if (((SugiliteConditionBlock) currentBlock).getThenBlock() != null) {
                ((SugiliteConditionBlock) currentBlock).getThenBlock().setParentBlock(currentBlock);
                setParentBlockForNextBlocks(((SugiliteConditionBlock) currentBlock).getThenBlock());
            }
            if (((SugiliteConditionBlock) currentBlock).getElseBlock() != null) {
                ((SugiliteConditionBlock) currentBlock).getElseBlock().setParentBlock(currentBlock);
                setParentBlockForNextBlocks(((SugiliteConditionBlock) currentBlock).getElseBlock());
            }
        }
        if (currentBlock.getNextBlock() != null) {
            setParentBlockForNextBlocks(currentBlock.getNextBlock());
        }
    }
}
