package edu.cmu.hcii.sugilite.ui.main;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTriggerDao;
import edu.cmu.hcii.sugilite.model.trigger.SugiliteTrigger;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.ui.dialog.AddTriggerDialog;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

public class FragmentTriggerListTab extends Fragment {
    private SugiliteTriggerDao triggerDao;
    private Activity activity;
    private View rootView;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private FragmentTriggerListTab fragmentTriggerListTab;
    private SugiliteTriggerDao sugiliteTriggerDao;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.activity = getActivity();
        this.triggerDao = new SugiliteTriggerDao(activity);
        this.rootView = inflater.inflate(R.layout.fragment_trigger_list, container, false);
        this.sugiliteData = (SugiliteData) activity.getApplication();
        this.fragmentTriggerListTab = this;
        this.sugiliteTriggerDao = new SugiliteTriggerDao(activity);

        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(activity);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(activity, sugiliteData);
        }
        View addButton = rootView.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                try {
                    new AddTriggerDialog(activity, sugiliteData, sugiliteScriptDao, activity.getPackageManager(), fragmentTriggerListTab).show();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        try {
            setUpTriggerList();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static final int ITEM_VIEW = Menu.FIRST;
    private static final int ITEM_DELETE = Menu.FIRST + 1;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        if (info instanceof AdapterView.AdapterContextMenuInfo &&
                ((AdapterView.AdapterContextMenuInfo) info).targetView instanceof TextView &&
                ((TextView) ((AdapterView.AdapterContextMenuInfo) info).targetView).getText() != null) {
            menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) info).targetView).getText());
        }
        else {
            menu.setHeaderTitle("Sugilite Operation Menu");
        }

        menu.add(0, ITEM_VIEW, 0, "View");
        menu.add(0, ITEM_DELETE, 0, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info == null)
            return super.onContextItemSelected(item);
        try {
            // delete before loading script in case serialization is broken
            // this should allow us to delete broken scripts
            if (item.getItemId() == ITEM_DELETE) {
                if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                    sugiliteTriggerDao.delete(((TextView) info.targetView).getText().toString());
                    PumiceDemonstrationUtil.showSugiliteAlertDialog(String.format("Successfully deleted the trigger \"%s\"!", ((TextView) info.targetView).getText().toString()));
                    setUpTriggerList();
                }
                return super.onContextItemSelected(item);
            }

            final String triggerName = ((TextView) info.targetView).getText().toString();
            switch (item.getItemId()) {
                case ITEM_VIEW:
                    //open the view script activity
                    triggerOnClick(triggerName);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onContextItemSelected(item);
    }

    /**
     * update the trigger list displayed at the main activity according to the DB
     */
    public void setUpTriggerList() {
        if (rootView != null) {
            final ListView triggerEntryList = (ListView) rootView.findViewById(R.id.triggerList);
            List<String> names = triggerDao.getAllNames();

            triggerEntryList.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, names));
            triggerEntryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String triggerName = (String) triggerEntryList.getItemAtPosition(position);
                    triggerOnClick(triggerName);
                }
            });
            registerForContextMenu(triggerEntryList);
        }
    }

    private void triggerOnClick (String triggerName) {
        SugiliteTrigger trigger = triggerDao.read(triggerName);
        try {
            AddTriggerDialog triggerDialog = new AddTriggerDialog(activity, sugiliteData, sugiliteScriptDao, activity.getPackageManager(), fragmentTriggerListTab);
            String errorMsg = null;
            try {
                triggerDialog.loadFromExistingTrigger(trigger);
            } catch (Exception e) {
                errorMsg = e.getMessage();
                //PumiceDemonstrationUtil.showSugiliteToast(e.getMessage(), Toast.LENGTH_SHORT);
            } finally {
                triggerDialog.show();
                if (errorMsg != null) {
                    PumiceDemonstrationUtil.showSugiliteAlertDialog(errorMsg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}