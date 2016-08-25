package edu.cmu.hcii.sugilite.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.ui.ReadableDescriptionGenerator;

/**
 * @author toby
 * @date 8/24/16
 * @time 10:18 AM
 */
public class Generalizer {

    private SugiliteScriptDao sugiliteScriptDao;
    private ReadableDescriptionGenerator descriptionGenerator;
    private Context context;
    public Generalizer(Context context){
        this.context = context;
        sugiliteScriptDao = new SugiliteScriptDao(context);
        descriptionGenerator = new ReadableDescriptionGenerator(context);
    }


    public void generalize (SugiliteStartingBlock script){
        String command = script.getScriptName();
        boolean modified = false;
        //for each operation block, go through the filters, if find match in the command, replace.
        SugiliteBlock block = script;
        while(block != null){
            //go through the filters
            if(block instanceof SugiliteOperationBlock){
                UIElementMatchingFilter filter = ((SugiliteOperationBlock) block).getElementMatchingFilter();
                if(filter.getText() != null && command.toLowerCase().contains(filter.getText().toLowerCase())){
                    script.variableNameDefaultValueMap.put(filter.getText(), new StringVariable(filter.getText(), filter.getText()));
                    filter.setText("@" + filter.getText());
                    ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                    block.setDescription(descriptionGenerator.generateReadableDescription(block));
                    modified = true;
                }
                if(filter.getContentDescription() != null && command.toLowerCase().contains(filter.getContentDescription().toLowerCase())){
                    script.variableNameDefaultValueMap.put(filter.getContentDescription(), new StringVariable(filter.getContentDescription(), filter.getContentDescription()));
                    filter.setContentDescription("@" + filter.getContentDescription());
                    ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                    block.setDescription(descriptionGenerator.generateReadableDescription(block));
                    modified = true;
                }
                if(filter.getChildFilter() != null) {
                    if (filter.getChildFilter().getText() != null && command.toLowerCase().contains(filter.getChildFilter().getText().toLowerCase())) {
                        UIElementMatchingFilter childFilter = filter.getChildFilter();
                        script.variableNameDefaultValueMap.put(filter.getChildFilter().getText(), new StringVariable(filter.getChildFilter().getText(), filter.getChildFilter().getText()));
                        childFilter.setText("@" + filter.getChildFilter().getText());
                        filter.setChildFilter(childFilter);
                        ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                        block.setDescription(descriptionGenerator.generateReadableDescription(block));
                        modified = true;
                    }
                    if (filter.getChildFilter().getContentDescription() != null && command.toLowerCase().contains(filter.getChildFilter().getContentDescription().toLowerCase())) {
                        UIElementMatchingFilter childFilter = filter.getChildFilter();
                        script.variableNameDefaultValueMap.put(filter.getChildFilter().getContentDescription(), new StringVariable(filter.getChildFilter().getContentDescription(), filter.getChildFilter().getContentDescription()));
                        childFilter.setContentDescription("@" + filter.getChildFilter().getContentDescription());
                        filter.setChildFilter(childFilter);
                        ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                        block.setDescription(descriptionGenerator.generateReadableDescription(block));
                        modified = true;
                    }
                }
                SugiliteOperation operation = ((SugiliteOperationBlock) block).getOperation();
                if(operation instanceof SugiliteSetTextOperation && ((SugiliteSetTextOperation) operation).getText() != null && ((SugiliteSetTextOperation) operation).getText().length() > 0){
                    if(command.toLowerCase().contains(((SugiliteSetTextOperation) operation).getText().toLowerCase())){
                        String originalText = ((SugiliteSetTextOperation) operation).getText();
                        ((SugiliteSetTextOperation) operation).setText("@" + originalText);
                        script.variableNameDefaultValueMap.put(originalText, new StringVariable(originalText, originalText));
                        ((SugiliteOperationBlock) block).setOperation(operation);
                        block.setDescription(descriptionGenerator.generateReadableDescription(block));
                        modified = true;
                    }
                }
            }
            block = block.getNextBlock();
        }
        //save the script as command_generalized
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Script Generalization")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });
        if(modified) {
            String fileName = new String(command);
            fileName = fileName.replace(".SugiliteScript", "");
            script.setScriptName(fileName + "_generalized" + ".SugiliteScript");
            try {
                sugiliteScriptDao.save(script);
                builder.setMessage("Generalization successful!");
            } catch (Exception e) {
                e.printStackTrace();
                builder.setMessage("Generalization failed!");
            }
        }
        else {
            builder.setMessage("Didn't find anything to generalize!");
        }
        builder.show();
    }
}
