package com.gramaangana.ui.maintenance

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.gramaangana.data.FirebaseHelper
import com.gramaangana.databinding.*
import com.gramaangana.model.MaintenanceItem
import com.gramaangana.model.Pledge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ─── ViewModel ─────────────────────────────────────────────────
class MaintenanceViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<MaintenanceItem>>(emptyList())
    val items: StateFlow<List<MaintenanceItem>> = _items

    fun loadItems() {
        viewModelScope.launch {
            _items.value = FirebaseHelper.getMaintenanceItems()
        }
    }
}

// ─── Fragment ───────────────────────────────────────────────────
class MaintenanceFragment : Fragment() {
    private var _binding: FragmentMaintenanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MaintenanceViewModel by viewModels()
    private lateinit var adapter: MaintenanceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMaintenanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MaintenanceAdapter { item -> showPledgeDialog(item) }
        binding.rvMaintenanceItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MaintenanceFragment.adapter
        }

        lifecycleScope.launch {
            viewModel.items.collect { items ->
                val totalNeeded = items.sumOf { it.amountNeeded }
                val totalRaised = items.sumOf { it.amountRaised }
                binding.tvTotalItems.text = "${items.size} items"
                binding.tvTotalFunds.text = "₹${totalRaised.toInt()} / ₹${totalNeeded.toInt()} raised"
                val progress = if (totalNeeded > 0) ((totalRaised / totalNeeded) * 100).toInt() else 0
                binding.progressOverall.progress = progress
                binding.tvOverallPercent.text = "$progress% of total funds raised"

                if (items.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvMaintenanceItems.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvMaintenanceItems.visibility = View.VISIBLE
                    adapter.submitList(items)
                }
            }
        }

        binding.fabAddItem.setOnClickListener {
            startActivity(Intent(requireContext(), AddMaintenanceActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadItems()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.loadItems()
    }

    private fun showPledgeDialog(item: MaintenanceItem) {
        PledgeBottomSheet.newInstance(item.id, item.title, item.amountNeeded - item.amountRaised)
            .show(childFragmentManager, "pledge")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─── Adapter ────────────────────────────────────────────────────
class MaintenanceAdapter(
    private val onPledgeClick: (MaintenanceItem) -> Unit
) : ListAdapter<MaintenanceItem, MaintenanceAdapter.ViewHolder>(DIFF) {
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MaintenanceItem>() {
            override fun areItemsTheSame(a: MaintenanceItem, b: MaintenanceItem) = a.id == b.id
            override fun areContentsTheSame(a: MaintenanceItem, b: MaintenanceItem) = a == b
        }
    }
    inner class ViewHolder(val binding: ItemMaintenanceBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemMaintenanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            tvItemTitle.text = item.title
            tvItemDescription.text = item.description
            tvCategory.text = item.category
            tvAmountNeeded.text = "₹${item.amountNeeded.toInt()} needed"
            tvAmountRaised.text = "₹${item.amountRaised.toInt()} raised"
            val progress = if (item.amountNeeded > 0)
                ((item.amountRaised / item.amountNeeded) * 100).toInt() else 0
            progressBar.progress = progress
            tvProgressPercent.text = "$progress%"
            val isCompleted = item.status == "COMPLETED"
            btnPledge.isEnabled = !isCompleted
            btnPledge.text = if (isCompleted) "✅ Funded!" else "💚 Pledge Support"
            btnPledge.setOnClickListener { onPledgeClick(item) }
            tvPledgeCount.text = ""
        }
    }
}

// ─── Pledge BottomSheet ─────────────────────────────────────────
class PledgeBottomSheet : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(itemId: String, itemTitle: String, remaining: Double) =
            PledgeBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("itemId", itemId)
                    putString("itemTitle", itemTitle)
                    putDouble("remaining", remaining)
                }
            }
    }

    private var _binding: BottomSheetPledgeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPledgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val itemId = arguments?.getString("itemId") ?: return
        binding.tvItemTitle.text = arguments?.getString("itemTitle") ?: ""
        binding.tvRemainingAmount.text = "₹${(arguments?.getDouble("remaining") ?: 0.0).toInt()} still needed"

        binding.btnConfirmPledge.setOnClickListener {
            val name = binding.etPledgerName.text.toString().trim()
            val amount = binding.etAmount.text.toString().toDoubleOrNull()
            val message = binding.etMessage.text.toString().trim()

            if (name.isEmpty()) { binding.etPledgerName.error = "Required"; return@setOnClickListener }
            if (amount == null || amount <= 0) { binding.etAmount.error = "Enter valid amount"; return@setOnClickListener }

            binding.btnConfirmPledge.isEnabled = false
            lifecycleScope.launch {
                val result = FirebaseHelper.addPledge(itemId, Pledge(name, amount, message))
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "🎉 Thank you for pledging ₹${amount.toInt()}!", Toast.LENGTH_LONG).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed. Try again.", Toast.LENGTH_SHORT).show()
                    binding.btnConfirmPledge.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─── Add Maintenance Activity ───────────────────────────────────
class AddMaintenanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMaintenanceBinding

    private val categories = listOf("ELECTRICAL", "PLUMBING", "FURNITURE", "CLEANING", "STRUCTURE", "OTHER")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMaintenanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Maintenance Item"

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val amount = binding.etAmount.text.toString().toDoubleOrNull()
            val category = categories[binding.spinnerCategory.selectedItemPosition]

            if (title.isEmpty()) { binding.tilTitle.error = "Required"; return@setOnClickListener }
            if (amount == null || amount <= 0) { binding.tilAmount.error = "Enter valid amount"; return@setOnClickListener }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmit.isEnabled = false

            lifecycleScope.launch {
                val item = com.gramaangana.model.MaintenanceItem(
                    title = title, description = description,
                    amountNeeded = amount, category = category
                )
                val result = FirebaseHelper.addMaintenanceItem(item)
                binding.progressBar.visibility = View.GONE
                if (result.isSuccess) {
                    Snackbar.make(binding.root, "✅ Item added!", Snackbar.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.btnSubmit.isEnabled = true
                    Snackbar.make(binding.root, "Failed. Try again.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { onBackPressed(); return true }
        return super.onOptionsItemSelected(item)
    }
}
