package com.pes.pockles.view.ui.chat


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.pes.pockles.R
import com.pes.pockles.databinding.FragmentChatBinding
import com.pes.pockles.view.ui.base.BaseFragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.BindingViewHolder
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.pes.pockles.data.Resource
import com.pes.pockles.databinding.ChatItemBinding
import com.pes.pockles.databinding.LikeItemBinding
import com.pes.pockles.model.Chat
import com.pes.pockles.model.ChatData
import com.pes.pockles.view.ui.chat.item.BindingChatItem

/**
 * A simple [Fragment] subclass
 */
class AllChatsFragment : BaseFragment<FragmentChatBinding>() {

    private val viewModel: AllChatsViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(AllChatsViewModel::class.java)
    }

    // Create the ItemAdapter holding your Items
    private val itemAdapter = ItemAdapter<BindingChatItem>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeChats.isRefreshing = true
        binding.viewmodel = viewModel

        val fastAdapter = FastAdapter.with(itemAdapter)
        binding.rvChats.let {
            it.layoutManager = LinearLayoutManager(activity)
            it.adapter = fastAdapter
        }

        fastAdapter.addEventHook(object : ClickEventHook<BindingChatItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                return viewHolder.asBinding<ChatItemBinding> {
                    it.chatItemCard
                }
            }

            override fun onClick(
                v: View,
                position: Int,
                fastAdapter: FastAdapter<BindingChatItem>,
                item: BindingChatItem
            ) {

                val intent = Intent(context, ChatActivity::class.java).apply {
                    var chatData: ChatData = ChatData(item.chat?.id, null, item.chat?.user2!!.name, item.chat?.user2!!.profileImageUrl)
                    putExtra("chatData", chatData)
                }
                context!!.startActivity(intent)
            }
        })

        initializeObservers()

        initilizeListeners()

    }

    private fun initilizeListeners() {
        // Add refresh action
        binding.swipeChats.setOnRefreshListener {
            viewModel.getAllChats()
        }

    }

    private fun initializeObservers() {
        viewModel.chats.observe(this, Observer {value: Resource<List<Chat>> ->
            value?.let {
                when (value) {
                    is Resource.Success<List<Chat>> -> setDataRecyclerView(it.data!!)
                    is Resource.Error -> handleError()
                }
            }
        })
    }

    private fun handleError() {
        val text = getString(R.string.cannot_load_chats)
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, text, duration)
        toast.show()
    }

    private fun setDataRecyclerView(chats: List<Chat>) {
        binding.swipeChats.isRefreshing = false

        binding.txtNoChats.visibility = View.GONE
        if (chats.isEmpty()) {
            binding.txtNoChats.visibility = View.VISIBLE
        } else {
            val chatListBinding: List<BindingChatItem> = chats.map { chat ->
                val binding =
                    BindingChatItem()
                binding.chat = chat
                binding
            }
            //Fill and set the items to the ItemAdapter
            itemAdapter.setNewList(chatListBinding)

            // Set up the notification observers
            viewModel.setUpNotificationObserver()
        }

    }

    override fun getLayout(): Int {
        return R.layout.fragment_chat
    }

    inline fun <reified T : ViewBinding> RecyclerView.ViewHolder.asBinding(block: (T) -> View): View? {
        return if (this is BindingViewHolder<*> && this.binding is T) {
            block(this.binding as T)
        } else {
            null
        }
    }
}