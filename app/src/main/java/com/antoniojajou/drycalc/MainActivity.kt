package com.antoniojajou.drycalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.antoniojajou.drycalc.ui.OsrsDryCalcApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OsrsDryCalcApp() }
    }
}
