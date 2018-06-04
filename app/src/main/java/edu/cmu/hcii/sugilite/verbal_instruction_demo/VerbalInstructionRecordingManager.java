package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.operation.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResults;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 12/10/17
 * @time 10:06 PM
 */

public class VerbalInstructionRecordingManager {
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private Context context;
    private SugiliteScriptDao sugiliteScriptDao;
    private ReadableDescriptionGenerator readableDescriptionGenerator;


    public VerbalInstructionRecordingManager(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.readableDescriptionGenerator = new ReadableDescriptionGenerator(context);
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }
    }

    public void addToRecording(VerbalInstructionServerResults.VerbalInstructionResult result, Node node, SerializableUISnapshot uiSnapshot, String utterance){
        //TODO: add the step specified in result to the current recording
        if (sharedPreferences.getBoolean("recording_in_process", false)) {
            //if recording is in process

            //de-serialize the query
            OntologyQuery parentQuery = new OntologyQuery(OntologyQuery.relationType.AND);

            String queryFormula = result.getFormula();
            OntologyQuery query = OntologyQuery.deserialize(queryFormula);
            parentQuery.addSubQuery(query);

            if(node.getClassName() != null) {
                OntologyQuery classQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
                classQuery.addObject(new SugiliteEntity<>(-1, String.class, node.getClassName()));
                classQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
                parentQuery.addSubQuery(classQuery);
            }

            if(node.getPackageName() != null) {
                OntologyQuery packageQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
                packageQuery.addObject(new SugiliteEntity<>(-1, String.class, node.getPackageName()));
                packageQuery.setQueryFunction(SugiliteRelation.HAS_PACKAGE_NAME);
                parentQuery.addSubQuery(packageQuery);
            }


            //using the parent query for now

            //generate the block
            SugiliteOperationBlock operationBlock = generateBlock(parentQuery, parentQuery.toString(), utterance);

            //
            AlertDialog.Builder confirmationDialogBuilder = new AlertDialog.Builder(context);
            confirmationDialogBuilder.setTitle("Save Operation Confirmation").setMessage(Html.fromHtml("Are you sure you want to record the operation: " + operationBlock.getDescription()));
            confirmationDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    //save the block
                    saveBlock(operationBlock, node.getPackageName());
                    Toast.makeText(context, "Added " + queryFormula + " to the current recording", Toast.LENGTH_SHORT).show();
                    Map.Entry<String, Long> boundsInScreenTimeStampPair = new AbstractMap.SimpleEntry<>(node.getBoundsInScreen(), Calendar.getInstance().getTimeInMillis());
                    sugiliteData.NodeToIgnoreRecordingBoundsInScreenTimeStampQueue.add(boundsInScreenTimeStampPair);
                    //TODO: add the block to running
                    sugiliteData.addInstruction(operationBlock);
                }
            })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            final AlertDialog confirmationDialog = confirmationDialogBuilder.create();
            confirmationDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            confirmationDialog.show();
        }
    }

    /**
     * generate a SugiliteOperation block based on an OntologyQuery
     * @param query
     * @param formula
     * @return
     */
    private SugiliteOperationBlock generateBlock(OntologyQuery query, String formula, String utterance){
        //generate the sugilite operation
        SugiliteUnaryOperation sugiliteOperation = new SugiliteUnaryOperation();
        //assume it's click for now -- need to expand to more types of operations
        sugiliteOperation.setOperationType(SugiliteOperation.CLICK);
        SerializableOntologyQuery serializedQuery = new SerializableOntologyQuery(query);

        SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        operationBlock.setOperation(sugiliteOperation);
        operationBlock.setFeaturePack(null);
        operationBlock.setElementMatchingFilter(null);
        operationBlock.setScreenshot(null);
        operationBlock.setQuery(serializedQuery);

        operationBlock.setDescription(readableDescriptionGenerator.generateDescriptionForVerbalBlock(operationBlock, formula, utterance));
        return operationBlock;
    }

    /**
     * save a SugiliteOperationBlock to the current recording script
     * @param operationBlock
     * @param packageName
     */
    private void saveBlock(SugiliteOperationBlock operationBlock, String packageName){
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
            sugiliteData.getScriptHead().relevantPackages.add(packageName);
            sugiliteScriptDao.save(sugiliteData.getScriptHead());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("saved block");
    }

}
