package com.error.onlineexam;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Objects;

public class Result extends AppCompatActivity {

    private Question[] data;
    private String uid;
    private String quizID;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.result_main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        quizID = getIntent().getStringExtra("Quiz ID");
        uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        if (getIntent().hasExtra("User UID")) uid = getIntent().getStringExtra("User UID");

        TextView title = findViewById(R.id.title);
        ListView listView = findViewById(R.id.listview);
        TextView total = findViewById(R.id.total);

        db = FirebaseFirestore.getInstance();
        loadResults(title, listView, total);
    }

    private void loadResults(TextView title, ListView listView, TextView total) {
        db.collection("Quizzes").document(quizID).get().addOnSuccessListener(quizSnapshot -> {
            if (quizSnapshot.exists()) {
                title.setText(quizSnapshot.getString("title"));
                Long totalQCount = quizSnapshot.getLong("totalQuestions");
                int num = totalQCount != null ? totalQCount.intValue() : 0;

                db.collection("Quizzes").document(quizID).collection("Answers").document(uid).get().addOnSuccessListener(ansSnapshot -> {
                    db.collection("Quizzes").document(quizID).collection("Questions").get().addOnSuccessListener(queryDocumentSnapshots -> {
                        data = new Question[num];
                        int correctAns = 0;
                        
                        for (QueryDocumentSnapshot qDoc : queryDocumentSnapshots) {
                            int index;
                            try {
                                index = Integer.parseInt(qDoc.getId());
                            } catch (NumberFormatException e) {
                                continue;
                            }
                            
                            if (index >= num) continue;

                            Question question = qDoc.toObject(Question.class);
                            if (ansSnapshot.contains(String.valueOf(index + 1))) {
                                Long selected = ansSnapshot.getLong(String.valueOf(index + 1));
                                question.setSelectAnswer(selected != null ? selected.intValue() : 0);
                            }
                            if (question.getCorrectAnswer() == question.getSelectAnswer()) {
                                correctAns++;
                            }
                            data[index] = question;
                        }

                        total.setText("Total " + correctAns + "/" + num);
                        ListAdapter listAdapter = new ListAdapter(data);
                        listView.setAdapter(listAdapter);
                    });
                });
            } else {
                finish();
            }
        });
    }
    public class ListAdapter extends BaseAdapter {
            Question [] arr;

            ListAdapter(Question[] arr2){
                arr =arr2;
            }

            @Override
            public int getCount() {
                return arr.length;
            }

            @Override
            public Object getItem(int position) {
                return arr[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @SuppressLint("SetTextI18n")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                LayoutInflater inflater = getLayoutInflater();
                @SuppressLint({"InflateParams", "ViewHolder"}) View v = inflater.inflate(R.layout.question,null);

                TextView question = v.findViewById(R.id.question);
                RadioButton option1 = v.findViewById(R.id.option1);
                RadioButton option2 = v.findViewById(R.id.option2);
                RadioButton option3 = v.findViewById(R.id.option3);
                RadioButton option4 = v.findViewById(R.id.option4);
                TextView result = v.findViewById(R.id.result);

                question.setText(data[position].getQuestion());
                option1.setText(data[position].getOption1());
                option2.setText(data[position].getOption2());
                option3.setText(data[position].getOption3());
                option4.setText(data[position].getOption4());



                switch (data[position].getSelectAnswer()){
                    case 1 :
                        option1.setChecked(true);
                        break;

                    case 2 :
                        option2.setChecked(true);
                        break;
                    case 3 :
                        option3.setChecked(true);
                        break;

                    case 4 :
                        option4.setChecked(true);
                        break;
                }
                option1.setEnabled(false);
                option2.setEnabled(false);
                option3.setEnabled(false);
                option4.setEnabled(false);
                result.setVisibility(View.VISIBLE);

                if (data[position].getSelectAnswer()==data[position].getCorrectAnswer()){
                    result.setBackgroundResource(R.drawable.green_background);
                    result.setTextColor(ContextCompat.getColor(Result.this, R.color.green_dark));
                    result.setText("Correct Answer");
                }else {
                    result.setBackgroundResource(R.drawable.red_background);
                    result.setTextColor(ContextCompat.getColor(Result.this, R.color.red_dark));
                    result.setText("Wrong Answer");

                    switch (data[position].getCorrectAnswer()){
                        case 1 :
                            option1.setBackgroundResource(R.drawable.green_background);
                            break;
                        case 2 :
                            option2.setBackgroundResource(R.drawable.green_background);
                            break;
                        case 3 :
                            option3.setBackgroundResource(R.drawable.green_background);
                            break;
                        case 4 :
                            option4.setBackgroundResource(R.drawable.green_background);
                            break;
                    }
                }

                return v;
            }
        }
}