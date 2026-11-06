package com.hacms.app;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.view.Menu;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import androidx.navigation.NavOptions;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_FOCUS_TIMER = "com.hacms.app.OPEN_FOCUS_TIMER";
    private final FocusScheduleSync focusSync=new FocusScheduleSync();
    private UnreadBadgeController unreadBadge;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        // Android 13+ notifications require one user permission prompt.
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)
                   != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},301);
        }

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.navHostFragment);
        NavController navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        NavigationUI.setupWithNavController(bottomNav, navController);

// Handle navigation when a bottom navigation item is selected
        bottomNav.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.dashboardFragment) {
                openDashboard(navController);
                return true;
            }

            return NavigationUI.onNavDestinationSelected(item, navController);
        });

// Handle Home being tapped when it is already selected
        bottomNav.setOnItemReselectedListener(item -> {

            if (item.getItemId() == R.id.dashboardFragment) {
                openDashboard(navController);
            }

        });
        handleNavigationIntent(getIntent());
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_FOCUS_TIMER, false)) return;
        intent.removeExtra(EXTRA_OPEN_FOCUS_TIMER);
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (host == null) return;
        NavController controller = host.getNavController();
        if (controller.getCurrentDestination() == null ||
                controller.getCurrentDestination().getId() != R.id.focusStatusFragment) {
            controller.navigate(R.id.focusStatusFragment);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
        FocusAccessibilityService.acknowledgeNavigation(true);
    }

    @Override protected void onResume(){
        super.onResume();
        FocusAccessibilityService.acknowledgeNavigation(true);
    }

    @Override protected void onStart(){super.onStart();focusSync.start(this);FocusModeManager.rescheduleCached(this);ReminderScheduler.rearmCachedBoundaries(this);if(unreadBadge==null)unreadBadge=new UnreadBadgeController((BottomNavigationView)findViewById(R.id.bottomNav));unreadBadge.start();}
    @Override protected void onStop(){if(unreadBadge!=null)unreadBadge.stop();focusSync.stop();super.onStop();}
    public void logout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
    private void openDashboard(NavController navController) {

        if (navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getId()
                        == R.id.dashboardFragment) {
            return;
        }

        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, false)
                .setLaunchSingleTop(true)
                .setRestoreState(false)
                .build();

        navController.navigate(
                R.id.dashboardFragment,
                null,
                options
        );
    }
}
