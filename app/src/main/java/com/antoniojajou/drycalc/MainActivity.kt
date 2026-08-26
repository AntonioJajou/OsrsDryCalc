package com.antoniojajou.drycalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.antoniojajou.drycalc.ui.OsrsDryCalcApp
import com.antoniojajou.drycalc.viewmodel.DryCalcViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: DryCalcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OsrsDryCalcApp(viewModel) }
    }
}
