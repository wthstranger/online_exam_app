package com.error.onlineexam;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Signup extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        EditText firstName = findViewById(R.id.first_name);
        EditText lastNAme = findViewById(R.id.last_name);
        EditText email = findViewById(R.id.sign_email);
        EditText passOne = findViewById(R.id.signup_pass_word);
        TextView already = findViewById(R.id.already_account_login);
        EditText rePass = findViewById(R.id.re_pass_word);
        Button signUp = findViewById(R.id.signup);

        signUp.setOnClickListener(view -> {
            String pas = passOne.getText().toString().trim();
            String rePas = rePass.getText().toString().trim();
            String em = email.getText().toString().trim();
            String fName = firstName.getText().toString().trim();
            String lName = lastNAme.getText().toString().trim();

            if (em.isEmpty() || pas.isEmpty() || fName.isEmpty()) {
                Toast.makeText(Signup.this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!pas.equals(rePas)){
                rePass.setError("Password Doesn't match");
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(Signup.this);
            progressDialog.setMessage("Creating account...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            auth.createUserWithEmailAndPassword(em, pas).addOnCompleteListener(this, task -> {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("firstName", fName);
                        userData.put("lastName", lName);
                        userData.put("totalPoints", 0);
                        userData.put("totalQuestions", 0);
                        userData.put("quizzesCreated", new java.util.ArrayList<String>());
                        userData.put("quizzesSolved", new java.util.ArrayList<String>());

                        db.collection("Users").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Intent i = new Intent(Signup.this, MainActivity.class);
                                    i.putExtra("User UID", user.getUid());
                                    startActivity(i);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(Signup.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Registration Failed";
                    if (error != null && error.contains("CONFIGURATION_NOT_FOUND")) {
                        error = "Firebase Configuration Error: Please check if reCAPTCHA is configured or disabled in Firebase Console, and ensure your SHA-1 is added.";
                    }
                    Toast.makeText(Signup.this, error, Toast.LENGTH_LONG).show();
                }
            });
        });
        already.setOnClickListener(click -> {
            Intent i = new Intent(Signup.this , MainActivity.class);
            startActivity(i);
            finish();
        });
    }
}