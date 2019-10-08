package edu.cmu.hcii.sugilite.sharing;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.OperationBlockDescriptionRegenerator;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.sharing.SugiliteScriptSharingHTTPQueryManager;
import edu.cmu.hcii.sugilite.ui.LocalScriptDetailActivity;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;

import static edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator.setConditionBlockDescription;

public class SharingScriptReviewActivity extends ScriptDetailActivity {
    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_script_detail);
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(this);
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();

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
            setTitle("Sharing Script: " + scriptName.replace(".SugiliteScript", ""));
        }

        //load the local script
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    script = sugiliteScriptDao.read(scriptName);
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

    public void scriptDetailDownloadButtonOnClick (View view) {
        downloadScriptToLocal();
    }


    //TODO: fix this so that textual information is clickable
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
                    System.out.println("iterBlock: " + iterBlock);
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
        menu.add(Menu.NONE, Menu.FIRST + 1, 2, "Download Script");

        return true;
    }

    public class CustomClickableSpan extends ClickableSpan {
        @Override
        public void onClick(View widget) {
            Spanned s = (Spanned) ((TextView) widget).getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);
            PumiceDemonstrationUtil.showSugiliteToast("CLICKED! " + s.toString().substring(start, end) , Toast.LENGTH_SHORT);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.bgColor = getResources().getColor(android.R.color.holo_red_dark);
            ds.setColor(getResources().getColor(android.R.color.white));
            ds.setUnderlineText(true);
        }
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

            //SpannableString descriptionSpannableString = new SpannableString(Html.fromHtml(block.getDescription()));
            Spanned descriptionSpannableString = ontologyDescriptionGenerator.getSpannedDescriptionForOperation(((SugiliteOperationBlock)block).getOperation(), ((SugiliteOperationBlock) block).getOperation().getDataDescriptionQueryIfAvailable(), true);

            /*
            Pattern pattern = Pattern.compile("\\[hashed\\].*\\[\\/hashed\\]");
            Matcher matcher = pattern.matcher(descriptionSpannableString);

            while(matcher.find()) {
                descriptionSpannableString.setSpan(new CustomClickableSpan(), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            */

            tv.setText(descriptionSpannableString, TextView.BufferType.SPANNABLE);
            tv.setMovementMethod(LinkMovementMethod.getInstance());

            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(10, 10, 10, 10);

            //TODO: set interactive textview
            //tv.setOnTouchListener(textViewOnTouchListener);
            //registerForContextMenu(tv);
            return tv;

        } else if (block instanceof SugiliteConditionBlock) {
            setConditionBlockDescription((SugiliteConditionBlock) block, 0);
            TextView tv = new TextView(context);
            tv.setText(block.getDescription());
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
                //rename the script
                downloadScriptToLocal();
                break;
        }
        return true;
    }
}
