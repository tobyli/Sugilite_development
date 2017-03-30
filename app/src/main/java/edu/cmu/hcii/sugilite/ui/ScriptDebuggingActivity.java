package edu.cmu.hcii.sugilite.ui;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.Automator;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.recording.RecordingPopUpDialog;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;

import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;


/**
 * the debug activity should:
 * - support adding breakpoint
 * - support single stepping
 * - transparent debugging overlay
 */

public class ScriptDebuggingActivity extends AppCompatActivity {

    private LinearLayout operationStepList;
    private SugiliteData sugiliteData;
    private String scriptName;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteStartingBlock script;
    private ActivityManager activityManager;
    private ServiceStatusManager serviceStatusManager;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_debugging);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        serviceStatusManager = ServiceStatusManager.getInstance(this);
        if (savedInstanceState == null) {
            scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            scriptName = savedInstanceState.getString("scriptName");
        }
        sugiliteData = (SugiliteData)getApplication();
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            sugiliteScriptDao = new SugiliteScriptSQLDao(this);
        else
            sugiliteScriptDao = new SugiliteScriptFileDao(this, sugiliteData);        //script is read from the DB
        try {
            script = sugiliteScriptDao.read(scriptName);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        this.context = this;
        if(scriptName != null)
            setTitle("View Script: " + scriptName.replace(".SugiliteScript", ""));
        loadOperationList();

    }

    //TODO: set up operation on resume

    public void loadOperationList(){
        operationStepList = (LinearLayout)findViewById(R.id.operation_list_view);
        operationStepList.removeAllViews();
        SugiliteBlock iterBlock = script;
        while(iterBlock != null){
            operationStepList.addView(getViewForBlock(iterBlock));
            if (iterBlock instanceof SugiliteStartingBlock)
                iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlock();
            else if (iterBlock instanceof SugiliteOperationBlock)
                iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlock();
            else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlock();
            else if (iterBlock instanceof SugiliteErrorHandlingForkBlock)
                break;
            else
                new Exception("unsupported block type").printStackTrace();
        }

        TextView tv = new TextView(context);
        tv.setText(Html.fromHtml("<b>END SCRIPT</b>"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setPadding(10, 10, 10, 10);
        operationStepList.addView(tv);
    }

    /**
     * recursively construct the list of operations
     * @param block
     * @return
     */
    public View getViewForBlock(SugiliteBlock block) {
        if (block instanceof SugiliteStartingBlock) {
            TextView tv = new TextView(context);
            tv.setText(Html.fromHtml(block.getDescription()));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);
            tv.setOnTouchListener(textViewOnTouchListener);
            registerForContextMenu(tv);
            return tv;
        } else if (block instanceof SugiliteOperationBlock) {
            return new DebuggingOperationView(context, (SugiliteOperationBlock) block);
        } else if (block instanceof SugiliteErrorHandlingForkBlock) {
            LinearLayout mainLayout = new LinearLayout(context);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            TextView tv = new TextView(context);
            tv.setText(Html.fromHtml("<b>" + "TRY" + "</b>"));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);
            registerForContextMenu(tv);
            mainLayout.addView(tv);
            LinearLayout originalBranch = new LinearLayout(context);
            originalBranch.setOrientation(LinearLayout.VERTICAL);
            originalBranch.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            SugiliteBlock iterBlock = ((SugiliteErrorHandlingForkBlock) block).getOriginalNextBlock();

            //add blocks in original branch
            while (iterBlock != null) {
                View blockView = getViewForBlock(iterBlock);
                originalBranch.addView(blockView);
                if (iterBlock instanceof SugiliteStartingBlock)
                    iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof SugiliteOperationBlock)
                    iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof  SugiliteSpecialOperationBlock)
                    iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof SugiliteErrorHandlingForkBlock)
                    break;
                else
                    new Exception("unsupported block type").printStackTrace();
            }
            originalBranch.setPadding(60, 0, 0, 0);
            mainLayout.addView(originalBranch);
            TextView tv2 = new TextView(context);
            tv2.setText(Html.fromHtml("<b>" + "IF FAILED" + "</b>"));
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv2.setPadding(10, 10, 10, 10);
            registerForContextMenu(tv2);
            mainLayout.addView(tv2);
            LinearLayout alternativeBranch = new LinearLayout(context);
            alternativeBranch.setOrientation(LinearLayout.VERTICAL);
            alternativeBranch.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            //add blocks in the alternative branch
            iterBlock = ((SugiliteErrorHandlingForkBlock) block).getAlternativeNextBlock();
            while (iterBlock != null) {
                View blockView = getViewForBlock(iterBlock);
                alternativeBranch.addView(blockView);
                if (iterBlock instanceof SugiliteStartingBlock)
                    iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof SugiliteOperationBlock)
                    iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof  SugiliteSpecialOperationBlock)
                    iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlock();
                else if (iterBlock instanceof SugiliteErrorHandlingForkBlock)
                    break;
                else
                    new Exception("unsupported block type").printStackTrace();
            }
            alternativeBranch.setPadding(60, 0, 0, 0);
            mainLayout.addView(alternativeBranch);
            return mainLayout;
        } else
            new Exception("UNSUPPORTED BLOCK TYPE").printStackTrace();

        return null;


    }

    public void scriptDebugRunButtonOnClick (final View view){
        //kill the relevant apps before executing the script
        final Context activityContext = this;
        new AlertDialog.Builder(this)
                .setTitle("Run Script")
                .setMessage("Are you sure you want to run this script?")
                .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //clear the queue first before adding new instructions

                        if(!serviceStatusManager.isRunning()){
                            //prompt the user if the accessiblity service is not active
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(view.getContext());
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
                        else {
                            VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(activityContext, getLayoutInflater(), sugiliteData, script, sharedPreferences, SugiliteData.REGULAR_DEBUG_STATE);
                            if(script.variableNameDefaultValueMap.size() > 0) {
                                //has variable
                                sugiliteData.stringVariableMap.putAll(script.variableNameDefaultValueMap);
                                boolean needUserInput = false;
                                for(Map.Entry<String, Variable> entry : script.variableNameDefaultValueMap.entrySet()){
                                    if(entry.getValue().type == Variable.USER_INPUT){
                                        needUserInput = true;
                                        break;
                                    }
                                }
                                if(needUserInput)
                                    //show the dialog to obtain user input
                                    variableSetValueDialog.show();
                                else
                                    variableSetValueDialog.executeScript(null);
                            }
                            else{
                                //execute the script without showing the dialog
                                variableSetValueDialog.executeScript(null);
                            }
                        }
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

    /**
     * set a breakpoint for every operation block in the script
     * @param block
     */
    private void addABreakpointForEveryOperationBlock(SugiliteBlock block){
        if(block == null)
            return;
        if(block instanceof SugiliteStartingBlock){
            addABreakpointForEveryOperationBlock(((SugiliteStartingBlock) block).getNextBlock());
        }
        else if(block instanceof SugiliteOperationBlock){
            ((SugiliteOperationBlock) block).isSetAsABreakPoint = true;
            addABreakpointForEveryOperationBlock(((SugiliteOperationBlock) block).getNextBlock());
        }
        else if(block instanceof  SugiliteSpecialOperationBlock){
            addABreakpointForEveryOperationBlock(((SugiliteSpecialOperationBlock) block).getNextBlock());
        }
        else if(block instanceof SugiliteErrorHandlingForkBlock){
            addABreakpointForEveryOperationBlock(((SugiliteErrorHandlingForkBlock) block).getAlternativeNextBlock());
            addABreakpointForEveryOperationBlock(((SugiliteErrorHandlingForkBlock) block).getOriginalNextBlock());
        }
    }

    public void scriptDebugSingleStepButtonOnClick (final View view){
        //add a breakpoint at every single operation block
        addABreakpointForEveryOperationBlock(script);

        //kill the relevant apps before executing the script
        final Context activityContext = this;
        new AlertDialog.Builder(this)
                .setTitle("Run Script")
                .setMessage("Are you sure you want to run this script?")
                .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //clear the queue first before adding new instructions

                        if(!serviceStatusManager.isRunning()){
                            //prompt the user if the accessiblity service is not active
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(view.getContext());
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
                        else {
                            VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(activityContext, getLayoutInflater(), sugiliteData, script, sharedPreferences, SugiliteData.REGULAR_DEBUG_STATE);
                            if(script.variableNameDefaultValueMap.size() > 0) {
                                //has variable
                                sugiliteData.stringVariableMap.putAll(script.variableNameDefaultValueMap);
                                boolean needUserInput = false;
                                for(Map.Entry<String, Variable> entry : script.variableNameDefaultValueMap.entrySet()){
                                    if(entry.getValue().type == Variable.USER_INPUT){
                                        needUserInput = true;
                                        break;
                                    }
                                }
                                if(needUserInput)
                                    //show the dialog to obtain user input
                                    variableSetValueDialog.show();
                                else
                                    variableSetValueDialog.executeScript(null);
                            }
                            else{
                                //execute the script without showing the dialog
                                variableSetValueDialog.executeScript(null);
                            }
                        }
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

    public void scriptDebugCancelButtonOnClick (View view){
        onBackPressed();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(this, SugiliteMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void scriptDebugDeleteButtonOnClick (View view){
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deleting")
                .setMessage("Are you sure you want to delete this script?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SugiliteStartingBlock testBlock = new SugiliteStartingBlock();
                        // continue with delete
                        try {
                            sugiliteScriptDao.delete(scriptName);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            //error in saving the script
                        }
                        finish();
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

    private List<SugiliteBlock> traverseBlock(SugiliteStartingBlock startingBlock) throws Exception{
        List<SugiliteBlock> sugiliteBlocks = new ArrayList<>();
        SugiliteBlock currentBlock = startingBlock;
        while(currentBlock != null){
            sugiliteBlocks.add(currentBlock);
            if(currentBlock instanceof SugiliteStartingBlock){
                currentBlock = ((SugiliteStartingBlock)currentBlock).getNextBlock();
            }
            else if (currentBlock instanceof SugiliteOperationBlock){
                currentBlock = ((SugiliteOperationBlock)currentBlock).getNextBlock();
            }
            else if (currentBlock instanceof SugiliteErrorHandlingForkBlock){
                currentBlock = ((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock();
            }
            else if (currentBlock instanceof SugiliteSpecialOperationBlock)
                currentBlock = ((SugiliteSpecialOperationBlock) currentBlock).getNextBlock();
            else{
                throw new RuntimeException("Unsupported Block Type!");
            }
        }
        return sugiliteBlocks;
    }

    
    private static final int ITEM_1 = Menu.FIRST;
    private static final int ITEM_2 = Menu.FIRST + 1;
    private static final int ITEM_3 = Menu.FIRST + 2;
    private static final int ITEM_4 = Menu.FIRST + 3;

    private TextView contextTextView = null;


    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info){
        super.onCreateContextMenu(menu, view, info);
        menu.setHeaderTitle("Sugilite Operation Menu");
        menu.add(0, ITEM_1, 0, "View Screenshot");
        menu.add(0, ITEM_2, 0, "Edit");
        menu.add(0, ITEM_3, 0, "Fork");
        menu.add(0, ITEM_4, 0, "Delete");
        if(view instanceof TextView) {
            view.setBackgroundResource(android.R.color.transparent);
            contextTextView = (TextView) view;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case ITEM_1:
                viewOperation(item);
                break;
            case ITEM_2:
                editOperation(item);
                break;
            case ITEM_3:
                forkOperation(item);
            case ITEM_4:
                deleteOperation(item);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void viewOperation(MenuItem item){
        TextView textView = contextTextView;
        if(textView == null)
            return;
        SugiliteBlock currentBlock = script;
        while(true) {
            if (currentBlock == null)
                break;
            if (currentBlock instanceof SugiliteOperationBlock) {
                //TODO: check if content equals is the right method to use here
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    if (((SugiliteOperationBlock) currentBlock).getFeaturePack() == null) {
                        //scripts passed from external sources (via json) has no feature pack & previous block fields
                        Toast.makeText(this, "Can't view operations from external source!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    //match, pop up the screenshot view
                    File screenshot = currentBlock.getScreenshot();
                    if (screenshot == null) {
                        Toast.makeText(this, "No screenshot available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(screenshot), "image/*");
                    startActivity(intent);
                    break;
                } else {
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlock();
                }
            } else if (currentBlock instanceof SugiliteStartingBlock) {
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    //match, can't edit starting block
                    Toast.makeText(this, "Can't view starting block", Toast.LENGTH_SHORT).show();
                    break;
                } else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlock();
                }
            } else if (currentBlock instanceof SugiliteSpecialOperationBlock) {
                //TODO: do something
            } else if (currentBlock instanceof SugiliteErrorHandlingForkBlock) {
                //TODO: do something
            } else
                new Exception("UNSUPPORTED BLOCK TYPE").printStackTrace();
        }
    }

    private void editOperation(MenuItem item){
        TextView textView = contextTextView;
        if(textView == null) {
            System.out.println("Can't find view " + item.getItemId());
            return;
        }
        attemptToEdit(script, textView);
    }
    private boolean attemptToEdit(SugiliteBlock currentBlock, TextView textView){
        while(true){
            if(currentBlock == null)
                break;
            else if(currentBlock instanceof SugiliteOperationBlock){
                //TODO: check if content equals is the right method to use here
                if(Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())){
                    if(((SugiliteOperationBlock) currentBlock).getFeaturePack() == null){
                        //scripts passed from external sources (via json) has no feature pack & previous block fields
                        Toast.makeText(this, "Can't edit scripts from external source!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    DialogInterface.OnClickListener callback = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.out.println("callback called");
                            try {
                                script = sugiliteScriptDao.read(scriptName);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            loadOperationList();
                        }
                    };
                    RecordingPopUpDialog recordingPopUpDialog = new RecordingPopUpDialog(sugiliteData, getApplicationContext(), script, sharedPreferences, (SugiliteOperationBlock)currentBlock, LayoutInflater.from(getApplicationContext()), RecordingPopUpDialog.TRIGGERED_BY_EDIT, callback);
                    sugiliteData.initiatedExternally = false;
                    recordingPopUpDialog.show(true);
                    break;
                    //match, pop up the edit
                    //the pop up should save the new script to db
                }
                else{
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlock();
                }
            }
            else if(currentBlock instanceof SugiliteStartingBlock){
                if(Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())){
                    //match, can't edit starting block
                    Toast.makeText(this, "Can't edit starting block", Toast.LENGTH_SHORT).show();
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlock();
                }
            }
            else if(currentBlock instanceof SugiliteErrorHandlingForkBlock){
                attemptToEdit(((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock(), textView);
                attemptToEdit(((SugiliteErrorHandlingForkBlock) currentBlock).getAlternativeNextBlock(), textView);
                break;
            }
            else if(currentBlock instanceof SugiliteSpecialOperationBlock){
                //TODO: do something
            }
            else {
                throw new RuntimeException("Unsupported Block Type!");
            }
        }
        //hack
        //TODO: use block id to match operation instead
        return false;
    }

    private void forkOperation(MenuItem item) {
        //TODO
        /*
        1. create a new fork popup that generates fork blocks
        2. change the automator so it can handle fork blocks
        3.
         */

    }

        @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == RecordingPopUpDialog.TRIGGERED_BY_EDIT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Successfully Editing the Operation", Toast.LENGTH_SHORT).show();
                try {
                    script = sugiliteScriptDao.read(scriptName);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                loadOperationList();
            }
            else {
                Toast.makeText(this, "Failed to Editing the Operation", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteOperation(MenuItem item){
        TextView textView = contextTextView;
        if(textView == null)
            return;
        attemptToDelete(script, textView);
        try {
            script = sugiliteScriptDao.read(scriptName);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        loadOperationList();
    }

    private void attemptToDelete(SugiliteBlock currentBlock, TextView textView){
        while(true){
            if(currentBlock == null)
                break;
            else if(currentBlock instanceof SugiliteOperationBlock){
                if(Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())){
                    //scripts passed from external sources (via json) has no feature pack & previous block fields
                    if(((SugiliteOperationBlock) currentBlock).getFeaturePack() == null){
                        Toast.makeText(this, "Can't edit scripts from external source!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    ((SugiliteOperationBlock) currentBlock).delete();
                    try {
                        sugiliteScriptDao.save(script);
                        sugiliteScriptDao.commitSave();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                }
                else{
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlock();
                }
            }
            else if(currentBlock instanceof SugiliteStartingBlock){
                if(Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())){
                    //match, can't delete starting block
                    Toast.makeText(this, "Can't delete starting block", Toast.LENGTH_SHORT).show();
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlock();
                }
            }
            else if(currentBlock instanceof SugiliteErrorHandlingForkBlock){
                attemptToDelete(((SugiliteErrorHandlingForkBlock) currentBlock).getAlternativeNextBlock(), textView);
                attemptToDelete(((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock(), textView);
                break;
            }
            else {
                throw new RuntimeException("Unsupported Block Type!");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST, 1, "Resume Recording");
        menu.add(Menu.NONE, Menu.FIRST + 1, 2, "Rename");
        menu.add(Menu.NONE, Menu.FIRST + 2, 3, "Delete");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case Menu.FIRST:
                resumeRecording();
                break;
            case Menu.FIRST + 1:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final EditText newName = new EditText(this);
                builder.setView(newName)
                        .setTitle("Enter the new name")
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
                                    sugiliteScriptDao.delete(scriptName);
                                    sugiliteScriptDao.commitSave();
                                    Intent intent = new Intent(context, ScriptDebuggingActivity.class);
                                    intent.putExtra("scriptName", startingBlock.getScriptName());
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
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
                    break;
            case Menu.FIRST + 2:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setTitle("Confirm Deleting")
                        .setMessage("Are you sure you want to delete this script?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    sugiliteScriptDao.delete(scriptName);
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
                                onBackPressed();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                break;
        }
        return true;
    }

    //TODO: rewrite resume recording
    private void resumeRecording(){
        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessiblity service is not active
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setTitle("Service not running")
                    .setMessage("The " + Const.appNameUpperCase + " accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            serviceStatusManager.promptEnabling();
                            //do nothing
                        }
                    }).show();
        }
        else {
            SharedPreferences.Editor prefEditor = sharedPreferences.edit();
            //turn off the recording before executing
            prefEditor.putBoolean("recording_in_process", false);
            prefEditor.putString("scriptName", script.getScriptName().replace(".SugiliteScript", ""));
            prefEditor.commit();
            sugiliteData.initiatedExternally = false;
            sugiliteData.setScriptHead(script);
            sugiliteData.setCurrentScriptBlock(script.getTail());
            //force stop all the relevant packages
            for (String packageName : script.relevantPackages) {
                Automator.killPackage(packageName);
            }
            sugiliteData.runScript(script, true, SugiliteData.EXECUTION_STATE);
            //need to have this delay to ensure that the killing has finished before we start executing
            try {
                Thread.sleep(SCRIPT_DELAY);
            } catch (Exception e) {
                // do nothing
            }
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    float lastY = 0;

    final GestureDetector gestureDetector = new GestureDetector(context,new GestureDetector.SimpleOnGestureListener() {
        public void onLongPress(MotionEvent e) {

        }
    });
    View highlightedView = null;
    View.OnTouchListener textViewOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    v.setBackgroundResource(android.R.color.holo_blue_light);
                    //fix the multiple highlighting issue
                    if(highlightedView != null && highlightedView instanceof TextView)
                        highlightedView.setBackgroundResource(android.R.color.transparent);
                    highlightedView = v;
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    v.setBackgroundResource(android.R.color.transparent);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float abs = Math.abs(lastY - event.getY());
                    if(abs > 3)
                        v.setBackgroundResource(android.R.color.transparent);
                    break;
            }

            return false;
        }
    };

    class DebuggingOperationView extends LinearLayout{
        private TextView operationTextView;
        public boolean isBreakpointSet = false;
        private ImageView operationIconImageView;
        SugiliteOperationBlock block;

        public DebuggingOperationView(Context context){
            super(context);
            init();
        }

        public DebuggingOperationView(Context context, SugiliteOperationBlock block){
            super(context);
            init();
            setText(Html.fromHtml(block.getDescription()));
            this.block = block;
        }

        public DebuggingOperationView(Context context, AttributeSet attrs){
            super(context, attrs);
            init();
        }

        private void init(){
            this.setOrientation(HORIZONTAL);
            operationTextView = new TextView(context);
            operationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            operationIconImageView = new ImageView(context);
            operationIconImageView.setClickable(true);

            operationTextView.setPadding(10, 10, 10, 10);
            operationIconImageView.setPadding(10, 10, 10, 10);
            registerForContextMenu(operationTextView);

            operationIconImageView.setImageResource(R.drawable.gray_dot_icon);
            operationIconImageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isBreakpointSet){
                        isBreakpointSet = false;
                        //change icon back to false
                        operationIconImageView.setImageResource(R.drawable.gray_dot_icon);
                        block.isSetAsABreakPoint = false;
                    }
                    else{
                        isBreakpointSet = true;
                        //change icon back to true
                        operationIconImageView.setImageResource(R.drawable.red_dot_icon);
                        block.isSetAsABreakPoint = true;

                    }
                }
            });
            this.addView(operationIconImageView);
            this.addView(operationTextView);
        }

        public void setText(CharSequence text){
            operationTextView.setText(text);
        }

        public CharSequence getText(){
            return operationTextView.getText();
        }

    }


}
