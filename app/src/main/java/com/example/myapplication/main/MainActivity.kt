package com.example.myapplication.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.auth.LoginActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.home.UsersAdapter
import com.example.myapplication.model.User
import com.example.myapplication.utils.PrefManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PrefManager
    private lateinit var usersAdapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)
        setupUI()
        setupFinancialData()
        setupUsersList()
    }

    private fun setupUI() {
        // Set up welcome message (keeping your existing welcome message)
        val userEmail = prefManager.getUserEmail()
        //binding.tvAppName.text = "MyPaws" // App name in the toolbar

        // Set up the tab selection
        binding.tabLayout.getTabAt(1)?.select() // Select "Historical" tab by default

        // Set up logout button (keeping your existing logout functionality)
        binding.buttonLogout.setOnClickListener { logoutUser() }

        // You can also add logout to profile icon if desired
        //binding.ivProfile.setOnClickListener { logoutUser() }

        // Set up bottom navigation default selection
        binding.bottomNavigation.selectedItemId = binding.bottomNavigation.menu.getItem(0).itemId
    }

    private fun setupFinancialData() {
        // Set financial data
        binding.tvBalance.text = "$45,678.90"
        binding.tvBalancePercentage.text = "+20% month over month"
        binding.tvCount.text = "2,405"
        binding.tvCountPercentage.text = "+33% month over month"
    }

    private fun setupUsersList() {
        // Configure RecyclerView
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        usersAdapter = UsersAdapter(getSampleUsers())
        binding.rvUsers.adapter = usersAdapter
    }

    private fun getSampleUsers(): List<User> {
        // Get the current user email
        val currentUserEmail = prefManager.getUserEmail()

        // Create a list with the current user and some sample users
        return listOf(
            User(name = "Elynn Lee", email = "elynn@example.net", password = ""),
            User(name = "Oscar Dum", email = "oscar@example.net", password = ""),
            User(name = "Carlo Emilion", email = "carlo@example.net", password = ""),
            User(name = "Daniel Jay Park", email = "daniel@example.net", password = ""),
            User(name = "Mark Rojas", email = "mark@example.net", password = "")
        ).toMutableList().apply {
            // Add current user at the top if not already in the list and if the email isn't empty
            if (currentUserEmail != null) {
                if (currentUserEmail.isNotEmpty() && !any { it.email == currentUserEmail }) {
                    add(0, User(name = "Current User", email = currentUserEmail, password = ""))
                }
            }
        }
    }

    private fun logoutUser() {
        prefManager.clearSession()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}