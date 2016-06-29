package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

public class ScriptDetailActivity extends AppCompatActivity {

    private LinearLayout operationStepList;
    private SugiliteData sugiliteData;
    private String scriptName;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteStartingBlock script;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_detail);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (savedInstanceState == null) {
            scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            scriptName = savedInstanceState.getString("scriptName");
        }
        sugiliteData = (SugiliteData)getApplication();
        sugiliteScriptDao = new SugiliteScriptDao(this);
        script = sugiliteScriptDao.read(scriptName);
        loadOperationList();


    }

    public void loadOperationList(){
        operationStepList = (LinearLayout)findViewById(R.id.operationStepList);
        operationStepList.removeAllViews();
        if (script != null){
            for(final SugiliteBlock block : traverseBlock(script)) {
                TextView operationStepItem = new TextView(this);
                operationStepItem.setText(Html.fromHtml(block.getDescription()));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 0, 0, 30);
                operationStepItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //show screenshot
                        if (block.getScreenshot() == null || (!block.getScreenshot().exists())) {
                            //no screenshot available
                            Toast.makeText(v.getContext(), "No screenshot available!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(block.getScreenshot()), "image/*");
                        startActivity(intent);
                    }
                });
                registerForContextMenu(operationStepItem);
                operationStepList.addView(operationStepItem, layoutParams);
            }
        }
        else{
            TextView operationStepItem = new TextView(this);
            operationStepItem.setText("NULL SCRIPT!");
            operationStepList.addView(operationStepItem);
        }
    }

    public void scriptDetailRunButtonOnClick (View view){
        new AlertDialog.Builder(this)
                .setTitle("Run Script")
                .setMessage("Are you sure you want to run this script?")
                .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //clear the queue first before adding new instructions
                        sugiliteData.clearInstructionQueue();
                        addItemsToInstructionQueue(traverseBlock(script));
                        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                        //turn off the recording before executing
                        prefEditor.putBoolean("recording_in_process", false);
                        prefEditor.commit();
                        //go to home screen for running the automation
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);
                        //TODO: kill the relevant apps before executing the script
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
    public void scriptDetailCancelButtonOnClick (View view){
        finish();
    }
    public void scriptDetailDeleteButtonOnClick (View view){
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

    private List<SugiliteBlock> traverseBlock(SugiliteStartingBlock startingBlock){
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
            else{
                currentBlock = null;
            }
        }
        return sugiliteBlocks;
    }

    private void addItemsToInstructionQueue(List<SugiliteBlock> blocks){
        for(SugiliteBlock block : blocks){
            sugiliteData.addInstruction(block);
        }
    }
    private static final int ITEM_1 = Menu.FIRST;
    private static final int ITEM_2 = Menu.FIRST + 1;
    private static final int ITEM_3 = Menu.FIRST + 2;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info){
        super.onCreateContextMenu(menu, view, info);
        menu.setHeaderTitle("Sugilite Operation Menu");
        menu.add(0, ITEM_1, 0, "View");
        menu.add(0, ITEM_2, 0, "Edit");
        menu.add(0, ITEM_3, 0, "Delete");
    }

    //TODO:implement context menu
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case ITEM_1:
                Toast.makeText(this, "View Operation", Toast.LENGTH_SHORT).show();
                item.getActionView().performClick();
                break;
            case ITEM_2:
                Toast.makeText(this, "Edit Operation", Toast.LENGTH_SHORT).show();
                editOperation(item);
                break;
            case ITEM_3:
                Toast.makeText(this, "Delete Operation", Toast.LENGTH_SHORT).show();
                deleteOperation(item);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void editOperation(MenuItem item){
        TextView textView;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(info.targetView instanceof TextView){
            int index = info.position;
            textView = (TextView)info.targetView;
        }
        else
            return;
        SugiliteBlock currentBlock = script;
        while(true){
            if(currentBlock == null)
                break;
            if(currentBlock instanceof SugiliteOperationBlock){
                //TODO: check if content equals is the right method to use here
                if(currentBlock.getDescription().contentEquals(textView.getText())){
                    //match, pop up the edit
                    //the pop up should save the new script to db
                    Intent intent = new Intent(this, RecordingPopUpActivity.class);
                    intent.putExtra("trigger", RecordingPopUpActivity.TRIGGERED_BY_EDIT);
                    intent.putExtra("originalScript", script);
                    intent.putExtra("blockToEdit", currentBlock);
                    startActivityForResult(intent, RecordingPopUpActivity.TRIGGERED_BY_EDIT);
                    break;
                }
                else{
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlock();
                }
            }
            if(currentBlock instanceof SugiliteStartingBlock){
                if(currentBlock.getDescription().equals(textView.getText())){
                    //match, can't edit starting block
                    Toast.makeText(this, "Can't edit starting block", Toast.LENGTH_SHORT).show();
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlock();
                }
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == RecordingPopUpActivity.TRIGGERED_BY_EDIT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Successfully Editing the Operation", Toast.LENGTH_SHORT).show();
                script = sugiliteScriptDao.read(scriptName);
                loadOperationList();
            }
            else {
                Toast.makeText(this, "Failed to Editing the Operation", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteOperation(MenuItem item){
        TextView textView;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(info.targetView instanceof TextView){
            int index = info.position;
            textView = (TextView)info.targetView;
        }
        else
            return;
        SugiliteBlock currentBlock = script;
        while(true){
            if(currentBlock == null)
                break;
            if(currentBlock instanceof SugiliteOperationBlock){
                if(currentBlock.getDescription().contentEquals(textView.getText())){
                    ((SugiliteOperationBlock) currentBlock).delete();
                    try {
                        sugiliteScriptDao.save(script);
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
            if(currentBlock instanceof SugiliteStartingBlock){
                if(currentBlock.getDescription().equals(textView.getText())){
                    //match, can't delete starting block
                    Toast.makeText(this, "Can't delete starting block", Toast.LENGTH_SHORT).show();
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlock();
                }
            }
        }
        script = sugiliteScriptDao.read(scriptName);
        loadOperationList();
    }



}
