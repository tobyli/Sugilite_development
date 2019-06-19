package edu.cmu.hcii.sugilite.verbal_instruction_demo.study;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteAndroidAPIVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteGoogleCloudVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;


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
    private VerbalInstructionIconManager iconManager;
    private SugiliteAccessibilityService accessibilityService;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private SugiliteStudyHandler sugiliteStudyHandler;

    public SugiliteStudyHandler(Context context, LayoutInflater layoutInflater, SugiliteAccessibilityService accessibilityService, TextToSpeech tts){
        this.context = context;
        this.layoutInflater = layoutInflater;
        this.accessibilityService = accessibilityService;
        if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.ANDROID) {
            this.sugiliteVoiceRecognitionListener = new SugiliteAndroidAPIVoiceRecognitionListener(context, null, tts);
        } else if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.GOOGLE_CLOUD) {
            this.sugiliteVoiceRecognitionListener = new SugiliteGoogleCloudVoiceRecognitionListener(context, null, tts);
        }
        this.sugiliteStudyHandler = this;
        this.gson = new Gson();
    }

    public void handleEvent(SugiliteAvailableFeaturePack featurePack, UISnapshot uiSnapshot, String path, String fileName) {
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
            //stop recording after recording one event
            setToRecordNextOperation(false);
            refreshIconStatus();

            SerializableUISnapshot serializableUISnapshot = new SerializableUISnapshot(uiSnapshot);
            SugiliteSerializableEntity<Node> serializableEntity = new SugiliteSerializableEntity<>(foundEntity);
            accessibilityService.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SugiliteVerbalInstructionStudyDialog verbalInstructionTestDialog = new SugiliteVerbalInstructionStudyDialog(serializableUISnapshot, serializableEntity, sugiliteStudyHandler, context, layoutInflater, sugiliteVoiceRecognitionListener, path, fileName);
                    verbalInstructionTestDialog.show();
                }
            });

        }

    }

    public void setIconManager(VerbalInstructionIconManager iconManager) {
        this.iconManager = iconManager;
    }



    /**
     * dump the packet locally
     * @param packet
     */
    public void savePacket(SugiliteStudyPacket packet, String path, String fileName){
        PrintWriter out1 = null;
        String packet_gson = gson.toJson(packet);
        try {
            File f = new File(path);
            if (!f.exists() || !f.isDirectory()) {
                f.mkdirs();
                System.out.println("dir created");
            }
            System.out.println(f.getAbsolutePath());




            File queryFile = new File(f.getPath() + "/" + fileName + ".json");

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
            accessibilityService.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Packet saved to " + path, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public boolean isToRecordNextOperation() {
        return toRecordNextOperation;
    }

    public void setToRecordNextOperation(boolean toRecordNextOperation) {
        this.toRecordNextOperation = toRecordNextOperation;
        refreshIconStatus();
    }

    private void refreshIconStatus(){
        accessibilityService.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(iconManager != null) {
                    if (toRecordNextOperation) {
                        iconManager.startStudyRecording();
                    }
                    else{
                        iconManager.endStudyRecording();
                    }
                }
            }
        });
    }
}
