package com.drdev.hlsplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;

public class DownloadsActivity extends AppCompatActivity {
    private ArrayList<File> fileList;
    private ArrayList<String> fileNames;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        ListView listView = findViewById(R.id.listView);
        Button btnClearAll = findViewById(R.id.btnClearAll);

        loadFiles(listView);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            File file = fileList.get(position);
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("PLAY_LOCAL", file.getAbsolutePath());
            startActivity(intent);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            File file = fileList.get(position);
            new AlertDialog.Builder(this)
                .setTitle("Delete Video")
                .setMessage("Delete " + file.getName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (file.delete()) loadFiles(listView);
                }).setNegativeButton("No", null).show();
            return true;
        });

        btnClearAll.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Delete All")
            .setMessage("Delete all downloaded videos?")
            .setPositiveButton("Yes", (dialog, which) -> {
                for (File f : fileList) f.delete();
                loadFiles(listView);
            }).setNegativeButton("No", null).show());
    }

    private void loadFiles(ListView listView) {
        fileList = new ArrayList<>();
        fileNames = new ArrayList<>();
        File dir = getExternalFilesDir(null);
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".ts")) {
                        fileList.add(f);
                        long sizeMb = f.length() / (1024 * 1024);
                        fileNames.add(f.getName() + " (" + sizeMb + " MB)");
                    }
                }
            }
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        listView.setAdapter(adapter);
    }
}
