package com.example.quizapp;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class TakeQuizActivity extends AppCompatActivity {

    private String quizId;
    private TextView tvHeader, tvTimer, tvQuestion;
    private RadioGroup rg;
    private RadioButton[] rbs;
    private Button btnPrev, btnNext, btnSubmit;

    private boolean perQTimerOn, quizTimerOn;
    private int perQSeconds, quizSeconds;

    private final List<Question> questions = new ArrayList<>();
    private final Map<Integer, Integer> answers = new HashMap<>(); // index -> chosen
    private int idx = 0;

    private CountDownTimer quizTimer, perQTimer;

    // Âm thanh đúng / sai
    private MediaPlayer correctPlayer, wrongPlayer;
    private boolean isSoundEnabled = false; // Biến lưu trạng thái âm thanh

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_quiz);

        quizId = getIntent().getStringExtra("quizId");
        bindViews();
        loadQuiz();

        // Khởi tạo MediaPlayer (nhớ tạo file res/raw/correct.mp3 và res/raw/wrong.mp3)
        correctPlayer = MediaPlayer.create(this, R.raw.correct_answer);
        wrongPlayer = MediaPlayer.create(this, R.raw.wrong_answer);
    }

    private void bindViews() {
        tvHeader = findViewById(R.id.tvHeader);
        tvTimer = findViewById(R.id.tvTimer);
        tvQuestion = findViewById(R.id.tvQuestion);
        rg = findViewById(R.id.rgOptions);
        rbs = new RadioButton[]{
                findViewById(R.id.rb0), findViewById(R.id.rb1),
                findViewById(R.id.rb2), findViewById(R.id.rb3)
        };
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Lắng nghe sự kiện của switch âm thanh
        Switch switchSound = findViewById(R.id.switchSound);
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSoundEnabled = isChecked; // Cập nhật trạng thái âm thanh
        });

        btnPrev.setOnClickListener(v -> showQuestion(Math.max(0, idx - 1)));

        // NÚT TIẾP: phát âm thanh đúng/sai cho CÂU HIỆN TẠI rồi mới chuyển câu
        btnNext.setOnClickListener(v -> {
            if (questions.isEmpty()) return;

            // Lấy đáp án mà user chọn cho câu hiện tại
            Integer chosen = answers.get(idx);
            if (chosen == null) {
                Toast.makeText(TakeQuizActivity.this,
                        "Bạn chưa chọn đáp án cho câu này", Toast.LENGTH_SHORT).show();
            } else {
                boolean isCorrect = (chosen == questions.get(idx).correctIndex);
                playResultSound(isCorrect);
            }

            // Chuyển sang câu kế tiếp (nếu còn)
            if (idx < questions.size() - 1) {
                showQuestion(idx + 1);
            } else {
                // Nếu là câu cuối cùng, tự động nộp bài
                submitAuto("Đây là câu cuối, hệ thống sẽ nộp bài.");
            }
        });

        btnSubmit.setOnClickListener(v -> submit());

        rg.setOnCheckedChangeListener((g, checkedId) -> {
            int chosen = -1;
            for (int i = 0; i < 4; i++) {
                if (rbs[i].getId() == checkedId) {
                    chosen = i;
                    break;
                }
            }
            if (chosen >= 0) {
                answers.put(idx, chosen);
                btnPrev.setEnabled(true);  // Enable prev button when answer is selected
            }

            // Khi đã chọn đáp án, tắt đi nút "Tiếp" nếu không có lựa chọn
            btnNext.setEnabled(true); // Bật nút "Tiếp" khi có đáp án
        });
    }

    // Phát âm thanh đúng/sai (chỉ phát nếu bật âm thanh)
    private void playResultSound(boolean correct) {
        if (!isSoundEnabled) return; // Nếu âm thanh tắt, không phát âm thanh

        MediaPlayer p = correct ? correctPlayer : wrongPlayer;
        if (p == null) return;
        try {
            if (p.isPlaying()) {
                p.seekTo(0);
            }
            p.start();
        } catch (Exception ignored) {}
    }

    private void loadQuiz() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("quizzes").document(quizId).get().addOnSuccessListener(doc -> {
            String title = doc.getString("title");
            perQTimerOn = Boolean.TRUE.equals(doc.getBoolean("perQuestionTimerOn"));
            quizTimerOn = Boolean.TRUE.equals(doc.getBoolean("quizTimerOn"));
            perQSeconds = doc.getLong("perQuestionSeconds") == null ? 0 : doc.getLong("perQuestionSeconds").intValue();
            quizSeconds = doc.getLong("quizSeconds") == null ? 0 : doc.getLong("quizSeconds").intValue();

            tvHeader.setText(title == null ? "Bài trắc nghiệm" : title);

            // Tải câu hỏi (theo order)
            db.collection("quizzes").document(quizId)
                    .collection("questions")
                    .orderBy("order")
                    .get().addOnSuccessListener(qs -> {
                        questions.clear();
                        for (DocumentSnapshot d : qs.getDocuments()) {
                            Question q = new Question();
                            q.order = d.getLong("order") == null ? 0 : d.getLong("order").intValue();
                            q.question = d.getString("question");
                            @SuppressWarnings("unchecked")
                            List<String> ops = (List<String>) d.get("options");
                            q.options = ops == null ? Arrays.asList("A", "B", "C", "D") : ops;
                            q.correctIndex = d.getLong("correctIndex") == null ? 0 : d.getLong("correctIndex").intValue();
                            questions.add(q);
                        }
                        if (questions.isEmpty()) {
                            Toast.makeText(this, "Bài này chưa có câu hỏi", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        // Timer toàn bài
                        if (quizTimerOn && quizSeconds > 0) startQuizTimer(quizSeconds);
                        showQuestion(0);
                    });
        });
    }

    private void showQuestion(int newIndex) {
        idx = newIndex;
        Question q = questions.get(idx);
        tvQuestion.setText((idx + 1) + ". " + q.question);
        for (int i = 0; i < 4; i++) {
            rbs[i].setText(i < q.options.size() ? q.options.get(i) : "");
            rbs[i].setEnabled(true);
        }
        rg.clearCheck();
        if (answers.containsKey(idx)) rbs[answers.get(idx)].setChecked(true);

        btnPrev.setEnabled(idx > 0);
        btnNext.setEnabled(idx < questions.size() - 1);

        // Reset per-question timer
        if (perQTimer != null) perQTimer.cancel();
        if (perQTimerOn && perQSeconds > 0) startPerQuestionTimer(perQSeconds);
    }

    private void startQuizTimer(int seconds) {
        tvTimer.setText("Thời gian toàn bài: " + seconds + "s");
        quizTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override public void onTick(long ms) { tvTimer.setText("Còn " + (ms/1000) + "s"); }
            @Override public void onFinish() { submitAuto("Hết thời gian toàn bài!"); }
        }.start();
    }

    private void startPerQuestionTimer(int seconds) {
        perQTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override public void onTick(long ms) { /* có thể hiển thị riêng nếu muốn */ }
            @Override public void onFinish() {
                // tự động chuyển câu tiếp theo
                if (idx < questions.size() - 1) showQuestion(idx + 1);
                else submitAuto("Hết thời gian cho câu cuối!");
            }
        }.start();
    }

    private void submitAuto(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg + "\nHệ thống sẽ nộp bài.")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> submit())
                .show();
    }

    private void submit() {
        if (quizTimer != null) quizTimer.cancel();
        if (perQTimer != null) perQTimer.cancel();

        int correct = 0;
        for (int i = 0; i < questions.size(); i++) {
            Integer chosen = answers.get(i);
            if (chosen != null && chosen == questions.get(i).correctIndex) correct++;
        }
        int total = questions.size();
        int score = Math.round((correct * 100f) / total);

        // Lưu lịch sử (optional)
        saveAttempt(score, correct, total);

        new AlertDialog.Builder(this)
                .setTitle("Kết quả")
                .setMessage("Đúng " + correct + "/" + total + " • Điểm: " + score)
                .setPositiveButton("Xong", (d, w) -> finish())
                .show();
    }

    private void saveAttempt(int score, int correct, int total) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        Map<String, Object> att = new HashMap<>();
        att.put("quizId", quizId);
        att.put("userId", uid);
        att.put("score", score);
        att.put("correct", correct);
        att.put("total", total);
        att.put("createdAt", new Date());
        FirebaseFirestore.getInstance().collection("attempts").add(att);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (correctPlayer != null) {
            correctPlayer.release();
            correctPlayer = null;
        }
        if (wrongPlayer != null) {
            wrongPlayer.release();
            wrongPlayer = null;
        }
    }

    static class Question {
        int order, correctIndex;
        String question;
        List<String> options;
    }
}
