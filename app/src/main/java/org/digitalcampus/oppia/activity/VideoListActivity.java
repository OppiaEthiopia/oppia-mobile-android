package org.digitalcampus.oppia.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.learning.databinding.ActivityVideoListBinding;
import org.digitalcampus.oppia.adapter.VideoListAdapter;
import org.digitalcampus.oppia.listener.DownloadMediaListener;
import org.digitalcampus.oppia.listener.VideoListListener;
import org.digitalcampus.oppia.model.Media;
import org.digitalcampus.oppia.service.DownloadBroadcastReceiver;
import org.digitalcampus.oppia.service.DownloadService;
import org.digitalcampus.oppia.service.DownloadServiceDelegate;
import org.digitalcampus.oppia.utils.resources.ExternalResourceOpener;
import org.digitalcampus.oppia.utils.ui.DrawerMenuManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import javax.inject.Inject;

import com.google.android.material.tabs.TabLayout;

public class VideoListActivity extends AppActivity implements VideoListListener, DownloadMediaListener {

    private enum VideoFilter {
        ALL,
        FAVORITES
    }

    private static final String PREFS_NAME = "video_list_preferences";
    private static final String PREF_FAVORITES = "favorite_videos";

    private final ArrayList<Media> allVideos = new ArrayList<>();
    private final Set<String> favoriteVideoPaths = new HashSet<>();
    private VideoFilter currentFilter = VideoFilter.ALL;

    private ArrayList<Media> videos;
    private VideoListAdapter videoAdapter;
    private ActivityVideoListBinding binding;
    private DownloadBroadcastReceiver receiver;
    private DrawerMenuManager drawerMenuManager;

    @Inject
    DownloadServiceDelegate downloadServiceDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoListBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        getAppComponent().inject(this);

        setSupportActionBar((androidx.appcompat.widget.Toolbar) binding.toolbar.getRoot());
        configureBottomNavigation();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        loadFavoritePreferences();

        videos = new ArrayList<>();
        configureVideoAdapter();
        binding.videoList.setAdapter(videoAdapter);

        setupFilterTabs();

