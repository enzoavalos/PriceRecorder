package com.example.pricerecorder.signInFragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pricerecorder.*
import com.example.pricerecorder.R
import com.example.pricerecorder.theme.PriceRecorderTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class SignInFragment : Fragment() {
    //Used for user authentication with google account
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    /*Keeps track of the current logged user, in order to be used as a state*/
    private val user = MutableLiveData<FirebaseUser?>()
    companion object {
        const val TAG = "Google_Sign_In"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        //Entry point for firebase authentication sdk
        mAuth = FirebaseAuth.getInstance()
        user.value = mAuth.currentUser

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                SignInScreen()
            }
        }
    }

    private fun checkInternetConnection() : Boolean{
        if(!ConnectivityChecker.isOnline(requireActivity().application)){
            Toast.makeText(requireContext(), getString(R.string.no_internet_signal_msg), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun signIn() {
        if(checkInternetConnection()){
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }
    }

    private fun signOut() {
        mAuth.signOut()
        googleSignInClient.signOut()
        user.value = null
    }

    /*Registers an activity to get a result from it*/
    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.data != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                if (task.isSuccessful) {
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        val account = task.getResult(ApiException::class.java)!!
                        Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                        firebaseAuthWithGoogle(account.idToken!!)
                    } catch (e: ApiException) {
                        // Google Sign In failed, update UI appropriately
                        Log.w(TAG, "Google sign in failed", e)
                        showSignInErrorMessage()
                    }
                } else {
                    Log.w(TAG, "warning: " + task.exception.toString())
                    showSignInErrorMessage()
                }
            }
        }

    /*After a user successfully signs in, get an ID token from the GoogleSignInAccount object, exchange it
    for a Firebase credential, and authenticate with Firebase using the Firebase credential*/
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential: success")
                    user.value = mAuth.currentUser

                    //Check if user is new or existing
                    if (task.result.additionalUserInfo!!.isNewUser) {
                        Toast.makeText(requireContext(),
                            resources.getString(R.string.account_created_string,
                                mAuth.currentUser!!.email),
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(),
                            resources.getString(R.string.logged_in_successfully,
                                mAuth.currentUser!!.displayName),
                            Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential: failure", task.exception)
                    showSignInErrorMessage()
                }
            }
    }

    private fun navigateUp() {
        findNavController().navigate(SignInFragmentDirections.actionSignInFragmentToSettingsFragment())
    }

    private fun showSignInErrorMessage(){
        Toast.makeText(requireContext(),getString(R.string.sign_in_error_message),Toast.LENGTH_SHORT).show()
    }

    @Composable
    private fun SignInScreen(){
        BackPressHandler(onBackPressed = {navigateUp()})
        val currentUser = user.observeAsState()
        
        PriceRecorderTheme {
            Scaffold(
                topBar = {
                    ShowTopAppBar(appBarTitle = stringResource(id = R.string.account_section_title),
                        actionItems = listOf(),
                        navigationIcon = {
                            IconButton(onClick = { navigateUp() }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                            }
                        })
                }
            ){
                SignInScreenContent(currentUser.value,Modifier.padding(it))
            }
        }
    }

    @Composable
    private fun SignInScreenContent(user:FirebaseUser?,modifier: Modifier = Modifier){
        Surface(modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colors.primaryVariant,
                    MaterialTheme.colors.primary
                )
            )),
            color = Color.Unspecified) {
            if(user == null){
                Column(modifier = Modifier
                    .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    AdaptiveIconImage(
                        adaptiveDrawable = R.mipmap.launcher_icon_round,
                        drawable = R.drawable.ic_account_circle,
                        modifier = Modifier
                            .clip(CircleShape)
                            .sizeIn(minHeight = 120.dp, minWidth = 120.dp)
                            .background(color = Color.Transparent))
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.ic_error), contentDescription = "")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.sign_in_to_enable_functions_string),
                            color = MaterialTheme.colors.onSurface,
                            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Normal),
                            modifier = Modifier.weight(1f,true))
                    }
                    GoogleSignInButton(onClick = { signIn() },
                        modifier = Modifier.padding(16.dp))
                }
            }else
                UserSignedInScreen(user = user,
                    onSignOut = {
                        signOut()
                    })
        }
    }

    @Composable
    private fun UserSignedInScreen(user: FirebaseUser, onSignOut:() -> Unit){
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.photoUrl)
                    .crossfade(true)
                    .build(),
                modifier = Modifier
                    .clip(CircleShape)
                    .sizeIn(minHeight = 120.dp, minWidth = 120.dp),
                contentDescription = "")
            user.displayName?.let { 
                Text(text = it, color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
            }
            user.email?.let {
                Text(text = it,  color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Normal),
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp,top = 8.dp))
            }
            Button(onClick = onSignOut, colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary
            ), modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(id = R.string.log_out_title_string),
                    color = MaterialTheme.colors.onSecondary)
            }
        }
    }

    @Preview(heightDp = 450, widthDp = 360, showBackground = true)
    @Composable
    private fun SignInScreenContentPreview(){
        PriceRecorderTheme {
            SignInScreenContent(null)
        }
    }
}