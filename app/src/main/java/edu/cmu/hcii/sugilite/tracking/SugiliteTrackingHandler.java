package edu.cmu.hcii.sugilite.tracking;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTrackingDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

/**
 * Created by toby on 7/25/16.
 */

/*
everytime the start tracking switch changes -> name the tracking record to default_ + DATE_TIME
recording "created_time"
 */
public class SugiliteTrackingHandler {
    private SugiliteData sugiliteData;
    private SugiliteTrackingDao sugiliteTrackingDao;

    public SugiliteTrackingHandler(SugiliteData sugiliteData, Context applicationContext){
        this.sugiliteTrackingDao = new SugiliteTrackingDao(applicationContext);
        this.sugiliteData = sugiliteData;
    }

    public void handle(AccessibilityEvent event, AccessibilityNodeInfo sourceNode, SugiliteAvailableFeaturePack featurePack){
        if(event == null || sourceNode == null)
            return;
        SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        SugiliteOperation sugiliteOperation = null;
        switch (event.getEventType()){
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                sugiliteOperation = new SugiliteOperation();
                sugiliteOperation.setOperationType(SugiliteOperation.CLICK);
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                sugiliteOperation = new SugiliteOperation();
                sugiliteOperation.setOperationType(SugiliteOperation.LONG_CLICK);
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                sugiliteOperation = new SugiliteOperation();
                sugiliteOperation.setOperationType(SugiliteOperation.SELECT);
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                sugiliteOperation = new SugiliteOperation();
                sugiliteOperation.setOperationType(SugiliteOperation.SET_TEXT);
                break;
        }
        Rect boundsInParents = new Rect();
        Rect boundsInScreen = new Rect();
        AccessibilityNodeInfo parentNode = null;
        sourceNode.getBoundsInParent(boundsInParents);
        sourceNode.getBoundsInScreen(boundsInScreen);

        operationBlock.setFeaturePack(featurePack);

        operationBlock.setOperation(sugiliteOperation);
        UIElementMatchingFilter filter = new UIElementMatchingFilter();
        if(sourceNode.getPackageName() != null)
            filter.setPackageName(sourceNode.getPackageName().toString());
        if(sourceNode.getClassName() != null)
            filter.setClassName(sourceNode.getClassName().toString());
        if(sourceNode.getContentDescription() != null)
            filter.setContentDescription(sourceNode.getContentDescription().toString());
        if(sourceNode.getViewIdResourceName() != null)
            filter.setViewId(sourceNode.getViewIdResourceName());
        filter.setBoundsInParent(boundsInParents);
        filter.setBoundsInScreen(boundsInScreen);
        filter.setIsClickable(sourceNode.isClickable());

        operationBlock.setElementMatchingFilter(filter);

        //TODO: set previous block and next block


    }

    private void save(){

    }

}