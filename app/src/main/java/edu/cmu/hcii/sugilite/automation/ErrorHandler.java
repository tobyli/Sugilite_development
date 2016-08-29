package edu.cmu.hcii.sugilite.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ui.ReadableDescriptionGenerator;

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
    private SugiliteData sugiliteData;
    private ReadableDescriptionGenerator descriptionGenerator;

    public ErrorHandler(Context context, SugiliteData sugiliteData){
        this.applicationContext = context;
        relevantPackages = new HashSet<>();
        this.sugiliteData = sugiliteData;
        this.descriptionGenerator = new ReadableDescriptionGenerator(context);
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
            String oldPackage = lastPackageName;
            lastPackageName = event.getSource().getPackageName().toString();
            if(oldPackage != null && lastPackageName != null && !oldPackage.equals(lastPackageName))
                System.out.println("last package set to " + lastPackageName);
        }

        //handle wrong package error
        if(relevantPackages != null && relevantPackages.size() > 0 && event.getSource() != null && event.getSource().getPackageName() != null) {
            String currentPackageName = event.getSource().getPackageName().toString();
            if (!relevantPackages.contains(currentPackageName)) {
                //error
                handleError("<b>Wrong app!</b> Current app is " + ReadableDescriptionGenerator.setColor(descriptionGenerator.getReadableName(currentPackageName), "#ff00ff") + ". <br><br> Next operation: " + description);
                return true;
            }
        }

        //handle timeout error
        checkError(nextInstruction, eventTime);



        return false;
    }

    /**
     * execute every N seconds
     * @return
     */
    public boolean checkError(SugiliteBlock nextInstruction, long eventTime){
        if(nextInstruction == null)
            return false;
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        long sinceLastWindowChange = currentTime - lastWindowChange;
        long sinceLastSuccesss = currentTime - lastSuccess;
        lastCheckTime = currentTime;
        System.out.println("Since last success: " + (currentTime - lastSuccess) + "\n" +
        "Since last window change: " + (currentTime - lastWindowChange) + "\n\n");
        if(sinceLastSuccesss > 30000){
            //stucked
            handleError("The current window is not responding in executing the next operation: " + nextInstruction.getDescription() + "<br><br>" + "sinceLastSuccess: " + sinceLastSuccesss + "<br>" + "Stucked! Too long since the last success.");
            return true;
        }
        if(sinceLastWindowChange > 10000){
            //stucked
            handleError("The current window is not responding in executing the next operation: " + nextInstruction.getDescription() + "<br><br>" + "sinceLastWindowChange: " + sinceLastWindowChange + "<br>" + "Stucked! Too long since the last window content change.");
            return true;
        }
        return false;
    }



    private void handleError(String errorMsg){
        //pause the execution when the duck is clicked
        final Queue<SugiliteBlock> storedQueue =  sugiliteData.getCopyOfInstructionQueue();
        sugiliteData.clearInstructionQueue();


        //TODO: pause the execution
        AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext);
        builder.setTitle("Script Execution Error")
                .setMessage(Html.fromHtml(errorMsg))
                .setPositiveButton("Keep Waiting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //reset the timer when the user chooses to keep waiting
                        reportSuccess(Calendar.getInstance().getTimeInMillis());
                        sugiliteData.addInstructions(storedQueue);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("End Executing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sugiliteData.clearInstructionQueue();
                        Toast.makeText(applicationContext, "Cleared Operation Queue!", Toast.LENGTH_SHORT).show();
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
