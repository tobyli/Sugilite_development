package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResults;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerQuery;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteAndroidAPIVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteGoogleCloudVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 12/9/17
 * @time 11:11 PM
 */


/**
 * the dialog used for testing the semantic parser
 */
public class VerbalInstructionTestDialog implements SugiliteVoiceInterface, SugiliteVerbalInstructionHTTPQueryInterface {
    private SerializableUISnapshot serializableUISnapshot;
    private Context context;
    private EditText instructionTextbox;
    private AlertDialog dialog;
    private AlertDialog progressDialog;
    private ImageButton speakButton;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private SugiliteVerbalInstructionHTTPQueryManager sugiliteVerbalInstructionHTTPQueryManager;
    private VerbalInstructionOverlayManager overlayManager;
    private Gson gson;
    private LinearLayout mainLayout;
    private VerbalInstructionTestDialog verbalInstructionTestDialog;
    public boolean isListening = false;


    public VerbalInstructionTestDialog(SerializableUISnapshot serializableUISnapshot, Context context, LayoutInflater inflater, SugiliteData sugiliteData, SharedPreferences sharedPreferences, TextToSpeech tts){
        this.serializableUISnapshot = serializableUISnapshot;
        this.context = context;
        if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.ANDROID) {
            this.sugiliteVoiceRecognitionListener = new SugiliteAndroidAPIVoiceRecognitionListener(context, this, tts);
        } else if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.GOOGLE_CLOUD) {
            this.sugiliteVoiceRecognitionListener = new SugiliteGoogleCloudVoiceRecognitionListener(context, this, tts);
        }
        this.sugiliteVerbalInstructionHTTPQueryManager = new SugiliteVerbalInstructionHTTPQueryManager(sharedPreferences);
        this.overlayManager = new VerbalInstructionOverlayManager(context, sugiliteData, sharedPreferences);
        this.gson = new Gson();
        this.verbalInstructionTestDialog = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = inflater.inflate(R.layout.dialog_send_server_query, null);
        mainLayout = (LinearLayout)dialogView.findViewById(R.id.layout_send_server_query);
        instructionTextbox = (EditText)dialogView.findViewById(R.id.edittext_instruction_content);
        speakButton = (ImageButton)dialogView.findViewById(R.id.button_verbal_instruction_talk);
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // speak button
                if(isListening) {
                    sugiliteVoiceRecognitionListener.stopListening();
                }

                else {
                    sugiliteVoiceRecognitionListener.startListening();
                }
            }
        });
        builder.setView(dialogView)
                .setTitle(Const.appNameUpperCase + " Verbal Instruction")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(isListening) {
                    sugiliteVoiceRecognitionListener.stopListening();
                }
            }
        });
    }

    public void show(){
        dialog.getWindow().setType(OVERLAY_TYPE);
        dialog.show();
        speakButton.performClick();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {


                String userInput = instructionTextbox.getText().toString();
                VerbalInstructionServerQuery query = new VerbalInstructionServerQuery(userInput, serializableUISnapshot.triplesToStringWithFilter(SugiliteRelation.HAS_CHILD, SugiliteRelation.HAS_PARENT), null);

                //save the query locally
                dumpQuery(query);
                VerbalInstructionIconManager.dumpUISnapshot(serializableUISnapshot);

                //send the query

                try {
                    sugiliteVerbalInstructionHTTPQueryManager.sendQueryRequestOnASeparateThread(query, verbalInstructionTestDialog);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                //TODO: show loading popup
                showProgressDialog();

            }
        });
    }


    private void dumpQuery(VerbalInstructionServerQuery query){
        PrintWriter out1 = null;
        String query_gson = gson.toJson(query);
        try {
            File f = new File("/sdcard/Download/ui_snapshots");
            if (!f.exists() || !f.isDirectory()) {
                f.mkdirs();
                System.out.println("dir created");
            }
            System.out.println(f.getAbsolutePath());


            Date time = Calendar.getInstance().getTime();
            String timeString = Const.dateFormat.format(time);

            File queryFile = new File(f.getPath() + "/query_" + timeString + ".json");

            if (!queryFile.exists()) {
                queryFile.getParentFile().mkdirs();
                queryFile.createNewFile();
                System.out.println("file created");
            }

            out1 = new PrintWriter(new FileOutputStream(queryFile), true);
            out1.println(query_gson);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out1 != null) out1.close();
        }
    }

    private void showProgressDialog(){
        progressDialog = new AlertDialog.Builder(context).setMessage("Processing the query ...").create();
        progressDialog.getWindow().setType(OVERLAY_TYPE);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    @Override
    /**
     * callback for HTTP query
     */
    public void resultReceived(int responseCode, String result, String originalQuery) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        //raw response
        System.out.print(responseCode + ": " + result);

        //de-serialize to VerbalInstructionResults
        VerbalInstructionServerResults results = gson.fromJson(result, VerbalInstructionServerResults.class);
        for (VerbalInstructionServerResults.VerbalInstructionResult verbalInstructionResult : results.getQueries()) {
            System.out.println(gson.toJson(verbalInstructionResult));
        }

        //find matches
        Map<String, SugiliteSerializableEntity> idEntityMap = serializableUISnapshot.getSugiliteEntityIdSugiliteEntityMap();
        for (VerbalInstructionServerResults.VerbalInstructionResult verbalInstructionResult : results.getQueries()) {
            List<Node> filteredNodes = new ArrayList<>();
            Map<Node, String> filteredNodeNodeIdMap = new HashMap<>();
            for (String nodeId : verbalInstructionResult.getGrounding()){
                if (idEntityMap.containsKey(nodeId)) {
                    SugiliteSerializableEntity entity = idEntityMap.get(nodeId);
                    if (entity.getType().equals(Node.class)){
                        Node node = (Node)entity.getEntityValue();
                        if(node.getClickable()){
                            filteredNodes.add(node);
                            filteredNodeNodeIdMap.put(node, nodeId);
                        }
                    }
                }
                else{
                    continue;
                }
            }
            if(filteredNodes.size() > 0){
                //matched

                //=== print debug info ===
                System.out.println("MATCHED " + verbalInstructionResult.getId()  + ": " + verbalInstructionResult.getFormula());
                int nodeCount = 0;
                for(Node node : filteredNodes){
                    System.out.println("Node " + ++nodeCount + ": " + gson.toJson(node));
                }
                //=== done printing debug info ===

                Toast.makeText(context, verbalInstructionResult.getFormula(), Toast.LENGTH_SHORT).show();
                for(Node node : filteredNodes){
                    //TODO: show overlay
                    String utternace = "";
                    if(instructionTextbox != null && instructionTextbox.getText() != null){
                        utternace = instructionTextbox.getText().toString();
                    }
                    //node, nodeId, corresponding VerbalInstructionResult, VerbalInstructionResults
                    overlayManager.addOverlay(node, filteredNodeNodeIdMap.get(node), verbalInstructionResult, results.getQueries(), serializableUISnapshot, utternace);
                }
                break;
            }
        }
        dialog.dismiss();
    }

    @Override
    public void listeningStartedCallback() {
        isListening = true;
    }

    @Override
    public void listeningEndedCallback() {
        isListening = false;
    }

    @Override
    public void speakingStartedCallback() {

    }

    @Override
    public void speakingEndedCallback() {

    }

    @Override
    public void resultAvailableCallback(List<String> matches, boolean isFinal) {
        if(isFinal && matches.size() > 0) {
            instructionTextbox.setText(matches.get(0));
        }
    }

    @Override
    public void runOnMainThread(Runnable r) {
        try {
            mainLayout.post(r);
        }
        catch (Exception e){
            //do nothing
            e.printStackTrace();
        }
    }




}
