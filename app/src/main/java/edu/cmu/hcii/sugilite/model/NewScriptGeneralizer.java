package edu.cmu.hcii.sugilite.model;

import android.app.Activity;
import android.content.Context;
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
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;

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
    Activity context;
    public NewScriptGeneralizer(Activity context) {
        this.context = context;
    }

    /**
     * extract parameters and their possible values from a script based on the input user utterance
     * @param sugiliteStartingBlock
     * @param userUtterance
     */
    public void extractParameters (SugiliteStartingBlock sugiliteStartingBlock, String userUtterance) {
        //go through all operation blocks to check if any clicked entity matches the user utterance
        List<SugiliteOperationBlock> allOperationBlocksInTheScript = getAllOperationBlocks(sugiliteStartingBlock);
        if (sugiliteStartingBlock.variableNameDefaultValueMap == null) {
            sugiliteStartingBlock.variableNameDefaultValueMap = new HashMap<>();
        }

        if (sugiliteStartingBlock.variableNameAlternativeValueMap == null) {
            sugiliteStartingBlock.variableNameAlternativeValueMap = new HashMap<>();
        }
        sugiliteStartingBlock.variableNameDefaultValueMap.clear();
        sugiliteStartingBlock.variableNameAlternativeValueMap.clear();


        for (SugiliteOperationBlock operationBlock : allOperationBlocksInTheScript) {

            SerializableOntologyQuery ontologyQuery = operationBlock.getOperation().getDataDescriptionQueryIfAvailable();
            if ((!operationBlock.getOperation().containsDataDescriptionQuery()) || ontologyQuery == null) {
                // skip operation blocks without a data description query
                continue;
            }

            List<Pair<SugiliteRelation, String>> allStringsUsedInTheDataDescriptionQuery = new ArrayList<>();




            SugiliteBlockMetaInfo blockMetaInfo = operationBlock.getSugiliteBlockMetaInfo();

            if (blockMetaInfo.getUiSnapshot() != null && blockMetaInfo.getTargetEntity() != null) {
                List<String> allTextAndChildTextLabels = getAllTextAndChildTextLabels(blockMetaInfo.getTargetEntity(), blockMetaInfo.getUiSnapshot());
                for (String textLabel : allTextAndChildTextLabels) {
                    //TODO: support more complex matching method than exact string matching
                    if (userUtterance.contains(textLabel.toLowerCase())) {
                        //matched
                        System.out.printf("Found parameter: \"%s\" in the utterance was found in the operation %s\n", textLabel, operationBlock.toString());

                        //TODO: extract possible values from uiSnapshot
                        Map<SugiliteSerializableEntity<Node>, List<String>> alternativeNodeTextLabelsMap = getPossibleValueForParameter(blockMetaInfo.getTargetEntity(), blockMetaInfo.getUiSnapshot());


                        //construct the Variable object
                        String variableName = textLabel.toLowerCase();
                        Variable variable = new Variable(Variable.USER_INPUT, variableName);
                        Set<String> alternativeValue = new HashSet<>();
                        alternativeNodeTextLabelsMap.forEach((x, y) -> alternativeValue.addAll(y));


                        //fill the results back to the SugiliteStartingBlock
                        sugiliteStartingBlock.variableNameDefaultValueMap.put(variableName, variable);
                        sugiliteStartingBlock.variableNameAlternativeValueMap.put(variableName, alternativeValue);


                        //print out the found parameters and alternative values
                        List<String> alternativeTextLabels = new ArrayList<>();
                        alternativeNodeTextLabelsMap.forEach((x, y) -> alternativeTextLabels.add(y.toString()));
                        System.out.printf("Found %d alternative options for the parameter \"%s\": %s", alternativeNodeTextLabelsMap.size(), textLabel, alternativeTextLabels.toString());
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, String.format("Found %d alternative options for the parameter \"%s\": %s", alternativeNodeTextLabelsMap.size(), textLabel, alternativeTextLabels.toString()), Toast.LENGTH_SHORT).show();
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
    private Map<SugiliteSerializableEntity<Node>, List<String>> getPossibleValueForParameter(SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot) {
        Map<SugiliteSerializableEntity<Node>, List<String>> alternativeNodeTextLabelsMap = new HashMap<>();
        SugiliteSerializableEntity<Node> currentEntity = nodeEntity;
        String nodeEntityClassName = getUISnapshotRelationValue(nodeEntity, HAS_CLASS_NAME, uiSnapshot);
        if (nodeEntityClassName != null) {
            while (getParentNodeEntity(currentEntity, uiSnapshot) != null) {
                //trying going up in the UI tree
                SugiliteSerializableEntity<Node> parentEntity = getParentNodeEntity(currentEntity, uiSnapshot);
                List<SugiliteSerializableEntity<Node>> immediateChildNodeEntities = getAllChildNodeEntity(parentEntity, uiSnapshot, true, currentEntity);
                for (SugiliteSerializableEntity<Node> sibling : immediateChildNodeEntities) {
                    String siblingIsClickable = getUISnapshotRelationValue(sibling, IS_CLICKABLE, uiSnapshot);
                    String siblingClassName = getUISnapshotRelationValue(sibling, HAS_CLASS_NAME, uiSnapshot);
                    List<String> siblingTextLabels = getAllTextAndChildTextLabels(sibling, uiSnapshot);
                    if (siblingIsClickable != null && siblingIsClickable.equals("true") &&
                            siblingClassName != null && siblingClassName.equals(nodeEntityClassName) &&
                            siblingTextLabels != null && siblingTextLabels.size() > 0)  {
                        alternativeNodeTextLabelsMap.put(sibling, siblingTextLabels);
                    }
                }
                if (alternativeNodeTextLabelsMap.size() > 0) {
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
            for (SugiliteSerializableTriple triple : triples) {
                if (triple.getPredicateStringValue().equals(HAS_CHILD.getRelationName())){
                    String targetEntityId = triple.getObjectStringValue();
                    if (uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().containsKey(targetEntityId)){
                        allTextAndChildTextLabels.addAll(getAllTextAndChildTextLabels(uiSnapshot.getSugiliteEntityIdSugiliteEntityMap().get(targetEntityId), uiSnapshot));
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
