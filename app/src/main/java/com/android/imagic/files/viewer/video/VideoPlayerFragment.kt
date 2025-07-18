package com.android.imagic.files.viewer.video

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.android.imagic.files.databinding.VideoPlayerFragmentBinding
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.ParcelableParceler
import com.android.imagic.files.util.ParcelableState
import com.android.imagic.files.util.args
import com.android.imagic.files.util.getState
import com.android.imagic.files.util.mediumAnimTime
import com.android.imagic.files.util.putState
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.systemuihelper.SystemUiHelper

class VideoPlayerFragment : Fragment() {
    private val args by args<Args>()
    private val videoUrl by lazy { args.intent.data }
    private val videoName by lazy { args.name }

    private lateinit var uri: Uri

    private lateinit var name: String

    private lateinit var player: ExoPlayer

    private lateinit var binding: VideoPlayerFragmentBinding

    private lateinit var systemUiHelper: SystemUiHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(requireContext()).build()
        uri = savedInstanceState?.getState<State>()?.uri ?: videoUrl!!
        name = savedInstanceState?.getState<State>()?.name ?: videoName

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = VideoPlayerFragmentBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = activity as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        // Our app bar will draw the status bar background.
        activity.window.statusBarColor = Color.TRANSPARENT
        binding.appBarLayout.applySystemWindowInsetsToPadding(left = true, top = true, right = true)
        activity.title = name
        binding.toolbar.subtitle = null
        systemUiHelper = SystemUiHelper(
            activity, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_IMMERSIVE_STICKY
        ) { visible: Boolean ->
            binding.appBarLayout.animate()
                .alpha(if (visible) 1f else 0f)
                .translationY(if (visible) 0f else -binding.appBarLayout.bottom.toFloat())
                .setDuration(mediumAnimTime.toLong())
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
        // This will set up window flags.
        systemUiHelper.show()

        binding.playerView.setOnClickListener { systemUiHelper.toggle() }
        binding.playerView.player = player
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onStop() {
        super.onStop()

        player.release()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        updateTitle()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putState(State(uri, args.name))
    }

    private fun updateTitle() {
        requireActivity().title = args.name
        binding.toolbar.subtitle = null
    }

    @Parcelize
    class Args(val intent: Intent, val uri: Uri, val name: String) : ParcelableArgs

    @Parcelize
    private class State(val uri: @WriteWith<ParcelableParceler> Uri, val name: String) :
        ParcelableState
}
