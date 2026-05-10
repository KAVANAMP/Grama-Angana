package com.gramaangana.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.gramaangana.R
import com.gramaangana.data.FirebaseHelper
import com.gramaangana.databinding.FragmentAdminBookingsBinding
import com.gramaangana.databinding.ItemAdminBookingBinding
import com.gramaangana.model.Booking
import com.gramaangana.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── ViewModel ─────────────────────────────────────────────────
class AdminViewModel : ViewModel() {
    private val _pendingBookings = MutableStateFlow<List<Booking>>(emptyList())
    val pendingBookings: StateFlow<List<Booking>> = _pendingBookings

    private val _allBookings = MutableStateFlow<List<Booking>>(emptyList())
    val allBookings: StateFlow<List<Booking>> = _allBookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadBookings() {
        viewModelScope.launch {
            _isLoading.value = true
            val bookings = FirebaseHelper.getBookings()
            _allBookings.value = bookings
            _pendingBookings.value = bookings.filter { it.status == "PENDING" }
            _isLoading.value = false
        }
    }

    fun updateBookingStatus(
        bookingId: String,
        status: String,
        keyHolder: String = "",
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val updates = mutableMapOf<String, Any>("status" to status)
                if (keyHolder.isNotEmpty()) updates["keyHolder"] = keyHolder
                db.collection("bookings").document(bookingId).update(updates).await()
                loadBookings()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}

// ─── Fragment ───────────────────────────────────────────────────
class AdminBookingsFragment : Fragment() {
    private var _binding: FragmentAdminBookingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var adapter: AdminBookingAdapter
    private var showPendingOnly = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Single adapter for both tabs
        adapter = AdminBookingAdapter(
            onApprove = { showApproveDialog(it) },
            onReject = { showRejectDialog(it) }
        )

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AdminBookingsFragment.adapter
        }

        setupTabButtons()
        setupObservers()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadBookings()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.loadBookings()
    }

    private fun setupTabButtons() {
        // Pending tab - default selected
        updateTabUI(isPendingSelected = true)

        binding.btnPending.setOnClickListener {
            showPendingOnly = true
            updateTabUI(isPendingSelected = true)
            // Show pending bookings
            val pending = viewModel.pendingBookings.value
            adapter.submitList(pending)
            updateEmptyState(pending.isEmpty())
        }

        binding.btnAll.setOnClickListener {
            showPendingOnly = false
            updateTabUI(isPendingSelected = false)
            // Show all bookings
            val all = viewModel.allBookings.value
            adapter.submitList(all)
            updateEmptyState(all.isEmpty())
        }
    }

    private fun updateTabUI(isPendingSelected: Boolean) {
        if (isPendingSelected) {
            binding.btnPending.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.status_pending)
            )
            binding.btnPending.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )
            binding.btnAll.setBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.transparent)
            )
            binding.btnAll.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
        } else {
            binding.btnAll.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
            binding.btnAll.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )
            binding.btnPending.setBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.transparent)
            )
            binding.btnPending.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.status_pending)
            )
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingBookings.collect { bookings ->
                binding.tvPendingCount.text = "${bookings.size} pending requests"
                // Update list if pending tab is active
                if (showPendingOnly) {
                    adapter.submitList(bookings)
                    updateEmptyState(bookings.isEmpty())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allBookings.collect { bookings ->
                // Update list if all tab is active
                if (!showPendingOnly) {
                    adapter.submitList(bookings)
                    updateEmptyState(bookings.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvBookings.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showApproveDialog(booking: Booking) {
        val input = android.widget.EditText(requireContext())
        input.hint = "Key Holder Name (e.g., Sarpanch)"
        input.setPadding(48, 24, 48, 24)

        AlertDialog.Builder(requireContext())
            .setTitle("✅ Approve Booking")
            .setMessage(
                "Approve for: ${booking.requesterName}\n" +
                "📅 ${DateUtils.formatDate(booking.date)}\n" +
                "🕐 ${DateUtils.formatTime(booking.startTime)} - ${DateUtils.formatTime(booking.endTime)}"
            )
            .setView(input)
            .setPositiveButton("Approve") { _, _ ->
                val keyHolder = input.text.toString().trim()
                viewModel.updateBookingStatus(booking.id, "APPROVED", keyHolder) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "✅ Booking Approved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "❌ Failed to approve", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectDialog(booking: Booking) {
        AlertDialog.Builder(requireContext())
            .setTitle("❌ Reject Booking")
            .setMessage("Reject booking for ${booking.requesterName}?\nThis cannot be undone.")
            .setPositiveButton("Reject") { _, _ ->
                viewModel.updateBookingStatus(booking.id, "REJECTED") { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "❌ Booking Rejected", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to reject", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─── Adapter ────────────────────────────────────────────────────
class AdminBookingAdapter(
    private val onApprove: (Booking) -> Unit,
    private val onReject: (Booking) -> Unit
) : ListAdapter<Booking, AdminBookingAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Booking>() {
            override fun areItemsTheSame(a: Booking, b: Booking) = a.id == b.id
            override fun areContentsTheSame(a: Booking, b: Booking) = a == b
        }
    }

    inner class ViewHolder(val binding: ItemAdminBookingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAdminBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = getItem(position)
        val ctx = holder.itemView.context
        holder.binding.apply {
            tvRequesterName.text = "👤 ${booking.requesterName}"
            tvPhone.text = "📞 ${booking.requesterPhone}"
            tvPurpose.text = booking.purpose
            tvEventType.text = booking.eventType
            tvDate.text = "📅 ${DateUtils.formatDate(booking.date)}"
            tvTime.text = "🕐 ${DateUtils.formatTime(booking.startTime)} - ${DateUtils.formatTime(booking.endTime)}"
            tvAttendees.text = "👥 ${booking.attendeeCount} attendees"

            tvNotes.visibility = if (booking.notes.isNotEmpty()) View.VISIBLE else View.GONE
            tvNotes.text = "📝 ${booking.notes}"

            tvStatus.text = when (booking.status) {
                "APPROVED" -> "✅ Approved"
                "PENDING" -> "⏳ Pending"
                "REJECTED" -> "❌ Rejected"
                else -> booking.status
            }
            tvStatus.setTextColor(
                ContextCompat.getColor(ctx, when (booking.status) {
                    "APPROVED" -> R.color.status_free
                    "PENDING" -> R.color.status_pending
                    else -> R.color.status_booked
                })
            )

            // Show approve/reject only for PENDING
            layoutActions.visibility =
                if (booking.status == "PENDING") View.VISIBLE else View.GONE
            btnApprove.setOnClickListener { onApprove(booking) }
            btnReject.setOnClickListener { onReject(booking) }

            tvKeyHolder.visibility =
                if (booking.keyHolder.isNotEmpty()) View.VISIBLE else View.GONE
            tvKeyHolder.text = "🗝 Key Holder: ${booking.keyHolder}"
        }
    }
}