package com.gramaangana.ui.events

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramaangana.data.FirebaseHelper
import com.gramaangana.databinding.FragmentEventsBinding
import com.gramaangana.databinding.ItemEventBinding
import com.gramaangana.model.CommunityEvent
import com.gramaangana.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EventsViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<CommunityEvent>>(emptyList())
    val events: StateFlow<List<CommunityEvent>> = _events

    fun loadEvents() {
        viewModelScope.launch {
            _events.value = FirebaseHelper.getEvents()
        }
    }
}

class EventsFragment : Fragment() {
    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EventsViewModel by viewModels()
    private lateinit var adapter: EventsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EventsAdapter()
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@EventsFragment.adapter
        }

        binding.tvToday.text = "Today: ${DateUtils.formatDate(DateUtils.getTodayString())}"

        lifecycleScope.launch {
            viewModel.events.collect { events ->
                if (events.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvEvents.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvEvents.visibility = View.VISIBLE
                    adapter.submitList(events)
                }
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadEvents()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.loadEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class EventsAdapter : ListAdapter<CommunityEvent, EventsAdapter.ViewHolder>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CommunityEvent>() {
            override fun areItemsTheSame(a: CommunityEvent, b: CommunityEvent) = a.id == b.id
            override fun areContentsTheSame(a: CommunityEvent, b: CommunityEvent) = a == b
        }
    }
    inner class ViewHolder(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position)
        holder.binding.apply {
            tvEventTitle.text = event.title
            tvEventDate.text = "📅 ${DateUtils.formatDate(event.date)}"
            tvEventTime.text = "🕐 ${DateUtils.formatTime(event.startTime)} - ${DateUtils.formatTime(event.endTime)}"
            tvEventOrganizer.text = "👤 ${event.organizer}"
            tvEventDescription.text = event.description
            tvEventType.text = event.eventType
            tvContactPhone.text = "📞 ${event.contactPhone}"
            tvFeatured.visibility = if (event.isFeatured) View.VISIBLE else View.GONE
        }
    }
}
