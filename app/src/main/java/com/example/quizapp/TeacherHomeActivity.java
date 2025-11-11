package com.example.quizapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TeacherHomeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_home);

        BottomNavigationView nav = findViewById(R.id.teacherBottomNav);
        nav.setOnItemSelectedListener(item -> {
            Fragment f = (item.getItemId()==R.id.nav_qbank) ? new TeacherQuestionsFragment() : new AccountFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.teacherFragmentContainer, f).commit();
            return true;
        });
        nav.setSelectedItemId(R.id.nav_qbank);
    }
}
