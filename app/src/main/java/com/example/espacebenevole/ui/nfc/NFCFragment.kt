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
            writeIdToTag(lastDetectedTag)
        }
        return binding.root
    }

    private fun initNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (nfcAdapter == null) {
            Toast.makeText(context, "NFC is not supported on this device.", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
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
        val ndef = Ndef.get(tag)
        ndef?.connect()
        val message = ndef.ndefMessage?.records?.joinToString(separator = "\n") { record ->
            String(record.payload)
        } ?: "Empty Tag"
        ndef?.close()

        activity?.runOnUiThread {
            Toast.makeText(context, "Read from NFC Tag:\n$message", Toast.LENGTH_LONG).show()
        }
    }

    private fun writeIdToTag(tag: Tag?, id: String = "id=1") {
        if (tag == null) {
            Toast.makeText(context, "No NFC tag detected.", Toast.LENGTH_LONG).show()
            return
        }

        val nfcData = Ndef.get(tag)
        if (nfcData == null) {
            Toast.makeText(context, "NFC is not NDEF.", Toast.LENGTH_LONG).show()
            return
        }

        nfcData.use { ndef ->
            ndef.connect()
            if (ndef.isWritable) {
                val record = NdefRecord.createTextRecord("en", id)
                val message = NdefMessage(arrayOf(record))
                ndef.writeNdefMessage(message)
                Toast.makeText(context, "ID written to tag successfully.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "NFC tag is read-only.", Toast.LENGTH_LONG).show()
            }
            ndef.close()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}