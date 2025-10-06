package com.example.quizapp;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.databinding.ActivityEditProfileBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding b;
    private Uri newAvatarUri = null;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.btnBack.setOnClickListener(v -> finish());
        b.btnPickAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        b.dobEt.setOnClickListener(v -> showDatePicker());
        b.btnSave.setOnClickListener(v -> saveProfile());

        loadCurrent();
    }

    // chọn ảnh
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    newAvatarUri = uri;
                    b.avatarIv.setImageURI(uri);
                }
            });

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày sinh").build();
        picker.addOnPositiveButtonClickListener(time -> {
            String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(time));
            b.dobEt.setText(date);
        });
        picker.show(getSupportFragmentManager(), "DOB");
    }

    private void loadCurrent() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    b.nameEt.setText(doc.getString("displayName"));
                    b.phoneEt.setText(doc.getString("phone"));
                    b.dobEt.setText(doc.getString("dob"));
                    b.emailTv.setText(doc.getString("email"));
                    b.roleTv.setText("teacher".equals(doc.getString("role")) ? "Giảng viên" : "Học viên");

                    String b64 = doc.getString("avatarBase64");
                    if (b64 != null && !b64.isEmpty()) {
                        byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bmp != null) b.avatarIv.setImageBitmap(bmp);
                    }
                });
    }

    private void saveProfile() {
        String name  = b.nameEt.getText().toString().trim();
        String phone = b.phoneEt.getText().toString().trim();
        String dob   = b.dobEt.getText().toString().trim();

        if (name.isEmpty()) { b.nameEt.setError("Nhập họ tên"); return; }
        if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
            b.phoneEt.setError("SĐT không hợp lệ"); return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ProgressDialog dlg = ProgressDialog.show(this, null, "Đang cập nhật...", true, false);

        // ảnh (base64) nếu thay
        String avatarBase64 = null;
        if (newAvatarUri != null) avatarBase64 = encodeToBase64(newAvatarUri);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        var updates = new java.util.HashMap<String, Object>();
        updates.put("displayName", name);
        updates.put("phone", phone);
        updates.put("dob", dob);
        if (avatarBase64 != null) updates.put("avatarBase64", avatarBase64);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(v -> {
                    dlg.dismiss();
                    Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    dlg.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // nén ảnh nhỏ rồi Base64 (giống lúc đăng ký)
    private String encodeToBase64(Uri uri) {
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
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}
