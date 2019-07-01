package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

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

    private SugiliteBlock parentBlock;
    //the UI snapshot from the screen at the time of the block recording
    private SerializableUISnapshot uiSnapshot;
    private SugiliteSerializableEntity targetEntity;


    public SugiliteBlockMetaInfo(SugiliteBlock parentBlock, SerializableUISnapshot uiSnapshot, SugiliteSerializableEntity targetEntity) {
        this.parentBlock = parentBlock;
        this.uiSnapshot = uiSnapshot;
        this.targetEntity = targetEntity;
    }

    public SerializableUISnapshot getUiSnapshot() {
        return uiSnapshot;
    }

    public SugiliteBlock getParentBlock() {
        return parentBlock;
    }

    public SugiliteSerializableEntity getTargetEntity() {
        return targetEntity;
    }
}
