package com.example.quizapp;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.databinding.ActivityRegisterBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding b;
    private Uri selectedAvatarUri = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    b.avatarIv.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Prefill email nếu có
        String prefill = getIntent().getStringExtra("prefillEmail");
        if (prefill != null && !prefill.isEmpty()) b.emailEt.setText(prefill);

        b.btnPickAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        b.dobEt.setOnClickListener(v -> openDatePicker());
        b.btnRegister.setOnClickListener(v -> registerUser());
    }

    private void openDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày sinh").build();
        picker.addOnPositiveButtonClickListener(selection -> {
            String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(selection));
            b.dobEt.setText(date);
        });
        picker.show(getSupportFragmentManager(), "DOB");
    }

    private String getSelectedRole() {
        int checkedId = b.roleGroup.getCheckedRadioButtonId();
        RadioButton rb = findViewById(checkedId);
        if (rb == null) return "student";
        return rb.getId() == R.id.rbTeacher ? "teacher" : "student";
    }

    private void registerUser() {
        String name  = b.fullNameEt.getText().toString().trim();
        String dob   = b.dobEt.getText().toString().trim();
        String phone = b.phoneEt.getText().toString().trim();
        String email = b.emailEt.getText().toString().trim();
        String pass  = b.passEt.getText().toString().trim();
        String role  = getSelectedRole();

        if (selectedAvatarUri == null) { toast("Chọn ảnh đại diện"); return; }
        if (TextUtils.isEmpty(name))  { b.fullNameEt.setError("Nhập họ tên"); return; }
        if (TextUtils.isEmpty(dob))   { b.dobEt.setError("Chọn ngày sinh"); return; }
        if (TextUtils.isEmpty(phone)) { b.phoneEt.setError("Nhập số điện thoại"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.emailEt.setError("Email không hợp lệ"); return; }
        if (pass.length() < 6) { b.passEt.setError("Mật khẩu ≥ 6 ký tự"); return; }

        ProgressDialog dlg = ProgressDialog.show(this, null, "Đang tạo tài khoản...", true, false);

        // 1) Tạo tài khoản Auth
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(auth -> {
                    String uid = auth.getUser().getUid();

                    // 2) Ảnh → Base64
                    String avatarBase64 = encodeImageToBase64(selectedAvatarUri);
                    if (avatarBase64 == null) {
                        dlg.dismiss(); toast("Không thể xử lý ảnh."); return;
                    }

                    // 3) Lưu users/{uid}
                    Map<String, Object> settings = new HashMap<>();
                    settings.put("bgm", false);
                    settings.put("sfx", false);
                    settings.put("perQuestionTimerOn", false);
                    settings.put("questionsPerPlay", 5);

                    Map<String, Object> user = new HashMap<>();
                    user.put("displayName", name);
                    user.put("dob", dob);
                    user.put("phone", phone);
                    user.put("email", email);   // 👈 lưu email
                    user.put("role", role);
                    user.put("avatarBase64", avatarBase64);
                    user.put("createdAt", new Date());
                    user.put("settings", settings);

                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(v -> {
                                dlg.dismiss();
                                // Đi thẳng vào Home theo role
                                Class<?> dest = "teacher".equals(role)
                                        ? TeacherHomeActivity.class : StudentHomeActivity.class;
                                startActivity(new android.content.Intent(this, dest)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                finish();
                            })
                            .addOnFailureListener(e -> { dlg.dismiss(); toast("Lỗi lưu Firestore: " + e.getMessage()); });
                })
                .addOnFailureListener(e -> { dlg.dismiss(); toast(e.getMessage()); });
    }

    private String encodeImageToBase64(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap src = BitmapFactory.decodeStream(in);
            if (src == null) return null;
            int w = src.getWidth(), h = src.getHeight();
            int target = 256;
            float scale = Math.min((float) target / w, (float) target / h);
            Bitmap resized = Bitmap.createScaledBitmap(src,
                    Math.max(1, Math.round(w * scale)),
                    Math.max(1, Math.round(h * scale)), true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
