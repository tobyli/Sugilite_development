package edu.cmu.hcii.sugilite.model;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlockMetaInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ontology.*;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;

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
    public void extractParameters (SugiliteStartingBlock sugiliteStartingBlock, String userUtterance) {

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

            if (blockMetaInfo.getUiSnapshot() != null && blockMetaInfo.getTargetEntity() != null) {
                for (Pair<SugiliteRelation, String> sugiliteRelationTextLabelPair : allStringsUsedInTheDataDescriptionQuery) {
                    //TODO: support more complex matching method than exact string matching

                    SugiliteRelation relation = sugiliteRelationTextLabelPair.first;
                    String textLabel = sugiliteRelationTextLabelPair.second;

                    if (userUtterance.contains(textLabel.toLowerCase())) {
                        //matched
                        System.out.printf("Found parameter: \"%s\" in the utterance was found in the operation %s\n", textLabel, operationBlock.toString());

                        //extract possible values from uiSnapshot
                        Map<SugiliteSerializableEntity<Node>, List<String>> alternativeNodeTextLabelsMap = getPossibleValueForParameter(blockMetaInfo.getTargetEntity(), blockMetaInfo.getUiSnapshot(), relation, depthLimit );

                        //construct the Variable object
                        String variableName = textLabel;
                        Variable variable = new StringVariable(variableName, variableName);
                        Set<String> alternativeValue = new HashSet<>();
                        alternativeNodeTextLabelsMap.forEach((x, y) -> alternativeValue.addAll(y));


                        //fill the results back to the SugiliteStartingBlock
                        sugiliteStartingBlock.variableNameDefaultValueMap.put(variableName, variable);
                        sugiliteStartingBlock.variableNameAlternativeValueMap.put(variableName, alternativeValue);

                        //edit the original data description query to reflect the new parameters
                        replaceParametersInOntologyQuery (ontologyQuery, variableName);
                        operationBlock.setDescription(ontologyDescriptionGenerator.getDescriptionForOperation(operationBlock.getOperation(), operationBlock.getOperation().getDataDescriptionQueryIfAvailable()));


                        //print out the found parameters and alternative values
                        List<String> alternativeTextLabels = new ArrayList<>();
                        alternativeNodeTextLabelsMap.forEach((x, y) -> alternativeTextLabels.add(y.toString()));
                        System.out.printf("Found %d alternative options for the parameter \"%s\": %s", alternativeNodeTextLabelsMap.size(), textLabel, alternativeTextLabels.toString());
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                PumiceDemonstrationUtil.showSugiliteToast(String.format("Found %d alternative options for the parameter \"%s\": %s", alternativeNodeTextLabelsMap.size(), textLabel, alternativeTextLabels.toString()), Toast.LENGTH_SHORT);
                            }
                        });
                        //TODO: add the parameter back to SugiliteStartingBlock
                    }
                }
            } else {
                continue;
            }
        }
    }

    private void replaceParametersInOntologyQuery (OntologyQuery ontologyQuery, String parameter) {
        if (ontologyQuery instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery)ontologyQuery;
            if (loq.getObject() != null) {
                for (SugiliteSerializableEntity objectEntity : loq.getObject()) {
                    if (objectEntity.getEntityValue() instanceof String && parameter.equals(objectEntity.getEntityValue())) {
                        objectEntity.setEntityValue("@" + objectEntity.getEntityValue());
                    }
                }
            }
        }

        if (ontologyQuery instanceof OntologyQueryWithSubQueries) {
            for (OntologyQuery subQuery : ((OntologyQueryWithSubQueries)ontologyQuery).getSubQueries()) {
                replaceParametersInOntologyQuery(subQuery, parameter);
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


    private Map<SugiliteSerializableEntity<Node>, List<String>> getPossibleValueForParameter(SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot, SugiliteRelation relation, int depthLimit) {
        Map<SugiliteSerializableEntity<Node>, List<String>> alternativeNodeTextLabelsMap = new HashMap<>();
        SugiliteSerializableEntity<Node> currentEntity = nodeEntity;
        String nodeEntityClassName = getUISnapshotRelationValue(nodeEntity, HAS_CLASS_NAME, uiSnapshot);
        if (nodeEntityClassName != null) {
            while (getParentNodeEntity(currentEntity, uiSnapshot) != null) {
                //trying going up in the UI tree
                SugiliteSerializableEntity<Node> parentEntity = getParentNodeEntity(currentEntity, uiSnapshot);
                List<SugiliteSerializableEntity<Node>> immediateChildNodeEntities = getAllChildNodeEntity(parentEntity, uiSnapshot, true, currentEntity);
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
        return alternativeNodeTextLabelsMap;
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


    private List<SugiliteSerializableEntity<Node>> getAllChildNodeEntity(SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot, boolean getAllDescendantRecursively, @Nullable SugiliteSerializableEntity<Node> currentEntityToAvoid) {
        List<SugiliteSerializableEntity<Node>> childNodes = new ArrayList<>();
        Set<SugiliteSerializableTriple> triples = uiSnapshot.getSubjectTriplesMap().get("@" + String.valueOf(nodeEntity.getEntityId()));
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
                        childNodes.addAll(getAllChildNodeEntity(childNode, uiSnapshot, true, currentEntityToAvoid));
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
    private List<SugiliteOperationBlock> getAllOperationBlocks(SugiliteBlock block) {
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
}
