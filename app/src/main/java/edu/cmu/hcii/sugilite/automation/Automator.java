package edu.cmu.hcii.sugilite.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.ui.BoundingBoxManager;
import edu.cmu.hcii.sugilite.ui.StatusIconManager;

import android.speech.tts.TextToSpeech;

import static edu.cmu.hcii.sugilite.Const.DEBUG_DELAY;
import static edu.cmu.hcii.sugilite.Const.DELAY;
import static edu.cmu.hcii.sugilite.Const.HOME_SCREEN_PACKAGE_NAMES;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;


/**
 * Created by toby on 6/13/16.
 */
public class Automator {
    private SugiliteData sugiliteData;
    private SugiliteAccessibilityService serviceContext;
    private Context context;
    private BoundingBoxManager boundingBoxManager;
    private VariableHelper variableHelper;
    private TextToSpeech tts;
    private SugiliteScriptDao sugiliteScriptDao;
    private LayoutInflater layoutInflater;
    private SharedPreferences sharedPreferences;
    private boolean ttsReady = false;
    private SugiliteScreenshotManager screenshotManager;
    static private Set<String> homeScreenPackageNameSet;

    public Automator(SugiliteData sugiliteData, SugiliteAccessibilityService context, StatusIconManager statusIconManager, SharedPreferences sharedPreferences){
        this.sugiliteData = sugiliteData;
        this.serviceContext = context;
        this.boundingBoxManager = new BoundingBoxManager(context);
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        else
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        this.layoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        this.sharedPreferences = sharedPreferences;
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                ttsReady = true;
            }
        });
        screenshotManager = new SugiliteScreenshotManager(sharedPreferences, context);
        homeScreenPackageNameSet = new HashSet<>();
        homeScreenPackageNameSet.addAll(Arrays.asList(HOME_SCREEN_PACKAGE_NAMES));
    }

    //the return value is not used?
    public boolean handleLiveEvent (AccessibilityNodeInfo rootNode, Context context){
        //TODO: fix the highlighting for matched element
        if(sugiliteData.getInstructionQueueSize() == 0 || rootNode == null)
            return false;
        this.context = context;
        final SugiliteBlock blockToMatch = sugiliteData.peekInstructionQueue();
        if(blockToMatch == null)
            return false;

        if (!(blockToMatch instanceof SugiliteOperationBlock)) {
            //handle non Sugilite operation blocks
            /**
             * nothing really special needed for starting blocks, just add the next block to the queue
             */
            if (blockToMatch instanceof SugiliteStartingBlock) {
                //Toast.makeText(context, "Start running script " + ((SugiliteStartingBlock)blockToMatch).getScriptName(), Toast.LENGTH_SHORT).show();
                sugiliteData.removeInstructionQueueItem();
                addNextBlockToQueue(blockToMatch);
                return true;
            }
            /**
             * handle error handling block - "addNextBlockToQueue" will determine which block to add
             */
            else if (blockToMatch instanceof SugiliteErrorHandlingForkBlock){
                sugiliteData.removeInstructionQueueItem();
                addNextBlockToQueue(blockToMatch);
                return true;
            }
            /**
             * for subscript operation blocks, the subscript should be executed
             */
            else if (blockToMatch instanceof SugiliteSpecialOperationBlock){
                sugiliteData.removeInstructionQueueItem();
                SugiliteSpecialOperationBlock specialOperationBlock = (SugiliteSpecialOperationBlock) blockToMatch;
                try{
                    specialOperationBlock.run(context, sugiliteData, sugiliteScriptDao, sharedPreferences);
                    return true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            else {
                throw new RuntimeException("Unsupported Block Type!");
            }
        }

        else {
            //the blockToMatch is an operation block
            SugiliteOperationBlock operationBlock = (SugiliteOperationBlock) blockToMatch;

            if(operationBlock.isSetAsABreakPoint) {
                sugiliteData.storedInstructionQueueForPause.clear();
                sugiliteData.storedInstructionQueueForPause.addAll(sugiliteData.getCopyOfInstructionQueue());
                sugiliteData.clearInstructionQueue();
                sugiliteData.setCurrentSystemState(SugiliteData.PAUSED_FOR_BREAKPOINT_STATE);
                return false;
            }

//            if (operationBlock.getElementMatchingFilter() == null) {
//                //there is no element matching filter in the operation block
//                if (operationBlock.getOperation().getOperationType() == SugiliteOperation.SPECIAL_GO_HOME) {
//                    //perform the go home operation - because the go home operation will have a null filter
//                    boolean retVal = performAction(null, operationBlock);
//                    if (retVal) {
//                        sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
//                        sugiliteData.removeInstructionQueueItem();
//                        addNextBlockToQueue(operationBlock);
//                        if(sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE) {
//                            try {
//                                screenshotManager.take(false, SugiliteScreenshotManager.DIRECTORY_PATH, SugiliteScreenshotManager.getDebugScreenshotFileNameWithDate());
//                            }
//                            catch (Exception e){
//                                e.printStackTrace();
//                            }
//                        }
//
//                        try {
//                            //wait for DELAY/2 after adding the next block to queue
//                            if(sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE)
//                                Thread.sleep(DEBUG_DELAY / 2);
//                            else
//                                Thread.sleep(DELAY / 2);
//                        } catch (Exception e) {
//                            // do nothing
//                        }
//                    }
//                    return retVal;
//
//                } else
//                    return false;
//            }
//            else {
//                //the operation has an element matching filter
//                variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
//                //if we can match this event, perform the action and remove the head object
//                List<AccessibilityNodeInfo> allNodes = preOrderTraverse(rootNode);
//                List<AccessibilityNodeInfo> filteredNodes = new ArrayList<>();
//                for (AccessibilityNodeInfo node : allNodes) {
//                    if(node.getClassName().toString().contains("EditText")){
//                        if (operationBlock.getElementMatchingFilter().filter(node, variableHelper))
//                            filteredNodes.add(node);
//                    }
//                    else {
//                        if (operationBlock.getElementMatchingFilter().filter(node, variableHelper))
//                            filteredNodes.add(node);
//                    }
//                }
//
//                if (operationBlock.getElementMatchingFilter().getTextOrChildTextOrContentDescription() != null) {
//                    //process the order of TextOrChildOrContentDescription
//                    UIElementMatchingFilter filter = operationBlock.getElementMatchingFilter();
//                    List<AccessibilityNodeInfo> textMatchedNodes = new ArrayList<>();
//                    List<AccessibilityNodeInfo> contentDescriptionMatchedNodes = new ArrayList<>();
//                    List<AccessibilityNodeInfo> childTextMatchedNodes = new ArrayList<>();
//                    List<AccessibilityNodeInfo> childContentDescriptionMatchedNodes = new ArrayList<>();
//
//                    for (AccessibilityNodeInfo node : filteredNodes) {
//
//                        if (node.getText() != null && UIElementMatchingFilter.equalsToIgnoreCaseTrimSymbols(filter.getTextOrChildTextOrContentDescription(), node.getText()))
//                            textMatchedNodes.add(node);
//                        if (node.getContentDescription() != null && UIElementMatchingFilter.equalsToIgnoreCaseTrimSymbols(filter.getTextOrChildTextOrContentDescription(), node.getContentDescription()))
//                            contentDescriptionMatchedNodes.add(node);
//                        boolean childTextMatched = false;
//                        boolean childContentDescriptionMatched = false;
//                        for (AccessibilityNodeInfo childNode : preOrderTraverse(node)) {
//                            if (childTextMatched == false &&
//                                    childNode.getText() != null &&
//                                    UIElementMatchingFilter.equalsToIgnoreCaseTrimSymbols(filter.getTextOrChildTextOrContentDescription(), childNode.getText())) {
//                                childTextMatchedNodes.add(node);
//                                childTextMatched = true;
//                            }
//                            if (childContentDescriptionMatched == false &&
//                                    childNode.getContentDescription() != null &&
//                                    UIElementMatchingFilter.equalsToIgnoreCaseTrimSymbols(filter.getTextOrChildTextOrContentDescription(), childNode.getContentDescription())) {
//                                childContentDescriptionMatchedNodes.add(node);
//                                childContentDescriptionMatched = true;
//                            }
//                        }
//                    }
//
//                    filteredNodes = new ArrayList<>();
//                    if (textMatchedNodes.size() > 0)
//                        filteredNodes.addAll(textMatchedNodes);
//                    else if (contentDescriptionMatchedNodes.size() > 0)
//                        filteredNodes.addAll(contentDescriptionMatchedNodes);
//                    else if (childTextMatchedNodes.size() > 0)
//                        filteredNodes.addAll(childTextMatchedNodes);
//                    else if (childContentDescriptionMatchedNodes.size() > 0)
//                        filteredNodes.addAll(childContentDescriptionMatchedNodes);
//                }
//
//
//                if (filteredNodes.size() == 0)
//                    return false;
//
//                boolean succeeded = false;
//                for (AccessibilityNodeInfo node : filteredNodes) {
//                    //TODO: scrolling to find more nodes -- not only the ones displayed on the current screen
//                    if (operationBlock.getOperation().getOperationType() == SugiliteOperation.CLICK && (!node.isClickable()))
//                        continue;
//                    try {
//                    } catch (Exception e) {
//                        // do nothing
//                    }
//                    //TODO: add handle breakpoint for debugging
//                    boolean retVal = performAction(node, operationBlock);
//                    if (retVal) {
//                /*
//                Rect tempRect = new Rect();
//                node.getBoundsInScreen(tempRect);
//                statusIconManager.moveIcon(tempRect.centerX(), tempRect.centerY());
//                */
//                        if (!succeeded) {
//                            sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
//                            if (sugiliteData.getInstructionQueueSize() > 0)
//                                sugiliteData.removeInstructionQueueItem();
//                            addNextBlockToQueue(operationBlock);
//                        }
//                        succeeded = true;
//
//                        try {
//                            //delay delay/2 length after successfuly performing the action
//                            if(sugiliteData.getCurrentSystemState() == SugiliteData.DEFAULT_STATE)
//                                Thread.sleep(DEBUG_DELAY / 2);
//                            else
//                                Thread.sleep(DELAY / 2);
//                        } catch (Exception e) {
//                            // do nothing
//                        }
//                    }
//                }
//                return succeeded;
//            }
            if (operationBlock.getQuery() == null) {
                //there is no query in the operation block
                if (operationBlock.getOperation().getOperationType() == SugiliteOperation.SPECIAL_GO_HOME) {
                    //perform the go home operation - because the go home operation will have a null filter
                    boolean retVal = performAction(null, operationBlock);
                    if (retVal) {
                        sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                        sugiliteData.removeInstructionQueueItem();
                        addNextBlockToQueue(operationBlock);
                        if(sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE) {
                            try {
                                screenshotManager.take(false, SugiliteScreenshotManager.DIRECTORY_PATH, SugiliteScreenshotManager.getDebugScreenshotFileNameWithDate());
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        try {
                            //wait for DELAY/2 after adding the next block to queue
                            if(sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE)
                                Thread.sleep(DEBUG_DELAY / 2);
                            else
                                Thread.sleep(DELAY / 2);
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                    return retVal;

                } else
                    return false;
            }
            else {
                //the operation has a query
                variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
                //if we can match this event, perform the action and remove the head object

                UISnapshot uiSnapshot = new UISnapshot(rootNode);
                Set<SugiliteEntity> querySet = operationBlock.getQuery().executeOn(uiSnapshot);

                List<AccessibilityNodeInfo> filteredNodes = new ArrayList<AccessibilityNodeInfo>();
                for(SugiliteEntity e : querySet) {
                    if(e.getEntityValue() instanceof AccessibilityNodeInfo){
                        filteredNodes.add((AccessibilityNodeInfo) (e.getEntityValue()));
                    }
                }

                if (filteredNodes.size() == 0)
                    return false;

                boolean succeeded = false;
                for (AccessibilityNodeInfo node : filteredNodes) {
                    //TODO: scrolling to find more nodes -- not only the ones displayed on the current screen
                    if (operationBlock.getOperation().getOperationType() == SugiliteOperation.CLICK && (!node.isClickable()))
                        continue;
                    try {
                    } catch (Exception e) {
                        // do nothing
                    }
                    //TODO: add handle breakpoint for debugging
                    boolean retVal = performAction(node, operationBlock);
                    if (retVal) {
                /*
                Rect tempRect = new Rect();
                node.getBoundsInScreen(tempRect);
                statusIconManager.moveIcon(tempRect.centerX(), tempRect.centerY());
                */
                        if (!succeeded) {
                            sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                            if (sugiliteData.getInstructionQueueSize() > 0)
                                sugiliteData.removeInstructionQueueItem();
                            addNextBlockToQueue(operationBlock);
                        }
                        succeeded = true;

                        try {
                            //delay delay/2 length after successfuly performing the action
                            if(sugiliteData.getCurrentSystemState() == SugiliteData.DEFAULT_STATE)
                                Thread.sleep(DEBUG_DELAY / 2);
                            else
                                Thread.sleep(DELAY / 2);
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                }
                return succeeded;
            }
        }
    }

    public boolean performAction(AccessibilityNodeInfo node, SugiliteOperationBlock block) {

        AccessibilityNodeInfo nodeToAction = node;
        if(block.getOperation().getOperationType() == SugiliteOperation.CLICK){
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        if(block.getOperation().getOperationType() == SugiliteOperation.SET_TEXT){
            variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
            String text = variableHelper.parse(((SugiliteSetTextOperation)block.getOperation()).getText());
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo
                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        }
        if(block.getOperation().getOperationType() == SugiliteOperation.LONG_CLICK){
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        }
        if(block.getOperation().getOperationType() == SugiliteOperation.SELECT){
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_SELECT);
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.SPECIAL_GO_HOME){
            //perform the GO_HOME operation
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startMain);
            return true;
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.READ_OUT){
            if(block.getOperation().getParameter().toLowerCase().contentEquals("text")) {
                if (ttsReady && node != null && node.getText() != null) {
                    tts.speak("Result", TextToSpeech.QUEUE_ADD, null);
                    tts.speak(node.getText().toString(), TextToSpeech.QUEUE_ADD, null);
                }
            }
            else if (block.getOperation().getParameter().toLowerCase().contentEquals("child text")) {
                List<AccessibilityNodeInfo> children = preOrderTraverse(node);
                if (ttsReady && node != null && children != null && children.size() > 0) {
                    String childText = "";
                    for(AccessibilityNodeInfo childNode : children){
                        if(childNode.getText() != null)
                            childText += childNode.getText();
                    }
                    if(childText.length() > 0) {
                        tts.speak("Result", TextToSpeech.QUEUE_ADD, null);
                        tts.speak(childText, TextToSpeech.QUEUE_ADD, null);
                    }
                }
            }
            else if (block.getOperation().getParameter().toLowerCase().contentEquals("content description")){
                if (ttsReady && node != null && node.getContentDescription() != null) {
                    tts.speak("Result", TextToSpeech.QUEUE_ADD, null);
                    tts.speak(node.getContentDescription().toString(), TextToSpeech.QUEUE_ADD, null);
                }
            }
            return true;
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.LOAD_AS_VARIABLE) {
            if (block.getOperation() instanceof SugiliteLoadVariableOperation) {
                String variableName = ((SugiliteLoadVariableOperation) block.getOperation()).getVariableName();
                StringVariable stringVariable = new StringVariable(variableName);
                stringVariable.type = Variable.LOAD_RUNTIME;

                if (block.getOperation().getParameter().contentEquals("Text")) {
                    if (node.getText() != null) {
                        stringVariable.setValue(node.getText().toString());
                    }
                } else if (block.getOperation().getParameter().contentEquals("Child Text")) {
                    List<AccessibilityNodeInfo> children = preOrderTraverse(node);
                    if (ttsReady && node != null && children != null && children.size() > 0) {
                        String childText = "";
                        for (AccessibilityNodeInfo childNode : children) {
                            if (childNode.getText() != null)
                                childText += childNode.getText();
                        }
                        if (childText.length() > 0) {
                            stringVariable.setValue(childText);
                        }
                    }
                } else if (block.getOperation().getParameter().contentEquals("Content Description")) {
                    if (node.getContentDescription() != null) {
                        stringVariable.setValue(node.getContentDescription().toString());
                    }
                }
                if(stringVariable.getValue() != null && stringVariable.getValue().length() > 0){
                    //save the string variable to run time symbol table
                    sugiliteData.stringVariableMap.put(stringVariable.getName(), stringVariable);
                    return true;
                }
                else
                    return false;
            }
        }
        return false;
    }


    private static boolean isChild(AccessibilityNodeInfo child, AccessibilityNodeInfo parent) {
        Rect childBox = new Rect();
        Rect compBox = new Rect();
        child.getBoundsInScreen(childBox);

        int numChildren = parent.getChildCount();
        for(int i = 0; i < numChildren; i++){
            AccessibilityNodeInfo c = parent.getChild(i);
            if(c == null) continue;
            c.getBoundsInScreen(compBox);
            if(child.getClassName().toString().equals(c.getClassName().toString()) &&
                    childBox.contains(compBox) && compBox.contains(childBox)){
                return true;
            }
        }
        return false;
    }

    public static AccessibilityNodeInfo customGetParent(AccessibilityNodeInfo child) {
        AccessibilityNodeInfo potentialParent = child.getParent();
        if(potentialParent == null) return null;
        if(isChild(child, potentialParent)) return potentialParent;

        // this is the wrong parent :(
        int numChildren = potentialParent.getChildCount();
        for(int i = 0 ; i < numChildren; i++){
            AccessibilityNodeInfo newPotentialParent = potentialParent.getChild(i);
            if(newPotentialParent == null) continue;
            if(isChild(child, newPotentialParent)) return newPotentialParent;
        }
        return null;
    }

    /**
     * traverse a tree from the root, and return all the notes in the tree
     * @param root
     * @return
     */
    public static List<AccessibilityNodeInfo> preOrderTraverse(AccessibilityNodeInfo root){
        if(root == null)
            return null;
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        list.add(root);
        int childCount = root.getChildCount();
        for(int i = 0; i < childCount; i ++){
            AccessibilityNodeInfo node = root.getChild(i);
            if(node != null)
                list.addAll(preOrderTraverse(node));
        }
        return list;
    }

    public static List<AccessibilityNodeInfo> preOrderTraverseSiblings(AccessibilityNodeInfo node){
        if(node == null) return null;
        List<AccessibilityNodeInfo> siblingNodes = new ArrayList<AccessibilityNodeInfo>();
        AccessibilityNodeInfo parent = node.getParent();
        if(parent == null) return siblingNodes;
        // adding parent for now
        siblingNodes.add(parent);
        Rect nodeRect = new Rect();
        Rect compRect = new Rect();
        node.getBoundsInScreen(nodeRect);
        int numSibling = parent.getChildCount();
        for (int i = 0; i < numSibling; i++){
            AccessibilityNodeInfo currSib = parent.getChild(i);
            if(currSib == null) continue;
            currSib.getBoundsInScreen(compRect);
            // checking bounding screen + name for equality
            if(currSib.getClassName().toString().equals(node.getClassName().toString()) &&
                    nodeRect.contains(compRect) && compRect.contains(nodeRect)) continue;
            siblingNodes.add(currSib);
        }

        List<AccessibilityNodeInfo> preOrderTraverseSibNode = new ArrayList<AccessibilityNodeInfo>();
        for (AccessibilityNodeInfo sib : siblingNodes) {
            // add all children of the sibling node
            preOrderTraverseSibNode.addAll(Automator.preOrderTraverse(sib));
        }
        return preOrderTraverseSibNode;
    }

    public List<AccessibilityNodeInfo> getClickableList (List<AccessibilityNodeInfo> nodeInfos){
        List<AccessibilityNodeInfo> retList = new ArrayList<>();
        for(AccessibilityNodeInfo node : nodeInfos){
            if(node.isClickable())
                retList.add(node);
        }
        return retList;
    }

    private String textVariableParse (String text, Set<String> variableSet, Map<String, Variable> variableValueMap){
        if(variableSet == null || variableValueMap == null)
            return text;
        String currentText = new String(text);
        for(Map.Entry<String, Variable> entry : variableValueMap.entrySet()){
            if(!variableSet.contains(entry.getKey()))
                continue;
            if(entry.getValue() instanceof StringVariable)
                currentText = currentText.replace("@" + entry.getKey(), ((StringVariable) entry.getValue()).getValue());
        }
        return currentText;
    }

    /**
     * kill a package named packageName
     * @param packageName
     */
    static public void killPackage(String packageName){
        //don't kill the home screen
        if(homeScreenPackageNameSet.contains(packageName))
            return;
        try {
            Process sh = Runtime.getRuntime().exec("su", null, null);
            OutputStream os = sh.getOutputStream();
            os.write(("am force-stop " + packageName).getBytes("ASCII"));
            os.flush();
            os.close();
            System.out.println(packageName);
        } catch (Exception e) {
            System.out.println("FAILED TO KILL RELEVANT PACKAGES (permission denied)");
            e.printStackTrace();
            // do nothing, likely this exception is caused by non-rooted device
        }
    }

    public void addNextBlockToQueue(final SugiliteBlock block){
        if(block == null)
            return;
        if(block instanceof SugiliteStartingBlock)
            sugiliteData.addInstruction(((SugiliteStartingBlock) block).getNextBlock());
        else if (block instanceof  SugiliteOperationBlock)
            sugiliteData.addInstruction(((SugiliteOperationBlock) block).getNextBlock());
        //if the current block is a fork, then SUGILITE needs to determine which "next block" to add to the queue
        else if (block instanceof SugiliteErrorHandlingForkBlock){
            //TODO: add automatic feature if can only find solution for one
            final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle("Choose which branch to execute")
                    .setMessage(Html.fromHtml("<b>Original: </b>" + ((SugiliteErrorHandlingForkBlock) block).getOriginalNextBlock().getDescription() +
                            "<br><br>" + "<b>Alternative: </b>" + ((SugiliteErrorHandlingForkBlock) block).getAlternativeNextBlock().getDescription()))
                    .setNegativeButton("Original", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sugiliteData.addInstruction(((SugiliteErrorHandlingForkBlock) block).getOriginalNextBlock());
                        }
                    })
                    .setPositiveButton("Alternative", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sugiliteData.addInstruction(((SugiliteErrorHandlingForkBlock) block).getAlternativeNextBlock());
                        }
                    });
            serviceContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                }
            });
        }
        else if (block instanceof SugiliteSpecialOperationBlock)
            sugiliteData.addInstruction(((SugiliteSpecialOperationBlock) block).getNextBlock());
        else
            throw new RuntimeException("Unsupported Block Type!");
    }

    /**
     *
     * @param block
     * @return true for original branch, false for alternative branch
     */
    //ONLY THE ALTERNATIVE BLOCK CAN BE ANOTHER FORK BLOCK!!!
    public boolean chooseBranchForForkBlock(SugiliteErrorHandlingForkBlock block){
        return false;
    }




}
