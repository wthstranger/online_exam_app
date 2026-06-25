package com.error.onlineexam;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Objects;

public class Home extends AppCompatActivity {

    private String userUID;
    private String firstname;
    AlertDialog progressDialog;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("HOME_TEST", "Home Activity Started");

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        
        View header = findViewById(R.id.header_layout);
        View root = findViewById(R.id.main_home);
        
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topPadding = systemBars.top + (int) (24 * getResources().getDisplayMetrics().density);
            int bottomPadding = (int) (60 * getResources().getDisplayMetrics().density); 
            header.setPadding(header.getPaddingLeft(), topPadding, header.getPaddingRight(), bottomPadding);
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
       AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.progress_dialog,null);

        builder.setView(dialogView);
        builder.setCancelable(false);

        progressDialog = builder.create();
        progressDialog.show();


        Bundle b = getIntent().getExtras();
        if (b != null) {
            userUID = b.getString("User UID");
        }
        if (userUID == null) {
            userUID = FirebaseAuth.getInstance().getUid();
        }

        if (userUID == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        TextView name = findViewById(R.id.welcome_name);
        TextView total_question = findViewById(R.id.total_questions);
        TextView total_points = findViewById(R.id.total_points);
        Button startQuiz = findViewById(R.id.start_quiz);
        Button createQuiz = findViewById(R.id.Create_Quiz);
        RelativeLayout solvedQuiz = findViewById(R.id.solvedquizzes);
        RelativeLayout your_quiz = findViewById(R.id.your_quizzes);
        RelativeLayout all_quizzes = findViewById(R.id.all_quizzes);
        EditText quiz_tittle = findViewById(R.id.Quiz_tittle);
        EditText start_quiz_id = findViewById(R.id.start_quiz_id);
        ImageView signOut = findViewById(R.id.signout_home);

        DocumentReference userDoc = db.collection("Users").document(userUID);
        userListener = userDoc.addSnapshotListener((snapshot, e) -> {
            if (isFinishing()) return;

            if (e != null) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Log.e("HomeActivity", "Firestore error", e);
                Toast.makeText(Home.this, "Connection Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                firstname = snapshot.getString("firstName");
                if (firstname == null) firstname = snapshot.getString("First Name");
                if (firstname == null) firstname = "User";

                Object pObj = snapshot.get("totalPoints");
                if (pObj == null) pObj = snapshot.get("Total Points");
                int pointsValue = 0;
                if (pObj instanceof Long) pointsValue = ((Long) pObj).intValue();
                else if (pObj instanceof String) {
                    try { pointsValue = Integer.parseInt((String) pObj); } catch (Exception ex) {}
                }
                total_points.setText(String.format("%03d", pointsValue));

                Object qObj = snapshot.get("totalQuestions");
                if (qObj == null) qObj = snapshot.get("Total Questions");
                int questionsValue = 0;
                if (qObj instanceof Long) questionsValue = ((Long) qObj).intValue();
                else if (qObj instanceof String) {
                    try { questionsValue = Integer.parseInt((String) qObj); } catch (Exception ex) {}
                }
                total_question.setText(String.format("%03d", questionsValue));

                name.setText("Welcome " + firstname + "!");
            }

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });

         signOut.setOnClickListener(view ->{
             FirebaseAuth.getInstance().signOut();

             Intent i = new Intent(Home.this , MainActivity.class);
             startActivity(i);
             finish();
         });

         createQuiz.setOnClickListener(v ->{
             if(quiz_tittle.getText().toString().isEmpty()){
                 quiz_tittle.setError("Quiz title cannot be Empty");
                 return;

             }
             Intent i = new Intent(Home.this , ExamEditor.class);
             i.putExtra("Quiz title",quiz_tittle.getText().toString());
             quiz_tittle.setText("");
             startActivity(i);
         });

        startQuiz.setOnClickListener(v->{
            String qId = start_quiz_id.getText().toString().trim();
            if(qId.isEmpty()){
                start_quiz_id.setError("Quiz ID cannot be empty");
                return;
            }

            // Check if already solved
            db.collection("Users").document(userUID).get().addOnSuccessListener(doc -> {
                java.util.List<String> solved = (java.util.List<String>) doc.get("quizzesSolved");
                if (solved != null && solved.contains(qId)) {
                    Toast.makeText(Home.this, "You have already solved this quiz!", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(Home.this, Result.class);
                    i.putExtra("Quiz ID", qId);
                    start_quiz_id.setText("");
                    startActivity(i);
                } else {
                    Intent i = new Intent(Home.this, Exam.class);
                    i.putExtra("Quiz ID", qId);
                    start_quiz_id.setText("");
                    startActivity(i);
                }
            });
        });
        solvedQuiz.setOnClickListener(v ->{
            Intent i = new Intent(Home.this , ListQuizzes.class);
            i.putExtra("Operation","List Solved Quizzes");
            startActivity(i);
        });

        your_quiz.setOnClickListener(v ->{
            Intent i = new Intent(Home.this , ListQuizzes.class);
            i.putExtra("Operation" , "List Created Quizzes");
            startActivity(i);
        });

        all_quizzes.setOnClickListener(v -> {
            Intent i = new Intent(Home.this, ListQuizzes.class);
            i.putExtra("Operation", "List All Quizzes");
            startActivity(i);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }
}