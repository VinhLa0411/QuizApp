package com.example.quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.*;

public class QuizFragment extends Fragment {

    private RecyclerView rv;
    private final List<QuizItem> data = new ArrayList<>();
    private QuizListAdapter adapter;
    private ListenerRegistration reg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_quiz, container, false);
        rv = v.findViewById(R.id.rvQuizzes);
        rv.setLayoutManager(new LinearLayoutManager(requireContext())); // Đặt LayoutManager cho RecyclerView

        // Khởi tạo adapter với dữ liệu và xử lý sự kiện khi người dùng bấm vào "Làm ngay"
        adapter = new QuizListAdapter(data, item -> {
            // Khi người dùng bấm "Làm ngay", mở TakeQuizActivity
            Intent i = new Intent(requireContext(), TakeQuizActivity.class);
            i.putExtra("quizId", item.id); // Truyền quizId sang TakeQuizActivity
            startActivity(i);
        });
        rv.setAdapter(adapter); // Thiết lập adapter cho RecyclerView

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Lấy dữ liệu quiz từ Firestore và lắng nghe sự thay đổi
        reg = FirebaseFirestore.getInstance().collection("quizzes")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        // Xử lý lỗi khi lấy dữ liệu
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    data.clear(); // Xóa dữ liệu cũ

                    // Duyệt qua các document trong snapshot
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            QuizItem q = new QuizItem();
                            q.id = d.getId(); // Lấy ID của quiz
                            q.title = d.getString("title"); // Lấy tiêu đề của quiz
                            q.category = d.getString("category"); // Lấy danh mục
                            q.numQuestions = d.getLong("numQuestions") == null ? 0 : d.getLong("numQuestions").intValue();
                            q.perQuestionTimerOn = Boolean.TRUE.equals(d.getBoolean("perQuestionTimerOn"));
                            q.perQuestionSeconds = d.getLong("perQuestionSeconds") == null ? 0 : d.getLong("perQuestionSeconds").intValue();
                            q.quizTimerOn = Boolean.TRUE.equals(d.getBoolean("quizTimerOn"));
                            q.quizSeconds = d.getLong("quizSeconds") == null ? 0 : d.getLong("quizSeconds").intValue();
                            q.createdAt = d.getDate("createdAt"); // Lấy thời gian tạo
                            q.ownerId = d.getString("ownerId"); // Lấy ownerId của quiz (giảng viên tạo quiz)
                            data.add(q); // Thêm quiz vào danh sách
                        }
                    }

                    // Lấy tên giảng viên (teacher) dựa trên ownerId
                    for (QuizItem item : data) {
                        fetchTeacherName(item);
                    }

                    // Sắp xếp quiz theo thời gian tạo (mới nhất lên đầu)
                    Collections.sort(data, (a, b) -> {
                        if (a.createdAt == null && b.createdAt == null) return 0;
                        if (a.createdAt == null) return 1;
                        if (b.createdAt == null) return -1;
                        return b.createdAt.compareTo(a.createdAt);
                    });

                    // Cập nhật giao diện với dữ liệu mới
                    adapter.notifyDataSetChanged();
                });
    }

    // Lấy tên giảng viên từ Firestore dựa trên ownerId
    private void fetchTeacherName(QuizItem quizItem) {
        FirebaseFirestore.getInstance().collection("users").document(quizItem.ownerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Lấy tên giảng viên từ trường displayName
                        quizItem.teacherName = doc.getString("displayName");
                        // Cập nhật lại giao diện (notify adapter)
                        adapter.notifyDataSetChanged(); // Cập nhật dữ liệu trong RecyclerView
                    }
                });
    }


    @Override
    public void onStop() {
        super.onStop();
        // Hủy lắng nghe khi Fragment bị dừng
        if (reg != null) reg.remove();
    }

    // POJO chứa thông tin quiz
    public static class QuizItem {
        public String id; // ID của quiz
        public String title; // Tiêu đề của quiz
        public String category; // Danh mục của quiz
        public String teacherName; // Tên giảng viên
        public int numQuestions; // Số câu hỏi
        public int perQuestionSeconds; // Thời gian cho mỗi câu hỏi (giây)
        public int quizSeconds; // Thời gian cho toàn bộ bài thi (giây)
        public boolean perQuestionTimerOn; // Kiểm tra có bật đồng hồ cho từng câu hỏi không
        public boolean quizTimerOn; // Kiểm tra có bật đồng hồ cho bài thi không
        public Date createdAt; // Thời gian tạo quiz
        public String ownerId; // Owner ID (giảng viên)
    }
}
