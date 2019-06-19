package edu.cmu.hcii.sugilite.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Color;

import java.io.File;
import java.util.List;
import java.util.Locale;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.AutomatorUtil;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpression;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.dialog.ConditionalPumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.RecordingPopUpDialog;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteAndroidAPIVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteGoogleCloudVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.PACKAGE_NAME;
import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceConditionalIntentHandler;

public class ScriptDetailActivity extends AppCompatActivity implements SugiliteVoiceInterface {

    private LinearLayout operationStepList;
    private SugiliteData sugiliteData;
    private String scriptName;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteStartingBlock script;
    private ActivityManager activityManager;
    private ServiceStatusManager serviceStatusManager;
    private Activity context;
    private AlertDialog progressDialog;
    private EditText editText;
    private String condition = "";
    private SugiliteBlock current;
    public int lastBlockIndex;
    private int stepCounter;
    private static int sc;

    private ConditionalPumiceDialogManager conditionalPumiceDialogManager;
    private TextToSpeech tts;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    public boolean isSpeaking = false;
    public boolean isListening = false;
    private ImageButton speakButton;
    private ImageButton cancelButton;
    private SugiliteVerbalInstructionHTTPQueryManager sugiliteVerbalInstructionHTTPQueryManager;
    private SerializableUISnapshot serializableUISnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_detail);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        serviceStatusManager = ServiceStatusManager.getInstance(this);
        if (savedInstanceState == null) {
            scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            scriptName = savedInstanceState.getString("scriptName");
        }
        sugiliteData = (SugiliteData) getApplication();
        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            sugiliteScriptDao = new SugiliteScriptSQLDao(this);
        else
            sugiliteScriptDao = new SugiliteScriptFileDao(this, sugiliteData);
        this.context = this;
        if (scriptName != null)
            setTitle("View Script: " + scriptName.replace(".SugiliteScript", ""));

        //progress dialog for loading the script
        progressDialog = new AlertDialog.Builder(context).setMessage(Const.LOADING_MESSAGE).create();
        progressDialog.getWindow().setType(OVERLAY_TYPE);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    script = sugiliteScriptDao.read(scriptName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Runnable dismissDialog = new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                };
                Runnable loadOperation = new Runnable() {
                    @Override
                    public void run() {
                        loadOperationList();
                    }
                };
                context.runOnUiThread(loadOperation);
                context.runOnUiThread(dismissDialog);

            }
        }).start();

        //add back the duck icon
        if (sugiliteData != null && sugiliteData.statusIconManager != null && serviceStatusManager != null) {
            if (!sugiliteData.statusIconManager.isShowingIcon() && serviceStatusManager.isRunning()) {
                sugiliteData.statusIconManager.addStatusIcon();
            }
        }


        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });
        tts.setLanguage(Locale.US);
        sugiliteVoiceRecognitionListener = new SugiliteVoiceRecognitionListener(this, this, tts);
        speakButton = findViewById(R.id.button5);
        cancelButton = findViewById(R.id.button5);
        conditionalPumiceDialogManager = new ConditionalPumiceDialogManager(this);
        conditionalPumiceDialogManager.setSugiliteVoiceRecognitionListener(sugiliteVoiceRecognitionListener);
        conditionalPumiceDialogManager.setSpeakButtonForCallback(speakButton);
    }

    //TODO: set up operation on resume

    public void loadOperationList() {
        operationStepList = (LinearLayout) findViewById(R.id.operation_list_view);
        operationStepList.removeAllViews();
        SugiliteBlock iterBlock = script;
        stepCounter = 0;
        while (iterBlock != null) {
            operationStepList.addView(getViewForBlock(iterBlock));
            if (iterBlock instanceof SugiliteStartingBlock)
                iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteOperationBlock)
                iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
            else if (iterBlock instanceof SugiliteErrorHandlingForkBlock)
                break;
            else if (iterBlock instanceof SugiliteConditionBlock)
                iterBlock = ((SugiliteConditionBlock) iterBlock).getNextBlockToRun();
            else
                new Exception("unsupported block type").printStackTrace();
            stepCounter++;
        }
        lastBlockIndex = stepCounter - 1;

        TextView tv = new TextView(context);
        tv.setText(Html.fromHtml("<b>END SCRIPT</b>"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setPadding(10, 10, 10, 10);
        operationStepList.addView(tv);
    }

    /*
     * set description for condition block
     * @param: SugiliteConditionBlock block for which to get description, int count to keep track of how many recursive calls have been made
     */
    public static String setConditionBlockDescription(SugiliteConditionBlock block, int count) {
        //SugiliteBooleanExpressionNew booleanExpression = block.getSugiliteBooleanExpressionNew();//SugiliteBooleanExpression booleanExpression = block.getSugiliteBooleanExpression();
        String boolExp = block.getPumiceUserReadableDecription().substring(2,block.getPumiceUserReadableDecription().indexOf(",") + 1);

        /*boolExp = boolExp.substring(1,boolExp.length()-1).trim();
        String[] split = boolExp.split("\\(");
        boolExp = booleanExpression.breakdown();
        if(!split[0].contains("&&") && !split[0].contains("||")) {
            boolExp = ReadableDescriptionGenerator.setColor(boolExp, "#954608");
        }*/

        SugiliteBlock ifBlock = block.getThenBlock();
        SugiliteBlock elseBlock = block.getElseBlock();
        if (ifBlock instanceof SugiliteConditionBlock) {
            setConditionBlockDescription(((SugiliteConditionBlock) ifBlock), count + 1);
        }
        if (elseBlock != null && elseBlock instanceof SugiliteConditionBlock) {
            setConditionBlockDescription(((SugiliteConditionBlock) elseBlock), count + 1);
        }

        if (elseBlock != null) {
            String t = "&nbsp;&nbsp;&nbsp;&nbsp;";
            String tabs = "&nbsp;&nbsp;&nbsp;&nbsp;";
            String tabs2 = "";
            for (int c = 0; c < count; c++) {
                tabs += t;
                tabs2 += t;
            }
            String ifBlockDes = "";
            SugiliteBlock iterBlock = ifBlock;
            while(iterBlock != null) {
                ifBlockDes = ifBlockDes + " <br/>" + sc + tabs + " " + iterBlock.getDescription();
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
                sc++;
            }
            String elseBlockDes = "";
            SugiliteBlock iterBlock2 = elseBlock;
            while(iterBlock2 != null) {
                if (! (iterBlock2 instanceof SugiliteStartingBlock)) {
                    if (iterBlock2.getDescription() == null || iterBlock2.getDescription().length() == 0) {
                        iterBlock2.setDescription(iterBlock2.toString());
                    }
                    elseBlockDes = elseBlockDes + " <br/>" + sc + tabs  + " " + iterBlock2.getDescription();
                }
                if (iterBlock2 instanceof SugiliteStartingBlock) {
                    iterBlock2 = ((SugiliteStartingBlock) iterBlock2).getNextBlockToRun();
                }
                else if (iterBlock2 instanceof SugiliteOperationBlock) {
                    iterBlock2 = ((SugiliteOperationBlock) iterBlock2).getNextBlockToRun();
                }
                else if (iterBlock2 instanceof SugiliteSpecialOperationBlock) {
                    iterBlock2 = ((SugiliteSpecialOperationBlock) iterBlock2).getNextBlockToRun();
                }
                else if (iterBlock2 instanceof SugiliteConditionBlock) {
                    iterBlock2 = ((SugiliteConditionBlock) iterBlock2).getNextBlockToRun();
                }
                else {
                    new Exception("unsupported block type").printStackTrace();
                }
                sc++;
            }
            System.out.println(block.getPumiceUserReadableDecription());
            block.setDescription(ReadableDescriptionGenerator.setColor("If ", Const.SCRIPT_CONDITIONAL_COLOR) + boolExp + ReadableDescriptionGenerator.setColor(" then ", Const.SCRIPT_CONDITIONAL_COLOR) + ifBlockDes + "<br/>" + tabs2 + ReadableDescriptionGenerator.setColor("Otherwise", Const.SCRIPT_CONDITIONAL_COLOR) + elseBlockDes);
            return ReadableDescriptionGenerator.setColor("If ", Const.SCRIPT_CONDITIONAL_COLOR) + boolExp + ReadableDescriptionGenerator.setColor(" then ", Const.SCRIPT_CONDITIONAL_COLOR) + ifBlockDes + "<br/>" + tabs2 + ReadableDescriptionGenerator.setColor("Otherwise", Const.SCRIPT_CONDITIONAL_COLOR) + elseBlockDes;
        } else {
            String t = "&nbsp;&nbsp;&nbsp;&nbsp;";
            String tabs = "&nbsp;&nbsp;&nbsp;&nbsp;";
            for (int c = 0; c < count - 1; c++) {
                tabs += t;
            }
            String ifBlockDes = "";
            SugiliteBlock iterBlock = ifBlock;
            while(iterBlock != null) {
                ifBlockDes = ifBlockDes + " <br/>" + sc + tabs + " " + iterBlock.getDescription();
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
                sc++;
            }

            block.setDescription(ReadableDescriptionGenerator.setColor("If ", Const.SCRIPT_CONDITIONAL_COLOR) + boolExp + ReadableDescriptionGenerator.setColor(" then ", Const.SCRIPT_CONDITIONAL_COLOR) + ifBlockDes);
            return ReadableDescriptionGenerator.setColor("If ", Const.SCRIPT_CONDITIONAL_COLOR) + boolExp + ReadableDescriptionGenerator.setColor(" then ", Const.SCRIPT_CONDITIONAL_COLOR) + ifBlockDes;
        }
    }

    /**
     * recursively construct the list of operations
     *
     * @param block
     * @return
     */
    public View getViewForBlock(SugiliteBlock block) {
        if (block instanceof SugiliteStartingBlock) {
            TextView tv = new TextView(context);
            tv.setText(Html.fromHtml(Integer.toString(stepCounter) + " " + block.getDescription()));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);
            tv.setOnTouchListener(textViewOnTouchListener);
            registerForContextMenu(tv);
            return tv;

        } else if (block instanceof SugiliteOperationBlock || block instanceof SugiliteSpecialOperationBlock) {
            TextView tv = new TextView(context);
            tv.setText(Html.fromHtml(Integer.toString(stepCounter) + " " + block.getDescription()));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);
            tv.setOnTouchListener(textViewOnTouchListener);
            registerForContextMenu(tv);
            return tv;

        } else if (block instanceof SugiliteConditionBlock) {
            sc = stepCounter + 1;
            setConditionBlockDescription((SugiliteConditionBlock) block, 0);
            TextView tv = new TextView(context);
            tv.setText(Html.fromHtml(Integer.toString(stepCounter) + " " + block.getDescription()));//getPumiceUserReadableDecription()
            stepCounter = sc - 1;
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);
            if (block.inScope) {
                tv.setBackgroundColor(Color.YELLOW);
            }
            tv.setOnTouchListener(textViewOnTouchListener);
            registerForContextMenu(tv);
            return tv;

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
                    iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteOperationBlock)
                    iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof  SugiliteSpecialOperationBlock)
                    iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteConditionBlock)
                    iterBlock = ((SugiliteConditionBlock) iterBlock).getNextBlockToRun();
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
                    iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteOperationBlock)
                    iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof  SugiliteSpecialOperationBlock)
                    iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteConditionBlock)
                    iterBlock = ((SugiliteConditionBlock) iterBlock).getNextBlockToRun();
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


    public void scriptDetailRunButtonOnClick(final View view) {
        final Context activityContext = this;
        new AlertDialog.Builder(this)
                .setTitle("Run Script")
                .setMessage("Are you sure you want to run this script?")
                .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //clear the queue first before adding new instructions
                        PumiceDemonstrationUtil.executeScript(context, serviceStatusManager, script, sugiliteData, getLayoutInflater(), sharedPreferences, null, null, null);
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

    public void scriptDetailCancelButtonOnClick(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, SugiliteMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void scriptDetailDeleteButtonOnClick(View view) {
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

                        } catch (Exception e) {
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


    private static final int ITEM_1 = Menu.FIRST;
    private static final int ITEM_2 = Menu.FIRST + 1;
    private static final int ITEM_3 = Menu.FIRST + 2;
    private static final int ITEM_4 = Menu.FIRST + 3;
    private static final int ITEM_5 = Menu.FIRST + 4;

    private TextView contextTextView = null;


    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        menu.setHeaderTitle("Sugilite Operation Menu");
        menu.add(0, ITEM_1, 0, "View Screenshot");
        menu.add(0, ITEM_2, 0, "Edit");
        menu.add(0, ITEM_3, 0, "Fork");
        menu.add(0, ITEM_4, 0, "Delete");
        menu.add(0, ITEM_5, 0, "Add Check");
        if (view instanceof TextView) {
            view.setBackgroundResource(android.R.color.transparent);
            contextTextView = (TextView) view;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
            case ITEM_5: {
                addCheckOperation(item);
                break;
            }
        }
        return super.onContextItemSelected(item);
    }

    private void viewOperation(MenuItem item) {
        TextView textView = contextTextView;
        if (textView == null)
            return;
        SugiliteBlock currentBlock = script;
        while (true) {
            if (currentBlock == null)
                break;
            if (currentBlock instanceof SugiliteOperationBlock) {
                //view the screenshot taken during the demonstration
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
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteStartingBlock) {
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    //match, can't edit starting block
                    Toast.makeText(this, "Can't view starting block", Toast.LENGTH_SHORT).show();
                    break;
                } else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteSpecialOperationBlock) {
                //TODO: do something
            } else if (currentBlock instanceof SugiliteErrorHandlingForkBlock) {
                //TODO: do something
            } else if (currentBlock instanceof SugiliteConditionBlock) {
                //TODO: do something
            } else
                new Exception("UNSUPPORTED BLOCK TYPE").printStackTrace();
        }
    }

    private void editOperation(MenuItem item) {
        TextView textView = contextTextView;
        if (textView == null) {
            System.out.println("Can't find view " + item.getItemId());
            return;
        }
        editScript();
        //Toast.makeText(this, "Edit doesn't work", Toast.LENGTH_SHORT).show();

        //TODO: need to fix script editing for the new query format
        //attemptToEdit(script, textView);
    }

    private void addCheckOperation(MenuItem item) {
        TextView textView = contextTextView;
        if (textView == null) {
            System.out.println("Can't find view " + item.getItemId());
            return;
        }
        addCheck();
    }

    private boolean attemptToEdit(SugiliteBlock currentBlock, TextView textView) {
        while (true) {
            if (currentBlock == null)
                break;
            else if (currentBlock instanceof SugiliteOperationBlock) {
                //TODO: check if content equals is the right method to use here
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    if (((SugiliteOperationBlock) currentBlock).getFeaturePack() == null) {
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
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            loadOperationList();
                        }
                    };
                    RecordingPopUpDialog recordingPopUpDialog = new RecordingPopUpDialog(sugiliteData, this, script, sharedPreferences, (SugiliteOperationBlock) currentBlock, LayoutInflater.from(getApplicationContext()), RecordingPopUpDialog.TRIGGERED_BY_EDIT, callback);
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
            } else if (currentBlock instanceof SugiliteStartingBlock) {
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    //match, can't edit starting block
                    Toast.makeText(this, "Can't edit starting block", Toast.LENGTH_SHORT).show();
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteErrorHandlingForkBlock) {
                attemptToEdit(((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock(), textView);
                attemptToEdit(((SugiliteErrorHandlingForkBlock) currentBlock).getAlternativeNextBlock(), textView);
                break;
            } else if (currentBlock instanceof SugiliteSpecialOperationBlock) {
                //TODO: do something
            } else if (currentBlock instanceof SugiliteConditionBlock) {
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
        if (textView == null)
            return;
        attemptToFork(script, textView);
        try {
            System.out.println("try : " + script.getTail());
            script = sugiliteScriptDao.read(scriptName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("after : " + script.getTail());
        loadOperationList();
        /*
        1. create a new fork popup that generates fork blocks
        2. change the automator so it can handle fork blocks
        3.
         */
    }

    private void attemptToFork(SugiliteBlock currentBlock, TextView textView) {
        while (true) {
            if (currentBlock == null)
                break;
            else if (currentBlock instanceof SugiliteOperationBlock) {
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    //scripts passed from external sources (via json) has no feature pack & previous block fields
                    if (((SugiliteOperationBlock) currentBlock).getFeaturePack() == null) {
                        Toast.makeText(this, "Can't edit scripts from external source!", Toast.LENGTH_SHORT).show();
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
                        builder.setMessage(Const.GET_CONDITION).create();
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                condition = input.getText().toString();

                                SugiliteBooleanExpression sbe = new SugiliteBooleanExpression(" " + condition + " ");
                                SugiliteConditionBlock scb = new SugiliteConditionBlock(current, null, sbe, current.getPreviousBlock());
                                current.getPreviousBlock().setNextBlock(scb);
                                current.setPreviousBlock(null);

                                AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                                builder2.setMessage(Const.CHECK_FOR_ELSE).create();
                                builder2.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            dialog.dismiss();
                                            resumeRecording(scb);
                                            //scb.setElseBlock(script.getTail());
                                        } catch (Exception e) {
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
                                } catch (Exception e) {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                else{
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteStartingBlock) {
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    //match, can't delete starting block
                    Toast.makeText(this, "Can't fork starting block", Toast.LENGTH_SHORT).show();
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteConditionBlock) {
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    //match, can't delete starting block
                    Toast.makeText(this, "Can't fork forking block", Toast.LENGTH_SHORT).show();
                    break;
                } else {
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
                Toast.makeText(this, "Successfully Editing the Operation", Toast.LENGTH_SHORT).show();
                try {
                    script = sugiliteScriptDao.read(scriptName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                loadOperationList();
            } else {
                Toast.makeText(this, "Failed to Editing the Operation", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * delete the selected operation (and everything after it)
     *
     * @param item
     */
    private void deleteOperation(MenuItem item) {
        TextView textView = contextTextView;
        if (textView == null)
            return;
        attemptToDelete(script, textView);
        try {
            script = sugiliteScriptDao.read(scriptName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadOperationList();
    }

    private void attemptToDelete(SugiliteBlock currentBlock, TextView textView) {
        while (true) {
            if (currentBlock == null)
                break;
            else if (currentBlock instanceof SugiliteOperationBlock) {
                System.out.println(Html.fromHtml(currentBlock.getDescription()).toString());
                System.out.println(textView.getText().toString());
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString().substring(2))) {
                    //scripts passed from external sources (via json) has no feature pack & previous block fields
                    if (((SugiliteOperationBlock) currentBlock).getFeaturePack() == null) {
                        Toast.makeText(this, "Can't edit scripts from external source!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    ((SugiliteOperationBlock) currentBlock).delete();
                    try {
                        sugiliteScriptDao.save(script);
                        sugiliteScriptDao.commitSave();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                else{
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteStartingBlock) {
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
                    //match, can't delete starting block
                    Toast.makeText(this, "Can't delete starting block", Toast.LENGTH_SHORT).show();
                    break;
                }
                else {
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlockToRun();
                }
            } else if (currentBlock instanceof SugiliteErrorHandlingForkBlock) {
                attemptToDelete(((SugiliteErrorHandlingForkBlock) currentBlock).getAlternativeNextBlock(), textView);
                attemptToDelete(((SugiliteErrorHandlingForkBlock) currentBlock).getOriginalNextBlock(), textView);
                break;
            } else if (currentBlock instanceof SugiliteConditionBlock) {
                if (Html.fromHtml(currentBlock.getDescription()).toString().contentEquals(textView.getText().toString())) {
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
        menu.add(Menu.NONE, Menu.FIRST + 4, 5, "Edit Script");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                startingBlock.setScriptName(newName.getText().toString() + ".SugiliteScript");
                                try {
                                    sugiliteScriptDao.save(startingBlock);
                                    sugiliteScriptDao.delete(scriptName);
                                    sugiliteScriptDao.commitSave();
                                    Intent intent = new Intent(context, ScriptDetailActivity.class);
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
                                } catch (Exception e) {
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
                //add check to script
                addCheck();
                break;
            case Menu.FIRST + 4:
                //edit the script
                break;
        }
        return true;
    }


    private void editScript() {
        //TO DO
    }

    public class cancelListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            cancelButton.callOnClick();
            sugiliteVoiceRecognitionListener.stopListening();
            sugiliteVoiceRecognitionListener.stopTTS();
            isListening = false;
            isSpeaking = false;
            ((PumiceConditionalIntentHandler) conditionalPumiceDialogManager.getPumiceDialogState().getPumiceUtteranceIntentHandlerInUse()).setLastIntent(null);

        }
    }

    public class voiceListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            //speakButton.callOnClick();
            //do nothing
            speakButton.callOnClick();
        }
    }


    public void addSnackbar(String s) {
        Snackbar mySnackbar = Snackbar.make(cancelButton, s, BaseTransientBottomBar.LENGTH_INDEFINITE);
        mySnackbar.show();
        View snackbarView = mySnackbar.getView();
        TextView textView = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(10);
        mySnackbar.setAction(R.string.cancel_btn,new cancelListener());
    }

    public void addCheck() {
        conditionalPumiceDialogManager.addCheck = true;
        addSnackbar("You are adding a check to do different steps in different cases. Please say something like 'check if it's cold' or 'check if the current time is before 5pm.'");
        conditionalPumiceDialogManager.sendAgentMessage("You are adding a check to do different steps in different cases. Please say something like 'check if it's cold' or 'check if the current time is before 5pm.'", true, true);
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
            sugiliteData.runScript(script, true, SugiliteData.EXECUTION_STATE);

            //turn on the cat overlay to prepare for demonstration
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

    @Override
    public void resultAvailableCallback(List<String> matches, boolean isFinal) {
        if(isFinal && matches.size() > 0) {
            System.out.println(matches.get(0));
            if(conditionalPumiceDialogManager.addCheck) {
                conditionalPumiceDialogManager.sendUserMessage(matches.get(0));
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

    public ConditionalPumiceDialogManager getConditionalPumiceDialogManager() {
        return conditionalPumiceDialogManager;
    }
}
