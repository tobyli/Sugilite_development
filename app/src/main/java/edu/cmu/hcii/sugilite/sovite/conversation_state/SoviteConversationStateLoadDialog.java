package edu.cmu.hcii.sugilite.sovite.conversation_state;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.OperationBlockDescriptionRegenerator;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.study.SoviteStudyDumpPacket;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 4/23/20
 * @time 7:33 PM
 */
public class SoviteConversationStateLoadDialog {
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private File conversationStateDir;
    private LayoutInflater layoutInflater;

    private AlertDialog dialog;
    private ListView conversationalStateListView;


    public SoviteConversationStateLoadDialog (Activity context, PumiceDialogManager pumiceDialogManager) {
        this.context = context;
        this.pumiceDialogManager = pumiceDialogManager;
        this.layoutInflater = LayoutInflater.from(context);

        try {
            File rootDataDir = context.getFilesDir();
            conversationStateDir = new File(rootDataDir.getPath() + "/sovite_conversation_state_dump");
            if (!conversationStateDir.exists() || !conversationStateDir.isDirectory())
                conversationStateDir.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        this.conversationalStateListView = new ListView(context);

        List<SoviteConversationState> allSoviteConversationState = getAllStoredSoviteConversationState();
        Map<String, SoviteConversationState> soviteConversationStateNameSoviteConversationStateMap = new HashMap<>();
        allSoviteConversationState.forEach(soviteConversationState -> soviteConversationStateNameSoviteConversationStateMap.put(soviteConversationState.getName(), soviteConversationState));

        String[] allSoviteConversationStateNameArray = new String[soviteConversationStateNameSoviteConversationStateMap.size()];
        int i = 0;
        for (String soviteConversationStateName : soviteConversationStateNameSoviteConversationStateMap.keySet()) {
            allSoviteConversationStateNameArray[i++] = soviteConversationStateName;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SugiliteData.getAppContext(), android.R.layout.simple_list_item_1, allSoviteConversationStateNameArray)
        {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View row;
                if (null == convertView) {
                    row = layoutInflater.inflate(android.R.layout.simple_list_item_1, null);
                } else {
                    row = convertView;
                }
                TextView tv = (TextView) row.findViewById(android.R.id.text1);
                tv.setText(getItem(position));
                return row;
            }

        };
        conversationalStateListView.setAdapter(adapter);
        builder.setView(conversationalStateListView);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Clear the stored conversation states", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //clear the conversationStateDir directory
                if (conversationStateDir.isDirectory()) {
                    String[] children = conversationStateDir.list();
                    for (int i = 0; i < children.length; i++) {
                        new File(conversationStateDir, children[i]).delete();
                    }
                }
                dialog.dismiss();
                SoviteConversationStateLoadDialog soviteConversationStateLoadDialog = new SoviteConversationStateLoadDialog(context, pumiceDialogManager);
                soviteConversationStateLoadDialog.show();
            }
        });

        conversationalStateListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //showConfirmation(sugiliteOperationBlockArray[position], featurePack, queryScoreList);
                String name = ((TextView)view).getText().toString();
                SoviteConversationState soviteConversationState = soviteConversationStateNameSoviteConversationStateMap.get(name);
                if (soviteConversationState != null) {
                    pumiceDialogManager.loadSoviteConversationState(soviteConversationState);
                    PumiceDemonstrationUtil.showSugiliteToast(String.format("Loading the conversation state: %s", name), Toast.LENGTH_SHORT);
                }
                dialog.dismiss();
            }
        });

        dialog = builder.create();




    }

    public void show() {
        if (dialog != null) {
            dialog.getWindow().setType(OVERLAY_TYPE);
            dialog.show();
        }
    }

    private List<SoviteConversationState> getAllStoredSoviteConversationState() {
        List<SoviteConversationState> results = new ArrayList<>();
        List<File> files = new ArrayList<>();
        try {
            for (File file : conversationStateDir.listFiles()) {
                if (file.getName().endsWith(".sovitedump")) {
                    files.add(file);
                }
                FileInputStream fin = null;
                ObjectInputStream ois = null;
                SoviteConversationState packet = null;
                try {
                    fin = new FileInputStream(file);
                    ois = new ObjectInputStream(new BufferedInputStream(fin));
                    packet = (SoviteConversationState) ois.readObject();
                    if (packet.getName() == null) {
                        packet.setName(file.getName().replace(".sovitedump", ""));
                    }
                    results.add(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                    //throw e;
                    //TODO: error handling
                } finally {
                    if (fin != null)
                        fin.close();
                    if (ois != null)
                        ois.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
}
