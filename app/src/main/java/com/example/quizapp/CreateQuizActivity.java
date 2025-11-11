package com.example.quizapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class CreateQuizActivity extends AppCompatActivity {

    private EditText edtTitle, edtCategory, edtPerQSeconds, edtQuizSeconds,
            edtQuestion, edtA, edtB, edtC, edtD, edtExplain;
    private CheckBox cbPerQTimer, cbQuizTimer;
    private RadioGroup rgCorrect;
    private TextView tvCounter;
    private Button btnAddQuestion, btnSaveQuiz;

    private final List<Map<String, Object>> pendingQuestions = new ArrayList<>();
    private int nextOrder = 1; // thứ tự câu hiện tại

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Tạo bài trắc nghiệm");
        setContentView(R.layout.activity_create_quiz);

        bindViews();
        setupTimerToggles();

        btnAddQuestion.setOnClickListener(v -> addCurrentQuestion());
        btnSaveQuiz.setOnClickListener(v -> saveQuizToFirestore());
        updateCounter();
    }

    private void bindViews() {
        edtTitle = findViewById(R.id.edtTitle);
        edtCategory = findViewById(R.id.edtCategory);
        cbPerQTimer = findViewById(R.id.cbPerQTimer);
        cbQuizTimer = findViewById(R.id.cbQuizTimer);
        edtPerQSeconds = findViewById(R.id.edtPerQSeconds);
        edtQuizSeconds = findViewById(R.id.edtQuizSeconds);

        edtQuestion = findViewById(R.id.edtQuestion);
        edtA = findViewById(R.id.edtA);
        edtB = findViewById(R.id.edtB);
        edtC = findViewById(R.id.edtC);
        edtD = findViewById(R.id.edtD);
        edtExplain = findViewById(R.id.edtExplain);
        rgCorrect = findViewById(R.id.rgCorrect);

        tvCounter = findViewById(R.id.tvCounter);
        btnAddQuestion = findViewById(R.id.btnAddQuestion);
        btnSaveQuiz = findViewById(R.id.btnSaveQuiz);
    }

    private void setupTimerToggles() {
        cbPerQTimer.setOnCheckedChangeListener((b, ck) -> edtPerQSeconds.setEnabled(ck));
        cbQuizTimer.setOnCheckedChangeListener((b, ck) -> edtQuizSeconds.setEnabled(ck));
    }

    private void updateCounter() {
        tvCounter.setText("Câu số: " + nextOrder + " | " + pendingQuestions.size() + " câu đã thêm");
    }

    private void addCurrentQuestion() {
        String q = edtQuestion.getText().toString().trim();
        String A = edtA.getText().toString().trim();
        String B = edtB.getText().toString().trim();
        String C = edtC.getText().toString().trim();
        String D = edtD.getText().toString().trim();
        String explain = edtExplain.getText().toString().trim();

        if (TextUtils.isEmpty(q)) { edtQuestion.setError("Nhập nội dung câu hỏi"); return; }
        if (TextUtils.isEmpty(A) || TextUtils.isEmpty(B) || TextUtils.isEmpty(C) || TextUtils.isEmpty(D)) {
            Toast.makeText(this, "Nhập đủ 4 đáp án", Toast.LENGTH_SHORT).show(); return;
        }
        int idx = -1;
        int checkedId = rgCorrect.getCheckedRadioButtonId();
        if (checkedId == R.id.rbA) idx = 0;
        else if (checkedId == R.id.rbB) idx = 1;
        else if (checkedId == R.id.rbC) idx = 2;
        else if (checkedId == R.id.rbD) idx = 3;
        if (idx == -1) { Toast.makeText(this, "Chọn đáp án đúng", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> item = new HashMap<>();
        item.put("order", nextOrder);
        item.put("question", q);
        item.put("options", Arrays.asList(A,B,C,D));
        item.put("correctIndex", idx);
        if (!TextUtils.isEmpty(explain)) item.put("explain", explain);

        pendingQuestions.add(item);
        nextOrder++;
        clearQuestionForm();
        updateCounter();
        Toast.makeText(this, "Đã thêm câu " + (nextOrder-1), Toast.LENGTH_SHORT).show();
    }

    private void clearQuestionForm() {
        edtQuestion.setText(""); edtA.setText(""); edtB.setText(""); edtC.setText(""); edtD.setText("");
        edtExplain.setText(""); rgCorrect.clearCheck();
    }

    private void saveQuizToFirestore() {
        String title = edtTitle.getText().toString().trim();
        String category = edtCategory.getText().toString().trim();
        boolean perQOn = cbPerQTimer.isChecked();
        boolean quizOn = cbQuizTimer.isChecked();
        int perQSec = perQOn ? safeParseInt(edtPerQSeconds.getText().toString()) : 0;
        int quizSec = quizOn ? safeParseInt(edtQuizSeconds.getText().toString()) : 0;

        if (TextUtils.isEmpty(title)) { edtTitle.setError("Nhập tiêu đề"); return; }
        if (TextUtils.isEmpty(category)) { edtCategory.setError("Nhập chủ đề"); return; }
        if (perQOn && perQSec <= 0) { edtPerQSeconds.setError("Số giây > 0"); return; }
        if (quizOn && quizSec <= 0) { edtQuizSeconds.setError("Số giây > 0"); return; }
        if (pendingQuestions.isEmpty()) {
            Toast.makeText(this, "Chưa có câu hỏi nào", Toast.LENGTH_SHORT).show(); return;
        }

        ProgressDialog dlg = ProgressDialog.show(this, null, "Đang lưu bài...", true, false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> quiz = new HashMap<>();
        quiz.put("title", title);
        quiz.put("category", category);
        quiz.put("ownerId", uid);
        quiz.put("perQuestionTimerOn", perQOn);
        quiz.put("perQuestionSeconds", perQSec);
        quiz.put("quizTimerOn", quizOn);
        quiz.put("quizSeconds", quizSec);
        quiz.put("numQuestions", pendingQuestions.size());
        quiz.put("createdAt", new Date());

        db.collection("quizzes").add(quiz)
                .addOnSuccessListener(ref -> {
                    // ghi subcollection questions
                    for (Map<String, Object> q : pendingQuestions) {
                        ref.collection("questions").add(q);
                    }
                    dlg.dismiss();
                    Toast.makeText(this, "Đã lưu bài trắc nghiệm!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    dlg.dismiss();
                    Toast.makeText(this, "Lỗi lưu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private int safeParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
