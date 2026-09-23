package com.example.gate

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

// Watches which app comes to the foreground; if it is gated and has no valid pass, opens the push-up screen.
class GateService : AccessibilityService() {
    private var last = ""

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == last) return
        last = pkg
        if (pkg == packageName) return
        val p = getSharedPreferences("gate", MODE_PRIVATE)
        if (pkg !in (p.getStringSet("gated", emptySet()) ?: emptySet())) return
        if (System.currentTimeMillis() < p.getLong("pass_$pkg", 0L)) return
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra("gate", pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    override fun onInterrupt() {}
}
