package com.example.pricerecorder.signInFragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.pricerecorder.MainToolbar
import com.example.pricerecorder.R
import com.example.pricerecorder.databinding.SignInFragmentBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignInFragment : Fragment() {
    private lateinit var binding : SignInFragmentBinding

    //Used for user authentication with google account
    private lateinit var mAuth : FirebaseAuth
    private lateinit var googleSignInClient : GoogleSignInClient

    companion object{
        const val TAG = "Google_Sign_In"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater,R.layout.sign_in_fragment,container,false)
        MainToolbar.show(activity as AppCompatActivity,resources.getString(R.string.account_section_title),false)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        //Entry point for firebase authentication sdk
        mAuth = FirebaseAuth.getInstance()

        enableSignOutButton(mAuth.currentUser != null)
        binding.signInButton.setOnClickListener { signIn() }
        binding.signOutButton.setOnClickListener { signOut() }

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun signOut() {
        mAuth.signOut()
        googleSignInClient.signOut()
        navigateUp()
    }

    /*Registers an activity to get a result from it*/
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.data != null){
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            if(task.isSuccessful){
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(TAG, "Google sign in failed", e)
                }
            }else{
                Log.w(TAG, "warning: " + task.exception.toString())
            }
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
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

                    //Check if user is new or existing
                    if(task.result.additionalUserInfo!!.isNewUser){
                        Toast.makeText(requireContext()
                            ,resources.getString(R.string.account_created_string,mAuth.currentUser!!.displayName)
                            ,Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(requireContext(),resources.getString(R.string.logged_in_successfully,mAuth.currentUser!!.email)
                            ,Toast.LENGTH_SHORT).show()
                    }

                    navigateUp()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential: failure", task.exception)
                }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> navigateUp()
        }
        return true
    }

    private fun enableSignOutButton(enable:Boolean){
        binding.signOutButton.visibility = if(enable) View.VISIBLE else View.GONE
    }

    private fun navigateUp() {
        Navigation.findNavController(binding.root).navigate(SignInFragmentDirections.actionSignInFragmentToSettingsFragment())
    }
}