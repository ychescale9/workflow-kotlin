package com.squareup.sample.helloworkflowfragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.sample.helloworkflowfragment.databinding.HelloGoodbyeLayoutBinding
import com.squareup.sample.helloworkflowfragment.databinding.ItemLayoutBinding
import com.squareup.workflow1.ui.LayoutRunner
import com.squareup.workflow1.ui.LayoutRunner.Companion.bind
import com.squareup.workflow1.ui.ViewEnvironment
import com.squareup.workflow1.ui.ViewFactory
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi

@OptIn(WorkflowUiExperimentalApi::class)
class HelloLayoutRunner(
  private val binding: HelloGoodbyeLayoutBinding
) : LayoutRunner<HelloWorkflow.Rendering> {

  init {
    binding.recyclerView.layoutManager = LinearLayoutManager(binding.root.context)
  }

  private val itemAdapter = ItemAdapter()

  override fun showRendering(rendering: HelloWorkflow.Rendering, viewEnvironment: ViewEnvironment) {
    binding.helloMessage.text = rendering.message + " Fragment!"
    binding.helloMessage.setOnClickListener { rendering.onClick() }
    itemAdapter.submitList(rendering.items)
    if (binding.recyclerView.adapter == null) binding.recyclerView.adapter = itemAdapter
  }

  companion object : ViewFactory<HelloWorkflow.Rendering> by bind(
    HelloGoodbyeLayoutBinding::inflate, ::HelloLayoutRunner
  )
}

private class ItemAdapter : ListAdapter<String, ItemAdapter.ViewHolder>(diffCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      ItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  private class ViewHolder(private val binding: ItemLayoutBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(item: String) {
      binding.itemTextView.text = item
    }
  }
}

private val diffCallback: DiffUtil.ItemCallback<String> =
  object : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(
      oldItem: String,
      newItem: String
    ) = oldItem == newItem

    override fun areContentsTheSame(
      oldItem: String,
      newItem: String
    ) = oldItem == newItem
  }
