package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import org.hamcrest.MatcherAssert.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
//@Config(sdk = [28])
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the view model.
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
        stopKoin()
        reminderDataSource = FakeDataSource(remindersDummy)
        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    /**
     * Test when listview model loading a reminder.
     * Verify that the progress indicator is hidden.
     */
    @Test
    fun loadReminders_loading() = runBlockingTest {
        //Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        //Execute: Load reminder
        remindersListViewModel.loadReminders()

        //Verify
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        //Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()
        //Verify assert that the progress indicator is hidden.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    /**
     * Test when listview model loading a reminder.
     * Verify that reminders list is not null.
     */
    @Test
    fun loadReminders_success() = runBlockingTest {
        //Condition: Load reminder
        remindersListViewModel.loadReminders()

        val remindersList = remindersListViewModel.remindersList.getOrAwaitValue()
        //Verify: reminders list is not null
        assertThat(remindersList, `is`(notNullValue()))
    }

    /**
     * Test when listview model loading a reminder but get a error.
     * Verify that a message "Test getReminders exception" is displayed and show no data.
     */
    @Test
    fun loadReminders_error() = runBlockingTest {
        //Setup: return error
        reminderDataSource.setReturnError(true)

        //Execute
        remindersListViewModel.loadReminders()

        val snackBarMessage = remindersListViewModel.showSnackBar.getOrAwaitValue()
        val showNoData = remindersListViewModel.showNoData.getOrAwaitValue()

        //Verify
        assertThat(snackBarMessage, `is`("Test getReminders exception"))
        assertThat(showNoData, `is`(true))
    }

    /**
     * Test when listview model loading a reminder.
     * Verify that listview model show no data cause list size is zero.
     */
    @Test
    fun loadReminders_resultSuccess_noReminders() = runBlockingTest {
        //Setup: delete all reminder
        reminderDataSource.deleteAllReminders()

        //Execute: Load reminder
        remindersListViewModel.loadReminders()

        //Verify: size of reminder listview model is zero
        val loadedItems = remindersListViewModel.remindersList.getOrAwaitValue()
        assertThat(loadedItems.size, `is`(0))
        //Reminder showNoData is true
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }
}