package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.util.concurrent.ExecutionException;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;

/**
 * @author toby
 * @date 12/9/17
 * @time 11:11 PM
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
    public boolean isListening = false;


    public VerbalInstructionTestDialog(SerializableUISnapshot serializableUISnapshot, Context context, LayoutInflater inflater, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.serializableUISnapshot = serializableUISnapshot;
        this.context = context;
        this.sugiliteVoiceRecognitionListener = new SugiliteVoiceRecognitionListener(context, this);
        this.sugiliteVerbalInstructionHTTPQueryManager = new SugiliteVerbalInstructionHTTPQueryManager(this);
        this.overlayManager = new VerbalInstructionOverlayManager(context, sugiliteData, sharedPreferences);
        this.gson = new Gson();
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
    }

    public void show(){
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        dialog.show();
        speakButton.performClick();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String userInput = instructionTextbox.getText().toString();
                Query query = new Query(userInput, serializableUISnapshot.triplesToString());

                //save the query locally
                dumpQuery(query);
                VerbalInstructionIconManager.dumpUISnapshot(serializableUISnapshot);

                //send the query
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            sugiliteVerbalInstructionHTTPQueryManager.sendQuery(query);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                //TODO: show loading popup
                thread.start();
                showProgressDialog();

            }
        });
    }


    private void dumpQuery(Query query){
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
        progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    @Override
    /**
     * callback for HTTP query
     */
    public void resultReceived(int responseCode, String result) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        //raw response
        System.out.print(responseCode + ": " + result);

        //de-serialize to VerbalInstructionResults
        VerbalInstructionResults results = gson.fromJson(result, VerbalInstructionResults.class);
        for (VerbalInstructionResults.VerbalInstructionResult verbalInstructionResult : results.getQueries()) {
            System.out.println(gson.toJson(verbalInstructionResult));
        }

        //find matches
        Map<String, SugiliteSerializableEntity> idEntityMap = serializableUISnapshot.getSugiliteEntityIdSugiliteEntityMap();
        for (VerbalInstructionResults.VerbalInstructionResult verbalInstructionResult : results.getQueries()) {
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

                    //node, nodeId, corresponding VerbalInstructionResult, VerbalInstructionResults
                    overlayManager.addOverlay(node, filteredNodeNodeIdMap.get(node), verbalInstructionResult, results.getQueries(), serializableUISnapshot);
                }
                break;
            }
        }
        dialog.dismiss();
    }

    @Override
    public void listeningStarted() {
        isListening = true;
    }

    @Override
    public void listeningEnded() {
        isListening = false;
    }

    @Override
    public void resultAvailable(List<String> matches) {
        if(matches.size() > 0) {
            instructionTextbox.setText(matches.get(0));
        }
    }

    @Override
    public void runOnMainThread(Runnable r) {
        mainLayout.post(r);
    }

    public class Query{
        private String mode;
        private String userInput;
        private List<List<String>> triples;

        public Query(String mode, String userInput, List<List<String>> triples){
            this.mode = mode;
            this.userInput = userInput;
            this.triples = triples;
        }

        public Query(String userInput, List<List<String>> triples){
            this("USER_COMMAND", userInput, triples);
        }

    }
}