        loadVideoList();
    }

    @Override
    public void onStart() {
        super.onStart();
        initialize();
        drawerMenuManager = new DrawerMenuManager(this, getSupportFragmentManager());
        drawerMenuManager.initializeDrawer();
    }

    private void configureVideoAdapter() {
        videoAdapter = new VideoListAdapter(this, videos); // Pass 'this' instead of 'context'
        videoAdapter.setOnVideoClickListener(this::openVideo);
        videoAdapter.setOnVideoFavoriteToggleListener(this::toggleFavoriteForVideo);
        videoAdapter.updateFavorites(favoriteVideoPaths);
    }

    private void configureBottomNavigation() {
        if (binding.bottomNavigation == null) {
            return;
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_bottom_video) {
                return true;
            }

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_BOTTOM_NAV_SELECTION, itemId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        });

        binding.bottomNavigation.setSelectedItemId(R.id.nav_bottom_video);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (drawerMenuManager != null) {
            drawerMenuManager.onPrepareOptionsMenu((Map<Integer, DrawerMenuManager.MenuOption>) null);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void setupFilterTabs() {
        TabLayout tabs = binding.tabsFilterVideos;
        if (tabs == null) {
            return;
        }

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getPosition() == 0 ? VideoFilter.ALL : VideoFilter.FAVORITES;
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // no-op
            }
        });

        TabLayout.Tab initialTab = tabs.getTabAt(currentFilter.ordinal());
        if (initialTab != null) {
            initialTab.select();
        }
    }

    private void openVideo(Media video) {
        if (video == null || video.getDownloadUrl() == null) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        File videoFile = new File(video.getDownloadUrl());
        if (!videoFile.exists()) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = ExternalResourceOpener.getIntentToOpenResource(this, videoFile);
        if (intent == null) {
            Toast.makeText(this, R.string.error_no_viewer_app, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_no_viewer_app, Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
            // Covers rare FileProvider misconfiguration / URI permission issues that can otherwise look like a freeze.
            Log.e("VideoListActivity", "Failed to open video: " + videoFile.getAbsolutePath(), e);
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadVideoList() {
        binding.progressVideo.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);

        new Thread(() -> {
            ArrayList<Media> loadedVideos = new ArrayList<>();

            // Define the directory where Oppia Mobile stores videos
            File videoDirectory = new File(getExternalFilesDir(null), "media");

            if (videoDirectory.exists() && videoDirectory.isDirectory()) {
                File[] videoFiles = videoDirectory.listFiles((dir, name) ->
                        name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv"));

                if (videoFiles != null) {
                    for (File videoFile : videoFiles) {
                        Media video = new Media();
                        video.setFilename(videoFile.getName());
                        // Using downloadUrl as a local path here (this screen is local-only).
                        video.setDownloadUrl(videoFile.getAbsolutePath());
                        loadedVideos.add(video);
                    }
                }
            }

            runOnUiThread(() -> {
                allVideos.clear();
                allVideos.addAll(loadedVideos);

                Set<String> availablePaths = new HashSet<>();
                for (Media media : loadedVideos) {
                    availablePaths.add(media.getDownloadUrl());
                }

                if (favoriteVideoPaths.retainAll(availablePaths)) {
                    persistFavoritePreferences();
                }

                applyFilter();
                videoAdapter.updateFavorites(favoriteVideoPaths);
                binding.progressVideo.setVisibility(View.GONE);
                binding.emptyState.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void applyFilter() {
        videos.clear();
        if (currentFilter == VideoFilter.ALL) {
            videos.addAll(allVideos);
        } else {
            for (Media media : allVideos) {
                if (favoriteVideoPaths.contains(media.getDownloadUrl())) {
                    videos.add(media);
                }
            }
        }
        videoAdapter.notifyDataSetChanged();
        binding.emptyState.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void toggleFavoriteForVideo(Media video) {
        if (video == null || video.getDownloadUrl() == null) {
            return;
        }
        String path = video.getDownloadUrl();
        boolean added;
        if (favoriteVideoPaths.contains(path)) {
            favoriteVideoPaths.remove(path);
            added = false;
        } else {
            favoriteVideoPaths.add(path);
            added = true;
        }
        persistFavoritePreferences();
        videoAdapter.updateFavorites(favoriteVideoPaths);
        applyFilter();
        Toast.makeText(this,
                added ? R.string.video_favorite_added_toast : R.string.video_favorite_removed_toast,
                Toast.LENGTH_SHORT).show();
    }

    private void loadFavoritePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(PREF_FAVORITES, Collections.emptySet());
        favoriteVideoPaths.clear();
        if (saved != null) {
            favoriteVideoPaths.addAll(saved);
        }
    }

    private void persistFavoritePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putStringSet(PREF_FAVORITES, new HashSet<>(favoriteVideoPaths)).apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new DownloadBroadcastReceiver();
        receiver.setMediaListener((DownloadMediaListener) this);
        IntentFilter broadcastFilter = new IntentFilter(DownloadService.BROADCAST_ACTION);
        broadcastFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(receiver, broadcastFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                Log.w("VideoListActivity", "Receiver already unregistered", e);
            }
            receiver = null;
        }
    }

    private Media findVideo(String fileUrl) {
        for (Media video : videos) {
            if (video.getDownloadUrl().equals(fileUrl)) {
                return video;
            }
        }
        return null;
    }

    @Override
    public void onVideoDownloadProgress(String fileUrl, int progress) {

    }

    @Override
    public void onVideoDownloadFailed(String fileUrl, String message) {

    }

    @Override
    public void onVideoDownloadComplete(String fileUrl) {

    }

    @Override
    public void onDownloadProgress(String fileUrl, int progress) {

    }

    @Override
    public void onDownloadFailed(String fileUrl, String message) {

    }

    @Override
    public void onDownloadComplete(String fileUrl) {

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerMenuManager != null) {
            drawerMenuManager.onPostCreate();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerMenuManager != null) {
            drawerMenuManager.onConfigurationChanged(newConfig);
        }
    }
}
