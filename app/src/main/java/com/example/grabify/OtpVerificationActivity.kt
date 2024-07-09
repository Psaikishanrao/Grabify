package com.example.grabify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grabify.ui.theme.GrabifyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class OtpVerificationActivity : ComponentActivity() {
    private lateinit var storedVerificationId: String
    private lateinit var phoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storedVerificationId = intent.getStringExtra("verificationId").orEmpty()
        phoneNumber = intent.getStringExtra("phoneNumber").orEmpty()
        setContent {
            GrabifyTheme {
                OtpVerificationScreen(
                    onVerify = { otp ->
                        verifyOtp(otp)
                    }
                )
            }
        }
    }

    private fun verifyOtp(otp: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    navigateToWelcomePage(user)
                } else {
                    Toast.makeText(this, "Verification failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToWelcomePage(user: FirebaseUser?) {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun OtpVerificationScreen(onVerify: (String) -> Unit) {
    var otp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun validateOtp() {
        if (otp.isEmpty()) {
            otpError = true
            Toast.makeText(context, "Please enter the OTP", Toast.LENGTH_SHORT).show()
        } else {
            otpError = false
            onVerify(otp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.text.BasicText(
            text = "OTP Verification",
            style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        androidx.compose.foundation.text.BasicText(
            text = "Please enter the OTP sent to your phone number",
            style = TextStyle(fontSize = 18.sp),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            BasicTextField(
                value = otp,
                onValueChange = { otp = it; otpError = false },
                textStyle = TextStyle(fontSize = 18.sp),
                singleLine = true,
                keyboardOptions =KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .background(Color.LightGray, RoundedCornerShape(4.dp))
                            .padding(16.dp)
                    ) {
                        if (otp.isEmpty()) {
                            androidx.compose.foundation.text.BasicText(
                                text = "OTP",
                                style = TextStyle(color = Color.Gray)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color(0xFFDEB887), RoundedCornerShape(4.dp))
                .clickable { validateOtp() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.text.BasicText("Verify OTP")
        }

        if (otpError) {
            Text(
                text = "Please enter a valid OTP",
                color = Color.Red,
                style = TextStyle(fontSize = 14.sp),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GrabifyTheme {
        OtpVerificationScreen(onVerify = {})
    }
}
