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
    }

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
