package com.christian.tarot;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private WebView web;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean busy;
    private volatile int session;
    private boolean resetOnReturn;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(0xfff0f0f0);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return true; }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    private String read(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > 1048576) throw new IOException("Response too large");
                out.write(buffer, 0, count);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private String request(String address, String body, String key, int timeout) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)new URL(address).openConnection();
        connection.setConnectTimeout(timeout); connection.setReadTimeout(timeout);
        connection.setUseCaches(false); connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "TarotReader-Android/1.4");
        try {
            if (body != null) {
                connection.setRequestMethod("POST"); connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + key);
                try (OutputStream out = connection.getOutputStream()) { out.write(body.getBytes(StandardCharsets.UTF_8)); }
            }
            if (connection.getResponseCode() != 200) throw new IOException("Service unavailable");
            return read(connection.getInputStream());
        } finally { connection.disconnect(); }
    }
    private void deliver(String function, JSONObject result, int requestSession) {
        runOnUiThread(() -> { if (!isDestroyed() && requestSession == session) web.evaluateJavascript(function + "(" + result.toString() + ")", null); });
    }
    public class Bridge {
        @JavascriptInterface public void setTheme(boolean dark) {
            runOnUiThread(() -> {
                int color = dark ? 0xff000000 : 0xfff0f0f0;
                web.setBackgroundColor(color);
                getWindow().setStatusBarColor(color);
                getWindow().setNavigationBarColor(color);
                int flags = dark ? 0 : android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (android.os.Build.VERSION.SDK_INT >= 26 && !dark) {
                    flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                getWindow().getDecorView().setSystemUiVisibility(flags);
            });
        }
        @JavascriptInterface public synchronized void draw(int count) {
            if (busy || (count != 1 && count != 3 && count != 10)) return;
            busy = true;
            final int requestSession = session;
            worker.execute(() -> {
                try {
                    ArrayList<Integer> indices = new ArrayList<>();
                    String source = "Atmospheric Noise";
                    try {
                        String response = request("https://www.random.org/sequences/?min=0&max=77&col=1&format=plain&rnd=new", null, null, 3000);
                        String[] tokens = response.trim().split("\\s+");
                        if (tokens.length != 78) throw new IOException("Incomplete sequence");
                        boolean[] used = new boolean[78];
                        for (String token : tokens) {
                            int value = Integer.parseInt(token);
                            if (value < 0 || value > 77 || used[value]) throw new IOException("Invalid sequence");
                            used[value] = true; indices.add(value);
                        }
                    } catch (Exception unavailable) {
                        indices.clear(); for (int i = 0; i < 78; i++) indices.add(i);
                        Collections.shuffle(indices, new SecureRandom()); source = "Local CSPRNG";
                    }
                    JSONObject result = new JSONObject().put("cards", new JSONArray(indices.subList(0,count))).put("source",source);
                    deliver("showDraw",result,requestSession);
                } catch (Exception error) { deliver("drawFailed",new JSONObject(),requestSession); }
                finally { if (requestSession == session) busy = false; }
            });
        }
        @JavascriptInterface public void reading(String prompt, int id) {
            if (prompt.length() > 20000) return;
            final int requestSession = session;
            worker.execute(() -> {
                JSONObject result = new JSONObject();
                try {
                    result.put("id", id);
                    String key = read(getAssets().open("groq-key.txt")).trim();
                    JSONObject body = new JSONObject().put("model","openai/gpt-oss-120b").put("max_tokens",2000).put("temperature",0.8)
                        .put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",prompt)));
                    JSONObject response = new JSONObject(request("https://api.groq.com/openai/v1/chat/completions",body.toString(),key,60000));
                    result.put("text",response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim());
                } catch (Exception unavailable) { try { result.put("error",true); } catch (JSONException ignored) {} }
                deliver("showReading",result,requestSession);
            });
        }
    }
    @Override protected void onStop() {
        super.onStop();
        session++;
        busy = false;
        resetOnReturn = true;
    }
    @Override protected void onStart() {
        super.onStart();
        if (resetOnReturn) {
            resetOnReturn = false;
            web.reload();
        }
    }
    @Override protected void onDestroy() { worker.shutdownNow(); web.destroy(); super.onDestroy(); }
}
