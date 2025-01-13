package com.example.shareplate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.auth.FacebookAuthProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import org.json.JSONException;
import android.os.Bundle;
import com.facebook.GraphRequest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.Manifest;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView cancelTextView;
    private FirebaseAuth mAuth;
    private ImageButton googleButton;
    private GoogleSignInClient mGoogleSignInClient;
    private ExecutorService executorService;
    private CredentialManager credentialManager;
    private Executor executor;
    private ImageButton facebookButton;
    private CallbackManager mCallbackManager;

    // ActivityResultLauncher for Google Sign-In
    ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleSignInResult(task);
                    } else {
                        // Handle sign-in failure
                        Log.e(TAG, "Google sign in failed");
                        showToast("Failed to sign in with Google");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        executorService = Executors.newFixedThreadPool(2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        cancelTextView = findViewById(R.id.cancelTextView);
        googleButton = findViewById(R.id.googleButton);
        facebookButton = findViewById(R.id.facebookButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show loading state
                loginButton.setEnabled(false);

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                loginButton.setEnabled(true);
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        if (user.isEmailVerified()) {
                                            // Email is verified, proceed to home page
                                            Log.d(TAG, "signInWithEmail:success");
                                            navigateToHome();
                                        } else {
                                            // Email is not verified
                                            Log.d(TAG, "signInWithEmail:email not verified");
                                            Toast.makeText(LoginActivity.this,
                                                    "Please verify your email address first",
                                                    Toast.LENGTH_LONG).show();
                                            // Send verification email again
                                            user.sendEmailVerification()
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                Toast.makeText(LoginActivity.this,
                                                                        "Verification email sent",
                                                                        Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                            mAuth.signOut();
                                        }
                                    }
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Initialize CredentialManager
        credentialManager = CredentialManager.create(this);
        executor = Executors.newSingleThreadExecutor();

        // Initialize GoogleSignInClient for fallback
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Configure Google Sign-In with GetGoogleIdOption
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

        cancelTextView.setOnClickListener(v -> finish());

        // Initialize Facebook Login
        mCallbackManager = CallbackManager.Factory.create();

        // Initialize Facebook Login button
        facebookButton.setOnClickListener(v -> {
            // Request email and public_profile permissions
            LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this,
                    Arrays.asList("email", "public_profile"));
        });

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "facebook:onSuccess:" + loginResult);
                        
                        // Get Facebook user data
                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "id,name,email");
                        
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                (object, response) -> {
                                    try {
                                        String email = null;
                                        String name = null;
                                        
                                        // Safely get email and name
                                        if (object.has("email")) {
                                            email = object.getString("email");
                                        } else {
                                            Log.w(TAG, "Email not provided by Facebook");
                                        }
                                        
                                        if (object.has("name")) {
                                            name = object.getString("name");
                                        } else {
                                            Log.w(TAG, "Name not provided by Facebook");
                                        }
                                        
                                        // If email is not available, use Facebook ID + @facebook.com
                                        if (email == null && object.has("id")) {
                                            email = object.getString("id") + "@facebook.com";
                                            Log.d(TAG, "Using Facebook ID as email: " + email);
                                        }
                                        
                                        // If name is not available, use "Facebook User"
                                        if (name == null) {
                                            name = "Facebook User";
                                        }
                                        
                                        // Now handle the Facebook authentication with the user data
                                        handleFacebookAccessToken(loginResult.getAccessToken(), email, name);
                                        
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error parsing Facebook user data", e);
                                        // Handle the authentication with default values
                                        handleFacebookAccessToken(loginResult.getAccessToken(), 
                                                loginResult.getAccessToken().getUserId() + "@facebook.com",
                                                "Facebook User");
                                    }
                                });
                        
                        request.setParameters(parameters);
                        request.executeAsync();
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "facebook:onCancel");
                        showToast("Facebook login cancelled");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, "facebook:onError", error);
                        showToast("Facebook login failed: " + error.getMessage());
                    }
                });
    }

    private void signInWithGoogle(GetCredentialRequest request) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting Google Sign-In process");
                credentialManager.getCredentialAsync(
                        LoginActivity.this,
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

    private void fallbackToTraditionalGoogleSignIn() {
        Log.d(TAG, "Falling back to traditional Google Sign-In");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            showToast("Failed to sign in with Google");
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        navigateToHome();
                    } else {
                        // Sign in failed
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHome() {
        // Check for location permission before proceeding to home
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            
            // Show rationale before requesting permission
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Location Permission Required")
                    .setMessage("SharePlate needs location access to show you nearby food donations and help you connect with your local community.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this,
                                new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                },
                                LOCATION_PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Not Now", (dialog, which) -> {
                        // Proceed without location permission
                        startHomeActivity();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            startHomeActivity();
        }
    }

    private void startHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
        executorService.shutdown();
    }

    private void handleFacebookAccessToken(AccessToken token, String facebookEmail, String facebookName) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Determine the document ID (email to use as the user's identifier)
                            String userEmail = facebookEmail;
                            if (userEmail == null || userEmail.isEmpty()) {
                                userEmail = user.getEmail();
                                if (userEmail == null || userEmail.isEmpty()) {
                                    // If still no email, use Facebook user ID
                                    userEmail = token.getUserId() + "@facebook.com";
                                }
                            }
                            
                            // Update user document with Facebook data
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", userEmail);
                            userData.put("username", facebookName != null ? facebookName : user.getDisplayName());
                            userData.put("authProvider", "facebook");
                            userData.put("facebookId", token.getUserId());

                            // Store the final email for use in the onSuccess lambda
                            final String documentId = userEmail;
                            
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(documentId)
                                    .set(userData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User document updated successfully");
                                        navigateToHome();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating user document", e);
                                        navigateToHome();
                                    });
                        } else {
                            Log.w(TAG, "signInWithCredential:success but user is null");
                            Toast.makeText(LoginActivity.this, "Authentication succeeded but user data is missing.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If sign in fails, check if it's due to existing account
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(LoginActivity.this,
                                    "An account already exists with this email. Please sign in using your existing method (Google or Email/Password).",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Handle other errors
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
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
        executorService.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Whether permission was granted or not, proceed to home
            // The app will handle location availability in specific features
            startHomeActivity();
        }
    }
}