package edu.cmu.hcii.sugilite.sovite.conversation.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.NewScriptGeneralizer;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteReadoutConstOperation;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dao.PumiceKnowledgeDao;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.study.ScriptUsageLogManager;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 4/25/20
 * @time 1:53 AM
 */
public class SoviteProcedureKnowledgeConfigureDialog {
    private Context context;
    private PumiceKnowledgeManager pumiceKnowledgeManager;
    private PumiceProceduralKnowledge targetKnowledge;
    private LayoutInflater layoutInflater;
    private PumiceKnowledgeDao pumiceKnowledgeDao;
    private PumiceDialogManager pumiceDialogManager;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;

    private AlertDialog dialog;


    public SoviteProcedureKnowledgeConfigureDialog(Context context, PumiceProceduralKnowledge targetKnowledge, PumiceKnowledgeManager pumiceKnowledgeManager, PumiceDialogManager pumiceDialogManager, SugiliteData sugiliteData) {
        this.context = context;
        this.pumiceKnowledgeManager = pumiceKnowledgeManager;
        this.targetKnowledge = targetKnowledge;
        this.pumiceDialogManager = pumiceDialogManager;
        this.layoutInflater = LayoutInflater.from(context);
        this.pumiceKnowledgeDao = new PumiceKnowledgeDao(context, sugiliteData);
        this.sugiliteData = sugiliteData;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();


        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        } else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setPadding(15, 15, 15,15);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        Map<String, PumiceProceduralKnowledge.PumiceProceduralKnowledgeParameter> parameterNameParameterMap = targetKnowledge.getParameterNameParameterMap();
        Map<EditText, String> editTextViewOriginalParameterNameMap = new HashMap<>();
        if (parameterNameParameterMap != null && parameterNameParameterMap.size() > 0) {
            TextView titleTextView = new TextView(context);
            titleTextView.setText("Change slot names");
            mainLayout.addView(titleTextView);

            for (PumiceProceduralKnowledge.PumiceProceduralKnowledgeParameter parameter : parameterNameParameterMap.values()) {
                EditText editTextForParameter = new EditText(context);
                mainLayout.addView(editTextForParameter);
                editTextForParameter.setText(parameter.getParameterName());
                editTextViewOriginalParameterNameMap.put(editTextForParameter, parameter.getParameterName());
            }
        }

        builder.setView(mainLayout);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (Map.Entry<EditText, String> editTextViewOriginalParameterNameEntry : editTextViewOriginalParameterNameMap.entrySet()) {
                    if (! editTextViewOriginalParameterNameEntry.getValue().equals(editTextViewOriginalParameterNameEntry.getKey().getText().toString())) {
                        //change has been made
                        String oldParamName = editTextViewOriginalParameterNameEntry.getValue();
                        String newParamName = editTextViewOriginalParameterNameEntry.getKey().getText().toString();

                        //update the param name in PumiceProceduralKnowledge
                        PumiceProceduralKnowledge.PumiceProceduralKnowledgeParameter parameterObject = targetKnowledge.getParameterNameParameterMap().get(oldParamName);
                        if (parameterObject != null) {
                            parameterObject.setParameterName(newParamName);
                        }
                        targetKnowledge.getParameterNameParameterMap().remove(oldParamName);
                        targetKnowledge.getParameterNameParameterMap().put(newParamName, parameterObject);

                        //update the param name in the underlying script
                        String scriptName = targetKnowledge.getTargetScriptName(pumiceKnowledgeManager);
                        try {
                            SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName);
                            if (script != null) {
                                if (script.variableNameVariableObjectMap != null) {
                                    // change the name of variable object
                                    Variable variableObject = script.variableNameVariableObjectMap.get(oldParamName);
                                    if (variableObject != null) {
                                        variableObject.setName(newParamName);
                                        script.variableNameVariableObjectMap.remove(oldParamName);
                                        script.variableNameVariableObjectMap.put(newParamName, variableObject);
                                    }
                                }

                                if (script.variableNameDefaultValueMap != null) {
                                    // change the name of variable default value
                                    VariableValue defaultValue = script.variableNameDefaultValueMap.get(oldParamName);
                                    if (defaultValue != null) {
                                        defaultValue.setVariableName(newParamName);
                                        script.variableNameDefaultValueMap.remove(oldParamName);
                                        script.variableNameDefaultValueMap.put(newParamName, defaultValue);
                                    }
                                }

                                if (script.variableNameAlternativeValueMap != null) {
                                    // change the name of variable alternative values
                                    Set<VariableValue> alternativeValues = script.variableNameAlternativeValueMap.get(oldParamName);
                                    if (alternativeValues != null) {
                                        for (VariableValue variableValue : alternativeValues) {
                                            variableValue.setVariableName(newParamName);
                                        }
                                        script.variableNameAlternativeValueMap.remove(oldParamName);
                                        script.variableNameAlternativeValueMap.put(newParamName, alternativeValues);
                                    }
                                }

                                // change the references to the variable in the operations
                                for (SugiliteOperationBlock operationBlock : NewScriptGeneralizer.getAllOperationBlocks(script)) {
                                    //edit the original data description query to reflect the new parameters
                                    if (operationBlock.getOperation() != null) {
                                        SugiliteOperation operation = operationBlock.getOperation();
                                        if (operation.getDataDescriptionQueryIfAvailable() != null)
                                        {
                                            OntologyQuery ontologyQuery = operation.getDataDescriptionQueryIfAvailable();
                                            NewScriptGeneralizer.replaceParametersInOntologyQuery(ontologyQuery, "[" + oldParamName + "]", newParamName);
                                            operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(operation, operation.getDataDescriptionQueryIfAvailable()));

                                            if (operation instanceof SugiliteSetTextOperation) {
                                                if (((SugiliteSetTextOperation) operation).getParameter0().equals("[" + oldParamName + "]")) {
                                                    ((SugiliteSetTextOperation) operation).setParameter0("[" + newParamName + "]");
                                                    operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(operation, operation.getDataDescriptionQueryIfAvailable()));
                                                }
                                            }
                                        }
                                    }

                                }

                                // rename the script
                                script.setScriptName(scriptName.replace("[" + oldParamName + "]", "[" + newParamName + "]"));
                                try {
                                    // commit save
                                    sugiliteScriptDao.save(script);
                                    sugiliteScriptDao.commitSave(null);
                                    sugiliteScriptDao.delete(scriptName);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                targetKnowledge.setProcedureName(targetKnowledge.getProcedureName().replace("[" + oldParamName + "]", "[" + newParamName + "]"));

                                // change the script name in procedural knowledge
                                targetKnowledge.changeTargetScriptName(pumiceKnowledgeManager, script.getScriptName());
                                pumiceKnowledgeDao.savePumiceKnowledge(pumiceKnowledgeManager);
                                dialog.dismiss();



                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
        dialog = builder.create();

    }

    void show() {
        initDialog();
        if(dialog != null) {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(OVERLAY_TYPE);
            }
            dialog.show();
        }
    }


}
