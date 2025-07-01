package com.example.myapplication.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.User
import com.example.myapplication.utils.SharedPreferencesManager

class HomeFragment : Fragment() {

    private lateinit var tvBalance: TextView
    private lateinit var tvBalancePercentage: TextView
    private lateinit var tvCount: TextView
    private lateinit var tvCountPercentage: TextView
    private lateinit var rvUsers: RecyclerView
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize components
        initComponents(view)

        // Set up data
        setupData()

        return view
    }

    private fun initComponents(view: View) {
        tvBalance = view.findViewById(R.id.tv_balance)
        tvBalancePercentage = view.findViewById(R.id.tv_balance_percentage)
        tvCount = view.findViewById(R.id.tv_count)
        tvCountPercentage = view.findViewById(R.id.tv_count_percentage)
        rvUsers = view.findViewById(R.id.rv_users)

        rvUsers.layoutManager = LinearLayoutManager(context)
        usersAdapter = UsersAdapter(emptyList())
        rvUsers.adapter = usersAdapter

        sharedPreferencesManager = SharedPreferencesManager(requireContext())
    }

    private fun setupData() {
        // Set balance and count values
        tvBalance.text = "$45,678.90"
        tvBalancePercentage.text = "+20% month over month"
        tvCount.text = "2,405"
        tvCountPercentage.text = "+33% month over month"

        // Get registered users from SharedPreferences
        val registeredUsers = sharedPreferencesManager.getRegisteredUsers()

        // Sample data for testing if no users are registered
        val sampleUsers = if (registeredUsers.isEmpty()) {
            listOf(
                User(1, "Elynn Lee", "elynn@example.net", "122333333"),

            )
        } else {
            registeredUsers
        }

        // Update the adapter with users
        usersAdapter.updateUsers(sampleUsers)
    }
}