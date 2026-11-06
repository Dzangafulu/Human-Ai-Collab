package com.hacms.app;

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
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(name, email, password)) return;

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = auth.getCurrentUser();
                        if (fbUser != null) {
                            // 1) send the verification email
                            fbUser.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                // 2) save the profile in Firestore
                                Map<String, Object> userDoc = new HashMap<>();
                                userDoc.put("name", name);
                                userDoc.put("email", email);
                                userDoc.put("emailLowercase", email.toLowerCase(java.util.Locale.ROOT));
                                userDoc.put("role", "student");

                                db.collection("users").document(fbUser.getUid())
                                        .set(userDoc)
                                        .addOnCompleteListener(profileTask -> {
                                            setLoading(false);
                                            if (!profileTask.isSuccessful()) {
                                                Toast.makeText(this,
                                                        "Account created, but profile save failed.",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                            // 3) sign out and send them to Login until they verify
                                            auth.signOut();
                                            Toast.makeText(this,
                                                    "Account created! We sent a verification link to "
                                                            + email + ". Verify it, then log in.",
                                                    Toast.LENGTH_LONG).show();
                                            goToLogin();
                                        });
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

        // Any email provider is allowed, but the address must be properly formatted
        // (must contain @ and a domain such as .com)
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address (e.g. name@example.com)");
            valid = false;
        }

        String passwordError = getPasswordError(password);
        if (passwordError != null) {
            etPassword.setError(passwordError);
            valid = false;
        }

        return valid;
    }

    // Returns a message for the first rule that fails, or null if the password is strong
    private String getPasswordError(String password) {
        if (password.length() < 8)
            return "Password must be at least 8 characters";
        if (!password.matches(".*[A-Z].*"))
            return "Add at least one uppercase letter";
        if (!password.matches(".*[a-z].*"))
            return "Add at least one lowercase letter";
        if (!password.matches(".*\\d.*"))
            return "Add at least one number";
        if (!password.matches(".*[^A-Za-z0-9].*"))
            return "Add at least one special character (e.g. ! @ # $ %)";
        return null;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!loading);
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
