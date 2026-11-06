package com.hacms.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Opt-in foreground-package monitor; does not read text, gestures, or screen contents. */
public class FocusAccessibilityService extends AccessibilityService {
    private WindowManager wm;
    private View overlay;
    private String overlaySessionId;
    // During an explicit button-driven transition, ignore events from the app
    // underneath the overlay.  Without this guard Android can redraw the
    // blocker before MainActivity / FocusUnlockActivity becomes foreground.
    private long navigatingUntil;
    private enum Destination { NONE, TIMER, UNLOCK }
    private Destination pending = Destination.NONE;
    private static java.lang.ref.WeakReference<FocusAccessibilityService> serviceRef =
            new java.lang.ref.WeakReference<>(null);
    /** Called when the actual destination Activity is ready, not when it merely starts launching. */
    public static void acknowledgeNavigation(boolean timer){
        FocusAccessibilityService service=serviceRef.get();
        if(service!=null)service.handler.post(()->service.confirmNavigation(timer));
    }
    private void confirmNavigation(boolean timer){
        if(pending!=(timer?Destination.TIMER:Destination.UNLOCK))return;
        pending=Destination.NONE;
        navigatingUntil=System.currentTimeMillis()+1500;
        handler.removeCallbacks(navigationFailure);
        removeOverlay();
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable navigationFailure = () -> {
        if(pending!=Destination.NONE){
            pending=Destination.NONE;
            navigatingUntil=0;
            Toast.makeText(this,"Could not open UniTask. Please try the button again.",Toast.LENGTH_SHORT).show();
        }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            FocusModeManager.Session current = FocusModeManager.active(FocusAccessibilityService.this);
            if (overlay != null && (current == null || !current.id.equals(overlaySessionId))) {
                removeOverlay();
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        serviceRef = new java.lang.ref.WeakReference<>(this);
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName() == null) return;
        FocusModeManager.Session session = FocusModeManager.active(this);
        if (session == null) { removeOverlay(); return; }
        if (System.currentTimeMillis() < navigatingUntil) return;
        String pkg = event.getPackageName().toString();
        if (pkg.equals(getPackageName())) {
            // Overlay window events belong to UniTask. They should not re-trigger
            // detection or dismiss an overlay already visible.
            return;
        }
        if (overlay != null) return;
        if (FocusModeManager.blocked(this).contains(pkg)) showOverlay(pkg, session);
    }

    private void goToUniTask(Class<?> destination, boolean timer) {
        // Set the cooldown BEFORE dismissing the overlay, since removing an
        // accessibility window generates new window-state events.
        if(pending!=Destination.NONE)return;
        pending=timer?Destination.TIMER:Destination.UNLOCK;
        navigatingUntil = System.currentTimeMillis() + 5000;
        handler.removeCallbacks(navigationFailure);
        handler.postDelayed(navigationFailure,4500);
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (timer) intent.putExtra(MainActivity.EXTRA_OPEN_FOCUS_TIMER, true);
        try {
            // Keep the overlay visible until the new Activity acknowledges onResume.
            // This avoids briefly revealing the blocked app and reopening the blocker.
            startActivity(intent);
        } catch (RuntimeException ex) {
            pending=Destination.NONE;
            handler.removeCallbacks(navigationFailure);
            navigatingUntil = 0;
            Toast.makeText(this, "Could not open UniTask. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showOverlay(String pkg, FocusModeManager.Session session) {
        if (overlay != null) return;
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) return;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(40, 40, 40, 40);
        box.setBackgroundColor(0xff14263d);
        TextView title = new TextView(this);
        title.setText("UniTask · Focus Mode");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        box.addView(title);
        TextView body = new TextView(this);
        body.setText("You chose to avoid this app while studying.\n\n" + session.title
                + "\n\nReturn to your study timer or verify to end Focus Mode early.");
        body.setTextColor(Color.WHITE);
        body.setTextSize(18);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, 25, 0, 25);
        box.addView(body);
        Button timer = new Button(this);
        timer.setText("Return to my study");
        timer.setOnClickListener(v -> goToUniTask(MainActivity.class, true));
        box.addView(timer);
        Button end = new Button(this);
        end.setText("End Focus early · verify password");
        end.setOnClickListener(v -> goToUniTask(FocusUnlockActivity.class, false));
        box.addView(end);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                -1, -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        try {
            wm.addView(box, params);
            overlay = box;
            overlaySessionId = session.id;
        } catch (RuntimeException ex) {
            overlay = null;
            overlaySessionId = null;
        }
    }

    private void removeOverlay() {
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (RuntimeException ignored) { }
            overlay = null;
            overlaySessionId = null;
        }
    }
    @Override public void onInterrupt() { removeOverlay(); }
    @Override public void onDestroy() {
        handler.removeCallbacks(tick);
        handler.removeCallbacks(navigationFailure);
        if(serviceRef.get()==this)serviceRef.clear();
        removeOverlay();
        super.onDestroy();
    }
}
