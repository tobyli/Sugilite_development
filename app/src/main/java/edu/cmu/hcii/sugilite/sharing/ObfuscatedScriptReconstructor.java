package edu.cmu.hcii.sugilite.sharing;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 11/7/19
 * @time 2:42 PM
 */
public class ObfuscatedScriptReconstructor {
    private SugiliteStartingBlock scriptInProcess;
    private String originalScriptName;
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteBlockBuildingHelper blockBuildingHelper;

    public ObfuscatedScriptReconstructor (Context context, SugiliteData sugiliteData) {
        this.blockBuildingHelper = new SugiliteBlockBuildingHelper(context, sugiliteData);
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }
    }

    /**
     * replace the HashedStringLeafOntologyQuery in blockToMatch in scriptInProcess with a newly generated Ontology query from matchedNode
     * @param blockToMatch
     * @param matchedNode
     */
    public void replaceBlockInScript(SugiliteOperationBlock blockToMatch, SugiliteEntity<Node> matchedNode, UISnapshot uiSnapshot) {
        if (scriptInProcess == null) {
            return;
        }
        SugiliteOperation operation = blockToMatch.getOperation();
        OntologyQuery originalOntologyQuery = operation.getDataDescriptionQueryIfAvailable();

        if (originalOntologyQuery == null) {
            Log.e(ObfuscatedScriptReconstructor.class.getName(), "null original ontology query! abort.");
            return;
        }

        if (!SugiliteBlockBuildingHelper.checkIfOntologyQueryContainsHashedQuery(originalOntologyQuery)) {
            //contains no HashedStringLeafOntologyQuery -> nothing to replace
            return;
        }

        //generate the new block
        List<Pair<OntologyQuery, Double>> queryScoreList = SugiliteBlockBuildingHelper.newGenerateDefaultQueries(uiSnapshot, matchedNode);
        SugiliteOperationBlock newBlock = blockBuildingHelper.getUnaryOperationBlockWithOntologyQueryFromQuery(queryScoreList.get(0).first, operation.getOperationType(), new SugiliteAvailableFeaturePack(matchedNode, uiSnapshot, null), SugiliteBlockBuildingHelper.getFirstNonTextQuery(queryScoreList));

        //replace the block
        replaceBlockInScript(scriptInProcess, blockToMatch, newBlock);
        scriptInProcess.setScriptName("RECONSTRUCTED: " + originalScriptName);

        //save the block
        try {
            sugiliteScriptDao.save(scriptInProcess);
            sugiliteScriptDao.commitSave(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void replaceBlockInScript(SugiliteBlock blockToStart, SugiliteOperationBlock blockToMatch, SugiliteOperationBlock newBlock) {
        if (blockToMatch == null) {
            return;
        }
        if (blockToStart instanceof SugiliteOperationBlock) {
            if (blockToStart.equals(blockToMatch)) {
                //matched
                if (blockToMatch.getPreviousBlock() != null) {
                    blockToMatch.getPreviousBlock().setNextBlock(newBlock);
                }

                if (blockToMatch.getNextBlock() != null) {
                    blockToMatch.getNextBlock().setPreviousBlock(newBlock);
                }

                newBlock.setParentBlock(blockToMatch.getParentBlock());
                newBlock.setPreviousBlock(blockToMatch.getPreviousBlock());
                newBlock.setNextBlock(blockToMatch.getNextBlock());
            }
            replaceBlockInScript(blockToStart.getNextBlock(), blockToMatch, newBlock);
        } else if (blockToStart instanceof SugiliteStartingBlock) {
            replaceBlockInScript(blockToStart.getNextBlock(), blockToMatch, newBlock);
        } else if (blockToStart instanceof SugiliteSpecialOperationBlock) {
            replaceBlockInScript(blockToStart.getNextBlock(), blockToMatch, newBlock);
        } else if (blockToStart instanceof SugiliteConditionBlock) {
            replaceBlockInScript(((SugiliteConditionBlock) blockToStart).getThenBlock(), blockToMatch, newBlock);
            replaceBlockInScript(((SugiliteConditionBlock) blockToStart).getElseBlock(), blockToMatch, newBlock);
            replaceBlockInScript(blockToStart.getNextBlock(), blockToMatch, newBlock);
        }
    }


    public void setScriptInProcess(SugiliteStartingBlock scriptInProcess) {
        this.scriptInProcess = scriptInProcess;
        if (scriptInProcess != null) {
            originalScriptName = scriptInProcess.getScriptName();
        } else {
            originalScriptName = null;
        }
    }

    public SugiliteStartingBlock getScriptInProcess() {
        return scriptInProcess;
    }
}
