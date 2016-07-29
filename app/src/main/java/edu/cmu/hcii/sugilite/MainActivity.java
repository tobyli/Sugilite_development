package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

public class MainActivity extends AppCompatActivity {
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private ServiceStatusManager serviceStatusManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        View addButton = findViewById(R.id.addButton);
        serviceStatusManager = new ServiceStatusManager(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sugiliteScriptDao = new SugiliteScriptDao(this);
        sugiliteData = (SugiliteData)getApplication();
        setTitle("Sugilite Script List");
        //TODO: confirm overwrite when duplicated name
        //TODO: combine the two instances of script creation
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                sugiliteData.clearInstructionQueue();
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                final EditText scriptName = new EditText(v.getContext());
                scriptName.setText("New Script");
                scriptName.setSelectAllOnFocus(true);
                builder.setMessage("Specify the name for your new script")
                        .setView(scriptName)
                        .setPositiveButton("Start Recording", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!serviceStatusManager.isRunning()){
                                    //prompt the user if the accessiblity service is not active
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(v.getContext());
                                    builder1.setTitle("Service not running")
                                            .setMessage("The Sugilite accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    serviceStatusManager.promptEnabling();
                                                    //do nothing
                                                }
                                            }).show();
                                }
                                else if (scriptName != null && scriptName.getText().toString().length() > 0) {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("scriptName", scriptName.getText().toString());
                                    editor.putBoolean("recording_in_process", true);
                                    editor.commit();
                                    //set the active script to the newly created script
                                    sugiliteData.initiateScript(scriptName.getText().toString() + ".SugiliteScript");
                                    sugiliteData.initiatedExternally = false;
                                    //save the newly created script to DB
                                    try {
                                        sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    //Toast.makeText(v.getContext(), "Changed script name to " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();
                                    setUpScriptList();
                                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                                    startMain.addCategory(Intent.CATEGORY_HOME);
                                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(startMain);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setTitle("New Script");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        setSupportActionBar(toolbar);
        setUpScriptList();
    }

    /**
     * update the script list displayed at the main activity according to the DB
     */
    private void setUpScriptList(){
        final ListView scriptList = (ListView)findViewById(R.id.scriptList);
        List<String> names = sugiliteScriptDao.getAllNames();
        List<String> displayNames = new ArrayList<>();
        for(String name : names){
            displayNames.add(new String(name).replace(".SugiliteScript", ""));
        }
        scriptList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayNames));
        final Context activityContext = this;
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info){
        super.onCreateContextMenu(menu, view, info);
        if(view instanceof TextView && ((TextView) view).getText() != null)
            menu.setHeaderTitle(((TextView) view).getText());
        else
            menu.setHeaderTitle("Sugilite Operation Menu");
        menu.add(0, ITEM_1, 0, "View");
        menu.add(0, ITEM_2, 0, "Rename");
        menu.add(0, ITEM_3, 0, "Share");
        menu.add(0, ITEM_4, 0, "Delete");
    }

    //TODO:implement context menu
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(info == null)
            return super.onContextItemSelected(item);
        switch (item.getItemId()){
        case ITEM_1:
            //open the view script activity
            if(info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                String scriptName = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
                final Intent scriptDetailIntent = new Intent(this, ScriptDetailActivity.class);
                scriptDetailIntent.putExtra("scriptName", scriptName);
                startActivity(scriptDetailIntent);
            }
            break;
        case ITEM_2:
            if(info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                final String scriptName = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final EditText newName = new EditText(this);
                builder.setView(newName)
                        .setTitle("Enter the new name for \"" + ((TextView) info.targetView).getText().toString() + "\"")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SugiliteStartingBlock startingBlock = sugiliteScriptDao.read(scriptName);
                                startingBlock.setScriptName(newName.getText().toString() + ".SugiliteScript");
                                try {
                                    sugiliteScriptDao.save(startingBlock);
                                    setUpScriptList();
                                    //sugiliteScriptDao.delete(scriptName);
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
        case ITEM_3:
            final String scriptName = ((TextView) info.targetView).getText().toString() + ".SugiliteScript";
            SugiliteStartingBlock startingBlock = sugiliteScriptDao.read(scriptName);
            SugiliteBlockJSONProcessor processor = new SugiliteBlockJSONProcessor(this.getApplicationContext());
            try {
                String json = processor.scriptToJson(startingBlock);
                System.out.println(json);
                SugiliteStartingBlock recoveredFromJSON = processor.jsonToScript(json);
                recoveredFromJSON.setScriptName("recovered_" + recoveredFromJSON.getScriptName());
                sugiliteScriptDao.save(recoveredFromJSON);
                setUpScriptList();

                sugiliteData.communicationController.sendAllScripts();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            Toast.makeText(this, "Sharing Script is not supported yet!", Toast.LENGTH_SHORT).show();
            break;
        case ITEM_4:
            if(info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                sugiliteScriptDao.delete(((TextView) info.targetView).getText().toString() + ".SugiliteScript");
                setUpScriptList();
            }
            break;
    }
    return super.onContextItemSelected(item);
    }

        @Override
    public void onResume(){
        super.onResume();
        setUpScriptList();
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
            int count = (int)sugiliteScriptDao.size();
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Clearing Script List")
                    .setMessage("Are you sure to clear " + count + " scripts?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sugiliteScriptDao.clear();
                            setUpScriptList();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setUpScriptList();
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
