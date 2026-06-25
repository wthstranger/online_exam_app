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

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button login = findViewById(R.id.login_button);
        EditText email = findViewById(R.id.email);
        EditText pass = findViewById(R.id.pass_word);
        TextView create = findViewById(R.id.create_new);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if(user!=null){
            Intent i = new Intent(MainActivity.this , Home.class);
            i.putExtra("User UID",user.getUid());
            startActivity(i);
            finish();
        }

        login.setOnClickListener(click -> {
            String name = email.getText().toString().trim();
            String pas = pass.getText().toString().trim();

            if (name.isEmpty() || pas.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Logging in...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            auth.signInWithEmailAndPassword(name, pas).addOnCompleteListener(this, task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    FirebaseUser user1 = auth.getCurrentUser();
                    Intent i = new Intent(MainActivity.this, Home.class);
                    if (user1 != null) {
                        i.putExtra("User UID", user1.getUid());
                    }
                    startActivity(i);
                    finish();
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Authentication Failed";
                    if (error != null && error.contains("CONFIGURATION_NOT_FOUND")) {
                        error = "Firebase Configuration Error: Please check your Firebase Console settings (reCAPTCHA/Identity Platform).";
                    }
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });
        });
    create.setOnClickListener(click ->{
        Intent i = new Intent(MainActivity.this ,Signup.class);
        startActivity(i);
        finish();
    });

    }
}