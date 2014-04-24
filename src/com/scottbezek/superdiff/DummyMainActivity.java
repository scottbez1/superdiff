package com.scottbezek.superdiff;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class DummyMainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy_main);

        final String[] SAMPLES = {
                "view.diff",
                "basicMultiFile.diff",
        };
        final Spinner sampleSelect = (Spinner)findViewById(R.id.sample_selector);
        sampleSelect.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, SAMPLES));

        final Button demoButton = (Button)findViewById(R.id.button_view_sample);
        demoButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DummyMainActivity.this, ListViewActivity.class);
                intent.putExtra(ListViewActivity.EXTRA_SAMPLE, (String)sampleSelect.getSelectedItem());
                startActivity(intent);
            }
        });
    }

}
