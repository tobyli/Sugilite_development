package edu.cmu.hcii.sugilite.verbal_instruction_demo.study;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.Node;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionTestDialog;

/**
 * @author toby
 * @date 1/21/18
 * @time 12:22 AM
 */
public class SugiliteStudyHandler {
    private boolean toRecordNextOperation = false;
    private Context context;
    private LayoutInflater layoutInflater;
    private Gson gson;

    public SugiliteStudyHandler(Context context, LayoutInflater layoutInflater){
        this.context = context;
        this.layoutInflater = layoutInflater;
        this.gson = new Gson();
    }

    public void handleEvent(SugiliteAvailableFeaturePack featurePack, UISnapshot uiSnapshot) {
        //identify the entity clicked on from the UI snapshot
        SugiliteEntity<Node> foundEntity = null;
        if(uiSnapshot != null) {
            for (Map.Entry<Node, SugiliteEntity<Node>> entityEntry : uiSnapshot.getNodeSugiliteEntityMap().entrySet()) {
                if (entityEntry.getKey().getBoundsInScreen().equals(featurePack.boundsInScreen) &&
                        entityEntry.getKey().getClassName().equals(featurePack.className)) {
                    //found
                    foundEntity = entityEntry.getValue();
                    break;
                }
            }
        }

        if (foundEntity != null){
            SerializableUISnapshot serializableUISnapshot = new SerializableUISnapshot(uiSnapshot);
            SugiliteSerializableEntity<Node> serializableEntity = new SugiliteSerializableEntity<>(foundEntity);
            SugiliteVerbalInstructionStudyDialog verbalInstructionTestDialog = new SugiliteVerbalInstructionStudyDialog(serializableUISnapshot, serializableEntity, this, context, layoutInflater);
            verbalInstructionTestDialog.show();


            //stop recording after recording one event
            setToRecordNextOperation(false);
        }

    }

    /**
     * dump the packet locally
     * @param packet
     */
    public void savePacket(SugiliteStudyPacket packet){
        PrintWriter out1 = null;
        String packet_gson = gson.toJson(packet);
        try {
            File f = new File("/sdcard/Download/sugilite_study_packets");
            if (!f.exists() || !f.isDirectory()) {
                f.mkdirs();
                System.out.println("dir created");
            }
            System.out.println(f.getAbsolutePath());


            Date time = Calendar.getInstance().getTime();
            String timeString = Const.dateFormat.format(time);

            File queryFile = new File(f.getPath() + "/packet_" + timeString + ".json");

            if (!queryFile.exists()) {
                queryFile.getParentFile().mkdirs();
                queryFile.createNewFile();
                System.out.println("file created");
            }

            out1 = new PrintWriter(new FileOutputStream(queryFile), true);
            out1.println(packet_gson);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out1 != null) out1.close();
            Toast.makeText(context, "Packet saved!", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isToRecordNextOperation() {
        return toRecordNextOperation;
    }

    public void setToRecordNextOperation(boolean toRecordNextOperation) {
        this.toRecordNextOperation = toRecordNextOperation;
    }
}
