package com.oscar.atestados.viewModel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NfcViewModel : ViewModel() {
    private val _nfcTag = MutableStateFlow<Tag?>(null)
    val nfcTag: StateFlow<Tag?> = _nfcTag.asStateFlow()

    fun setNfcTag(tag: Tag?) {
        _nfcTag.value = tag
    }

    fun clearNfcTag() {
        _nfcTag.value = null
    }
}