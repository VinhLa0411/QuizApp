package com.example.quizapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEt, passEt;
    private Button loginBtn, registerBtn;
    private TextView forgotTv, switchRoleTv;
    private CheckBox showPassCb;
    private MaterialButton googleBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    // false = student mode (mặc định), true = teacher mode
    private boolean isTeacherMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        emailEt = findViewById(R.id.emailEt);
        passEt = findViewById(R.id.passEt);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);
        forgotTv = findViewById(R.id.forgotTv);
        showPassCb = findViewById(R.id.showPassCb);
        googleBtn = findViewById(R.id.googleSignInBtn);
        switchRoleTv = findViewById(R.id.switchRoleTv);

        // Show/Hide password
        showPassCb.setOnCheckedChangeListener((cb, checked) -> {
            passEt.setInputType(checked
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passEt.setSelection(passEt.getText().length());
        });

        // Toggle teacher/student
        switchRoleTv.setOnClickListener(v -> toggleRole());

        // Actions
        loginBtn.setOnClickListener(v -> login());
        registerBtn.setOnClickListener(v -> register()); // dùng hàm register() của lớp
        forgotTv.setOnClickListener(v -> resetPassword());

        // Google sign-in
        setupGoogleSignIn();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            // Đã có session -> vào thẳng theo role
            routeToHomeByRole(true);
        }
    }

    private void toggleRole() {
        isTeacherMode = !isTeacherMode;
        if (isTeacherMode) {
            switchRoleTv.setText("Đăng nhập với tư cách Học viên");
            loginBtn.setText("Đăng nhập Giảng viên");
            registerBtn.setEnabled(false);        // giáo viên không tự đăng ký
            // Google thường dùng cho học viên → có thể ẩn nếu muốn
            // googleBtn.setVisibility(View.GONE);
            Toast.makeText(this, "Chế độ: Giảng viên", Toast.LENGTH_SHORT).show();
        } else {
            switchRoleTv.setText("Đăng nhập với tư cách Giảng viên");
            loginBtn.setText("Đăng nhập");
            registerBtn.setEnabled(true);
            // googleBtn.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Chế độ: Học viên", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- Email/Password ----------------
    private void login() {
        String email = emailEt.getText().toString().trim();
        String pass  = passEt.getText().toString().trim();
        if (!validate(email, pass)) return;

        ProgressDialog dlg = ProgressDialog.show(this, null, "Đang đăng nhập...", true, false);

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    dlg.dismiss();
                    // Sau khi đăng nhập thành công, điều hướng theo role
                    routeToHomeByRole(false);
                })
                .addOnFailureListener(e -> {
                    dlg.dismiss();
                    Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void register() {
        if (isTeacherMode) {
            Toast.makeText(this, "Chỉ quản trị viên mới thêm giảng viên", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = emailEt.getText().toString().trim();
        String pass = passEt.getText().toString().trim();
        if (!validate(email, pass)) return;

        ProgressDialog dlg = ProgressDialog.show(this, null, "Đang tạo tài khoản...", true, false);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> {
                    String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                    createUserDocIfMissing(uid, email, "student", dlg, /*thenRoute=*/true);
                })
                .addOnFailureListener(e -> {
                    dlg.dismiss();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetPassword() {
        String email = emailEt.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Nhập email để đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> Toast.makeText(this, "Đã gửi email đặt lại mật khẩu", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private boolean validate(String email, String pass) {
        if (TextUtils.isEmpty(email)) {
            emailEt.setError("Email không được trống");
            return false;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            passEt.setError("Mật khẩu ≥ 6 ký tự");
            return false;
        }
        return true;
    }

    // ---------------- Google Sign-in ----------------
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (data == null) {
                        Toast.makeText(this, "Không nhận được dữ liệu từ Google", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            Toast.makeText(this, "Google sign-in failed.", Toast.LENGTH_LONG).show();
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        googleBtn.setOnClickListener(v -> googleLauncher.launch(googleClient.getSignInIntent()));
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) {
            Toast.makeText(this, "Thiếu ID token", Toast.LENGTH_LONG).show();
            return;
        }
        ProgressDialog dlg = ProgressDialog.show(this, null, "Đang đăng nhập Google...", true, false);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    String uid = Objects.requireNonNull(authResult.getUser()).getUid();
                    String displayName = authResult.getUser().getDisplayName();
                    // Mặc định Google tạo học viên; admin có thể đổi role sau
                    createUserDocIfMissing(uid,
                            (displayName == null || displayName.isEmpty()) ? "User" : displayName,
                            "student",
                            dlg,
                            /*thenRoute=*/true);
                })
                .addOnFailureListener(e -> {
                    dlg.dismiss();
                    Toast.makeText(this, "Firebase auth failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ---------------- Firestore helpers ----------------
    private void createUserDocIfMissing(String uid, String name, String role,
                                        ProgressDialog dlg, boolean thenRoute) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        Map<String, Object> settings = new HashMap<>();
                        settings.put("bgm", false);
                        settings.put("sfx", false);
                        settings.put("perQuestionTimerOn", false);
                        settings.put("questionsPerPlay", 5);

                        Map<String, Object> user = new HashMap<>();
                        user.put("displayName", name);
                        user.put("role", role);
                        user.put("avatarId", "default");
                        user.put("settings", settings);

                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(v -> {
                                    if (dlg != null) dlg.dismiss();
                                    if (thenRoute) routeToHomeByRole(true);
                                })
                                .addOnFailureListener(e -> {
                                    if (dlg != null) dlg.dismiss();
                                    Toast.makeText(this, "Tạo hồ sơ thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        if (dlg != null) dlg.dismiss();
                        if (thenRoute) routeToHomeByRole(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dlg != null) dlg.dismiss();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Đọc users/{uid} -> role, kiểm tra hợp lệ với mode hiện tại,
     * rồi điều hướng tới Home tương ứng. Nếu sai role khi ở Teacher mode -> signOut.
     */
    private void routeToHomeByRole(boolean clearTask) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    String role = doc.getString("role");
                    if (role == null) role = "student";

                    if (isTeacherMode && !"teacher".equals(role)) {
                        Toast.makeText(this, "Tài khoản này không có quyền giảng viên", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        // Nếu dùng Google, nên signOut Google nữa (tuỳ bạn):
                        GoogleSignIn.getClient(this,
                                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        ).signOut();
                        return;
                    }

                    Class<?> dest = "teacher".equals(role)
                            ? TeacherHomeActivity.class
                            : StudentHomeActivity.class;

                    Intent i = new Intent(this, dest);
                    if (clearTask) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    }
                    startActivity(i);
                    if (clearTask) finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi đọc role: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
