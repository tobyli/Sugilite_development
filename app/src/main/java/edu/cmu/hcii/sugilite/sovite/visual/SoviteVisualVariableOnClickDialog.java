package edu.cmu.hcii.sugilite.sovite.visual;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;

import static android.view.View.GONE;
import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 3/29/20
 * @time 9:30 AM
 */
public class SoviteVisualVariableOnClickDialog {
    private Context context;
    private VariableValue currentlySelectedVariableValue;
    private SugiliteStartingBlock subScript;
    private SugiliteGetProcedureOperation getProcedureOperation;
    private SoviteVariableUpdateCallback soviteVariableUpdateCallback;
    private View originalScreenshotView;

    private Map<String, VariableValue> variableNameDefaultValueMap;
    private Map<String, Variable> variableNameVariableObjectMap;
    private Map<String, Set<VariableValue>> variableNameAlternativeValueMap;
    private Map<String, View> variableSelectionViewMap;

    private AlertDialog dialog;
    private boolean toUpdateEvenNoChange;

    public SoviteVisualVariableOnClickDialog (Context context, VariableValue currentlySelectedVariableValue, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable View originalScreenshotView, boolean toUpdateEvenNoChange) {
        this.context = context;
        this.currentlySelectedVariableValue = currentlySelectedVariableValue;
        this.subScript = subScript;
        this.getProcedureOperation = getProcedureOperation;
        this.soviteVariableUpdateCallback = soviteVariableUpdateCallback;
        this.originalScreenshotView = originalScreenshotView;
        this.toUpdateEvenNoChange = toUpdateEvenNoChange;
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_variable_set_value, null);
        LinearLayout mainLayout = (LinearLayout) dialogView.findViewById(R.id.layout_variable_set_value);

        String variableName = currentlySelectedVariableValue.getVariableName();
        String variableCurrentValue = currentlySelectedVariableValue.getVariableValue().toString();

        TextView scriptDescriptionTextView = new TextView(context);
        scriptDescriptionTextView.setPadding(0, PumiceDemonstrationUtil.dpToPx(20), 0, PumiceDemonstrationUtil.dpToPx(5));

        scriptDescriptionTextView.setText(Html.fromHtml(ReadableDescriptionGenerator.getHTMLColor("Task: ", "#000000") +
                getProcedureOperation.getParameterValueReplacedDescription().replace(variableCurrentValue, ReadableDescriptionGenerator.getHTMLColor(String.format("<u>%s</u>", variableName), "#000000"))));
        mainLayout.addView(scriptDescriptionTextView);

        variableNameDefaultValueMap = subScript.variableNameDefaultValueMap;
        variableNameAlternativeValueMap = subScript.variableNameAlternativeValueMap;
        variableNameVariableObjectMap = subScript.variableNameVariableObjectMap;
        variableSelectionViewMap = new HashMap<>();

        Variable variableObject = variableNameVariableObjectMap.get(variableName);
        LinearLayout selectionRowLayout = new LinearLayout(context);
        selectionRowLayout.setOrientation(LinearLayout.HORIZONTAL);
        selectionRowLayout.setWeightSum(3);
        TextView variableNameTextView = new TextView(context);
        variableNameTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        variableNameTextView.setWidth(0);
        variableNameTextView.setText(variableName);
        selectionRowLayout.addView(variableNameTextView);

        if (variableNameAlternativeValueMap != null && variableNameAlternativeValueMap.containsKey(variableName) && variableNameAlternativeValueMap.get(variableName).size() >= 1) {
            //has alternative values stored
            Spinner alternativeValueSpinner = new Spinner(context);
            List<String> spinnerItemList = new ArrayList<>();
            if (currentlySelectedVariableValue.getVariableValue() instanceof String) {
                spinnerItemList.add((String) currentlySelectedVariableValue.getVariableValue());
            }
            for (VariableValue alternative : variableNameAlternativeValueMap.get(variableName)) {
                if (alternative.equals(currentlySelectedVariableValue)) {
                    continue;
                }
                spinnerItemList.add(alternative.getVariableValue().toString());
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, spinnerItemList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            alternativeValueSpinner.setAdapter(spinnerAdapter);

            alternativeValueSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            selectionRowLayout.addView(alternativeValueSpinner);
            alternativeValueSpinner.setSelection(0);
            variableSelectionViewMap.put(variableName, alternativeValueSpinner);
        } else {
            //has no alternative values stored - show edit text to prompt the user to enter value
            EditText variableValue = new EditText(context);
            variableValue.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            variableValue.setWidth(0);
            /*
            this part save the state of the last variable setting
            if(variableNameVariableValueMap.containsKey(entry.getKey()) && variableNameVariableValueMap.get(entry.getKey()) instanceof StringVariable)
                variableValue.setText(((StringVariable) variableNameVariableValueMap.get(entry.getKey())).getValue());
            */
            if (currentlySelectedVariableValue.getVariableValue() instanceof String) {
                variableValue.setText((String) currentlySelectedVariableValue.getVariableValue());
            }
            selectionRowLayout.addView(variableValue);
            variableSelectionViewMap.put(variableName, variableValue);
        }
        mainLayout.addView(selectionRowLayout);

        builder.setView(dialogView)
                //.setTitle(Const.appNameUpperCase + " Parameter Settings")
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
    }

    public void show() {
        initDialog();
        if (dialog != null) {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(OVERLAY_TYPE);
            }
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //replace the variable value in the getProcedureOperation
                    for (Map.Entry<String, View> variableNameViewEntry : variableSelectionViewMap.entrySet()) {
                        String variableName = variableNameViewEntry.getKey();
                        String variableStringValue = null;
                        boolean changeMade = false;

                        if (variableNameViewEntry.getValue() instanceof EditText) {
                            variableStringValue = ((EditText) variableNameViewEntry.getValue()).getText().toString();
                        }
                        if (variableNameViewEntry.getValue() instanceof Spinner) {
                            variableStringValue = ((Spinner) variableNameViewEntry.getValue()).getSelectedItem().toString();
                        }

                        if (variableStringValue != null) {
                            VariableValue<String> newChangedVariableValue = new VariableValue<>(variableName, variableStringValue);
                            if (! variableStringValue.equals(currentlySelectedVariableValue.getVariableValue())) {
                                changeMade = true;
                                getProcedureOperation.getVariableValues().removeIf(variableValue -> variableName.equals(variableValue.getVariableName()));
                                getProcedureOperation.getVariableValues().add(new VariableValue<>(variableName, variableStringValue));
                            }
                            //return getProcedureOperation
                            if ((changeMade || toUpdateEvenNoChange) && soviteVariableUpdateCallback != null) {
                                if (originalScreenshotView != null && originalScreenshotView.getVisibility() == View.VISIBLE) {
                                    originalScreenshotView.setVisibility(GONE);
                                }
                                soviteVariableUpdateCallback.onGetProcedureOperationUpdated(getProcedureOperation, newChangedVariableValue, true);
                            }
                            dialog.dismiss();
                        }
                    }
                }
            });
        }
    }

}
