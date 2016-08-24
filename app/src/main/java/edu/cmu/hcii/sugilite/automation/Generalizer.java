package edu.cmu.hcii.sugilite.automation;

import android.content.Context;

import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;

/**
 * @author toby
 * @date 8/24/16
 * @time 10:18 AM
 */
public class Generalizer {

    private SugiliteScriptDao sugiliteScriptDao;
    private Context context;
    public Generalizer(Context context){
        this.context = context;
        sugiliteScriptDao = new SugiliteScriptDao(context);
    }


    public void generalize (SugiliteStartingBlock script){
        String command = script.getScriptName();
        boolean modified = false;
        //for each operation block, go through the filters, if find match in the command, replace.
        SugiliteBlock block = script;
        while(block.getNextBlock() != null){
            //go through the filters
            if(block instanceof SugiliteOperationBlock){
                UIElementMatchingFilter filter = ((SugiliteOperationBlock) block).getElementMatchingFilter();
                if(filter.getText() != null && command.contains(filter.getText())){
                    script.variableNameDefaultValueMap.put(filter.getText(), new StringVariable(filter.getText(), filter.getText()));
                    filter.setText("@" + filter.getText());
                    ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                }
                if(filter.getContentDescription() != null && command.contains(filter.getContentDescription())){
                    script.variableNameDefaultValueMap.put(filter.getContentDescription(), new StringVariable(filter.getContentDescription(), filter.getContentDescription()));
                    filter.setContentDescription("@" + filter.getContentDescription());
                    ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                }
                if(filter.getChildFilter() != null){
                    if(filter.getChildFilter().getText() != null && command.contains(filter.getChildFilter().getText())){
                        UIElementMatchingFilter childFilter = filter.getChildFilter();
                        script.variableNameDefaultValueMap.put(filter.getChildFilter().getText(), new StringVariable(filter.getChildFilter().getText(), filter.getChildFilter().getText()));
                        childFilter.setText("@" + filter.getChildFilter().getText());
                        filter.setChildFilter(childFilter);
                        ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                    }
                    if(filter.getChildFilter().getContentDescription() != null && command.contains(filter.getChildFilter().getContentDescription())){
                        UIElementMatchingFilter childFilter = filter.getChildFilter();
                        script.variableNameDefaultValueMap.put(filter.getChildFilter().getContentDescription(), new StringVariable(filter.getChildFilter().getContentDescription(), filter.getChildFilter().getContentDescription()));
                        childFilter.setContentDescription("@" + filter.getChildFilter().getContentDescription());
                        filter.setChildFilter(childFilter);
                        ((SugiliteOperationBlock) block).setElementMatchingFilter(filter);
                    }
                }

            }
            block = block.getNextBlock();
        }
        //save the script as command_generalized
        String fileName = new String(command);
        fileName.replace(".SugiliteScript", "");
        script.setScriptName(fileName + "_generalized" + ".SugiliteScript");
        try {
            sugiliteScriptDao.save(script);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
