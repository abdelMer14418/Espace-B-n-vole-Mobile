package com.example.espacebenevole.ui.nfc

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.espacebenevole.LoginActivity
import com.example.espacebenevole.databinding.FragmentNfcBinding
import org.json.JSONObject

class NFCFragment : Fragment(), NfcAdapter.ReaderCallback {

    private var _binding: FragmentNfcBinding? = null
    private val binding get() = _binding!!
    private var nfcAdapter: NfcAdapter? = null
    private var lastDetectedTag: Tag? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNfcBinding.inflate(inflater, container, false)
        initNFC()
        checkAuthentication()
        binding.writeButton.setOnClickListener {
            val beneficiaryId = binding.editTextBeneficiaryId.text.toString()
            if (beneficiaryId.isNotEmpty()) {
                writeIdToTag(lastDetectedTag, beneficiaryId)
            } else {
                Toast.makeText(context, "Veuillez entrer un ID valide !", Toast.LENGTH_SHORT).show()
            }
        }
        return binding.root
    }

    private fun initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (nfcAdapter == null) {
            Toast.makeText(context, "Le NFC n'est pas pris en charge sur cet appareil.", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(context, "Le NFC est désactivé.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAuthentication() {
        if (getToken().isNullOrEmpty()) {
            redirectToLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(activity, this, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag?) {
        lastDetectedTag = tag
    }

    private fun writeIdToTag(tag: Tag?, id: String) {
        if (tag == null) {
            Toast.makeText(context, "Aucun tag NFC détecté. Veuillez approcher un tag et réessayer.", Toast.LENGTH_LONG).show()
            lastDetectedTag = null
            return
        }

        val nfcData = Ndef.get(tag)
        nfcData?.use { ndef ->
            try {
                ndef.connect()
                if (ndef.isWritable) {
                    val record = NdefRecord.createTextRecord("en", id)
                    val message = NdefMessage(arrayOf(record))
                    ndef.writeNdefMessage(message)
                    sendVisitRegistration(id)
                } else {
                    Toast.makeText(context, "Le tag NFC est en lecture seule.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Échec de l'écriture sur le tag NFC : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                ndef.close()
            }
        }
    }

    private fun sendVisitRegistration(beneficiaryId: String) {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            redirectToLogin()
            return
        }

        val queue = Volley.newRequestQueue(context)
        val url = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/visits"

        val jsonBody = JSONObject().apply {
            put("id", beneficiaryId)
        }

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener<String> {
                Toast.makeText(context, "Visite bien enregistrée!", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener {
                handleVolleyError(it)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("auth" to token, "Content-Type" to "application/json")
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getBody(): ByteArray {
                return jsonBody.toString().toByteArray(Charsets.UTF_8)
            }
        }
        queue.add(stringRequest)
    }

    private fun getToken(): String? {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("AuthToken", null)
    }

    private fun redirectToLogin() {
        Toast.makeText(requireContext(), "Votre session s'est expirée, veuillez vous reconnecter!", Toast.LENGTH_LONG).show()
        val intent = Intent(activity, LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

    private fun handleVolleyError(error: VolleyError) {
        if (error.networkResponse?.statusCode == 401) {
            redirectToLogin()
        } else {
            Toast.makeText(context, "Erreur réseau: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
