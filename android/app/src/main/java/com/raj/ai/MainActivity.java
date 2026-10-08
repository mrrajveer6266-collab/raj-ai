package com.raj.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Base64;
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
    AndroidBridge androidBridge;

    private android.net.Uri pendingImageUri;
    private String pendingImageMimeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidBridge = new AndroidBridge(this);

        getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        int bg = Color.rgb(10, 15, 29);
        int panel = Color.rgb(18, 24, 40);
        int panel2 = Color.rgb(23, 30, 49);
        int white = Color.rgb(245, 247, 255);
        int muted = Color.rgb(155, 165, 185);
        int blue = Color.rgb(70, 150, 255);
        int purple = Color.rgb(150, 85, 255);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 14, 16, 10);
        root.setBackgroundColor(bg);

        // =========================================================
        // HEADER
        // =========================================================
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView menu = new TextView(this);
        menu.setText("☰");
        menu.setTextSize(23);
        menu.setTextColor(white);
        menu.setGravity(Gravity.CENTER);
        menu.setPadding(8, 8, 14, 8);

        TextView title = new TextView(this);
        title.setText("RAJ AI");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(white);

        TextView titleDot = new TextView(this);
        titleDot.setText("  ✦");
        titleDot.setTextSize(17);
        titleDot.setTextColor(Color.rgb(125, 150, 255));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.HORIZONTAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);
        titleBox.addView(title);
        titleBox.addView(titleDot);

        LinearLayout.LayoutParams titleBoxParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        header.addView(menu);
        header.addView(titleBox, titleBoxParams);

        TextView notification = new TextView(this);
        notification.setText("◉");
        notification.setTextSize(19);
        notification.setTextColor(Color.rgb(180, 190, 215));
        notification.setGravity(Gravity.CENTER);
        notification.setPadding(12, 8, 8, 8);

        TextView avatar = new TextView(this);
        avatar.setText("R");
        avatar.setTextSize(15);
        avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        avatar.setTextColor(Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundColor(Color.rgb(55, 65, 95));
        avatar.setPadding(12, 10, 12, 10);

        header.addView(notification);
        header.addView(avatar);

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // =========================================================
        // SCROLLABLE HOME / CHAT AREA
        // =========================================================
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(2, 10, 2, 14);

        // =========================================================
        // GREETING
        // =========================================================
        TextView welcome = new TextView(this);
        welcome.setText("Hello 👋");
        welcome.setTextSize(29);
        welcome.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        welcome.setTextColor(white);
        welcome.setPadding(4, 10, 4, 2);
        content.addView(welcome);

        TextView subtitle = new TextView(this);
        subtitle.setText("How can I assist you today?");
        subtitle.setTextSize(16);
        subtitle.setTextColor(muted);
        subtitle.setPadding(4, 0, 4, 8);
        content.addView(subtitle);

        voiceTranscript = new TextView(this);
        voiceTranscript.setText("");
        voiceTranscript.setTextSize(13);
        voiceTranscript.setTextColor(Color.rgb(135, 165, 210));
        voiceTranscript.setPadding(4, 0, 4, 8);
        content.addView(voiceTranscript);

        // =========================================================
        // AI CORE ORB
        // =========================================================
        LinearLayout orbWrap = new LinearLayout(this);
        orbWrap.setGravity(Gravity.CENTER);
        orbWrap.setPadding(0, 12, 0, 14);

        TextView orb = new TextView(this);
        orb.setText("✦");
        orb.setTextSize(42);
        orb.setGravity(Gravity.CENTER);
        orb.setTextColor(Color.WHITE);
        orb.setPadding(30, 30, 30, 30);

        android.graphics.drawable.GradientDrawable orbBg =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                Color.rgb(55, 120, 255),
                                Color.rgb(130, 70, 255),
                                Color.rgb(55, 210, 230)
                        }
                );
        orbBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        orbBg.setStroke(2, Color.rgb(115, 150, 255));
        orb.setBackground(orbBg);

        orbWrap.addView(
                orb,
                new LinearLayout.LayoutParams(120, 120)
        );
        content.addView(orbWrap);

        // subtle idle animation
        orb.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(1200)
                .withEndAction(() ->
                        orb.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(1200)
                                .start()
                )
                .start();

        TextView coreLabel = new TextView(this);
        coreLabel.setText("RAJ AI CORE  •  READY");
        coreLabel.setTextSize(12);
        coreLabel.setGravity(Gravity.CENTER);
        coreLabel.setTextColor(Color.rgb(135, 165, 235));
        coreLabel.setPadding(0, 0, 0, 14);
        content.addView(coreLabel);

        // =========================================================
        // INTELLIGENCE STATUS CARD
        // =========================================================
        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(18, 16, 18, 16);
        statusCard.setBackgroundColor(panel);

        TextView statusTitle = new TextView(this);
        statusTitle.setText("Core Intelligence");
        statusTitle.setTextSize(17);
        statusTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusTitle.setTextColor(white);

        TextView statusText = new TextView(this);
        statusText.setText("●  Neural system ready\n   Voice • Reasoning • Tools");
        statusText.setTextSize(14);
        statusText.setTextColor(Color.rgb(165, 180, 205));
        statusText.setPadding(0, 7, 0, 0);

        statusCard.addView(statusTitle);
        statusCard.addView(statusText);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        statusParams.setMargins(0, 4, 0, 16);
        content.addView(statusCard, statusParams);

        // =========================================================
        // FEATURE GRID
        // =========================================================
        TextView featureHeading = new TextView(this);
        featureHeading.setText("Key Features");
        featureHeading.setTextSize(19);
        featureHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        featureHeading.setTextColor(white);
        featureHeading.setPadding(4, 2, 4, 10);
        content.addView(featureHeading);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        String[][] features = {
                {"⌘", "Code"},
                {"✦", "Create"},
                {"⌕", "Research"},
                {"◈", "Analyze"},
                {"▣", "Image"},
                {"▤", "Files"},
                {"◉", "Camera"}
        };

        for (int i = 0; i < features.length; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(10, 14, 10, 14);
            card.setBackgroundColor(panel);

            TextView icon = new TextView(this);
            icon.setText(features[i][0]);
            icon.setTextSize(24);
            icon.setGravity(Gravity.CENTER);
            icon.setTextColor(
                    i % 2 == 0 ? blue : Color.rgb(170, 110, 255)
            );

            TextView name = new TextView(this);
            name.setText(features[i][1]);
            name.setTextSize(13);
            name.setGravity(Gravity.CENTER);
            name.setTextColor(white);
            name.setPadding(0, 5, 0, 0);

            card.addView(icon);
            card.addView(name);

            final String featureName = features[i][1];

            card.setOnClickListener(v -> {
                if (featureName.equals("Image")) {
                    input.setText("Create an image");
                } else if (featureName.equals("Camera")) {
                    openCameraAction();
                } else if (featureName.equals("Files")) {
                    openFilePickerAction();
                } else if (featureName.equals("Research")) {
                    input.setText("Research ");
                    input.requestFocus();
                } else if (featureName.equals("Code")) {
                    input.setText("Help me code ");
                    input.requestFocus();
                } else if (featureName.equals("Create")) {
                    input.setText("Create ");
                    input.requestFocus();
                } else if (featureName.equals("Analyze")) {
                    input.setText("Analyze ");
                    input.requestFocus();
                }

                input.setSelection(input.length());
            });

            LinearLayout.LayoutParams cp =
                    new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                    );
            cp.setMargins(4, 4, 4, 4);

            if (i < 3) {
                row1.addView(card, cp);
            } else {
                row2.addView(card, cp);
            }
        }

        content.addView(row1);
        content.addView(row2);

        // =========================================================
        // RECENT PROJECTS
        // =========================================================
        TextView recentHeading = new TextView(this);
        recentHeading.setText("Recent Projects");
        recentHeading.setTextSize(19);
        recentHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        recentHeading.setTextColor(white);
        recentHeading.setPadding(4, 18, 4, 10);
        content.addView(recentHeading);

        HorizontalScrollView recentScroll = new HorizontalScrollView(this);
        recentScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout recentRow = new LinearLayout(this);
        recentRow.setOrientation(LinearLayout.HORIZONTAL);

        String[] recentNames = {
                "New conversation",
                "AI Research",
                "Creative Studio"
        };

        for (String recent : recentNames) {
            TextView recentCard = new TextView(this);
            recentCard.setText(recent);
            recentCard.setTextSize(14);
            recentCard.setTextColor(white);
            recentCard.setGravity(Gravity.CENTER_VERTICAL);
            recentCard.setPadding(18, 20, 28, 20);
            recentCard.setBackgroundColor(panel2);

            LinearLayout.LayoutParams rp =
                    new LinearLayout.LayoutParams(
                            210,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            rp.setMargins(4, 4, 8, 4);
            recentRow.addView(recentCard, rp);
        }

        recentScroll.addView(recentRow);
        content.addView(recentScroll);

        // =========================================================
        // ACTUAL CHAT MESSAGES
        // =========================================================
        TextView chatHeading = new TextView(this);
        chatHeading.setText("Conversation");
        chatHeading.setTextSize(19);
        chatHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chatHeading.setTextColor(white);
        chatHeading.setPadding(4, 18, 4, 8);
        content.addView(chatHeading);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, 2, 0, 8);

        content.addView(
                messages,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        scroll.addView(content);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        // =========================================================
        // PREMIUM PROMPT COMPOSER
        // =========================================================
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(8, 8, 8, 8);
        bottom.setBackgroundColor(Color.rgb(18, 23, 37));

        TextView plusButton = new TextView(this);
        plusButton.setText("+");
        plusButton.setTextSize(25);
        plusButton.setGravity(Gravity.CENTER);
        plusButton.setTextColor(white);
        plusButton.setPadding(10, 6, 12, 6);

        bottom.addView(
                plusButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        input = new EditText(this);
        input.setHint("Query RAJ AI...");
        input.setHintTextColor(Color.rgb(135, 145, 165));
        input.setTextColor(white);
        input.setTextSize(16);
        input.setPadding(16, 11, 12, 11);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setBackgroundColor(Color.rgb(25, 31, 48));

        bottom.addView(
                input,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        // Voice button stays in the composer
        micButton = new TextView(this);
        micButton.setText("🎙");
        micButton.setTextSize(22);
        micButton.setGravity(Gravity.CENTER);
        micButton.setTextColor(white);
        micButton.setPadding(10, 8, 10, 8);

        bottom.addView(
                micButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView sendButton = new TextView(this);
        sendButton.setText("➤");
        sendButton.setTextSize(22);
        sendButton.setGravity(Gravity.CENTER);
        sendButton.setTextColor(Color.rgb(120, 175, 255));
        sendButton.setPadding(10, 8, 8, 8);

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                bottom,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(root);

        // =========================================================
        // TEXT TO SPEECH
        // =========================================================
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
                textToSpeech.setSpeechRate(1.0f);
            }
        });

        // =========================================================
        // SPEECH RECOGNITION
        // =========================================================
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
                            micButton.setText("⏹");
                            voiceTranscript.setText("Listening…");
                        }

                        @Override
                        public void onBeginningOfSpeech() {
                            voiceTranscript.setText("Listening…");
                        }

                        @Override
                        public void onRmsChanged(float rmsdB) {}

                        @Override
                        public void onBufferReceived(byte[] buffer) {}

                        @Override
                        public void onEndOfSpeech() {
                            if (listening) {
                                micButton.setText("🎙");
                            }
                        }

                        @Override
                        public void onError(int error) {
                            listening = false;
                            micButton.setText("🎙");

                            if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                                    error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                voiceTranscript.setText("");
                            }
                        }

                        @Override
                        public void onResults(Bundle results) {
                            if (!listening) return;

                            listening = false;
                            micButton.setText("🎙");

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
                    }
            );
        }

        // =========================================================
        // BUTTON ACTIONS
        // =========================================================
        micButton.setOnClickListener(v -> toggleListening());

        sendButton.setOnClickListener(v -> {
            String text = input.getText().toString().trim();

            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });

        plusButton.setOnClickListener(v -> showToolsMenu());

        input.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (actionId != EditorInfo.IME_ACTION_SEND) {
                        return false;
                    }

                    String text = input.getText().toString().trim();

                    if (!text.isEmpty()) {
                        sendMessage(text);
                    }

                    return true;
                }
        );

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

