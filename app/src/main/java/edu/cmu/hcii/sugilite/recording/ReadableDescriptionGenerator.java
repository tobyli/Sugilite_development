package edu.cmu.hcii.sugilite.recording;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;

/**
 * @author toby
 * @date 6/21/16
 * @time 4:03 PM
 */
public class ReadableDescriptionGenerator {
    private Map<String, String> packageNameReadableNameMap;
    private PackageManager packageManager;
    public ReadableDescriptionGenerator(Context applicationContext){
        packageNameReadableNameMap = new HashMap<>();
        setupPackageNameReadableNameMap();
        packageManager = applicationContext.getPackageManager();
    }
    public String generateReadableDescription(SugiliteBlock block){
        String message = "";
        if(block instanceof SugiliteStartingBlock)
            return "<b>START SCRIPT</b>";
        /**
         * structure: [OPERATION] + "the button/textbox/object" + [IDENTIFIER] + "that has [VIEWID]" + at [LOCATION] + in [PACKAGE]
         */
        if(block instanceof SugiliteOperationBlock){
            SugiliteOperation operation = ((SugiliteOperationBlock) block).getOperation();
            //print the operation
            switch (operation.getOperationType()){
                case SugiliteOperation.CLICK:
                    message += setColor("Click ", Const.SCRIPT_ACTION_COLOR) + "on ";
                    break;
                case SugiliteOperation.SELECT:
                    message += setColor("Select ", Const.SCRIPT_ACTION_COLOR);
                    break;
                case SugiliteOperation.SET_TEXT:
                    message += setColor("Set Text ", Const.SCRIPT_ACTION_COLOR) + "to \"" + setColor(((SugiliteSetTextOperation)((SugiliteOperationBlock) block).getOperation()).getText(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + "\" for ";
                    break;
                case SugiliteOperation.LONG_CLICK:
                    message += setColor("Long click ", Const.SCRIPT_ACTION_COLOR) + "on ";
                    break;
                case SugiliteOperation.READ_OUT:
                    message += setColor("Read out ", Const.SCRIPT_ACTION_COLOR) + "the " + setColor(((SugiliteOperationBlock)block).getOperation().getParameter(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " for ";
                    break;
                case SugiliteOperation.LOAD_AS_VARIABLE:
                    message += setColor("Load the value ", Const.SCRIPT_ACTION_COLOR) + "of the" + setColor(((SugiliteOperationBlock)block).getOperation().getParameter(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " as a variable for ";
                case SugiliteOperation.SPECIAL_GO_HOME:
                    return "<b>GO TO HOME SCREEN</b>";
            }

            //print the object type
            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getClassName() != null){
                String className =  ((SugiliteOperationBlock) block).getElementMatchingFilter().getClassName();
                int lastIndex = className.lastIndexOf('.');
                if(lastIndex > -1)
                    message += setColor("the " + className.substring(lastIndex + 1) + " object ", Const.SCRIPT_TARGET_TYPE_COLOR);
                else
                    message += setColor("the object ", Const.SCRIPT_TARGET_TYPE_COLOR);
            }
            boolean thatPrinted = false;

            Map<String, String> labels = new HashMap<>();

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getText() != null){
                labels.put("text", ((SugiliteOperationBlock) block).getElementMatchingFilter().getText());
            }
            if (((SugiliteOperationBlock) block).getElementMatchingFilter().getContentDescription() != null){
                labels.put("content description", ((SugiliteOperationBlock)block).getElementMatchingFilter().getContentDescription());
            }
            if (((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter()!= null && ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getText() != null){
                labels.put("child text", ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getText());
            }
            if (((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter()!= null && ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getContentDescription() != null){
                labels.put("child content description", ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getContentDescription());
            }

            if(labels.size() == 1){
                for(Map.Entry<String, String> entry : labels.entrySet()){
                    message += "\"" + setColor(entry.getValue(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + "\" ";
                }
            }
            else if(labels.size() > 1){
                int count = 0;
                for(Map.Entry<String, String> entry : labels.entrySet()){
                    message += (thatPrinted ? "" : "that ") + "has " + entry.getKey() + " \"" + setColor(entry.getValue(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + "\" " + (count == labels.size() - 2 ? "and " :(count == labels.size() - 1 ? ", " : " "));
                    thatPrinted = true;
                    count ++;
                }
            }

            if (((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter()!= null && ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getViewId() != null){
                message += (thatPrinted ? "" : "that ") + "has child Object ID \"" + setColor(((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter().getViewId(), Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                thatPrinted = true;            }


            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getViewId() != null){
                message += (thatPrinted ? "" : "that ") + "has the Object ID \"" + setColor(((SugiliteOperationBlock) block).getElementMatchingFilter().getViewId(), Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                thatPrinted = true;
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInScreen() != null){
                message += "at the screen location (" + setColor(((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInScreen(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + ") ";
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInParent() != null){
                message += "at the parent location (" + setColor(((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInParent(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + ") ";
            }

            if(((SugiliteOperationBlock) block).getElementMatchingFilter().getPackageName() != null)
                message += "in " + setColor(getReadableName(((SugiliteOperationBlock) block).getElementMatchingFilter().getPackageName()), Const.SCRIPT_WITHIN_APP_COLOR) + " ";
            return message;
        }
        else if (block instanceof SugiliteSpecialOperationBlock){
            return "<b> SPECIAL OPERATION " + setColor(((SugiliteSpecialOperationBlock) block).getDescription(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + "</b>";
        }



        return "NULL";
    }

    private void setupPackageNameReadableNameMap(){
        packageNameReadableNameMap.put("com.google.android.googlequicksearchbox", "Home Screen");
    }

    /**
     * get readable app name from package name
     * @param packageName
     * @return
     */
    public String getReadableName(String packageName){
        ApplicationInfo applicationInfo;
        try{
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        }
        catch (Exception e){
            applicationInfo = null;
        }
        if(packageNameReadableNameMap.containsKey(packageName))
            return packageNameReadableNameMap.get(packageName);
        else if (applicationInfo != null)
            return (String)packageManager.getApplicationLabel(applicationInfo);
        else
            return packageName;
    }

    static public String setColor(String message, String color){
        return "<font color=\"" + color + "\"><b>" + message + "</b></font>";
    }

}
