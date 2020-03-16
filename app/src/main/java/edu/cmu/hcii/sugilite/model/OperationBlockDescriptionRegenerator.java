package edu.cmu.hcii.sugilite.model;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;

public class OperationBlockDescriptionRegenerator {
    public static void regenerateBlockDescription(SugiliteBlock block, OntologyDescriptionGenerator ontologyDescriptionGenerator) {
        if (block instanceof SugiliteOperationBlock) {
            SugiliteOperationBlock sob = (SugiliteOperationBlock) block;
            if (sob.getOperation().getDataDescriptionQueryIfAvailable() != null) {
                block.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperation(sob.getOperation(), sob.getOperation().getDataDescriptionQueryIfAvailable()));
            } else {
                block.setDescription(ontologyDescriptionGenerator.getSpannedDescriptionForOperationWithoutOntologyQuery(sob.getOperation()));
            }
        } else if (block.getPlainDescription() != null) {
           block.setDescription(block.getPlainDescription());
        }
    }

    public static void regenerateScriptDescriptions(SugiliteStartingBlock script, OntologyDescriptionGenerator ontologyDescriptionGenerator) {
        regenerateBlockDescription(script, ontologyDescriptionGenerator);
        for (SugiliteBlock block : script.getFollowingBlocks()) {
            regenerateBlockDescription(block, ontologyDescriptionGenerator);
        }
    }
}
