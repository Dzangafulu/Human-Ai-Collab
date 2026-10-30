package com.hacms.app;

import androidx.annotation.NonNull;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;
import java.util.concurrent.Executor;

/** Firebase AI Logic: the Gemini API credential remains on Google's server, not in this APK. */
public final class GeminiAiClient {
    // Supported Gemini Developer API free-tier model. Check availability/quota in AI Studio.
    public static final String MODEL = "gemini-3.5-flash-lite";

    public interface Callback {
        void success(@NonNull String answer);
        void failure(@NonNull Throwable error);
    }

    private final GenerativeModelFutures model;

    public GeminiAiClient() {
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(MODEL);
        model = GenerativeModelFutures.from(ai);
    }

    public void ask(String instructionsAndQuestion, Executor callbackExecutor, Callback callback) {
        Content content = new Content.Builder().addText(instructionsAndQuestion).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse result) {
                String text = result == null ? null : result.getText();
                if (text == null || text.trim().isEmpty()) {
                    callback.failure(new IllegalStateException("AI returned no text"));
                } else {
                    callback.success(text.trim());
                }
            }
            @Override public void onFailure(@NonNull Throwable t) { callback.failure(t); }
        }, callbackExecutor);
    }
}
