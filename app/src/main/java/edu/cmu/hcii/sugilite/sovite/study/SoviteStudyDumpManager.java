package edu.cmu.hcii.sugilite.sovite.study;

import android.app.Activity;
import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.OperationBlockDescriptionRegenerator;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 3/5/20
 * @time 9:33 AM
 */
public class SoviteStudyDumpManager {
    private Context context;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private PumiceDialogManager pumiceDialogManager;
    private PumiceKnowledgeManager pumiceKnowledgeManager;
    private File studyDumpPackageDir;

    public SoviteStudyDumpManager(Context context, SugiliteData sugiliteData, PumiceDialogManager pumiceDialogManager) {
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.pumiceDialogManager = pumiceDialogManager;
        this.pumiceKnowledgeManager = pumiceDialogManager.getPumiceKnowledgeManager();
        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        } else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }
        try {
            File rootDataDir = context.getFilesDir();
            studyDumpPackageDir = new File(rootDataDir.getPath() + "/sovite_dump");
            if (!studyDumpPackageDir.exists() || !studyDumpPackageDir.isDirectory())
                studyDumpPackageDir.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
    }

    // save file
    public void saveSoviteStudyDumpPacketToFile(String packetFileName) throws Exception {
        SoviteStudyDumpPacket studyDumpPacket = generateSoviteStudyDumpPacket(packetFileName);
        ObjectOutputStream oos = null;
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(studyDumpPackageDir.getPath() + "/" + packetFileName + ".sovitedump");
            oos = new ObjectOutputStream(fout);
            oos.writeObject(studyDumpPacket);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
            //TODO: error handling
        } finally {
            if (oos != null)
                oos.close();
            if (fout != null)
                fout.close();
        }
    }

    public List<SoviteStudyDumpPacket> getAllStoredSoviteStudyDumpPacket() {
        List<SoviteStudyDumpPacket> results = new ArrayList<>();
        List<File> files = new ArrayList<>();
        OntologyDescriptionGenerator ontologyDescriptionGenerator = new OntologyDescriptionGenerator();
        try {
            for (File file : studyDumpPackageDir.listFiles()) {
                if (file.getName().endsWith(".sovitedump")) {
                    files.add(file);
                }
                FileInputStream fin = null;
                ObjectInputStream ois = null;
                SoviteStudyDumpPacket packet = null;
                try {
                    fin = new FileInputStream(file);
                    ois = new ObjectInputStream(new BufferedInputStream(fin));
                    packet = (SoviteStudyDumpPacket) ois.readObject();
                    for (SugiliteStartingBlock script : packet.scripts) {
                        OperationBlockDescriptionRegenerator.regenerateScriptDescriptions(script, ontologyDescriptionGenerator);
                    }
                    results.add(packet);
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
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private SoviteStudyDumpPacket generateSoviteStudyDumpPacket(String name) throws Exception {
        return new SoviteStudyDumpPacket(pumiceKnowledgeManager, sugiliteScriptDao.getAllScripts(), name);
    }

    public class SoviteStudyDumpPacket {
        private PumiceKnowledgeManager pumiceKnowledgeManager;
        private List<SugiliteStartingBlock> scripts;
        private String name;

        public SoviteStudyDumpPacket(PumiceKnowledgeManager pumiceKnowledgeManager, List<SugiliteStartingBlock> scripts, String name) {
            this.pumiceKnowledgeManager = pumiceKnowledgeManager;
            this.scripts = scripts;
            this.name = name;
        }

        public List<SugiliteStartingBlock> getScripts() {
            return scripts;
        }

        public PumiceKnowledgeManager getPumiceKnowledgeManager() {
            return pumiceKnowledgeManager;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
