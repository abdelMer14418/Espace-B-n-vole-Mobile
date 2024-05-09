package com.example.espacebenevole.ui.nfc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NFCViewModel : ViewModel() {

    private val _nfcData = MutableLiveData<String>()
    val nfcData: LiveData<String> = _nfcData

    fun updateNfcData(data: String) {
        _nfcData.value = data
    }
}
