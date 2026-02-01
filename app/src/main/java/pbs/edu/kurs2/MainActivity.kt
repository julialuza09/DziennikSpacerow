package pbs.edu.kurs2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pbs.edu.kurs2.navigation.WalkNavigation
import pbs.edu.kurs2.ui.theme.Kurs2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Kurs2Theme { WalkNavigation() }
        }
    }
}
