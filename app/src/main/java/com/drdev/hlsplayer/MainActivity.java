package com.drdev.hlsplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.ui.PlayerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🎬 HLS Native Player & Downloader
 * Copyright © Dr. Dev || Dr. Hamza 2026
 */
public class MainActivity extends AppCompatActivity {
    private EditText etUrl;
    private PlayerView playerView;
    private ExoPlayer player;
    private DBHelper dbHelper;

    private final String USER_AGENT = "Mozilla/5.0 (Android 15; Mobile; rv:150.0) Gecko/150.0 Firefox/150.0";
    private final String ORIGIN = "https://www.pornhub.com";
    private final String REFERER = "https://www.pornhub.com/";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        etUrl = findViewById(R.id.etUrl);
        playerView = findViewById(R.id.playerView);

        findViewById(R.id.btnHistory).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btnDownloads).setOnClickListener(v -> startActivity(new Intent(this, DownloadsActivity.class)));

        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                dbHelper.addUrl(url); // Save to history
                startNetworkPlayback(url);
            }
        });

        findViewById(R.id.btnDownload).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                dbHelper.addUrl(url);
                fetchQualities(url);
            }
        });

        // Check for Intents from History or Downloads
        if (getIntent().hasExtra("PLAY_URL")) {
            String url = getIntent().getStringExtra("PLAY_URL");
            etUrl.setText(url);
            startNetworkPlayback(url);
        } else if (getIntent().hasExtra("PLAY_LOCAL")) {
            String localPath = getIntent().getStringExtra("PLAY_LOCAL");
            startLocalPlayback(localPath);
        }
    }

    private void startNetworkPlayback(String url) {
        if (player != null) player.release();
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setDefaultRequestProperties(java.util.Collections.singletonMap("Origin", ORIGIN))
                .setDefaultRequestProperties(java.util.Collections.singletonMap("Referer", REFERER));

        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url));

        player.setMediaSource(hlsMediaSource);
        player.prepare();
        player.play();
    }

    private void startLocalPlayback(String localPath) {
        if (player != null) player.release();
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri("file://" + localPath));
        player.prepare();
        player.play();
    }

    private void fetchQualities(String masterUrl) {
        Toast.makeText(this, "Parsing qualities...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                String m3u8Content = fetchHttpContent(masterUrl);
                String[] lines = m3u8Content.split("\n");
                List<String> names = new ArrayList<>(), urls = new ArrayList<>();
                URI baseUri = new URI(masterUrl);

                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].startsWith("#EXT-X-STREAM-INF")) {
                        String res = lines[i].contains("RESOLUTION=") ? lines[i].split("RESOLUTION=")[1].split(",")[0] : "Auto";
                        names.add(res);
                        urls.add(baseUri.resolve(lines[i + 1].trim()).toString());
                    }
                }
                mainHandler.post(() -> {
                    if (names.isEmpty()) startDownloadTask(masterUrl);
                    else showQualityDialog(names, urls);
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showQualityDialog(List<String> names, List<String> urls) {
        new AlertDialog.Builder(this).setTitle("Select Quality")
                .setItems(names.toArray(new String[0]), (dialog, which) -> startDownloadTask(urls.get(which))).show();
    }

    private void startDownloadTask(String playlistUrl) {
        Toast.makeText(this, "Downloading in background...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                String[] lines = fetchHttpContent(playlistUrl).split("\n");
                List<String> tsUrls = new ArrayList<>();
                URI baseUri = new URI(playlistUrl);
                
                for (String line : lines) {
                    if (!line.startsWith("#") && !line.trim().isEmpty()) {
                        tsUrls.add(baseUri.resolve(line.trim()).toString());
                    }
                }
                if (tsUrls.isEmpty()) return;

                File outputFile = new File(getExternalFilesDir(null), "dr_dev_video_" + System.currentTimeMillis() + ".ts");
                FileOutputStream fos = new FileOutputStream(outputFile);

                for (String chunkUrl : tsUrls) {
                    HttpURLConnection conn = (HttpURLConnection) new URL(chunkUrl).openConnection();
                    conn.setRequestProperty("User-Agent", USER_AGENT);
                    conn.setRequestProperty("Referer", REFERER);
                    InputStream is = conn.getInputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) fos.write(buffer, 0, bytesRead);
                    is.close();
                }
                fos.close();
                mainHandler.post(() -> Toast.makeText(this, "Download Complete!", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Download Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String fetchHttpContent(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Referer", REFERER);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
        executor.shutdown();
    }
}
