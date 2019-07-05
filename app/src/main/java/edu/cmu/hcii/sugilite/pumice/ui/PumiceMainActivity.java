package edu.cmu.hcii.sugilite.pumice.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import edu.cmu.hcii.sugilite.R;

public class PumiceMainActivity extends AppCompatActivity {
    //keep the conversation context
    //textbox + speak button for user input

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pumice_main);
    }
}
