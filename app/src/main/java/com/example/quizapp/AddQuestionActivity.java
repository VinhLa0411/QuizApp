package com.example.quizapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddQuestionActivity extends AppCompatActivity {

    private EditText edtCategory, edtQuestion, edtA, edtB, edtC, edtD, edtExplain;
    private Spinner spnDifficulty;
    private RadioGroup correctGroup;
    private Button btnSave, btnCancel;

    private String docId = null; // != null => chế độ sửa
    private ProgressDialog dlg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_question);

        // Ánh xạ view
        edtCategory = findViewById(R.id.edtCategory);
        edtQuestion = findViewById(R.id.edtQuestion);
        edtA = findViewById(R.id.edtA);
        edtB = findViewById(R.id.edtB);
        edtC = findViewById(R.id.edtC);
        edtD = findViewById(R.id.edtD);
        edtExplain = findViewById(R.id.edtExplain);
        spnDifficulty = findViewById(R.id.spnDifficulty);
        correctGroup = findViewById(R.id.correctGroup);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Spinner độ khó
        spnDifficulty.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Easy", "Medium", "Hard"}
        ));

        // Nút
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> {
            btnSave.setEnabled(false); // chống double click
            saveQuestion();
        });

        // Chế độ sửa (nếu có docId)
        docId = getIntent().getStringExtra("docId");
        if (docId != null) {
            setTitle("Sửa câu hỏi");
            loadForEdit(docId);
        } else {
            setTitle("Thêm câu hỏi");
        }
    }

    /** Nạp dữ liệu vào form khi sửa */
    private void loadForEdit(String id) {
        FirebaseFirestore.getInstance().collection("questions").document(id).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) return;
                    edtCategory.setText(d.getString("category"));
                    edtQuestion.setText(d.getString("question"));
                    edtExplain.setText(d.getString("explain"));

                    String diff = d.getString("difficulty");
                    if (diff != null) {
                        int pos = diff.equals("Hard") ? 2 : diff.equals("Medium") ? 1 : 0;
                        spnDifficulty.setSelection(pos);
                    }

                    @SuppressWarnings("unchecked")
                    java.util.List<String> ops = (java.util.List<String>) d.get("options");
                    if (ops != null && ops.size() == 4) {
                        edtA.setText(ops.get(0));
                        edtB.setText(ops.get(1));
                        edtC.setText(ops.get(2));
                        edtD.setText(ops.get(3));
                    }

                    Long ci = d.getLong("correctIndex");
                    if (ci != null) {
                        int[] ids = {R.id.rbA, R.id.rbB, R.id.rbC, R.id.rbD};
                        if (ci >= 0 && ci < ids.length) correctGroup.check(ids[ci.intValue()]);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải câu hỏi: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /** Validate & lưu lên Firestore */
    private void saveQuestion() {
        String category  = edtCategory.getText().toString().trim();
        String question  = edtQuestion.getText().toString().trim();
        String A         = edtA.getText().toString().trim();
        String B         = edtB.getText().toString().trim();
        String C         = edtC.getText().toString().trim();
        String D         = edtD.getText().toString().trim();
        String explain   = edtExplain.getText().toString().trim();
        String difficulty = (String) spnDifficulty.getSelectedItem();

        // --- Validate tối thiểu ---
        if (TextUtils.isEmpty(category)) { edtCategory.setError("Nhập chủ đề"); reEnable(); return; }
        if (TextUtils.isEmpty(question)) { edtQuestion.setError("Nhập câu hỏi"); reEnable(); return; }
        if (TextUtils.isEmpty(A) || TextUtils.isEmpty(B) || TextUtils.isEmpty(C) || TextUtils.isEmpty(D)) {
            toast("Nhập đủ 4 đáp án"); reEnable(); return;
        }
        int checkedId = correctGroup.getCheckedRadioButtonId();
        int correctIndex = -1;
        if (checkedId == R.id.rbA) correctIndex = 0;
        else if (checkedId == R.id.rbB) correctIndex = 1;
        else if (checkedId == R.id.rbC) correctIndex = 2;
        else if (checkedId == R.id.rbD) correctIndex = 3;
        if (correctIndex == -1) { toast("Chọn đáp án đúng"); reEnable(); return; }

        dlg = ProgressDialog.show(this, null,
                (docId == null ? "Đang lưu..." : "Đang cập nhật..."), true, false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        data.put("options", Arrays.asList(A, B, C, D));
        data.put("correctIndex", correctIndex);
        data.put("category", category);
        data.put("difficulty", difficulty);
        data.put("explain", explain);
        data.put("createdBy", uid);
        if (docId == null) data.put("createdAt", new Date());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (docId == null) {
            // Thêm mới
            db.collection("questions").add(data)
                    .addOnSuccessListener(r -> {
                        closeDlg();
                        toast("Đã lưu câu hỏi!");
                        clearForm();
                        reEnable();
                    })
                    .addOnFailureListener(e -> {
                        closeDlg();
                        toast("Lỗi lưu: " + e.getMessage());
                        reEnable();
                    });
        } else {
            // Cập nhật
            db.collection("questions").document(docId).update(data)
                    .addOnSuccessListener(r -> {
                        closeDlg();
                        toast("Đã cập nhật!");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        closeDlg();
                        toast("Lỗi cập nhật: " + e.getMessage());
                        reEnable();
                    });
        }
    }

    /** Dọn form cho lần nhập mới */
    private void clearForm() {
        edtQuestion.setText("");
        edtA.setText(""); edtB.setText(""); edtC.setText(""); edtD.setText("");
        edtExplain.setText("");
        correctGroup.clearCheck();
        edtQuestion.requestFocus();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void reEnable() { btnSave.setEnabled(true); }
    private void closeDlg() { if (dlg != null) dlg.dismiss(); }
}
