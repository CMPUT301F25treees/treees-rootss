package com.example.myapplication.features.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFrag extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private ProgressBar progress;

    public LoginFrag() {}

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_auth_login, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        tilEmail = v.findViewById(R.id.tilEmail);
        tilPassword = v.findViewById(R.id.tilPassword);
        etEmail = v.findViewById(R.id.etEmail);
        etPassword = v.findViewById(R.id.etPassword);
        progress = v.findViewById(R.id.progress);
        MaterialButton btnLogin = v.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(x -> signIn());
    }

    private void setLoading(boolean on){ progress.setVisibility(on?View.VISIBLE:View.GONE); }

    private void signIn() {
        String email = text(etEmail), pass = text(etPassword);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Enter a valid email"); return; }
        tilEmail.setError(null);
        if (TextUtils.isEmpty(pass)) { tilPassword.setError("Enter your password"); return; }
        tilPassword.setError(null);

        setLoading(true);
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(t -> {
            if (!t.isSuccessful()) {
                setLoading(false);
                Toast.makeText(requireContext(),
                        t.getException() != null ? t.getException().getMessage() : "Login failed",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).get().addOnCompleteListener(rt -> {
                setLoading(false);
                if (!rt.isSuccessful()) { toast("Failed to load role"); return; }
                DocumentSnapshot d = rt.getResult();
                String role = d != null ? String.valueOf(d.get("role")) : null;
                if ("admin".equalsIgnoreCase(role)) {
                    NavHostFragment.findNavController(this).navigate(R.id.navigation_admin_home);
                } else {
                    toast("This account is not an admin.");
                }
            });
        });
    }

    private static String text(TextInputEditText e){ return e.getText()==null ? "" : e.getText().toString().trim(); }
    private void toast(String m){ Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show(); }
}
