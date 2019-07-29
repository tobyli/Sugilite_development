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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.longrunning.Operation;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.NewScriptGeneralizer;
import edu.cmu.hcii.sugilite.model.OperationBlockDescriptionRegenerator;
import edu.cmu.hcii.sugilite.model.ScriptQueryHasher;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.util.ScriptPrinter;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.sharing.PrepareScriptForSharingTask;
import edu.cmu.hcii.sugilite.sharing.UploadScriptTask;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;
import edu.cmu.hcii.sugilite.ui.ScriptDebuggingActivity;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.ui.ScriptSourceActivity;
import edu.cmu.hcii.sugilite.ui.dialog.NewScriptDialog;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * Created by toby on 1/16/17.
 */

public class FragmentScriptListTab extends Fragment {
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private ServiceStatusManager serviceStatusManager;
    private View rootView;
    private Activity activity;
    private AlertDialog progressDialog;
    private NewScriptGeneralizer newScriptGeneralizer;
    private ScriptQueryHasher scriptQueryHasher;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.activity = getActivity();
        super.onCreate(savedInstanceState);
        this.rootView = inflater.inflate(R.layout.fragment_script_list, container, false);
        this.serviceStatusManager = ServiceStatusManager.getInstance(activity);
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.sugiliteData = activity.getApplication() instanceof SugiliteData? (SugiliteData)activity.getApplication() : new SugiliteData();
        this.newScriptGeneralizer = new NewScriptGeneralizer(activity);
        this.scriptQueryHasher = new ScriptQueryHasher(activity);
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator(getContext());

        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(activity);
        else
            this.sugiliteScriptDao = new SugiliteScriptFileDao(activity, sugiliteData);
        this.activity.setTitle("Sugilite Script List");
        //TODO: confirm overwrite when duplicated name
        //TODO: combine the two instances of script creation

