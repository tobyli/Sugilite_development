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
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.RecordingPopUpDialog;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteAndroidAPIVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteGoogleCloudVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;
import static edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator.getConditionBlockDescription;

public abstract class ScriptDetailActivity extends AppCompatActivity {

    protected LinearLayout operationStepList;
    protected SugiliteData sugiliteData;
    protected String scriptName;
    protected SharedPreferences sharedPreferences;
    protected SugiliteScriptDao sugiliteScriptDao;
    protected SugiliteStartingBlock script;
    protected ActivityManager activityManager;
    protected ServiceStatusManager serviceStatusManager;
    protected Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        this.serviceStatusManager = ServiceStatusManager.getInstance(this);
        this.sugiliteData = (SugiliteData)getApplication();
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(this);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(this, sugiliteData);
        }

        //add back the duck icon
        if(sugiliteData != null && sugiliteData.statusIconManager != null && serviceStatusManager != null){
            if(! sugiliteData.statusIconManager.isShowingIcon() && serviceStatusManager.isRunning()){
                sugiliteData.statusIconManager.addStatusIcon();
            }
        }

    }



    public void loadOperationList(SugiliteStartingBlock script){
        SugiliteData.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.loading_script_message);
                progressDialog.show();

                operationStepList = (LinearLayout)findViewById(R.id.operation_list_view);
                operationStepList.removeAllViews();
                SugiliteBlock iterBlock = script;


                while(iterBlock != null){
                    System.out.println("iterBlock: " + iterBlock);
                    operationStepList.addView(getViewForBlock(iterBlock));
                    if (iterBlock instanceof SugiliteStartingBlock) {
                        iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
                    }
                    else if (iterBlock instanceof SugiliteOperationBlock) {
                        iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
                    }
                    else if (iterBlock instanceof SugiliteSpecialOperationBlock) {
                        iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
                    }
                    else if (iterBlock instanceof SugiliteErrorHandlingForkBlock) {
                        break;
                    }
                    else if (iterBlock instanceof SugiliteConditionBlock) {
                        iterBlock = ((SugiliteConditionBlock) iterBlock).getNextBlockToRun();
                    }
                    else
                        new Exception("unsupported block type").printStackTrace();
                }


                //add the end script line
                TextView tv = new TextView(context);
                tv.setText(Html.fromHtml("<b>END SCRIPT</b>"));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setPadding(10, 10, 10, 10);
                operationStepList.addView(tv);

                progressDialog.dismiss();
            }
        });
    }



    /**
     * recursively construct the list of operations
     * @param block
     * @return
     */
    public View getViewForBlock(SugiliteBlock block) {
        if (block instanceof SugiliteStartingBlock) {
            TextView tv = new TextView(context);
            tv.setText( block.getDescription());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);
            tv.setOnTouchListener(textViewOnTouchListener);
            registerForContextMenu(tv);
            return tv;

        } else if (block instanceof SugiliteOperationBlock || block instanceof SugiliteSpecialOperationBlock) {
            TextView tv = new TextView(context);
            tv.setText(block.getDescription());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);
            tv.setOnTouchListener(textViewOnTouchListener);
            registerForContextMenu(tv);
            return tv;

        } else if (block instanceof SugiliteConditionBlock) {
            TextView tv = new TextView(context);
            tv.setText(getConditionBlockDescription((SugiliteConditionBlock) block, 0));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);
            if(block.inScope) {
                /*Drawable[] d = tv.getCompoundDrawables();
                for(int i = 0; i < d.length; i++) {
                    d[i].setColorFilter(0x800000ff,Mode.MULTIPLY);
                }*/
                //ColorDrawable cd = new ColorDrawable(0x800000ff);
                //cd.setBounds(3,0,3,0);
                //ColorDrawable cd2 = new ColorDrawable(0x800000ff);
                //tv.setCompoundDrawablesWithIntrinsicBounds(cd,cd,cd,cd);
                tv.setBackgroundColor(Color.YELLOW);
                //addConditionalBlock = false;
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
        }
        else
            new Exception("UNSUPPORTED BLOCK TYPE").printStackTrace();

        return null;
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(this, SugiliteMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("active_tab", "local_scripts");
        startActivity(intent);
    }

    //used for tracking selection gesture
    private float lastY = 0;
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
}
