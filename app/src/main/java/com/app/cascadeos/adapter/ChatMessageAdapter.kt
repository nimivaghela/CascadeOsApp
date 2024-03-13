package com.app.cascadeos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ItemChatMessageBinding
import com.app.cascadeos.model.MessageModel
import com.app.cascadeos.utility.dpToPx

class ChatMessageAdapter(private var isFromCoolEcall: Boolean = false) : RecyclerView.Adapter<ChatMessageAdapter.ViewHolder>() {

    private val dataList = ArrayList<MessageModel>()

    init {
        dataList.add(MessageModel("Hey, what's up?", isSender = false))
        dataList.add(MessageModel("Not much, just hanging out at home. How about you?", isSender = true))
        dataList.add(
            MessageModel(
                "Same here. I was thinking of grabbing a bite to eat later, want to join me?",
                isSender = false
            )
        )
        dataList.add(MessageModel("Sure, that sounds great! Where do you want to go?", isSender = true))
        dataList.add(MessageModel("How about that new Italian place on Main Street?", isSender = false))
        dataList.add(MessageModel("Sounds perfect! What time were you thinking?", isSender = true))
        dataList.add(MessageModel("How about 7 PM?", isSender = false))
        dataList.add(MessageModel("7 PM works for me. I'll see you there.", isSender = true))
        dataList.add(MessageModel("Hey man, Let's Connect", isSender = false))
        dataList.add(MessageModel("Are you there ?", isSender = false))
        dataList.add(MessageModel("Sorry, I am stuck somewhere else so can't talk right now!", isSender = true))
        dataList.add(MessageModel("Can we reschedule the call ?", isSender = true))
        dataList.add(MessageModel("No problem. We will connect later.", isSender = false))
        dataList.add(MessageModel("We can connect tomorrow same time!", isSender = true))
        dataList.add(MessageModel("Works for me....", isSender = false))
        dataList.add(MessageModel("Thanks !!", isSender = true))
        dataList.add(MessageModel("Bye Bye 👋", isSender = false))
        dataList.add(MessageModel("Bye !!", isSender = true))
    }

    class ViewHolder(private val itemBinding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(messageModel: MessageModel) {
            itemBinding.isOtherUserMsg = !messageModel.isSender
            itemBinding.message = messageModel.message
            itemBinding.executePendingBindings()
         /*  if (isFromCoolEcall){
               itemBinding.txtMsgLeft.textSize = 5.dpToPx(context = itemBinding.root.context)
               itemBinding.txtMsgRight.textSize = 5.dpToPx(context = itemBinding.root.context)
                itemBinding.txtMsgLeft.maxWidth = itemBinding.root.context.resources.getDimension(R.dimen.dp_130).toInt()
               itemBinding.txtMsgRight.maxWidth = itemBinding.root.context.resources.getDimension(R.dimen.dp_130).toInt()
           }else{
               itemBinding.txtMsgLeft.maxWidth = itemBinding.root.context.resources.getDimension(R.dimen.dp_250).toInt()
               itemBinding.txtMsgRight.maxWidth =itemBinding.root.context.resources.getDimension(R.dimen.dp_250).toInt()
           }*/
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding: ItemChatMessageBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_chat_message,
            parent,
            false
        )
        return ViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])

    }

    fun addMessage(message: String) {
        dataList.add(MessageModel(message, isSender = true))
        notifyItemInserted(dataList.size)
    }

}