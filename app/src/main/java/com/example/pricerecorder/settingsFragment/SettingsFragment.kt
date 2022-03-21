package com.example.pricerecorder.settingsFragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.pricerecorder.MainToolbar
import com.example.pricerecorder.R
import com.example.pricerecorder.databinding.SettingsFragmentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SettingsFragment : Fragment(){
    private lateinit var binding : SettingsFragmentBinding
    private lateinit var viewModel: SettingsFragmentViewModel
    private lateinit var mAuth : FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.settings_fragment,container,false)
        viewModel = SettingsFragmentViewModel()
        binding.viewModel = viewModel
        MainToolbar.show(activity as AppCompatActivity,getString(R.string.setting_fragment_title),true)

        viewModel.viewClicked.observe(viewLifecycleOwner,{
            it?.let {
                when(it){
                    R.id.account_section -> navigateToSignInFragment()
                    R.id.pick_theme -> Toast.makeText(requireContext(),"Tema",Toast.LENGTH_SHORT).show()
                    R.id.currency_view -> Toast.makeText(requireContext(),"Moneda",Toast.LENGTH_SHORT).show()
                    R.id.export_data_view -> Toast.makeText(requireContext(),"Exportar",Toast.LENGTH_SHORT).show()
                    R.id.import_data_view -> Toast.makeText(requireContext(),"Importar",Toast.LENGTH_SHORT).show()
                }
                viewModel.onClickEventHandled()
            }
        })

        /*Entry point of the firebase authentication sdk*/
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        if(user != null)
            setUserInfo(user)

        setHasOptionsMenu(true)
        return binding.root
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