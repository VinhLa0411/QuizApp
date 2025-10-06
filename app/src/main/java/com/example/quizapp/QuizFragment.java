package com.example.quizapp;

import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class QuizFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_quiz, container, false);

        // sau sẽ dẫn tới màn chọn chủ đề / bắt đầu quiz
        Button startBtn = v.findViewById(R.id.startQuizBtn);
        startBtn.setOnClickListener(view -> {
            // TODO: mở màn chọn chủ đề hoặc QuizActivity
        });

        return v;
    }
}
