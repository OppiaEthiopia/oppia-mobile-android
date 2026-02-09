package org.digitalcampus.oppia.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.digitalcampus.oppia.application.SessionManager;
import org.digitalcampus.oppia.database.DbHelper;
import org.digitalcampus.oppia.exception.UserNotFoundException;
import org.digitalcampus.oppia.model.User;
import org.digitalcampus.oppia.utils.AiChatConfig;
import org.digitalcampus.oppia.utils.HTTPClientUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.learning.BuildConfig;
import org.digitalcampus.oppia.adapter.ChatAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import androidx.preference.PreferenceManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class CustomAIChatFragment extends Fragment {

    private static final String TAG = "CustomAIChat";

    private static final String PREF_AI_CHAT_ID = "pref_ai_chat_id";

    private List<ChatAdapter.Message> messages = new ArrayList<>();
    private ChatAdapter adapter;

    // Make recyclerView a class member
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private EditText editText;
    private Button sendButton;
    private CheckBox checkBoxUseReferenceDocument;
    private ProgressBar progressLoading;

    private boolean isLoading = false;
    private ChatAdapter.Message thinkingMsg;

    public CustomAIChatFragment() {
        super(R.layout.fragment_custom_ai_chat);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewMessages); // ✅ now class-level
        editText = view.findViewById(R.id.editTextMessage);
        sendButton = view.findViewById(R.id.buttonSend);
        checkBoxUseReferenceDocument = view.findViewById(R.id.checkBoxUseReferenceDocument);
        progressLoading = view.findViewById(R.id.progressLoading);

        adapter = new ChatAdapter(messages, new ChatAdapter.OnFeedbackSelectedListener() {
            @Override
            public void onFeedbackSelected(int position, ChatAdapter.Message message, ChatAdapter.FeedbackState newState) {
                CustomAIChatFragment.this.onFeedbackSelected(position, message, newState);
            }

            @Override
            public void onFeedbackCategoricalSelected(int position, ChatAdapter.Message message, String optionValue) {
                CustomAIChatFragment.this.onFeedbackCategoricalSelected(position, message, optionValue);
            }
        });
        layoutManager = new LinearLayoutManager(requireContext());
        // Chat UX: keep the list anchored to the bottom as it grows.
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        addWelcomeMessage();

        sendButton.setOnClickListener(v -> {
            String userMsg = editText.getText().toString().trim();
            if (!userMsg.isEmpty()) {
                if (isLoading) {
                    return;
                }

                List<String> docTypes = new ArrayList<>();
                // Backend contract requires document_types to be non-empty.
                docTypes.add("primary");
                if (checkBoxUseReferenceDocument != null && checkBoxUseReferenceDocument.isChecked()) {
                    docTypes.add("secondary");
                }

                ensureAccessTokenThenSend(userMsg, docTypes);
            }
        });
    }

    private void addWelcomeMessage() {
        if (!messages.isEmpty()) {
            return;
        }

        String displayName = SessionManager.getUserDisplayName(requireContext());
        if (TextUtils.isEmpty(displayName)) {
            displayName = SessionManager.getUsername(requireContext());
        }

        final String welcomeText;
        if (TextUtils.isEmpty(displayName)) {
            welcomeText = getString(R.string.ai_chat_welcome_generic);
        } else {
            welcomeText = getString(R.string.ai_chat_welcome_with_name, displayName);
        }

        ChatAdapter.Message welcomeMessage = new ChatAdapter.Message(welcomeText, false);
        welcomeMessage.allowFeedback = false;
        messages.add(welcomeMessage);
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void ensureAccessTokenThenSend(String userMsg, List<String> docTypes) {
        String token = getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(requireContext(), "No token available. Please log in to the app first.", Toast.LENGTH_LONG).show();
            return;
        }

        sendToBackend(userMsg, docTypes, token);
    }

    private void sendToBackend(String userMsg, List<String> docTypes, String token) {
        // Add user message to UI immediately.
        messages.add(new ChatAdapter.Message(userMsg, true));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        editText.setText("");

        // Add thinking message + loading state.
        thinkingMsg = new ChatAdapter.Message("Loading...", false);
        thinkingMsg.allowFeedback = false;
        messages.add(thinkingMsg);
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        setLoading(true);

        String chatId = getOrCreateChatId();
        JSONObject json = new JSONObject();
        try {
            json.put("chat_id", chatId);
            json.put("message", userMsg);
            JSONArray types = new JSONArray();
            for (String t : docTypes) types.put(t);
            json.put("document_types", types);
            json.put("document_programs", JSONObject.NULL);
            json.put("document_modules", JSONObject.NULL);
        } catch (JSONException e) {
            setLoading(false);
            Toast.makeText(requireContext(), "Failed to build request", Toast.LENGTH_SHORT).show();
            removeThinkingMessage();
            return;
        }

        OkHttpClient client = HTTPClientUtils.getClient(requireContext());
        String usernameHeaderValue = SessionManager.getUsername(requireContext());
        if (usernameHeaderValue == null) usernameHeaderValue = "";

        String emailHeaderValue = "";
        if (!usernameHeaderValue.trim().isEmpty()) {
            try {
                User u = DbHelper.getInstance(requireContext()).getUser(usernameHeaderValue);
                if (u.getEmail() != null) emailHeaderValue = u.getEmail();
            } catch (UserNotFoundException ignored) {
                // best-effort only
            }
        }
        String url = AiChatConfig.buildUrl("/chat/");
        debugLogOutboundRequest(url, token, usernameHeaderValue, emailHeaderValue, json);

        Request request = new Request.Builder()
            .url(url)
                .addHeader("Authorization", "Bearer " + token)
            // FastAPI backend (Oppia validation) may require a username header to validate api_key.
            // We send a few common variants to maximize compatibility.
            .addHeader("username", usernameHeaderValue)
            .addHeader("X-Username", usernameHeaderValue)
            .addHeader("X-Oppia-Username", usernameHeaderValue)
                // Some backends also require email context; send best-effort variants.
                .addHeader("email", emailHeaderValue)
                .addHeader("X-Email", emailHeaderValue)
                .addHeader("X-Oppia-Email", emailHeaderValue)
                .post(RequestBody.create(json.toString(), HTTPClientUtils.MEDIA_TYPE_JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThreadSafe(() -> {
                    setLoading(false);
                    removeThinkingMessage();
                    ChatAdapter.Message errorMsg = new ChatAdapter.Message("Request failed: " + e.getMessage(), false);
                    errorMsg.allowFeedback = false;
                    messages.add(errorMsg);
                    adapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                debugLogInboundResponse(response.code(), body);

                runOnUiThreadSafe(() -> {
                    setLoading(false);
                    removeThinkingMessage();
                });

                if (!response.isSuccessful()) {
                    runOnUiThreadSafe(() -> {
                        ChatAdapter.Message errorMsg = new ChatAdapter.Message(buildHttpErrorMessage(response.code(), body), false);
                        errorMsg.allowFeedback = false;
                        messages.add(errorMsg);
                        adapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                    return;
                }

                try {
                    JSONObject jsonResp = new JSONObject(body);
                    String assistantText = jsonResp.optString("response", "");

                    List<ChatAdapter.ImageAttachment> images = new ArrayList<>();
                    JSONArray imagesJson = jsonResp.optJSONArray("images");
                    if (imagesJson != null) {
                        for (int i = 0; i < imagesJson.length(); i++) {
                            JSONObject img = imagesJson.optJSONObject(i);
                            if (img == null) continue;
                            String imageData = img.optString("image_data", "");
                            String caption = img.optString("caption", "");
                            if (imageData != null && !imageData.trim().isEmpty()) {
                                images.add(new ChatAdapter.ImageAttachment(imageData, caption));
                            }
                        }
                    }

                    ChatAdapter.Message assistantMsg = new ChatAdapter.Message(assistantText, false, images);
                    String messageId = jsonResp.optString("message_id", "");
                    if (messageId.trim().isEmpty()) {
                        messageId = jsonResp.optString("id", "");
                    }
                    if (messageId.trim().isEmpty()) {
                        messageId = jsonResp.optString("response_id", "");
                    }
                    if (messageId.trim().isEmpty()) {
                        messageId = UUID.randomUUID().toString();
                    }
                    assistantMsg.messageId = messageId;

                    runOnUiThreadSafe(() -> {
                        messages.add(assistantMsg);
                        adapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                } catch (JSONException e) {
                    runOnUiThreadSafe(() -> {
                        ChatAdapter.Message parseError = new ChatAdapter.Message("Response parse error", false);
                        parseError.allowFeedback = false;
                        messages.add(parseError);
                        adapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
            }
        });
    }

    private void onFeedbackSelected(int position, ChatAdapter.Message message, ChatAdapter.FeedbackState newState) {
        if (message == null || adapter == null) {
            return;
        }
        if (!message.allowFeedback) {
            return;
        }

        ChatAdapter.FeedbackState previous = message.feedbackState;
        if (previous == newState) {
            return;
        }

        boolean previousShowCategorical = message.showCategorical;
        boolean previousCategoricalRemoved = message.categoricalOptionsRemoved;
        Set<String> previousSelections = new HashSet<>(message.categoricalSelections);
        String previousFeedbackId = message.feedbackId;

        if (newState == ChatAdapter.FeedbackState.DISLIKE) {
            message.showCategorical = true;
            message.categoricalOptionsRemoved = false;
            message.categoricalSelections.clear();
        } else {
            message.showCategorical = false;
            message.categoricalOptionsRemoved = true;
            message.categoricalSelections.clear();
        }

        message.feedbackState = newState;
        adapter.notifyItemChanged(position);
        sendFeedbackToBackend(position, message, previous, newState,
                previousShowCategorical, previousCategoricalRemoved, previousSelections, previousFeedbackId);
    }

    private void onFeedbackCategoricalSelected(int position,
                                               ChatAdapter.Message message,
                                               String optionValue) {
        if (message == null || adapter == null) {
            return;
        }
        if (!message.allowFeedback) {
            return;
        }
        if (message.feedbackState != ChatAdapter.FeedbackState.DISLIKE) {
            return;
        }
        if (message.feedbackId == null || message.feedbackId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Feedback session not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.categoricalSelections.contains(optionValue)) {
            return;
        }

        message.categoricalSelections.add(optionValue);
        adapter.notifyItemChanged(position);
        sendCategoricalFeedback(position, message, optionValue);
    }

    private void sendCategoricalFeedback(int position,
                                         ChatAdapter.Message message,
                                         String optionValue) {
        String token = getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            runOnUiThreadSafe(() -> {
                message.categoricalSelections.remove(optionValue);
                adapter.notifyItemChanged(position);
                Toast.makeText(requireContext(), "No token available. Please log in to send feedback.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("feedback_id", message.feedbackId);
            json.put("category", optionValue);
        } catch (JSONException e) {
            runOnUiThreadSafe(() -> {
                message.categoricalSelections.remove(optionValue);
                adapter.notifyItemChanged(position);
                Toast.makeText(requireContext(), "Failed to build feedback request", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        OkHttpClient client = HTTPClientUtils.getClient(requireContext());
        String url = AiChatConfig.buildUrl("/chat/feedback/categorical/");

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(json.toString(), HTTPClientUtils.MEDIA_TYPE_JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThreadSafe(() -> {
                    message.categoricalSelections.remove(optionValue);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(requireContext(), "Feedback failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                debugLogInboundResponse(response.code(), body);

                if (!response.isSuccessful()) {
                    runOnUiThreadSafe(() -> {
                        message.categoricalSelections.remove(optionValue);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(),
                                "Feedback error (" + response.code() + ")",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                runOnUiThreadSafe(() -> adapter.notifyItemChanged(position));
            }
        });
    }

    private void sendFeedbackToBackend(int position,
                                       ChatAdapter.Message message,
                                       ChatAdapter.FeedbackState previousState,
                                       ChatAdapter.FeedbackState newState,
                                       boolean previousShowCategorical,
                                       boolean previousCategoricalRemoved,
                                       Set<String> previousSelections,
                                       String previousFeedbackId) {
        String token = getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            runOnUiThreadSafe(() -> {
                message.feedbackState = previousState;
                message.showCategorical = previousShowCategorical;
                message.categoricalOptionsRemoved = previousCategoricalRemoved;
                message.categoricalSelections.clear();
                message.categoricalSelections.addAll(previousSelections);
                message.feedbackId = previousFeedbackId;
                adapter.notifyItemChanged(position);
                Toast.makeText(requireContext(), "No token available. Please log in to send feedback.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        String messageId = ensureMessageId(message);
        JSONObject json = new JSONObject();
        try {
            json.put("chat_id", getOrCreateChatId());
            json.put("message_id", messageId);
            json.put("feedback", feedbackStateToString(newState));
            json.put("response_text", message.text != null ? message.text : "");
        } catch (JSONException e) {
            runOnUiThreadSafe(() -> {
                message.feedbackState = previousState;
                adapter.notifyItemChanged(position);
                Toast.makeText(requireContext(), "Failed to build feedback request", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        OkHttpClient client = HTTPClientUtils.getClient(requireContext());
        String usernameHeaderValue = SessionManager.getUsername(requireContext());
        if (usernameHeaderValue == null) usernameHeaderValue = "";

        String emailHeaderValue = "";
        if (!usernameHeaderValue.trim().isEmpty()) {
            try {
                User u = DbHelper.getInstance(requireContext()).getUser(usernameHeaderValue);
                if (u.getEmail() != null) emailHeaderValue = u.getEmail();
            } catch (UserNotFoundException ignored) {
                // optional metadata only
            }
        }

        String url = AiChatConfig.buildUrl("/chat/feedback/");
        debugLogOutboundRequest(url, token, usernameHeaderValue, emailHeaderValue, json);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("username", usernameHeaderValue)
                .addHeader("X-Username", usernameHeaderValue)
                .addHeader("X-Oppia-Username", usernameHeaderValue)
                .addHeader("email", emailHeaderValue)
                .addHeader("X-Email", emailHeaderValue)
                .addHeader("X-Oppia-Email", emailHeaderValue)
                .post(RequestBody.create(json.toString(), HTTPClientUtils.MEDIA_TYPE_JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThreadSafe(() -> {
                    message.feedbackState = previousState;
                    message.showCategorical = previousShowCategorical;
                    message.categoricalOptionsRemoved = previousCategoricalRemoved;
                    message.categoricalSelections.clear();
                    message.categoricalSelections.addAll(previousSelections);
                    message.feedbackId = previousFeedbackId;
                    adapter.notifyItemChanged(position);
                    Toast.makeText(requireContext(), "Feedback failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                debugLogInboundResponse(response.code(), body);

                if (!response.isSuccessful()) {
                    runOnUiThreadSafe(() -> {
                        message.feedbackState = previousState;
                        message.showCategorical = previousShowCategorical;
                        message.categoricalOptionsRemoved = previousCategoricalRemoved;
                        message.categoricalSelections.clear();
                        message.categoricalSelections.addAll(previousSelections);
                        message.feedbackId = previousFeedbackId;
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(),
                                "Feedback error (" + response.code() + ")",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                if (body != null && !body.trim().isEmpty()) {
                    try {
                        JSONObject jsonResp = new JSONObject(body);
                        String updatedMessageId = jsonResp.optString("message_id", "");
                        if (!updatedMessageId.trim().isEmpty()) {
                            message.messageId = updatedMessageId;
                        }
                        String feedbackId = jsonResp.optString("feedback_id",
                                jsonResp.optString("saved_feedback_id", ""));
                        if (!feedbackId.trim().isEmpty()) {
                            message.feedbackId = feedbackId;
                        } else if (newState == ChatAdapter.FeedbackState.DISLIKE) {
                            // Ensure we have a reference ID even if backend omits it.
                            message.feedbackId = ensureMessageId(message) + "-local";
                        }
                    } catch (JSONException ignored) {
                        // feedback response not strictly required to parse
                    }
                }
            }
        });
    }

    private String feedbackStateToString(ChatAdapter.FeedbackState state) {
        if (state == ChatAdapter.FeedbackState.LIKE) {
            return "like";
        }
        if (state == ChatAdapter.FeedbackState.DISLIKE) {
            return "dislike";
        }
        return "none";
    }

    private String ensureMessageId(ChatAdapter.Message message) {
        if (message.messageId == null || message.messageId.trim().isEmpty()) {
            message.messageId = UUID.randomUUID().toString();
        }
        return message.messageId;
    }

    private void scrollToBottom() {
        if (recyclerView == null || adapter == null || layoutManager == null) return;
        recyclerView.post(() -> {
            if (adapter == null || layoutManager == null) return;
            int last = adapter.getItemCount() - 1;
            if (last < 0) return;

            layoutManager.scrollToPositionWithOffset(last, 0);
            layoutManager.smoothScrollToPosition(recyclerView, null, last);
        });
    }

    private void debugLogOutboundRequest(String url, String token, String username, String email, JSONObject payload) {
        if (!BuildConfig.DEBUG) return;

        Log.d(TAG, "POST " + url);
        Log.d(TAG, "username=" + safe(username) + ", email=" + safe(email));
        Log.d(TAG, "token=" + maskToken(token));
        Log.d(TAG, "payload=" + (payload != null ? payload.toString() : "<null>"));
    }

    private void debugLogInboundResponse(int code, String body) {
        if (!BuildConfig.DEBUG) return;

        String preview = body;
        if (preview == null) preview = "";
        // Avoid huge logs; keep it readable.
        if (preview.length() > 1500) preview = preview.substring(0, 1500) + "…";
        Log.d(TAG, "Response code=" + code + ", body=" + preview);
    }

    private String maskToken(String token) {
        if (token == null) return "<null>";
        String t = token.trim();
        if (t.isEmpty()) return "<empty>";
        if (t.length() <= 12) return "<len=" + t.length() + ">";
        return t.substring(0, 6) + "…" + t.substring(t.length() - 4);
    }

    private String safe(String value) {
        return value == null ? "<null>" : value;
    }

    private String buildHttpErrorMessage(int code, String body) {
        String snippet = "";
        if (body != null) {
            snippet = body.trim();
            // Prefer just the first line for big tracebacks.
            int newline = snippet.indexOf('\n');
            if (newline >= 0) snippet = snippet.substring(0, newline).trim();
            if (snippet.length() > 200) snippet = snippet.substring(0, 200) + "…";
        }

        if (snippet.isEmpty()) {
            return "Error (" + code + ")";
        }
        return "Error (" + code + "): " + snippet;
    }

    private void removeThinkingMessage() {
        if (thinkingMsg == null) return;
        int index = messages.indexOf(thinkingMsg);
        if (index != -1) {
            messages.remove(index);
            adapter.notifyItemRemoved(index);
        }
        thinkingMsg = null;
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        if (progressLoading != null) {
            progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (sendButton != null) sendButton.setEnabled(!loading);
        if (editText != null) editText.setEnabled(!loading);
        if (checkBoxUseReferenceDocument != null) checkBoxUseReferenceDocument.setEnabled(!loading);
    }

    private void runOnUiThreadSafe(Runnable action) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(action);
    }

    private String getOrCreateChatId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String existing = prefs.getString(PREF_AI_CHAT_ID, null);
        if (existing != null && !existing.trim().isEmpty()) return existing;

        String chatId = UUID.randomUUID().toString();
        prefs.edit().putString(PREF_AI_CHAT_ID, chatId).apply();
        return chatId;
    }

    private String getAccessToken() {
        String username = SessionManager.getUsername(requireContext());
        if (username == null || username.trim().isEmpty()) return null;

        try {
            User user = DbHelper.getInstance(requireContext()).getUser(username);
            return user.getApiKey();
        } catch (UserNotFoundException e) {
            return null;
        }
    }
}
