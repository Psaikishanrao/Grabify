package com.example.grabify

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.grabify.ui.theme.GrabifyTheme
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit



class OtpVerificationActivity : ComponentActivity() {
    private lateinit var storedVerificationId: String
    private lateinit var phoneNumber: String
    private lateinit var firstName: String
    private lateinit var lastName: String
    private lateinit var email: String
    private lateinit var address: String
    private lateinit var password: String
    private lateinit var resendingToken: PhoneAuthProvider.ForceResendingToken
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storedVerificationId = intent.getStringExtra("verificationId").orEmpty()
        phoneNumber = intent.getStringExtra("phoneNumber").orEmpty()
        firstName = intent.getStringExtra("firstName").orEmpty()
        lastName = intent.getStringExtra("lastName").orEmpty()
        email = intent.getStringExtra("email").orEmpty()
        address = intent.getStringExtra("address").orEmpty()
        password = intent.getStringExtra("password").orEmpty()

        setContent {
            var isLoading by remember { mutableStateOf(false) }
            var canResendOtp by remember { mutableStateOf(false) }
            val resendOtpTimer = remember { mutableStateOf(60000L) }

            LaunchedEffect(Unit) {
                object : CountDownTimer(60000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        resendOtpTimer.value = millisUntilFinished
                    }

                    override fun onFinish() {
                        canResendOtp = true
                    }
                }.start()
            }

            GrabifyTheme {
                OtpVerificationScreen(
                    onVerify = { otp ->
                        isLoading = true
                        verifyOtp(otp) { success ->
                            isLoading = false
                            if (success) {
                                saveUserToDatabase()
                                navigateToWelcomePage()
                            } else {
                                Toast.makeText(this, "Verification failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    isLoading = isLoading,
                    canResendOtp = canResendOtp,
                    onResendOtp = {
                        canResendOtp = false
                        resendVerificationCode(phoneNumber, resendingToken)
                        object : CountDownTimer(60000, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                resendOtpTimer.value = millisUntilFinished
                            }

                            override fun onFinish() {
                                canResendOtp = true
                            }
                        }.start()
                    },
                    resendOtpTimer = resendOtpTimer
                )
            }
        }
    }

    private fun verifyOtp(otp: String, callback: (Boolean) -> Unit) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, otp)
        signInWithPhoneAuthCredential(credential, callback)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, callback: (Boolean) -> Unit) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }

    private fun saveUserToDatabase() {
        val encryptedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "email" to email,
            "address" to address,
            "password" to encryptedPassword
        )
        FirebaseDatabase.getInstance().reference.child("users").child(phoneNumber).setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "User saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save user", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToWelcomePage() {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken
    ) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential) { success ->
                if (success) {
                    saveUserToDatabase()
                    navigateToWelcomePage()
                } else {
                    Toast.makeText(
                        this@OtpVerificationActivity,
                        "Verification failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(
                this@OtpVerificationActivity,
                "Verification failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            storedVerificationId = verificationId
            resendingToken = token


        }
    }


    @Composable
    fun OtpVerificationScreen(
        onVerify: (String) -> Unit,
        isLoading: Boolean,
        canResendOtp: Boolean,
        onResendOtp: () -> Unit,
        resendOtpTimer: State<Long>
    ) {
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
            BasicText(
                text = "OTP Verification",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BasicText(
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
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .background(Color.LightGray, RoundedCornerShape(4.dp))
                                .padding(16.dp)
                        ) {
                            if (otp.isEmpty()) {
                                BasicText(
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

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color(0xFFDEB887), RoundedCornerShape(4.dp))
                        .clickable { validateOtp() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("Verify OTP")
                }

                if (otpError) {
                    Text(
                        text = "Please enter a valid OTP",
                        color = Color.Red,
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (canResendOtp) {
                    Text(
                        text = "Resend OTP",
                        color = Color.Blue,
                        modifier = Modifier
                            .clickable { onResendOtp() }
                            .padding(top = 16.dp)
                    )
                } else {
                    Text(
                        text = "Resend OTP in ${(resendOtpTimer.value / 1000)} seconds",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        val resendOtpTimer = remember { mutableStateOf(60000L) }
        GrabifyTheme {
            OtpVerificationScreen(
                onVerify = {},
                isLoading = false,
                canResendOtp = false,
                onResendOtp = {},
                resendOtpTimer = resendOtpTimer
            )
        }
    }
}
