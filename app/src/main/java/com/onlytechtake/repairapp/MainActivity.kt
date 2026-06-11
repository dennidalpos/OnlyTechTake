package com.onlytechtake.repairapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onlytechtake.repairapp.core.design.RepairAppTheme
import com.onlytechtake.repairapp.navigation.RepairAppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepairAppTheme {
                RepairAppNavHost()
            }
        }
    }
}
