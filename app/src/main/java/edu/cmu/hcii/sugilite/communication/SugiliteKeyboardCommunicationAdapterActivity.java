package edu.cmu.hcii.sugilite.communication;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;

/**
 * @author toby
 * @date 3/30/17
 * @time 9:57 AM
 */
public class SugiliteKeyboardCommunicationAdapterActivity extends Activity {
    TextView timestampView, contentView;
    private SugiliteData sugiliteData;
    private SugiliteAvailableFeaturePack featurePack;
    private SugiliteOperationBlock textboxBlock;
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteSetTextOperation textOperation;

    /**
     * this class is used for the Sugilite Keyboard to communicate with Sugilite. The keyboard should pass in an intent with two arguments: "timestamp" and "content"
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sugilite_communication_acticvity);
        timestampView = (TextView) findViewById(R.id.receive_message_textview);
        contentView = (TextView) findViewById(R.id.receive_message_script_name);
        contentView.setText("TEST MESSAGE TYPE");

        String content = "";
        long timestamp = 0;

        if (getIntent().getExtras() != null) {
            timestamp = getIntent().getExtras().getLong("timestamp");
            content = getIntent().getExtras().getString("content");
            timestampView.setText(String.valueOf(timestamp));
            contentView.setText(content);

            processKeyboardInput(timestamp, content);
            finish();
        }
    }

    private void processKeyboardInput(long timestamp, String content){

        textOperation.setText(content);
        textboxBlock.setOperation(textOperation);

        if (sugiliteData.lastTextboxFeature.viewId != null) {
            featurePack.viewId = sugiliteData.lastTextboxFeature.viewId;
        }
        if (sugiliteData.lastTextboxFeature.contentDescription != null) {
            featurePack.contentDescription = sugiliteData.lastTextboxFeature.contentDescription;
        }
        if (sugiliteData.lastTextboxFeature.packageName != null) {
            featurePack.packageName = sugiliteData.lastTextboxFeature.packageName;
        }
        if (sugiliteData.lastTextboxFeature.boundsInScreen != null ) {
            featurePack.boundsInScreen = sugiliteData.lastTextboxFeature.boundsInScreen;
        }

        textboxBlock.setFeaturePack(featurePack);


        boolean success = false;

        textboxBlock.setPreviousBlock(sugiliteData.getCurrentScriptBlock());
        if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock) {
            ((SugiliteOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(textboxBlock);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock) {
            ((SugiliteStartingBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(textboxBlock);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteErrorHandlingForkBlock){
            ((SugiliteErrorHandlingForkBlock) sugiliteData.getCurrentScriptBlock()).setAlternativeNextBlock(textboxBlock);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteSpecialOperationBlock){
            ((SugiliteSpecialOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(textboxBlock);
        }
        else{
            throw new RuntimeException("Unsupported Block Type!");
        }
        sugiliteData.setCurrentScriptBlock(textboxBlock);
        try {
            sugiliteData.getScriptHead().relevantPackages.add(featurePack.packageName);
            sugiliteScriptDao.save(sugiliteData.getScriptHead());
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        System.out.println("saved block");
    }

}

