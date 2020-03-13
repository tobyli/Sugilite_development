package edu.cmu.hcii.sugilite.sovite.study;

import java.io.Serializable;
import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;

/**
 * @author toby
 * @date 3/9/20
 * @time 10:41 PM
 */
public class SoviteStudyDumpPacket implements Serializable {
    private PumiceKnowledgeManager pumiceKnowledgeManager;
    private List<SugiliteStartingBlock> scripts;
    private String name;

    public SoviteStudyDumpPacket(PumiceKnowledgeManager pumiceKnowledgeManager, List<SugiliteStartingBlock> scripts, String name) {
        this.pumiceKnowledgeManager = pumiceKnowledgeManager;
        this.scripts = scripts;
        this.name = name;
    }

    public List<SugiliteStartingBlock> getScripts() {
        return scripts;
    }

    public PumiceKnowledgeManager getPumiceKnowledgeManager() {
        return pumiceKnowledgeManager;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}