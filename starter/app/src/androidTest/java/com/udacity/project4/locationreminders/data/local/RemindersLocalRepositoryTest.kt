package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    //Add testing implementation to the RemindersLocalRepository.kt
    //Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setUp() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersLocalRepository =
            RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = runBlocking {
        //Setup: Create a reminder and insert to the database
        val title = "Sydney"
        val des = "Sydney town hall"
        val location = "Hall"
        val latLng = LatLng(-33.87365, 151.20689)
        val reminder = ReminderDTO(title, des, location, latLng.latitude, latLng.longitude)
        remindersLocalRepository.saveReminder(reminder)

        //Condition: Get the reminder by id from the database.
        val result = remindersLocalRepository.getReminder(reminder.id)

        //Verify: The result data contains the expected values.
        assertThat(result, `is`(notNullValue()))
        result as Result.Success
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminder_idNotFound() = runBlocking {
        //Condition: Get the reminder by id from the database.
        val result = remindersLocalRepository.getReminder("-1")
        result as Result.Error
        //Verify: return error message
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun deleteAllReminders_returnisEmpty() = runBlocking {
        //Setup: Create a reminder and insert to the database
        val title = "Sydney"
        val des = "Sydney town hall"
        val location = "Hall"
        val latLng = LatLng(-33.87365, 151.20689)
        val reminder = ReminderDTO(title, des, location, latLng.latitude, latLng.longitude)
        remindersLocalRepository.saveReminder(reminder)

        //Condition: Delete all reminder from the database.
        remindersLocalRepository.deleteAllReminders()

        //Verify: The result data is empty
        val result = remindersLocalRepository.getReminders()
        result as Result.Success
        assertThat(result.data, `is`(emptyList()))
    }
}