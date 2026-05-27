package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AfriRentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AfriRent", appName)
  }

  @Test
  fun `launch main activity`() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      assertNotNull(scenario)
    }
  }

  @Test
  fun `test database seeding`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    
    val repository = AfriRentRepository(db)
    
    // Seed database
    repository.seedDatabaseIfEmpty()
    
    // Retrieve users and properties to verify seeding was successful
    val properties = repository.getAllProperties().first()
    val landlord = db.userDao().getUserById("landlord@afrirent.com")
    val tenant = db.userDao().getUserById("tenant@afrirent.com")
    val admin = db.userDao().getUserById("admin@afrirent.com")
    
    assertNotNull(landlord)
    assertEquals("Malam Ibrahim Gwoza", landlord?.fullName)
    assertNotNull(tenant)
    assertEquals("Grace Bitrus", tenant?.fullName)
    assertNotNull(admin)
    assertEquals("Amina Musa", admin?.fullName)
    
    assertTrue(properties.isNotEmpty())
    assertEquals(4, properties.size)
    
    db.close()
  }
}
