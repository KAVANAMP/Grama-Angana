package com.gramaangana.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.view.*
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
import com.gramaangana.R
import com.gramaangana.data.FirebaseHelper
import com.gramaangana.databinding.FragmentCalendarBinding
import com.gramaangana.databinding.ItemBookingSlotBinding
import com.gramaangana.model.Booking
import com.gramaangana.ui.booking.BookingFormActivity
import com.gramaangana.utils.DateUtils
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarViewModel : ViewModel() {
    private val _allBookings = MutableStateFlow<List<Booking>>(emptyList())
    val allBookings: StateFlow<List<Booking>> = _allBookings

    private val _selectedDateBookings = MutableStateFlow<List<Booking>>(emptyList())
    val selectedDateBookings: StateFlow<List<Booking>> = _selectedDateBookings

    fun loadAllBookings() {
        viewModelScope.launch {
            _allBookings.value = FirebaseHelper.getBookings()
        }
    }

    fun loadBookingsForDate(date: String) {
        viewModelScope.launch {
            _selectedDateBookings.value = FirebaseHelper.getBookingsByDate(date)
        }
    }

    fun getBookedDates(): Set<String> =
        _allBookings.value.filter { it.status == "APPROVED" }.map { it.date }.toSet()

    fun getPendingDates(): Set<String> =
        _allBookings.value.filter { it.status == "PENDING" }.map { it.date }.toSet()
}

class CalendarFragment : Fragment() {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CalendarViewModel by viewModels()
    private var selectedDate: LocalDate? = null
    private lateinit var bookingAdapter: BookingSlotAdapter
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCalendar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        viewModel.loadAllBookings()
    }

    private fun setupCalendar() {
        val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY)
        val currentMonth = YearMonth.now()
        binding.calendarView.setup(
            currentMonth.minusMonths(1),
            currentMonth.plusMonths(6),
            daysOfWeek.first()
        )
        binding.calendarView.scrollToMonth(currentMonth)

        val daysViews = listOf(
            binding.legendLayout.tvSun, binding.legendLayout.tvMon,
            binding.legendLayout.tvTue, binding.legendLayout.tvWed,
            binding.legendLayout.tvThu, binding.legendLayout.tvFri,
            binding.legendLayout.tvSat
        )
        daysOfWeek.forEachIndexed { index, dayOfWeek ->
            daysViews[index].text =
                dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.binding.tvDay
                textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    textView.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_primary)
                    )
                    val dateStr = data.date.format(formatter)
                    container.binding.root.background = when {
                        data.date == selectedDate ->
                            ContextCompat.getDrawable(requireContext(), R.drawable.bg_day_selected)
                        data.date == LocalDate.now() ->
                            ContextCompat.getDrawable(requireContext(), R.drawable.bg_day_today)
                        else -> null
                    }
                    container.binding.dotBooked.visibility =
                        if (viewModel.getBookedDates().contains(dateStr)) View.VISIBLE else View.GONE
                    container.binding.dotPending.visibility =
                        if (viewModel.getPendingDates().contains(dateStr)) View.VISIBLE else View.GONE

                    container.view.setOnClickListener {
                        val old = selectedDate
                        selectedDate = data.date
                        old?.let { binding.calendarView.notifyDateChanged(it) }
                        binding.calendarView.notifyDateChanged(data.date)
                        binding.tvSelectedDate.text = "📅 ${
                            data.date.format(
                                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                            )
                        }"
                        binding.tvSelectPrompt.visibility = View.GONE
                        binding.cardDateDetails.visibility = View.VISIBLE
                        binding.fabBook.show()
                        viewModel.loadBookingsForDate(dateStr)
                    }
                } else {
                    textView.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_disabled)
                    )
                    container.binding.dotBooked.visibility = View.GONE
                    container.binding.dotPending.visibility = View.GONE
                    container.view.setOnClickListener(null)
                }
            }
        }

        binding.calendarView.monthScrollListener = { month ->
            binding.tvMonthYear.text =
                month.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingSlotAdapter()
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.allBookings.collect {
                binding.calendarView.notifyCalendarChanged()
            }
        }
        lifecycleScope.launch {
            viewModel.selectedDateBookings.collect { bookings ->
                if (bookings.isEmpty()) {
                    binding.tvNoBookings.visibility = View.VISIBLE
                    binding.rvBookings.visibility = View.GONE
                } else {
                    binding.tvNoBookings.visibility = View.GONE
                    binding.rvBookings.visibility = View.VISIBLE
                    bookingAdapter.submitList(bookings)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabBook.setOnClickListener {
            val dateStr = selectedDate?.format(formatter) ?: return@setOnClickListener
            val intent = Intent(requireContext(), BookingFormActivity::class.java)
            intent.putExtra("extra_date", dateStr)
            startActivity(intent)
        }
        binding.btnPrevMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1))
            }
        }
        binding.btnNextMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DayViewContainer(view: View) : ViewContainer(view) {
    lateinit var day: CalendarDay
    val binding = com.gramaangana.databinding.ItemCalendarDayBinding.bind(view)
}

class BookingSlotAdapter : ListAdapter<Booking, BookingSlotAdapter.ViewHolder>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Booking>() {
            override fun areItemsTheSame(a: Booking, b: Booking) = a.id == b.id
            override fun areContentsTheSame(a: Booking, b: Booking) = a == b
        }
    }

    inner class ViewHolder(val binding: ItemBookingSlotBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemBookingSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = getItem(position)
        val ctx = holder.itemView.context
        holder.binding.apply {
            tvBookingName.text = booking.requesterName
            tvBookingPurpose.text = booking.purpose
            tvBookingTime.text =
                "${DateUtils.formatTime(booking.startTime)} → ${DateUtils.formatTime(booking.endTime)}"
            tvBookingType.text = booking.eventType
            tvAttendees.text = "${booking.attendeeCount} attendees"
            tvStatus.text = when (booking.status) {
                "APPROVED" -> "✅ Approved"
                "PENDING" -> "⏳ Pending"
                "REJECTED" -> "❌ Rejected"
                else -> booking.status
            }
            val colorRes = when (booking.status) {
                "APPROVED" -> R.color.status_free
                "PENDING" -> R.color.status_pending
                else -> R.color.status_booked
            }
            tvStatus.setTextColor(ContextCompat.getColor(ctx, colorRes))
            if (booking.keyHolder.isNotEmpty()) {
                tvKeyHolder.text = "🗝 Key Holder: ${booking.keyHolder}"
                tvKeyHolder.visibility = View.VISIBLE
            } else {
                tvKeyHolder.visibility = View.GONE
            }
        }
    }
}
