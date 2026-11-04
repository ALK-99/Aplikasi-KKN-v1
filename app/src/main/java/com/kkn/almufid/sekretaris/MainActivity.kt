package com.kkn.almufid.sekretaris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkn.almufid.sekretaris.ui.GenericViewModelFactory
import com.kkn.almufid.sekretaris.ui.navigation.AppNav
import com.kkn.almufid.sekretaris.ui.theme.SekretarisKKNTheme
import com.kkn.almufid.sekretaris.update.UpdateDialog
import com.kkn.almufid.sekretaris.update.UpdateViewModel
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SekretarisKKNTheme {
                val updateVm: UpdateViewModel = viewModel(
                    factory = GenericViewModelFactory { UpdateViewModel(applicationContext) }
                )
                LaunchedEffect(Unit) { updateVm.cekSekaliSaatBuka() }

                AppNav()
                UpdateDialog(updateVm)
            }
        }
    }
}
