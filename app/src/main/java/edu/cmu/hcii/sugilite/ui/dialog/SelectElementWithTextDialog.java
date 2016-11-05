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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
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
    private GestureOverlayView gestureOverlayView;
    private GestureLibrary gestureLib;
    private WindowManager windowManager;


    public SelectElementWithTextDialog(final Context context, final LayoutInflater inflater, final SugiliteData sugiliteData){
        if(Const.KEEP_ALL_TEXT_LABEL_LIST == false){
            Toast.makeText(context, "Getting Text Selection Failed - Feature is Turned off!", Toast.LENGTH_SHORT);
            return;
        }
        this.context = context;
        this.inflater = inflater;
        this.sugiliteData = sugiliteData;
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
                List<String> textElementList = new ArrayList<>();
                for(AccessibilityNodeInfo node : sugiliteData.elementsWithTextLabels){
                    if(node.getText() != null && isIntersected(node, gesture)) {
                        textElementList.add(node.getText().toString());
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
                        if(listView != null && listView.getCheckedItemPosition() > 0 && listView.getItemAtPosition(listView.getCheckedItemPosition()) != null) {
                            Toast.makeText(context, "Chosen option " + listView.getItemAtPosition(listView.getCheckedItemPosition()).toString(), Toast.LENGTH_SHORT);
                            System.out.println(listView.getItemAtPosition(listView.getCheckedItemPosition()).toString());
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

}
