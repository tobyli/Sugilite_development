package edu.cmu.hcii.sugilite.communication;

import android.content.Context;

import com.google.gson.Gson;

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

    public SugiliteStartingBlock jsonToScript(String json){
        SugiliteScriptJSON jsonBlock = gson.fromJson(json, SugiliteScriptJSON.class);
        return jsonBlock.toSugiliteStartingBlock(context);
    }
}
