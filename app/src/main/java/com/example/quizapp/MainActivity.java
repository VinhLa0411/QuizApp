package com.example.quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeTv, roleTv;
    private Button logoutBtn, profileBtn, quizBtn, addQuestionBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Map view
        welcomeTv = findViewById(R.id.welcomeTv);
        roleTv = findViewById(R.id.roleTv);
        logoutBtn = findViewById(R.id.logoutBtn);
        profileBtn = findViewById(R.id.profileBtn);
        quizBtn = findViewById(R.id.quizBtn);
        addQuestionBtn = findViewById(R.id.addQuestionBtn);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Chưa đăng nhập -> về Login
        if (mAuth.getCurrentUser() == null) {
            goTo(LoginActivity.class, true);
            return;
        }

        // Tạm ẩn các nút cho tới khi biết role
        addQuestionBtn.setVisibility(View.GONE);
        quizBtn.setVisibility(View.GONE);

        // Tải thông tin người dùng & điều hướng theo role
        loadUserInfoAndRoute();

        // Đăng xuất
        logoutBtn.setOnClickListener(v -> {
            // Firebase sign out
            FirebaseAuth.getInstance().signOut();
            // Google sign out (nếu có đăng nhập Google)
            GoogleSignIn.getClient(
                    this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build()
            ).signOut();

            goTo(LoginActivity.class, true);
        });

        // Tuỳ bạn có các Activity này hay không — nếu chưa có, có thể comment lại
        /*
        profileBtn.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        quizBtn.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        addQuestionBtn.setOnClickListener(v -> startActivity(new Intent(this, AddQuestionActivity.class)));
        */
    }

    private void loadUserInfoAndRoute() {
        final String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        String role = doc.getString("role"); // expected: "student" | "teacher"

                        if (name == null || name.trim().isEmpty()) name = "bạn";
                        welcomeTv.setText("Xin chào, " + name + "!");

                        // Điều hướng thẳng theo role (ưu tiên trải nghiệm)
                        if ("student".equals(role)) {
                            roleTv.setText("Vai trò: Học viên");
                            goTo(StudentHomeActivity.class, true);
                            return;
                        } else if ("teacher".equals(role)) {
                            roleTv.setText("Vai trò: Giảng viên");
                            goTo(TeacherHomeActivity.class, true);
                            return;
                        }

                        // Nếu role không hợp lệ/thiếu: vẫn ở màn này và cấu hình nút mặc định cho học viên
                        roleTv.setText("Vai trò: Chưa xác định");
                        addQuestionBtn.setVisibility(View.GONE);
                        quizBtn.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Không tìm thấy hồ sơ người dùng!", Toast.LENGTH_SHORT).show();
                        roleTv.setText("Vai trò: (chưa có)");
                        quizBtn.setVisibility(View.VISIBLE);
                        addQuestionBtn.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải người dùng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Trường hợp lỗi mạng: vẫn cho dùng như học viên
                    roleTv.setText("Vai trò: (không xác định)");
                    quizBtn.setVisibility(View.VISIBLE);
                    addQuestionBtn.setVisibility(View.GONE);
                });
    }

    private void goTo(Class<?> clazz, boolean clearTask) {
        Intent intent = new Intent(this, clazz);
        if (clearTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        startActivity(intent);
    }
}
