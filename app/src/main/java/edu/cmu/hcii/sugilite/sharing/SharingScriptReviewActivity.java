package edu.cmu.hcii.sugilite.sharing;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.ui.LocalScriptDetailActivity;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;

import static edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil.removeScriptExtension;
import static edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator.getConditionBlockDescription;


public class SharingScriptReviewActivity extends ScriptDetailActivity {
    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;
    private SugiliteSharingScriptPreparer sugiliteSharingScriptPreparer;
    private TempUserAccountNameManager tempUserAccountNameManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_sharing_script);
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(this);
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();
        this.sugiliteSharingScriptPreparer = new SugiliteSharingScriptPreparer(this);
        this.tempUserAccountNameManager = new TempUserAccountNameManager(this);

        //load the local script
        if (savedInstanceState == null) {
            this.scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            this.scriptName = savedInstanceState.getString("scriptName");
        }

        //get the script name
        if (savedInstanceState == null) {
            this.scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            this.scriptName = savedInstanceState.getString("scriptName");
        }

        //set the activity title bar
        if(scriptName != null) {
            setTitle("Review Sharing Script: " + scriptName.replace(".SugiliteScript", ""));
        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        //load the local script
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //load the script
                    script = sugiliteScriptDao.read(scriptName);
                    //prepare the script -- removing private information
                    script = sugiliteSharingScriptPreparer.prepareScript(script);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                loadOperationList(script);
            }
        }).start();
    }

    public void scriptDetailCancelButtonOnClick (View view) {
        onBackPressed();
    }

    public void scriptShareButtonOnClick (View view) {
        try {
            //TODO: process the PrivateNonPrivateLeafOntologyQueryPairWrapper -> reduce it to queryInUse
            String id = sugiliteScriptSharingHTTPQueryManager.uploadScript(scriptName, tempUserAccountNameManager.getBestUserName(), script);
            Log.i("Upload script", "Script shared with id : " + id);
            PumiceDemonstrationUtil.showSugiliteAlertDialog(String.format("Successfully uploaded the script \"%s\"!", removeScriptExtension(scriptName)));

            //switch to remote script list
            Intent intent = new Intent(this, SugiliteMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("active_tab", "remote_scripts");
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            PumiceDemonstrationUtil.showSugiliteAlertDialog(String.format("Failed to upload the script \"%s\"!", removeScriptExtension(scriptName)));
        }
    }

    public void reloadOperationList(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadOperationList(script);
            }
        }).start();
    }


    @Override
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
                    //System.out.println("iterBlock: " + iterBlock);
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
                }


                TextView tv = new TextView(context);
                tv.setText(Html.fromHtml("<b>END SCRIPT</b>"));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setPadding(10, 10, 10, 10);
                operationStepList.addView(tv);

                progressDialog.dismiss();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST, 1, "View Information");
        menu.add(Menu.NONE, Menu.FIRST + 1, 2, "Refresh Script");

        return true;
    }


    /**
     * recursively construct the list of operations
     * @param block
     * @return
     */
    @Override
    public View getViewForBlock(SugiliteBlock block) {
        if (block instanceof SugiliteStartingBlock) {
            TextView tv = new TextView(context);
            tv.setText( block.getDescription());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);

            //tv.setOnTouchListener(textViewOnTouchListener);
            //registerForContextMenu(tv);
            return tv;

        } else if (block instanceof SugiliteOperationBlock || block instanceof SugiliteSpecialOperationBlock) {
            TextView tv = new TextView(context);

            // make the spanned that represent privacy masked strings clickable
            Spanned descriptionSpannableString = ontologyDescriptionGenerator.getSpannedDescriptionForOperation(((SugiliteOperationBlock)block).getOperation(), ((SugiliteOperationBlock) block).getOperation().getDataDescriptionQueryIfAvailable(), true);

            tv.setText(descriptionSpannableString, TextView.BufferType.SPANNABLE);
            tv.setMovementMethod(LinkMovementMethod.getInstance());

            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);

            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reloadOperationList();
                }
            });

            /*
            tv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    reloadOperationList();
                    return false;
                }
            });
            */

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


            //tv.setOnTouchListener(textViewOnTouchListener);
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
            //registerForContextMenu(tv);
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
            //registerForContextMenu(tv2);
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

    private void viewScriptInfo() {
        PumiceDemonstrationUtil.showSugiliteToast("View script meta info", Toast.LENGTH_SHORT);
    }

    private void downloadScriptToLocal() {
        PumiceDemonstrationUtil.showSugiliteToast("Downloading the script to local", Toast.LENGTH_SHORT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // save the script locally
                try {
                    script.setScriptName("DOWNLOADED: " + script.getScriptName());
                    sugiliteScriptDao.save(script);
                    sugiliteScriptDao.commitSave();
                } catch (Exception e){
                    Log.e("RemoteScriptDetailActivity", "failed to save the script locally");
                    PumiceDemonstrationUtil.showSugiliteAlertDialog("Failed to save the script locally!");
                }

                // open a new LocalScriptDetailActivitpy to view the local script
                SugiliteData.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Intent scriptDetailIntent = new Intent(context, LocalScriptDetailActivity.class);
                        scriptDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        scriptDetailIntent.putExtra("scriptName", script.getScriptName());
                        PumiceDemonstrationUtil.showSugiliteAlertDialog("Successfully downloaded the script!");
                        startActivity(scriptDetailIntent);

                    }
                });
            }
        }).start();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(this, SugiliteMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("active_tab", "local_scripts");
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case Menu.FIRST:
                //resume recording
                viewScriptInfo();
                break;
            case Menu.FIRST + 1:
                reloadOperationList();
                break;
        }
        return true;
    }
}
