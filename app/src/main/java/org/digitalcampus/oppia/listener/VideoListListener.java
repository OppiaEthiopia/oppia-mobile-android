package org.digitalcampus.oppia.listener;

public interface VideoListListener {
	void onVideoDownloadProgress(String fileUrl, int progress);
	void onVideoDownloadFailed(String fileUrl, String message);
	void onVideoDownloadComplete(String fileUrl);
}