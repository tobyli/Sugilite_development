package edu.cmu.hcii.sugilite.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

public class ScriptSourceActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private ActivityManager activityManager;
    private ServiceStatusManager serviceStatusManager;
    private String scriptName;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private Activity context;
    private SugiliteStartingBlock script;
    private EditText sourceEditText;
    private SugiliteScriptParser sugiliteScriptParser;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_source);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ontologyDescriptionGenerator = new OntologyDescriptionGenerator();
        sugiliteScriptParser = new SugiliteScriptParser(ontologyDescriptionGenerator);
        serviceStatusManager = ServiceStatusManager.getInstance(this);
        if (savedInstanceState == null) {
            scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            scriptName = savedInstanceState.getString("scriptName");
        }
        sugiliteData = (SugiliteData) getApplication();
        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            sugiliteScriptDao = new SugiliteScriptSQLDao(this);
        else
            sugiliteScriptDao = new SugiliteScriptFileDao(this, sugiliteData);
        this.context = this;
        if (scriptName != null) {
            setTitle("Edit Source: " + PumiceDemonstrationUtil.removeScriptExtension(scriptName));
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    script = sugiliteScriptDao.read(scriptName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Runnable loadOperation = new Runnable() {
                    @Override
                    public void run() {
                        loadScriptSource();
                    }
                };

                context.runOnUiThread(loadOperation);
            }
        }).start();

        //add back the duck icon
        if (sugiliteData != null && sugiliteData.statusIconManager != null && serviceStatusManager != null) {
            if (!sugiliteData.statusIconManager.isShowingIcon() && serviceStatusManager.isRunning()) {
                sugiliteData.statusIconManager.addStatusIcon();
            }
        }
    }

    public void loadScriptSource() {
        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.loading_script_message);
        progressDialog.show();

        sourceEditText = (EditText) findViewById(R.id.edit_text_source);
        sourceEditText.setHorizontallyScrolling(false);
        String source = SugiliteScriptParser.scriptToString(script);
        sourceEditText.setText(source.replace("\n", "\n\n"));

        progressDialog.dismiss();
    }

    public void scriptSourceRunSaveButtonOnClick(final View view) {
        SugiliteStartingBlock newScript = saveScript();
        if(newScript != null) {
            final Context activityContext = this;
            new AlertDialog.Builder(this)
                    .setTitle("Run Script")
                    .setMessage("Are you sure you want to run this script?")
                    .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //clear the queue first before adding new instructions
                            PumiceDemonstrationUtil.executeScript(context, serviceStatusManager, script, sugiliteData, sharedPreferences, false, null, null, null);

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    public void scriptSourceCancelButtonOnClick(final View view) {
        Intent intent = new Intent(this, SugiliteMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void scriptSourceSaveButtonOnClick(final View view){
        SugiliteStartingBlock script = saveScript();
        if(script != null) {
            scriptSourceCancelButtonOnClick(view);
        }
    }

    private SugiliteStartingBlock saveScript(){
        try{
            String input = sourceEditText.getText().toString();
            SugiliteStartingBlock newScript = sugiliteScriptParser.parseBlockFromString(input);
            newScript.setScriptName("EDITED: " + scriptName);
            sugiliteScriptDao.save(newScript);
            sugiliteScriptDao.commitSave(null);
            return newScript;
        } catch (Exception e){
            e.printStackTrace();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Failed to parse the script");
            builder.setMessage("Sugilite failed to parse your script, please check if the script is correct.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.show();
        }
        return null;
    }


    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(this, SugiliteMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }



}