        View addButton = rootView.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                NewScriptDialog newScriptDialog = new NewScriptDialog(v.getContext(), inflater, sugiliteScriptDao, serviceStatusManager, sharedPreferences, sugiliteData, false, new DialogInterface.OnClickListener() {
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

        //add back the duck icon
        if(sugiliteData != null && sugiliteData.statusIconManager != null && serviceStatusManager != null){
            if(!sugiliteData.statusIconManager.isShowingIcon() && serviceStatusManager.isRunning()){
                sugiliteData.statusIconManager.addStatusIcon();
            }
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

    private static final int ITEM_VIEW = Menu.FIRST;
    private static final int ITEM_RUN = Menu.FIRST + 1;
    private static final int ITEM_DEBUG = Menu.FIRST + 2;
    private static final int ITEM_RENAME = Menu.FIRST + 3;
    private static final int ITEM_SHARE = Menu.FIRST + 4;
    private static final int ITEM_GENERALIZE = Menu.FIRST + 5;
    private static final int ITEM_PRINT_DEBUG_INFO = Menu.FIRST + 6;
    private static final int ITEM_EDIT_SOURCE = Menu.FIRST + 7;
    private static final int ITEM_DELETE = Menu.FIRST + 8;
    private static final int ITEM_HASH_STRINGS = Menu.FIRST + 9;
    private static final int ITEM_DUPLICATE = Menu.FIRST + 10;

    //context menu are the long-click menus for each script
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info){
        super.onCreateContextMenu(menu, view, info);
        if(view instanceof TextView && ((TextView) view).getText() != null)
            menu.setHeaderTitle(((TextView) view).getText());
        else
            menu.setHeaderTitle("Sugilite Operation Menu");

        //TODO: add run script here
        menu.add(0, ITEM_VIEW, 0, "View");
        menu.add(0, ITEM_RUN, 0, "Run");
        menu.add(0, ITEM_DEBUG, 0, "Debug");
        menu.add(0, ITEM_RENAME, 0, "Rename");
        menu.add(0, ITEM_SHARE, 0, "Share");
        menu.add(0, ITEM_GENERALIZE, 0, "Generalize");
        menu.add(0, ITEM_PRINT_DEBUG_INFO, 0, "Print Debug Info");
        menu.add(0, ITEM_EDIT_SOURCE, 0, "Edit Source");
        menu.add(0, ITEM_DELETE, 0, "Delete");
        menu.add(0, ITEM_HASH_STRINGS, 0, "Hash Strings");
        menu.add(0, ITEM_DUPLICATE, 0, "Duplicate");

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(info == null)
            return super.onContextItemSelected(item);
        try {
            // delete before loading script in case serialization is broken
            // this should allow us to delete broken scripts
            if (item.getItemId() == ITEM_DELETE) {
                if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                    sugiliteScriptDao.delete(((TextView) info.targetView).getText().toString() + ".SugiliteScript");
                    setUpScriptList();
                }
                return super.onContextItemSelected(item);
            }

            final String scriptName = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
            final SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName);
            switch (item.getItemId()) {
                case ITEM_VIEW:
                    //open the view script activity
                    if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                        final Intent scriptDetailIntent = new Intent(activity, ScriptDetailActivity.class);
                        scriptDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        scriptDetailIntent.putExtra("scriptName", scriptName);
                        startActivity(scriptDetailIntent);
                    }
                    break;
                case ITEM_RUN:
                    //run the script
                    new AlertDialog.Builder(activity)
                            .setTitle("Run Script")
                            .setMessage("Are you sure you want to run this script?")
                            .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //clear the queue first before adding new instructions

                                    PumiceDemonstrationUtil.executeScript(activity, serviceStatusManager, script, sugiliteData, activity.getLayoutInflater(), sharedPreferences, null, null, null);

                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    break;
                case ITEM_DEBUG:
                    //open the debug activity
                    if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                        final Intent scriptDetailIntent = new Intent(activity, ScriptDebuggingActivity.class);
                        scriptDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        scriptDetailIntent.putExtra("scriptName", scriptName);
                        startActivity(scriptDetailIntent);
                    }
                    break;
                case ITEM_RENAME:
                    //rename
                    if (info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
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
                                        if (startingBlock != null) {
                                            startingBlock.setScriptName(newName.getText().toString() + ".SugiliteScript");
                                            try {
                                                sugiliteScriptDao.save(startingBlock);
                                                sugiliteScriptDao.commitSave();
                                                setUpScriptList();
                                                sugiliteScriptDao.delete(scriptName);
                                                setUpScriptList();
                                                sugiliteData.logUsageData(ScriptUsageLogManager.REMOVE_SCRIPT, scriptName);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
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
                case ITEM_SHARE:
                    //share
                    // SugiliteBlockJSONProcessor processor = new SugiliteBlockJSONProcessor(activity);
                    // try {
                    //     String json = processor.scriptToJson(script);
                    //     System.out.println(json);
                    //     SugiliteStartingBlock recoveredFromJSON = processor.jsonToScript(json);
                    //     recoveredFromJSON.setScriptName("recovered_" + recoveredFromJSON.getScriptName());
                    //     sugiliteScriptDao.save(recoveredFromJSON);
                    //     sugiliteScriptDao.commitSave();
                    //     setUpScriptList();

                    //     sugiliteData.communicationController.sendAllScripts();
                    // } catch (Exception e) {
                    //     e.printStackTrace();
                    // }
                    // Toast.makeText(activity, "Sharing Script is not supported yet!", Toast.LENGTH_SHORT).show();
                    progressDialog = new AlertDialog.Builder(activity).setMessage(Const.LOADING_MESSAGE).create();
                    progressDialog.getWindow().setType(OVERLAY_TYPE);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            PrepareScriptForSharingTask task = new PrepareScriptForSharingTask();
                            task.setScript(script);
                            ExecutorService executor = Executors.newFixedThreadPool(1);
                            try {
                                SugiliteStartingBlock sharable = executor.submit(task).get();
                                UploadScriptTask uploadTask = new UploadScriptTask();
                                uploadTask.setTitle(scriptName);
                                uploadTask.setAuthor("demo");
                                uploadTask.setScript(sharable);
                                String id = executor.submit(uploadTask).get();
                                Log.i("Upload script", "Script shared with id : " + id);
                                OperationBlockDescriptionRegenerator.regenerateScriptDescriptions(sharable, ontologyDescriptionGenerator);
                                sharable.setScriptName("UPLOADED: " + scriptName);
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
                                    progressDialog.dismiss();
                                    try {
                                        setUpScriptList();
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            };
                            if(activity != null){
                                activity.runOnUiThread(dismissDialog);
                            }
                        }
                    }).start();
                    break;
                case ITEM_GENERALIZE:
                    //generalize
                    progressDialog = new AlertDialog.Builder(activity).setMessage(Const.LOADING_MESSAGE).create();
                    progressDialog.getWindow().setType(OVERLAY_TYPE);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run()
                        {
                            try {
                                newScriptGeneralizer.extractParameters(script, scriptName.replace(".SugiliteScript", ""));
                                sugiliteScriptDao.save(script);
                                sugiliteScriptDao.commitSave();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            Runnable dismissDialog = new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    try {
                                        setUpScriptList();
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            };
                            if(activity != null){
                                activity.runOnUiThread(dismissDialog);
                            }
                        }
                    }).start();

                    break;
                case ITEM_PRINT_DEBUG_INFO:
                    //view debug info
                    System.out.println(ScriptPrinter.getStringScript(script));
                    break;
                case ITEM_EDIT_SOURCE:
                    //view and edit script source
                    final Intent scriptSourceIntent = new Intent(getContext(), ScriptSourceActivity.class);
                    scriptSourceIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    scriptSourceIntent.putExtra("scriptName", scriptName);
                    startActivity(scriptSourceIntent);
                    break;
                case ITEM_HASH_STRINGS:
                    // hash strings (DEBUG)
                    progressDialog = new AlertDialog.Builder(activity).setMessage(Const.LOADING_MESSAGE).create();
                    progressDialog.getWindow().setType(OVERLAY_TYPE);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run()
                        {
                            try {
                                scriptQueryHasher.hashOntologyQueries(script);
                                sugiliteScriptDao.save(script);
                                sugiliteScriptDao.commitSave();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            Runnable dismissDialog = new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    try {
                                        setUpScriptList();
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            };
                            if(activity != null){
                                activity.runOnUiThread(dismissDialog);
                            }
                        }
                    }).start();
                    break;
                case ITEM_DUPLICATE:
                    //duplicate
                    progressDialog = new AlertDialog.Builder(activity).setMessage(Const.LOADING_MESSAGE).create();
                    progressDialog.getWindow().setType(OVERLAY_TYPE);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run()
                        {
                            try {
                                script.setScriptName(scriptName.replace(".SugiliteScript", "") + "_copy" + ".SugiliteScript");
                                sugiliteScriptDao.save(script);
                                sugiliteScriptDao.commitSave();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            Runnable dismissDialog = new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    try {
                                        setUpScriptList();
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            };
                            if(activity != null){
                                activity.runOnUiThread(dismissDialog);
                            }
                        }
                    }).start();
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
