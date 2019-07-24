package edu.cmu.hcii.sugilite.model;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;

public class OperationBlockDescriptionRegenerator {
    public static void regenerateBlockDescription(SugiliteBlock block, OntologyDescriptionGenerator ontologyDescriptionGenerator) {
        if (block instanceof SugiliteOperationBlock) {
            SugiliteOperationBlock sob = (SugiliteOperationBlock) block;
            block.setDescription(ontologyDescriptionGenerator.getDescriptionForOperation(sob.getOperation(), sob.getOperation().getDataDescriptionQueryIfAvailable()));
        }
    }

    public static void regenerateScriptDescriptions(SugiliteStartingBlock script, OntologyDescriptionGenerator ontologyDescriptionGenerator) {
        for (SugiliteBlock block : script.getFollowingBlocks()) {
            regenerateBlockDescription(block, ontologyDescriptionGenerator);
        }
    }
}
