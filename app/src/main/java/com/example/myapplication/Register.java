package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * This activity is responsible for registering new users.
 *
 * Collects a users personal information: First Name, Last Name, Email, Phone, and Password
 *
 * Current Functionality: Validates the users inputs and displays a Toast.
 */

public class Register  extends AppCompatActivity {

    private TextInputEditText inputFirstName, inputLastName, inputEmail, inputPhone, inputPassword;


    /**
     * This method gets called when the activity is created. Initializes the input fields and
     * register button listener
     *
     *
     * @param savedInstanceState if there is any previous state of this activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        inputFirstName = findViewById(R.id.first_name_input);
        inputLastName = findViewById(R.id.last_name_input);
        inputEmail = findViewById(R.id.email_input);
        inputPhone = findViewById(R.id.phone_input);
        inputPassword = findViewById(R.id.password_input);
        MaterialButton registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> handleRegister());
    }

    /**
     * This method is called when the user clicks the register button.
     * Will get the user information and validate it; the collected info is displayed
     * as a Toast message.
     *
     * TODO: Hash password
     * TODO: Sending user information to Firebase
     *
     */
    private void handleRegister(){
        String firstName = inputFirstName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        
        if (firstName.isEmpty()){
            Toast.makeText(this, "First Name is Required", Toast.LENGTH_SHORT).show();
        } else if (lastName.isEmpty()) {
            Toast.makeText(this, "Last Name is Required", Toast.LENGTH_SHORT).show();
        } else if (email.isEmpty()) {
            Toast.makeText(this, "Email is Required", Toast.LENGTH_SHORT).show();
        } else if (password.isEmpty()) {
            Toast.makeText(this, "Password is Required", Toast.LENGTH_SHORT).show();
        } else {
            String checkMessage = "Name: " + firstName + " " + lastName + "\nEmail: " + email + "\nPhone: " + phone + "\nPassword: " + password;

            Toast.makeText(this, checkMessage, Toast.LENGTH_SHORT).show();
        }

        //NEED TO IMPLEMENT:
        //Hashing of password when passing into Firebase.
    }
}
