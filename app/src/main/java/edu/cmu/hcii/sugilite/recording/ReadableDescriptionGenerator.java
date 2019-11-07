package edu.cmu.hcii.sugilite.recording;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.util.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteReadoutOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteSetTextOperation;


/**
 * @author toby
 * @date 6/21/16
 * @time 4:03 PM
 */
public class ReadableDescriptionGenerator {
    private static Map<String, String> packageNameReadableNameMap;
    public ReadableDescriptionGenerator(Context applicationContext){
        packageNameReadableNameMap = new HashMap<>();
        setupPackageNameReadableNameMap();
    }

    public String generateDescriptionForVerbalBlock(SugiliteOperationBlock block, String formula, String utterance){
        String message = "";
        SugiliteOperation operation = block.getOperation();
        if(utterance != null && utterance.length() > 0) {
            message += getHTMLColor(utterance + " : ", Const.SCRIPT_ACTION_PARAMETER_COLOR);
        }
        switch (operation.getOperationType()){
            case SugiliteOperation.CLICK:
                message += getHTMLColor("Click ", Const.SCRIPT_ACTION_COLOR) + "on ";
                break;
            case SugiliteOperation.SELECT:
                message += getHTMLColor("Select ", Const.SCRIPT_ACTION_COLOR);
                break;
            case SugiliteOperation.SET_TEXT:
                message += getHTMLColor("Set Text ", Const.SCRIPT_ACTION_COLOR) + "to \"" + getHTMLColor(((SugiliteSetTextOperation)((SugiliteOperationBlock) block).getOperation()).getText(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + "\" for ";
                break;
            case SugiliteOperation.LONG_CLICK:
                message += getHTMLColor("Long click ", Const.SCRIPT_ACTION_COLOR) + "on ";
                break;
            case SugiliteOperation.READ_OUT:
                message += getHTMLColor("Read out ", Const.SCRIPT_ACTION_COLOR) + "the " + getHTMLColor(((SugiliteReadoutOperation)(block.getOperation())).getPropertyToReadout(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " for ";
                break;
            case SugiliteOperation.LOAD_AS_VARIABLE:
                message += getHTMLColor("Load the value ", Const.SCRIPT_ACTION_COLOR) + "of the" + getHTMLColor(((SugiliteLoadVariableOperation)(block.getOperation())).getPropertyToSave(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " as a variable for ";
                break;
            case SugiliteOperation.SPECIAL_GO_HOME:
                return "<b>GO TO HOME SCREEN</b>";
        }

        message += getHTMLColor(formula, Const.SCRIPT_IDENTIFYING_FEATURE_COLOR);
        return message;
    }

    /**
     * generate the readable description for a block
     *
     * @param block
     * @return
     */
    public static Spanned generateReadableDescription(SugiliteBlock block){
        String message = "";
        if(block instanceof SugiliteStartingBlock)
            return Html.fromHtml("<b>START SCRIPT</b>");
        /**
         * structure: [OPERATION] + "the button/textbox/object" + [IDENTIFIER] + "that has [VIEWID]" + at [LOCATION] + in [PACKAGE]
         */
        if(block instanceof SugiliteOperationBlock){
            SugiliteOperation operation = ((SugiliteOperationBlock) block).getOperation();
            //print the operation
            switch (operation.getOperationType()){
                case SugiliteOperation.CLICK:
                    message += getHTMLColor("Click ", Const.SCRIPT_ACTION_COLOR) + "on ";
                    break;
                case SugiliteOperation.SELECT:
                    message += getHTMLColor("Select ", Const.SCRIPT_ACTION_COLOR);
                    break;
                case SugiliteOperation.SET_TEXT:
                    message += getHTMLColor("Set Text ", Const.SCRIPT_ACTION_COLOR) + "to \"" + getHTMLColor(((SugiliteSetTextOperation)((SugiliteOperationBlock) block).getOperation()).getText(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + "\" for ";
                    break;
                case SugiliteOperation.LONG_CLICK:
                    message += getHTMLColor("Long click ", Const.SCRIPT_ACTION_COLOR) + "on ";
                    break;
                case SugiliteOperation.READ_OUT:
                    message += getHTMLColor("Read out ", Const.SCRIPT_ACTION_COLOR) + "the " + getHTMLColor(((SugiliteReadoutOperation)((SugiliteOperationBlock) block).getOperation()).getPropertyToReadout(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " for ";
                    break;
                case SugiliteOperation.LOAD_AS_VARIABLE:
                    message += getHTMLColor("Load the value ", Const.SCRIPT_ACTION_COLOR) + "of the" + getHTMLColor(((SugiliteLoadVariableOperation)((SugiliteOperationBlock) block).getOperation()).getPropertyToSave(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + " as a variable for ";
                    break;
                case SugiliteOperation.SPECIAL_GO_HOME:
                    return Html.fromHtml("<b>GO TO HOME SCREEN</b>");
            }

            //print the object type
            if (((SugiliteOperationBlock) block).getElementMatchingFilter() != null) {
                if (((SugiliteOperationBlock) block).getElementMatchingFilter().getClassName() != null) {
                    String className = ((SugiliteOperationBlock) block).getElementMatchingFilter().getClassName();
                    int lastIndex = className.lastIndexOf('.');
                    if (lastIndex > -1)
                        message += getHTMLColor("the " + className.substring(lastIndex + 1) + " object ", Const.SCRIPT_TARGET_TYPE_COLOR);
                    else
                        message += getHTMLColor("the object ", Const.SCRIPT_TARGET_TYPE_COLOR);
                }

                boolean thatPrinted = false;

                Map<String, String> labels = new HashMap<>();

                if (((SugiliteOperationBlock) block).getElementMatchingFilter().getText() != null) {
                    labels.put("text", ((SugiliteOperationBlock) block).getElementMatchingFilter().getText());
                }
                if (((SugiliteOperationBlock) block).getElementMatchingFilter().getContentDescription() != null) {
                    labels.put("content description", ((SugiliteOperationBlock) block).getElementMatchingFilter().getContentDescription());
                }

                Set<UIElementMatchingFilter> childFilters = ((SugiliteOperationBlock) block).getElementMatchingFilter().getChildFilter();
                if (childFilters != null && childFilters.size() != 0) {
                    for (UIElementMatchingFilter cf : childFilters) {
                        String sText = cf.getText();
                        String sContent = cf.getContentDescription();
                        String sViewId = cf.getViewId();

                        if (sText != null) {
                            labels.put("child text", sText);
                        }
                        if (sContent != null) {
                            labels.put("child content description", sContent);
                        }
                        if (sViewId != null) {
                            message += (thatPrinted ? "" : "that ") + "has child Object ID \"" + getHTMLColor(sViewId, Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                            thatPrinted = true;
                        }
                    }
                }

                Set<UIElementMatchingFilter> siblingFilters = ((SugiliteOperationBlock) block).getElementMatchingFilter().getSiblingFilter();
                if (siblingFilters != null && siblingFilters.size() != 0) {
                    for (UIElementMatchingFilter sf : siblingFilters) {
                        String sText = sf.getText();
                        String sContent = sf.getContentDescription();
                        String sViewId = sf.getViewId();

                        if (sText != null) {
                            labels.put("sibling text", sText);
                        }
                        if (sContent != null) {
                            labels.put("sibling content description", sContent);
                        }
                        if (sViewId != null) {
                            message += (thatPrinted ? "" : "that ") + "has sibling Object ID \"" + getHTMLColor(sViewId, Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                            thatPrinted = true;
                        }
                    }
                }


                if (labels.size() == 1) {
                    for (Map.Entry<String, String> entry : labels.entrySet()) {
                        message += "\"" + getHTMLColor(entry.getValue(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + "\" ";
                    }
                } else if (labels.size() > 1) {
                    int count = 0;
                    for (Map.Entry<String, String> entry : labels.entrySet()) {
                        message += (thatPrinted ? "" : "that ") + "has " + entry.getKey() + " \"" + getHTMLColor(entry.getValue(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + "\" " + (count == labels.size() - 2 ? "and " : (count == labels.size() - 1 ? ", " : " "));
                        thatPrinted = true;
                        count++;
                    }
                }

                if (((SugiliteOperationBlock) block).getElementMatchingFilter().getViewId() != null) {
                    message += (thatPrinted ? "" : "that ") + "has the Object ID \"" + getHTMLColor(((SugiliteOperationBlock) block).getElementMatchingFilter().getViewId(), Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                    thatPrinted = true;
                }

                if (((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInScreen() != null) {
                    message += "at the screen location (" + getHTMLColor(((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInScreen(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + ") ";
                }

                if (((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInParent() != null) {
                    message += "at the parent location (" + getHTMLColor(((SugiliteOperationBlock) block).getElementMatchingFilter().getBoundsInParent(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + ") ";
                }

                if (((SugiliteOperationBlock) block).getElementMatchingFilter().getPackageName() != null) {
                    message += "in " + getHTMLColor(getReadableAppNameFromPackageName(((SugiliteOperationBlock) block).getElementMatchingFilter().getPackageName()), Const.SCRIPT_WITHIN_APP_COLOR) + " ";
                }
                return Html.fromHtml(message);
            } else {
                return new SpannableString(block.toString());
            }
        }
        else if (block instanceof SugiliteSpecialOperationBlock){
            return Html.fromHtml("<b> SPECIAL OPERATION " + getHTMLColor(((SugiliteSpecialOperationBlock) block).getDescription().toString(), Const.SCRIPT_ACTION_PARAMETER_COLOR) + "</b>");
        }
        else if (block instanceof SugiliteConditionBlock) {
            return getConditionBlockDescription(((SugiliteConditionBlock) block), 0);
        }

        return new SpannableString("NULL");
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
                        message += (thatPrinted ? "" : "that ") + "has child Object ID \"" + getHTMLColor(sViewId, Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
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
                        message += (thatPrinted ? "" : "that ") + "has sibling Object ID \"" + getHTMLColor(sViewId, Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                        thatPrinted = true;
                    }
                }
            }

            if(labels.size() == 1){
                for(Map.Entry<String, String> entry : labels.entrySet()){
                    message += "labeled \"" + getHTMLColor(entry.getValue(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + "\" ";
                }
            }
            else if(labels.size() > 1){
                int count = 0;
                for(Map.Entry<String, String> entry : labels.entrySet()){
                    message += (thatPrinted ? "" : "that ") + "has " + entry.getKey() + " \"" + getHTMLColor(entry.getValue(), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + "\" " + (count == labels.size() - 2 ? "and " :(count == labels.size() - 1 ? ", " : " "));
                    thatPrinted = true;
                    count ++;
                }
            }

            if(sugiliteOperationBlock.getElementMatchingFilter().getViewId() != null){
                message += (thatPrinted ? "" : "that ") + "has the Object ID \"" + getHTMLColor((sugiliteOperationBlock.getElementMatchingFilter().getViewId()), Const.SCRIPT_VIEW_ID_COLOR) + "\" ";
                thatPrinted = true;
            }

            if(sugiliteOperationBlock.getElementMatchingFilter().getBoundsInScreen() != null){
                message += "at the screen location (" + getHTMLColor((sugiliteOperationBlock.getElementMatchingFilter().getBoundsInScreen()), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + ") ";
            }

            if(sugiliteOperationBlock.getElementMatchingFilter().getBoundsInParent() != null){
                message += "at the parent location (" + getHTMLColor((sugiliteOperationBlock.getElementMatchingFilter().getBoundsInParent()), Const.SCRIPT_IDENTIFYING_FEATURE_COLOR) + ") ";
            }

            if(sugiliteOperationBlock.getElementMatchingFilter().getPackageName() != null)
                message += "in the " + getHTMLColor(getReadableAppNameFromPackageName(sugiliteOperationBlock.getElementMatchingFilter().getPackageName()), Const.SCRIPT_WITHIN_APP_COLOR) + " app ";
            return message;



        }
        return "NULL";

    }

    /*
   * set description for condition block
   * @param: SugiliteConditionBlock block for which to get description, int count to keep track of how many recursive calls have been made
   */
    public static Spanned getConditionBlockDescription(SugiliteConditionBlock block, int tabCount) {
        SugiliteBooleanExpressionNew booleanExpression = block.getSugiliteBooleanExpressionNew();//SugiliteBooleanExpression booleanExpression = block.getSugiliteBooleanExpression();
        String booleanExpressionString = booleanExpression.toString();

        //boolExp = boolExp.substring(1,boolExp.length()-1).trim();
        //String[] split = boolExp.split("\\(");
        //boolExp = booleanExpression.breakdown();
        /*if(!split[0].contains("&&") && !split[0].contains("||")) {
            boolExp = ReadableDescriptionGenerator.getColoredHTMLFromMessage(boolExp, "#954608");
        }*/

        SugiliteBlock thenBlock = block.getThenBlock();
        SugiliteBlock elseBlock = block.getElseBlock();

        if(elseBlock != null) {
            //condition with no else block
            Spanned tab = Html.fromHtml("&nbsp;&nbsp;&nbsp;&nbsp;");
            Spanned tabs = Html.fromHtml("&nbsp;&nbsp;&nbsp;&nbsp;");
            Spanned tabs2 = Html.fromHtml("");
            for(int c = 0; c < tabCount; c++) {
                tabs = (Spanned) TextUtils.concat(tabs, tab);
                tabs2 = (Spanned) TextUtils.concat(tabs2, tab);
            }
            Spanned ifBlockDes = new SpannableString("");
            SugiliteBlock iterBlock = thenBlock;
            while(iterBlock != null) {
                ifBlockDes = (Spanned) TextUtils.concat(ifBlockDes, Html.fromHtml("<br>"), tabs, iterBlock.getDescription());
                if (iterBlock instanceof SugiliteStartingBlock) {
                    iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
                }
                else if (iterBlock instanceof SugiliteOperationBlock) {
                    iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
                }
                else if (iterBlock instanceof SugiliteSpecialOperationBlock) {
                    iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
                }
                else if (iterBlock instanceof SugiliteConditionBlock) {
                    iterBlock = ((SugiliteConditionBlock) iterBlock).getNextBlockToRun();
                }
                else {
                    new Exception("unsupported block type").printStackTrace();
                }
            }

            Spanned elseBlockDes = new SpannableString("");
            SugiliteBlock iterBlock2 = elseBlock;
            while(iterBlock2 != null) {
                if (! (iterBlock2 instanceof SugiliteStartingBlock)) {
                    if (iterBlock2.getDescription() == null || iterBlock2.getDescription().length() == 0) {
                        iterBlock2.setDescription(iterBlock2.toString());
                    }
                    elseBlockDes = (Spanned) TextUtils.concat(elseBlockDes, Html.fromHtml("<br>"), tabs, generateReadableDescription(iterBlock2));
                } else {

                }
                if (iterBlock2 instanceof SugiliteStartingBlock) {
                    iterBlock2 = ((SugiliteStartingBlock) iterBlock2).getNextBlockToRun();
                }
                else if (iterBlock2 instanceof SugiliteOperationBlock) {
                    iterBlock2 = ((SugiliteOperationBlock) iterBlock2).getNextBlockToRun();
                }
                else if (iterBlock2 instanceof SugiliteSpecialOperationBlock) {
                    iterBlock2 = ((SugiliteSpecialOperationBlock) iterBlock2).getNextBlockToRun();
                }
                else if (iterBlock2 instanceof SugiliteConditionBlock) {
                    iterBlock2 = ((SugiliteConditionBlock) iterBlock2).getNextBlockToRun();
                }
                else {
                    new Exception("unsupported block type").printStackTrace();
                }
            }
            return (Spanned) TextUtils.concat(ReadableDescriptionGenerator.getSpannedColor("If ", Const.SCRIPT_CONDITIONAL_COLOR),
                    booleanExpressionString,
                    Html.fromHtml("<br>"),
                    ReadableDescriptionGenerator.getSpannedColor(" then ", Const.SCRIPT_CONDITIONAL_COLOR),
                    ifBlockDes,
                    Html.fromHtml("<br>"),
                    tabs2,
                    ReadableDescriptionGenerator.getSpannedColor("Otherwise", Const.SCRIPT_CONDITIONAL_COLOR),
                    elseBlockDes);
        }
        else {
            Spanned tab = Html.fromHtml("&nbsp;&nbsp;&nbsp;&nbsp;");
            Spanned tabs = Html.fromHtml("&nbsp;&nbsp;&nbsp;&nbsp;");
            for(int c = 0; c < tabCount-1; c++) {
                tabs = (Spanned) TextUtils.concat(tabs, tab);
            }
            Spanned ifBlockDes = new SpannableString("");
            SugiliteBlock iterBlock = thenBlock;
            while(iterBlock != null) {
                ifBlockDes = (Spanned) TextUtils.concat(ifBlockDes, Html.fromHtml("<br>"), tabs, generateReadableDescription(iterBlock));
                if (iterBlock instanceof SugiliteStartingBlock)
                    iterBlock = ((SugiliteStartingBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteOperationBlock)
                    iterBlock = ((SugiliteOperationBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteSpecialOperationBlock)
                    iterBlock = ((SugiliteSpecialOperationBlock) iterBlock).getNextBlockToRun();
                else if (iterBlock instanceof SugiliteConditionBlock)
                    iterBlock = ((SugiliteConditionBlock) iterBlock).getNextBlockToRun();
                else
                    new Exception("unsupported block type").printStackTrace();

            }
            return (Spanned) TextUtils.concat(ReadableDescriptionGenerator.getSpannedColor("If ", Const.SCRIPT_CONDITIONAL_COLOR),
                    booleanExpressionString,
                    Html.fromHtml("<br>"),
                    ReadableDescriptionGenerator.getSpannedColor(" then ", Const.SCRIPT_CONDITIONAL_COLOR),
                    ifBlockDes);
        }
    }



    private void setupPackageNameReadableNameMap(){
        packageNameReadableNameMap.put("com.google.android.googlequicksearchbox", "Home Screen");
    }

    /**
     * get readable app name from package name
     * @param packageName
     * @return
     */
    public static String getReadableAppNameFromPackageName(String packageName){
        PackageManager packageManager = SugiliteData.getAppContext().getPackageManager();
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
            return getHTMLColor("Button", Const.SCRIPT_TARGET_TYPE_COLOR);
        }
        if(className.toLowerCase().contains("edittext")){
            return getHTMLColor("Textbox", Const.SCRIPT_TARGET_TYPE_COLOR);
        }
        if(className.toLowerCase().contains("image")){
            return getHTMLColor("Image", Const.SCRIPT_TARGET_TYPE_COLOR);
        }
        if(className.toLowerCase().contains("framelayout"))
            return getHTMLColor("Item", Const.SCRIPT_TARGET_TYPE_COLOR);

        int lastIndex = className.lastIndexOf('.');
        if(lastIndex > -1) {
            return getHTMLColor(className.substring(lastIndex + 1), Const.SCRIPT_TARGET_TYPE_COLOR);
        }
        else{
            return getHTMLColor(className, Const.SCRIPT_TARGET_TYPE_COLOR);
        }
    }

    static public String getHTMLColor(String message, String color){
        return "<font color=\"" + color + "\"><b>" + message + "</b></font>";
    }
    static public Spanned getSpannedColor(String message, String color){
        return Html.fromHtml("<font color=\"" + color + "\"><b>" + message + "</b></font>");
    }


}
