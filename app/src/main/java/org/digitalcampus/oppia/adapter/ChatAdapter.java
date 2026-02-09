package org.digitalcampus.oppia.adapter;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.digitalcampus.mobile.learning.R;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public static class Message {
        public String text;
        public boolean isUser;

        /** Only used for assistant messages; can be empty. */
        public List<ImageAttachment> images;

        /** Backend identifier when available; fallback is generated client-side. */
        public String messageId;

        /** Tracks current like/dislike selection. */
        public FeedbackState feedbackState;

        /** Allows suppressing feedback controls for placeholder/error rows. */
        public boolean allowFeedback;

        /** Identifier returned after sending binary (like/dislike) feedback. */
        public String feedbackId;

        /** Shows categorical options after a dislike is recorded. */
        public boolean showCategorical;

        /** Tracks whether categorical options should be hidden after completion. */
        public boolean categoricalOptionsRemoved;

        /** Records selected categorical feedback values. */
        public Set<String> categoricalSelections;

        public Message(String text, boolean isUser) {
            this(text, isUser, null);
        }

        public Message(String text, boolean isUser, List<ImageAttachment> images) {
            this.text = text;
            this.isUser = isUser;
            this.images = images == null ? new ArrayList<>() : images;
            this.feedbackState = FeedbackState.NONE;
            this.allowFeedback = !isUser;
            this.feedbackId = null;
            this.showCategorical = false;
            this.categoricalOptionsRemoved = false;
            this.categoricalSelections = new HashSet<>();
        }
    }

    public static class ImageAttachment {
        public String imageDataBase64;
        public String caption;

        public ImageAttachment(String imageDataBase64, String caption) {
            this.imageDataBase64 = imageDataBase64;
            this.caption = caption;
        }
    }

    public static class FeedbackOption {
        public final String value;
        public final String label;
        public final String group;

        public FeedbackOption(String value, String label, String group) {
            this.value = value;
            this.label = label;
            this.group = group;
        }
    }

    private static final List<FeedbackOption> FEEDBACK_OPTIONS = Arrays.asList(
            new FeedbackOption("Too Short", "Too Short", "length"),
            new FeedbackOption("Too Long", "Too Long", "length"),
            new FeedbackOption("Fully Inaccurate", "Fully Inaccurate", "accuracy"),
            new FeedbackOption("Partially Inaccurate", "Partially Inaccurate", "accuracy"),
            new FeedbackOption("Uses External Knowledge", "Uses External Knowledge", "independent"),
            new FeedbackOption("Incorrect Formatting", "Incorrect Formatting", "independent"),
            new FeedbackOption("Hard To Understand", "Hard To Understand", "independent"),
            new FeedbackOption("Incorrect Program Documents Used", "Incorrect Program Documents Used", "independent"),
            new FeedbackOption("Bad Translation", "Bad Translation", "independent")
    );

    public enum FeedbackState {
        NONE,
        LIKE,
        DISLIKE
    }

    public interface OnFeedbackSelectedListener {
        void onFeedbackSelected(int position, Message message, FeedbackState newState);
        void onFeedbackCategoricalSelected(int position, Message message, String optionValue);
    }

    private final List<Message> messages;
    private final OnFeedbackSelectedListener feedbackListener;

    public ChatAdapter(List<Message> messages, OnFeedbackSelectedListener feedbackListener) {
        this.messages = messages;
        this.feedbackListener = feedbackListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser ? 0 : 1;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == 0 ? R.layout.item_user_message : R.layout.item_ai_message;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message msg = messages.get(position);
        holder.textView.setText(formatRichText(msg.text, holder.textView.getLineHeight()));

        bindFeedback(holder, msg);

        if (holder.imagesContainer != null) {
            holder.imagesContainer.removeAllViews();

            if (msg.images != null) {
                for (ImageAttachment attachment : msg.images) {
                    View item = LayoutInflater.from(holder.itemView.getContext())
                            .inflate(R.layout.item_ai_image, holder.imagesContainer, false);

                    ImageView imageView = item.findViewById(R.id.imageView);
                    TextView captionView = item.findViewById(R.id.captionView);

                    Bitmap bitmap = decodeBase64ToBitmap(attachment.imageDataBase64);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                    } else {
                        imageView.setVisibility(View.GONE);
                    }

                    if (attachment.caption != null && !attachment.caption.trim().isEmpty()) {
                        captionView.setText(attachment.caption);
                        captionView.setVisibility(View.VISIBLE);
                    } else {
                        captionView.setVisibility(View.GONE);
                    }

                    holder.imagesContainer.addView(item);
                }
            }

            holder.imagesContainer.setVisibility(
                    (msg.images != null && !msg.images.isEmpty()) ? View.VISIBLE : View.GONE
            );
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        LinearLayout imagesContainer;
        LinearLayout feedbackContainer;
        ImageButton likeButton;
        ImageButton dislikeButton;
        TextView feedbackPromptText;
        LinearLayout feedbackChipContainer;
        TextView feedbackStatusText;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewMessage);
            imagesContainer = itemView.findViewById(R.id.imagesContainer);
            feedbackContainer = itemView.findViewById(R.id.feedbackContainer);
            likeButton = itemView.findViewById(R.id.buttonLike);
            dislikeButton = itemView.findViewById(R.id.buttonDislike);
            feedbackPromptText = itemView.findViewById(R.id.feedbackPromptText);
            feedbackChipContainer = itemView.findViewById(R.id.feedbackChipContainer);
            feedbackStatusText = itemView.findViewById(R.id.feedbackStatusText);
        }
    }

    private static Bitmap decodeBase64ToBitmap(String base64) {
        if (base64 == null) return null;
        try {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Converts lightweight markdown (bold + bullets) into TextView spans for richer replies.
    private static CharSequence formatRichText(String raw, int lineHeight) {
        if (TextUtils.isEmpty(raw)) {
            return "";
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = raw.split("\\n", -1);
        int defaultGapWidth = Math.max(lineHeight / 2, 16);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.trim().isEmpty()) {
                // Preserve blank lines to match incoming layout expectations.
                builder.append('\n');
                continue;
            }

            boolean isBullet = line.trim().startsWith("- ") || line.trim().startsWith("* ");
            String content = line;

            if (isBullet) {
                content = line.trim().substring(2).trim();
            }

            int start = builder.length();
            String plainContent = stripMarkdownBold(content);
            builder.append(plainContent);

            applyBoldMarkdownSpans(builder, start, content, plainContent);

            if (isBullet) {
                int end = builder.length();
                builder.setSpan(new BulletSpan(defaultGapWidth), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }

        // Remove trailing newline if the source ended with blank lines to avoid extra space at the bottom.
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\n') {
            builder.delete(builder.length() - 1, builder.length());
        }

        return builder;
    }

    private static void applyBoldMarkdownSpans(SpannableStringBuilder builder, int startOffset, String markdownContent, String plainContent) {
        int markdownIndex = 0;
        int plainIndex = 0;

        while (markdownIndex < markdownContent.length()) {
            int open = markdownContent.indexOf("**", markdownIndex);
            if (open == -1) {
                break;
            }
            int close = markdownContent.indexOf("**", open + 2);
            if (close == -1) {
                break;
            }

            String before = markdownContent.substring(markdownIndex, open);
            plainIndex += stripMarkdownBold(before).length();

            String boldSegment = markdownContent.substring(open + 2, close);
            int boldStart = startOffset + plainIndex;
            plainIndex += stripMarkdownBold(boldSegment).length();
            int boldEnd = startOffset + plainIndex;

            if (boldStart < boldEnd && boldEnd <= startOffset + plainContent.length()) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), boldStart, boldEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            markdownIndex = close + 2;
        }
    }

    private static String stripMarkdownBold(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while (idx < text.length()) {
            int open = text.indexOf("**", idx);
            if (open == -1) {
                sb.append(text.substring(idx));
                break;
            }
            sb.append(text, idx, open);
            idx = open + 2;
            int close = text.indexOf("**", idx);
            if (close == -1) {
                sb.append(text.substring(open));
                break;
            }
            sb.append(text, idx, close);
            idx = close + 2;
        }
        return sb.toString();
    }

    private void bindFeedback(ChatViewHolder holder, Message message) {
        if (holder.feedbackContainer == null) {
            return;
        }

        if (message.isUser || !message.allowFeedback) {
            holder.feedbackContainer.setVisibility(View.GONE);
            if (holder.feedbackStatusText != null) {
                holder.feedbackStatusText.setVisibility(View.GONE);
            }
            if (holder.feedbackPromptText != null) {
                holder.feedbackPromptText.setVisibility(View.GONE);
            }
            if (holder.feedbackChipContainer != null) {
                holder.feedbackChipContainer.setVisibility(View.GONE);
                holder.feedbackChipContainer.removeAllViews();
            }
            return;
        }

        boolean hasSelection = message.feedbackState != FeedbackState.NONE;

        holder.feedbackContainer.setVisibility(hasSelection ? View.GONE : View.VISIBLE);

        int activeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.ai_feedback_active);
        int inactiveColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.ai_feedback_inactive);

        updateButtonTint(holder.likeButton, false, activeColor, inactiveColor);
        updateButtonTint(holder.dislikeButton, false, activeColor, inactiveColor);

        boolean buttonsEnabled = !hasSelection;
        holder.likeButton.setEnabled(buttonsEnabled);
        holder.dislikeButton.setEnabled(buttonsEnabled);

        View.OnClickListener likeListener = v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            if (message.feedbackState != FeedbackState.NONE) {
                return;
            }
            FeedbackState targetState = FeedbackState.LIKE;
            if (feedbackListener != null) {
                feedbackListener.onFeedbackSelected(adapterPosition, message, targetState);
            }
        };

        View.OnClickListener dislikeListener = v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            if (message.feedbackState != FeedbackState.NONE) {
                return;
            }
            FeedbackState targetState = FeedbackState.DISLIKE;
            if (feedbackListener != null) {
                feedbackListener.onFeedbackSelected(adapterPosition, message, targetState);
            }
        };

        holder.likeButton.setOnClickListener(likeListener);
        holder.dislikeButton.setOnClickListener(dislikeListener);

        if (holder.feedbackStatusText != null) {
            if (hasSelection) {
                int statusColor = message.feedbackState == FeedbackState.LIKE
                        ? activeColor
                        : inactiveColor;
                holder.feedbackStatusText.setTextColor(statusColor);
                holder.feedbackStatusText.setText(
                        message.feedbackState == FeedbackState.LIKE
                                ? holder.itemView.getContext().getString(R.string.chat_feedback_thanks_message)
                                : holder.itemView.getContext().getString(R.string.chat_feedback_sorry_message)
                );
                holder.feedbackStatusText.setVisibility(View.VISIBLE);
            } else {
                holder.feedbackStatusText.setVisibility(View.GONE);
            }
        }

        bindCategoricalFeedback(holder, message);
    }

    private void bindCategoricalFeedback(ChatViewHolder holder, Message message) {
        if (holder.feedbackChipContainer == null || holder.feedbackPromptText == null) {
            return;
        }

        if (message.feedbackState != FeedbackState.DISLIKE || !message.showCategorical || message.categoricalOptionsRemoved) {
            holder.feedbackPromptText.setVisibility(View.GONE);
            holder.feedbackChipContainer.setVisibility(View.GONE);
            holder.feedbackChipContainer.removeAllViews();
            return;
        }

        holder.feedbackPromptText.setVisibility(View.VISIBLE);
        holder.feedbackChipContainer.setVisibility(View.VISIBLE);
        holder.feedbackChipContainer.removeAllViews();

        for (FeedbackOption option : FEEDBACK_OPTIONS) {
            if (!isOptionVisible(message, option)) {
                continue;
            }

            TextView chip = createFeedbackChip(holder, option.label);
            boolean alreadySelected = message.categoricalSelections.contains(option.value);
            chip.setEnabled(!alreadySelected);
            chip.setAlpha(alreadySelected ? 0.6f : 1.0f);

            chip.setOnClickListener(v -> {
                if (feedbackListener != null && !alreadySelected) {
                    int adapterPosition = holder.getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        feedbackListener.onFeedbackCategoricalSelected(adapterPosition, message, option.value);
                    }
                }
            });

            holder.feedbackChipContainer.addView(chip);
        }
    }

    private TextView createFeedbackChip(ChatViewHolder holder, String label) {
        TextView chip = new TextView(holder.itemView.getContext());
        chip.setText(label);
        chip.setAllCaps(false);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        int paddingHorizontal = dpToPx(holder, 12);
        int paddingVertical = dpToPx(holder, 6);
        chip.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        chip.setBackgroundResource(R.drawable.bg_feedback_chip);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dpToPx(holder, 8));
        params.bottomMargin = dpToPx(holder, 8);
        chip.setLayoutParams(params);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
        return chip;
    }

    private int dpToPx(ChatViewHolder holder, int dp) {
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private boolean isOptionVisible(Message message, FeedbackOption candidate) {
        if (candidate.group == null) {
            return true;
        }
        if (candidate.group.equals("independent")) {
            return true;
        }
        for (String selected : message.categoricalSelections) {
            for (FeedbackOption defined : FEEDBACK_OPTIONS) {
                if (defined.value.equals(selected) && defined.group.equals(candidate.group) && !defined.value.equals(candidate.value)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateButtonTint(ImageButton button, boolean active, int activeColor, int inactiveColor) {
        if (button == null) {
            return;
        }
        int color = active ? activeColor : inactiveColor;
        button.setColorFilter(color);
        button.setAlpha(active ? 1.0f : 0.7f);
    }
}
