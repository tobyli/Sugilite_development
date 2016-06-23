package edu.cmu.hcii.sugilite.ui;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

/**
 * @author toby
 * @date 6/21/16
 * @time 4:03 PM
 */
public class ReadableDescriptionGenerator {
    Map<String, String> packageNameReadableNameMap;
    public ReadableDescriptionGenerator(){
        packageNameReadableNameMap = new HashMap<>();
        setupPackageNameReadableNameMap();
    }
    public String generateReadableDescription(SugiliteBlock block){
        String message = "";
        if(block instanceof SugiliteStartingBlock)
            return "Starting Block";
        /**
         * structure: [OPERATION] + "the button/textbox/object" + [IDENTIFIER] + "that has [VIEWID]" + at [LOCATION] + in [PACKAGE]
         */
        if(block instanceof SugiliteOperationBlock){
            SugiliteOperation operation = ((SugiliteOperationBlock) block).getOperation();

            switch (operation.getOperationType()){
                case SugiliteOperation.CLICK:
                    message += "Click on ";
                    break;
                case SugiliteOperation.SELECT:
                    message += "Select ";
                    break;
                case SugiliteOperation.SET_TEXT:
                    message += "Set Text to \"" + operation.getParameter() + "\" for";
                    break;
                case SugiliteOperation.LONG_CLICK:
                    message += "Long click on ";
                    break;
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getClassName() != null){
                switch (((SugiliteOperationBlock) block).getElementMatchingFilter().getClassName()){
                    case "android.widget.ImageButton":
                    case "android.widget.Button":
                    case "android.widget.TextView":
                    case "android.widget.ImageView":
                        message += "the button ";
                        break;
                    case "android.widget.EditText":
                        message += "the textbox ";
                        break;
                    default:
                        message += "the object";
                }
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getText() != null){
                message += "\"" + ((SugiliteOperationBlock) block).getElementMatchingFilter().getText() + "\" ";
            }
            else if (((SugiliteOperationBlock) block).getElementMatchingFilter().getContentDescription() != null){
                message += "\"" + ((SugiliteOperationBlock) block).getElementMatchingFilter().getContentDescription() + "\" ";
            }
            else if (((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getText() != null){
                message += "\"" + ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getText() + "\" ";
            }
            else if (((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getContentDescription() != null){
                message += "\"" + ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getContentDescription() + "\" ";
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getViewId() != null){
                message += "that has the view ID \"" + ((SugiliteOperationBlock) block).getElementMatchingFilter().getViewId() + "\" ";
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInScreen() != null){
                message += "at screen location (" + ((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInScreen() + ") ";
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInParent() != null){
                message += "at parent location (" + ((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInParent() + ") ";
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getPackageName() != null)
                message += "in " + getReadableName(((SugiliteOperationBlock) block).getElementMatchingFilter().getPackageName()) + " ";
            return message;
        }



        return "NULL";
    }

    private void setupPackageNameReadableNameMap(){

    }

    private String getReadableName(String packageName){
        if(packageNameReadableNameMap.containsKey(packageName))
            return packageNameReadableNameMap.get(packageName);
        else
            return packageName;
    }

}
