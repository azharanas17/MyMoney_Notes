package com.example.mymoney_notes

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymoney_notes.data.Expense
import com.example.mymoney_notes.databinding.ItemExpenseBinding
import java.text.DecimalFormat

class ExpenseAdapter(
    private var expenses: MutableList<Expense>,
    private val onPhotoClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        with(holder.binding) {
            tvAmount.text = DecimalFormat("0.00").format(expense.amount)
            tvAmount.setTextColor(if (expense.type == "income") Color.GREEN else Color.RED)
            tvCategory.text = expense.category
            tvCategory.setTypeface(null, android.graphics.Typeface.BOLD)
            tvDate.text = expense.date
            tvDescription.text = expense.description
            tvTime.text = "${expense.startTime} - ${expense.endTime}"

            // Load photo
            ivPhoto.setImageDrawable(null) // Clear previous image
            if (expense.photoPath.isNotEmpty()) {
                try {
                    Glide.with(ivPhoto.context)
                        .load(expense.photoPath)
                        .error(R.drawable.ic_photo_placeholder)
                        .into(ivPhoto)
                    Log.d("ExpenseAdapter", "Loaded photo for expense ${expense.id}: ${expense.photoPath}")
                    ivPhoto.isClickable = true
                    ivPhoto.setOnClickListener { onPhotoClick(expense) }
                } catch (e: Exception) {
                    Log.e("ExpenseAdapter", "Error loading photo for expense ${expense.id}: $e")
                    ivPhoto.setImageResource(R.drawable.ic_photo_placeholder
                    )
                    ivPhoto.isClickable = false
                }
            } else {
                ivPhoto.setImageResource(R.drawable.ic_photo_placeholder)
                ivPhoto.isClickable = false
                Log.d("ExpenseAdapter", "No photo for expense ${expense.id}")
            }
        }
        Log.d("ExpenseAdapter", "Bound expense at position $position: $expense")
    }

    override fun getItemCount(): Int = expenses.size

    fun updateExpenses(newExpenses: List<Expense>) {
        expenses.clear()
        expenses.addAll(newExpenses)
        notifyDataSetChanged()
        Log.d("ExpenseAdapter", "Updated expenses: $expenses")
    }
}