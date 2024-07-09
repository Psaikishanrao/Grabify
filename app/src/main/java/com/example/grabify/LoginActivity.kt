package com.example.grabify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.grabify.ui.theme.GrabifyTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import at.favre.lib.crypto.bcrypt.BCrypt

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrabifyTheme {
                LoginScreen(
                    onLogin = { phoneNumber, password ->
                        loginUser(phoneNumber, password)
                    }
                )
            }
        }
    }

    private fun loginUser(phoneNumber: String, password: String) {
        val formattedPhoneNumber = "+91$phoneNumber"
        val database = FirebaseDatabase.getInstance().reference
        val userRef = database.child("users").child(formattedPhoneNumber)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val storedEncryptedPassword = snapshot.child("password").value as String
                    val result = BCrypt.verifyer().verify(password.toCharArray(), storedEncryptedPassword)
                    if (result.verified) {
                        val intent = Intent(this@LoginActivity, WelcomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "User not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var phoneNumberError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.back),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 16.dp)
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
        if (phoneNumberError) {
            Text("Please enter your phone number", color = MaterialTheme.colorScheme.error)
        }

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
        if (passwordError) {
            Text("Please enter your password", color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (phoneNumber.isEmpty()) phoneNumberError = true
                if (password.isEmpty()) passwordError = true
                if (phoneNumber.isNotEmpty() && password.isNotEmpty()) {
                    onLogin(phoneNumber, password)
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Login")
        }
        Text(
            text = "New to Grabify?",
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            color = androidx.compose.ui.graphics.Color.Gray
        )
        Text(
            text = "Sign-up",
            modifier = Modifier
                .clickable {
                    context.startActivity(Intent(context, SignupActivity::class.java))
                }
                .fillMaxWidth(),
            color = androidx.compose.ui.graphics.Color.Blue
        )
    }
}
