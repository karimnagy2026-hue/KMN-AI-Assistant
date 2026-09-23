package com.karimmnagy.aiassistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ============================================================
    // إعدادات التطبيق
    // ============================================================

    private static final String PREFS_NAME = "KMN_AI_SETTINGS";
    private static final String KEY_API = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String KEY_INSTRUCTIONS = "instructions";

    private static final String API_URL =
            "https://api.openai.com/v1/responses";

    // ============================================================
    // عناصر الواجهة
    // ============================================================

    private LinearLayout chatContainer;
    private EditText messageInput;
    private Button sendButton;
    private ProgressBar progressBar;

    // ============================================================
    // إعدادات المستخدم
    // ============================================================

    private String apiKey = "";
    private String selectedModel = "gpt-5.6-luna";
    private String customInstructions = "";

    private SharedPreferences preferences;

    // ============================================================
    // سجل المحادثة
    // ============================================================

    private final List<ChatMessage> conversation = new ArrayList<>();

    // ============================================================
    // onCreate
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        loadSettings();

        createMainInterface();
    }

    // ============================================================
    // تحميل الإعدادات
    // ============================================================

    private void loadSettings() {

        apiKey = preferences.getString(KEY_API, "");

        selectedModel = preferences.getString(
                KEY_MODEL,
                "gpt-5.6-luna"
        );

        customInstructions = preferences.getString(
                KEY_INSTRUCTIONS,
                ""
        );
    }

    // ============================================================
    // إنشاء الواجهة الرئيسية
    // ============================================================

    private void createMainInterface() {

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);

        root.setBackgroundColor(
                Color.rgb(11, 15, 20)
        );

        // ========================================================
        // الشريط العلوي
        // ========================================================

        LinearLayout topBar = new LinearLayout(this);

        topBar.setOrientation(
                LinearLayout.HORIZONTAL
        );

        topBar.setGravity(
                Gravity.CENTER_VERTICAL
        );

        topBar.setPadding(
                24,
                20,
                16,
                16
        );

        topBar.setBackgroundColor(
                Color.rgb(11, 15, 20)
        );

        // عنوان التطبيق

        LinearLayout titleContainer =
                new LinearLayout(this);

        titleContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView title =
                new TextView(this);

        title.setText("KMN AI");

        title.setTextColor(Color.WHITE);

        title.setTextSize(22);

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        TextView subtitle =
                new TextView(this);

        subtitle.setText(
                "مساعد ذكاء اصطناعي"
        );

        subtitle.setTextColor(
                Color.rgb(170, 178, 191)
        );

        subtitle.setTextSize(13);

        titleContainer.addView(title);

        titleContainer.addView(subtitle);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        topBar.addView(
                titleContainer,
                titleParams
        );

        // زر الإعدادات

        Button settingsButton =
                new Button(this);

        settingsButton.setText("⚙");

        settingsButton.setTextSize(20);

        settingsButton.setTextColor(Color.WHITE);

        settingsButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        settingsButton.setOnClickListener(
                v -> showSettingsDialog()
        );

        topBar.addView(
                settingsButton,
                new LinearLayout.LayoutParams(
                        60,
                        60
                )
        );

        root.addView(
                topBar,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ========================================================
        // منطقة المحادثة
        // ========================================================

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(true);

        chatContainer =
                new LinearLayout(this);

        chatContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        chatContainer.setPadding(
                16,
                16,
                16,
                16
        );

        scrollView.addView(
                chatContainer,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        // ========================================================
        // رسالة الترحيب
        // ========================================================

        if (conversation.isEmpty()) {

            addAssistantMessage(
                    "مرحبًا 👋\n\n" +
                    "أنا KMN AI، مساعدك للذكاء الاصطناعي.\n\n" +
                    "يمكنك سؤالي عن الدراسة، البرمجة، العلوم، " +
                    "الرياضيات، المعلومات العامة وغير ذلك."
            );
        }

        // ========================================================
        // شريط الكتابة
        // ========================================================

        LinearLayout inputContainer =
                new LinearLayout(this);

        inputContainer.setOrientation(
                LinearLayout.HORIZONTAL
        );

        inputContainer.setGravity(
                Gravity.CENTER_VERTICAL
        );

        inputContainer.setPadding(
                12,
                10,
                12,
                12
        );

        inputContainer.setBackgroundColor(
                Color.rgb(21, 26, 33)
        );

        messageInput =
                new EditText(this);

        messageInput.setHint(
                "اكتب رسالتك..."
        );

        messageInput.setHintTextColor(
                Color.rgb(150, 158, 170)
        );

        messageInput.setTextColor(Color.WHITE);

        messageInput.setTextSize(16);

        messageInput.setSingleLine(false);

        messageInput.setMaxLines(4);

        messageInput.setPadding(
                18,
                12,
                18,
                12
        );

        messageInput.setBackgroundColor(
                Color.rgb(29, 35, 44)
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        inputContainer.addView(
                messageInput,
                inputParams
        );

        // زر الإرسال

        sendButton =
                new Button(this);

        sendButton.setText("إرسال");

        sendButton.setTextColor(Color.WHITE);

        sendButton.setTextSize(14);

        sendButton.setBackgroundColor(
                Color.rgb(124, 92, 252)
        );

        LinearLayout.LayoutParams sendParams =
                new LinearLayout.LayoutParams(
                        100,
                        55
                );

        sendParams.setMargins(
                10,
                0,
                0,
                0
        );

        inputContainer.addView(
                sendButton,
                sendParams
        );

        root.addView(
                inputContainer,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ========================================================
        // مؤشر الانتظار
        // ========================================================

        progressBar =
                new ProgressBar(this);

        progressBar.setVisibility(
                View.GONE
        );

        root.addView(
                progressBar,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ========================================================
        // زر الإرسال
        // ========================================================

        sendButton.setOnClickListener(
                v -> sendMessage()
        );

        setContentView(root);
    }

    // ============================================================
    // إرسال الرسالة
    // ============================================================

    private void sendMessage() {

        String message =
                messageInput.getText()
                        .toString()
                        .trim();

        if (message.isEmpty()) {

            Toast.makeText(
                    this,
                    "اكتب رسالة أولًا",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "أضف مفتاح API من الإعدادات أولًا",
                    Toast.LENGTH_LONG
            ).show();

            showSettingsDialog();

            return;
        }

        // إضافة رسالة المستخدم

        addUserMessage(message);

        conversation.add(
                new ChatMessage(
                        "user",
                        message
                )
        );

        messageInput.setText("");

        setLoading(true);

        // إرسال الطلب في Thread منفصل
        // حتى لا تتجمد واجهة التطبيق.

        new Thread(() -> {

            String response =
                    callOpenAI(message);

            new Handler(
                    Looper.getMainLooper()
            ).post(() -> {

                setLoading(false);

                if (response == null ||
                        response.trim().isEmpty()) {

                    addAssistantMessage(
                            "حدث خطأ ولم يصل رد من الخادم."
                    );

                    return;
                }

                addAssistantMessage(response);

                conversation.add(
                        new ChatMessage(
                                "assistant",
                                response
                        )
                );
            });

        }).start();
    }

    // ============================================================
    // الاتصال بـ OpenAI Responses API
    // ============================================================

    private String callOpenAI(String userMessage) {

        HttpURLConnection connection = null;

        try {

            URL url =
                    new URL(API_URL);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("POST");

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + apiKey
            );

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );

            connection.setDoOutput(true);

            connection.setConnectTimeout(
                    30000
            );

            connection.setReadTimeout(
                    60000
            );

            // ====================================================
            // إنشاء JSON للطلب
            // ====================================================

            JSONObject request =
                    new JSONObject();

            request.put(
                    "model",
                    selectedModel
            );

            // التعليمات المخصصة

            if (customInstructions != null &&
                    !customInstructions.trim().isEmpty()) {

                request.put(
                        "instructions",
                        customInstructions
                );
            }

            // ====================================================
            // إنشاء قائمة الرسائل
            // ====================================================

            JSONArray input =
                    new JSONArray();

            // نرسل المحادثة السابقة حتى يحتفظ
            // النموذج بسياق المحادثة.

            for (ChatMessage message :
                    conversation) {

                JSONObject item =
                        new JSONObject();

                item.put(
                        "role",
                        message.role
                );

                JSONArray content =
                        new JSONArray();

                JSONObject textPart =
                        new JSONObject();

                textPart.put(
                        "type",
                        "input_text"
                );

                textPart.put(
                        "text",
                        message.text
                );

                content.put(textPart);

                item.put(
                        "content",
                        content
                );

                input.put(item);
            }

            // الرسالة الحالية موجودة بالفعل
            // داخل conversation، لذلك لا نضيفها مرة أخرى.

            request.put(
                    "input",
                    input
            );

            // ====================================================
            // إرسال الطلب
            // ====================================================

            byte[] requestBytes =
                    request.toString()
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            OutputStream outputStream =
                    connection.getOutputStream();

            outputStream.write(
                    requestBytes
            );

            outputStream.flush();

            outputStream.close();

            // ====================================================
            // قراءة استجابة الخادم
            // ====================================================

            int responseCode =
                    connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200 &&
                    responseCode < 300) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();
            }

            if (inputStream == null) {

                return "لم يصل رد من الخادم. رمز HTTP: "
                        + responseCode;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder responseBuilder =
                    new StringBuilder();

            String line;

            while ((line =
                    reader.readLine()) != null) {

                responseBuilder.append(line);
            }

            reader.close();

            String rawResponse =
                    responseBuilder.toString();

            // ====================================================
            // إذا كان هناك خطأ HTTP
            // ====================================================

            if (responseCode < 200 ||
                    responseCode >= 300) {

                return extractErrorMessage(
                        rawResponse,
                        responseCode
                );
            }

            // ====================================================
            // استخراج نص الإجابة
            // ====================================================

            return extractResponseText(
                    rawResponse
            );

        } catch (Exception e) {

            return "حدث خطأ أثناء الاتصال:\n"
                    + e.getMessage();

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    // ============================================================
    // استخراج النص من استجابة Responses API
    // ============================================================

    private String extractResponseText(
            String jsonString
    ) {

        try {

            JSONObject response =
                    new JSONObject(jsonString);

            // بعض الاستجابات تحتوي على output_text

            if (response.has("output_text")) {

                String text =
                        response.optString(
                                "output_text",
                                ""
                        );

                if (!text.isEmpty()) {

                    return text;
                }
            }

            // ====================================================
            // البحث داخل output
            // ====================================================

            JSONArray output =
                    response.optJSONArray(
                            "output"
                    );

            if (output != null) {

                StringBuilder result =
                        new StringBuilder();

                for (int i = 0;
                     i < output.length();
                     i++) {

                    JSONObject outputItem =
                            output.optJSONObject(i);

                    if (outputItem == null) {
                        continue;
                    }

                    JSONArray content =
                            outputItem.optJSONArray(
                                    "content"
                            );

                    if (content == null) {
                        continue;
                    }

                    for (int j = 0;
                         j < content.length();
                         j++) {

                        JSONObject contentItem =
                                content.optJSONObject(j);

                        if (contentItem == null) {
                            continue;
                        }

                        String type =
                                contentItem.optString(
                                        "type",
                                        ""
                                );

                        if ("output_text".equals(type)) {

                            String text =
                                    contentItem.optString(
                                            "text",
                                            ""
                                    );

                            if (!text.isEmpty()) {

                                if (result.length() > 0) {
                                    result.append("\n");
                                }

                                result.append(text);
                            }
                        }
                    }
                }

                if (result.length() > 0) {

                    return result.toString();
                }
            }

            return "وصلت استجابة من الخادم، ولكن لم أستطع استخراج النص منها.";

        } catch (JSONException e) {

            return "تعذر تحليل استجابة الخادم:\n"
                    + e.getMessage();
        }
    }

    // ============================================================
    // استخراج رسالة الخطأ
    // ============================================================

    private String extractErrorMessage(
            String jsonString,
            int responseCode
    ) {

        try {

            JSONObject response =
                    new JSONObject(jsonString);

            JSONObject error =
                    response.optJSONObject(
                            "error"
                    );

            if (error != null) {

                String message =
                        error.optString(
                                "message",
                                ""
                        );

                if (!message.isEmpty()) {

                    return "خطأ من OpenAI:\n"
                            + message
                            + "\n\nHTTP "
                            + responseCode;
                }
            }

        } catch (Exception ignored) {
        }

        return "حدث خطأ في الاتصال بالخادم.\n\n"
                + "HTTP "
                + responseCode;
    }

    // ============================================================
    // إضافة رسالة المستخدم إلى الشاشة
    // ============================================================

    private void addUserMessage(
            String message
    ) {

        TextView bubble =
                createMessageBubble(
                        message,
                        true
                );

        chatContainer.addView(
                bubble
        );

        scrollToBottom();
    }

    // ============================================================
    // إضافة رسالة المساعد
    // ============================================================

    private void addAssistantMessage(
            String message
    ) {

        TextView bubble =
                createMessageBubble(
                        message,
                        false
                );

        chatContainer.addView(
                bubble
        );

        scrollToBottom();
    }

    // ============================================================
    // إنشاء فقاعة الرسالة
    // ============================================================

    private TextView createMessageBubble(
            String message,
            boolean isUser
    ) {

        TextView bubble =
                new TextView(this);

        bubble.setText(message);

        bubble.setTextSize(16);

        bubble.setTextColor(Color.WHITE);

        bubble.setGravity(
                Gravity.RIGHT
        );

        bubble.setPadding(
                18,
                14,
                18,
                14
        );

        if (isUser) {

            bubble.setBackgroundColor(
                    Color.rgb(43, 36, 74)
            );

        } else {

            bubble.setBackgroundColor(
                    Color.rgb(23, 29, 37)
            );
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                8,
                0,
                8
        );

        bubble.setLayoutParams(params);

        return bubble;
    }

    // ============================================================
    // التمرير إلى أسفل المحادثة
    // ============================================================

    private void scrollToBottom() {

        if (chatContainer == null) {
            return;
        }

        chatContainer.post(() -> {

            View parent =
                    (View) chatContainer.getParent();

            if (parent instanceof ScrollView) {

                ScrollView scrollView =
                        (ScrollView) parent;

                scrollView.fullScroll(
                        View.FOCUS_DOWN
                );
            }
        });
    }

    // ============================================================
    // حالة التحميل
    // ============================================================

    private void setLoading(
            boolean loading
    ) {

        if (progressBar != null) {

            progressBar.setVisibility(
                    loading
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (sendButton != null) {

            sendButton.setEnabled(
                    !loading
            );
        }

        if (messageInput != null) {

            messageInput.setEnabled(
                    !loading
            );
        }
    }

    // ============================================================
    // نافذة الإعدادات
    // ============================================================

    private void showSettingsDialog() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                40,
                20,
                40,
                10
        );

        // ========================================================
        // API Key
        // ========================================================

        TextView apiLabel =
                new TextView(this);

        apiLabel.setText(
                "OpenAI API Key"
        );

        apiLabel.setTextSize(16);

        apiLabel.setTextColor(Color.DKGRAY);

        layout.addView(apiLabel);

        EditText apiInput =
                new EditText(this);

        apiInput.setHint(
                "ضع مفتاح API هنا"
        );

        apiInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        apiInput.setText(
                apiKey
        );

        layout.addView(
                apiInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ========================================================
        // النموذج
        // ========================================================

        TextView modelLabel =
                new TextView(this);

        modelLabel.setText(
                "النموذج"
        );

        modelLabel.setTextSize(16);

        modelLabel.setTextColor(Color.DKGRAY);

        LinearLayout.LayoutParams modelLabelParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        modelLabelParams.setMargins(
                0,
                25,
                0,
                5
        );

        layout.addView(
                modelLabel,
                modelLabelParams
        );

        Spinner modelSpinner =
                new Spinner(this);

        String[] models = {

                "gpt-5.6-luna",
                "gpt-5.6-terra",
                "gpt-5.6-sol"

        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        models
                );

        modelSpinner.setAdapter(adapter);

        int selectedIndex = 0;

        for (int i = 0;
             i < models.length;
             i++) {

            if (models[i].equals(
                    selectedModel
            )) {

                selectedIndex = i;
                break;
            }
        }

        modelSpinner.setSelection(
                selectedIndex
        );

        layout.addView(
                modelSpinner
        );

        // ========================================================
        // التعليمات المخصصة
        // ========================================================

        TextView instructionsLabel =
                new TextView(this);

        instructionsLabel.setText(
                "التعليمات المخصصة"
        );

        instructionsLabel.setTextSize(16);

        instructionsLabel.setTextColor(Color.DKGRAY);

        LinearLayout.LayoutParams instructionLabelParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        instructionLabelParams.setMargins(
                0,
                25,
                0,
                5
        );

        layout.addView(
                instructionsLabel,
                instructionLabelParams
        );

        EditText instructionsInput =
                new EditText(this);

        instructionsInput.setHint(
                "مثال: أجب باللغة العربية وبشكل مفصل."
        );

        instructionsInput.setGravity(
                Gravity.TOP
        );

        instructionsInput.setMinLines(4);

        instructionsInput.setText(
                customInstructions
        );

        layout.addView(
                instructionsInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ========================================================
        // إنشاء Dialog
        // ========================================================

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "إعدادات KMN AI"
                        )
                        .setView(layout)
                        .setNegativeButton(
                                "إلغاء",
                                null
                        )
                        .setPositiveButton(
                                "حفظ",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                dialogInterface -> {

                    Button saveButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    saveButton.setOnClickListener(
                            v -> {

                                apiKey =
                                        apiInput
                                                .getText()
                                                .toString()
                                                .trim();

                                selectedModel =
                                        modelSpinner
                                                .getSelectedItem()
                                                .toString();

                                customInstructions =
                                        instructionsInput
                                                .getText()
                                                .toString()
                                                .trim();

                                // حفظ الإعدادات

                                preferences.edit()
                                        .putString(
                                                KEY_API,
                                                apiKey
                                        )
                                        .putString(
                                                KEY_MODEL,
                                                selectedModel
                                        )
                                        .putString(
                                                KEY_INSTRUCTIONS,
                                                customInstructions
                                        )
                                        .apply();

                                Toast.makeText(
                                        this,
                                        "تم حفظ الإعدادات ✓",
                                        Toast.LENGTH_SHORT
                                ).show();

                                dialog.dismiss();
                            }
                    );
                }
        );

        dialog.show();
    }

    // ============================================================
    // نموذج رسالة المحادثة
    // ============================================================

    private static class ChatMessage {

        String role;
        String text;

        ChatMessage(
                String role,
                String text
        ) {

            this.role = role;
            this.text = text;
        }
    }
}
