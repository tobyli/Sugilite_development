package edu.cmu.hcii.sugilite.ui.main;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.sharing.SugiliteScriptSharingHTTPQueryManager;
import edu.cmu.hcii.sugilite.sharing.model.SugiliteRepoListing;
import edu.cmu.hcii.sugilite.ui.LocalScriptDetailActivity;
import edu.cmu.hcii.sugilite.ui.RemoteScriptDetailActivity;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;
import static edu.cmu.hcii.sugilite.Const.dateFormat_simple;

public class FragmentRemoteScriptListTab extends Fragment {
    private Activity activity;
    private View rootView;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private FragmentRemoteScriptListTab fragmentRemoteScriptListTab;
    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.activity = getActivity();
        this.rootView = inflater.inflate(R.layout.fragment_content_remote_script_list, container, false);
        this.sugiliteData = (SugiliteData) activity.getApplication();
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(SugiliteData.getAppContext());
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(activity);
        }
        else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(activity, sugiliteData);
        }

        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        try {
            setUpRemoveScriptList();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * update the trigger list displayed at the main activity according to the DB
     */
    public void setUpRemoveScriptList() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (rootView != null) {
                        final ListView remoteScriptEntryList = (ListView) rootView.findViewById(R.id.remoteScriptList);
                        List<SugiliteRepoListing> remoteScriptList = sugiliteScriptSharingHTTPQueryManager.getRepoList(true);

                        SugiliteData.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                remoteScriptEntryList.setAdapter(new SugiliteRepoListingAdapter(activity, R.layout.listitem_sugilite_repo_listing, remoteScriptList));
                                remoteScriptEntryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        SugiliteRepoListing repoListing = (SugiliteRepoListing) parent.getItemAtPosition(position);
                                        if (repoListing != null) {
                                            final Intent scriptDetailIntent = new Intent(activity, RemoteScriptDetailActivity.class);
                                            scriptDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            scriptDetailIntent.putExtra("repoListingId", repoListing.getId());
                                            scriptDetailIntent.putExtra("scriptName", repoListing.getTitle());
                                            startActivity(scriptDetailIntent);
                                        } else {
                                            Log.e("FragmentRemoteScriptListTable", "Failed to load the remote script");
                                        }
                                    }
                                });
                                registerForContextMenu(remoteScriptEntryList);
                            }
                        });
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

        //TODO: enable context menu for remote script entries
    }

    public class SugiliteRepoListingAdapter extends ArrayAdapter{
        private final int resourceId;

        public SugiliteRepoListingAdapter(Context context, int textViewResourceId, List<SugiliteRepoListing> objects) {
            super(context, textViewResourceId, objects);
            resourceId = textViewResourceId;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SugiliteRepoListing repoListing = (SugiliteRepoListing) getItem(position);
            View view = LayoutInflater.from(getContext()).inflate(resourceId, null);
            TextView scriptName = (TextView) view.findViewById(R.id.scriptName);
            TextView scriptAuthor = (TextView) view.findViewById(R.id.authorName);
            TextView scriptTimeStamp = (TextView) view.findViewById(R.id.timeStamp);
            scriptName.setText(PumiceDemonstrationUtil.removeScriptExtension(repoListing.getTitle()));
            if (repoListing.getAuthor() != null) {
                scriptAuthor.setText(repoListing.getAuthor());
            }
            if (repoListing.getUploadedTimeStamp() != null) {
                scriptTimeStamp.setText(dateFormat_simple.format(repoListing.getUploadedTimeStamp()));
            }
            return view;
        }
    }


}