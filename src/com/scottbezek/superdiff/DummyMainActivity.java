package com.scottbezek.superdiff;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DummyMainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy_main);

        final Button demoButton = (Button)findViewById(R.id.button_demo);
        demoButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DummyMainActivity.this, ListViewActivity.class);
                startActivity(intent);
            }
        });
    }

}
