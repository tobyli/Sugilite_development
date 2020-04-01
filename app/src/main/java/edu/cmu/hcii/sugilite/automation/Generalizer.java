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
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.util.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.util.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 8/24/16
 * @time 10:18 AM
 */
@Deprecated
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
                    script.variableNameDefaultValueMap.put(filter.getText(), new VariableValue<>(filter.getText(), filter.getText()));
                    script.variableNameVariableObjectMap.put(filter.getText(), new Variable(Variable.USER_INPUT, filter.getText()));
                    addVariableAlternatives(filter.getText(), "text", script, (SugiliteOperationBlock) block);
                    filter.setText("[" + filter.getText() + "]");
                    ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                    block.setDescription(descriptionGenerator.generateReadableDescription(block));
                    modified = true;
                }
                if(filter.getContentDescription() != null && command.toLowerCase().contains(filter.getContentDescription().toLowerCase())){
                    script.variableNameDefaultValueMap.put(filter.getContentDescription(), new VariableValue<>(filter.getContentDescription(), filter.getContentDescription()));
                    script.variableNameVariableObjectMap.put(filter.getContentDescription(), new Variable(Variable.USER_INPUT, filter.getContentDescription()));
                    addVariableAlternatives(filter.getContentDescription(), "contentDescription", script, (SugiliteOperationBlock) block);
                    filter.setContentDescription("[" + filter.getContentDescription() + "]");
                    ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                    block.setDescription(descriptionGenerator.generateReadableDescription(block));
                    modified = true;
                }

                Set<UIElementMatchingFilter> childFilter = filter.getChildFilter();
                if(childFilter != null && childFilter.size() != 0) {
                    for(UIElementMatchingFilter cf : childFilter){
                        String sText = cf.getText();
                        String sContent = cf.getContentDescription();

                        if (sText != null && command.toLowerCase().contains(sText.toLowerCase())) {
                            script.variableNameDefaultValueMap.put(sText, new VariableValue<>(sText, sText));
                            script.variableNameVariableObjectMap.put(sText, new Variable(Variable.USER_INPUT, sText));
                            addVariableAlternatives(sText, "childText", script, (SugiliteOperationBlock) block);
                            cf.setText("[" + sText + "]");
                            // don't have to reset cf here because we are changing the text of the object so it should be updated automatically in the set
                            ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                            block.setDescription(descriptionGenerator.generateReadableDescription(block));
                            modified = true;
                        }

                        if (sContent != null && command.toLowerCase().contains(sContent.toLowerCase())) {
                            script.variableNameDefaultValueMap.put(sContent, new VariableValue<>(sContent, sContent));
                            script.variableNameVariableObjectMap.put(sContent, new Variable(Variable.USER_INPUT, sContent));
                            addVariableAlternatives(sContent, "childContentDescription", script, (SugiliteOperationBlock) block);
                            cf.setContentDescription("[" + sContent + "]");
                            // don't have to reset cf here because we are changing the text of the object so it should be updated automatically in the set
                            ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                            block.setDescription(descriptionGenerator.generateReadableDescription(block));
                            modified = true;
                        }
                    }
                }

                Set<UIElementMatchingFilter> siblingFilter = filter.getSiblingFilter();
                if(siblingFilter != null && siblingFilter.size() != 0) {
                    for(UIElementMatchingFilter sf : siblingFilter){
                        String sText = sf.getText();
                        String sContent = sf.getContentDescription();

                        if (sText != null && command.toLowerCase().contains(sText.toLowerCase())) {
                            script.variableNameDefaultValueMap.put(sText, new VariableValue<>(sText, sText));
                            script.variableNameVariableObjectMap.put(sText, new Variable(Variable.USER_INPUT, sText));
                            addVariableAlternatives(sText, "siblingText", script, (SugiliteOperationBlock) block);
                            sf.setText("[" + sText + "]");
                            // don't have to reset cf here because we are changing the text of the object so it should be updated automatically in the set
                            ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                            block.setDescription(descriptionGenerator.generateReadableDescription(block));
                            modified = true;
                        }

                        if (sContent != null && command.toLowerCase().contains(sContent.toLowerCase())) {
                            script.variableNameDefaultValueMap.put(sContent, new VariableValue<>(sContent, sContent));
                            script.variableNameVariableObjectMap.put(sContent, new Variable(Variable.USER_INPUT, sContent));
                            addVariableAlternatives(sContent, "siblingContentDescription", script, (SugiliteOperationBlock) block);
                            sf.setContentDescription("[" + sContent + "]");
                            // don't have to reset cf here because we are changing the text of the object so it should be updated automatically in the set
                            ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                            block.setDescription(descriptionGenerator.generateReadableDescription(block));
                            modified = true;
                        }
                    }
                }
                SugiliteOperation operation = ((SugiliteOperationBlock) block).getOperation();
                if(operation instanceof SugiliteSetTextOperation && ((SugiliteSetTextOperation) operation).getText() != null && ((SugiliteSetTextOperation) operation).getText().length() > 0){
                    if(command.toLowerCase().contains(((SugiliteSetTextOperation) operation).getText().toLowerCase())){
                        String originalText = ((SugiliteSetTextOperation) operation).getText();
                        ((SugiliteSetTextOperation) operation).setText("[" + originalText + "]");
                        script.variableNameDefaultValueMap.put(originalText, new VariableValue<>(originalText, originalText));
                        script.variableNameVariableObjectMap.put(originalText, new Variable(Variable.USER_INPUT, originalText));
                        ((SugiliteOperationBlock) block).setOperation(operation);
                        block.setDescription(descriptionGenerator.generateReadableDescription(block));
                        modified = true;
                    }
                }
                block = ((SugiliteOperationBlock) block).getNextBlockToRun();
            }
            else if (block instanceof SugiliteStartingBlock)
                block = ((SugiliteStartingBlock) block).getNextBlockToRun();
            else if (block instanceof SugiliteConditionBlock) {///
                block.setDescription(descriptionGenerator.generateReadableDescription(block));
                block = ((SugiliteConditionBlock) block).getNextBlockToRun();
            }
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
            fileName = PumiceDemonstrationUtil.removeScriptExtension(fileName);
            script.setScriptName(PumiceDemonstrationUtil.addScriptExtension(fileName + "_generalized"));
            try {
                sugiliteScriptDao.save(script);
                sugiliteScriptDao.commitSave(null);
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
                            script.variableNameAlternativeValueMap.get(variableName).add(new VariableValue<>(variableName, node.text));
                        }
                        else {
                            Set<VariableValue> stringSet = new HashSet<>();
                            stringSet.add(new VariableValue<>(variableName, node.text));
                            script.variableNameAlternativeValueMap.put(variableName, stringSet);
                        }
                    }
                    break;
                case "contentDescription":
                    if(node.contentDescription != null){
                        if(script.variableNameAlternativeValueMap.containsKey(variableName)){
                            script.variableNameAlternativeValueMap.get(variableName).add(new VariableValue<>(variableName, node.contentDescription));
                        }
                        else {
                            Set<VariableValue> stringSet = new HashSet<>();
                            stringSet.add(new VariableValue<>(variableName, node.contentDescription));
                            script.variableNameAlternativeValueMap.put(variableName, stringSet);
                        }
                    }
                    break;
                case "childText":
                    if(node.childText != null){
                        if (! script.variableNameAlternativeValueMap.containsKey(variableName)) {
                            script.variableNameAlternativeValueMap.put(variableName, new HashSet<>());
                        }
                        Set<VariableValue> stringSet = new HashSet<>();
                        node.childText.forEach(text -> stringSet.add(new VariableValue<>(variableName, text)));
                        script.variableNameAlternativeValueMap.put(variableName, stringSet);

                    }
                    break;
                case "childContentDescription":
                    if(node.childContentDescription != null){
                        if (! script.variableNameAlternativeValueMap.containsKey(variableName)) {
                            script.variableNameAlternativeValueMap.put(variableName, new HashSet<>());
                        }
                        Set<VariableValue> stringSet = new HashSet<>();
                        node.childContentDescription.forEach(text -> stringSet.add(new VariableValue<>(variableName, text)));
                        script.variableNameAlternativeValueMap.put(variableName, stringSet);
                    }
                    break;
            }
        }
    }
}
