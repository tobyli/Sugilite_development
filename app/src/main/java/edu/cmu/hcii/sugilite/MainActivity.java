package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

public class MainActivity extends AppCompatActivity {
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        View addButton = findViewById(R.id.addButton);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                final EditText scriptName = new EditText(v.getContext());
                scriptName.setText("New Script");
                scriptName.setSelectAllOnFocus(true);
                builder.setMessage("Specify the name for your new script")
                        .setView(scriptName)
                        .setPositiveButton("Start Recording", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (scriptName != null && scriptName.getText().toString().length() > 0) {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("scriptName", scriptName.getText().toString());
                                    editor.putBoolean("recording_in_process", true);
                                    editor.commit();
                                    Toast.makeText(v.getContext(), "Changed script name to " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //do nothing
                            }
                        })
                        .setTitle("New Script");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        setSupportActionBar(toolbar);


        LinearLayout listOfScripts = (LinearLayout)findViewById(R.id.listOfScripts);
        sugiliteData = (SugiliteData)getApplication();
        if(sugiliteData.getScriptHead() != null){
            TextView scriptItem = new TextView(this);
            scriptItem.setText(((SugiliteStartingBlock)sugiliteData.getScriptHead()).getScriptName());
            final Intent scriptDetailIntent = new Intent(this, ScriptDetailActivity.class);
            scriptDetailIntent.putExtra("scriptName", ((SugiliteStartingBlock)sugiliteData.getScriptHead()).getScriptName());
            scriptItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(scriptDetailIntent);
                }
            });
            listOfScripts.addView(scriptItem);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.clear_automation_queue) {
            int count = sugiliteData.getInstructionQueueSize();
            sugiliteData.clearInstructionQueue();
            new AlertDialog.Builder(this)
                    .setTitle("Automation Queue Cleared")
                    .setMessage("Cleared " + count + " operations from the automation queue")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
