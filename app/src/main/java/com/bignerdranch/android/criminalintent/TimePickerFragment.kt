package com.bignerdranch.android.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import java.util.Calendar

class TimePickerFragment : DialogFragment() {

    private val args: TimePickerFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val calendar = Calendar.getInstance()
        calendar.time = args.crimeDate

        val timeListener =
            TimePickerDialog
                .OnTimeSetListener { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE , minute)
                    val resultDate = calendar.time
                    setFragmentResult(
                        REQUEST_KEY_TIME ,
                        bundleOf(REQUEST_KEY_BUNDLE to resultDate)
                    )
                }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)


        return TimePickerDialog(
            requireContext(),
            timeListener,
            hour,
            minute,
            false
        )
    }

    companion object {
        const val REQUEST_KEY_TIME = "REQUEST_KEY_TIME"
        const val REQUEST_KEY_BUNDLE = "REQUEST_KEY_BUNDLE"
    }
}