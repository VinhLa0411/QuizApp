package com.example.quizapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;
    private final ArrayList<String> items = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView lv = new ListView(this);
        setContentView(lv);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        lv.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("attempts")
                .whereEqualTo("userId", uid)
                .orderBy("startedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();
                    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
                    snap.forEach(doc -> {
                        String cat = doc.getString("category");
                        Long score = doc.getLong("score");
                        Long total = doc.getLong("total");
                        com.google.firebase.Timestamp ts = doc.getTimestamp("startedAt");
                        String time = ts != null ? fmt.format(ts.toDate()) : "";
                        items.add(time + " • " + (cat==null?"":cat) + " • " + score + "/" + total);
                    });
                    adapter.notifyDataSetChanged();
                });
    }
}
