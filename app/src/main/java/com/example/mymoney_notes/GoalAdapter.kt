package com.example.mymoney_notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymoney_notes.databinding.ItemGoalBinding
import java.text.DecimalFormat

class GoalAdapter : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    private var goals: List<GoalItem> = emptyList()

    inner class GoalViewHolder(val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goalItem = goals[position]
        val goal = goalItem.goal
        val totalSpent = goalItem.totalSpent
        with(holder.binding) {
            tvCategory.text = goal.category
            tvDescription.text = goal.description.takeIf { it.isNotEmpty() } ?: "No description"
            tvType.text = goal.type.replaceFirstChar { it.uppercase() }
            tvMinMax.text = "Min: ${DecimalFormat("0.00").format(goal.minGoal)} / Max: ${DecimalFormat("0.00").format(goal.maxGoal)}"

            // Calculate progress
            val progress = if (goal.maxGoal > 0) {
                ((totalSpent / goal.maxGoal) * 100).coerceIn(0.0, 100.0).toInt()
            } else {
                0
            }
            progressBar.progress = progress

            // Set amount progress (e.g., "1250.00/2500.00")
            tvAmountProgress.text = "${DecimalFormat("0.00").format(totalSpent)}/${DecimalFormat("0.00").format(goal.maxGoal)}"

            // Set status message
            tvStatus.text = if (totalSpent <= goal.maxGoal) {
                "You're within your budget"
            } else {
                "You've overspent"
            }

            // Load photo
            ivPhoto.setImageDrawable(null)
            if (goal.photoPath.isNotEmpty()) {
                try {
                    Glide.with(ivPhoto.context)
                        .load(goal.photoPath)
                        .error(R.drawable.ic_photo_placeholder)
                        .into(ivPhoto)
                } catch (e: Exception) {
                    ivPhoto.setImageResource(R.drawable.ic_photo_placeholder)
                }
            } else {
                ivPhoto.setImageResource(R.drawable.ic_photo_placeholder)
            }
        }
    }

    override fun getItemCount(): Int = goals.size

    fun submitList(newGoals: List<GoalItem>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}