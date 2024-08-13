package org.digitalcampus.oppia.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.oppia.model.Media;
import org.digitalcampus.oppia.utils.MultiChoiceHelper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.ViewHolder> {

    private final Context context;
    private final List<Media> videos;
    private MultiChoiceHelper multiChoiceHelper;

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

        // Bind other video details
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void setMultiChoiceHelper(MultiChoiceHelper multiChoiceHelper) {
        this.multiChoiceHelper = multiChoiceHelper;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.video_title);
        }
    }
}
