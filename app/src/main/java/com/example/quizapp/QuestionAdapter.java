package com.example.quizapp;

import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.VH> {

    public interface OnItemAction {
        void onEdit(TeacherQuestionsFragment.QuestionItem item);
        void onDelete(TeacherQuestionsFragment.QuestionItem item);
    }

    private final List<TeacherQuestionsFragment.QuestionItem> items;
    private final OnItemAction action;

    public QuestionAdapter(List<TeacherQuestionsFragment.QuestionItem> items, OnItemAction action) {
        this.items = items; this.action = action;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_question, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        var it = items.get(pos);
        h.title.setText(it.question);
        h.sub.setText("Chủ đề: " + (it.category==null?"":it.category) + " • Đúng: " + it.correct);
        h.btnEdit.setOnClickListener(v -> action.onEdit(it));
        h.btnDel.setOnClickListener(v -> action.onDelete(it));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, sub; ImageButton btnEdit, btnDel;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            sub   = v.findViewById(R.id.sub);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDel  = v.findViewById(R.id.btnDel);
        }
    }
}
