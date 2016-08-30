package edu.cmu.hcii.sugilite.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
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
    private SharedPreferences sharedPreferences;
    private Set<String> excludedPackageFromWrongPackage;
    private String[] excludedPackageSet = {"com.google.android.inputmethod.pinyin"};

    public ErrorHandler(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.applicationContext = context;
        relevantPackages = new HashSet<>();
        this.sugiliteData = sugiliteData;
        this.descriptionGenerator = new ReadableDescriptionGenerator(context);
        this.sharedPreferences = sharedPreferences;
        excludedPackageFromWrongPackage = new HashSet<>(Arrays.asList(excludedPackageSet));
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

        if(nextInstruction instanceof SugiliteOperationBlock){
            SugiliteOperation operation = ((SugiliteOperationBlock) nextInstruction).getOperation();
            if(operation.getOperationType() == SugiliteOperation.SPECIAL_GO_HOME)
                return false;
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
            if (!relevantPackages.contains(currentPackageName) &&
                    (!(excludedPackageFromWrongPackage.contains(currentPackageName) || excludedPackageFromWrongPackage.contains(event.getSource().getPackageName())))) {
                //error
                handleError("<b>Wrong app!</b> Current app is " + ReadableDescriptionGenerator.setColor(descriptionGenerator.getReadableName(currentPackageName), "#ff00ff") + ". <br><br> Next operation: " + description);
                return true;
            }
        }

        //handle timeout error
        return checkError(nextInstruction);
    }

    /**
     * execute every N seconds
     * @return
     */
    public boolean checkError(SugiliteBlock nextInstruction){
        if(nextInstruction == null || sharedPreferences.getBoolean("recording_in_process", true))
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
                .setNeutralButton("Create Fork", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SugiliteBlock currentBlock = storedQueue.peek().getPreviousBlock();
                        //find the starting block for the current executing script
                        SugiliteBlock mBlock = currentBlock;
                        while(mBlock.getPreviousBlock() != null)
                            mBlock = mBlock.getPreviousBlock();
                        if(!(mBlock instanceof SugiliteStartingBlock))
                            return;
                        SugiliteStartingBlock startingBlock = (SugiliteStartingBlock)mBlock;
                        System.out.println("*** Found original script " + ((SugiliteStartingBlock) mBlock).getScriptName());
                        startingBlock.setScriptName(startingBlock.getScriptName().replace(".SugiliteScript", "") + "_forked" + ".SugiliteScript");
                        //put the script back to "current recording"
                        sugiliteData.setScriptHead(startingBlock);
                        sugiliteData.setCurrentScriptBlock(currentBlock);
                        sugiliteData.initiatedExternally = false;
                        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                        //resume recording
                        prefEditor.putBoolean("recording_in_process", true);
                        prefEditor.putString("scriptName", startingBlock.getScriptName().replace(".SugiliteScript", ""));
                        prefEditor.commit();
                        Toast.makeText(applicationContext, "resuming recording", Toast.LENGTH_SHORT).show();
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
