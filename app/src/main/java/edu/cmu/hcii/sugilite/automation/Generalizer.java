package edu.cmu.hcii.sugilite.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 8/24/16
 * @time 10:18 AM
 */
public class Generalizer {

    private SugiliteScriptDao sugiliteScriptDao;
    private ReadableDescriptionGenerator descriptionGenerator;
    private Context context;
    public Generalizer(Context context, SugiliteData sugiliteData){
        this.context = context;
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        else
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        descriptionGenerator = new ReadableDescriptionGenerator(context);
    }


    public void generalize (SugiliteStartingBlock script){
        //command: often the utterance the user used
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
                    addVariableAlternatives(filter.getText(), "text", script, (SugiliteOperationBlock) block);
                    filter.setText("@" + filter.getText());
                    ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                    block.setDescription(descriptionGenerator.generateReadableDescription(block));
                    modified = true;
                }
                if(filter.getContentDescription() != null && command.toLowerCase().contains(filter.getContentDescription().toLowerCase())){
                    script.variableNameDefaultValueMap.put(filter.getContentDescription(), new StringVariable(filter.getContentDescription(), filter.getContentDescription()));
                    addVariableAlternatives(filter.getContentDescription(), "contentDescription", script, (SugiliteOperationBlock) block);
                    filter.setContentDescription("@" + filter.getContentDescription());
                    ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                    block.setDescription(descriptionGenerator.generateReadableDescription(block));
                    modified = true;
                }
                if(filter.getChildFilter() != null) {
                    if (filter.getChildFilter().getText() != null && command.toLowerCase().contains(filter.getChildFilter().getText().toLowerCase())) {
                        UIElementMatchingFilter childFilter = filter.getChildFilter();
                        script.variableNameDefaultValueMap.put(filter.getChildFilter().getText(), new StringVariable(filter.getChildFilter().getText(), filter.getChildFilter().getText()));
                        addVariableAlternatives(filter.getChildFilter().getText(), "childText", script, (SugiliteOperationBlock) block);
                        childFilter.setText("@" + filter.getChildFilter().getText());
                        filter.setChildFilter(childFilter);
                        ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                        block.setDescription(descriptionGenerator.generateReadableDescription(block));
                        modified = true;
                    }
                    if (filter.getChildFilter().getContentDescription() != null && command.toLowerCase().contains(filter.getChildFilter().getContentDescription().toLowerCase())) {
                        UIElementMatchingFilter childFilter = filter.getChildFilter();
                        script.variableNameDefaultValueMap.put(filter.getChildFilter().getContentDescription(), new StringVariable(filter.getChildFilter().getContentDescription(), filter.getChildFilter().getContentDescription()));
                        addVariableAlternatives(filter.getChildFilter().getContentDescription(), "childContentDescriptio", script, (SugiliteOperationBlock)block);
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
                block = ((SugiliteOperationBlock) block).getNextBlock();
            }
            else if (block instanceof SugiliteStartingBlock)
                block = ((SugiliteStartingBlock) block).getNextBlock();
            else if (block instanceof SugiliteErrorHandlingForkBlock)
                block = ((SugiliteErrorHandlingForkBlock) block).getOriginalNextBlock();
            else
                throw new RuntimeException("Unsupported Block Type!");
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
                sugiliteScriptDao.commitSave();
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

    private void addVariableAlternatives(String variableName, String variableType, SugiliteStartingBlock script, SugiliteOperationBlock operationBlock){
        if(operationBlock == null || operationBlock.getFeaturePack() == null || operationBlock.getFeaturePack().alternativeNodes == null || operationBlock.getFeaturePack().alternativeNodes.size() < 1)
            return;
        Set<SerializableNodeInfo> alternativeNodes = operationBlock.getFeaturePack().alternativeNodes;
        if(operationBlock == null || operationBlock.getElementMatchingFilter() == null)
            return;
        UIElementMatchingFilter filter = operationBlock.getElementMatchingFilter();
        String className = filter.getClassName();

        for(SerializableNodeInfo node : alternativeNodes){
            if(className != null && (node.className == null || (!className.equals(node.className))))
                continue;
            switch (variableType) {
                case "text":
                    if(node.text != null){
                        if(script.variableNameAlternativeValueMap.containsKey(variableName)){
                            script.variableNameAlternativeValueMap.get(variableName).add(node.text);
                        }
                        else {
                            Set<String> stringSet = new HashSet<>();
                            stringSet.add(node.text);
                            script.variableNameAlternativeValueMap.put(variableName, stringSet);
                        }
                    }
                    break;
                case "contentDescription":
                    if(node.contentDescription != null){
                        if(script.variableNameAlternativeValueMap.containsKey(variableName)){
                            script.variableNameAlternativeValueMap.get(variableName).add(node.contentDescription);
                        }
                        else {
                            Set<String> stringSet = new HashSet<>();
                            stringSet.add(node.contentDescription);
                            script.variableNameAlternativeValueMap.put(variableName, stringSet);
                        }
                    }
                    break;
                case "childText":
                    if(node.childText != null){
                        if(script.variableNameAlternativeValueMap.containsKey(variableName)){
                            script.variableNameAlternativeValueMap.get(variableName).addAll(node.childText);
                        }
                        else {
                            Set<String> stringSet = new HashSet<>();
                            stringSet.addAll(node.childText);
                            script.variableNameAlternativeValueMap.put(variableName, stringSet);
                        }
                    }
                    break;
                case "childContentDescription":
                    if(node.childContentDescription != null){
                        if(script.variableNameAlternativeValueMap.containsKey(variableName)){
                            script.variableNameAlternativeValueMap.get(variableName).addAll(node.childContentDescription);
                        }
                        else {
                            Set<String> stringSet = new HashSet<>();
                            stringSet.addAll(node.childContentDescription);
                            script.variableNameAlternativeValueMap.put(variableName, stringSet);
                        }
                    }
                    break;
            }
        }
    }
}
