package com.error.onlineexam;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Exam extends AppCompatActivity {
    private Question[] data;
    private String quizID;
    private String uid;
    private int oldTotalPoint = 0;
    private int oldTotalQuestion = 0;
    private ListAdapter listAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exam);
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.exam_main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        quizID = getIntent().getStringExtra("Quiz ID");
        if (quizID == null) {
            Toast.makeText(this, "Quiz ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ListView listView = findViewById(R.id.listview);
        Button submit = findViewById(R.id.submit);
        TextView title = findViewById(R.id.title);

        // Initialize Firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = auth.getCurrentUser().getUid();

        // Initialize adapter with empty data
        data = new Question[0];
        listAdapter = new ListAdapter(data);
        listView.setAdapter(listAdapter);

        db = FirebaseFirestore.getInstance();
        loadQuizAndUserStats(listView, title);

        submit.setOnClickListener(v -> submitQuiz());
    }

    private void loadQuizAndUserStats(ListView listView, TextView title) {
        // Check if already solved first
        db.collection("Quizzes").document(quizID).collection("Answers").document(uid).get().addOnSuccessListener(ansDoc -> {
            if (ansDoc.exists()) {
                Toast.makeText(Exam.this, "Already solved! Opening results...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Exam.this, Result.class);
                intent.putExtra("Quiz ID", quizID);
                startActivity(intent);
                finish();
                return;
            }

            db.collection("Quizzes").document(quizID).get().addOnSuccessListener(quizSnapshot -> {
                if (!quizSnapshot.exists()) {
                    Toast.makeText(Exam.this, "Quiz not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                title.setText(quizSnapshot.getString("title"));

                db.collection("Quizzes").document(quizID).collection("Questions").get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            int questionCount = queryDocumentSnapshots.size();
                            data = new Question[questionCount];
                            
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                try {
                                    int index = Integer.parseInt(doc.getId());
                                    if (index < questionCount) {
                                        data[index] = doc.toObject(Question.class);
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e("ExamActivity", "Invalid question ID: " + doc.getId());
                                }
                            }
                            listAdapter.updateData(data);
                        });

                db.collection("Users").document(uid).get().addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        Long points = userSnapshot.getLong("totalPoints");
                        if (points != null) oldTotalPoint = points.intValue();

                        Long questions = userSnapshot.getLong("totalQuestions");
                        if (questions != null) oldTotalQuestion = questions.intValue();
                    }
                });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(Exam.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void submitQuiz() {
        if (data == null || data.length == 0) {
            Toast.makeText(this, "No questions to submit", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> answers = new HashMap<>();
        int pCount = 0;

        for (int i = 0; i < data.length; i++) {
            if (data[i] == null) continue;
            answers.put(String.valueOf(i + 1), data[i].getSelectAnswer());
            if (data[i].getSelectAnswer() == data[i].getCorrectAnswer()) {
                pCount++;
            }
        }
        answers.put("points", pCount);

        final int finalPoints = pCount;
        db.collection("Quizzes").document(quizID).collection("Answers").document(uid)
                .set(answers)
                .addOnSuccessListener(aVoid -> {
                    db.collection("Users").document(uid).get().addOnSuccessListener(userSnapshot -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("totalPoints", com.google.firebase.firestore.FieldValue.increment(finalPoints));
                        updates.put("totalQuestions", com.google.firebase.firestore.FieldValue.increment(data.length));
                        updates.put("quizzesSolved", com.google.firebase.firestore.FieldValue.arrayUnion(quizID));

                        db.collection("Users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge());

                        Intent intent = new Intent(Exam.this, Result.class);
                        intent.putExtra("Quiz ID", quizID);
                        startActivity(intent);
                        finish();
                    });
                });
    }

    public class ListAdapter extends BaseAdapter {
        private Question[] arr;

        ListAdapter(Question[] arr2) {
            this.arr = arr2;
        }

        public void updateData(Question[] newData) {
            this.arr = newData;
            notifyDataSetChanged();
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

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View v = inflater.inflate(R.layout.question, parent, false);

            TextView question = v.findViewById(R.id.question);
            RadioButton option1 = v.findViewById(R.id.option1);
            RadioButton option2 = v.findViewById(R.id.option2);
            RadioButton optionButton3 = v.findViewById(R.id.option3);
            RadioButton option4 = v.findViewById(R.id.option4);

            Question currentQuestion = arr[position];
            if (currentQuestion == null) {
                return v;
            }

            question.setText(currentQuestion.getQuestion());
            option1.setText(currentQuestion.getOption1());
            option2.setText(currentQuestion.getOption2());
            optionButton3.setText(currentQuestion.getOption3());
            option4.setText(currentQuestion.getOption4());

            CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
                if (isChecked) {
                    int selectedAnswer = 0;
                    if (buttonView == option1) selectedAnswer = 1;
                    else if (buttonView == option2) selectedAnswer = 2;
                    else if (buttonView == optionButton3) selectedAnswer = 3;
                    else if (buttonView == option4) selectedAnswer = 4;

                    currentQuestion.setSelectAnswer(selectedAnswer);
                }
            };

            option1.setOnCheckedChangeListener(listener);
            option2.setOnCheckedChangeListener(listener);
            optionButton3.setOnCheckedChangeListener(listener);
            option4.setOnCheckedChangeListener(listener);

            // Restore selected answer
            switch (currentQuestion.getSelectAnswer()) {
                case 1:
                    option1.setChecked(true);
                    break;
                case 2:
                    option2.setChecked(true);
                    break;
                case 3:
                    optionButton3.setChecked(true);
                    break;
                case 4:
                    option4.setChecked(true);
                    break;
            }

            return v;
        }
    }
}