private void showToolsMenu() {
        final PopupWindow[] holder = new PopupWindow[1];

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(18, 18, 18, 18);
        panel.setBackgroundColor(Color.rgb(20, 27, 45));

        String[][] tools = {
                {"▣", "Image"},
                {"▶", "Video"},
                {"▤", "Files"},
                {"⌕", "Web"},
                {"◉", "Camera"}
        };

        for (String[] tool : tools) {
            TextView item = new TextView(this);
            item.setText(tool[0] + "  " + tool[1]);
            item.setTextSize(16);
            item.setTextColor(Color.WHITE);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(22, 20, 22, 20);

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            lp.setMargins(0, 4, 0, 4);
            panel.addView(item, lp);

            item.setOnClickListener(v -> {
                String name = tool[1];

                if (name.equals("Image")) {
                    input.setText("Create an image");
                    input.requestFocus();
                } else if (name.equals("Video")) {
                    input.setText("Create a video");
                    input.requestFocus();
                } else if (name.equals("Files")) {
                    openFilePickerAction();
                } else if (name.equals("Web")) {
                    input.setText("Search the web ");
                    input.requestFocus();
                } else if (name.equals("Camera")) {
                    openCameraAction();
                }

                input.setSelection(input.length());

                if (holder[0] != null) {
                    holder[0].dismiss();
                }
            });
        }

        PopupWindow popup = new PopupWindow(
                panel,
                (int) (260 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        holder[0] = popup;

        popup.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                        Color.rgb(20, 27, 45)
                )
        );
        popup.setOutsideTouchable(true);
        popup.setElevation(12f);

        popup.showAsDropDown(
                input,
                0,
                -330
        );
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

        if (!androidBridge.hasMicrophonePermission()) {
            androidBridge.requestMicrophonePermission(1001);
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


    private void attachImage(android.net.Uri uri) {
        if (uri == null) {
            addMessage("RAJ AI: Photo नहीं मिली।");
            return;
        }

        pendingImageUri = uri;

        String mime = getContentResolver().getType(uri);
        if (mime == null || !mime.startsWith("image/")) {
            mime = "image/jpeg";
        }
        pendingImageMimeType = mime;

        addMessage("You: 📷 Photo attached");

        input.requestFocus();

        runOnUiThread(
                () -> scrollMessagesToBottom()
        );
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
        final android.net.Uri imageUri = pendingImageUri;
        final String imageMimeType = pendingImageMimeType;

        new Thread(() -> {
            try {
                String answer;

                if (imageUri != null) {
                    String base64 = readImageBase64(imageUri);

                    answer = askImageBackend(
                            base64,
                            imageMimeType,
                            finalText
                    );

                    pendingImageUri = null;
                    pendingImageMimeType = null;
                } else {
                    answer = askBackend(finalText);
                }

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

    private String readImageBase64(
            android.net.Uri uri
    ) throws Exception {

        final long MAX_IMAGE_SIZE = 6L * 1024L * 1024L;

        java.io.ByteArrayOutputStream output =
                new java.io.ByteArrayOutputStream();

        try (InputStream inputStream =
                     getContentResolver().openInputStream(uri)) {

            if (inputStream == null) {
                throw new Exception("Photo पढ़ी नहीं जा सकी।");
            }

            byte[] buffer = new byte[8192];
            int read;
            long total = 0;

            while ((read = inputStream.read(buffer)) != -1) {
                total += read;

                if (total > MAX_IMAGE_SIZE) {
                    throw new Exception(
                            "Photo 6 MB से बड़ी है।"
                    );
                }

                output.write(buffer, 0, read);
            }
        }

        byte[] bytes = output.toByteArray();

        if (bytes.length == 0) {
            throw new Exception("Photo खाली है।");
        }

        return Base64.encodeToString(
                bytes,
                Base64.NO_WRAP
        );
    }

    private String askImageBackend(
            String base64,
            String mimeType,
            String question
    ) throws Exception {

        URL url = new URL(
                "https://raj-ai-juvm.onrender.com/analyze-image"
        );

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        JSONObject request = new JSONObject();
        request.put("data", base64);
        request.put(
                "mimeType",
                mimeType == null
                        ? "image/jpeg"
                        : mimeType
        );
        request.put("question", question);

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

        if (stream == null) {
            throw new Exception(
                    "Server ने कोई response नहीं दिया।"
            );
        }

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

        JSONObject result =
                new JSONObject(response.toString());

        if (status < 200 || status >= 300) {
            throw new Exception(
                    result.optString(
                            "error",
                            "Image analysis failed"
                    )
            );
        }

        if (!result.optBoolean("ok", false)) {
            throw new Exception(
                    result.optString(
                            "error",
                            "Image analysis failed"
                    )
            );
        }

        String answer =
                result.optString("answer", "").trim();

        if (answer.isEmpty()) {
            throw new Exception(
                    "RAJ AI ने कोई answer नहीं दिया।"
            );
        }

        return answer;
    }

    private void scrollMessagesToBottom() {
        if (messages == null) return;

        ViewParent parent = messages.getParent();

        while (parent != null && !(parent instanceof ScrollView)) {
            parent = parent.getParent();
        }

        if (parent instanceof ScrollView) {
            ScrollView chatScroll = (ScrollView) parent;
            chatScroll.post(() ->
                    chatScroll.fullScroll(ScrollView.FOCUS_DOWN)
            );
        }
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
        if (button != null) button.setText("⏸️");

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
                                if (button != null) if (button != null) button.setText("🔊");
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
                                if (button != null) if (button != null) button.setText("🔊");
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
        if (textToSpeech == null || text == null || text.trim().isEmpty()) {
            return;
        }

        resetActiveSpeaker();

        SpeakerState state = new SpeakerState();
        state.chunks = splitForSpeech(text);
        state.id = String.valueOf(System.currentTimeMillis());

        if (state.chunks.length == 0) {
            return;
        }

        activeSpeaker = state;
        activeSpeakerButton = null;

        speakSpeakerChunk(state, null);
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
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1002
                && resultCode == RESULT_OK
                && androidBridge != null) {

            android.net.Uri uri = androidBridge.getCameraOutputUri();

            if (uri != null) {
                attachImage(uri);
            } else {
                addMessage("RAJ AI: Camera photo नहीं मिली।");
            }

            androidBridge.clearCameraOutputUri();
            return;
        }

        if (requestCode == 1003
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            android.net.Uri uri = data.getData();
            String mimeType = getContentResolver().getType(uri);

            if (mimeType != null && mimeType.startsWith("image/")) {
                attachImage(uri);
            } else {
                analyzeSelectedFile(uri);
            }
        }
    }

    private void analyzeSelectedFile(android.net.Uri uri) {
        if (uri == null) {
            addMessage("RAJ AI: File नहीं मिला।");
            return;
        }

        input.setEnabled(false);
        addMessage("You: 📁 File analysis");

        new Thread(() -> {
            try {
                String fileName = "Selected file";

                android.database.Cursor cursor =
                        getContentResolver().query(
                                uri,
                                new String[]{
                                        android.provider.OpenableColumns.DISPLAY_NAME
                                },
                                null,
                                null,
                                null
                        );

                if (cursor != null) {
                    try {
                        int nameIndex =
                                cursor.getColumnIndex(
                                        android.provider.OpenableColumns.DISPLAY_NAME
                                );

                        if (cursor.moveToFirst()
                                && nameIndex >= 0
                                && !cursor.isNull(nameIndex)) {
                            fileName = cursor.getString(nameIndex);
                        }
                    } finally {
                        cursor.close();
                    }
                }

                String mimeType =
                        getContentResolver().getType(uri);

                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                InputStream inputStream =
                        getContentResolver().openInputStream(uri);

                if (inputStream == null) {
                    throw new Exception("File पढ़ी नहीं जा सकी।");
                }

                ByteArrayOutputStream buffer =
                        new ByteArrayOutputStream();

                byte[] temp = new byte[8192];
                int read;
                long total = 0;

                final long MAX_FILE_SIZE =
                        6L * 1024L * 1024L;

                try {
                    while ((read = inputStream.read(temp)) != -1) {
                        total += read;

                        if (total > MAX_FILE_SIZE) {
                            throw new Exception(
                                    "File 6 MB से बड़ी है।"
                            );
                        }

                        buffer.write(temp, 0, read);
                    }
                } finally {
                    inputStream.close();
                }

                String base64 =
                        Base64.encodeToString(
                                buffer.toByteArray(),
                                Base64.NO_WRAP
                        );

                JSONObject request =
                        new JSONObject();

                request.put("fileName", fileName);
                request.put("mimeType", mimeType);
                request.put("data", base64);
                request.put(
                        "question",
                        "इस file को पढ़कर मुख्य जानकारी, महत्वपूर्ण बातें और user के लिए उपयोगी निष्कर्ष बताओ।"
                );

                URL url =
                        new URL(
                                "https://raj-ai-juvm.onrender.com/analyze-file"
                        );

                HttpURLConnection connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                try (OutputStream os =
                             connection.getOutputStream()) {
                    os.write(
                            request.toString()
                                    .getBytes("UTF-8")
                    );
                }

                int status =
                        connection.getResponseCode();

                InputStream responseStream =
                        status >= 200 && status < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                if (responseStream == null) {
                    throw new Exception(
                            "Server से response नहीं मिला।"
                    );
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        responseStream,
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
                        new JSONObject(
                                response.toString()
                        );

                if (!json.optBoolean("ok", false)) {
                    throw new Exception(
                            json.optString(
                                    "error",
                                    "File analysis failed"
                            )
                    );
                }

                String answer =
                        json.optString(
                                "answer",
                                "File पढ़ी गई, लेकिन analysis नहीं मिला।"
                        );

                final String finalFileName =
                        fileName;

                final String finalAnswer =
                        answer;

                runOnUiThread(() -> {
                    addMessage(
                            "RAJ AI: 📁 "
                                    + finalFileName
                                    + "\n\n"
                                    + finalAnswer
                    );

                    speakAnswer(finalAnswer);
                    input.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    addMessage(
                            "RAJ AI: File analysis error — "
                                    + e.getMessage()
                    );

                    input.setEnabled(true);
                });
            }
        }).start();
    }

    private void openFilePickerAction() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, 1003);
        } catch (Exception e) {
            addMessage("RAJ AI: File picker नहीं खुल पाया।");
        }
    }

    private void openCameraAction() {
        if (androidBridge != null) {
            androidBridge.openCamera(1002);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == 1001 && androidBridge != null) {
            if (androidBridge.hasMicrophonePermission()) {
                if (micButton != null) {
                    micButton.setAlpha(1f);
                }
            } else {
                if (micButton != null) {
                    micButton.setAlpha(0.7f);
                }
            }
        }

        if (requestCode == 1002 && androidBridge != null) {
            if (androidBridge.hasCameraPermission()) {
                openCameraAction();
            }
        }
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
