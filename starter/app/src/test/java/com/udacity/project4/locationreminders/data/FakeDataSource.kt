package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var remindersList: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

    //Create a fake data source to act as a double to the real data source

    // shouldReturnError default value
    private var shouldReturnError = false

    /**
     * Set return error when loading reminder from the data source.
     */
    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    /**
     * Get a reminders.
     */
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        // If shouldReturnError is true, the return result is an error for the case test get reminders exception
        if (shouldReturnError) {
            return Result.Error("Test getReminders exception")
        }
        remindersList?.let { return Result.Success(ArrayList(it)) }
        return Result.Error("Reminders not found")
    }

    /**
     * Add a reminder to data source.
     */
    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList?.add(reminder)
    }

    /**
     * Get a reminder by id.
     */
    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test getReminder exception")
        }
        val reminder = remindersList?.find { it.id == id }
        return if (reminder == null) {
            Result.Error("Could not find reminder")
        } else {
            Result.Success(reminder)
        }
    }

    /**
     * Delete all reminders
     */
    override suspend fun deleteAllReminders() {
        remindersList?.clear()
    }
}