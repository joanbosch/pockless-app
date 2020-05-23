package com.pes.pockles.view.ui.pockshistory.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.pes.pockles.R
import com.pes.pockles.databinding.PockHistoryItemBinding
import com.pes.pockles.model.Pock

class BindingPockItem : AbstractBindingItem<PockHistoryItemBinding>() {
    public var pock: Pock? = null
    public var showEdit: Boolean = false

    override val type: Int
        get() = R.id.card

    override fun bindView(binding: PockHistoryItemBinding, payloads: List<Any>) {
        pock?.let {
            binding.pock = it
        }

        if (showEdit) {
            binding.editButton.visibility = View.VISIBLE
        } else {
            binding.editButton.visibility = View.GONE
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): PockHistoryItemBinding {
        return PockHistoryItemBinding.inflate(inflater, parent, false)
    }
}