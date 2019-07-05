package edu.cmu.hcii.sugilite.model.block.special_operation;

import android.content.Context;
import android.content.SharedPreferences;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;

/**
 * @author toby
 * @date 10/31/16
 * @time 2:04 PM
 */


public class SugiliteDelaySpecialOperationBlock extends SugiliteSpecialOperationBlock {

    int delayInMilliseconds;

    public SugiliteDelaySpecialOperationBlock(int delayInMilliseconds){
        super();
        this.delayInMilliseconds = delayInMilliseconds;
    }

    @Override
    public void run(Context context, SugiliteData sugiliteData, SugiliteScriptDao sugiliteScriptDao, SharedPreferences sharedPreferences) throws Exception{
            Thread.sleep(delayInMilliseconds);
    }

    @Override
    public String toString() {
        return "(" + "call" + " " + "delay" + " " + delayInMilliseconds + ")";
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("Delay the script execution for %f milliseconds", delayInMilliseconds);
    }
}
