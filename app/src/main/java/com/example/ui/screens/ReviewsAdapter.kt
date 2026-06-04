package com.example.ui.screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ItemReviewBinding
import com.example.domain.model.Review

class ReviewsAdapter(
    private var reviews: List<Review>
) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    fun updateData(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {
            binding.reviewAuthor.text = review.author
            binding.reviewContent.text = review.content
            
            if (review.rating != null) {
                binding.reviewRatingBadge.visibility = View.VISIBLE
                binding.reviewRatingText.text = String.format("%.0f", review.rating)
            } else {
                binding.reviewRatingBadge.visibility = View.GONE
            }
        }
    }
}
