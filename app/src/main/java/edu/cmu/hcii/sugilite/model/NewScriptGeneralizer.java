package edu.cmu.hcii.sugilite.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlockMetaInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;

import static edu.cmu.hcii.sugilite.ontology.SugiliteRelation.HAS_CHILD;
import static edu.cmu.hcii.sugilite.ontology.SugiliteRelation.HAS_CHILD_TEXT;

/**
 * @author toby
 * @date 7/1/19
 * @time 4:29 PM
 */
public class NewScriptGeneralizer {

    public NewScriptGeneralizer() {

    }

    /**
     * extract parameters and their possible values from a script based on the input user utterance
     * @param sugiliteStartingBlock
     * @param userUtterance
     */
    public void extractParameters (SugiliteStartingBlock sugiliteStartingBlock, String userUtterance) {
        //go through all operation blocks to check if any clicked entity matches the user utterance
        List<SugiliteOperationBlock> allOperationBlocksInTheScript = getAllOperationBlocks(sugiliteStartingBlock);
        for (SugiliteOperationBlock operationBlock : allOperationBlocksInTheScript) {
            SugiliteBlockMetaInfo blockMetaInfo = operationBlock.getSugiliteBlockMetaInfo();

            if (blockMetaInfo.getUiSnapshot() != null && blockMetaInfo.getTargetEntity() != null) {
                Set<String> allTextAndChildTextLabels = getAllTextAndChildTextLabels(blockMetaInfo.getTargetEntity(), blockMetaInfo.getUiSnapshot());
                for (String textLabel : allTextAndChildTextLabels) {
                    //TODO: support more complex matching method than exact string matching
                    if (userUtterance.contains(textLabel.toLowerCase())) {
                        //matched
                        System.out.printf("Found parameter: %s in the utterance was found in the operation %s", textLabel, operationBlock.toString());
                        //TODO: extract possible values from uiSnapshot

                        //TODO: add the parameter back to SugiliteStartingBlock
                    }
                }
            } else {
                continue;
            }
        }
    }

    /**
     * get all text and child text labels for a nodeEntity in a uiSnapshot
     * @param nodeEntity
     * @param uiSnapshot
     * @return
     */
    private Set<String> getAllTextAndChildTextLabels(SugiliteSerializableEntity<Node> nodeEntity, SerializableUISnapshot uiSnapshot) {
        Set<String> allTextAndChildTextLabels = new HashSet<>();
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
