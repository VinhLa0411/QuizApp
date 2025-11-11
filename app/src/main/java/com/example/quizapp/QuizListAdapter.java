package com.example.quizapp;

import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class QuizListAdapter extends RecyclerView.Adapter<QuizListAdapter.VH> {

    // Interface callback để xử lý sự kiện khi bấm vào nút "Làm ngay"
    public interface OnStartClick {
        void onStart(QuizFragment.QuizItem item); // Phương thức khi bấm nút "Làm ngay"
    }

    private final List<QuizFragment.QuizItem> data; // Danh sách bài quiz
    private final OnStartClick cb; // Callback xử lý sự kiện khi bấm "Làm ngay"

    public QuizListAdapter(List<QuizFragment.QuizItem> data, OnStartClick cb) {
        this.data = data;
        this.cb = cb;
    }

    // Tạo ViewHolder để hiển thị mỗi item quiz
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        // Inflating layout row_quiz_item.xml
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.row_quiz_item, p, false);
        return new VH(view);
    }

    // Gắn dữ liệu vào các View trong ViewHolder
    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        QuizFragment.QuizItem it = data.get(i); // Lấy đối tượng QuizItem tại vị trí i

        // Hiển thị tên bài quiz
        h.tvTitle.setText(it.title == null ? "(Chưa có tiêu đề)" : it.title);

        // Hiển thị tên giảng viên
        String teacherInfo = it.teacherName != null ? "Giảng viên: " + it.teacherName : "Giảng viên chưa có";
        h.tvTeacher.setText(teacherInfo);

        // Xây dựng chuỗi mô tả về quiz (số câu hỏi, thời gian, v.v.)
        String meta = (it.category == null ? "" : it.category) +
                " • " + it.numQuestions + " câu" +
                (it.quizTimerOn ? (" • " + it.quizSeconds + "s toàn bài") : "") +
                (it.perQuestionTimerOn ? (" • " + it.perQuestionSeconds + "s/câu") : "");
        h.tvMeta.setText(meta);

        // Xử lý sự kiện khi bấm nút "Làm ngay"
        h.btnStart.setOnClickListener(v -> cb.onStart(it)); // Khi bấm "Làm ngay", gọi phương thức onStart
    }

    // Trả về số lượng item trong adapter
    @Override
    public int getItemCount() {
        return data.size(); // Trả về số lượng câu hỏi
    }

    // ViewHolder chứa các phần tử giao diện của mỗi item
    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta, tvTeacher; // Tên quiz, thông tin mô tả, tên giảng viên
        Button btnStart; // Nút "Làm ngay"

        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle); // Gắn TextView cho tên quiz
            tvMeta = v.findViewById(R.id.tvMeta);   // Gắn TextView cho mô tả quiz
            tvTeacher = v.findViewById(R.id.tvTeacherName); // Gắn TextView cho tên giảng viên
            btnStart = v.findViewById(R.id.btnStart); // Gắn Button "Làm ngay"
        }
    }
}
