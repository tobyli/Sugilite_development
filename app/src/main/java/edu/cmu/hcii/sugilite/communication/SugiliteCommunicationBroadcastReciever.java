package edu.cmu.hcii.sugilite.communication;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteAppVocabularyDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTrackingDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;

import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;

/**
 * This class sends broadcasts sugilite responses.
 * Other ways of communicating with sugilite include :
 * - directly communicating via intents
 * - communicating via the inmind middleware
 */

public class SugiliteCommunicationBroadcastReciever extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // call helper class to process request
        SugiliteCommunicationHelper sugiliteCommunicationHelper = new SugiliteCommunicationHelper(context, intent, (SugiliteData)context.getApplicationContext());
        Intent resultIntent = sugiliteCommunicationHelper.handleRequest();

        if(resultIntent!=null)
        {
            resultIntent.setAction("edu.cmu.hcii.sugilite.SUGILITE_SERVICES");
            context.sendBroadcast(resultIntent);

            Log.d("sug_comm_broadcast", "result : "+resultIntent.getStringExtra("result"));
        }
        Log.d("sug_comm_broadcast", "responded to : "+intent.getStringExtra("messageType"));
    }

    public void finish()
    {}
}
