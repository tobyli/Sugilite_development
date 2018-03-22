package edu.cmu.hcii.sugilite.recording;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.operation.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.operation.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.util.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteReadoutOperation;
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

    public String generateDescriptionForVerbalBlock(SugiliteOperationBlock block, String formula, String utterance){
        String message = "";
        SugiliteOperation operation = block.getOperation();
        if(utterance != null && utterance.length() > 0) {
            message += setColor(utterance + " : ", Const.SCRIPT_ACTION_PARAMETER_COLOR);
        }
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
                message += setColor("Read out ", Const.SCRIPT_ACTION_COLOR) + "the " + setColor(((SugiliteReadoutOperation)(block.getOperation())).getPropertyToReadout(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " for ";
                break;
            case SugiliteOperation.LOAD_AS_VARIABLE:
                message += setColor("Load the value ", Const.SCRIPT_ACTION_COLOR) + "of the" + setColor(((SugiliteLoadVariableOperation)(block.getOperation())).getPropertyToSave(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " as a variable for ";
                break;
            case SugiliteOperation.SPECIAL_GO_HOME:
                return "<b>GO TO HOME SCREEN</b>";
        }

        message += setColor(formula, Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);
        return message;
    }

    /**
     * generate the readable description for a block
     *
     * @param block
     * @return
     */
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
                    message += setColor("Read out ", Const.SCRIPT_ACTION_COLOR) + "the " + setColor(((SugiliteReadoutOperation)((SugiliteOperationBlock) block).getOperation()).getPropertyToReadout(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " for ";
                    break;
                case SugiliteOperation.LOAD_AS_VARIABLE:
                    message += setColor("Load the value ", Const.SCRIPT_ACTION_COLOR) + "of the" + setColor(((SugiliteLoadVariableOperation)((SugiliteOperationBlock) block).getOperation()).getPropertyToSave(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " as a variable for ";
                    break;
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

            Set<UIElementMatchingFilter> childFilters = ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter();
            if(childFilters != null && childFilters.size() != 0){
                for (UIElementMatchingFilter cf : childFilters) {
                    String sText = cf.getText();
                    String sContent = cf.getContentDescription();
                    String sViewId = cf.getViewId();

                    if(sText != null) {
                        labels.put("child text", sText);
                    }
                    if(sContent != null) {
                        labels.put("child content description", sContent);
                    }
                    if(sViewId != null){
                        message += (thatPrinted ? "" : "that ") + "has child Object ID \"" + setColor(sViewId, Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                        thatPrinted = true;
                    }
                }
            }

            Set<UIElementMatchingFilter> siblingFilters = ((SugiliteOperationBlock) block).getElementMatchingFilter().getSiblingFilter();
            if(siblingFilters != null && siblingFilters.size() != 0){
                for (UIElementMatchingFilter sf : siblingFilters) {
                    String sText = sf.getText();
                    String sContent = sf.getContentDescription();
                    String sViewId = sf.getViewId();

                    if(sText != null) {
                        labels.put("sibling text", sText);
                    }
                    if(sContent != null) {
                        labels.put("sibling content description", sContent);
                    }
                    if(sViewId != null){
                        message += (thatPrinted ? "" : "that ") + "has sibling Object ID \"" + setColor(sViewId, Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                        thatPrinted = true;
                    }
                }
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

    public String generateObjectDescription(SugiliteOperationBlock sugiliteOperationBlock){
        UIElementMatchingFilter filter = sugiliteOperationBlock.getElementMatchingFilter();
        if(filter != null){
            String message = "";
            message += "The ";

            if(filter.getClassName() != null){
                message += getReadableClassName(filter.getClassName()) + " ";
            }
            else {
                message += "item ";
            }

            Map<String, String> labels = new HashMap<>();

            if(sugiliteOperationBlock.getElementMatchingFilter().getText() != null){
                labels.put("text", (sugiliteOperationBlock.getElementMatchingFilter().getText()));
            }
            if (sugiliteOperationBlock.getElementMatchingFilter().getContentDescription() != null){
                labels.put("content description", (sugiliteOperationBlock.getElementMatchingFilter().getContentDescription()));
            }
            boolean thatPrinted = false;

            Set<UIElementMatchingFilter> childFilters = (sugiliteOperationBlock.getElementMatchingFilter().getChildFilter());
            if(childFilters != null && childFilters.size() != 0){
                for (UIElementMatchingFilter cf : childFilters) {
                    String sText = cf.getText();
                    String sContent = cf.getContentDescription();
                    String sViewId = cf.getViewId();

                    if(sText != null) {
                        labels.put("child text", sText);
                    }
                    if(sContent != null) {
                        labels.put("child content description", sContent);
                    }
                    if(sViewId != null){
                        message += (thatPrinted ? "" : "that ") + "has child Object ID \"" + setColor(sViewId, Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                        thatPrinted = true;
                    }
                }
            }

            Set<UIElementMatchingFilter> siblingFilters = sugiliteOperationBlock.getElementMatchingFilter().getSiblingFilter();
            if(siblingFilters != null && siblingFilters.size() != 0){
                for (UIElementMatchingFilter sf : siblingFilters) {
                    String sText = sf.getText();
                    String sContent = sf.getContentDescription();
                    String sViewId = sf.getViewId();

                    if(sText != null) {
                        labels.put("sibling text", sText);
                    }
                    if(sContent != null) {
                        labels.put("sibling content description", sContent);
                    }
                    if(sViewId != null){
                        message += (thatPrinted ? "" : "that ") + "has sibling Object ID \"" + setColor(sViewId, Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                        thatPrinted = true;
                    }
                }
            }

            if(labels.size() == 1){
                for(Map.Entry<String, String> entry : labels.entrySet()){
                    message += "labeled \"" + setColor(entry.getValue(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + "\" ";
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

            if(sugiliteOperationBlock.getElementMatchingFilter().getViewId() != null){
                message += (thatPrinted ? "" : "that ") + "has the Object ID \"" + setColor((sugiliteOperationBlock.getElementMatchingFilter().getViewId()), Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                thatPrinted = true;
            }

            if(sugiliteOperationBlock.getElementMatchingFilter().getBoundsInScreen() != null){
                message += "at the screen location (" + setColor((sugiliteOperationBlock.getElementMatchingFilter().getBoundsInScreen()), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + ") ";
            }

            if(sugiliteOperationBlock.getElementMatchingFilter().getBoundsInParent() != null){
                message += "at the parent location (" + setColor((sugiliteOperationBlock.getElementMatchingFilter().getBoundsInParent()), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + ") ";
            }

            if(sugiliteOperationBlock.getElementMatchingFilter().getPackageName() != null)
                message += "in the " + setColor(getReadableName(sugiliteOperationBlock.getElementMatchingFilter().getPackageName()), Const.SCRIPT_WITHIN_APP_COLOR) + " app ";
            return message;



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

    public String getReadableClassName(String className){
        if(className.toLowerCase().contains("button")){
            return setColor("Button", Const.SCRIPT_TARGET_TYPE_COLOR);
        }
        if(className.toLowerCase().contains("edittext")){
            return setColor("Textbox", Const.SCRIPT_TARGET_TYPE_COLOR);
        }
        if(className.toLowerCase().contains("image")){
            return setColor("Image", Const.SCRIPT_TARGET_TYPE_COLOR);
        }
        if(className.toLowerCase().contains("framelayout"))
            return setColor("Item", Const.SCRIPT_TARGET_TYPE_COLOR);

        int lastIndex = className.lastIndexOf('.');
        if(lastIndex > -1) {
            return setColor(className.substring(lastIndex + 1), Const.SCRIPT_TARGET_TYPE_COLOR);
        }
        else{
            return setColor(className, Const.SCRIPT_TARGET_TYPE_COLOR);
        }
    }

    static public String setColor(String message, String color){
        return "<font color=\"" + color + "\"><b>" + message + "</b></font>";
    }

}
