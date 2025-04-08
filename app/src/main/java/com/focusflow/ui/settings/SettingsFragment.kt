package com.focusflow.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.focusflow.R
import com.focusflow.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTimerSettings()
        setupAudioSettings()
        setupAppearanceSettings()
        setupSaveButton()
        
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTimerSettings() {
        // Focus duration slider
        binding.sliderFocusDuration.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val minutes = value.toInt()
                binding.textViewFocusDurationValue.text = resources.getQuantityString(
                    R.plurals.minutes, minutes, minutes
                )
                viewModel.setFocusMinutes(minutes)
            }
        }
        
        // Short break slider
        binding.sliderShortBreak.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val minutes = value.toInt()
                binding.textViewShortBreakValue.text = resources.getQuantityString(
                    R.plurals.minutes, minutes, minutes
                )
                viewModel.setShortBreakMinutes(minutes)
            }
        }
        
        // Long break slider
        binding.sliderLongBreak.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val minutes = value.toInt()
                binding.textViewLongBreakValue.text = resources.getQuantityString(
                    R.plurals.minutes, minutes, minutes
                )
                viewModel.setLongBreakMinutes(minutes)
            }
        }
        
        // Sessions count slider
        binding.sliderSessionsCount.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val sessions = value.toInt()
                binding.textViewSessionsCountValue.text = resources.getQuantityString(
                    R.plurals.sessions, sessions, sessions
                )
                viewModel.setSessionsBeforeLongBreak(sessions)
            }
        }
    }

    private fun setupAudioSettings() {
        // Background audio switch
        binding.switchPlayBackgroundAudio.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setPlayBackgroundAudio(isChecked)
        }
        
        // Audio track spinner
        val audioTracks = arrayOf(
            getString(R.string.audio_track_binaural),
            getString(R.string.audio_track_nature),
            getString(R.string.audio_track_white_noise)
        )
        
        val audioAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            audioTracks
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        binding.spinnerAudioTrack.adapter = audioAdapter
        binding.spinnerAudioTrack.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTrack = when (position) {
                    0 -> "binaural"
                    1 -> "nature"
                    2 -> "white_noise"
                    else -> "binaural"
                }
                viewModel.setAudioTrack(selectedTrack)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        
        // Volume slider
        binding.sliderDefaultVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val volumePercent = value.toInt()
                binding.textViewVolumeValue.text = getString(R.string.percent_format, volumePercent)
                viewModel.setVolume(volumePercent / 100f)
            }
        }
    }

    private fun setupAppearanceSettings() {
        // Dark mode spinner
        val darkModeOptions = arrayOf(
            getString(R.string.dark_mode_system),
            getString(R.string.dark_mode_light),
            getString(R.string.dark_mode_dark)
        )
        
        val darkModeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            darkModeOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        binding.spinnerDarkMode.adapter = darkModeAdapter
        binding.spinnerDarkMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val darkModeSetting = when (position) {
                    0 -> "system"
                    1 -> "light"
                    2 -> "dark"
                    else -> "system"
                }
                viewModel.setDarkMode(darkModeSetting)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        
        // Tree type spinner
        val treeTypes = arrayOf(
            getString(R.string.tree_type_oak),
            getString(R.string.tree_type_pine),
            getString(R.string.tree_type_maple)
        )
        
        val treeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            treeTypes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        binding.spinnerTreeType.adapter = treeAdapter
        binding.spinnerTreeType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val treeType = when (position) {
                    0 -> "oak"
                    1 -> "pine"
                    2 -> "maple"
                    else -> "oak"
                }
                viewModel.setTreeType(treeType)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupSaveButton() {
        binding.buttonSaveSettings.setOnClickListener {
            viewModel.saveSettings()
            Snackbar.make(
                binding.root,
                R.string.settings_saved,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        viewModel.focusMinutes.observe(viewLifecycleOwner) { minutes ->
            binding.sliderFocusDuration.value = minutes.toFloat()
            binding.textViewFocusDurationValue.text = resources.getQuantityString(
                R.plurals.minutes, minutes, minutes
            )
        }
        
        viewModel.shortBreakMinutes.observe(viewLifecycleOwner) { minutes ->
            binding.sliderShortBreak.value = minutes.toFloat()
            binding.textViewShortBreakValue.text = resources.getQuantityString(
                R.plurals.minutes, minutes, minutes
            )
        }
        
        viewModel.longBreakMinutes.observe(viewLifecycleOwner) { minutes ->
            binding.sliderLongBreak.value = minutes.toFloat()
            binding.textViewLongBreakValue.text = resources.getQuantityString(
                R.plurals.minutes, minutes, minutes
            )
        }
        
        viewModel.sessionsBeforeLongBreak.observe(viewLifecycleOwner) { sessions ->
            binding.sliderSessionsCount.value = sessions.toFloat()
            binding.textViewSessionsCountValue.text = resources.getQuantityString(
                R.plurals.sessions, sessions, sessions
            )
        }
        
        viewModel.playBackgroundAudio.observe(viewLifecycleOwner) { play ->
            binding.switchPlayBackgroundAudio.isChecked = play
        }
        
        viewModel.audioTrack.observe(viewLifecycleOwner) { track ->
            val position = when (track) {
                "binaural" -> 0
                "nature" -> 1
                "white_noise" -> 2
                else -> 0
            }
            binding.spinnerAudioTrack.setSelection(position)
        }
        
        viewModel.volume.observe(viewLifecycleOwner) { volume ->
            val volumePercent = (volume * 100).toInt()
            binding.sliderDefaultVolume.value = volumePercent.toFloat()
            binding.textViewVolumeValue.text = getString(R.string.percent_format, volumePercent)
        }
        
        viewModel.darkMode.observe(viewLifecycleOwner) { mode ->
            val position = when (mode) {
                "system" -> 0
                "light" -> 1
                "dark" -> 2
                else -> 0
            }
            binding.spinnerDarkMode.setSelection(position)
        }
        
        viewModel.treeType.observe(viewLifecycleOwner) { type ->
            val position = when (type) {
                "oak" -> 0
                "pine" -> 1
                "maple" -> 2
                else -> 0
            }
            binding.spinnerTreeType.setSelection(position)
        }
    }
}
