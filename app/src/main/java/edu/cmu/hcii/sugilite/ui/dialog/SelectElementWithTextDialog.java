package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;

/**
 * @author toby
 * @date 10/27/16
 * @time 12:29 PM
 */
public class SelectElementWithTextDialog {
    private Context context;
    private AlertDialog dialog;
    private LayoutInflater inflater;
    private SugiliteData sugiliteData;
    private ListView listView;
    private Spinner actionSpinner;
    private GestureOverlayView gestureOverlayView;
    private GestureLibrary gestureLib;
    private WindowManager windowManager;
    private List<AccessibilityNodeInfo> textNodes;
    private SugiliteScriptDao sugiliteScriptDao;
    private final static int READ_OUT = 0, LOAD_AS_VARIABLE = 1;


    public SelectElementWithTextDialog(final Context context, final LayoutInflater inflater, final SugiliteData sugiliteData, SugiliteScriptDao sugiliteScriptDao){
        if(Const.KEEP_ALL_TEXT_LABEL_LIST == false){
            Toast.makeText(context, "Getting Text Selection Failed - Feature is Turned off!", Toast.LENGTH_SHORT);
            return;
        }
        this.context = context;
        this.inflater = inflater;
        this.sugiliteData = sugiliteData;
        this.sugiliteScriptDao = sugiliteScriptDao;
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        gestureOverlayView = new GestureOverlayView(context);
        gestureLib = GestureLibraries.fromFile("/sdcard/gestures");
        gestureOverlayView.addOnGesturePerformedListener(new GestureOverlayView.OnGesturePerformedListener() {
            @Override
            public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
                //remove the gesture overlay
                windowManager.removeViewImmediate(gestureOverlayView);

                //build the dialog first
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View dialogView = inflater.inflate(R.layout.dialog_select_element_with_text, null);
                LinearLayout mainLayout = (LinearLayout)dialogView.findViewById(R.id.dialog_select_element_with_text);
                listView = (ListView)dialogView.findViewById(R.id.text_element_list);
                actionSpinner = (Spinner) dialogView.findViewById(R.id.text_select_action_spinner);

                //options available for actions
                final String[] actionSpinnerSelections = {"Read out", "Load as a variable"};
                final ArrayAdapter<String> actionAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, Arrays.asList(actionSpinnerSelections));
                actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                actionSpinner.setAdapter(actionAdapter);
                actionSpinner.setSelection(0);
                textNodes = new ArrayList<AccessibilityNodeInfo>(sugiliteData.elementsWithTextLabels);

                //initialize the list of texts
                List<String> textElementList = new ArrayList<>();
                final List<AccessibilityNodeInfo> textNodeList = new ArrayList<AccessibilityNodeInfo>();
                for(AccessibilityNodeInfo node : textNodes){
                    if(node.getText() != null && isIntersected(node, gesture)) {
                        textElementList.add(node.getText().toString());
                        textNodeList.add(node);
                    }
                }
                if(listView != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice, textElementList);
                    listView.setAdapter(adapter);

                    View emptyView = dialogView.findViewById(R.id.empty);
                    listView.setEmptyView(emptyView);
                    listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    listView.setItemChecked(0,true);
                }

                builder.setView(dialogView)
                        .setTitle("Sugilite Text Selection")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
                System.out.println("Dialog building finished");

                //
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if(listView != null && listView.getCheckedItemPosition() >= 0 && listView.getItemAtPosition(listView.getCheckedItemPosition()) != null && actionSpinner != null && actionSpinner.getSelectedItemPosition() >= 0) {
                            Toast.makeText(context, actionSpinnerSelections[actionSpinner.getSelectedItemPosition()] + " : " + listView.getItemAtPosition(listView.getCheckedItemPosition()).toString(), Toast.LENGTH_SHORT).show();
                            System.out.println(listView.getItemAtPosition(listView.getCheckedItemPosition()).toString());
                            processSelection(textNodeList.get(listView.getCheckedItemPosition()), actionSpinner.getSelectedItemPosition());
                        }
                        else {
                            Toast.makeText(context, "Failed to get the text selection!", Toast.LENGTH_SHORT);
                            System.out.println("Failed to get the text selection! " + listView.getCheckedItemPosition());
                        }
                        dialog.dismiss();
                    }
                });
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    public void show(){
        if(Const.KEEP_ALL_TEXT_LABEL_LIST == false){
            Toast.makeText(context, "Getting Text Selection Failed - Feature is Turned off!", Toast.LENGTH_SHORT);
            return;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        windowManager.addView(gestureOverlayView, params);
    }

    private boolean isIntersected(AccessibilityNodeInfo node, Gesture gesture){
        Path gesturePath = gesture.toPath();
        Rect nodeBoundingBox = new Rect();
        node.getBoundsInScreen(nodeBoundingBox);
        Path boundingBoxPath = new Path();
        boundingBoxPath.addRect(new RectF(nodeBoundingBox), Path.Direction.CW);
        gesturePath.op(gesturePath, boundingBoxPath, Path.Op.INTERSECT);
        return (!gesturePath.isEmpty());
    }

    private void processSelection(AccessibilityNodeInfo selectedNode, int action){
        if(action == READ_OUT){
            //add a read out block
            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            SugiliteOperation sugiliteOperation = new SugiliteOperation();
            sugiliteOperation.setOperationType(SugiliteOperation.READ_OUT);
            sugiliteOperation.setParameter("text");
            UIElementMatchingFilter filter = new UIElementMatchingFilter();
            if(selectedNode.getPackageName() != null)
                filter.setPackageName(selectedNode.getPackageName().toString());
            if(selectedNode.getClassName() != null)
                filter.setClassName(selectedNode.getClassName().toString());
            Rect boundsInScreen = new Rect();
            selectedNode.getBoundsInScreen(boundsInScreen);
            filter.setBoundsInScreen(boundsInScreen);

            operationBlock.setOperation(sugiliteOperation);
            operationBlock.setElementMatchingFilter(filter);
            operationBlock.setDescription("Read out the text at (" + boundsInScreen.toShortString() + ")");
            System.out.println("CREATE READ_OUT BLOCK FOR " + selectedNode.getText());
            //save the block
            operationBlock.setPreviousBlock(sugiliteData.getCurrentScriptBlock());
            if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock) {
                ((SugiliteOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
            }
            else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock) {
                ((SugiliteStartingBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
            }
            else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteErrorHandlingForkBlock){
                ((SugiliteErrorHandlingForkBlock) sugiliteData.getCurrentScriptBlock()).setAlternativeNextBlock(operationBlock);
            }
            else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteSpecialOperationBlock){
                ((SugiliteSpecialOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
            }
            else{
                throw new RuntimeException("Unsupported Block Type!");
            }
            sugiliteData.setCurrentScriptBlock(operationBlock);
            try {
                sugiliteScriptDao.save(sugiliteData.getScriptHead());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("saved read out block");
        }
        else if (action == LOAD_AS_VARIABLE) {
            ChooseVariableDialog chooseVariableDialog = null;
            if (sugiliteData.getScriptHead() != null && selectedNode.getText() != null)
                chooseVariableDialog = new ChooseVariableDialog(context, null, inflater, sugiliteData, sugiliteData.getScriptHead(), null, selectedNode.getText().toString(), true, sugiliteScriptDao, selectedNode);
            if(chooseVariableDialog != null)
                chooseVariableDialog.show();
        }
    }

}
