package org.digitalcampus.oppia.activity;

import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class VideoListActivity extends AppCompatActivity implements VideoListListener {

    private ArrayList<Media> videos;
    private VideoListAdapter videoAdapter;
    private ActivityVideoListBinding binding;
    private DownloadBroadcastReceiver receiver;

    @Inject
    DownloadServiceDelegate downloadServiceDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoListBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        //((MyApp) getApplication()).getAppComponent().inject(this); // Use your Application class to get the component

        videos = new ArrayList<>();
        configureVideoAdapter();
        binding.videoList.setAdapter(videoAdapter);

        loadVideoList();
    }

    private void configureVideoAdapter() {
        videoAdapter = new VideoListAdapter(this, videos); // Pass 'this' instead of 'context'
    }

    private void loadVideoList() {
        // Load videos from all
        // For demonstration, we'll add some dummy videos
        for (int i = 0; i < 10; i++) {
            Media video = new Media();
            video.setFilename("Video " + (i + 1));
            video.setDownloadUrl("http://example.com/video" + (i + 1));
            videos.add(video);
        }
        videoAdapter.notifyDataSetChanged();
        binding.emptyState.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
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
        unregisterReceiver(receiver);
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
}
