package com.diploma.roadsideassistance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.diploma.roadsideassistance.ui.navigation.RoadsideNavGraph
import com.diploma.roadsideassistance.ui.theme.RoadsideAssistanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as RoadsideApp).container

        setContent {
            RoadsideAssistanceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoadsideNavGraph(container = container)
                }
            }
        }
    }
}
