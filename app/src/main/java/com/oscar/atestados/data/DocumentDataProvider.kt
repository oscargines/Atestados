package com.oscar.atestados.data

interface DocumentDataProvider {
    fun getData(): Map<String, String>
    fun validateData(): Pair<Boolean, List<String>>
}