package edu.cmu.hcii.sugilite.communication;

import android.content.Context;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.cmu.hcii.sugilite.communication.json.SugiliteScriptJSON;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * @author toby
 * @date 7/13/16
 * @time 2:28 PM
 */
public class SugiliteBlockJSONProcessor {
    Gson gson;
    Context context;
    public SugiliteBlockJSONProcessor(Context context){
        gson = new Gson();
        this.context = context;
    }

    public String scriptToJson(SugiliteStartingBlock startingBlock){
        SugiliteScriptJSON scriptJSON = new SugiliteScriptJSON(startingBlock);
        String json = gson.toJson(scriptJSON);
        return json;
    }

    public SugiliteStartingBlock jsonToScript(String json) throws Exception{
        SugiliteScriptJSON jsonBlock = gson.fromJson(json, SugiliteScriptJSON.class);
        if(jsonBlock != null) {
            return jsonBlock.toSugiliteStartingBlock(context);
        }
        else {
            throw new Exception("invalid json string!\n\n" + json);
        }
    }

    public String scriptsToJson(List<SugiliteStartingBlock> startingBlockList){
        List<SugiliteScriptJSON> retVal = new ArrayList<>();
        for(SugiliteStartingBlock startingBlock : startingBlockList){
            SugiliteScriptJSON scriptJSON = new SugiliteScriptJSON(startingBlock);
            retVal.add(scriptJSON);
        }
        return gson.toJson(retVal);
    }
}
