package edu.cmu.hcii.sugilite.model.variable;

import java.io.File;
import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;

/**
 * @author toby
 * @date 3/27/20
 * @time 11:04 AM
 */
public class VariableContext implements Serializable {
    private String packageName;
    private String activityName;
    private Node targetNode;
    private File screenshot;

    public VariableContext() {

    }

    public static VariableContext fromOperationBlockAndAlternativeNode(SugiliteOperationBlock sugiliteOperationBlock, Node alternativeNode) {
        VariableContext variableContext = new VariableContext();
        if (sugiliteOperationBlock != null) {
            if (sugiliteOperationBlock.getSugiliteBlockMetaInfo() != null && sugiliteOperationBlock.getSugiliteBlockMetaInfo().getUiSnapshot() != null) {
                variableContext.setActivityName(sugiliteOperationBlock.getSugiliteBlockMetaInfo().getUiSnapshot().getActivityName());
                variableContext.setPackageName(sugiliteOperationBlock.getSugiliteBlockMetaInfo().getUiSnapshot().getPackageName());
            }
            variableContext.setScreenshot(sugiliteOperationBlock.getScreenshot());
            variableContext.setTargetNode(alternativeNode);
        }
        return variableContext;
    }

    public static VariableContext fromOperationBlockAndItsTargetNode(SugiliteOperationBlock sugiliteOperationBlock) {
        VariableContext variableContext = new VariableContext();
        if (sugiliteOperationBlock != null) {
            if (sugiliteOperationBlock.getSugiliteBlockMetaInfo() != null) {
                if (sugiliteOperationBlock.getSugiliteBlockMetaInfo().getUiSnapshot() != null) {
                    variableContext.setActivityName(sugiliteOperationBlock.getSugiliteBlockMetaInfo().getUiSnapshot().getActivityName());
                    variableContext.setPackageName(sugiliteOperationBlock.getSugiliteBlockMetaInfo().getUiSnapshot().getPackageName());
                }
                if (sugiliteOperationBlock.getSugiliteBlockMetaInfo().getTargetEntity() != null) {
                    variableContext.setTargetNode(sugiliteOperationBlock.getSugiliteBlockMetaInfo().getTargetEntity().getEntityValue());
                }
            }
            variableContext.setScreenshot(sugiliteOperationBlock.getScreenshot());
        }
        return variableContext;
    }

    public void setTargetNode(Node targetNode) {
        this.targetNode = targetNode;
    }

    public void setScreenshot(File screenshot) {
        this.screenshot = screenshot;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public Node getTargetNode() {
        return targetNode;
    }

    public File getScreenshot() {
        return screenshot;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }
}
