package com.app.cascadeos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.databinding.ItemInteractReactionBinding
import com.app.cascadeos.model.MessageModel
import com.app.cascadeos.model.ReactionModel

class ReactionAdapter(
    val context: Context,
    private var reactionList: ArrayList<ReactionModel>,
) : RecyclerView.Adapter<ReactionAdapter.ReactionViewHolder>() {

    inner class ReactionViewHolder(val binding: ItemInteractReactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReactionViewHolder {
        return ReactionViewHolder(
            ItemInteractReactionBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun getItemCount() = reactionList.size

    override fun onBindViewHolder(holder: ReactionViewHolder, position: Int) {
        with(holder) {
            binding.apply {
                imgProfile.setImageResource(reactionList[bindingAdapterPosition].profileImage)
                tvName.text = reactionList[bindingAdapterPosition].name
                tvReaction.text = reactionList[bindingAdapterPosition].message
            }
        }
    }

    fun addMessage(reactionModel: ReactionModel) {
        reactionList.add(reactionModel)
        notifyItemInserted(reactionList.size)
    }
}