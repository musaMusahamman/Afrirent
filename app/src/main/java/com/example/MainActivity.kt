package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AfriRentRepository
import com.example.ui.AfriRentApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AfriRentViewModel
import com.example.viewmodel.AfriValueFactory

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: AfriRentRepository
    private lateinit var viewModel: AfriRentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "afrirent_secure_db"
        ).fallbackToDestructiveMigration().build()

        // Initialize Repository & ViewModel Factory
        repository = AfriRentRepository(database)
        val factory = AfriValueFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AfriRentViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AfriRentApp(viewModel)
                }
            }
        }
    }
}
