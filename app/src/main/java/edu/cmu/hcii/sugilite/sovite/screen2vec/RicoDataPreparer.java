package edu.cmu.hcii.sugilite.sovite.screen2vec;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlockMetaInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;

/**
 * @author toby
 * @date 8/18/20
 * @time 10:02 PM
 */
public class RicoDataPreparer {

    private File studyDumpPackageDir;
    private SugiliteScriptParser sugiliteScriptParser;
    private Gson gson;

    public RicoDataPreparer(Context context) {
        this.sugiliteScriptParser = new SugiliteScriptParser();
        this.gson = new Gson();
        try {
            File rootDataDir = new File("/mnt/sdcard/");
            studyDumpPackageDir = new File(rootDataDir.getPath() + "/rico_dump");
            if (!studyDumpPackageDir.exists() || !studyDumpPackageDir.isDirectory())
                studyDumpPackageDir.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
    }


    public File exportRicoDataForScript (SugiliteStartingBlock script) throws IOException {
        //1. create the target directory
        File targetDirectory = new File(studyDumpPackageDir.getPath() + "/" + PumiceDemonstrationUtil.removeScriptExtension(script.getScriptName()));
        if (targetDirectory.exists()) {
            if (targetDirectory.isDirectory()) {
                String[] children = targetDirectory.list();
                for (int i = 0; i < children.length; i++)
                {
                    new File(targetDirectory, children[i]).delete();
                }
            }
            targetDirectory.delete();
        }
        targetDirectory.mkdir();
        Log.i(RicoDataPreparer.class.getName(), String.format("Exporting the script to %s", targetDirectory.getAbsolutePath()));

        //2. export the overall script
        String scriptContent = SugiliteScriptParser.scriptToString(script);
        BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(targetDirectory + "/" + "script.txt"));
        scriptWriter.write(scriptContent);
        scriptWriter.close();

        //3. start exporting the RICO data for the following blocks
        if (script.getNextBlock() != null) {
            exportRicoDataForBlockAndFollowingBlocks(script, targetDirectory, "0");
        }

        return targetDirectory;
    }

    private void exportRicoDataForBlockAndFollowingBlocks (SugiliteBlock sugiliteBlock, File targetDirectory, String blockLabel) throws IOException {
        if (sugiliteBlock instanceof SugiliteOperationBlock) {
            // export the block
            if (((SugiliteOperationBlock) sugiliteBlock).getSugiliteBlockMetaInfo() != null) {
                SugiliteBlockMetaInfo sugiliteBlockMetaInfo = ((SugiliteOperationBlock) sugiliteBlock).getSugiliteBlockMetaInfo();
                // generate and write the RicoScreen object
                if (sugiliteBlockMetaInfo.getUiSnapshot() != null) {
                    RicoScreen ricoScreen = RicoScreen.fromSugiliteUISnapshot(sugiliteBlockMetaInfo.getUiSnapshot());
                    String ricoScreenJSON = gson.toJson(ricoScreen);
                    BufferedWriter ricoScreenJSONWriter = new BufferedWriter(new FileWriter(targetDirectory + "/" + String.format("RicoScreen_%s.json", blockLabel)));
                    ricoScreenJSONWriter.write(ricoScreenJSON);
                    ricoScreenJSONWriter.close();

                    // generate and write the targetEntity
                    if (sugiliteBlockMetaInfo.getTargetEntity() != null) {
                        RicoNode targetRicoNode = RicoNode.fromSugiliteNode(sugiliteBlockMetaInfo.getTargetEntity(), sugiliteBlockMetaInfo.getUiSnapshot(), new HashSet<>());
                        String targetRicoNodeJSON = gson.toJson(targetRicoNode);
                        BufferedWriter targetRicoNodeJSONWriter = new BufferedWriter(new FileWriter(targetDirectory + "/" + String.format("TargetRicoNode_%s.json", blockLabel)));
                        targetRicoNodeJSONWriter.write(targetRicoNodeJSON);
                        targetRicoNodeJSONWriter.close();
                    }
                }
                // write the screenshot
                if (((SugiliteOperationBlock) sugiliteBlock).getFeaturePack() != null && ((SugiliteOperationBlock) sugiliteBlock).getFeaturePack().screenshot != null) {
                    File newScreenShotFileDst = new File(targetDirectory + "/" + String.format("Screenshot_%s.png", blockLabel));
                    Files.copy(((SugiliteOperationBlock) sugiliteBlock).getFeaturePack().screenshot.toPath(), newScreenShotFileDst.toPath());
                }
            }
        }
        if (sugiliteBlock instanceof SugiliteConditionBlock) {
            // handle the branches in SugiliteConditionBlock
            if (((SugiliteConditionBlock) sugiliteBlock).getThenBlock() != null) {
                exportRicoDataForBlockAndFollowingBlocks(((SugiliteConditionBlock) sugiliteBlock).getThenBlock(), targetDirectory, blockLabel + ".T.0");
            }
            if (((SugiliteConditionBlock) sugiliteBlock).getElseBlock() != null) {
                exportRicoDataForBlockAndFollowingBlocks(((SugiliteConditionBlock) sugiliteBlock).getElseBlock(), targetDirectory, blockLabel + ".E.0");
            }
        }
        if (sugiliteBlock.getNextBlock() != null) {
            // increase the counter for naming and process the nextBlock
            String[] blockLabels = blockLabel.split("\\.");
            String newLastBlockLabel = String.valueOf(Integer.parseInt(blockLabels[blockLabels.length - 1]) + 1);
            blockLabels[blockLabels.length - 1] = newLastBlockLabel;
            String newBlockLabel = StringUtils.join(blockLabels, ".");
            exportRicoDataForBlockAndFollowingBlocks(sugiliteBlock.getNextBlock(), targetDirectory, newBlockLabel);
        }
    }

}
