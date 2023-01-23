package com.example.sms_app_proj;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.appcompat.app.AppCompatActivity;

public class BlockedNum extends AppCompatActivity {
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spam);
        context = this;


        Cursor cursor = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cursor = context.getContentResolver().query(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
        }

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                context,
                android.R.layout.simple_list_item_1,
                cursor,
                new String[] {BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER},
                new int[] {android.R.id.text1},
                0
        );
        ListView listView = (ListView) findViewById(R.id.shBlNum);
        listView.setAdapter(adapter);

        Button button = findViewById(R.id.btnBack);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BlockedNum.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }



}
