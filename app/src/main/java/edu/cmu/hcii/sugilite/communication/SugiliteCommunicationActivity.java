package edu.cmu.hcii.sugilite.communication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
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
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteAppVocabularyDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTrackingDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;

import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;

/**
 * This is the activty used for communicating with external apps through the Android Intent Mechanism
 */

public class SugiliteCommunicationActivity extends Activity {

    TextView messageType, scriptName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // extract parameters from request, and prepare for processing
        if (getIntent().getExtras() != null)
        {
            String requestedMessageTypeString = getIntent().getStringExtra("messageType");
            this.messageType.setText(requestedMessageTypeString);
            this.scriptName.setText(getIntent().getStringExtra("arg1"));

            // call helper class to process request
            SugiliteCommunicationHelper sugiliteCommunicationHelper = new SugiliteCommunicationHelper(this, getIntent(), (SugiliteData)getApplication());
            Intent resultIntent = sugiliteCommunicationHelper.handleRequest();

            if(resultIntent != null)
            {
                setResult(Activity.RESULT_OK, resultIntent);
            }


        }
        finish();

    }
}
