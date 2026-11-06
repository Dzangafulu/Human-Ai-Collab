package com.hacms.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorAssertion;
import com.google.firebase.auth.TotpMultiFactorGenerator;
import com.google.firebase.auth.TotpSecret;

/**
 * Shown after a verified user logs in without a second factor enrolled.
 * Sets up an authenticator app (TOTP) as the second sign-in factor.
 */
public class MfaEnrollActivity extends AppCompatActivity {

    private TextView tvSecretKey;
    private EditText etCode;
    private MaterialButton btnOpenApp, btnCopyKey, btnVerify;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private TotpSecret totpSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mfa_enroll);

        auth = FirebaseAuth.getInstance();

        tvSecretKey = findViewById(R.id.tvSecretKey);
        etCode = findViewById(R.id.etCode);
        btnOpenApp = findViewById(R.id.btnOpenApp);
        btnCopyKey = findViewById(R.id.btnCopyKey);
        btnVerify = findViewById(R.id.btnVerifyCode);
        progressBar = findViewById(R.id.progressBar);
        TextView tvCancel = findViewById(R.id.tvCancel);

        btnOpenApp.setOnClickListener(v -> openAuthenticatorApp());
        btnCopyKey.setOnClickListener(v -> copyKey());
        btnVerify.setOnClickListener(v -> verifyCode());
        tvCancel.setOnClickListener(v -> {
            auth.signOut();
            goToLogin();
        });

        startSetup();
    }

    // Step 1: ask Firebase for a fresh secret key for this user
    private void startSetup() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        setLoading(true);
        user.getMultiFactor().getSession().addOnCompleteListener(sessionTask -> {
            if (!sessionTask.isSuccessful()) {
                setLoading(false);
                handleError(sessionTask.getException());
                return;
            }

            TotpMultiFactorGenerator.generateSecret(sessionTask.getResult())
                    .addOnCompleteListener(secretTask -> {
                        setLoading(false);
                        if (secretTask.isSuccessful()) {
                            totpSecret = secretTask.getResult();
                            String key = totpSecret.getSharedSecretKey();
                            // Show in groups of 4 so it is easier to read and type
                            tvSecretKey.setText(key.replaceAll("(.{4})", "$1 ").trim());
                            btnOpenApp.setEnabled(true);
                            btnCopyKey.setEnabled(true);
                            btnVerify.setEnabled(true);
                        } else {
                            handleError(secretTask.getException());
                        }
                    });
        });
    }

    // Opens Google/Microsoft Authenticator with uniTask pre-filled
    private void openAuthenticatorApp() {
        FirebaseUser user = auth.getCurrentUser();
        if (totpSecret == null || user == null) return;
        try {
            String account = user.getEmail() != null ? user.getEmail() : "uniTask user";
            String qrUrl = totpSecret.generateQrCodeUrl(account, "uniTask");
            totpSecret.openInOtpApp(qrUrl);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Could not open an authenticator app. Install one, then add the key manually.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void copyKey() {
        if (totpSecret == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("uniTask key", totpSecret.getSharedSecretKey()));
            Toast.makeText(this, "Key copied.", Toast.LENGTH_SHORT).show();
        }
    }

    // Step 2: the user types the code their app shows; Firebase checks it and enrols the factor
    private void verifyCode() {
        String code = etCode.getText().toString().trim();
        if (code.length() != 6) {
            etCode.setError("Enter the 6-digit code from your authenticator app");
            etCode.requestFocus();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null || totpSecret == null) {
            goToLogin();
            return;
        }

        setLoading(true);
        MultiFactorAssertion assertion =
                TotpMultiFactorGenerator.getAssertionForEnrollment(totpSecret, code);
        user.getMultiFactor().enroll(assertion, "Authenticator app")
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Two-step verification is now on.",
                                Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this,
                                "That code didn't work. Check your phone's clock is set to "
                                        + "automatic, wait for a new code and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleError(Exception e) {
        String msg = e != null && e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (e instanceof FirebaseAuthRecentLoginRequiredException || msg.contains("recent")) {
            Toast.makeText(this, "Your session expired. Please log in again to continue.",
                    Toast.LENGTH_LONG).show();
            auth.signOut();
            goToLogin();
        } else {
            Toast.makeText(this,
                    "Could not start setup: " + (e != null ? e.getMessage() : "unknown error"),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            btnVerify.setEnabled(false);
        } else if (totpSecret != null) {
            btnVerify.setEnabled(true);
        }
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

