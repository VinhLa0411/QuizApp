package com.example.quizapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StudentHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_quiz)      f = new QuizFragment();
            else /* R.id.nav_account */   f = new AccountFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.studentFragmentContainer, f)
                    .commit();
            return true;
        });

        // mở mặc định tab Làm bài
        bottomNav.setSelectedItemId(R.id.nav_quiz);
    }
}
