package com.kkn.almufid.sekretaris.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Satu factory generik dipakai ulang oleh semua ViewModel di app ini,
 * supaya menambah modul baru tidak perlu menambah file Factory baru.
 * Cukup panggil: GenericViewModelFactory { JurnalViewModel(dao) }
 */
class GenericViewModelFactory(private val pembuat: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = pembuat() as T
}
