package com.example.espacebenevole.ui.nfc

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.espacebenevole.R
import com.example.espacebenevole.databinding.FragmentHomeBinding
import com.example.espacebenevole.databinding.FragmentNfcBinding
import org.json.JSONArray



class NFCFragment : Fragment() {

    private var _binding: FragmentNfcBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNfcBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun checkAuthenticationAndFetchEvents() {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            redirectToLogin()
        } else {
            //fetchAndDisplayEvents(token)
        }
    }

    private fun getToken(): String? {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("AuthToken", null)
    }

    private fun redirectToLogin() {
        Toast.makeText(requireContext(), "Votre session s'est expirée, veuillez vous reconnecter!", Toast.LENGTH_LONG).show()
        // Handle redirection to login; possibly via Navigation Component or starting a new Activity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
