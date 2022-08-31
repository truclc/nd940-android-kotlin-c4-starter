package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import org.hamcrest.MatcherAssert.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    //Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    //Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    //Use a fake repository to be injected into the view model.
    private lateinit var reminderDataSource: FakeDataSource

    //Setup: Create a dummy reminder
    val title = "Sydney"
    val des = "Sydney town hall"
    val location = "Hall"
    val latLng = LatLng(-33.87365, 151.20689)
    private var remindersDummy = mutableListOf<ReminderDTO>(
        ReminderDTO(title, des, location, latLng.latitude, latLng.longitude),
        ReminderDTO(title + " 1", des + " 1", location + " 1", latLng.latitude, latLng.longitude),
        ReminderDTO(title + " 2", des + " 2", location + " 2", latLng.latitude, latLng.longitude)
    )

    @Before
    fun setupViewModel() {
        //stop the original app koin
        stopKoin()
        reminderDataSource = FakeDataSource(remindersDummy)
        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    @Test
    fun saveReminder_showLoading() = runBlockingTest {
        //Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        //Execute: Save reminder
        val reminderData = ReminderDataItem(
            title,
            des, location, latLng.latitude, latLng.longitude
        )
        saveReminderViewModel.saveReminder(reminderData)
        // THEN
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun saveReminder_success() = runBlockingTest {
        //Setup: Create a reminder
        val reminderData = ReminderDataItem(
            title,
            des, location, latLng.latitude, latLng.longitude
        )
        //Execute: save reminder
        saveReminderViewModel.saveReminder(reminderData)
        val remindersList = (reminderDataSource.getReminders() as Result.Success).data
        val item = remindersList.last()

        //Verify
        assertThat(item.title, `is`(reminderData.title))
        assertThat(item.description, `is`(reminderData.description))
        assertThat(item.location, `is`(reminderData.location))
    }

    @Test
    fun validateEnteredData_titleisEmpty_returnFalse() = runBlockingTest {
        //Setup: Create a reminder with empty title
        val reminderData = ReminderDataItem(
            "",
            des, location, latLng.latitude, latLng.longitude
        )
        //Execute: validate entered data of reminder
        val result = saveReminderViewModel.validateEnteredData(reminderData)
        val snackBarMessage = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()

        //Verify
        assertThat(result, `is`(false))
        assertThat(snackBarMessage, `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_validData_returnsTrue() = runBlockingTest {
        //Setup: Create a reminder
        val reminderData = ReminderDataItem(
            title,
            des, location, latLng.latitude, latLng.longitude
        )
        //Execute: validate entered aata of reminder
        val result = saveReminderViewModel.validateEnteredData(reminderData)
        //Verify
        assertThat(result, `is`(true))
    }

    @Test
    fun onClear_resetAllData() = runBlockingTest {
        //Execute: clear data
        saveReminderViewModel.onClear()

        //Verify
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(
            saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(),
            `is`(nullValue())
        )
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
    }
}