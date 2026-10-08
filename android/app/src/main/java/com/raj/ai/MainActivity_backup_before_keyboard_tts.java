package com.raj.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import android.widget.*;
import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class MainActivity extends Activity {

    LinearLayout messages;
    EditText input;
    Button send;
    Button voice;
    SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("🤖 RAJ AI");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 10, 0, 20);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(messages);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);

        input = new EditText(this);
        input.setHint("RAJ AI से कुछ पूछो...");
        input.setSingleLine(false);

        send = new Button(this);
        voice = new Button(this);
        send.setText("SEND");
        voice.setText("🎤");

        bottom.addView(input, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        bottom.addView(voice, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        bottom.addView(send, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root.addView(title);
        root.addView(scroll, scrollParams);
        root.addView(bottom);


        setContentView(root);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override public void onReadyForSpeech(android.os.Bundle params) {
                voice.setText("🔴");
            }

            @Override public void onBeginningOfSpeech() {}

            @Override public void onRmsChanged(float rmsdB) {}

            @Override public void onBufferReceived(byte[] buffer) {}

            @Override public void onEndOfSpeech() {
                voice.setText("🎤");
            }

            @Override public void onError(int error) {
                voice.setText("🎤");
                addMessage("RAJ AI: Voice input error");
            }

            @Override public void onResults(android.os.Bundle results) {
                voice.setText("🎤");

                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    input.setText(matches.get(0));
                    input.setSelection(input.length());
                }
            }

            @Override public void onPartialResults(android.os.Bundle partialResults) {}

            @Override public void onEvent(int eventType, android.os.Bundle params) {}

            @Override public void onSegmentResults(android.os.Bundle segmentResults) {}

            @Override public void onEndOfSegmentedSession() {}
        });

        voice.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission("android.permission.RECORD_AUDIO")
                            != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{"android.permission.RECORD_AUDIO"},
                        1001
                );
                return;
            }

            startVoiceRecognition();
        });


        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();

            if (text.isEmpty()) return;

            addMessage("You: " + text);
            input.setText("");
            send.setEnabled(false);

            new Thread(() -> {
                try {
                    String answer = askBackend(text);

                    runOnUiThread(() -> {
                        addMessage("RAJ AI: " + answer);
                        send.setEnabled(true);
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        addMessage("RAJ AI: Connection error — " + e.getMessage());
                        send.setEnabled(true);
                    });
                }
            }).start();
        });
    }


    private void startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            addMessage("RAJ AI: इस device पर speech recognition उपलब्ध नहीं है।");
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.startListening(intent);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == 1001 &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startVoiceRecognition();
        } else {
            addMessage("RAJ AI: Microphone permission नहीं मिली।");
        }
    }

    private String askBackend(String message) throws Exception {
        URL url = new URL("https://raj-ai-juvm.onrender.com/chat");
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

        try (OutputStream os = connection.getOutputStream()) {
            os.write(request.toString().getBytes("UTF-8"));
        }

        int status = connection.getResponseCode();

        InputStream stream =
                status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, "UTF-8"));

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();

        JSONObject json = new JSONObject(response.toString());

        if (!json.optBoolean("ok", false)) {
            throw new Exception(json.optString("error", "Backend error"));
        }

        return json.optString("answer", "कोई जवाब नहीं मिला।");
    }

    private void addMessage(String text) {
        TextView message = new TextView(this);
        message.setText(text);
        message.setTextSize(18);
        message.setTextColor(Color.BLACK);
        message.setPadding(12, 12, 12, 12);
        messages.addView(message);
    }
}
