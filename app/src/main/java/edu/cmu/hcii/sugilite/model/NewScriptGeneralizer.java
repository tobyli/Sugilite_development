package edu.cmu.hcii.sugilite.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.AutomatorUtil;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlockMetaInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteReadoutOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteReadoutConstOperation;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableContext;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.ontology.*;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.ontology.SugiliteRelation.HAS_CHILD;
import static edu.cmu.hcii.sugilite.ontology.SugiliteRelation.HAS_CLASS_NAME;
import static edu.cmu.hcii.sugilite.ontology.SugiliteRelation.HAS_PARENT;
import static edu.cmu.hcii.sugilite.ontology.SugiliteRelation.IS_CLICKABLE;

/**
 * @author toby
 * @date 7/1/19
 * @time 4:29 PM
 */
public class NewScriptGeneralizer {
    private Activity context;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;
    private static final int DEFAULT_DEPTH_LIMIT = 3;
    private int depthLimit;

    public NewScriptGeneralizer(Activity context, int depthLimit) {
        this.context = context;
        this.depthLimit = depthLimit;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();
    }

    public NewScriptGeneralizer(Activity context) {
        this(context, DEFAULT_DEPTH_LIMIT);
    }


        /**
         * extract parameters and their possible values from a script based on the input user utterance
         * @param sugiliteStartingBlock
         * @param userUtterance
         */
    public void extractParameters (SugiliteStartingBlock sugiliteStartingBlock, String userUtterance, Runnable onGeneralizationReadyCallback) {
        Thread extractParameterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int parameterNumberCounter = 1;

                //clear the existing variable maps
                if (sugiliteStartingBlock.variableNameDefaultValueMap == null) {
                    sugiliteStartingBlock.variableNameDefaultValueMap = new HashMap<>();
                }

                if (sugiliteStartingBlock.variableNameAlternativeValueMap == null) {
                    sugiliteStartingBlock.variableNameAlternativeValueMap = new HashMap<>();
                }
                sugiliteStartingBlock.variableNameDefaultValueMap.clear();
                sugiliteStartingBlock.variableNameAlternativeValueMap.clear();

                //go through all operation blocks to check if * the selected data description * of any clicked entity matches the user utterance
                for (SugiliteOperationBlock operationBlock : getAllOperationBlocks(sugiliteStartingBlock)) {

                    OntologyQuery ontologyQuery = operationBlock.getOperation().getDataDescriptionQueryIfAvailable();
                    if ((!operationBlock.getOperation().containsDataDescriptionQuery()) || ontologyQuery == null) {
                        // skip operation blocks without a data description query
                        continue;
                    }

                    List<Pair<SugiliteRelation, String>> allStringsUsedInTheDataDescriptionQuery = getAllStringsUsedInTheDataDescriptionQuery(ontologyQuery);
                    SugiliteBlockMetaInfo blockMetaInfo = operationBlock.getSugiliteBlockMetaInfo();

                    if (blockMetaInfo != null && blockMetaInfo.getUiSnapshot() != null && blockMetaInfo.getTargetEntity() != null) {

                        // skip variables on the home screen
                        boolean operationIsOnHomeScreen = false;
                        //ignore parameters found in the home screen
                        String packageName = blockMetaInfo.getUiSnapshot().getPackageName();
                        if (packageName != null && AutomatorUtil.isHomeScreenPackage(packageName)) {
                            operationIsOnHomeScreen = true;
                        }

                        if (operationIsOnHomeScreen) {
                            continue;
                        }

                        for (String homeScreenPackageName : Const.HOME_SCREEN_PACKAGE_NAMES) {
                            if (PumiceDemonstrationUtil.checkIfOntologyQueryContains(ontologyQuery, SugiliteRelation.HAS_PACKAGE_NAME, homeScreenPackageName)) {
                                operationIsOnHomeScreen = true;
                                break;
                            }
                        }

                        if (operationIsOnHomeScreen) {
                            continue;
                        }

                        // for parameters found in the data description queries
                        for (Pair<SugiliteRelation, String> sugiliteRelationTextLabelPair : allStringsUsedInTheDataDescriptionQuery) {
                            //TODO: support more complex matching method than exact string matching


                            SugiliteRelation relation = sugiliteRelationTextLabelPair.first;
                            String textLabel = sugiliteRelationTextLabelPair.second;

                            if (textLabel == null || textLabel.trim().length() == 0) {
                                continue;
                            }

                            if (userUtterance.toLowerCase().contains(textLabel.toLowerCase())) {
                                //matched
                                int parameterNumber = parameterNumberCounter++;
                                System.out.printf("Found parameter %d: \"%s\" in the utterance was found in the operation %s\n", parameterNumber, textLabel, operationBlock.toString());

                                //extract possible values from uiSnapshot
                                Pair<SugiliteSerializableEntity<Node>, Map<SugiliteSerializableEntity<Node>, List<String>>> result = getPossibleValueForParameter(blockMetaInfo.getTargetEntity(), blockMetaInfo.getUiSnapshot(), relation, depthLimit);
                                Map<SugiliteSerializableEntity<Node>, List<String>> alternativeNodeTextLabelsMap = result.second;
                                SugiliteSerializableEntity<Node> parentNode = result.first;

                                //construct the Variable object
                                String variableName = String.format("parameter%d", parameterNumber);
                                Variable variableObject = new Variable(Variable.USER_INPUT, variableName);
                                VariableContext variableContext = VariableContext.fromOperationBlockAndAlternativeNode(operationBlock, parentNode.getEntityValue());
                                variableObject.setVariableContext(variableContext);

                                //construct the default value of the variable
                                VariableValue<String> defaultVariableValue = new VariableValue<String>(variableName, textLabel);
                                VariableContext defaultVariableValueContext = VariableContext.fromOperationBlockAndItsTargetNode(operationBlock);
                                defaultVariableValue.setVariableValueContext(defaultVariableValueContext);


                                //construct the alternative values for the variable
                                Map <String, SugiliteSerializableEntity<Node>> alternativeValueNodeMap = new HashMap<>();
                                for (Map.Entry<SugiliteSerializableEntity<Node>, List<String>> entry : alternativeNodeTextLabelsMap.entrySet()) {
                                    for (String altTextLabel : entry.getValue()) {
                                        alternativeValueNodeMap.put(altTextLabel, entry.getKey());
                                    }
                                }
                                Object lock = new Object();

                                //add a pop up to allow the user to choose between alternative values
                                SelectAlternativeValueDialog selectAlternativeValueDialog = new SelectAlternativeValueDialog(context, alternativeValueNodeMap, new Runnable() {
                                    @Override
                                    public void run() {
                                        Set<VariableValue> alternativeValues = new HashSet<>();
                                        for (Map.Entry<String, SugiliteSerializableEntity<Node>> entry : alternativeValueNodeMap.entrySet()) {
                                            VariableValue<String> alternativeVariableValue = new VariableValue<>(variableName, entry.getKey());
                                            VariableContext alternativeVariableValueContext = VariableContext.fromOperationBlockAndAlternativeNode(operationBlock, entry.getValue().getEntityValue());
                                            alternativeVariableValue.setVariableValueContext(alternativeVariableValueContext);
                                            alternativeValues.add(alternativeVariableValue);
                                        }

                                        //add the default value to the set of alternative value too
                                        alternativeValues.add(defaultVariableValue);


                                        //fill the results back to the SugiliteStartingBlock
                                        sugiliteStartingBlock.variableNameDefaultValueMap.put(variableName, defaultVariableValue);
                                        sugiliteStartingBlock.variableNameAlternativeValueMap.put(variableName, alternativeValues);
                                        sugiliteStartingBlock.variableNameVariableObjectMap.put(variableName, variableObject);

                                        //edit the original data description query to reflect the new parameters
                                        replaceParametersInOntologyQuery(ontologyQuery, textLabel, variableName);
                                        operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(operationBlock.getOperation(), operationBlock.getOperation().getDataDescriptionQueryIfAvailable()));

                                        //replace the occurrence of parameter default values in the script name
                                        sugiliteStartingBlock.setScriptName(sugiliteStartingBlock.getScriptName().replaceAll("(?i)" + Pattern.quote(textLabel.toLowerCase()), "[" + variableName + "]"));

                                        //print out the found parameters and alternative values
                                        List<String> alternativeTextLabels = new ArrayList<>();
                                        alternativeValueNodeMap.forEach((x, y) -> alternativeTextLabels.add(x));
                                        System.out.printf("Found %d alternative options for the parameter \"%s\": %s", alternativeTextLabels.size(), textLabel, alternativeTextLabels.toString());
                                        PumiceDemonstrationUtil.showSugiliteToast(String.format("Found %d alternative options for the parameter \"%s\": %s", alternativeTextLabels.size(), textLabel, alternativeTextLabels.toString()), Toast.LENGTH_SHORT);

                                        synchronized (lock) {
                                            try {
                                                lock.notify();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });

                                selectAlternativeValueDialog.show();

                                synchronized (lock) {
                                    try {
                                        lock.wait();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }




                            }
                        }
                    } else {
                        continue;
                    }
                }

                //go through all operation blocks to check if * the action parameter * of any clicked entity matches the user utterance e.g., SET_TEXT and READ_OUT
                for (SugiliteOperationBlock operationBlock : getAllOperationBlocks(sugiliteStartingBlock)) {
                    SugiliteOperation operation = operationBlock.getOperation();
                    if (operation instanceof SugiliteSetTextOperation) {
                        String textParam = ((SugiliteSetTextOperation) operation).getParameter0();
                        if (userUtterance.toLowerCase().contains(textParam.toLowerCase())) {
                            //matched
                            int parameterNumber = parameterNumberCounter++;
                            System.out.printf("Found parameter %d: \"%s\" in the utterance was found in the operation %s\n", parameterNumber, textParam, operationBlock.toString());

                            Node editTextNode = null;
                            if (operationBlock.getSugiliteBlockMetaInfo() != null && operationBlock.getSugiliteBlockMetaInfo().getTargetEntity() != null) {
                                editTextNode = operationBlock.getSugiliteBlockMetaInfo().getTargetEntity().getEntityValue();
                            }

                            //construct the Variable object
                            String variableName = String.format("parameter%d", parameterNumber);
                            Variable variableObject = new Variable(Variable.USER_INPUT, variableName);
                            VariableContext variableContext = VariableContext.fromOperationBlockAndAlternativeNode(operationBlock, editTextNode);
                            variableObject.setVariableContext(variableContext);

                            //construct the default value of the variable
                            VariableValue<String> defaultVariableValue = new VariableValue<String>(variableName, textParam);
                            VariableContext defaultVariableValueContext = VariableContext.fromOperationBlockAndItsTargetNode(operationBlock);
                            defaultVariableValue.setVariableValueContext(defaultVariableValueContext);

                            //fill the results back to the SugiliteStartingBlock
                            sugiliteStartingBlock.variableNameDefaultValueMap.put(variableName, defaultVariableValue);
                            sugiliteStartingBlock.variableNameVariableObjectMap.put(variableName, variableObject);
                            //should have an empty set of alternative values
                            sugiliteStartingBlock.variableNameAlternativeValueMap.put(variableName, new HashSet<>());

                            //edit the original parameter in the SugiliteOperation to reflect the new parameters
                            ((SugiliteSetTextOperation) operation).setParameter0("[" + variableName + "]");
                            operationBlock.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(operation, operation.getDataDescriptionQueryIfAvailable()));


                            //replace the occurrence of parameter default values in the script name
                            sugiliteStartingBlock.setScriptName(sugiliteStartingBlock.getScriptName().replaceAll("(?i)" + Pattern.quote(textParam.toLowerCase()), "[" + variableName + "]"));

                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    PumiceDemonstrationUtil.showSugiliteToast(String.format("Found a parameter named %s, whose default value is \"%s\"", variableName, textParam), Toast.LENGTH_SHORT);
                                }
                            });
                        }
                    }
                }
                onGeneralizationReadyCallback.run();
            }
        });
        extractParameterThread.start();
    }

    public static void replaceParametersInOntologyQuery (OntologyQuery ontologyQuery, String parameterDefaultValue, String parameterName) {
        if (ontologyQuery instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery)ontologyQuery;
            if (loq.getObject() != null) {
                for (SugiliteSerializableEntity objectEntity : loq.getObject()) {
                    if (objectEntity.getEntityValue() instanceof String && parameterDefaultValue.equals(objectEntity.getEntityValue())) {
                        objectEntity.setEntityValue("[" + parameterName + "]");
                    }
                }
            }
        }

        if (ontologyQuery instanceof OntologyQueryWithSubQueries) {
            for (OntologyQuery subQuery : ((OntologyQueryWithSubQueries)ontologyQuery).getSubQueries()) {
                replaceParametersInOntologyQuery(subQuery, parameterDefaultValue, parameterName);
            }
        }
    }



    private List<Pair<SugiliteRelation, String>> getAllStringsUsedInTheDataDescriptionQuery (OntologyQuery ontologyQuery) {
        List<Pair<SugiliteRelation, String>> allStringsUsedInTheDataDescriptionQuery = new ArrayList<>();

        if (ontologyQuery instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery)ontologyQuery;
            if (loq.getObject() != null && loq.getR() != null) {
                for (SugiliteSerializableEntity objectEntity : loq.getObject()) {
                    if (objectEntity.getEntityValue() instanceof String) {
                        allStringsUsedInTheDataDescriptionQuery.add(new Pair<>(loq.getR(), (String) objectEntity.getEntityValue()));
                    }
                }
            }
        }

        if (ontologyQuery instanceof OntologyQueryWithSubQueries) {
            for (OntologyQuery subQuery : ((OntologyQueryWithSubQueries)ontologyQuery).getSubQueries()) {
                allStringsUsedInTheDataDescriptionQuery.addAll(getAllStringsUsedInTheDataDescriptionQuery(subQuery));
            }
        }

        return allStringsUsedInTheDataDescriptionQuery;
    }


    /**
     *
     * @param nodeEntity
     * @param uiSnapshot
     * @param relation
     * @param depthLimit
     * @return
     *
     * a Pair with (topParentNode, Map<node, list of text labels on the node>)
     */
    private Pair<SugiliteSerializableEntity<Node>, Map<SugiliteSerializableEntity<Node>, List<String>>> getPossibleValueForParameter(SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot, SugiliteRelation relation, int depthLimit) {
        Map<SugiliteSerializableEntity<Node>, List<String>> alternativeNodeTextLabelsMap = new HashMap<>();
        SugiliteSerializableEntity<Node> currentEntity = nodeEntity;
        String nodeEntityClassName = getUISnapshotRelationValue(nodeEntity, HAS_CLASS_NAME, uiSnapshot);
        SugiliteSerializableEntity<Node> topParentEntity = null;
        if (nodeEntityClassName != null) {
            while (getParentNodeEntity(currentEntity, uiSnapshot) != null) {
                //trying going up in the UI tree
                SugiliteSerializableEntity<Node> parentEntity = getParentNodeEntity(currentEntity, uiSnapshot);
                topParentEntity = parentEntity;
                List<SugiliteSerializableEntity<Node>> immediateChildNodeEntities = getAllChildNodeEntity(new HashSet<>(), parentEntity, uiSnapshot, true, currentEntity);
                boolean foundAtThisLevel = false;
                for (SugiliteSerializableEntity<Node> sibling : immediateChildNodeEntities) {
                    if (sibling.getEntityValue().getBoundsInScreen() != null && nodeEntity.getEntityValue().getBoundsInScreen() != null && sibling.getEntityValue().getBoundsInScreen().equals(nodeEntity.getEntityValue().getBoundsInScreen())) {
                        continue;
                    }
                    String siblingIsClickable = getUISnapshotRelationValue(sibling, IS_CLICKABLE, uiSnapshot);
                    String siblingClassName = getUISnapshotRelationValue(sibling, HAS_CLASS_NAME, uiSnapshot);

                    //TODO: expand this to support relations other than text labels
                    //List<String> siblingTextLabels = getAllTextAndChildTextLabels(sibling, uiSnapshot);
                    List<String> siblingValues = getAllObjectValues(sibling, uiSnapshot, relation);

                    if (siblingIsClickable != null && siblingIsClickable.equals("true") &&
                            siblingClassName != null && siblingClassName.equals(nodeEntityClassName) &&
                            siblingValues != null && siblingValues.size() > 0)  {
                        alternativeNodeTextLabelsMap.put(sibling, siblingValues);
                        foundAtThisLevel = true;
                    }
                }
                if (foundAtThisLevel) {
                    depthLimit --;
                } if (depthLimit <= 0) {
                    break;
                } else {
                    currentEntity = parentEntity;
                }
            }
        }
        return new Pair<SugiliteSerializableEntity<Node>, Map<SugiliteSerializableEntity<Node>, List<String>>>(topParentEntity, alternativeNodeTextLabelsMap);
    }

    private String getUISnapshotRelationValue(SugiliteSerializableEntity<Node> subject, SugiliteRelation relation, SerializableUISnapshot uiSnapshot) {
        Set<SugiliteSerializableTriple> triples = uiSnapshot.getSubjectTriplesMap().get("@" + String.valueOf(subject.getEntityId()));
        for (SugiliteSerializableTriple triple : triples) {
            if (triple.getPredicateStringValue().equals(relation.getRelationName())){
                return triple.getObjectStringValue();
            }
        }
        return null;
    }


    private List<SugiliteSerializableEntity<Node>> getAllChildNodeEntity(Set<SugiliteSerializableEntity<Node>> exploredSet, SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot, boolean getAllDescendantRecursively, @Nullable SugiliteSerializableEntity<Node> currentEntityToAvoid) {
        List<SugiliteSerializableEntity<Node>> childNodes = new ArrayList<>();
        Set<SugiliteSerializableTriple> triples = uiSnapshot.getSubjectTriplesMap().get("@" + String.valueOf(nodeEntity.getEntityId()));
        exploredSet.add(nodeEntity);

        for (SugiliteSerializableTriple triple : triples) {
            if (triple.getPredicateStringValue().equals(HAS_CHILD.getRelationName())){
                String targetEntityId = triple.getObjectStringValue();
                if (currentEntityToAvoid.getEntityId().equals(targetEntityId)) {
                    continue;
                }
                if (uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().containsKey(targetEntityId)){
                    SugiliteSerializableEntity<Node> childNode = uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().get(targetEntityId);
                    childNodes.add(childNode);
                    if (getAllDescendantRecursively) {
                        if (!exploredSet.contains(childNode)) {
                            childNodes.addAll(getAllChildNodeEntity(exploredSet, childNode, uiSnapshot, true, currentEntityToAvoid));
                        }
                    }
                }
            }
        }
        return childNodes;
    }

    private SugiliteSerializableEntity<Node> getParentNodeEntity(SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot) {
        Set<SugiliteSerializableTriple> triples = uiSnapshot.getSubjectTriplesMap().get("@" + String.valueOf(nodeEntity.getEntityId()));
        for (SugiliteSerializableTriple triple : triples) {
            if (triple.getPredicateStringValue().equals(HAS_PARENT.getRelationName())){
                String targetEntityId = triple.getObjectStringValue();
                if (uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().containsKey(targetEntityId)){
                    return uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().get(targetEntityId);
                }
            }
        }
        return null;
    }


    private List<String> getAllObjectValues(SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot, SugiliteRelation sugiliteRelation) {
        List<String> result = new ArrayList<>();
        Set<SugiliteSerializableTriple> triples = uiSnapshot.getSubjectTriplesMap().get("@" + String.valueOf(nodeEntity.getEntityId()));
        if (triples != null) {
            for (SugiliteSerializableTriple triple : triples) {
                if (triple.getPredicateStringValue().equals(sugiliteRelation.getRelationName())) {
                    String objectStringValue = triple.getObjectStringValue();
                    if (objectStringValue != null) {
                        result.add(objectStringValue);
                    }
                }
            }
        }
        return result;
    }

    /**
     * get all text and child text labels for a nodeEntity in a uiSnapshot
     * @param nodeEntity
     * @param uiSnapshot
     * @return
     */
    private List<String> getAllTextAndChildTextLabels(SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot) {
        List<String> allTextAndChildTextLabels = new ArrayList<>();
        if (nodeEntity != null && uiSnapshot != null) {
            //add the text label of the current node
            if (nodeEntity.getEntityValue() != null) {
                Node node = nodeEntity.getEntityValue();
                if (node.getText() != null) {
                    allTextAndChildTextLabels.add(node.getText());
                }
            }
            //add the text labels of its children
            Set<SugiliteSerializableTriple> triples = uiSnapshot.getSubjectTriplesMap().get("@" + String.valueOf(nodeEntity.getEntityId()));
            if (triples != null) {
                for (SugiliteSerializableTriple triple : triples) {
                    if (triple.getPredicateStringValue().equals(HAS_CHILD.getRelationName())) {
                        String targetEntityId = triple.getObjectStringValue();
                        if (uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().containsKey(targetEntityId)) {
                            allTextAndChildTextLabels.addAll(getAllTextAndChildTextLabels(uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().get(targetEntityId), uiSnapshot));
                        }
                    }
                }
            }
        }
        return allTextAndChildTextLabels;
    }


    /**
     * get operation blocks in all the subsequent blocks of a block
     * @param block
     * @return
     */
    public static List<SugiliteOperationBlock> getAllOperationBlocks(SugiliteBlock block) {
        List<SugiliteOperationBlock> operationBlocks = new ArrayList<>();
        if (block == null) {
            return operationBlocks;
        }
        if (block instanceof SugiliteOperationBlock) {
            operationBlocks.add((SugiliteOperationBlock)block);
        }
        if (block instanceof SugiliteConditionBlock) {
            operationBlocks.addAll(getAllOperationBlocks(((SugiliteConditionBlock) block).getThenBlock()));
            operationBlocks.addAll(getAllOperationBlocks(((SugiliteConditionBlock) block).getElseBlock()));
        }
        operationBlocks.addAll(getAllOperationBlocks(block.getNextBlock()));
        return operationBlocks;
    }


    private class SelectAlternativeValueDialog {
        private Context context;
        private Map<String, SugiliteSerializableEntity<Node>> alternativeValueNodeMap;
        private Runnable afterRunnable;

        private AlertDialog dialog;
        public SelectAlternativeValueDialog (Context context, Map<String, SugiliteSerializableEntity<Node>> alternativeValueNodeMap, Runnable afterRunnable) {
            this.context = context;
            this.alternativeValueNodeMap = alternativeValueNodeMap;
            this.afterRunnable = afterRunnable;


        }

        void initDialog () {
            String[] fields = new String[alternativeValueNodeMap.size()];
            int i = 0;
            for(String alternativeValueText : alternativeValueNodeMap.keySet()) {
                fields[i++] = alternativeValueText;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LinearLayout mainLayout = new LinearLayout(context);
            mainLayout.setPadding(15, 15, 15, 15);
            mainLayout.setOrientation(LinearLayout.VERTICAL);

            TextView titleTextView = new TextView(context);
            titleTextView.setText("Select alternative values to use");
            mainLayout.addView(titleTextView);

            ListView checkboxListView = new ListView(context);
            checkboxListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            checkboxListView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_multiple_choice, fields));
            mainLayout.addView(checkboxListView);

            builder.setView(mainLayout);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Set<String> alternativeValuesToKeep = new HashSet<>();
                    Set<String> alternativeValuesToRemove = new HashSet<>();
                    SparseBooleanArray checkedItemPositions = checkboxListView.getCheckedItemPositions();
                    for (int i = 0; i < checkedItemPositions.size(); i++) {
                        int key = checkedItemPositions.keyAt(i);
                        Boolean value = checkedItemPositions.get(key);
                        if (value) {
                            alternativeValuesToKeep.add(fields[key]);
                        }
                    }
                    for (String alternativeValue : alternativeValueNodeMap.keySet()) {
                        if (! alternativeValuesToKeep.contains(alternativeValue)) {
                            alternativeValuesToRemove.add(alternativeValue);
                        }
                    }


                    for (String alternativeValueToRemove : alternativeValuesToRemove) {
                        alternativeValueNodeMap.remove(alternativeValueToRemove);
                    }
                    dialog.dismiss();
                    afterRunnable.run();
                }
            });

            dialog = builder.create();
        }

        void show() {
            SugiliteData.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initDialog();
                    if (dialog != null) {
                        if (dialog.getWindow() != null) {
                            dialog.getWindow().setType(OVERLAY_TYPE);
                        }
                        dialog.show();
                    }
                }
            });
        }
    }
}
