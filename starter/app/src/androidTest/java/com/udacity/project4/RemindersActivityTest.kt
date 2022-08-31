package com.udacity.project4

import android.app.Application
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    var permissions = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun reminder_displayDetail() = runBlocking {
        //Setup: Create a reminder and insert to the database
        val title = "Sydney"
        val des = "Sydney town hall"
        val location = "Hall"
        val latLng = LatLng(-33.87365, 151.20689)
        repository.saveReminder(
            ReminderDTO(
                title,
                des,
                location,
                latLng.latitude,
                latLng.longitude
            )
        )

        //Startup the list reminder screen with scenario.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        Thread.sleep(3000)

        //Execute: Click on the reminder on the list and verify that all the data is correct.
        onView(withText(title)).perform(click())

        //Verify: The detail of reminder on screen
        onView(withId(R.id.text_title)).check(matches(withText(title)))
        onView(withId(R.id.text_description)).check(matches(withText(des)))
        onView(withId(R.id.text_location)).check(matches(withText(location)))

        //Close scenario
        activityScenario.close()
    }

    @Test
    fun saveReminder_isSuccess() {
        //Setup: Create a reminder and insert to the database
        val title = "Sydney"
        val des = "Sydney town hall"
        //Startup list reminder screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //Execute: Click add reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText(title))
        onView(isRoot()).perform(closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(typeText(des))
        onView(isRoot()).perform(closeSoftKeyboard())

        //When openMap
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.google_map)).perform(longClick())

        //save location from map
        onView(withId(R.id.save_btn)).perform(click())

        //Click save reminder button
        onView(withId(R.id.saveReminder)).perform(click())

        // verify toast message
        var decorView: View? = null
        activityScenario.onActivity { decorView = it.window.decorView }
        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(decorView)))
            .check(matches(isDisplayed()))

        //Close Scenario
        activityScenario.close()
    }

    @Test
    fun saveReminder_locationisEmpty_getErrorMessage() {
        //Setup: Create a reminder and insert to the database
        val title = "Sydney"
        val des = "Sydney town hall"
        // Start up List reminder screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //Execute: Click add reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        //Add reminder title
        onView(withId(R.id.reminderTitle)).perform(typeText(title))
        onView(isRoot()).perform(closeSoftKeyboard())
        //Add reminder description
        onView(withId(R.id.reminderDescription)).perform(typeText(des))
        onView(isRoot()).perform(closeSoftKeyboard())

        // Click save reminder button
        onView(withId(R.id.saveReminder)).perform(click())

        //Verify: Check message error
        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))

        //Close Scenario
        activityScenario.close()
    }

    @Test
    fun saveReminder_titleisEmpty_getErrorMessage() {
        // Startup list reminder screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //Execute: Click add reminder
        onView(withId(R.id.addReminderFAB)).perform(click())

        // openMap
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.google_map)).perform(longClick())
        // save location from map
        onView(withId(R.id.save_btn)).perform(click())

        // Click Save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        //Verify: Check message error
        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))

        //Close Scenario
        activityScenario.close()
    }
}
