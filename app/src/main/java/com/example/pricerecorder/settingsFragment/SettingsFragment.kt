package com.example.pricerecorder.settingsFragment

import android.app.Application
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.pricerecorder.*
import com.example.pricerecorder.R
import com.example.pricerecorder.database.ProductDatabase
import com.example.pricerecorder.theme.PriceRecorderTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class SettingsFragment : Fragment(){
    private lateinit var viewModel: SettingsFragmentViewModel
    private lateinit var database : ProductDatabase
    companion object{
        //Equivalent to 1 Gigabyte
        const val MAX_DOWNLOAD_SIZE = 1L * 1024 * 1024 * 1024
    }

    data class SectionOption(
        val title:String,
        var desc:String? = null,
        var onClick:() -> Unit
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application: Application = requireNotNull(this.activity).application
        database = ProductDatabase.getInstance(application)
        val viewModelFactory = SettingFragmentViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory)[SettingsFragmentViewModel::class.java]
        viewModel.getLastBackupDate()

        return ComposeView(requireContext()).apply {
            setContent {
                SettingsScreen()
            }
        }
    }

    /*Downloads the backup file associated to the user from firebase cloud storage*/
    private fun getBackupFromCloudStorage(){
        when {
            !viewModel.isUserSignedIn { navigateToSignInFragment() } -> return
            !viewModel.checkInternetConnection{
                Toast.makeText(requireContext(), getString(R.string.no_internet_signal_msg), Toast.LENGTH_SHORT).show()
            } -> return
            else -> {
                val fileRef = viewModel.storageRef.child("room_backups/" + viewModel.user.value!!.uid)

                createProgressNotification(
                    requireContext(),
                    DOWNLOAD_NOTIFICATION_ID,
                    R.drawable.ic_download,
                    getString(R.string.download_notification_title),
                    null,
                    indefinite = true,
                    pendingWork = { _, onComplete ->
                        fileRef.getBytes(MAX_DOWNLOAD_SIZE)
                            .addOnSuccessListener {
                                importBackupToDatabase(it)
                                viewModel.notifyDatabaseRestored()
                                onComplete(getString(R.string.download_notification_success_text),
                                    R.drawable.ic_download_done)
                            }
                            .addOnFailureListener {
                                onComplete(getString(R.string.download_notification_failed_text),
                                    R.drawable.ic_warning)
                                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                            }
                    }
                )
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
    private fun saveBackupToCloudStorage(backupFile:File?){
        if (backupFile != null) {
            val backupRef = viewModel.storageRef.child("room_backups/" + viewModel.user.value!!.uid)

            createProgressNotification(
                requireContext(),
                UPLOAD_NOTIFICATION_ID,
                R.drawable.ic_cloud_upload,
                getString(R.string.uploading_in_progress_msg),
                null,
                false,
            ){ setProgress, onComplete ->
                backupRef.putBytes(backupFile.readBytes())
                    .addOnSuccessListener {
                        onComplete(getString(R.string.upload_notification_success_text),
                            R.drawable.ic_download_done)
                        Toast.makeText(requireContext(),
                            R.string.upload_notification_success_text,
                            Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        onComplete(getString(R.string.upload_notification_failed_text),
                            R.drawable.ic_warning)
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener {
                        val progress = ((100 * it.bytesTransferred) / it.totalByteCount).toInt()
                        setProgress(progress)
                    }
            }
        }
    }

    /*Creates a db backup and stores it in a local file*/
    @OptIn(DelicateCoroutinesApi::class)
    private fun createDbBackup(){
        when{
            !viewModel.isUserSignedIn { navigateToSignInFragment() } -> return
            !viewModel.checkInternetConnection{
                Toast.makeText(requireContext(), getString(R.string.no_internet_signal_msg), Toast.LENGTH_SHORT).show()
            } -> return
            else -> {
                val deferred = viewModel.backupDatabaseAsync()
                GlobalScope.launch {
                    val backupSuccessful = deferred.await()
                    if(backupSuccessful){
                        saveBackupToCloudStorage(File(database.openHelper.writableDatabase.path))
                    }else{
                        Toast.makeText(requireContext(),getString(R.string.database_backup_error_msg),Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun navigateUp(){
        findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToHomeFragment())
    }

    private fun navigateToSignInFragment(){
        findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToSignInFragment())
    }

    @Composable
    private fun SettingsScreen(){
        BackPressHandler(onBackPressed = { navigateUp() })
        val scaffoldState = rememberScaffoldState()

        PriceRecorderTheme {
            Scaffold(scaffoldState = scaffoldState,
                backgroundColor = MaterialTheme.colors.background,
                topBar = {
                    ShowTopAppBar(appBarTitle = stringResource(id = R.string.setting_fragment_title),
                        actionItems = listOf(),
                        navigationIcon = {
                            IconButton(onClick = { navigateUp() }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                            }
                        })
                }) {
                SettingScreenContent(modifier = Modifier.padding(it))
            }
        }
    }

    @Composable
    private fun SettingScreenContent(modifier: Modifier = Modifier){
        val user by viewModel.user
        val backupDate by viewModel.lastBackupDate

        Surface(modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colors.surface) {
            Column(modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())) {
                SectionHeader(title = stringResource(id = R.string.account_section_title))
                val accountTitle = user?.displayName ?: stringResource(id = R.string.sign_in_string)
                val accountDesc = user?.email ?: stringResource(id = R.string.sign_in_description)
                SectionOptions(options = listOf(
                    SectionOption(title = accountTitle,
                        desc = accountDesc, onClick = { navigateToSignInFragment() })
                ))

                SectionHeader(title = stringResource(id = R.string.db_section_title))
                SectionOptions(options = listOf(
                    SectionOption(title = stringResource(id = R.string.export_data_string),
                        desc = backupDate ?: getString(R.string.no_backups_found_msg),
                        onClick = { createDbBackup() }),
                    SectionOption(title = stringResource(id = R.string.import_data_string),
                        desc = stringResource(id = R.string.restore_data_description),
                        onClick = { getBackupFromCloudStorage() })
                ))

                SectionHeader(title = stringResource(id = R.string.info_section_title))
                SectionOptions(options = listOf(
                    SectionOption(title = stringResource(id = R.string.version_title_string),
                        desc = BuildConfig.VERSION_NAME, onClick = {}),
                    SectionOption(title = stringResource(id = R.string.help_and_faq_string),
                        onClick = {}),
                    SectionOption(title = stringResource(id = R.string.about_title_string),
                        onClick = {}),
                    SectionOption(title = stringResource(id = R.string.review_app_string),
                        onClick = {})
                ))
            }
        }
    }

    @Composable
    private fun SectionHeader(title:String, modifier: Modifier = Modifier){
        Column(modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start) {
            Text(text = title, color = MaterialTheme.colors.secondary,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp))
            Divider(thickness = 2.dp,
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp)
                    .fillMaxWidth())
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun SectionOptions(options:List<SectionOption>, modifier: Modifier = Modifier){
        Column(modifier = modifier.fillMaxWidth()){
            options.forEachIndexed { index, sectionOption ->
                Surface(modifier = Modifier.fillMaxWidth(),
                    onClick = sectionOption.onClick) {
                    Column(modifier = modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start){
                        Text(text = sectionOption.title, color = MaterialTheme.colors.onSurface,
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp),
                            modifier = Modifier
                                .padding(start = 20.dp, top = 8.dp)
                                .alpha(1f))

                        val desc = sectionOption.desc
                        if(!desc.isNullOrEmpty()) {
                            Text(text = desc, color = MaterialTheme.colors.onSurface,
                                style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Normal),
                                modifier = Modifier
                                    .padding(start = 20.dp)
                                    .alpha(0.8f))
                        }

                        if(index < options.size-1){
                            Divider(thickness = 1.dp,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                                    .fillMaxWidth()
                                    .alpha(0.8f))
                        }
                    }
                }
            }
        }
    }
}