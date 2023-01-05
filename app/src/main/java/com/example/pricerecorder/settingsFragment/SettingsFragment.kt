package com.example.pricerecorder.settingsFragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
    private val viewModel: SettingsFragmentViewModel by viewModels { SettingsFragmentViewModel.factory }
    private lateinit var database : ProductDatabase
    /*Variables to handle theme changes made by the user*/
    private lateinit var appSettingPrefs : SharedPreferences
    private lateinit var sharedPrefsEditor : SharedPreferences.Editor
    private var nightMode : Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    companion object{
        //Equivalent to 1 Gigabyte
        const val MAX_DOWNLOAD_SIZE = 1L * 1024 * 1024 * 1024
    }
    data class SectionOption(
        val title:String,
        var desc:String? = null,
        var onClick:() -> Unit,
        val leadingIcon:@Composable (() -> Unit)? = null
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        database = ProductDatabase.getInstance(requireNotNull(this.activity).application)
        viewModel.getLastBackupDate()

        /*SharedPreferences is an interface for accessing and modifying preference data. The first argument is the desired file
        * and the second is the mode of operation*/
        appSettingPrefs = requireContext().getSharedPreferences("AppSettingPrefs", Context.MODE_PRIVATE)
        /*Editor is an interface used for modifying values in a shared preferences object. All changes made are not copied back to the
        original SharedPreferences until commit() or apply() is called*/
        sharedPrefsEditor = appSettingPrefs.edit()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                SettingsScreen()
            }
        }
    }

    /*Saves the user preference regarding the theme to be applied in the app*/
    private fun saveUserThemePreference(mode:Int){
        AppCompatDelegate.setDefaultNightMode(mode)
        //Set an int value in the preferences editor, to be written back once apply() is called.
        sharedPrefsEditor.putInt(ThemeUtils.KEY_NIGHT_MODE,mode)
        sharedPrefsEditor.apply()
    }

    /*Creates a dialog where the user can select a theme to be applied throughout the app*/
    @Composable
    private fun SelectAppThemeDialog(show:Boolean,onDismiss:()->Unit){
        /*Retrieve an Int value from the preferences. In case the preference does not exist then the default value is returned*/
        nightMode = appSettingPrefs.getInt(ThemeUtils.KEY_NIGHT_MODE,AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        val radioOptions = listOf(
            RadioButtonItem(text = stringResource(id = R.string.light_theme_string),
                selected = (nightMode == AppCompatDelegate.MODE_NIGHT_NO),
                onClick = {
                    saveUserThemePreference(AppCompatDelegate.MODE_NIGHT_NO)
                    onDismiss()
                }),
            RadioButtonItem(text = stringResource(id = R.string.dark_theme_string),
                selected = (nightMode == AppCompatDelegate.MODE_NIGHT_YES),
                onClick = {
                    saveUserThemePreference(AppCompatDelegate.MODE_NIGHT_YES)
                    onDismiss()
                }),
            RadioButtonItem(text = stringResource(id = R.string.system_default_theme_string),
                selected = (nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
                onClick = {
                    saveUserThemePreference(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    onDismiss()
                })
        )

        CustomAlertDialog(show = show,
            title = stringResource(id = R.string.select_theme_title),
            msg = {
                SingleSelectableRadioButtons(radioOptions)
            },
            confirmButtonText = null,
            dismissButtonText = null,
            onConfirm = {},
            onDismiss = onDismiss)
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
                        viewModel.getLastBackupDate()
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

        PriceRecorderTheme(
            context = requireContext()
        ) {
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
        val showThemeDialog = remember {
            mutableStateOf(false)
        }
        
        SelectAppThemeDialog(show = showThemeDialog.value,
            onDismiss = { showThemeDialog.value = false })

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
                        desc = accountDesc, onClick = { navigateToSignInFragment() },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.AccountCircle,
                                contentDescription = "",
                                modifier = Modifier.padding(end = 16.dp))
                        })
                ))
                
                SectionHeader(title = stringResource(id = R.string.screen_section_title))
                SectionOptions(options = listOf(
                    SectionOption(title = stringResource(id = R.string.theme_setting_option),
                        desc = when(appSettingPrefs.getInt(ThemeUtils.KEY_NIGHT_MODE,AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)){
                            AppCompatDelegate.MODE_NIGHT_NO -> stringResource(id = R.string.light_theme_string)
                            AppCompatDelegate.MODE_NIGHT_YES -> stringResource(id = R.string.dark_theme_string)
                            else -> stringResource(id = R.string.system_default_theme_string)
                        },
                        onClick = { showThemeDialog.value = true },
                        leadingIcon = {
                            Icon(painter = painterResource(id = R.drawable.ic_theme),
                                contentDescription = "",
                                modifier = Modifier.padding(end = 16.dp))
                        })
                ))

                SectionHeader(title = stringResource(id = R.string.db_section_title))
                SectionOptions(options = listOf(
                    SectionOption(title = stringResource(id = R.string.export_data_string),
                        desc = backupDate ?: getString(R.string.no_backups_found_msg),
                        onClick = { createDbBackup() },
                        leadingIcon = {
                            Icon(painter = painterResource(id = R.drawable.ic_cloud_upload),
                                contentDescription = "",
                                modifier = Modifier.padding(end = 16.dp))
                        }),
                    SectionOption(title = stringResource(id = R.string.import_data_string),
                        desc = stringResource(id = R.string.restore_data_description),
                        onClick = { getBackupFromCloudStorage() },
                        leadingIcon = {
                            Icon(Icons.Default.CloudDownload,
                                contentDescription = "",
                                modifier = Modifier.padding(end = 16.dp))
                        })
                ))

                SectionHeader(title = stringResource(id = R.string.info_section_title))
                SectionOptions(options = listOf(
                    SectionOption(title = stringResource(id = R.string.version_title_string),
                        desc = BuildConfig.VERSION_NAME, onClick = {}),
                    SectionOption(title = stringResource(id = R.string.privacy_policy_section_title),
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
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start) {
                            sectionOption.leadingIcon?.let { it() }

                            Column(modifier = modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, end = 8.dp),
                                horizontalAlignment = Alignment.Start){

                                Text(text = sectionOption.title, color = MaterialTheme.colors.onSurface,
                                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp),
                                    modifier = Modifier
                                        .alpha(1f))

                                val desc = sectionOption.desc
                                if(!desc.isNullOrEmpty()) {
                                    Text(text = desc, color = MaterialTheme.colors.onSurface,
                                        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Normal),
                                        modifier = Modifier
                                            .alpha(0.8f))
                                }
                            }
                        }

                        if(index < options.size-1){
                            Divider(thickness = 1.dp,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                                    .fillMaxWidth()
                                    .alpha(0.8f))
                        }
                    }
                }
            }
        }
    }


}