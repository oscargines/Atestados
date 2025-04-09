package com.oscar.atestados.data

data class RawNfcData(
    val uid: String?,
    val can: String?,
    val dg1Bytes: ByteArray?,
    val dg11Bytes: ByteArray?,
    val dg13Bytes: ByteArray?
)