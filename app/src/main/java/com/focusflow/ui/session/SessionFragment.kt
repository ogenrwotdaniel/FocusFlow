package com.focusflow.ui.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.focusflow.R
import com.focusflow.data.model.AudioTracks
import com.focusflow.databinding.FragmentSessionBinding
import com.focusflow.domain.model.SessionType
import com.focusflow.service.audio.AudioService
import com.focusflow.service.timer.SessionType as TimerSessionType
import com.focusflow.service.timer.TimerInfo
import com.focusflow.service.timer.TimerService
import com.focusflow.service.timer.TimerState
import com.focusflow.ui.dialog.AudioTrackDialogHelper
import com.focusflow.ui.dialog.VolumeDialogHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SessionFragment : Fragment() {

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels()
    
    @Inject
    lateinit var volumeDialogHelper: VolumeDialogHelper
    
    private val audioTrackDialogHelper = AudioTrackDialogHelper()

    private var timerService: TimerService? = null
    private var audioService: AudioService? = null
    private var timerBound = false
    private var audioBound = false
    
    // Store the session ID to track tree growth
    private var currentSessionId: Long? = null

    private val timerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            timerBound = true
            
            timerService?.timerInfo?.observe(viewLifecycleOwner) { timerInfo ->
                updateTimerUI(timerInfo)
                // Update tree growth based on timer progress
                viewModel.updateTimerState(timerInfo)
                
                // Start tree growth when a focus session begins
                if (timerInfo.sessionType == TimerSessionType.FOCUS && 
                    currentSessionId == null &&
                    timerInfo.state == TimerState.RUNNING) {
                    currentSessionId = timerInfo.sessionId
                    viewModel.startTreeGrowth(timerInfo.sessionId)
                }
                
                // Complete tree when a focus session finishes
                if (timerInfo.sessionType == TimerSessionType.FOCUS && 
                    timerInfo.state == TimerState.COMPLETED &&
                    currentSessionId == timerInfo.sessionId) {
                    viewModel.completeTree()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            timerBound = false
        }
    }

    private val audioConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioBinder
            audioService = binder.getService()
            audioBound = true
            
            // Update audio service with current volume from preferences
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.volume.collectLatest { volume ->
                        audioService?.setVolume(volume)
                    }
                }
            }
            
            // Update audio service with current track from preferences
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.selectedAudioTrack.collectLatest { track ->
                        track?.let {
                            audioService?.setAudioTrack(it.resourceId)
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            audioBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        // Bind to TimerService
        Intent(requireContext(), TimerService::class.java).also { intent ->
            requireActivity().bindService(intent, timerConnection, Context.BIND_AUTO_CREATE)
        }
        
        // Bind to AudioService
        Intent(requireContext(), AudioService::class.java).also { intent ->
            requireActivity().bindService(intent, audioConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind from services
        if (timerBound) {
            requireActivity().unbindService(timerConnection)
            timerBound = false
        }
        
        if (audioBound) {
            requireActivity().unbindService(audioConnection)
            audioBound = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClickListeners() {
        binding.buttonPauseResume.setOnClickListener {
            if (timerService?.timerInfo?.value?.state == TimerState.RUNNING) {
                pauseSession()
            } else {
                resumeSession()
            }
        }
        
        binding.buttonStop.setOnClickListener {
            showStopConfirmationDialog()
        }
        
        binding.buttonVolume.setOnClickListener {
            showVolumeDialog()
        }
        
        binding.buttonAudioTrack.setOnClickListener {
            showAudioTrackDialog()
        }
    }

    private fun observeViewModel() {
        // Observe motivational messages
        viewModel.motivationalMessage.observe(viewLifecycleOwner) { message ->
            binding.textViewMotivation.text = message
        }
        
        // Observe tree growth image
        viewModel.currentTreeImage.observe(viewLifecycleOwner) { treeImageResId ->
            treeImageResId?.let {
                binding.imageViewTree.setImageResource(it)
            }
        }
        
        // Observe volume changes
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.volume.collectLatest { volume ->
                    audioService?.setVolume(volume)
                }
            }
        }
    }

    private fun updateTimerUI(timerInfo: TimerInfo) {
        binding.textViewTimer.text = formatTime(timerInfo.remainingTimeMs)
        binding.progressTimer.progress = timerInfo.progressPercent.toInt()
        
        // Update session type title
        when (timerInfo.sessionType) {
            TimerSessionType.FOCUS -> {
                binding.textViewSessionTitle.text = getString(R.string.focus_session)
                viewModel.loadFocusMotivation()
            }
            TimerSessionType.BREAK -> {
                binding.textViewSessionTitle.text = getString(R.string.break_session)
                viewModel.loadBreakMotivation()
            }
        }
        
        // Update pause/resume button icon
        val iconRes = if (timerInfo.state == TimerState.RUNNING) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        binding.buttonPauseResume.setIconResource(iconRes)
        
        // If session is complete, navigate back
        if (timerInfo.state == TimerState.IDLE) {
            findNavController().popBackStack()
        }
    }
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun pauseSession() {
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        requireActivity().startService(intent)
        
        // Also pause audio if it's a focus session
        if (timerService?.timerInfo?.value?.sessionType == TimerSessionType.FOCUS) {
            val audioIntent = Intent(requireContext(), AudioService::class.java).apply {
                action = AudioService.ACTION_PAUSE
            }
            requireActivity().startService(audioIntent)
        }
    }

    private fun resumeSession() {
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        requireActivity().startService(intent)
        
        // Also resume audio if it's a focus session
        if (timerService?.timerInfo?.value?.sessionType == TimerSessionType.FOCUS) {
            val audioIntent = Intent(requireContext(), AudioService::class.java).apply {
                action = AudioService.ACTION_RESUME
            }
            requireActivity().startService(audioIntent)
        }
    }

    private fun stopSession() {
        // Mark tree as withered if stopping a focus session before completion
        if (timerService?.timerInfo?.value?.sessionType == TimerSessionType.FOCUS &&
            timerService?.timerInfo?.value?.state == TimerState.RUNNING) {
            viewModel.witherTree()
        }
        
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        requireActivity().startService(intent)
        
        // Also stop audio if it's playing
        val audioIntent = Intent(requireContext(), AudioService::class.java).apply {
            action = AudioService.ACTION_STOP
        }
        requireActivity().startService(audioIntent)
        
        // Reset session tracking
        currentSessionId = null
        
        // Navigate back to home
        findNavController().popBackStack()
    }

    private fun showStopConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.stop_session_title)
            .setMessage(R.string.stop_session_message)
            .setPositiveButton(R.string.stop) { _, _ ->
                stopSession()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showVolumeDialog() {
        volumeDialogHelper.showVolumeDialog(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner
        ) { volume ->
            // Update volume in AudioService and ViewModel
            viewModel.updateVolume(volume)
        }
    }
    
    private fun showAudioTrackDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentTrackId = viewModel.selectedAudioTrack.value?.id ?: AudioTracks.NATURE
            
            audioTrackDialogHelper.showAudioTrackDialog(
                context = requireContext(),
                currentTrackId = currentTrackId
            ) { trackId ->
                // Update audio track selection
                viewModel.setAudioTrack(trackId)
            }
        }
    }
}
