package com.android.imagic.files.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.imagic.files.compat.getDrawableCompat
import com.android.imagic.files.navigation.QuickAccess
import com.android.imagic.files.navigation.QuickAccessesLiveData
import com.android.imagic.files.ui.PreferenceFragmentCompat
import com.android.imagic.files.util.valueCompat

class QuickAccessListPreferenceFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener {
    override fun onCreatePreferencesFix(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        QuickAccessesLiveData.observe(viewLifecycleOwner) { onQuickAccessesChanged(it) }
    }

    private fun onQuickAccessesChanged(
        quickAccesses: List<QuickAccess>
    ) {
        var preferenceScreen = preferenceScreen
        val context = requireContext()
        val oldPreferences = mutableMapOf<String, Preference>()
        if (preferenceScreen == null) {
            preferenceScreen = preferenceManager.createPreferenceScreen(context)
            setPreferenceScreen(preferenceScreen)
        } else {
            for (index in preferenceScreen.preferenceCount - 1 downTo 0) {
                val preference = preferenceScreen.getPreference(index)
                preferenceScreen.removePreference(preference)
                oldPreferences[preference.key] = preference
            }
        }
        for (quickAccess in quickAccesses) {
            val key = quickAccess.key
            var preference = oldPreferences[key] as SwitchPreferenceCompat?
            if (preference == null) {
                preference = SwitchPreferenceCompat(context).apply {
                    this.key = key
                    isPersistent = false
                    onPreferenceClickListener = this@QuickAccessListPreferenceFragment
                }
            }
            preference.apply {
                icon = context.getDrawableCompat(quickAccess.iconRes)
                title = quickAccess.getTitle(context)
                isChecked = quickAccess.isEnabled
            }
            preferenceScreen.addPreference(preference)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        preference as SwitchPreferenceCompat
        val id = preference.key
        val isEnabled = preference.isChecked
        val settingsList = Settings.QUICK_ACCESS_SETTINGS.valueCompat.toMutableList()
        val index = settingsList.indexOfFirst { it.id == id }
        if (index != -1) {
            settingsList[index] = settingsList[index].copy(isEnabled = isEnabled)
        } else {
            val quickAccess = QuickAccessesLiveData.valueCompat.find { it.key == id }!!
            settingsList += quickAccess.toSettings().copy(isEnabled = isEnabled)
        }
        Settings.QUICK_ACCESS_SETTINGS.putValue(settingsList)
        return true
    }
}
