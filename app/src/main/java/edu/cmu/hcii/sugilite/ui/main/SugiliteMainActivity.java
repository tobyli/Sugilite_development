package edu.cmu.hcii.sugilite.ui.main;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;


import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTriggerDao;
import edu.cmu.hcii.sugilite.ui.SettingsActivity;
import edu.cmu.hcii.sugilite.ui.dialog.AddTriggerDialog;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;


public class SugiliteMainActivity extends AppCompatActivity {
    ActionBar.Tab scriptListTab, triggerListTab;
    Fragment fragmentScriptListTab = new FragmentScriptListTab();
    Fragment fragmentTriggerListTab = new FragmentTriggerListTab();
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteTriggerDao sugiliteTriggerDao;
    private SugiliteData sugiliteData;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);
        sugiliteData = getApplication() instanceof SugiliteData? (SugiliteData)getApplication() : new SugiliteData();
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            sugiliteScriptDao = new SugiliteScriptSQLDao(this);
        else
            sugiliteScriptDao = new SugiliteScriptFileDao(this, sugiliteData);
        sugiliteTriggerDao = new SugiliteTriggerDao(this);


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

        return super.onOptionsItemSelected(item);
    }
}
