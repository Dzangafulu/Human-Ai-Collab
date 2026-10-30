package com.hacms.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.TotpMultiFactorGenerator;
import com.google.firebase.auth.TotpMultiFactorInfo;
import com.google.firebase.auth.MultiFactorAssertion;
import com.google.firebase.auth.MultiFactorInfo;
import com.google.firebase.auth.MultiFactorResolver;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneMultiFactorGenerator;
import com.google.firebase.auth.PhoneMultiFactorInfo;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvGoToSignup = findViewById(R.id.tvGoToSignup);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
        tvGoToSignup.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            if (user.isEmailVerified()
                    && !user.getMultiFactor().getEnrolledFactors().isEmpty()) {
                goToMain();
            } else {
                // not verified, or no phone enrolled yet: require a fresh login
                auth.signOut();
            }
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) return;

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            routeAfterLogin(user);
                        } else {
                            showUnverifiedDialog(user);
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthMultiFactorException) {
                            // Password was right, but this account has SMS two-step verification
                            startMfaChallenge(((FirebaseAuthMultiFactorException) e).getResolver());
                        } else {
                            String error = e != null
                                    ? e.getMessage()
                                    : "Login failed. Please try again.";
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Verified users with no phone enrolled go to the enrolment screen; everyone else goes in
    private void routeAfterLogin(FirebaseUser user) {
        if (user.getMultiFactor().getEnrolledFactors().isEmpty()) {
            startActivity(new Intent(this, MfaEnrollActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
            goToMain();
        }
    }

    // ---------- SMS second step at login ----------

    private void startMfaChallenge(MultiFactorResolver resolver) {
        MultiFactorInfo hint = resolver.getHints().get(0);

        if (hint instanceof TotpMultiFactorInfo) {
            showTotpDialog(resolver, hint.getUid());
            return;
        }
        if (!(hint instanceof PhoneMultiFactorInfo)) {
            Toast.makeText(this, "Unsupported verification method.", Toast.LENGTH_LONG).show();
            return;
        }
        // ... existing phone code continues unchanged

        setLoading(true);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setActivity(this)
                .setMultiFactorHint((PhoneMultiFactorInfo) hint)
                .setMultiFactorSession(resolver.getSession())
                .setTimeout(60L, TimeUnit.SECONDS)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        setLoading(false);
                        finishMfaSignIn(resolver, credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Could not send SMS: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId,
                                           PhoneAuthProvider.ForceResendingToken token) {
                        setLoading(false);
                        showCodeDialog(resolver, verificationId);
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showTotpDialog(MultiFactorResolver resolver, String factorUid) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("6-digit code");

        FrameLayout container = new FrameLayout(this);
        container.setPadding(48, 16, 48, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Enter your authenticator code")
                .setMessage("Open your authenticator app and enter the current 6-digit code.")
                .setView(container)
                .setCancelable(false)
                .setPositiveButton("Verify", (d, w) -> {
                    String code = input.getText().toString().trim();
                    MultiFactorAssertion assertion =
                            TotpMultiFactorGenerator.getAssertionForSignIn(factorUid, code);
                    setLoading(true);
                    resolver.resolveSignIn(assertion).addOnCompleteListener(task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            goToMain();
                        } else {
                            Toast.makeText(this,
                                    "Incorrect or expired code. Please try logging in again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCodeDialog(MultiFactorResolver resolver, String verificationId) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("6-digit code");

        FrameLayout container = new FrameLayout(this);
        container.setPadding(48, 16, 48, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Enter the SMS code")
                .setMessage("We sent a verification code to your phone.")
                .setView(container)
                .setCancelable(false)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    PhoneAuthCredential credential =
                            PhoneAuthProvider.getCredential(verificationId, code);
                    finishMfaSignIn(resolver, credential);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void finishMfaSignIn(MultiFactorResolver resolver, PhoneAuthCredential credential) {
        MultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);
        setLoading(true);
        resolver.resolveSignIn(assertion).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                goToMain();
            } else {
                Toast.makeText(this,
                        "Incorrect or expired code. Please try logging in again.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------- Email verification dialog ----------

    private void showUnverifiedDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Verify your email")
                .setMessage("Please verify your email address before logging in. "
                        + "Check your inbox (and spam folder) for the link we sent you.")
                .setCancelable(false)
                .setPositiveButton("Resend email", (dialog, which) -> {
                    if (user != null) {
                        user.sendEmailVerification().addOnCompleteListener(t -> {
                            Toast.makeText(this,
                                    t.isSuccessful()
                                            ? "Verification email sent."
                                            : "Could not send email. Try again later.",
                                    Toast.LENGTH_LONG).show();
                            auth.signOut();
                        });
                    } else {
                        auth.signOut();
                    }
                })
                .setNegativeButton("OK", (dialog, which) -> auth.signOut())
                .show();
    }

    // ---------- Forgot password ----------

    private void sendPasswordReset() {
        String email = etEmail.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this,
                                "No account found with that email. Please sign up first.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(v -> Toast.makeText(this,
                                        "Reset link sent. Check your inbox (and spam).",
                                        Toast.LENGTH_LONG).show())
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Could not send reset email: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Something went wrong: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private boolean validateInput(String email, String password) {
        boolean valid = true;
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
        btnLogin.setEnabled(!loading);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
