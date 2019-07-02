package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;

/**
 * @author toby
 * @date 6/19/19
 * @time 1:20 PM
 */

//this class can be used for storing meta information about a block
public class SugiliteBlockMetaInfo implements Serializable {

    //link to the parent block that the SugiliteBlockMetaInfo belongs to
    private transient SugiliteBlock parentBlock;
    private int parentBlockId;
    //the UI snapshot from the screen at the time of the block recording
    private SerializableUISnapshot uiSnapshot;

    //the targetEntity of the operation in the uiSnapshot
    private SugiliteSerializableEntity<Node> targetEntity;


    public SugiliteBlockMetaInfo(SugiliteBlock parentBlock, SerializableUISnapshot uiSnapshot, SugiliteSerializableEntity<Node> targetEntity) {
        this.parentBlock = parentBlock;
        this.parentBlockId = parentBlock.getBlockId();
        this.uiSnapshot = uiSnapshot;
        this.targetEntity = targetEntity;
    }

    public SerializableUISnapshot getUiSnapshot() {
        return uiSnapshot;
    }

    public SugiliteBlock getParentBlock() {
        return parentBlock;
    }

    public SugiliteSerializableEntity<Node> getTargetEntity() {
        return targetEntity;
    }
}
