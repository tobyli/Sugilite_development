package edu.cmu.hcii.sugilite.ui.main;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.dao.SugiliteTriggerDao;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;

public class FragmentTriggerListTab extends Fragment {

    SugiliteTriggerDao triggerDao;
    Activity activity;
    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity = getActivity();
        triggerDao = new SugiliteTriggerDao(activity);
        rootView = inflater.inflate(R.layout.fragment_trigger_list, container, false);
        setUpTriggerList();
        return rootView;
    }

    /**
     * update the trigger list displayed at the main activity according to the DB
     */
    public void setUpTriggerList() {
        final ListView triggerList = (ListView)rootView.findViewById(R.id.triggerList);
        List<String> names = triggerDao.getAllNames();

        triggerList.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, names));
        triggerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String triggerName = (String) triggerList.getItemAtPosition(position);
                Toast.makeText(activity, "CLICKED ON" + triggerName, Toast.LENGTH_SHORT).show();
            }
        });
        registerForContextMenu(triggerList);
    }

    //TODO: enable edit/delete/view triggers



}