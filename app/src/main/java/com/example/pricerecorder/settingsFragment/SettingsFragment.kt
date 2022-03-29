package com.example.pricerecorder.settingsFragment

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.pricerecorder.ConnectivityChecker
import com.example.pricerecorder.DateFormatter
import com.example.pricerecorder.MainToolbar
import com.example.pricerecorder.R
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.databinding.SettingsFragmentBinding
import com.example.pricerecorder.databinding.UploadProgressIndicatorLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class SettingsFragment : Fragment(){
    private lateinit var binding : SettingsFragmentBinding
    private lateinit var viewModel: SettingsFragmentViewModel
    private lateinit var database : ProductDatabase
    /*Variables used for accessing firebase functionality as authentication and cloud storage*/
    private lateinit var mAuth : FirebaseAuth
    private lateinit var storageRef : StorageReference
    companion object{
        //Equivalent to 1 Gigabyte
        const val MAX_DOWNLOAD_SIZE = 1L * 1024 * 1024 * 1024
    }

    private lateinit var appSettingPrefs : SharedPreferences
    private lateinit var sharedPrefsEditor : SharedPreferences.Editor
    private var isNightModeOn : Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.settings_fragment,container,false)

        val application: Application = requireNotNull(this.activity).application
        database = ProductDatabase.getInstance(application)
        val dataSource = database.productDatabaseDao
        val viewModelFactory = SettingFragmentViewModelFactory(dataSource,application)
        viewModel = ViewModelProvider(this, viewModelFactory)[SettingsFragmentViewModel::class.java]
        binding.viewModel = viewModel
        MainToolbar.show(activity as AppCompatActivity,getString(R.string.setting_fragment_title),true)

        viewModel.viewClicked.observe(viewLifecycleOwner,{
            it?.let {
                when(it){
                    R.id.account_section -> navigateToSignInFragment()
                    R.id.pick_theme -> selectAppTheme()
                    R.id.export_data_view -> saveBackupToCloudStorage()
                    R.id.import_data_view -> getBackupFromCloudStorage()
                }
                viewModel.onClickEventHandled()
            }
        })

        /*Handles the back device button pressed event. In this case is used to navigate back to the home fragment.
        It is explicitly added because otherwise, after restoring the db, the changes are not shown in the hme fragment*/
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp()
            }
        })

        /*Entry point of the firebase authentication sdk*/
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        if(user != null)
            setUserInfo(user)
        /*Creates a reference to upload,download or delete files. It can be tought as a pointer to a file in the cloud*/
        storageRef = FirebaseStorage.getInstance().reference
        setLastBackupDate()

        /*SharedPreferences is an interface for accessing and modifying preference data. The first argument is the desired file
        * and the second is the mode of operation*/
        appSettingPrefs = requireContext().getSharedPreferences("AppSettingPrefs",Context.MODE_PRIVATE)
        /*Editor is an interface used for modifying vales in a shared preferences object. All changes made are not copied back to the
        original SharedPreferences until commit() or apply() is called*/
        sharedPrefsEditor = appSettingPrefs.edit()
        /*Retrieve a boolean value from the preferences. In case the preference does not exist then the default value is returned*/
        isNightModeOn = appSettingPrefs.getBoolean("NightMode",false)

        if(isNightModeOn)
            binding.currentThemeTextview.setText(R.string.dark_theme_string)
        else
            binding.currentThemeTextview.setText(R.string.light_theme_string)

        setHasOptionsMenu(true)
        return binding.root
    }

    /*Gets the date of the last backup performed by the user only in the case one has been made*/
    private fun setLastBackupDate(){
        when{
            !isUserSignedIn(false) -> return
            !checkInternetConnection(false) -> return
            else -> {
                val fileRef = storageRef.child("room_backups/" + mAuth.currentUser!!.uid)
                fileRef.metadata.addOnSuccessListener {
                    val modifiedDate = DateFormatter.formatDate(it.creationTimeMillis)
                    binding.exportDateTextview.text = resources.getString(R.string.last_export_date,modifiedDate)
                }
            }
        }
    }

    /*Check if the user is currently signed in with its google account*/
    private fun isUserSignedIn(navigate:Boolean) : Boolean{
        return if(mAuth.currentUser == null){
            if(navigate)
                navigateToSignInFragment()
            false
        }else
            true
    }

    /*Downloads the backup file associated to the user from firebase cloud storage*/
    private fun getBackupFromCloudStorage(){
        when {
            !isUserSignedIn(true) -> return
            !checkInternetConnection(true) -> return
            else -> {
                val pair = createProgressDialog(R.string.downloading_in_progress_title,R.string.dowloading_msg)
                val dialog = pair.first
                val fileRef = storageRef.child("room_backups/" + mAuth.currentUser!!.uid)
                fileRef.getBytes(MAX_DOWNLOAD_SIZE)
                    .addOnSuccessListener {
                        importBackupToDatabase(it)
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    /*Receives a bytearray with all the backup data, and imports it to the database*/
    private fun importBackupToDatabase(data:ByteArray) {
        val databaseFile = File(database.openHelper.writableDatabase.path)
        databaseFile.writeBytes(data)
        Toast.makeText(requireContext(),getString(R.string.restore_successful_msg),Toast.LENGTH_SHORT).show()
    }

    /*Saves the db backup file created in firebase storage*/
    private fun saveBackupToCloudStorage(){
        when {
            !isUserSignedIn(true) -> return
            !checkInternetConnection(true) -> return
            else -> {
                val backupFile = createDbBackup()
                if (backupFile != null) {
                    val backupRef = storageRef.child("room_backups/" + mAuth.currentUser!!.uid)

                    val pair = createProgressDialog(R.string.uploading_in_progress_msg,R.string.percentage_uploaded_msg)
                    val dialog = pair.first
                    val dialogBinding = pair.second
                    backupRef.putBytes(backupFile.readBytes())
                        .addOnSuccessListener {
                            dialog.dismiss()
                            Toast.makeText(requireContext(),
                                R.string.backup_successful_msg,
                                Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            dialog.dismiss()
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        }
                        .addOnProgressListener {
                            val progress = (100 * it.bytesTransferred) / it.totalByteCount
                            dialogBinding.uploadProgressTextview.text =
                                resources.getString(R.string.percentage_uploaded_msg,
                                    "${progress}%")
                        }
                }
            }
        }
    }

    /*If the db was successfully backed up, it returns the file where it is stored*/
    private fun createDbBackup() : File?{
        if(viewModel.backupDatabase()){
            return File(database.openHelper.writableDatabase.path)
        }else{
            Toast.makeText(requireContext(),getString(R.string.database_backup_error_msg),Toast.LENGTH_SHORT).show()
        }
        return null
    }

    /*Checks if the device currently has internet connection*/
    private fun checkInternetConnection(showMsg:Boolean) : Boolean{
        if(!ConnectivityChecker.isOnline(requireActivity().application)){
            if(showMsg)
                Toast.makeText(requireContext(), getString(R.string.no_internet_signal_msg), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    /*Creates a bottom sheet dialog shows the user the current upload progress*/
    private fun createProgressDialog(title:Int,msg:Int) : Pair<Dialog,UploadProgressIndicatorLayoutBinding>{
        val dialogBinding = UploadProgressIndicatorLayoutBinding.inflate(layoutInflater)
        dialogBinding.progressTitle.text = resources.getString(title)
        when(msg){
            R.string.percentage_uploaded_msg ->  dialogBinding.uploadProgressTextview.text = resources.getString(msg,"0%")
            R.string.dowloading_msg -> dialogBinding.uploadProgressTextview.text = resources.getString(msg)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root).create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.show()
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)
        return Pair(dialog,dialogBinding)
    }

    /*Creates a dialog where the user can select a theme to be applied throughout the app*/
    private fun selectAppTheme(){
        val items = resources.getStringArray(R.array.theme_dialog_options)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.pick_theme_title))
            .setItems(items
            ) { dialog, item ->
                when(item){
                    //Light theme
                    0 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        //Set a boolean value in the preferences editor, to be written back once apply() is called.
                        sharedPrefsEditor.putBoolean("NightMode",false)
                        sharedPrefsEditor.apply()
                        binding.currentThemeTextview.setText(R.string.light_theme_string)
                    }
                    //Dark theme
                    1 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        sharedPrefsEditor.putBoolean("NightMode",true)
                        sharedPrefsEditor.apply()
                        binding.currentThemeTextview.setText(R.string.dark_theme_string)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel_button_string
            ) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun setUserInfo(user: FirebaseUser) {
        binding.accountNameTextview.text = user.displayName
        binding.accountEmailTextview.text = user.email
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> navigateUp()
        }
        return true
    }

    private fun navigateUp(){
        Navigation.findNavController(binding.root).navigate(SettingsFragmentDirections.actionSettingsFragmentToHomeFragment())
    }

    private fun navigateToSignInFragment(){
        Navigation.findNavController(binding.root).navigate(SettingsFragmentDirections.actionSettingsFragmentToSignInFragment())
    }
}