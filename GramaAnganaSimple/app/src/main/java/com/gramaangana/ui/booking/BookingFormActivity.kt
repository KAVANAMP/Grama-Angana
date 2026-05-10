package com.gramaangana.ui.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.gramaangana.databinding.ActivityBookingFormBinding
import com.gramaangana.data.FirebaseHelper
import com.gramaangana.model.Booking
import com.gramaangana.utils.DateUtils
import com.gramaangana.utils.ValidationUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class BookingFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingFormBinding
    private var selectedDate: String = ""
    private var startTime: String = ""
    private var endTime: String = ""

    private val eventTypes = listOf(
        "Wedding / Vivaha", "Gram Sabha / Meeting", "Sports / Training",
        "Health Camp", "Cultural Program", "Education / Workshop",
        "Religious Function", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Request Hall Booking"

        selectedDate = intent.getStringExtra("extra_date") ?: DateUtils.getTodayString()
        binding.tvSelectedDate.text = "📅 ${DateUtils.formatDate(selectedDate)}"

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eventTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = adapter

        binding.btnSelectDate.setOnClickListener { showDatePicker() }
        binding.btnStartTime.setOnClickListener { showTimePicker(true) }
        binding.btnEndTime.setOnClickListener { showTimePicker(false) }
        binding.btnSubmit.setOnClickListener { submitBooking() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
            binding.tvSelectedDate.text = "📅 ${DateUtils.formatDate(selectedDate)}"
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            .also { it.datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val time = "%02d:%02d".format(hour, minute)
            if (isStart) {
                startTime = time
                binding.btnStartTime.text = "🕐 ${DateUtils.formatTime(time)}"
            } else {
                endTime = time
                binding.btnEndTime.text = "🕑 ${DateUtils.formatTime(time)}"
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun submitBooking() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val purpose = binding.etPurpose.text.toString().trim()
        val attendees = binding.etAttendees.text.toString().toIntOrNull() ?: 0
        val eventType = eventTypes[binding.spinnerEventType.selectedItemPosition]
        val notes = binding.etNotes.text.toString().trim()

        if (!ValidationUtils.validateName(name)) {
            binding.tilName.error = "Please enter a valid name"; return
        }
        if (!ValidationUtils.validatePhone(phone)) {
            binding.tilPhone.error = "Please enter a valid 10-digit phone number"; return
        }
        if (purpose.isEmpty()) {
            binding.tilPurpose.error = "Please describe the purpose"; return
        }
        if (startTime.isEmpty() || endTime.isEmpty()) {
            Snackbar.make(binding.root, "Please select both start and end times", Snackbar.LENGTH_SHORT).show(); return
        }
        if (!ValidationUtils.validateTime(startTime, endTime)) {
            Snackbar.make(binding.root, "End time must be after start time", Snackbar.LENGTH_SHORT).show(); return
        }

        binding.tilName.error = null
        binding.tilPhone.error = null
        binding.tilPurpose.error = null

        val booking = Booking(
            requesterName = name, requesterPhone = phone,
            purpose = purpose, eventType = eventType,
            date = selectedDate, startTime = startTime,
            endTime = endTime, attendeeCount = attendees,
            notes = notes, status = "PENDING"
        )

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        lifecycleScope.launch {
            val result = FirebaseHelper.submitBooking(booking)
            binding.progressBar.visibility = View.GONE
            if (result.isSuccess) {
                Snackbar.make(binding.root, "✅ Booking request submitted!", Snackbar.LENGTH_LONG).show()
                finish()
            } else {
                binding.btnSubmit.isEnabled = true
                Snackbar.make(binding.root, "❌ ${result.exceptionOrNull()?.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
