package com.android.imagic.files.viewer.image

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager2.widget.ViewPager2
import com.android.imagic.files.R
import com.android.imagic.files.databinding.ImageViewerFragmentBinding
import com.android.imagic.files.ui.DepthPageTransformer
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.ParcelableListParceler
import com.android.imagic.files.util.ParcelableState
import com.android.imagic.files.util.args
import com.android.imagic.files.util.extraUrlStringList
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.getState
import com.android.imagic.files.util.mediumAnimTime
import com.android.imagic.files.util.putState
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.systemuihelper.SystemUiHelper

class CloudImageViewerFragment : Fragment() {
    private val args by args<Args>()
    private val argsUrls by lazy { args.intent.extraUrlStringList }

    private lateinit var urls: MutableList<String>

    private lateinit var binding: ImageViewerFragmentBinding

    private lateinit var systemUiHelper: SystemUiHelper

    private lateinit var adapter: CloudImageViewerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        urls = (savedInstanceState?.getState<State>()?.urls ?: argsUrls).toMutableList()

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ImageViewerFragmentBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (urls.isEmpty()) {
            // TODO: Show a toast.
            finish()
            return
        }

        val activity = activity as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        // Our app bar will draw the status bar background.
        activity.window.statusBarColor = Color.TRANSPARENT
        binding.appBarLayout.applySystemWindowInsetsToPadding(left = true, top = true, right = true)
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
        adapter = CloudImageViewerAdapter(viewLifecycleOwner) { systemUiHelper.toggle() }.apply {
            replace(urls)
        }
        binding.viewPager.apply {
            // 1 is the default for the old androidx.viewpager.widget.ViewPager.
            offscreenPageLimit = 1
            adapter = adapter
            // ViewPager saves its position and will restore it later.
            setCurrentItem(args.position, false)
            setPageTransformer(DepthPageTransformer)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateTitle()
                }
            })
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (urls.isEmpty()) {
            // We did finish the activity in onActivityCreated(), however we will still be called
            // here before the activity is actually finished.
            return
        }

        updateTitle()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putState(State(urls))
    }

    private fun updateTitle() {
        val url = currentUrl
        requireActivity().title = url
        val size = urls.size
        binding.toolbar.subtitle = if (size > 1) {
            getString(
                R.string.image_viewer_subtitle_format, binding.viewPager.currentItem + 1, size
            )
        } else {
            null
        }
    }

    private val currentUrl: String
        get() = urls[binding.viewPager.currentItem]

    @Parcelize
    class Args(val intent: Intent, val position: Int) : ParcelableArgs

    @Parcelize
    private class State(val urls: @WriteWith<ParcelableListParceler> List<String>) : ParcelableState
}
