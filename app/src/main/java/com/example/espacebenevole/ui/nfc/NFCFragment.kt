package com.example.espacebenevole.ui.nfc

import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
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
import java.util.Arrays

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

        // Configure the visibility of EditText and Button
        // Comment or uncomment the following lines for version switching
        binding.editTextBeneficiaryId.visibility = View.GONE // Change to View.VISIBLE for the write version
        binding.writeButton.visibility = View.GONE // Change to View.VISIBLE for the write version

        // Comment or uncomment the following line to enable setup for write mode
        //setupWriteButton()

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

    private fun setupWriteButton() {
        binding.writeButton.setOnClickListener {
            val beneficiaryId = binding.editTextBeneficiaryId.text.toString()
            if (beneficiaryId.isNotEmpty()) {
                lastDetectedTag?.let { tag ->
                    writeIdToTag(tag, beneficiaryId)
                } ?: Toast.makeText(context, "Aucun tag NFC détecté.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Veuillez entrer un ID valide !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeIdToTag(tag: Tag?, id: String) {
        if (tag == null) {
            Toast.makeText(context, "Aucun tag NFC détecté. Veuillez approcher un tag et réessayer.", Toast.LENGTH_LONG).show()
            return
        }

        val nfcData = Ndef.get(tag)
        nfcData?.use { ndef ->
            try {
                ndef.connect()
                val record = NdefRecord.createTextRecord("en", id)
                val message = NdefMessage(arrayOf(record))
                ndef.writeNdefMessage(message)
                Toast.makeText(context, "ID écrit sur le tag NFC : $id", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Échec de l'écriture sur le tag NFC : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                ndef.close()
            }
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
        // Uncomment the following line for the version that reads and sends requests automatically
         sendRequestIfIdFound(tag)
    }

    private fun readIdFromTag(tag: Tag): String? {
        val ndef = Ndef.get(tag)
        ndef?.connect()
        try {
            val message = ndef?.ndefMessage
            if (message != null && message.records.isNotEmpty()) {
                val record = message.records[0]
                if (record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_TEXT)) {
                    val payload = record.payload
                    val languageCodeLength = payload[0].toInt() and 0x3F // Get the language code length
                    return String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur lors de la lecture du tag NFC: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            ndef?.close()
        }
        return null
    }


    private fun sendRequestIfIdFound(tag: Tag?) {
        tag?.let {
            val id = readIdFromTag(it)
            if (id != null) {
                sendVisitRegistration(id)
            } else {
                Toast.makeText(context, "ID non trouvé sur le jeton NFC.", Toast.LENGTH_LONG).show()
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

        //val requestBody = jsonBody.toString()


        //Log.d("NFCFragment", "Sending POST request body: $requestBody")



        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener<String> {
                Toast.makeText(context, "Visite ajoutée au bénévole dont l'ID est :  $beneficiaryId.", Toast.LENGTH_SHORT).show()
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