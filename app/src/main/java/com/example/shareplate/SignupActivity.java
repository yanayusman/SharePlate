package com.example.shareplate;

import androidx.credentials.exceptions.GetCredentialException;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialUnknownException;
import androidx.credentials.exceptions.GetCredentialInterruptedException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GoogleAuthProvider;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CredentialManagerCallback;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Map;
import java.util.HashMap;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.auth.FacebookAuthProvider;

import java.util.Arrays;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button signupButton;
    private TextView cancelTextView;
    private FirebaseAuth mAuth;
    private ImageButton googleButton;
    private CredentialManager credentialManager;
    private ActivityResultLauncher<Intent> signInLauncher;
    private Executor executor;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private ImageButton facebookButton;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize CredentialManager
        credentialManager = CredentialManager.create(this);

        // Initialize ActivityResultLauncher with proper callback
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleSignInResult(task);
                    } else {
                        Log.e(TAG, "Google sign in failed");
                        Toast.makeText(SignupActivity.this, "Failed to sign in with Google",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signupButton = findViewById(R.id.loginButton);
        cancelTextView = findViewById(R.id.cancelTextView);
        googleButton = findViewById(R.id.googleButton);
        facebookButton = findViewById(R.id.facebookButton);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(SignupActivity.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show loading state
                signupButton.setEnabled(false);

                // Create user with email and password
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "createUserWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        // Create user document with default username
                                        String defaultUsername = email.substring(0, email.indexOf('@'));
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("email", email);
                                        userData.put("username", defaultUsername);
                                        userData.put("location", "");
                                        userData.put("phoneNumber", "");

                                        FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .document(email)
                                                .set(userData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "User document created successfully");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Error creating user document", e);
                                                });

                                        // Send verification email
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        signupButton.setEnabled(true);
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(SignupActivity.this,
                                                                    "Account created! Please check your email to verify your account.",
                                                                    Toast.LENGTH_LONG).show();
                                                            // Sign out until email is verified
                                                            mAuth.signOut();
                                                            // Go to login screen
                                                            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                                            startActivity(intent);
                                                            finish();
                                                        } else {
                                                            Log.e(TAG, "sendEmailVerification", task.getException());
                                                            Toast.makeText(SignupActivity.this,
                                                                    "Failed to send verification email.",
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    }
                                } else {
                                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                    signupButton.setEnabled(true);
                                    String errorMessage;
                                    if (task.getException() instanceof FirebaseAuthException) {
                                        FirebaseAuthException e = (FirebaseAuthException) task.getException();
                                        errorMessage = getErrorMessage(e.getErrorCode());
                                    } else {
                                        errorMessage = task.getException() != null ?
                                                task.getException().getMessage() :
                                                "Sign up failed";
                                    }
                                    Toast.makeText(SignupActivity.this, errorMessage,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        // Update Google Sign-In configuration
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(null)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        googleButton.setOnClickListener(v -> signInWithGoogle(request));

        cancelTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
        });

        executor = Executors.newSingleThreadExecutor();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Facebook Login
        mCallbackManager = CallbackManager.Factory.create();

        // Initialize Facebook Login button
        facebookButton.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(SignupActivity.this,
                    Arrays.asList("email", "public_profile"));
        });

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "facebook:onSuccess:" + loginResult);
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "facebook:onCancel");
                        Toast.makeText(SignupActivity.this, "Facebook login cancelled",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, "facebook:onError", error);
                        Toast.makeText(SignupActivity.this, "Facebook login failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle(GetCredentialRequest request) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting Google Sign-In process");
                credentialManager.getCredentialAsync(
                        SignupActivity.this,
                        request,
                        null,
                        executor,
                        new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                            @Override
                            public void onResult(@NonNull GetCredentialResponse result) {
                                Log.d(TAG, "Credential retrieved successfully");
                                if (result.getCredential() instanceof GoogleIdTokenCredential) {
                                    GoogleIdTokenCredential credential = (GoogleIdTokenCredential) result.getCredential();
                                    String idToken = credential.getIdToken();
                                    if (idToken != null) {
                                        Log.d(TAG, "ID token retrieved successfully");
                                        runOnUiThread(() -> firebaseAuthWithGoogle(idToken));
                                    } else {
                                        Log.e(TAG, "ID token is null");
                                        runOnUiThread(() -> fallbackToTraditionalGoogleSignIn());
                                    }
                                } else {
                                    Log.e(TAG, "Unexpected credential type: " + result.getCredential().getClass().getName());
                                    runOnUiThread(() -> fallbackToTraditionalGoogleSignIn());
                                }
                            }

                            @Override
                            public void onError(GetCredentialException e) {
                                Log.e(TAG, "Google Sign-In error", e);
                                // Fall back to traditional Google Sign-In when Credential Manager fails
                                runOnUiThread(() -> fallbackToTraditionalGoogleSignIn());
                            }
                        }
                );
            } catch (Exception e) {
                Log.e(TAG, "Fatal error starting credential manager", e);
                runOnUiThread(() -> fallbackToTraditionalGoogleSignIn());
            }
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken);
            } else {
                Log.e(TAG, "ID Token is null in handleSignInResult");
                Toast.makeText(this, "Failed to get Google ID token", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode(), e);
            Toast.makeText(this, "Google Sign-In failed: " + e.getStatusMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + idToken);
        googleButton.setEnabled(false); // Disable button during authentication

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    googleButton.setEnabled(true); // Re-enable button
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Update the user's profile with Google display name if not set
                            if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
                                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
                                if (acct != null && acct.getDisplayName() != null) {
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(acct.getDisplayName())
                                            .build();
                                    
                                    user.updateProfile(profileUpdates)
                                            .addOnCompleteListener(profileTask -> {
                                                createUserDocument(user);
                                                navigateToHome();
                                            });
                                    return;
                                }
                            }
                            createUserDocument(user);
                            navigateToHome();
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(SignupActivity.this, errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserDocument(FirebaseUser user) {
        if (user == null || user.getEmail() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String email = user.getEmail();
        
        // Get username from Google account if available, otherwise use email prefix
        String defaultUsername;
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            defaultUsername = user.getDisplayName();
        } else {
            defaultUsername = email.substring(0, email.indexOf('@'));
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("username", defaultUsername);
        userData.put("location", "");
        userData.put("phoneNumber", "");

        db.collection("users")
                .document(email)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created successfully with username: " + defaultUsername);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user document", e);
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(SignupActivity.this, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                return "The email address is badly formatted.";
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "The email address is already in use by another account.";
            case "ERROR_WEAK_PASSWORD":
                return "The password is too weak. Please use at least 6 characters.";
            default:
                return "Sign up failed: " + errorCode;
        }
    }

    private void fallbackToTraditionalGoogleSignIn() {
        Log.d(TAG, "Falling back to traditional Google Sign-In");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Create user profile in Firestore
                        createUserDocument(user);
                        navigateToHome();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(SignupActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        
        // Handle Google Sign In Result (existing code)
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }
}