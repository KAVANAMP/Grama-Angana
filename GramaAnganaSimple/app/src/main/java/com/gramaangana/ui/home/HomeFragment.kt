package com.gramaangana.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramaangana.R
import com.gramaangana.data.FirebaseHelper
import com.gramaangana.databinding.FragmentHomeBinding
import com.gramaangana.databinding.ItemTodayEventBinding
import com.gramaangana.model.CommunityEvent
import com.gramaangana.ui.login.LoginActivity
import com.gramaangana.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _todayEvents = MutableStateFlow<List<CommunityEvent>>(emptyList())
    val todayEvents: StateFlow<List<CommunityEvent>> = _todayEvents

    private val _hallStatus = MutableStateFlow("Checking availability...")
    val hallStatus: StateFlow<String> = _hallStatus

    private val _statusColor = MutableStateFlow(R.color.status_free)
    val statusColor: StateFlow<Int> = _statusColor

    private val _openMaintenanceCount = MutableStateFlow(0)
    val openMaintenanceCount: StateFlow<Int> = _openMaintenanceCount

    fun loadData() {
        viewModelScope.launch {
            val today = DateUtils.getTodayString()
            _todayEvents.value = FirebaseHelper.getTodayEvents(today)

            val bookings = FirebaseHelper.getBookingsByDate(today)
            val approved = bookings.filter { it.status == "APPROVED" }
            when {
                approved.isNotEmpty() -> {
                    _hallStatus.value = "🔒 Hall is BOOKED — ${approved.first().purpose}"
                    _statusColor.value = R.color.status_booked
                }
                bookings.any { it.status == "PENDING" } -> {
                    _hallStatus.value = "⏳ Booking pending approval"
                    _statusColor.value = R.color.status_pending
                }
                else -> {
                    _hallStatus.value = "✅ Hall is FREE today!"
                    _statusColor.value = R.color.status_free
                }
            }
            val items = FirebaseHelper.getMaintenanceItems()
            _openMaintenanceCount.value = items.count { it.status == "OPEN" }
        }
    }
}

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var eventAdapter: TodayEventAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("GramaAnganaPrefs", 0)
        val userName = prefs.getString("user_name", "Guest") ?: "Guest"
        val userRole = prefs.getString("user_role", "USER") ?: "USER"

        eventAdapter = TodayEventAdapter()
        binding.rvTodayEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }

        binding.tvDate.text = DateUtils.formatDate(DateUtils.getTodayString())
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "🌅 Good Morning, $userName!"
            hour < 17 -> "☀️ Good Afternoon, $userName!"
            else -> "🌙 Good Evening, $userName!"
        }

        binding.tvUserRole.text = if (userRole == "ADMIN") "🏛️ Admin" else "👤 Member"
        binding.tvUserRole.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayEvents.collect { events ->
                if (events.isEmpty()) {
                    binding.tvNoEvents.visibility = View.VISIBLE
                    binding.rvTodayEvents.visibility = View.GONE
                } else {
                    binding.tvNoEvents.visibility = View.GONE
                    binding.rvTodayEvents.visibility = View.VISIBLE
                    eventAdapter.submitList(events)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hallStatus.collect { status ->
                binding.tvHallStatus.text = status
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statusColor.collect { colorRes ->
                binding.cardHallStatus.setCardBackgroundColor(
                    requireContext().getColor(colorRes)
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.openMaintenanceCount.collect { count ->
                binding.tvMaintenanceCount.text = "$count items need attention"
                binding.cardMaintenance.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
        }

        binding.btnBookHall.setOnClickListener { findNavController().navigate(R.id.action_home_to_calendar) }
        binding.btnViewCalendar.setOnClickListener { findNavController().navigate(R.id.action_home_to_calendar) }
        binding.btnViewMaintenance.setOnClickListener { findNavController().navigate(R.id.action_home_to_maintenance) }
        binding.cardHallStatus.setOnClickListener { findNavController().navigate(R.id.action_home_to_calendar) }
        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        viewModel.loadData()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes, Logout") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        requireActivity().getSharedPreferences("GramaAnganaPrefs", 0).edit().clear().apply()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

class TodayEventAdapter : ListAdapter<CommunityEvent, TodayEventAdapter.ViewHolder>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CommunityEvent>() {
            override fun areItemsTheSame(a: CommunityEvent, b: CommunityEvent) = a.id == b.id
            override fun areContentsTheSame(a: CommunityEvent, b: CommunityEvent) = a == b
        }
    }
    inner class ViewHolder(val binding: ItemTodayEventBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTodayEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position)
        holder.binding.apply {
            tvEventTitle.text = event.title
            tvEventTime.text = "${DateUtils.formatTime(event.startTime)} - ${DateUtils.formatTime(event.endTime)}"
            tvEventOrganizer.text = "By ${event.organizer}"
            tvEventType.text = event.eventType
        }
    }
}
