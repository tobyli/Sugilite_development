package edu.cmu.hcii.sugilite.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.AutomatorUtil;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpression;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.recording.RecordingPopUpDialog;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteAndroidAPIVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteGoogleCloudVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;

public class LocalScriptDetailActivity extends ScriptDetailActivity implements SugiliteVoiceInterface {
    private AlertDialog editDialog;
    private EditText editText;
    private SugiliteBlock newBlock;
    private String condition = "";
    private SugiliteBlock current;
    private int newBlockIndex;
    private Activity activity;


    private PumiceDialogManager pumiceDialogManager;
    private TextToSpeech tts;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private boolean isSpeaking = false;
    private boolean isListening = false;
    private ImageButton speakButton;
    private boolean clickedStep;
    private SugiliteVerbalInstructionHTTPQueryManager sugiliteVerbalInstructionHTTPQueryManager;
    private SerializableUISnapshot serializableUISnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = this;
        setContentView(R.layout.activity_local_script_detail);


        //set up the TTS and dialog manager
        tts = sugiliteData.getTTS();


        if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.ANDROID) {
            this.sugiliteVoiceRecognitionListener = new SugiliteAndroidAPIVoiceRecognitionListener(this, this, tts);
        } else if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.GOOGLE_CLOUD) {
            this.sugiliteVoiceRecognitionListener = new SugiliteGoogleCloudVoiceRecognitionListener(this, this, tts);
        }

        //load the local script
        if (savedInstanceState == null) {
            this.scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            this.scriptName = savedInstanceState.getString("scriptName");
        }

        if(scriptName != null) {
            setTitle("View Script: " + scriptName.replace(".SugiliteScript", ""));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    pumiceDialogManager = new PumiceDialogManager(activity, false);
                    pumiceDialogManager.setSpeakButtonForCallback(speakButton);
                    pumiceDialogManager.setSugiliteVoiceRecognitionListener(sugiliteVoiceRecognitionListener);
                    script = sugiliteScriptDao.read(scriptName);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                loadOperationList(script);
            }
        }).start();
    }


    public void scriptDetailRunButtonOnClick (final View view){
        final Context activityContext = this;
        new AlertDialog.Builder(this)
                .setTitle("Run Script")
                .setMessage("Are you sure you want to run this script?")
                .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //clear the queue first before adding new instructions
                        PumiceDemonstrationUtil.executeScript(context, serviceStatusManager, script, sugiliteData, sharedPreferences, false, null, null, null);
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
        onBackPressed();
    }

    public void scriptDetailReconstructButtonOnClick (){
        final Context activityContext = this;
        new AlertDialog.Builder(this)
                .setTitle("Run Script")
                .setMessage("Are you sure you want to reconstruct this script?")
                .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //clear the queue first before adding new instructions
                        PumiceDemonstrationUtil.executeScript(context, serviceStatusManager, script, sugiliteData, sharedPreferences, true, null, null, null);
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


    public void scriptDetailDeleteButtonOnClick (View view){
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deleting")
                .setMessage("Are you sure you want to delete this script?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        try {
                            //delete the script
                            sugiliteScriptDao.delete(scriptName);
                            sugiliteData.logUsageData(ScriptUsageLogManager.REMOVE_SCRIPT, scriptName);

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


    //set up the context menu for operations

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
        //TODO: temporarily remove edit & fork options
        /*
        menu.add(0, ITEM_2, 0, "Edit");
        menu.add(0, ITEM_3, 0, "Fork");
        */
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
            case ITEM_3: {
                forkOperation(item);
                break;
            }
            case ITEM_4: {
                deleteOperation(item);
                break;
            }
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
                //view the screenshot taken during the demonstration
                //TODO: check if content equals is the right method to use here
                if (currentBlock.getDescription().toString().contentEquals(textView.getText().toString())) {
                    if (((SugiliteOperationBlock) currentBlock).getFeaturePack() == null) {
                        //scripts passed from external sources (via json) has no feature pack & previous block fields
                        PumiceDemonstrationUtil.showSugiliteToast("Can't view operations from external source!", Toast.LENGTH_SHORT);
                        break;
                    }
                    //match, pop up the screenshot view
                    File screenshot = currentBlock.getScreenshot();
                    if (screenshot == null) {
                        PumiceDemonstrationUtil.showSugiliteToast("No screenshot available", Toast.LENGTH_SHORT);
                        return;
                    }
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(screenshot), "image/*");
                    startActivity(intent);
                    break;
                } else {
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteStartingBlock) {
                if (currentBlock.getDescription().toString().contentEquals(textView.getText().toString())) {
                    //match, can't edit starting block
                    PumiceDemonstrationUtil.showSugiliteToast("Can't view starting block", Toast.LENGTH_SHORT);
                    break;
                } else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteSpecialOperationBlock) {
                //TODO: do something
            } else if (currentBlock instanceof SugiliteErrorHandlingForkBlock) {
                //TODO: do something
            } else if(currentBlock instanceof SugiliteConditionBlock) {
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
        editScript(false);
        //Toast.makeText(this, "Edit doesn't work", Toast.LENGTH_SHORT).show();

        //TODO: need to fix script editing for the new query format
        //attemptToEdit(script, textView);
    }

    private boolean attemptToEdit(SugiliteBlock currentBlock, TextView textView){
        while(true){
            if(currentBlock == null)
                break;
            else if(currentBlock instanceof SugiliteOperationBlock){
                //TODO: check if content equals is the right method to use here
                if(currentBlock.getDescription().toString().contentEquals(textView.getText().toString())){
                    if(((SugiliteOperationBlock) currentBlock).getFeaturePack() == null){
                        //scripts passed from external sources (via json) has no feature pack & previous block fields
                        PumiceDemonstrationUtil.showSugiliteToast("Can't edit scripts from external source!", Toast.LENGTH_SHORT);
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
                            loadOperationList(script);
                        }
                    };
                    RecordingPopUpDialog recordingPopUpDialog = new RecordingPopUpDialog(sugiliteData, this, script, sharedPreferences, (SugiliteOperationBlock)currentBlock, RecordingPopUpDialog.TRIGGERED_BY_EDIT, callback);
                    sugiliteData.initiatedExternally = false;
                    sugiliteData.logUsageData(ScriptUsageLogManager.EDIT_SCRIPT, scriptName);
                    recordingPopUpDialog.show(true);
                    break;
                    //match, pop up the edit
                    //the pop up should save the new script to db
                }
                else{
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlockToRun();
                }
            }
            else if(currentBlock instanceof SugiliteStartingBlock){
                if(currentBlock.getDescription().toString().contentEquals(textView.getText().toString())){
                    //match, can't edit starting block
                    PumiceDemonstrationUtil.showSugiliteToast("Can't edit starting block", Toast.LENGTH_SHORT);
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
                }
            }
            else if(currentBlock instanceof SugiliteErrorHandlingForkBlock){
                attemptToEdit(((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock(), textView);
                attemptToEdit(((SugiliteErrorHandlingForkBlock) currentBlock).getAlternativeNextBlock(), textView);
                break;
            }
            else if(currentBlock instanceof SugiliteSpecialOperationBlock){
                //TODO: do something
            } else if(currentBlock instanceof SugiliteConditionBlock) {
                //TODO: do something
            } else {
                throw new RuntimeException("Unsupported Block Type!");
            }
        }
        //hack
        //TODO: use block id to match operation instead
        return false;
    }

    private void forkOperation(MenuItem item) {
        //TODO
        TextView textView = contextTextView;
        if(textView == null)
            return;
        attemptToFork(script,textView);
        try {
            System.out.println("try : " + script.getTail());
            script = sugiliteScriptDao.read(scriptName);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("after : " + script.getTail());
        loadOperationList(script);
        /*
        1. create a new fork popup that generates fork blocks
        2. change the automator so it can handle fork blocks
        3.
         */
    }

    private void attemptToFork(SugiliteBlock currentBlock, TextView textView){
        while(true){
            if(currentBlock == null)
                break;
            else if(currentBlock instanceof SugiliteOperationBlock){
                if(currentBlock.getDescription().toString().contentEquals(textView.getText().toString())){
                    //scripts passed from external sources (via json) has no feature pack & previous block fields
                    if(((SugiliteOperationBlock) currentBlock).getFeaturePack() == null) {
                        PumiceDemonstrationUtil.showSugiliteToast("Can't edit scripts from external source!", Toast.LENGTH_SHORT);
                        break;
                    }
                    try {
                        current = currentBlock;

                        final EditText input = new EditText(context);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        input.setLayoutParams(lp);
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage(getString(R.string.ask_new_fork_message)).create();
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                condition = input.getText().toString();

                                SugiliteBooleanExpression sbe = new SugiliteBooleanExpression(" "+condition+" ");
                                SugiliteConditionBlock scb = new SugiliteConditionBlock(current,null,sbe,current.getPreviousBlock());
                                current.getPreviousBlock().setNextBlock(scb);
                                current.setPreviousBlock(null);

                                AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                                builder2.setMessage(getString(R.string.ask_else_fork_message)).create();
                                builder2.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            dialog.dismiss();
                                            resumeRecording(scb);
                                            //scb.setElseBlock(script.getTail());
                                        }
                                        catch (Exception e){
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                builder2.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                AlertDialog alert2 = builder2.create();
                                alert2.getWindow().setType(OVERLAY_TYPE);
                                alert2.setCanceledOnTouchOutside(true);
                                alert2.show();

                                try {
                                    sugiliteScriptDao.save(script);
                                    System.out.println("before commit : " + script.getTail());
                                    sugiliteScriptDao.commitSave();
                                    System.out.println("after commit : " + script.getTail());
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.setView(input);
                        alert.getWindow().setType(OVERLAY_TYPE);
                        alert.setCanceledOnTouchOutside(true);
                        alert.show();
                        System.out.println("weird part : " + script.getTail());
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                }
                else{
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlockToRun();
                }
            }
            else if(currentBlock instanceof SugiliteStartingBlock){
                if(currentBlock.getDescription().toString().contentEquals(textView.getText().toString())){
                    //match, can't delete starting block
                    PumiceDemonstrationUtil.showSugiliteToast("Can't fork starting block", Toast.LENGTH_SHORT);
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
                }
            }
            else if(currentBlock instanceof SugiliteConditionBlock) {
                if(currentBlock.getDescription().toString().contentEquals(textView.getText().toString())){
                    //match, can't delete starting block
                    PumiceDemonstrationUtil.showSugiliteToast("Can't fork forking block", Toast.LENGTH_SHORT);
                    break;
                }
                else {
                    currentBlock = ((SugiliteConditionBlock) currentBlock).getNextBlockToRun(sugiliteData);
                }
            } else {
                throw new RuntimeException("Unsupported Block Type!");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == RecordingPopUpDialog.TRIGGERED_BY_EDIT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                PumiceDemonstrationUtil.showSugiliteToast("Successfully Editing the Operation", Toast.LENGTH_SHORT);
                try {
                    script = sugiliteScriptDao.read(scriptName);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                loadOperationList(script);
            }
            else {
                PumiceDemonstrationUtil.showSugiliteToast("Failed to Editing the Operation", Toast.LENGTH_SHORT);
            }
        }
    }

    /**
     * delete the selected operation (and everything after it)
     * @param item
     */
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
        loadOperationList(script);
    }

    private void attemptToDelete(SugiliteBlock currentBlock, TextView textView){
        while(true){
            if(currentBlock == null)
                break;
            else if(currentBlock instanceof SugiliteOperationBlock){
                if(currentBlock.getDescription().toString().contentEquals(textView.getText().toString())){
                    //scripts passed from external sources (via json) has no feature pack & previous block fields
                    if(((SugiliteOperationBlock) currentBlock).getFeaturePack() == null){
                        PumiceDemonstrationUtil.showSugiliteToast("Can't edit scripts from external source!", Toast.LENGTH_SHORT);
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
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlockToRun();
                }
            }
            else if(currentBlock instanceof SugiliteStartingBlock){
                if(currentBlock.getDescription().toString().contentEquals(textView.getText().toString())){
                    //match, can't delete starting block
                    PumiceDemonstrationUtil.showSugiliteToast("Can't delete starting block", Toast.LENGTH_SHORT);
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
                }
            }
            else if(currentBlock instanceof SugiliteErrorHandlingForkBlock){
                attemptToDelete(((SugiliteErrorHandlingForkBlock) currentBlock).getAlternativeNextBlock(), textView);
                attemptToDelete(((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock(), textView);
                break;
            } else if(currentBlock instanceof SugiliteConditionBlock) {
                if (currentBlock.getDescription().toString().contentEquals(textView.getText().toString())) {
                    //scripts passed from external sources (via json) has no feature pack & previous block fields
                    /*if (((SugiliteConditionBlock) currentBlock).getFeaturePack() == null) {
                        Toast.makeText(this, "Can't edit scripts from external source!", Toast.LENGTH_SHORT).show();
                        break;
                    }*/
                    ((SugiliteConditionBlock) currentBlock).delete();
                    try {
                        sugiliteScriptDao.save(script);
                        sugiliteScriptDao.commitSave();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                else{
                    currentBlock = ((SugiliteConditionBlock) currentBlock).getNextBlockToRun();
                }
            } else {
                throw new RuntimeException("Unsupported Block Type!");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST, 1, "Resume Recording");
        menu.add(Menu.NONE, Menu.FIRST + 1, 2, "Rename Script");
        menu.add(Menu.NONE, Menu.FIRST + 2, 3, "Delete Script");
        menu.add(Menu.NONE, Menu.FIRST + 3, 4, "Add Check to Script");
        menu.add(Menu.NONE, Menu.FIRST + 4, 5, "Reconstruct the script");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case Menu.FIRST:
                //resume recording
                resumeRecording();
                break;
            case Menu.FIRST + 1:
                //rename the script
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
                                    Intent intent = new Intent(context, LocalScriptDetailActivity.class);
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
                //delete the script
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
            case Menu.FIRST + 3:
                //edit the script
                editScript(false);
                break;
            case Menu.FIRST + 4: {
                scriptDetailReconstructButtonOnClick();
                break;
            }
        }
        return true;
    }

    private void editScript(boolean c) {
        clickedStep = c;
        speakButton = (ImageButton) findViewById(R.id.button5);
        pumiceDialogManager.sendAgentMessage("You are adding a step to do different things in different cases. What should I check to figure out what case we're in? Please say something like check if it's cold or check if it's before 5pm.", true, true);


        //PumiceConditionalIntentHandler ih = new PumiceConditionalIntentHandler(context);
        //pumiceDialogManager = new PumiceDialogManager(this, new PumiceConditionalIntentHandler(pumiceDialogManager, this));

    }

    public void determineConditionalLoc(SugiliteBlock sb) {
        //String newText = "(call if (call equal (number 90 Fahrenheit) (number 90 Fahrenheit)) (call click (hasText coldCoffee)))";
        newBlock = sb;
        ((SugiliteConditionBlock) newBlock).setThenBlock(null);
        if(clickedStep) {
            newBlockIndex = Integer.parseInt(contextTextView.getText().toString().substring(0,1));
        }
        else {
            newBlockIndex = 1;
        }
        newBlock.inScope = true;
        //if(((SugiliteConditionBlock) newBlock).getIfBlock() != null) {
        if(clickedStep) {
                pumiceDialogManager.sendAgentMessage("Ok, do you want this check to happen before or after step " + newBlockIndex + "?",true,true);
        } else {
                SugiliteBlock holdBlock = script.getNextBlockToRun();
                script.setNextBlock(newBlock);
                newBlock.setPreviousBlock(script);
                newBlock.setNextBlock(holdBlock);
                holdBlock.setPreviousBlock(newBlock);
                loadOperationList(script);
                pumiceDialogManager.sendAgentMessage("Ok, does it look like the check happens at the right time?",true,true);
        }
        /*}
        else {
            //handle for both versions of editScript()
            SugiliteBlock holdBlock = script.getNextBlockToRun();
            script.setNextBlock(newBlock);
            newBlock.setPreviousBlock(script);
            ((SugiliteConditionBlock) newBlock).setIfBlock(holdBlock);
            holdBlock.setPreviousBlock(null);
            holdBlock.setParentBlock(newBlock);
            newBlock.setNextBlock(null);
            loadOperationList();
            //pumiceDialogManager.sendAgentMessage("Ok, does it look like the new step happens at the right time?",true,true);
        }*/
    }

    public void determineConditionalLoc2(boolean after) {
        int i = newBlockIndex;
        SugiliteBlock iterBlock = script;
        int count = 0;
        while(count < i) {
            if (iterBlock instanceof SugiliteStartingBlock)
                iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteOperationBlock)
                iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteConditionBlock)
                iterBlock = ((SugiliteConditionBlock) iterBlock).getNextBlockToRun();
            else
                new Exception("unsupported block type").printStackTrace();
            count++;
        }
        if(!after) {
            SugiliteBlock iterPrevBlock = iterBlock.getPreviousBlock();
            newBlock.setNextBlock(iterBlock);
            newBlock.setPreviousBlock(iterPrevBlock);
            iterBlock.setPreviousBlock(newBlock);
            iterPrevBlock.setNextBlock(newBlock);
        }
        else {
            SugiliteBlock iterNextBlock = iterBlock.getNextBlockToRun();
            newBlock.setNextBlock(iterNextBlock);
            newBlock.setPreviousBlock(iterBlock);
            iterBlock.setNextBlock(newBlock);
            iterNextBlock.setPreviousBlock(newBlock);
            newBlockIndex = i+1;
        }
        loadOperationList(script);
        pumiceDialogManager.sendAgentMessage("Ok, does it look like the check happens at the right time?",true,true);
    }

    public void testRun() {
        pumiceDialogManager.sendAgentMessage("Ok, let me know if anything goes wrong by saying pause.",true,true);
        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessiblity service is not active
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
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
            VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(this, sugiliteData, script, sharedPreferences, SugiliteData.REGULAR_DEBUG_STATE, pumiceDialogManager, false);
            //execute the script without showing the dialog
            variableSetValueDialog.executeScript(null, pumiceDialogManager, null);
        }
    }

    public void getScope(String s) {
        System.out.println("GETSCOPE");
        System.out.println(newBlock);
        int i = Integer.parseInt(s);//index of step that scope of if block should go through
        SugiliteBlock iterBlock = script;
        int count = 0;
        while (count < i) {
            if (iterBlock instanceof SugiliteStartingBlock)
                iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteOperationBlock)
                iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteConditionBlock)
                iterBlock = ((SugiliteConditionBlock) iterBlock).getNextBlockToRun();
            else
                new Exception("unsupported block type").printStackTrace();
            count++;
        }
        System.out.println(iterBlock);
        System.out.println(newBlock.getNextBlockToRun());
        SugiliteBlock newBlockNext = newBlock.getNextBlockToRun();
        SugiliteBlock storedNext = iterBlock.getNextBlockToRun();
        System.out.println(newBlockNext);
        System.out.println(storedNext);
        ((SugiliteConditionBlock) newBlock).setThenBlock(newBlockNext);
        newBlockNext.setParentBlock(newBlock);
        newBlockNext.setPreviousBlock(null);
        iterBlock.setNextBlock(null);
        if(storedNext != null) {
            storedNext.setPreviousBlock(newBlock);
        }
        newBlock.setNextBlock(storedNext);
        loadOperationList(script);
    }

    public void fixScope(String s,String s2) {
        int i = Integer.parseInt(s);
        SugiliteBlock iterBlock = script;
        boolean goIf = false;
        if(s2.equals("true")) {
            goIf = true;
        }
        int count = 0;
        while (count < i) {
            if (iterBlock instanceof SugiliteStartingBlock)
                iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteOperationBlock)
                iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteConditionBlock) {
                SugiliteConditionBlock condBlock = ((SugiliteConditionBlock) iterBlock);
                if(goIf) {
                    iterBlock = condBlock.getThenBlock();
                }
                else {
                    iterBlock = condBlock.getElseBlock();
                }
            }
            else
                new Exception("unsupported block type").printStackTrace();
            count++;
        }
        int count2 = newBlockIndex;
        SugiliteBlock iterBlock2 = newBlock;
        while (iterBlock2 != null) {
            if (iterBlock2 instanceof SugiliteStartingBlock)
                iterBlock2 = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock2 instanceof SugiliteOperationBlock)
                iterBlock2 = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock2 instanceof SugiliteSpecialOperationBlock)
                iterBlock2 = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
            else
                new Exception("unsupported block type").printStackTrace();
            count2++;
        }
        if(count2 < i) {
            iterBlock2.setNextBlock(iterBlock2.getNextBlockToRun());
        }
        SugiliteBlock oldNext = iterBlock.getNextBlockToRun();
        iterBlock.setNextBlock(null);
        iterBlock.setParentBlock(newBlock);
        newBlock.setNextBlock(oldNext);

        loadOperationList(script);
    }

    public void moveStep(String s) {
        int i = Integer.parseInt(s);//index of step that new step should go after; indices start at 1 and new step starts at index 1
        int j = newBlockIndex;
        SugiliteBlock storedCondBlock = null;
        boolean onIf = false;
        if(i == j-1) {
            return;
        }
        if(i != j) {
            SugiliteBlock iterBlock = script;
            int count = 0;
            while (count < i) {
                if (iterBlock instanceof SugiliteStartingBlock)
                    iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteOperationBlock)
                    iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                    iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteConditionBlock) {
                    storedCondBlock = iterBlock;
                    SugiliteConditionBlock condBlock = ((SugiliteConditionBlock) iterBlock);
                    iterBlock = condBlock.getThenBlock();
                    if(iterBlock != null) {
                        onIf = true;
                    }
                    else {
                        iterBlock = condBlock.getNextBlockToRun();
                    }
                }
                else if (iterBlock == null && onIf) {
                    iterBlock = ((SugiliteConditionBlock) storedCondBlock).getElseBlock();
                    if(iterBlock == null) {
                        iterBlock = storedCondBlock.getNextBlockToRun();
                    }
                }
                else
                    new Exception("unsupported block type").printStackTrace();
                count++;
            }
            SugiliteBlock iterNextBlock = iterBlock.getNextBlockToRun();
            SugiliteBlock newNextBlock = newBlock.getNextBlockToRun();
            SugiliteBlock newPrevBlock;
            if (j == 1) {
                newPrevBlock = script;
            } else {
                newPrevBlock = newBlock.getPreviousBlock();
            }

            int check = j + 1;
            if (i == check) {
                iterBlock.setPreviousBlock(newPrevBlock);
                newPrevBlock.setNextBlock(iterBlock);
            } else {
                if (newNextBlock != null) {
                    newNextBlock.setPreviousBlock(newPrevBlock);
                }
                newPrevBlock.setNextBlock(newNextBlock);
            }

            newBlock.setPreviousBlock(iterBlock);
            iterBlock.setNextBlock(newBlock);


            newBlock.setNextBlock(iterNextBlock);
            if (iterNextBlock != null) {
                iterNextBlock.setPreviousBlock(newBlock);
            }

            if (j < i) {
                newBlockIndex = i;
            } else {
                newBlockIndex = i + 1;
            }

            loadOperationList(script);
        }
    }

    public void pumiceSendButtonOnClick (View view) {
        // speak button
        if (isListening) {
            sugiliteVoiceRecognitionListener.stopListening();
        }

        else {
            sugiliteVoiceRecognitionListener.startListening();
        }

        /*
        if(userTextBox != null) {
            String userTextBoxContent = userTextBox.getText().toString();
            if(pumiceDialogManager != null){
                pumiceDialogManager.sendUserMessage(userTextBoxContent);
            }
        }
        */
    }

    //TODO: rewrite resume recording
    private void resumeRecording(){
        resumeRecording(script.getTail());
    }

    private void resumeRecording(SugiliteBlock blockToResumeRecordingFrom){
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
            sugiliteData.setCurrentScriptBlock(blockToResumeRecordingFrom);
            //force stop all the relevant packages
            for (String packageName : script.relevantPackages) {
                AutomatorUtil.killPackage(packageName);
            }
            sugiliteData.runScript(script, true, SugiliteData.EXECUTION_STATE, false);

            //turn on the cat overlay to prepare for demonstration - resume recording
            if(sugiliteData.verbalInstructionIconManager != null){
                sugiliteData.verbalInstructionIconManager.turnOnCatOverlay();
            }

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

    @Override
    public void resultAvailableCallback(List<String> matches, boolean isFinal) {
        if(isFinal && matches.size() > 0) {
            System.out.println(matches.get(0));
            if(pumiceDialogManager != null){
                pumiceDialogManager.sendUserMessage(matches.get(0));
            }
        }
    }

    @Override
    public void speakingStartedCallback() {
        System.out.println("speakingS");
        isSpeaking = true;
    }

    @Override
    public void speakingEndedCallback() {
        System.out.println("speakingE");
        isSpeaking = false;
    }

    @Override
    public void listeningStartedCallback() {
        System.out.println("listeningS");
        isListening = true;
    }

    @Override
    public void listeningEndedCallback() {
        System.out.println("listeningE");
        isListening = false;
    }

    public SugiliteData getSugiliteData() {
        return sugiliteData;
    }

    public SugiliteStartingBlock getScript() {
        return script;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sugiliteVoiceRecognitionListener != null) {
            sugiliteVoiceRecognitionListener.stopAllAndEndASRService();
        }
    }
}
