package me.magnum.melonds.ui.settings.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import me.magnum.melonds.R
import me.magnum.melonds.ui.settings.PreferenceFragmentTitleProvider


class AutoSavesettingFragment : PreferenceFragmentCompat(), PreferenceFragmentTitleProvider {

    override fun getTitle() = getString(R.string.autosavetitle)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_auto_save, rootKey)
    }
}