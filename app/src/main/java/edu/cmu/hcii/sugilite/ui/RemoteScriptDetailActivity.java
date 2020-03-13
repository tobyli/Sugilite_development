package edu.cmu.hcii.sugilite.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.OperationBlockDescriptionRegenerator;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.sharing.SugiliteScriptSharingHTTPQueryManager;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;

public class RemoteScriptDetailActivity extends ScriptDetailActivity {
    private Integer repoListingId;
    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_script_detail);
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(this);
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();

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
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                    sugiliteScriptDao.commitSave(null);
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
                        PumiceDemonstrationUtil.showSugiliteAlertDialog("Successfully downloaded the script!");
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
