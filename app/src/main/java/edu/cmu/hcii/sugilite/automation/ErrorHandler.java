package edu.cmu.hcii.sugilite.automation;

import android.view.accessibility.AccessibilityEvent;

/**
 * @author toby
 * @date 7/7/16
 * @time 10:45 AM
 */
public class ErrorHandler {
    private long lastCheckTime, lastWindowChange, lastSuccess;
    private String lastPackageName;
    /**
     *
     * @return true if there is an error in running the automation
     */
    /*
    ideas: long delay since last success
    wrong package
    long since last window change

     */
    public boolean checkError(AccessibilityEvent event){

        switch (event.getEventType()){
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                lastWindowChange = event.getEventTime();
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                lastWindowChange = event.getEventTime();
                break;
        }

        lastPackageName = event.getPackageName().toString();
        lastCheckTime = event.getEventTime();
        return false;
    }

    private void handleError(){

    }

    public void reportSuccess(long time){
        lastSuccess = time;
    }

}
