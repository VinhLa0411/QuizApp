package com.example.quizapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountFragment extends Fragment {

    private ImageView avatarIv;
    private TextView nameTv, roleTv, emailTv, phoneTv, dobTv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_account, container, false);

        avatarIv = v.findViewById(R.id.avatarIv);
        nameTv   = v.findViewById(R.id.nameTv);
        roleTv   = v.findViewById(R.id.roleTv);
        emailTv  = v.findViewById(R.id.emailTv);
        phoneTv  = v.findViewById(R.id.phoneTv);
        dobTv    = v.findViewById(R.id.dobTv);

        // Đăng xuất
        v.findViewById(R.id.logoutBtn).setOnClickListener(btn -> {
            FirebaseAuth.getInstance().signOut();
            // quay về LoginActivity
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        // Chỉnh sửa thông tin
        v.findViewById(R.id.editBtn).setOnClickListener(btn ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        // Xem lịch sử làm bài
        v.findViewById(R.id.historyBtn).setOnClickListener(btn ->
                startActivity(new Intent(requireContext(), HistoryActivity.class)));

        loadProfile();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // nếu vừa chỉnh sửa xong quay lại -> reload
        loadProfile();
    }

    private void loadProfile() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(requireContext(), "Không tìm thấy hồ sơ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    nameTv.setText(s(doc.getString("displayName")));
                    emailTv.setText(s(doc.getString("email")));
                    phoneTv.setText(s(doc.getString("phone")));
                    dobTv.setText(s(doc.getString("dob")));

                    String role = doc.getString("role");
                    roleTv.setText("teacher".equals(role) ? "Giảng viên" : "Học viên");

                    String b64 = doc.getString("avatarBase64");
                    if (b64 != null && !b64.isEmpty()) {
                        try {
                            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            if (bmp != null) avatarIv.setImageBitmap(bmp);
                        } catch (Exception ignored) {}
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Lỗi tải hồ sơ: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private static String s(String x) { return x == null ? "" : x; }
}
