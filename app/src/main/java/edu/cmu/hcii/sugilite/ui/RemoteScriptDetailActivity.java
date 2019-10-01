package edu.cmu.hcii.sugilite.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.AutomatorUtil;
import edu.cmu.hcii.sugilite.model.OperationBlockDescriptionRegenerator;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpression;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.recording.RecordingPopUpDialog;
import edu.cmu.hcii.sugilite.sharing.SugiliteScriptSharingHTTPQueryManager;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteAndroidAPIVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteGoogleCloudVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;

public class RemoteScriptDetailActivity extends ScriptDetailActivity {
    private Integer repoListingId;
    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_script_detail);
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(this);

        //load the local script
        if (savedInstanceState == null) {
            this.repoListingId = this.getIntent().getIntExtra("repoListingId", -1);
            this.scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            this.repoListingId = savedInstanceState.getInt("repoListingId");
            this.scriptName = savedInstanceState.getString("scriptName");
        }

        if(scriptName != null) {
            setTitle("View Remote Script: " + scriptName.replace(".SugiliteScript", ""));
        }
        OntologyDescriptionGenerator ontologyDescriptionGenerator = new OntologyDescriptionGenerator(getApplicationContext());
        if (repoListingId != -1) {
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    try {
                        script = sugiliteScriptSharingHTTPQueryManager.downloadScript(String.valueOf(repoListingId));
                        OperationBlockDescriptionRegenerator.regenerateScriptDescriptions(script, ontologyDescriptionGenerator);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    loadOperationList(script);
                }
            }).start();
        } else {
            PumiceDemonstrationUtil.showSugiliteToast("Can't load the remote script -- invalid ID!", Toast.LENGTH_SHORT);
        }

    }

    public void scriptDetailCancelButtonOnClick (View view) {
        onBackPressed();
    }

    public void scriptDetailDownloadButtonOnClick (View view) {
        downloadScriptToLocal();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST, 1, "View Information");
        menu.add(Menu.NONE, Menu.FIRST + 1, 2, "Download Script");

        return true;
    }

    private void viewScriptInfo() {
        PumiceDemonstrationUtil.showSugiliteToast("View script meta info", Toast.LENGTH_SHORT);
    }

    private void downloadScriptToLocal() {
        PumiceDemonstrationUtil.showSugiliteToast("Downloading the script to local", Toast.LENGTH_SHORT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // save the script locally
                try {
                    script.setScriptName("DOWNLOADED: " + script.getScriptName());
                    sugiliteScriptDao.save(script);
                    sugiliteScriptDao.commitSave();
                } catch (Exception e){
                    Log.e("RemoteScriptDetailActivity", "failed to save the script locally");
                    PumiceDemonstrationUtil.showSugiliteAlertDialog("Failed to save the script locally!");
                }

                // open a new LocalScriptDetailActivitpy to view the local script
                SugiliteData.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Intent scriptDetailIntent = new Intent(context, LocalScriptDetailActivity.class);
                        scriptDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        scriptDetailIntent.putExtra("scriptName", script.getScriptName());
                        PumiceDemonstrationUtil.showSugiliteAlertDialog("Successfully saved the script!");
                        startActivity(scriptDetailIntent);

                    }
                });
            }
        }).start();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(this, SugiliteMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("active_tab", "remote_scripts");
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case Menu.FIRST:
                //resume recording
                viewScriptInfo();
                break;
            case Menu.FIRST + 1:
                //rename the script
                downloadScriptToLocal();
                break;
        }
        return true;
    }
}
