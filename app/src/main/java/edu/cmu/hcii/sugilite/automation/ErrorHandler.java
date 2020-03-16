package edu.cmu.hcii.sugilite.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

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
    ReadableDescriptionGenerator readableDescriptionGenerator;
    private SugiliteScriptDao sugiliteScriptDao;
    private String[] excludedPackageSet = {"com.google.android.inputmethod.pinyin", "com.inMind.inMindAgent", "com.google.android.inputmethod.latin"};

    static final private int LAST_WINDOW_CHANGE_TIMEOUT = 30000, LAST_SUCCESSFUL_OPERATION = 30000;

    public ErrorHandler(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.applicationContext = context;
        relevantPackages = new HashSet<>();
        this.sugiliteData = sugiliteData;
        this.descriptionGenerator = new ReadableDescriptionGenerator(context);
        this.sharedPreferences = sharedPreferences;
        this.readableDescriptionGenerator = new ReadableDescriptionGenerator(context);
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        else
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        excludedPackageFromWrongPackage = new HashSet<>(Arrays.asList(excludedPackageSet));
    }

    public long getLastWindowChange(){
        return lastWindowChange;
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
     * @return true if there is an error in running the automation`
     *
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

        if(event.getSource() != null && event.getSource().getPackageName() != null) {
            String oldPackage = lastPackageName;
            lastPackageName = event.getSource().getPackageName().toString();
            /*
            if(oldPackage != null && lastPackageName != null && !oldPackage.equals(lastPackageName))
                System.out.println("last package set to " + lastPackageName);
                */
        }

        //handle wrong package error
        /*
        if(relevantPackages != null && relevantPackages.size() > 0 && event.getSource() != null && event.getSource().getPackageName() != null) {
            String currentPackageName = event.getSource().getPackageName().toString();
            if (!relevantPackages.contains(currentPackageName) &&
                    (!(excludedPackageFromWrongPackage.contains(currentPackageName) || excludedPackageFromWrongPackage.contains(event.getSource().getPackageName())))) {
                //error
                handleError("<b>Wrong app!</b> Current app is " + ReadableDescriptionGenerator.getColoredHTMLFromMessage(descriptionGenerator.getReadableAppNameFromPackageName(currentPackageName), "#ff00ff") + ". <br><br> Next operation: " + description);
                return true;
            }
        }
        */

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
        //System.out.println("Since last success: " + (currentTime - lastSuccess) + "\n" + "Since last window change: " + (currentTime - lastWindowChange) + "\n\n");
        if(sinceLastSuccesss > LAST_SUCCESSFUL_OPERATION){
            //stucked
            //handleError("The current window is not responding in executing the next operation: " + nextInstruction.getDescription() + "<br><br>" + "sinceLastSuccess: " + sinceLastSuccesss + "<br>" + "Stucked! Too long since the last success.");
            handleError("This page seems different than what was expected. Sugilite cannot find the user interface item it is looking for, which is: " + readableDescriptionGenerator.generateObjectDescription((SugiliteOperationBlock)nextInstruction));
            return true;
        }
        if(sinceLastWindowChange > LAST_WINDOW_CHANGE_TIMEOUT){
            //stucked
            //handleError("The current window is not responding in executing the next operation: " + nextInstruction.getDescription() + "<br><br>" + "sinceLastWindowChange: " + sinceLastWindowChange + "<br>" + "Stucked! Too long since the last window content change.");
            handleError("This page seems different than what was expected. Sugilite cannot find the user interface item it is looking for, which is: " + readableDescriptionGenerator.generateObjectDescription((SugiliteOperationBlock) nextInstruction));
            return true;
        }
        return false;
    }



    private void handleError(String errorMsg){
        //TODO: seprate the error handling dialog out
        //pause the execution when the duck is clicked
        final Queue<SugiliteBlock> storedQueue =  sugiliteData.getCopyOfInstructionQueue();
        sugiliteData.clearInstructionQueue();
        final int previousState = sugiliteData.getCurrentSystemState();
        sugiliteData.setCurrentSystemState(SugiliteData.PAUSED_FOR_ERROR_HANDLING_STATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext);
        builder.setTitle("Script Execution Error")
                .setMessage(Html.fromHtml(errorMsg))
                .setPositiveButton("Keep Waiting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //reset the timer when the user chooses to keep waiting
                        reportSuccess(Calendar.getInstance().getTimeInMillis());
                        sugiliteData.addInstructions(storedQueue);
                        sugiliteData.setCurrentSystemState(previousState);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("End Executing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sugiliteData.clearInstructionQueue();
                        sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                        PumiceDemonstrationUtil.showSugiliteToast("Cleared Operation Queue!", Toast.LENGTH_SHORT);

                        dialog.dismiss();
                    }
                })
                .setNeutralButton("Fix the Script", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        final SugiliteBlock currentBlock = storedQueue.peek().getPreviousBlock();

                        //find the starting block for the current executing script
                        SugiliteBlock mBlock = currentBlock;
                        while(mBlock.getPreviousBlock() != null)
                            mBlock = mBlock.getPreviousBlock();
                        if(!(mBlock instanceof SugiliteStartingBlock))
                            return;
                        final SugiliteStartingBlock startingBlock = (SugiliteStartingBlock)mBlock;
                        System.out.println("*** Found original script " + ((SugiliteStartingBlock) mBlock).getScriptName());

                        AlertDialog.Builder replaceOrParallelDialogBuilder = new AlertDialog.Builder(applicationContext);
                        replaceOrParallelDialogBuilder.setTitle("Create Fork")
                                .setMessage("Do you want to replace the corresponding part in the original script or to create a fork?")
                                .setPositiveButton("Replace", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        forkResumeRecording(startingBlock, currentBlock);
                                        sugiliteData.setCurrentSystemState(SugiliteData.RECORDING_FOR_ERROR_HANDLING_STATE);
                                    }
                                })
                                .setNegativeButton("Create Fork", new DialogInterface.OnClickListener() {
                                    //create a parallel fork and resume recording
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        forkParallelBranchResumeRecording(startingBlock, currentBlock);
                                        sugiliteData.setCurrentSystemState(SugiliteData.RECORDING_FOR_ERROR_HANDLING_STATE);
                                    }
                                });
                        AlertDialog replaceOrParallelDialog = replaceOrParallelDialogBuilder.create();
                        replaceOrParallelDialog.getWindow().setType(OVERLAY_TYPE);
                        replaceOrParallelDialog.show();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        showingErrorDialog = false;
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(OVERLAY_TYPE);
        if(showingErrorDialog == false)
            dialog.show();
        showingErrorDialog = true;
    }

    public void reportSuccess(){
        reportSuccess(Calendar.getInstance().getTimeInMillis());
    }

    /**
     * execute at every successful operation (*.perform() returns true)
     * @param time
     */
    public void reportSuccess(long time){
        lastSuccess = time;
        lastWindowChange = time;
    }

    public void forkResumeRecording(SugiliteStartingBlock startingBlock, SugiliteBlock currentBlock){
        //put the script back to "current recording"
        sugiliteData.setScriptHead(startingBlock);
        sugiliteData.setCurrentScriptBlock(currentBlock);
        sugiliteData.initiatedExternally = false;
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        //resume recording
        prefEditor.putBoolean("recording_in_process", true);
        prefEditor.putString("scriptName", PumiceDemonstrationUtil.removeScriptExtension(startingBlock.getScriptName()));
        prefEditor.commit();
        PumiceDemonstrationUtil.showSugiliteToast("resuming recording", Toast.LENGTH_SHORT);

    }

    /**
     * insert a fork block & resume recoding
     * @param startingBlock
     * @param currentBlock
     * @throws RuntimeException
     */
    public void forkParallelBranchResumeRecording(SugiliteStartingBlock startingBlock, SugiliteBlock currentBlock) throws RuntimeException{
        //put the script back to "current recording"
        sugiliteData.setScriptHead(startingBlock);
        sugiliteData.setCurrentScriptBlock(currentBlock);
        sugiliteData.initiatedExternally = false;

        SugiliteErrorHandlingForkBlock forkBlock = new SugiliteErrorHandlingForkBlock();
        if(currentBlock instanceof SugiliteStartingBlock){
            forkBlock.setOriginalNextBlock(((SugiliteStartingBlock) currentBlock).getNextBlockToRun());
            ((SugiliteStartingBlock) currentBlock).setNextBlock(forkBlock);
        }
        else if(currentBlock instanceof SugiliteOperationBlock){
            forkBlock.setOriginalNextBlock(((SugiliteOperationBlock) currentBlock).getNextBlockToRun());
            ((SugiliteOperationBlock) currentBlock).setNextBlock(forkBlock);
        }
        else if(currentBlock instanceof SugiliteErrorHandlingForkBlock) {
            forkBlock.setOriginalNextBlock(((SugiliteErrorHandlingForkBlock) currentBlock).getAlternativeNextBlock());
            ((SugiliteErrorHandlingForkBlock) currentBlock).setAlternativeNextBlock(forkBlock);
        }
        else {
            throw new RuntimeException("Unsupported Block Type!");
        }

        forkBlock.setPreviousBlock(currentBlock);
        sugiliteData.setCurrentScriptBlock(forkBlock);
        try {
            sugiliteScriptDao.save(startingBlock);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        //resume recording
        prefEditor.putBoolean("recording_in_process", true);
        prefEditor.putString("scriptName", PumiceDemonstrationUtil.removeScriptExtension(startingBlock.getScriptName()));
        prefEditor.commit();
        PumiceDemonstrationUtil.showSugiliteToast("resuming recording", Toast.LENGTH_SHORT);

    }

}
