package org.openkeyboard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import org.openkeyboard.R
import org.openkeyboard.databinding.ActivityMainBinding
import org.openkeyboard.model.KeymapInfo
import org.openkeyboard.storage.KeymapRepository

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: KeymapRepository
    private lateinit var adapter: KeymapAdapter
    private var allKeymaps = listOf<KeymapInfo>()

    private val xmlPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val filename = getFileName(uri) ?: "custom_keymap.xml"
            val result = repository.importXmlFromUri(uri, filename)
            result.onSuccess { info ->
                Toast.makeText(this, getString(R.string.xml_import_success, info.displayName), Toast.LENGTH_LONG).show()
                refreshKeymapList()
                binding.chipFilterCustom.isChecked = true
            }.onFailure { err ->
                Toast.makeText(this, getString(R.string.xml_import_failed, err.localizedMessage), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = KeymapRepository.getInstance(this)

        setupButtons()
        setupFiltersAndSearch()
        setupRecyclerView()
        refreshKeymapList()
    }

    private fun setupButtons() {
        binding.btnEnableSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        }

        binding.btnSelectIme.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        binding.btnUploadXml.setOnClickListener {
            xmlPickerLauncher.launch("*/*")
        }

        binding.editTestSearchField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Toast.makeText(this, "🔍 Search executed: ${v.text}", Toast.LENGTH_SHORT).show()
                true
            } else false
        }
    }

    private fun setupFiltersAndSearch() {
        binding.editSearchKeymap.doAfterTextChanged {
            applyFilter()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            applyFilter()
        }
    }

    private fun setupRecyclerView() {
        adapter = KeymapAdapter(
            onSelected = { info ->
                repository.setActiveKeymapId(info.id)
                refreshKeymapList()
                Toast.makeText(this, "Active Keyboard: ${info.displayName}", Toast.LENGTH_SHORT).show()
            },
            onToggleEnabled = { info, enabled ->
                val success = repository.setKeymapEnabled(info.id, enabled)
                if (!success) {
                    Toast.makeText(this, "At least one layout must remain enabled on the keyboard", Toast.LENGTH_SHORT).show()
                }
                refreshKeymapList()
            },
            getActiveId = { repository.getActiveKeymapId() },
            isKeymapEnabled = { repository.isKeymapEnabled(it) }
        )
        binding.rvKeymaps.layoutManager = LinearLayoutManager(this)
        binding.rvKeymaps.adapter = adapter
    }

    private fun refreshKeymapList() {
        allKeymaps = repository.listAllKeymaps().sortedWith(Comparator { a, b ->
            when {
                a.id == "English" -> -1
                b.id == "English" -> 1
                a.id == "Tamil26ADN" -> -1
                b.id == "Tamil26ADN" -> 1
                a.id == "Tamil 26" -> -1
                b.id == "Tamil 26" -> 1
                else -> a.keyboard.compareTo(b.keyboard, ignoreCase = true)
            }
        })
        updateHeaderAndChips()
        applyFilter()
    }

    private fun updateHeaderAndChips() {
        val enabledIds = repository.getEnabledKeymapIds()
        val enabledCount = allKeymaps.count { enabledIds.contains(it.id) }
        val tamilCount = allKeymaps.count { it.language.equals("Tamil", ignoreCase = true) }
        val customCount = allKeymaps.count { !it.isBuiltIn }

        binding.txtEnabledCount.text = "Enabled on Keyboard: $enabledCount of ${allKeymaps.size}"
        binding.chipFilterEnabled.text = "Enabled ($enabledCount)"
        binding.chipFilterAll.text = "All XMLs (${allKeymaps.size})"
        binding.chipFilterTamil.text = "Tamil ($tamilCount)"
        binding.chipFilterCustom.text = "Custom ($customCount)"
    }

    private fun applyFilter() {
        val query = binding.editSearchKeymap.text?.toString()?.trim()?.lowercase() ?: ""
        val enabledIds = repository.getEnabledKeymapIds()

        val filteredByChip = when {
            binding.chipFilterEnabled.isChecked -> repository.getEnabledKeymaps()
            binding.chipFilterTamil.isChecked -> allKeymaps.filter { it.language.equals("Tamil", ignoreCase = true) }
            binding.chipFilterCustom.isChecked -> allKeymaps.filter { !it.isBuiltIn }
            else -> allKeymaps
        }

        val finalFiltered = if (query.isEmpty()) {
            filteredByChip
        } else {
            filteredByChip.filter {
                it.keyboard.lowercase().contains(query) ||
                it.language.lowercase().contains(query) ||
                it.author.lowercase().contains(query) ||
                it.filename.lowercase().contains(query)
            }
        }

        adapter.submitList(finalFiltered)
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    class KeymapAdapter(
        private val onSelected: (KeymapInfo) -> Unit,
        private val onToggleEnabled: (KeymapInfo, Boolean) -> Unit,
        private val getActiveId: () -> String,
        private val isKeymapEnabled: (String) -> Boolean
    ) : RecyclerView.Adapter<KeymapAdapter.ViewHolder>() {

        private var items = listOf<KeymapInfo>()

        fun submitList(newItems: List<KeymapInfo>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_keymap, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val isActive = item.id == getActiveId()
            val isEnabled = isKeymapEnabled(item.id)

            holder.radioSelected.isChecked = isActive
            holder.txtName.text = item.displayName
            holder.txtDetails.text = "${item.ruleCount} rules • Author: ${item.author}"
            holder.badgeSource.text = if (item.isBuiltIn) "BUILT-IN" else "CUSTOM"

            holder.badgeActive.visibility = if (isActive) View.VISIBLE else View.GONE

            // Avoid triggering listener during bind
            holder.switchEnabled.setOnCheckedChangeListener(null)
            holder.switchEnabled.isChecked = isEnabled
            holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggleEnabled(item, isChecked)
            }

            holder.itemView.setOnClickListener {
                onSelected(item)
            }

            holder.radioSelected.setOnClickListener {
                onSelected(item)
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val radioSelected: RadioButton = itemView.findViewById(R.id.radioSelected)
            val txtName: TextView = itemView.findViewById(R.id.txtKeymapName)
            val badgeActive: TextView = itemView.findViewById(R.id.badgeActive)
            val txtDetails: TextView = itemView.findViewById(R.id.txtKeymapDetails)
            val badgeSource: TextView = itemView.findViewById(R.id.badgeSource)
            val switchEnabled: MaterialSwitch = itemView.findViewById(R.id.switchEnabled)
        }
    }
}
