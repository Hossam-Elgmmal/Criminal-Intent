package com.bignerdranch.android.criminalintent

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimeBinding
import java.util.UUID

class CrimeHolder(
    private val binding: ListItemCrimeBinding
) : ViewHolder(binding.root) {
    fun bind(crime: Crime, onCrimeClicked: (crimeId: UUID) -> Unit) {
        binding.apply {
            crimeTitle.text = crime.title
            crimeDate.text = DateFormat.format("E , dd / MM / yyyy", crime.date)
            root.setOnClickListener {
                onCrimeClicked(crime.id)
            }
            crimeSolved.visibility = if (crime.isSolved) View.VISIBLE
            else View.GONE
        }
    }
}

class CrimeListAdapter(
    private val crimes: List<Crime>,
    private val onCrimeClicked: (crimeId: UUID) -> Unit
) : Adapter<CrimeHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
        return CrimeHolder(binding)

    }

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {

        val crime = crimes[position]
        holder.bind(crime, onCrimeClicked)

    }

    override fun getItemCount() = crimes.size

}