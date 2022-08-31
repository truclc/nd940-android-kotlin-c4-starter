package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase

    @Before
    fun setUp() {
        // Using an in-memory database so that the information stored here disappears when the process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    //Close the database
    fun closeDatabase() = database.close()

    @Test
    fun insertReminder_getById() = runBlockingTest {
        //Setup: Create a reminder and insert to the database
        val title = "Sydney"
        val des = "Sydney town hall"
        val location = "Hall"
        val latLng = LatLng(-33.87365, 151.20689)
        val reminder = ReminderDTO(title, des, location, latLng.latitude, latLng.longitude)
        database.reminderDao().saveReminder(reminder)

        //Execute: Get the reminder by id from the database.
        val result = database.reminderDao().getReminderById(reminder.id)

        //Verify: The result data contains the expected values.
        assertThat<ReminderDTO>(result as ReminderDTO, notNullValue())
        assertThat(result.id, `is`(reminder.id))
        assertThat(result.title, `is`(reminder.title))
        assertThat(result.description, `is`(reminder.description))
        assertThat(result.location, `is`(reminder.location))
        assertThat(result.latitude, `is`(reminder.latitude))
        assertThat(result.longitude, `is`(reminder.longitude))
    }

    @Test
    fun deleteAll() = runBlockingTest {
        //Setup: Create reminders and insert to the database
        val title = "Sydney"
        val des = "Sydney town hall"
        val location = "Hall"
        val latLng = LatLng(-33.87365, 151.20689)
        val reminder = ReminderDTO(title, des, location, latLng.latitude, latLng.longitude)
        val reminder1 =
            ReminderDTO(title + " 1", des + "1", location + " 1", latLng.latitude, latLng.longitude)
        val reminder2 =
            ReminderDTO(title + " 2", des + "2", location + " 2", latLng.latitude, latLng.longitude)
        val reminder3 =
            ReminderDTO(title + " 3", des + "3", location + " 3", latLng.latitude, latLng.longitude)
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        //Execute: Delete all the reminders from the database.
        database.reminderDao().deleteAllReminders()

        //Verify: The result data is empty
        val result = database.reminderDao().getReminders()
        assertThat(result, `is`(emptyList()))
    }
}