package com.example.quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TeacherQuestionsFragment extends Fragment {

    private RecyclerView rv;
    private QuestionAdapter adapter;
    private final List<QuestionItem> data = new ArrayList<>();
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_questions, container, false);

        rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new QuestionAdapter(data, new QuestionAdapter.OnItemAction() {
            @Override public void onEdit(QuestionItem item) {
                Intent i = new Intent(requireContext(), AddQuestionActivity.class);
                i.putExtra("docId", item.id);
                startActivity(i);
            }
            @Override public void onDelete(QuestionItem item) {
                FirebaseFirestore.getInstance().collection("questions").document(item.id)
                        .delete()
                        .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                "Xoá lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        rv.setAdapter(adapter);

        FloatingActionButton fab = v.findViewById(R.id.fabAdd);
        fab.setOnClickListener(btn ->
                // Đổi sang CreateQuizActivity nếu muốn tạo bài nhiều câu:
                startActivity(new Intent(requireContext(), CreateQuizActivity.class)));
        fab.show();

        // ----- ĐẨY FAB & CHỪA ĐÁY RECYCLER TRÁNH BỊ CHE BỞI BOTTOM BAR -----
        ViewCompat.setOnApplyWindowInsetsListener(fab, (view, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            int extra = (int) (view.getResources().getDisplayMetrics().density * 56); // khoảng an toàn
            lp.bottomMargin = sys.bottom + extra;
            fab.setLayoutParams(lp);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(rv, (view, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int pad = (int) (view.getResources().getDisplayMetrics().density * 80);
            rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(),
                    rv.getPaddingRight(), sys.bottom + pad);
            return insets;
        });
        // --------------------------------------------------------------------

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Không dùng orderBy để tránh cần composite index; sort ở client
        registration = FirebaseFirestore.getInstance()
                .collection("questions")
                .whereEqualTo("createdBy", user.getUid())
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(requireContext(),
                                "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    data.clear();
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            QuestionItem q = new QuestionItem();
                            q.id = d.getId();
                            q.question = d.getString("question");
                            q.category = d.getString("category");
                            q.createdAt = d.getDate("createdAt");

                            @SuppressWarnings("unchecked")
                            List<String> opts = (List<String>) d.get("options");
                            Long idx = d.getLong("correctIndex");
                            if (opts != null && idx != null && idx >= 0 && idx < opts.size()) {
                                q.correct = opts.get(idx.intValue());
                            } else q.correct = "";
                            data.add(q);
                        }
                    }

                    // sort theo createdAt desc (mới nhất trên cùng)
                    Collections.sort(data, (o1, o2) -> {
                        Date a = o1.createdAt, b = o2.createdAt;
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        return b.compareTo(a);
                    });

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    // POJO cho adapter
    public static class QuestionItem {
        public String id;
        public String question;
        public String category;
        public String correct;
        public Date createdAt;
    }
}
