package edu.cmu.hcii.sugilite;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class RecodingPopUpActivity extends AppCompatActivity {

    private String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    private long time;
    private int eventType;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_recoding_pop_up);
        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            if(extras == null){

            }
            else{
                packageName = extras.getString("packageName", "NULL");
                className = extras.getString("className", "NULL");
                text = extras.getString("text", "NULL");
                contentDescription = extras.getString("contentDescription", "NULL");
                viewId = extras.getString("viewId", "NULL");
                boundsInParent = extras.getString("boundsInParent", "NULL");
                boundsInScreen = extras.getString("boundsInScreen", "NULL");
                time = extras.getLong("time", -1);
                eventType = extras.getInt("eventType", -1);
            }
        }
        else{
            packageName = savedInstanceState.getString("packageName", "NULL");
            className = savedInstanceState.getString("className", "NULL");
            text = savedInstanceState.getString("text", "NULL");
            contentDescription = savedInstanceState.getString("contentDescription", "NULL");
            viewId = savedInstanceState.getString("viewId", "NULL");
            boundsInParent = savedInstanceState.getString("boundsInParent", "NULL");
            boundsInScreen = savedInstanceState.getString("boundsInScreen", "NULL");
            time = savedInstanceState.getLong("time", -1);
            eventType = savedInstanceState.getInt("eventType", -1);
        }

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));

        ((CheckBox) findViewById(R.id.packageName)).setText("Package Name: " + packageName);
        ((CheckBox)findViewById(R.id.className)).setText("Class Name: " + className);
        ((CheckBox)findViewById(R.id.text)).setText("Text: " + text);
        ((CheckBox)findViewById(R.id.contentDescription)).setText("Content Description: " + contentDescription);
        ((CheckBox)findViewById(R.id.viewId)).setText("ViewId: " + viewId);
        ((CheckBox)findViewById(R.id.boundsInParent)).setText("Bounds in Parent: " + boundsInParent);
        ((CheckBox)findViewById(R.id.boundsInScreen)).setText("Bounds in Screen: " + boundsInScreen);
        ((TextView)findViewById(R.id.time)).setText("Event Time: " + dateFormat.format(c.getTime()));

    }

    public void finishActivity(View view){

        finish();
    }
    public void turnOffRecording(View view)
    {

        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.commit();
        finish();
    }

    public void saveSelection(){

    }

    //TODO: change to check box
    public void entryOnSelect(View view){

        int viewId = view.getId();

        if(viewId == R.id.packageName){
            Toast.makeText(this, "Selected Package Name as Identifying Feature", Toast.LENGTH_SHORT).show();
            finish();
        }
        if(viewId == R.id.className){
            Toast.makeText(this, "Selected Class Name as Identifying Feature", Toast.LENGTH_SHORT).show();
            finish();
        }
        if(viewId == R.id.text){
            Toast.makeText(this, "Selected Text as Identifying Feature", Toast.LENGTH_SHORT).show();
            finish();
        }
        if(viewId == R.id.contentDescription){
            Toast.makeText(this, "Selected Content Description as Identifying Feature", Toast.LENGTH_SHORT).show();
            finish();
        }
        if(viewId == R.id.viewId){
            Toast.makeText(this, "Selected View ID as Identifying Feature", Toast.LENGTH_SHORT).show();
            finish();
        }
        if(viewId == R.id.boundsInParent){
            Toast.makeText(this, "Selected Bound in Parent as Identifying Feature", Toast.LENGTH_SHORT).show();
            finish();
        }
        if(viewId == R.id.boundsInScreen){
            Toast.makeText(this, "Selected Bount in Screen as Identifying Feature", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


}
