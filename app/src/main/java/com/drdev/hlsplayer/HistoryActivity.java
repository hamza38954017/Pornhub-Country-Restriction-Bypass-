package com.drdev.hlsplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private ArrayList<String> historyList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        dbHelper = new DBHelper(this);
        ListView listView = findViewById(R.id.listView);
        Button btnClearAll = findViewById(R.id.btnClearAll);

        loadHistory(listView);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String url = historyList.get(position);
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("PLAY_URL", url);
            startActivity(intent);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String url = historyList.get(position);
            new AlertDialog.Builder(this)
                .setTitle("Delete History")
                .setMessage("Remove this URL?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deleteUrl(url);
                    loadHistory(listView);
                }).setNegativeButton("No", null).show();
            return true;
        });

        btnClearAll.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Delete all history?")
            .setPositiveButton("Yes", (dialog, which) -> {
                dbHelper.clearAll();
                loadHistory(listView);
            }).setNegativeButton("No", null).show());
    }

    private void loadHistory(ListView listView) {
        historyList = dbHelper.getAllHistory();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyList);
        listView.setAdapter(adapter);
    }
}
