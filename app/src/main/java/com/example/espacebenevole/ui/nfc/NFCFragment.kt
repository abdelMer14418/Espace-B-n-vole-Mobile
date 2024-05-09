package com.example.espacebenevole.ui.nfc

import android.content.Context
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
import com.example.espacebenevole.databinding.FragmentNfcBinding

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
        binding.writeButton.setOnClickListener {
            val beneficiaryId = binding.editTextBeneficiaryId.text.toString()
            if (beneficiaryId.isNotEmpty()) {
                writeIdToTag(lastDetectedTag, beneficiaryId)
            } else {
                Toast.makeText(context, "Please enter a valid ID.", Toast.LENGTH_SHORT).show()
            }
        }
        return binding.root
    }

    private fun initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (nfcAdapter == null) {
            Toast.makeText(context, "NFC is not supported on this device.", Toast.LENGTH_LONG).show()
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(context, "NFC is disabled.", Toast.LENGTH_LONG).show()
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
        // Additional logic can be added here if needed
    }

    private fun writeIdToTag(tag: Tag?, id: String) {
        if (tag == null) {
            Toast.makeText(context, "No NFC tag detected. Please approach a tag and try again.", Toast.LENGTH_LONG).show()
            lastDetectedTag = null // Clear the last detected tag to prevent retries on an invalid tag
            return
        }

        val nfcData = Ndef.get(tag)
        if (nfcData == null) {
            Toast.makeText(context, "NFC is not NDEF.", Toast.LENGTH_LONG).show()
            return
        }

        nfcData.use { ndef ->
            try {
                ndef.connect()
                if (ndef.isWritable) {
                    val record = NdefRecord.createTextRecord("en", id)
                    val message = NdefMessage(arrayOf(record))
                    ndef.writeNdefMessage(message)
                    Toast.makeText(context, "ID written to tag successfully: $id", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "NFC tag is read-only.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to write to NFC tag: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                ndef.close()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}