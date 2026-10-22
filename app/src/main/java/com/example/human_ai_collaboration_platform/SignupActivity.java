package com.example.human_ai_collaboration_platform;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private MaterialButton btnSignup;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        progressBar = findViewById(R.id.progressBar);

        btnSignup.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(name, email, password)) return;

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = auth.getCurrentUser();
                        if (fbUser != null) {
                            Map<String, Object> userDoc = new HashMap<>();
                            userDoc.put("name", name);
                            userDoc.put("email", email);
                            userDoc.put("role", "student");

                            db.collection("users").document(fbUser.getUid())
                                    .set(userDoc)
                                    .addOnCompleteListener(profileTask -> {
                                        setLoading(false);
                                        if (profileTask.isSuccessful()) {
                                            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                                            goToMain();
                                        } else {
                                            Toast.makeText(this, "Account created, but profile save failed.", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        setLoading(false);
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Signup failed. Please try again.";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInput(String name, String email, String password) {
        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            etName.setError("This field is required");
            valid = false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            valid = false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            valid = false;
        }
        return valid;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!loading);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}