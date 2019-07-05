package edu.cmu.hcii.sugilite.communication;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import edu.cmu.hcii.sugilite.R;

/**
 * @author toby
 * @date 3/30/17
 * @time 9:57 AM
 */
public class SugiliteKeyboardCommunicationAdapterActivity extends Activity {
    TextView timestampView, contentView;

    /**
     * this class is used for the Sugilite Keyboard to communicate with Sugilite. The keyboard should pass in an intent with two arguments: "timestamp" and "content"
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sugilite_communication_acticvity);
        timestampView = (TextView) findViewById(R.id.receive_message_textview);
        contentView = (TextView) findViewById(R.id.receive_message_script_name);
        contentView.setText("TEST MESSAGE TYPE");

        String content = "";
        long timestamp = 0;

        if (getIntent().getExtras() != null) {
            timestamp = getIntent().getExtras().getLong("timestamp");
            content = getIntent().getExtras().getString("content");
            timestampView.setText(String.valueOf(timestamp));
            contentView.setText(content);

            processKeyboardInput(timestamp, content);
            finish();
        }
    }

    private void processKeyboardInput(long timestamp, String content){
        System.out.println("Received an Keyboard Input Event: " + content + " at " + timestamp);
        //TODO: do something

        /**
         * write the code for processing the keyboard input here
         */
    }

}

