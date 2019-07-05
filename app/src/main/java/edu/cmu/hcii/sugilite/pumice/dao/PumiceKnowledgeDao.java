package edu.cmu.hcii.sugilite.pumice.dao;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;

/**
 * @author toby
 * @date 2/14/19
 * @time 10:54 AM
 */
public class PumiceKnowledgeDao {
    Context context;
    private File knowledgeDir;
    private static final String fileName = "data.PumiceKnowledge";

    public PumiceKnowledgeDao(Context context, SugiliteData sugiliteData){
        this.context = context;
        try {
            File rootDataDir = context.getFilesDir();
            knowledgeDir = new File(rootDataDir.getPath() + "/pumice_knowledge");
            if (!knowledgeDir.exists() || !knowledgeDir.isDirectory())
                knowledgeDir.mkdir();
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
    }

    public PumiceKnowledgeManager getPumiceKnowledge() throws IOException, ClassNotFoundException {
        //read the script out from the file, and put it into the cache
        FileInputStream fin = null;
        ObjectInputStream ois = null;
        PumiceKnowledgeManager pumiceKnowledge = null;
        try {
            fin = new FileInputStream(knowledgeDir.getPath() + "/" + fileName);
            ois = new ObjectInputStream(fin);
            pumiceKnowledge = (PumiceKnowledgeManager) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fin != null)
                fin.close();
            if (ois != null)
                ois.close();
        }
        return pumiceKnowledge;
    }

    public void savePumiceKnowledge(PumiceKnowledgeManager pumiceKnowledge) throws IOException, ClassNotFoundException{
        ObjectOutputStream oos = null;
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(knowledgeDir.getPath() + "/" + fileName);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(pumiceKnowledge);
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

    public PumiceKnowledgeManager getPumiceKnowledgeOrANewInstanceIfNotAvailable(boolean toAddDefaultContentForNewInstance) throws IOException, ClassNotFoundException {
        PumiceKnowledgeManager pumiceKnowledgeManager = getPumiceKnowledge();
        if (pumiceKnowledgeManager == null){
            pumiceKnowledgeManager = new PumiceKnowledgeManager();
            if (toAddDefaultContentForNewInstance) {
                pumiceKnowledgeManager.initForTesting();
            }
        }
        return pumiceKnowledgeManager;
    }



}
