package org.digitalcampus.oppia.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.oppia.model.Media;
import org.digitalcampus.oppia.utils.MultiChoiceHelper;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.ViewHolder> {

    private final Context context;
    private final List<Media> videos;
    private MultiChoiceHelper multiChoiceHelper;
    private OnVideoClickListener onVideoClickListener;
    private OnVideoFavoriteToggleListener onVideoFavoriteToggleListener;
    private final Set<String> favoriteVideoPaths = new HashSet<>();

    public interface OnVideoClickListener {
        void onVideoClicked(Media video);
    }

    public interface OnVideoFavoriteToggleListener {
        void onFavoriteToggled(Media video);
    }

    public VideoListAdapter(Context context, List<Media> videos) {
        this.context = context;
        this.videos = videos;
        this.multiChoiceHelper = new MultiChoiceHelper((AppCompatActivity) context, this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Media video = videos.get(position);
        holder.title.setText(video.getFilename());

        boolean isFavorite = favoriteVideoPaths.contains(video.getDownloadUrl());
        holder.favoriteButton.setImageResource(isFavorite ? R.drawable.ic_video_favorite_on : R.drawable.ic_video_favorite_off);
        holder.favoriteButton.setContentDescription(holder.itemView.getContext().getString(
                isFavorite ? R.string.video_favorite_remove : R.string.video_favorite_add));

        holder.itemView.setOnClickListener(v -> {
            if (onVideoClickListener != null) {
                onVideoClickListener.onVideoClicked(video);
            }
        });

        holder.favoriteButton.setOnClickListener(v -> {
            if (onVideoFavoriteToggleListener != null) {
                onVideoFavoriteToggleListener.onFavoriteToggled(video);
            }
        });

        // Bind other video details
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void setMultiChoiceHelper(MultiChoiceHelper multiChoiceHelper) {
        this.multiChoiceHelper = multiChoiceHelper;
    }

    public void setOnVideoClickListener(OnVideoClickListener onVideoClickListener) {
        this.onVideoClickListener = onVideoClickListener;
    }

    public void setOnVideoFavoriteToggleListener(OnVideoFavoriteToggleListener listener) {
        this.onVideoFavoriteToggleListener = listener;
    }

    public void updateFavorites(Set<String> favorites) {
        favoriteVideoPaths.clear();
        if (favorites != null) {
            favoriteVideoPaths.addAll(favorites);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageButton favoriteButton;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.video_title);
            favoriteButton = itemView.findViewById(R.id.button_favorite);
        }
    }
}
