package com.example.grabify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.grabify.ui.theme.GrabifyTheme
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContent {
            GrabifyTheme {
                SignupScreen(
                    onSubmit = { firstName, lastName, phoneNumber, email, address, password ->
                        val formattedPhoneNumber = "+91$phoneNumber"
                        startPhoneNumberVerification(formattedPhoneNumber, firstName, lastName, email, address, password)
                    }
                )
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String, firstName: String, lastName: String, email: String, address: String, password: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@SignupActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                    resendToken = token
                    navigateToOtpVerification(phoneNumber, verificationId, firstName, lastName, email, address, password)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    navigateToWelcomePage(user)
                } else {
                    Toast.makeText(this, "Verification failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToOtpVerification(phoneNumber: String, verificationId: String, firstName: String, lastName: String, email: String, address: String, password: String) {
        val intent = Intent(this, OtpVerificationActivity::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        intent.putExtra("verificationId", verificationId)
        intent.putExtra("firstName", firstName)
        intent.putExtra("lastName", lastName)
        intent.putExtra("email", email)
        intent.putExtra("address", address)
        intent.putExtra("password", password)
        startActivity(intent)
    }

    private fun navigateToWelcomePage(user: FirebaseUser?) {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}



@Composable
fun SignupScreen(onSubmit: (String, String, String, String, String, String) -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var firstNameError by remember { mutableStateOf(false) }
    var lastNameError by remember { mutableStateOf(false) }
    var phoneNumberError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it; firstNameError = false },
                label = { Text("First Name") },
                isError = firstNameError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it; lastNameError = false },
                label = { Text("Last Name") },
                isError = lastNameError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it; phoneNumberError = false },
                label = { Text("Phone Number") },
                isError = phoneNumberError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = false },
                label = { Text("Email") },
                isError = emailError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it; addressError = false },
                label = { Text("Address") },
                isError = addressError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = false },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            Button(
                onClick = {
                    if (firstName.isEmpty()) firstNameError = true
                    if (lastName.isEmpty()) lastNameError = true
                    if (phoneNumber.isEmpty()) phoneNumberError = true
                    if (email.isEmpty()) emailError = true
                    if (address.isEmpty()) addressError = true
                    if (password.isEmpty()) passwordError = true

                    if (firstName.isNotEmpty() && lastName.isNotEmpty() && phoneNumber.isNotEmpty() &&
                        email.isNotEmpty() && address.isNotEmpty() && password.isNotEmpty()
                    ) {
                        isLoading = true
                        onSubmit(firstName, lastName, phoneNumber, email, address, password)
                    } else {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Submit")
            }

            Text(
                text = "Already have an account?",
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color.Gray
            )

            Text(
                text = "Sign-in",
                modifier = Modifier
                    .clickable {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                    .fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color.Blue
            )
        }
    }
}
