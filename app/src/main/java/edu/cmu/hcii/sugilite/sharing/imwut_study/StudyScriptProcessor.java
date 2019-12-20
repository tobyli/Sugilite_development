package edu.cmu.hcii.sugilite.sharing.imwut_study;

import android.content.Context;

import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableTriple;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.sharing.StringAlternativeGenerator;
import edu.cmu.hcii.sugilite.sharing.SugiliteScriptSharingHTTPQueryManager;
import edu.cmu.hcii.sugilite.sharing.SugiliteSharingScriptPreparer;
import edu.cmu.hcii.sugilite.sharing.model.StringInContext;

/**
 * @author toby
 * @date 11/10/19
 * @time 2:29 PM
 */
public class StudyScriptProcessor {
    SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;
    public StudyScriptProcessor(Context context) {
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(context);
    }

    public StudyResultForScript process (SugiliteStartingBlock script) {
        StudyResultForScript result = new StudyResultForScript();
        result.scriptName = script.getScriptName();

        //process each operation in the script
        processSubsequentBlocks(result, script);


        //send out information entries for determining privacy status
        try {
            result.filterdInformationEntriesInDataDescription = SugiliteSharingScriptPreparer.getReplacementsFromStringInContextSet(result.informationEntriesInDataDescription, sugiliteScriptSharingHTTPQueryManager);
            result.filterdInformationEntriesInUISnapshots = SugiliteSharingScriptPreparer.getReplacementsFromStringInContextSet(result.informationEntriesInUISnapshots, sugiliteScriptSharingHTTPQueryManager);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    //recursively process the blocks
    private void processSubsequentBlocks (StudyResultForScript result, SugiliteBlock block) {
        if (block == null) {
            return;
        }
        if (block instanceof SugiliteOperationBlock) {
            processOperationBlock(result, (SugiliteOperationBlock) block);
        }
        if (block instanceof SugiliteConditionBlock) {
            processSubsequentBlocks(result, ((SugiliteConditionBlock) block).getThenBlock());
            processSubsequentBlocks(result, ((SugiliteConditionBlock) block).getElseBlock());
        }
        processSubsequentBlocks(result, block.getNextBlock());
    }

    //process a SugiliteOperationBlock
    private void processOperationBlock (StudyResultForScript result, SugiliteOperationBlock block) {
        if (block.getOperation() == null) {
            return;
        }
        result.operations.add(block.getOperation());
        OntologyQuery ontologyQuery = block.getOperation().getDataDescriptionQueryIfAvailable();
        String packageName = null;
        String activityName = null;
        SerializableUISnapshot uiSnapshot = null;
        if (block.getSugiliteBlockMetaInfo() != null && block.getSugiliteBlockMetaInfo().getUiSnapshot() != null) {
            uiSnapshot = block.getSugiliteBlockMetaInfo().getUiSnapshot();
        }
        if (uiSnapshot != null) {
            packageName = uiSnapshot.getPackageName();
            activityName = uiSnapshot.getActivityName();
        }

        //add strings found in data description queries
        if (ontologyQuery != null) {
            Set<String> allDataDescriptionStrings = SugiliteBlockBuildingHelper.getAllResultsOfRelationTypesInOntologyQuery(ontologyQuery, SugiliteRelation.HAS_TEXT, SugiliteRelation.HAS_CHILD_TEXT, SugiliteRelation.HAS_CONTENT_DESCRIPTION);
            for (String s : allDataDescriptionStrings) {
                result.informationEntriesInDataDescription.add(new StringInContext(activityName, packageName, s));
            }
        }

        //add strings found in UI snapshots
        if (uiSnapshot != null && uiSnapshot.getTriples() != null) {
            for (SugiliteSerializableTriple triple : uiSnapshot.getTriples()) {
                if (triple.getPredicateStringValue() != null && (triple.getPredicateStringValue().equals(SugiliteRelation.HAS_TEXT.getRelationName()) || triple.getPredicateStringValue().equals(SugiliteRelation.HAS_CONTENT_DESCRIPTION.getRelationName()))) {
                    StringInContext entry = new StringInContext(activityName, packageName, triple.getObjectStringValue());
                    if (! result.informationEntriesInDataDescription.contains(entry)) {
                        result.informationEntriesInUISnapshots.add(entry);
                    }
                }
            }
        }

    }
}
