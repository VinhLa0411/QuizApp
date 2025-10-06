package com.example.quizapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quizapp.databinding.ActivityTeacherHomeBinding;

public class TeacherHomeActivity extends AppCompatActivity {
    private ActivityTeacherHomeBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityTeacherHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.textView.setText("Xin chào GIẢNG VIÊN!");
    }
}
