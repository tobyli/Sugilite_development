package edu.cmu.hcii.sugilite.ui.main;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TabHost;


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
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.ui.PumiceDialogActivity;
import edu.cmu.hcii.sugilite.sharing.SugiliteScriptSharingHTTPQueryManager;
import edu.cmu.hcii.sugilite.sharing.model.SugiliteRepoListing;
import edu.cmu.hcii.sugilite.sovite.visual.text_selection.SoviteSetTextParameterDialog;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;
import edu.cmu.hcii.sugilite.study.StudyConst;
import edu.cmu.hcii.sugilite.study.StudyDataUploadManager;
import edu.cmu.hcii.sugilite.ui.SettingsActivity;
import edu.cmu.hcii.sugilite.ui.dialog.AddTriggerDialog;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;
import static edu.cmu.hcii.sugilite.recording.SugiliteScreenshotManager.REQUEST_MEDIA_PROJECTION;


public class SugiliteMainActivity extends AppCompatActivity {
    private ActionBar.Tab scriptListTab, triggerListTab, remoteScriptListTab;
    private Fragment fragmentScriptListTab = new FragmentScriptListTab();
    private Fragment fragmentTriggerListTab = new FragmentTriggerListTab();
    private Fragment fragmentRemoteScriptListTab = new FragmentRemoteScriptListTab();
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteTriggerDao sugiliteTriggerDao;
    private SugiliteData sugiliteData;
    private SugiliteProgressDialog progressDialog;
    private StudyDataUploadManager uploadManager;
    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;
    private Context context;
    private MediaProjectionManager mMediaProjectionManager;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean("EXIT", false)) {
            finish();
            return;
        }


        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);

        this.mMediaProjectionManager = (MediaProjectionManager)getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        this.uploadManager = new StudyDataUploadManager(this, sugiliteData);
        this.sugiliteData = getApplication() instanceof SugiliteData? (SugiliteData)getApplication() : new SugiliteData();
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(this);
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(this);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(this, sugiliteData);
        }
        this.sugiliteTriggerDao = new SugiliteTriggerDao(this);
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
        this.scriptListTab = actionBar.newTab().setText("Local Scripts");
        this.triggerListTab = actionBar.newTab().setText("Triggers");
        this.remoteScriptListTab = actionBar.newTab().setText("Remote Scripts");

        // Set Tab Listeners
        scriptListTab.setTabListener(new TabListener(fragmentScriptListTab));
        triggerListTab.setTabListener(new TabListener(fragmentTriggerListTab));
        remoteScriptListTab.setTabListener(new TabListener(fragmentRemoteScriptListTab));

        // Add tabs to actionbar
        actionBar.addTab(scriptListTab);
        actionBar.addTab(triggerListTab);
        actionBar.addTab(remoteScriptListTab);

        // switch to the active tab
        String activeTab = null;
        //load the local script
        if (savedInstanceState == null) {
            activeTab = this.getIntent().getStringExtra("active_tab");
        } else {
            activeTab = savedInstanceState.getString("active_tab");
        }
        if (activeTab != null) {
            switch (activeTab) {
                case "remote_scripts":
                    remoteScriptListTab.select();
                    break;
                case "local_scripts":
                    scriptListTab.select();
                    break;
                case "triggers":
                    triggerListTab.select();
                    break;
            }
        }


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
    protected void onResume() {
        super.onResume();
        startScreenshotCaptureIntent();
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

        if (id == R.id.clear_hash_cache) {
            int size = SugiliteData.getScreenStringSaltedHashMap().size();
            SugiliteData.getScreenStringSaltedHashMap().clear();
            PumiceDemonstrationUtil.showSugiliteAlertDialog(String.format("Cleared %d entries in the hash cache!", size));
        }

        if(id == R.id.upload_scripts){
            //progress dialog for loading the script
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
                }
            }).start();
            return true;
        }

        if(id == R.id.test_feature){
            SoviteSetTextParameterDialog soviteSetTextParameterDialog = new SoviteSetTextParameterDialog(context, sugiliteData, new VariableValue<>("parameter1", "chicken sandwich"), "can you help me order a chicken sandwich from KFC", null, null, null, false);
            //SoviteSetTextParameterDialog soviteSetTextParameterDialog = new SoviteSetTextParameterDialog(context, new Variable("parameter1"),"world", "hello world");
            soviteSetTextParameterDialog.show();
            //new ScriptUsageLogManager(context).clearLog();
            return true;
        }
        if(id == R.id.launch_pumice){
            //launch pumice
            Intent intent = new Intent(this, PumiceDialogActivity.class);
            startActivity(intent);
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private Intent screenCaptureIntent = null;
    private int screenCaptureIntentResult = 0;
    private void startScreenshotCaptureIntent() {
        if (screenCaptureIntent != null && screenCaptureIntentResult != 0) {
            sugiliteData.setScreenshotIntent(screenCaptureIntent);
            sugiliteData.setScreenshotResult(screenCaptureIntentResult);
        } else {
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            sugiliteData.setScreenshotMediaProjectionManager(mMediaProjectionManager);
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }else if(data != null && resultCode != 0){
                Log.i("SugiliteMainActivity", "user agree the application to capture screen");
                //Service1.mResultCode = resultCode;
                //Service1.mResultData = data;
                screenCaptureIntentResult = resultCode;
                screenCaptureIntent = data;
                sugiliteData.setScreenshotResult(resultCode);
                sugiliteData.setScreenshotIntent(data);
            }
        }
    }


}
