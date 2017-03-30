package edu.cmu.hcii.sugilite.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.Generalizer;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ui.ScriptDebuggingActivity;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.ui.dialog.NewScriptDialog;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * Created by toby on 1/16/17.
 */

public class FragmentScriptListTab extends Fragment {
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private ServiceStatusManager serviceStatusManager;
    private Generalizer generalizer;
    private View rootView;
    private Activity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        Intent intent = activity.getIntent();
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_script_list, container, false);
        View addButton = rootView.findViewById(R.id.addButton);
        serviceStatusManager = ServiceStatusManager.getInstance(activity);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        sugiliteData = activity.getApplication() instanceof SugiliteData? (SugiliteData)activity.getApplication() : new SugiliteData();
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(activity);
        else
            this.sugiliteScriptDao = new SugiliteScriptFileDao(activity, sugiliteData);
        generalizer = new Generalizer(activity, sugiliteData);
        activity.setTitle("Sugilite Script List");
        //TODO: confirm overwrite when duplicated name
        //TODO: combine the two instances of script creation
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                NewScriptDialog newScriptDialog = new NewScriptDialog(v.getContext(), sugiliteScriptDao, serviceStatusManager, sharedPreferences, sugiliteData, false, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            setUpScriptList();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }, null);
                newScriptDialog.show();
            }
        });
        try {
            setUpScriptList();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return rootView;
    }

    /**
     * update the script list displayed at the main activity according to the DB
     */
    public void setUpScriptList() throws Exception{
        final ListView scriptList = (ListView)rootView.findViewById(R.id.scriptList);
        List<String> names = sugiliteScriptDao.getAllNames();
        List<String> displayNames = new ArrayList<>();
        for(String name : names){
            displayNames.add(new String(name).replace(".SugiliteScript", ""));
        }
        System.out.println("showing " + names.size() + " scripts: " + displayNames);
        scriptList.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, displayNames));
        final Context activityContext = activity;
        scriptList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String scriptName = (String) scriptList.getItemAtPosition(position) + ".SugiliteScript";
                final Intent scriptDetailIntent = new Intent(activityContext, ScriptDetailActivity.class);
                scriptDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                scriptDetailIntent.putExtra("scriptName", scriptName);
                startActivity(scriptDetailIntent);
            }
        });
        registerForContextMenu(scriptList);
    }

    private static final int ITEM_1 = Menu.FIRST;
    private static final int ITEM_2 = Menu.FIRST + 1;
    private static final int ITEM_3 = Menu.FIRST + 2;
    private static final int ITEM_4 = Menu.FIRST + 3;
    private static final int ITEM_5 = Menu.FIRST + 4;
    private static final int ITEM_6 = Menu.FIRST + 5;


    //context menu are the long-click menus for each script
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info){
        super.onCreateContextMenu(menu, view, info);
        if(view instanceof TextView && ((TextView) view).getText() != null)
            menu.setHeaderTitle(((TextView) view).getText());
        else
            menu.setHeaderTitle("Sugilite Operation Menu");
        menu.add(0, ITEM_1, 0, "View");
        menu.add(0, ITEM_2, 0, "Debug");
        menu.add(0, ITEM_3, 0, "Rename");
        menu.add(0, ITEM_4, 0, "Share");
        menu.add(0, ITEM_5, 0, "Generalize");
        menu.add(0, ITEM_6, 0, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(info == null)
            return super.onContextItemSelected(item);
        try {
            switch (item.getItemId()) {
                case ITEM_1:
                    //open the view script activity
                    if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                        String scriptName = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
                        final Intent scriptDetailIntent = new Intent(activity, ScriptDetailActivity.class);
                        scriptDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        scriptDetailIntent.putExtra("scriptName", scriptName);
                        startActivity(scriptDetailIntent);
                    }
                    break;
                case ITEM_2:
                    //open the debug activity
                    if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                        String scriptName = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
                        final Intent scriptDetailIntent = new Intent(activity, ScriptDebuggingActivity.class);
                        scriptDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        scriptDetailIntent.putExtra("scriptName", scriptName);
                        startActivity(scriptDetailIntent);
                    }
                    break;
                case ITEM_3:
                    if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                        final String scriptName = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        final EditText newName = new EditText(activity);
                        builder.setView(newName)
                                .setTitle("Enter the new name for \"" + ((TextView) info.targetView).getText().toString() + "\"")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        SugiliteStartingBlock startingBlock = null;
                                        try {
                                             startingBlock = sugiliteScriptDao.read(scriptName);
                                        }
                                        catch (Exception e){
                                            e.printStackTrace();
                                        }
                                        startingBlock.setScriptName(newName.getText().toString() + ".SugiliteScript");
                                        try {
                                            sugiliteScriptDao.save(startingBlock);
                                            sugiliteScriptDao.commitSave();
                                            setUpScriptList();
                                            sugiliteScriptDao.delete(scriptName);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();

                    }
                    break;
                case ITEM_4:
                    final String scriptName = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
                    SugiliteStartingBlock startingBlock = sugiliteScriptDao.read(scriptName);
                    SugiliteBlockJSONProcessor processor = new SugiliteBlockJSONProcessor(activity);
                    try {
                        String json = processor.scriptToJson(startingBlock);
                        System.out.println(json);
                        SugiliteStartingBlock recoveredFromJSON = processor.jsonToScript(json);
                        recoveredFromJSON.setScriptName("recovered_" + recoveredFromJSON.getScriptName());
                        sugiliteScriptDao.save(recoveredFromJSON);
                        sugiliteScriptDao.commitSave();
                        setUpScriptList();

                        sugiliteData.communicationController.sendAllScripts();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(activity, "Sharing Script is not supported yet!", Toast.LENGTH_SHORT).show();
                    break;
                case ITEM_5:
                    final String scriptName1 = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
                    SugiliteStartingBlock startingBlock1 = sugiliteScriptDao.read(scriptName1);
                    generalizer.generalize(startingBlock1);
                    setUpScriptList();
                    break;
                case ITEM_6:
                    if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                        sugiliteScriptDao.delete(((TextView) info.targetView).getText().toString() + ".SugiliteScript");
                        setUpScriptList();
                    }
                    break;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();
        try {
            setUpScriptList();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }




}
