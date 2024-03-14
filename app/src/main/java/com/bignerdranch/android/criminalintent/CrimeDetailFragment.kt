package com.bignerdranch.android.criminalintent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

private const val Date_Format = "EEE, MMM, dd"

class CrimeDetailFragment : Fragment() {

    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "cannot access binding because it is null. Is the view visible?"
        }
    private val args: CrimeDetailFragmentArgs by navArgs()

    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeId)
    }

    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->

        uri?.let { parseContactSelection(it) }

    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

        if (isGranted) {
            binding.callSuspect.performClick()
            Snackbar.make(
                binding.root,
                "Got the suspect phone number",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private var photoName: String? = null
    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto ->
        if (didTakePhoto && photoName != null) {
            crimeDetailViewModel.updateCrime { oldCrime ->
                oldCrime.copy(photoFileName = photoName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentCrimeDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(title = text.toString())
                }
            }

            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(isSolved = isChecked)
                }
            }

            deleteButton.setOnClickListener {

                requireActivity().lifecycleScope.launch {

                    crimeDetailViewModel.deleteCrime()
                    findNavController().navigateUp()

                }
            }

            crimeSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }

            val selectSuspectIntent =
                selectSuspect.contract.createIntent(requireContext(), null)

            crimeSuspect.isEnabled = canResolveIntent(selectSuspectIntent)

            crimeCamera.setOnClickListener {
                photoName = "IMG_${Date()}.JPG"
                val photoFile = File(requireContext().applicationContext.filesDir, photoName!!)
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    photoFile
                )
                takePhoto.launch(photoUri)
            }
            val photoFile = File(requireContext().applicationContext.filesDir, "Name")
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "com.bignerdranch.android.criminalintent.fileprovider",
                photoFile
            )

            val captureImageIntent = takePhoto.contract.createIntent(requireContext(), photoUri)
            crimeCamera.isEnabled = canResolveIntent(captureImageIntent)

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeDetailViewModel.crime.collect { crime ->
                    crime?.let { updateUi(it) }
                }
            }
        }

        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val newDate =
                bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }
        }

        setFragmentResultListener(
            TimePickerFragment.REQUEST_KEY_TIME
        ) { _, bundle ->
            val newDate =
                bundle.getSerializable(TimePickerFragment.REQUEST_KEY_BUNDLE) as Date
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }

        }

        checkTitleOnBackPressed()

    }

    private fun checkTitleOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.crimeTitle.text.toString().trim().isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "crime title cannot be empty", Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(crime: Crime) {

        binding.apply {

            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }

            crimeDate.text = DateFormat.format("dd / MM / yyyy", crime.date)

            crimeTime.text = DateFormat.format("hh:mm a", crime.date)

            crimeTime.setOnClickListener {
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectTime(crime.date)
                )
            }

            crimeDate.setOnClickListener {
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectDate(crime.date)
                )
            }

            crimeSolved.isChecked = crime.isSolved

            crimeReport.setOnClickListener {
                val reportIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            getString(R.string.crime_report_subject)
                        )
                    }
                val chooserIntent = Intent.createChooser(
                    reportIntent,
                    getString(R.string.send_report)
                )
                startActivity(chooserIntent)
            }

            crimeSuspect.text = crime.suspect.ifEmpty {
                getString(R.string.crime_suspect_text)
            }

            callSuspect.apply {
                isEnabled = crime.suspectId != ""
                if (crime.suspectPhoneNumber == "") {
                    text = getText(R.string.get_number)
                    setOnClickListener {
                        checkPermission(crime.suspectId)
                    }
                } else {
                    text = getString(R.string.call_the_suspect, crime.suspectPhoneNumber)
                    setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${crime.suspectPhoneNumber}")
                        }
                        startActivity(intent)
                    }
                }

            }
            updatePhoto(crime.photoFileName)

        }
    }

    private fun getCrimeReport(crime: Crime): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(Date_Format, crime.date)
        val suspectText = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspectText
        )
    }

    private fun parseContactSelection(contactUri: Uri) {
        val queryFields =
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID)

        requireActivity()
            .contentResolver
            .query(contactUri, queryFields, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                    val suspect = if (nameIndex >= 0) cursor.getString(nameIndex) else ""

                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)

                    val suspectId = if (idIndex >= 0) cursor.getLong(idIndex).toString() else ""

                    crimeDetailViewModel.updateCrime { oldCrime ->
                        oldCrime.copy(suspect = suspect, suspectId = suspectId)
                    }
                }
            }
    }

    private fun canResolveIntent(intent: Intent): Boolean {

        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    private fun checkPermission(suspectId: String) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {

                getSuspectPhoneNumber(suspectId)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.READ_CONTACTS
            ) -> {
                Snackbar.make(
                    binding.root,
                    "we need the permission to get the suspect phone number",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            else -> {
                requestPermission.launch(Manifest.permission.READ_CONTACTS)

            }
        }
    }

    private fun getSuspectPhoneNumber(suspectId: String) {

        requireActivity().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(suspectId),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val number = if (numberIndex >= 0) cursor.getString(numberIndex) else ""

                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspectPhoneNumber = number)
                }
            }
        }

    }

    private fun updatePhoto(photoFileName: String?) {
        if (binding.crimePhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }
            if (photoFile?.exists() == true) {
                binding.crimePhoto.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.crimePhoto.setImageBitmap(scaledBitmap)
                    binding.crimePhoto.tag = photoFileName

                }
            } else {
                binding.crimePhoto.setImageBitmap(null)
                binding.crimePhoto.tag = null
            }
        }
    }
}