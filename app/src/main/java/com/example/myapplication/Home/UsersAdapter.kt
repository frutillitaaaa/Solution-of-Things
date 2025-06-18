package com.example.myapplication.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.User

class UsersAdapter(private var users: List<User>) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProfilePic: ImageView = itemView.findViewById(R.id.iv_profile_pic)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvEmail: TextView = itemView.findViewById(R.id.tv_email)

        fun bind(user: User) {
            tvName.text = user.name
            tvEmail.text = user.email

            // Profile pic can be set from user data if available
            // For now, we'll just use a default image
            //ivProfilePic.setImageResource(R.drawable.default_profile)
        }
    }
}