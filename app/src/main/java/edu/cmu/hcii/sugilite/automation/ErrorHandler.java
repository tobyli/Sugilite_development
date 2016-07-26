package edu.cmu.hcii.sugilite.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * @author toby
 * @date 7/7/16
 * @time 10:45 AM
 */
public class ErrorHandler {
    private long lastWindowChange, lastSuccess, lastCheckTime;
    private String lastPackageName;
    private Context applicationContext;
    public Set<String> relevantPackages;
    private boolean showingErrorDialog = false;

    public ErrorHandler(Context context){
        this.applicationContext = context;
        relevantPackages = new HashSet<>();
    }

    /*
    ideas: long delay since last success
    wrong package
    long since last window change
     */

    /**
     * execute at every accessibility event (when running)
     * @param event
     * @param nextInstruction
     * @return true if there is an error in running the automation
     */
    public boolean checkError(AccessibilityEvent event, SugiliteBlock nextInstruction, long eventTime){
        if(nextInstruction == null)
            return false;
        switch (event.getEventType()){
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                lastWindowChange = eventTime;
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                lastWindowChange = eventTime;
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                lastWindowChange = eventTime;
                break;
        }

        String description = nextInstruction.getDescription();
        if(event.getSource() != null && event.getSource().getPackageName() != null) {
            lastPackageName = event.getSource().getPackageName().toString();
            System.out.println("last package set to " + lastPackageName);
        }

        //handle wrong package error
        if(relevantPackages != null && relevantPackages.size() > 0 && event.getSource() != null && event.getSource().getPackageName() != null) {
            String currentPackageName = event.getSource().getPackageName().toString();
            if (!relevantPackages.contains(currentPackageName)) {
                //error
                handleError("Wrong package! Now at " + currentPackageName + ", expecting " + description + ".");
                return true;
            }
        }

        //handle timeout error
        checkError(nextInstruction, eventTime);



        return false;
        //TODO: check if the current package is among the relevant package of the operation
    }

    /**
     * execute every N seconds
     * @return
     */
    public boolean checkError(SugiliteBlock nextInstruction, long eventTime){
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        long sinceLastWindowChange = currentTime - lastWindowChange;
        long sinceLastSuccesss = currentTime - lastSuccess;
        lastCheckTime = currentTime;
        if(sinceLastSuccesss > 30000){
            //stucked
            handleError("sinceLastSuccess: " + sinceLastSuccesss + "\n" + "Stucked! Too long since the last success, expecting " + nextInstruction.getDescription());
            return true;
        }
        if(sinceLastWindowChange > 5000){
            //stucked
            handleError("sinceLastWindowChange: " + sinceLastWindowChange + "\n" + "Stucked! Too long since the last window content change, expecting " + nextInstruction.getDescription());
            return true;
        }
        return false;
    }



    private void handleError(String errorMsg){
        AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext);
        builder.setTitle("Script Execution Error")
                .setMessage(errorMsg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        showingErrorDialog = false;
                    }
        });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        if(showingErrorDialog == false)
            dialog.show();
        showingErrorDialog = true;
    }

    /**
     * execute at every successful operation (*.perform() returns true)
     * @param time
     */
    public void reportSuccess(long time){
        lastSuccess = time;
        lastWindowChange = time;
    }

}
