package edu.cmu.hcii.sugilite.verbal_instruction_demo.study;

import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;

/**
 * @author toby
 * @date 1/20/18
 * @time 7:41 PM
 */
public class SugiliteStudyPacket {
    private SerializableUISnapshot uiSnapshot;
    private SugiliteSerializableEntity sourceNodeEntity;
    private String verbalInstruction;
    private String actionType;

    public static final String TYPE_CLICK = "CLICK";

    //TODO: support additional event types (e.g. text entry)

    public SugiliteStudyPacket(SerializableUISnapshot uiSnapshot, SugiliteSerializableEntity sourceNodeEntity, String verbalInstruction, String actionType, String query){
        this.uiSnapshot = uiSnapshot;
        this.sourceNodeEntity = sourceNodeEntity;
        this.verbalInstruction = verbalInstruction;
        this.actionType = actionType;
    }

    public SerializableUISnapshot getUiSnapshot() {
        return uiSnapshot;
    }

    public SugiliteSerializableEntity getSourceNodeEntity() {
        return sourceNodeEntity;
    }

    public String getVerbalInstruction() {
        return verbalInstruction;
    }

    public void setSourceNodeEntity(SugiliteSerializableEntity sourceNodeEntity) {
        this.sourceNodeEntity = sourceNodeEntity;
    }

    public void setUiSnapshot(SerializableUISnapshot uiSnapshot) {
        this.uiSnapshot = uiSnapshot;
    }

    public void setVerbalInstruction(String verbalInstruction) {
        this.verbalInstruction = verbalInstruction;
    }
}
