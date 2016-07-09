package edu.cmu.hcii.sugilite;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.SetMapEntrySerializableWrapper;

@Deprecated
public class RecordingPopupSubMenuActivity extends AppCompatActivity {
    private Set<Map.Entry<String, String>> allFeatures;
    private Set<Map.Entry<String, String>> selectedFeatures;
    private Set<Map.Entry<String, String>> retVal = new HashSet<>();
    private Map<Integer, Map.Entry<String, String>> idEntryMap = new HashMap<>();
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_popup_sub_menu);
        if (savedInstanceState == null) {
            allFeatures = ((SetMapEntrySerializableWrapper)getIntent().getExtras().getSerializable("allFeatures")).set;
            selectedFeatures = ((SetMapEntrySerializableWrapper)getIntent().getExtras().getSerializable("selectedFeatures")).set;
        }
        else {
            allFeatures = ((SetMapEntrySerializableWrapper)savedInstanceState.getSerializable("allFeatures")).set;
            selectedFeatures = ((SetMapEntrySerializableWrapper)savedInstanceState.getSerializable("selectedFeatures")).set;
        }

        //add all features to menu
        //select all selected features in menu
        linearLayout= (LinearLayout)findViewById(R.id.child_parent_feature_linear_layout);
        int counter = 0;
        for(Map.Entry<String, String> feature: allFeatures){
            CheckBox checkBox = new CheckBox(this);
            if(selectedFeatures.contains(feature)) {
                checkBox.setChecked(true);
            }
            else{
                checkBox.setChecked(false);
            }
            checkBox.setText("has " + feature.getKey() + " = " + feature.getValue());
            checkBox.setHint(String.valueOf(counter));
            idEntryMap.put(counter, feature);
            counter++;
            linearLayout.addView(checkBox);
        }
        if(linearLayout.getChildCount() == 0){
            TextView textView = new TextView(this);
            textView.setText("No feature available!");
            linearLayout.addView(textView);
        }
    }

    public void onButtonClicked(View view){
        if(view.getId() == R.id.ok_button){
            for(int i = 0; i < linearLayout.getChildCount(); i++){
                if(linearLayout.getChildAt(i) == null || (! (linearLayout.getChildAt(i) instanceof CheckBox))){
                    continue;
                }
                CheckBox checkBox = (CheckBox)linearLayout.getChildAt(i);
                if(checkBox.isChecked()){
                    retVal.add(idEntryMap.get(Integer.parseInt(checkBox.getHint().toString())));
                }
            }
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result", new SetMapEntrySerializableWrapper(retVal));
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        }
        else if (view.getId() == R.id.cancel_button){
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }

    }
}
