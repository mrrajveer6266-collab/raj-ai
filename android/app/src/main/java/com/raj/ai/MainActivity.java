package com.raj.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class MainActivity extends Activity {

    LinearLayout messages;
    EditText input;
    Button send;

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
        send.setText("SEND");

        bottom.addView(input, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        bottom.addView(send, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root.addView(title);
        root.addView(scroll, scrollParams);
        root.addView(bottom);

        setContentView(root);

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
