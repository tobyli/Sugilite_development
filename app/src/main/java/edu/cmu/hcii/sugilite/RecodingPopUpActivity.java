package edu.cmu.hcii.sugilite;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class RecodingPopUpActivity extends AppCompatActivity {

    private String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    private long time;
    private int eventType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        ((TextView) findViewById(R.id.packageName)).setText("Package Name: " + packageName);
        ((TextView)findViewById(R.id.className)).setText("Class Name: " + className);
        ((TextView)findViewById(R.id.text)).setText("Text: " + text);
        ((TextView)findViewById(R.id.contentDescription)).setText("Content Description: " + contentDescription);
        ((TextView)findViewById(R.id.viewId)).setText("ViewId: " + viewId);
        ((TextView)findViewById(R.id.boundInParent)).setText("Bounds in Parent: " + boundsInParent);
        ((TextView)findViewById(R.id.boundInScreen)).setText("Bounds in Screen: " + boundsInScreen);
        ((TextView)findViewById(R.id.time)).setText("Event Time: " + dateFormat.format(c.getTime()));

    }

    public void finishActivity(View view){
        finish();
    }


}
