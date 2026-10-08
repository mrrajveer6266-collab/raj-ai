package com.raj.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONObject;

public class MainActivity extends Activity {

    LinearLayout messages;
    EditText input;
    TextToSpeech textToSpeech;
    SpeechRecognizer speechRecognizer;

    SpeakerState activeSpeaker;
    TextView activeSpeakerButton;

    private static class SpeakerState {
        String[] chunks;
        int index = 0;
        boolean paused = false;
        boolean speaking = false;
        String id;
    }
    Intent speechIntent;
    TextView micButton;
    TextView voiceTranscript;
    boolean listening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);
        root.setBackgroundColor(Color.rgb(10, 10, 12));

        // Top header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("RAJ AI  ✦");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setPadding(4, 8, 8, 12);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1);

        header.addView(title, titleParams);

        // Mic icon only
        micButton = new TextView(this);
        micButton.setText("🎙️");
        micButton.setTextSize(25);
        micButton.setGravity(Gravity.CENTER);
        micButton.setPadding(14, 10, 14, 10);
        micButton.setTextColor(Color.WHITE);
        micButton.setBackgroundColor(Color.rgb(32, 32, 38));

        header.addView(micButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

        root.addView(header);

        // Live voice transcript
        voiceTranscript = new TextView(this);
        voiceTranscript.setText("");
        voiceTranscript.setTextSize(16);
        voiceTranscript.setTextColor(Color.rgb(185, 185, 195));
        voiceTranscript.setMaxLines(4);
        voiceTranscript.setPadding(6, 0, 6, 10);
        root.addView(voiceTranscript);

        // Welcome
        TextView welcome = new TextView(this);
        welcome.setText("नमस्ते 👋\nमैं RAJ AI हूँ।");
        welcome.setTextSize(18);
        welcome.setTextColor(Color.rgb(225, 225, 230));
        welcome.setPadding(4, 0, 4, 16);
        root.addView(welcome);

        // Horizontal feature scroller
        HorizontalScrollView featureScroll = new HorizontalScrollView(this);
        featureScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout featureRow = new LinearLayout(this);
        featureRow.setOrientation(LinearLayout.HORIZONTAL);
        featureRow.setPadding(0, 0, 0, 14);

        String[] featureNames = {
                "🖼️ Image",
                "🎬 Video",
                "📎 Files",
                "🌐 Web",
                "⚡ Live",
                "🛠️ Tools"
        };

        for (String name : featureNames) {
            TextView card = new TextView(this);
            card.setText(name);
            card.setTextSize(14);
            card.setTextColor(Color.WHITE);
            card.setGravity(Gravity.CENTER);
            card.setSingleLine(true);
            card.setPadding(18, 16, 18, 16);
            card.setBackgroundColor(Color.rgb(28, 28, 34));

            LinearLayout.LayoutParams cp =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            cp.setMargins(5, 3, 5, 3);

            featureRow.addView(card, cp);
        }

        featureScroll.addView(featureRow);
        root.addView(featureScroll);

        // Messages
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(4, 4, 4, 4);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(10, 10, 12));
        scroll.addView(messages);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        root.addView(scroll, scrollParams);

        // Bottom composer
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(6, 7, 6, 10);
        bottom.setBackgroundColor(Color.rgb(30, 30, 34));

        input = new EditText(this);
        input.setHint("Ask RAJ AI...");
        input.setHintTextColor(Color.rgb(155, 155, 165));
        input.setTextColor(Color.WHITE);
        input.setTextSize(17);
        input.setPadding(20, 12, 20, 12);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setBackgroundColor(Color.rgb(30, 30, 34));

        bottom.addView(input,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                ));

        root.addView(bottom);

        setContentView(root);

        // Text-to-speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.setSpeechRate(1.0f);
            }
        });

        // Speech recognition
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

            speechIntent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault().toLanguageTag()
            );
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
            );
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
            );

            speechRecognizer.setRecognitionListener(
                    new RecognitionListener() {

                @Override
                public void onReadyForSpeech(Bundle params) {
                    listening = true;
                    micButton.setText("🎙️");
                    voiceTranscript.setText("सुन रहा हूँ…");
                }

                @Override
                public void onBeginningOfSpeech() {
                    voiceTranscript.setText("बोलिए…");
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    if (listening) {
                        micButton.setText("🎙️");
                    }
                }

                @Override
                public void onError(int error) {
                    listening = false;
                    micButton.setText("🎙️");

                    if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                        error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        voiceTranscript.setText("");
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    if (!listening) return;

                    listening = false;
                    micButton.setText("🎙️");

                    ArrayList<String> matches =
                            results.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                            );

                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0).trim();

                        if (!text.isEmpty()) {
                            voiceTranscript.setText(
                                    limitTranscript(text)
                            );
                            sendMessage(text);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    if (!listening) return;

                    ArrayList<String> partial =
                            partialResults.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                            );

                    if (partial != null && !partial.isEmpty()) {
                        voiceTranscript.setText(
                                limitTranscript(partial.get(0))
                        );
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }

        // Mic tap = start/stop listening
        micButton.setOnClickListener(v -> toggleListening());

        // Keyboard Send
        input.setOnEditorActionListener(
                (v, actionId, event) -> {

            if (actionId != EditorInfo.IME_ACTION_SEND) {
                return false;
            }

            String text = input.getText().toString().trim();

            if (text.isEmpty()) {
                return true;
            }

            sendMessage(text);
            return true;
        });

        // Keep composer/chat visible with keyboard
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scroll.postDelayed(
                        () -> scroll.fullScroll(
                                ScrollView.FOCUS_DOWN
                        ),
                        120
                );
            }
        });

        input.setOnClickListener(v -> {
            scroll.postDelayed(
                    () -> scroll.fullScroll(
                            ScrollView.FOCUS_DOWN
                    ),
                    120
            );
        });

        scroll.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            scroll.postDelayed(
                    () -> scroll.fullScroll(
                            ScrollView.FOCUS_DOWN
                    ),
                    60
            );
        });
    }

    private void toggleListening() {
        if (speechRecognizer == null) {
            Toast.makeText(
                    this,
                    "Voice recognition उपलब्ध नहीं है।",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1001
            );
            return;
        }

        if (listening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void startListening() {
        try {
            listening = true;
            voiceTranscript.setText("सुन रहा हूँ…");
            speechRecognizer.startListening(speechIntent);
        } catch (Exception e) {
            listening = false;
            voiceTranscript.setText("");
            Toast.makeText(
                    this,
                    "Mic शुरू नहीं हो पाया।",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void stopListening() {
        listening = false;

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }

        voiceTranscript.setText("");
    }

    private String limitTranscript(String text) {
        if (text == null) return "";

        text = text.trim();

        if (text.length() > 260) {
            text = text.substring(0, 260) + "…";
        }

        return text;
    }

    private void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        text = text.trim();

        addMessage("You: " + text);
        input.setText("");
        input.setEnabled(false);

        final String finalText = text;

        new Thread(() -> {
            try {
                String answer = askBackend(finalText);

                runOnUiThread(() -> {
                    addMessage("RAJ AI: " + answer);
                    speakAnswer(answer);
                    input.setEnabled(true);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    addMessage(
                            "RAJ AI: Connection error — "
                                    + e.getMessage()
                    );
                    input.setEnabled(true);
                });
            }
        }).start();
    }

    private void scrollMessagesToBottom() {
        if (messages == null) return;

        messages.post(() -> {
            ViewParent parent = messages.getParent();

            if (parent instanceof ScrollView) {
                ((ScrollView) parent).fullScroll(
                        ScrollView.FOCUS_DOWN
                );
            }
        });
    }

    private String[] splitForSpeech(String text) {
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) {
            return new String[0];
        }

        String[] parts = clean.split("(?<=[.!?।])\\s+|\\n+");
        ArrayList<String> result = new ArrayList<>();

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            // बहुत लंबे हिस्से को छोटे chunks में बाँटें
            while (part.length() > 220) {
                int cut = part.lastIndexOf(' ', 220);
                if (cut < 80) cut = 220;
                result.add(part.substring(0, cut).trim());
                part = part.substring(cut).trim();
            }

            if (!part.isEmpty()) {
                result.add(part);
            }
        }

        return result.toArray(new String[0]);
    }

    private void resetActiveSpeaker() {
        if (activeSpeakerButton != null) {
            activeSpeakerButton.setText("🔊");
        }

        if (activeSpeaker != null) {
            activeSpeaker.speaking = false;
            activeSpeaker.paused = false;
        }

        activeSpeaker = null;
        activeSpeakerButton = null;
    }

    private void pauseSpeaker(SpeakerState state, TextView button) {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        state.paused = true;
        state.speaking = false;
        button.setText("▶️");
    }

    private void speakSpeakerChunk(final SpeakerState state, final TextView button) {
        if (textToSpeech == null ||
                state == null ||
                state.paused ||
                state.index >= state.chunks.length) {
            return;
        }

        String chunk = state.chunks[state.index].trim();
        if (chunk.isEmpty()) {
            state.index++;
            speakSpeakerChunk(state, button);
            return;
        }

        Locale language = detectLanguage(chunk);

        int result = textToSpeech.setLanguage(language);

        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech.setLanguage(Locale.getDefault());
        }

        state.speaking = true;
        button.setText("⏸️");

        textToSpeech.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            if (activeSpeaker != state || state.paused) {
                                return;
                            }

                            state.index++;

                            if (state.index < state.chunks.length) {
                                speakSpeakerChunk(state, button);
                            } else {
                                button.setText("🔊");
                                state.speaking = false;
                                activeSpeaker = null;
                                activeSpeakerButton = null;
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            if (activeSpeaker == state) {
                                button.setText("🔊");
                                state.speaking = false;
                                activeSpeaker = null;
                                activeSpeakerButton = null;
                            }
                        });
                    }
                }
        );

        textToSpeech.speak(
                chunk,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "raj-ai-answer-" + state.id + "-" + state.index
        );
    }

    private void handleSpeakerClick(final TextView button, final String text) {
        if (textToSpeech == null ||
                text == null ||
                text.trim().isEmpty()) {
            return;
        }

        // यही speaker पहले से active है
        if (activeSpeakerButton == button && activeSpeaker != null) {
            if (activeSpeaker.speaking) {
                pauseSpeaker(activeSpeaker, button);
                return;
            }

            if (activeSpeaker.paused) {
                activeSpeaker.paused = false;
                speakSpeakerChunk(activeSpeaker, button);
                return;
            }
        }

        // दूसरा speaker दबाया गया
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        resetActiveSpeaker();

        SpeakerState state = new SpeakerState();
        state.chunks = splitForSpeech(text);
        state.id = String.valueOf(System.currentTimeMillis());

        if (state.chunks.length == 0) {
            return;
        }

        activeSpeaker = state;
        activeSpeakerButton = button;

        speakSpeakerChunk(state, button);
    }

    private void speakAnswer(String text) {
        // AI message आने पर automatic speech नहीं चलेगी।
        // User message के साथ दिए गए 🔊 button से speech शुरू होगी।
    }

    private Locale detectLanguage(String text) {

        // Hindi / Marathi / Nepali style Devanagari
        if (text.matches(".*[\\u0900-\\u097F].*")) {
            return Locale.forLanguageTag("hi-IN");
        }

        // Bengali
        if (text.matches(".*[\\u0980-\\u09FF].*")) {
            return Locale.forLanguageTag("bn-IN");
        }

        // Gujarati
        if (text.matches(".*[\\u0A80-\\u0AFF].*")) {
            return Locale.forLanguageTag("gu-IN");
        }

        // Punjabi
        if (text.matches(".*[\\u0A00-\\u0A7F].*")) {
            return Locale.forLanguageTag("pa-IN");
        }

        // Tamil
        if (text.matches(".*[\\u0B80-\\u0BFF].*")) {
            return Locale.forLanguageTag("ta-IN");
        }

        // Telugu
        if (text.matches(".*[\\u0C00-\\u0C7F].*")) {
            return Locale.forLanguageTag("te-IN");
        }

        // Kannada
        if (text.matches(".*[\\u0C80-\\u0CFF].*")) {
            return Locale.forLanguageTag("kn-IN");
        }

        // Malayalam
        if (text.matches(".*[\\u0D00-\\u0D7F].*")) {
            return Locale.forLanguageTag("ml-IN");
        }

        // Default Latin text
        return Locale.ENGLISH;
    }

    private String askBackend(String message) throws Exception {

        URL url = new URL(
                "https://raj-ai-juvm.onrender.com/chat"
        );

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        JSONObject request = new JSONObject();
        request.put("message", message);

        try (OutputStream os =
                     connection.getOutputStream()) {

            os.write(
                    request.toString()
                            .getBytes("UTF-8")
            );
        }

        int status = connection.getResponseCode();

        InputStream stream =
                status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                stream,
                                "UTF-8"
                        )
                );

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();

        JSONObject json =
                new JSONObject(response.toString());

        if (!json.optBoolean("ok", false)) {
            throw new Exception(
                    json.optString(
                            "error",
                            "Backend error"
                    )
            );
        }

        return json.optString(
                "answer",
                "कोई जवाब नहीं मिला।"
        );
    }

    private String formatAIMessage(String text) {
        if (text == null) return "";

        String clean = text
                .replace("\r", "")
                .trim();

        if (clean.isEmpty()) return "";

        String[] lines = clean.split("\n");
        StringBuilder out = new StringBuilder();

        for (String line : lines) {
            String x = line.trim();

            if (x.isEmpty()) {
                if (out.length() > 0 &&
                        out.charAt(out.length() - 1) != '\n') {
                    out.append('\n');
                }
                continue;
            }

            // Markdown bullets को साफ bullet में दिखाएँ
            if (x.startsWith("- ")) {
                x = "• " + x.substring(2).trim();
            } else if (x.startsWith("* ")) {
                x = "• " + x.substring(2).trim();
            }

            if (out.length() > 0) {
                out.append('\n');
            }

            out.append(x);
        }

        return out.toString().trim();
    }

    private void addMessage(String text) {
        boolean isAI = text != null && text.startsWith("RAJ AI:");

        String displayText = text == null ? "" : text;

        if (isAI) {
            displayText = "RAJ AI:\n" +
                    formatAIMessage(text.substring("RAJ AI:".length()));
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(8, 6, 8, 6);

        TextView message = new TextView(this);
        message.setText(displayText);
        message.setTextSize(18);
        message.setTextColor(Color.rgb(235, 235, 240));
        message.setPadding(12, 10, 8, 10);
        message.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        row.addView(message);

        if (isAI) {
            TextView speaker = new TextView(this);
            speaker.setText("🔊");
            speaker.setTextSize(22);
            speaker.setGravity(Gravity.CENTER);
            speaker.setTextColor(Color.WHITE);
            speaker.setPadding(10, 10, 10, 10);

            speaker.setOnClickListener(v ->
                    handleSpeakerClick(
                            speaker,
                            text.substring("RAJ AI:".length()).trim()
                    )
            );

            row.addView(
                    speaker,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }

        messages.addView(row);

        scrollMessagesToBottom();
    }

    @Override
    public void onDestroy() {

        listening = false;

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        super.onDestroy();
    }
}
