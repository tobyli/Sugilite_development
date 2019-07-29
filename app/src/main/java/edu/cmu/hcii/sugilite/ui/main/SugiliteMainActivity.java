package edu.cmu.hcii.sugilite.ui.main;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;


import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTriggerDao;
import edu.cmu.hcii.sugilite.model.OperationBlockDescriptionRegenerator;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.ui.PumiceDialogActivity;
import edu.cmu.hcii.sugilite.sharing.DownloadRepoListTask;
import edu.cmu.hcii.sugilite.sharing.DownloadScriptTask;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;
import edu.cmu.hcii.sugilite.study.StudyConst;
import edu.cmu.hcii.sugilite.study.StudyDataUploadManager;
import edu.cmu.hcii.sugilite.ui.SettingsActivity;
import edu.cmu.hcii.sugilite.ui.dialog.AddTriggerDialog;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;


public class SugiliteMainActivity extends AppCompatActivity {
    ActionBar.Tab scriptListTab, triggerListTab;
    Fragment fragmentScriptListTab = new FragmentScriptListTab();
    Fragment fragmentTriggerListTab = new FragmentTriggerListTab();
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteTriggerDao sugiliteTriggerDao;
    private SugiliteData sugiliteData;
    private AlertDialog progressDialog;
    StudyDataUploadManager uploadManager;
    Context context;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean("EXIT", false)) {
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);
        uploadManager = new StudyDataUploadManager(this, sugiliteData);
        sugiliteData = getApplication() instanceof SugiliteData? (SugiliteData)getApplication() : new SugiliteData();
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            sugiliteScriptDao = new SugiliteScriptSQLDao(this);
        }
        else {
            sugiliteScriptDao = new SugiliteScriptFileDao(this, sugiliteData);
        }
        sugiliteTriggerDao = new SugiliteTriggerDao(this);
        this.context = this;

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(Const.appNameUpperCase);

        // Hide Actionbar Icon
        actionBar.setDisplayShowHomeEnabled(true);

        // Hide Actionbar Title
        actionBar.setDisplayShowTitleEnabled(true);

        // Create Actionbar Tabs
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set Tab Icon and Titles
        scriptListTab = actionBar.newTab().setText("Script List");
        triggerListTab = actionBar.newTab().setText("Trigger List");

        // Set Tab Listeners
        scriptListTab.setTabListener(new TabListener(fragmentScriptListTab));
        triggerListTab.setTabListener(new TabListener(fragmentTriggerListTab));

        // Add tabs to actionbar
        actionBar.addTab(scriptListTab);
        actionBar.addTab(triggerListTab);

        /*
        try {
            Process suProcess = Runtime.getRuntime().exec("su pm grant 'edu.cmu.hcii.sugilite' android.permission.INJECT_EVENTS");
            suProcess.waitFor();
        } catch (Exception e){
            e.printStackTrace();
        }
        */

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.clear_automation_queue) {
            int count = sugiliteData.getInstructionQueueSize();
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Clear Instruction Queue")
                    .setMessage("Are you sure to cleared " + count + " operations from the automation queue?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sugiliteData.clearInstructionQueue();
                            //set the system state back to DEFAULT_STATE
                            sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        }

        if (id == R.id.clear_script_list) {
            try {
                int count = (int) sugiliteScriptDao.size();
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Clearing Script List")
                        .setMessage("Are you sure to clear " + count + " scripts?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    sugiliteScriptDao.clear();
                                    sugiliteData.logUsageData(ScriptUsageLogManager.CLEAR_ALL_SCRIPTS, "N/A");
                                    if (fragmentScriptListTab instanceof FragmentScriptListTab)
                                        ((FragmentScriptListTab) fragmentScriptListTab).setUpScriptList();
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    if (fragmentScriptListTab instanceof FragmentScriptListTab)
                                        ((FragmentScriptListTab) fragmentScriptListTab).setUpScriptList();
                                    dialog.dismiss();
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return true;
        }

        if(id == R.id.add_trigger){
            try {
                new AddTriggerDialog(this, getLayoutInflater(), sugiliteData, sugiliteScriptDao, getPackageManager(), fragmentTriggerListTab).show();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return true;
        }

        if(id == R.id.clear_trigger_list){
            int count = (int)sugiliteTriggerDao.size();
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Clearing Trigger List")
                    .setMessage("Are you sure to clear " + count + " triggers?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sugiliteTriggerDao.clear();
                            if(fragmentTriggerListTab != null && fragmentTriggerListTab instanceof  FragmentTriggerListTab)
                                ((FragmentTriggerListTab)fragmentTriggerListTab).setUpTriggerList();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(fragmentTriggerListTab != null && fragmentTriggerListTab instanceof  FragmentTriggerListTab)
                                ((FragmentTriggerListTab)fragmentTriggerListTab).setUpTriggerList();
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        }

        if(id == R.id.upload_scripts){
            //progress dialog for loading the script
            progressDialog = new AlertDialog.Builder(this).setMessage(Const.LOADING_MESSAGE).create();
            progressDialog.getWindow().setType(OVERLAY_TYPE);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    List<SugiliteStartingBlock> scripts = null;
                    int uploadJSONCount = 0, uploadFileCount = 0;
                    try {
                        scripts = sugiliteScriptDao.getAllScripts();
                        if(scripts != null && uploadManager != null){
                            //upload JSON first
                            for(SugiliteStartingBlock script : scripts) {
                                uploadManager.uploadScriptJSON(script);
                                uploadJSONCount ++;
                                if (sugiliteScriptDao instanceof SugiliteScriptFileDao) {
                                    //upload script file only if SugiliteScriptFileDao is in use
                                    String scriptPath = ((SugiliteScriptFileDao) sugiliteScriptDao).getScriptPath(script.getScriptName());
                                    uploadManager.uploadScript(scriptPath, script.getCreatedTime());
                                    uploadFileCount ++;
                                }

                            }
                            String directoryPath = context.getFilesDir().getPath().toString();

                            //start uploading the usage log
                            File usageLog = new File(directoryPath + "/" + StudyConst.SCRIPT_USAGE_LOG_FILE_NAME);
                            if(usageLog.exists()) {
                                uploadManager.uploadScript(usageLog.getPath(), Calendar.getInstance().getTimeInMillis());
                                System.out.println("USAGE LOG UPLOADED");
                            }
                            else {
                                System.out.println("usage log doesn't exist!");
                            }
                            //finish uploading the usage log

                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    final int finalJSONCount = uploadJSONCount, finalFileCount = uploadFileCount;
                    Runnable dismissDialog = new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(context, "Uploaded " + finalJSONCount + " JSONs and " + finalFileCount + " files", Toast.LENGTH_SHORT).show();
                        }
                    };
                    runOnUiThread(dismissDialog);
                }
            }).start();
            return true;
        }

        if(id == R.id.clear_usage_log){
            new ScriptUsageLogManager(context).clearLog();
            return true;
        }
        if(id == R.id.launch_pumice){
            //launch pumice
            Intent intent = new Intent(this, PumiceDialogActivity.class);
            startActivity(intent);
            return true;

        }
        if(id == R.id.download_repo) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ExecutorService executor = Executors.newFixedThreadPool(1);
                    try {
                        DownloadRepoListTask repoListTask = new DownloadRepoListTask();
                        List<DownloadRepoListTask.SugiliteRepoListing> repo = executor.submit(repoListTask).get();
                        for (DownloadRepoListTask.SugiliteRepoListing listing : repo) {
                            DownloadScriptTask scriptTask = new DownloadScriptTask();
                            scriptTask.setId(listing.id + "");
                            SugiliteStartingBlock script = executor.submit(scriptTask).get();
                            script.setScriptName("DOWNLOADED: " + script.getScriptName());
                            OntologyDescriptionGenerator odg = new OntologyDescriptionGenerator(getApplicationContext());
                            OperationBlockDescriptionRegenerator.regenerateScriptDescriptions(script, odg);
                            sugiliteScriptDao.save(script);
                            sugiliteScriptDao.commitSave();
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Runnable dismissDialog = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (fragmentScriptListTab instanceof FragmentScriptListTab)
                                    ((FragmentScriptListTab) fragmentScriptListTab).setUpScriptList();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    runOnUiThread(dismissDialog);
                }
            }).start();
        }
        if(id == R.id.download_last_script) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ExecutorService executor = Executors.newFixedThreadPool(1);
                    try {
                        DownloadRepoListTask repoListTask = new DownloadRepoListTask();
                        List<DownloadRepoListTask.SugiliteRepoListing> repo = executor.submit(repoListTask).get();
                        DownloadRepoListTask.SugiliteRepoListing listing = repo.get(repo.size() - 1);
                        Log.i("SugiliteMainActivity", listing.id + " " + listing.title);

                        DownloadScriptTask scriptTask = new DownloadScriptTask();
                        scriptTask.setId(listing.id + "");
                        SugiliteStartingBlock script = executor.submit(scriptTask).get();
                        script.setScriptName("DOWNLOADED: " + script.getScriptName());
                        OntologyDescriptionGenerator odg = new OntologyDescriptionGenerator(getApplicationContext());
                        OperationBlockDescriptionRegenerator.regenerateScriptDescriptions(script, odg);
                        sugiliteScriptDao.save(script);
                        sugiliteScriptDao.commitSave();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Runnable dismissDialog = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (fragmentScriptListTab instanceof FragmentScriptListTab)
                                    ((FragmentScriptListTab) fragmentScriptListTab).setUpScriptList();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    runOnUiThread(dismissDialog);
                }
            }).start();
        }
        return super.onOptionsItemSelected(item);
    }
}
