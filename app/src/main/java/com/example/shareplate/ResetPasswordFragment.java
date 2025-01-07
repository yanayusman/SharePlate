package com.example.shareplate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class ResetPasswordFragment extends Fragment {

    private EditText oldPass, newPass, confPass;
    private ImageView backButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reset_password, container, false);
        setupViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        backButton = view.findViewById(R.id.backBtn);

        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }
    }

    private void setupViews(View view) {
        oldPass = view.findViewById(R.id.reset_old_ET);
        newPass = view.findViewById(R.id.reset_new_ET);
        confPass = view.findViewById(R.id.reset_reNew_ET);
        Button reset = view.findViewById(R.id.profile_resetPass_Btn);
        reset.setOnClickListener(v -> handleResetButtonClick());
    }

    private void handleResetButtonClick() {
        String oldUserPass = oldPass.getText().toString();
        String newUserPass = newPass.getText().toString();
        String confUserPass = confPass.getText().toString();

        if (!newUserPass.equals(confUserPass)) {
            Toast.makeText(requireActivity(), "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (oldUserPass.isEmpty() || newUserPass.isEmpty() || confUserPass.isEmpty()) {
            Toast.makeText(requireActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        resetPassword(oldUserPass, newUserPass);
    }

    private void resetPassword(String oldPassword, final String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireActivity(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), oldPassword);
        reAuthenticateUser(user, credential, newPassword);
    }

    private void reAuthenticateUser(FirebaseUser user, AuthCredential credential, String newPassword) {
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                updateUserPassword(newPassword);
            } else {
                Toast.makeText(requireActivity(), "Re-authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserPassword(String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
            if (updateTask.isSuccessful()) {
                Toast.makeText(requireActivity(), "Password updated successfully", Toast.LENGTH_SHORT).show();

                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(requireActivity(), "Failed to update password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
