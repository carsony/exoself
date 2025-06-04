package com.example.exoself

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.exoself.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_TIMER_UP = "timer_up"
        const val ARG_TIMER_UP = "timer_up"
        const val CHANNEL_ID = "channel_id"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
            setSupportActionBar(it.appBarMain.toolbar)
        }
        viewModel.getAuthState().observe(this) { isUserSignedOut ->
            if (isUserSignedOut) {
                signIn()
            }
        }
        createNotificationChannel()
        setupNotificationAndBroadcastReceiver()
        setupDrawerAndNavigation()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.open_settings_fragment)
                true
            }
            R.id.action_logout -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setupDrawerAndNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_stats
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    private fun signIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val signInIntent =
            AuthUI.getInstance().createSignInIntentBuilder().setIsSmartLockEnabled(false)
                .setAvailableProviders(providers).build()

        signInLauncher.launch(signInIntent)
    }

    private fun signOut() {
        viewModel.signOut()
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.addNewProfile()
        } else {
            Snackbar.make(
                binding.root,
                "There was an error in registering your account.",
                Snackbar.LENGTH_LONG
            )
        }
    }

    private fun setupNotificationAndBroadcastReceiver() {
        val bundle = bundleOf(ARG_TIMER_UP to true)

        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.nav_task)
            .setArguments(bundle)
            .createPendingIntent()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Exoself")
            .setContentText("Timer is up!")
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setSmallIcon(R.drawable.ic_baseline_timer_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)

        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val notificationId = 0
                notificationManager.notify(notificationId, notification)
            }
        }

        this.registerReceiver(receiver, IntentFilter(ACTION_TIMER_UP))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}