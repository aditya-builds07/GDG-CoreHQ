package com.gdgcorehq.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.widget.ImageView;

/**
 * Reusable helper for the floating pill bottom navigation bar.
 * Wires up navigation icon clicks and highlights the currently selected item.
 * Each Activity specifies which index is currently selected so the correct
 * icon gets the "active" treatment, keeping navigation state visible across
 * every main-destination screen.
 *
 * Navigation uses FLAG_ACTIVITY_REORDER_TO_FRONT so each destination is
 * brought to the front of the existing task instead of stacking new copies.
 */
public final class BottomNavHelper {

    public static final int INDEX_NONE = -1;
    public static final int INDEX_HOME = 0;
    public static final int INDEX_TASKS = 1;
    public static final int INDEX_SCANNER = 2; // Admin
    public static final int INDEX_AGENDA = 2;  // Member
    public static final int INDEX_EXPENSES = 3;
    public static final int INDEX_STRIKES = 4; // Admin
    public static final int INDEX_PROFILE = 4; // Member

    private BottomNavHelper() { /* utility class */ }

    /**
     * Represents a single navigation item with its view, icon tint behavior,
     * and the Activity it should navigate to.
     */
    public static class NavItem {
        public final ImageView view;
        public final Class<? extends Activity> target;

        public NavItem(ImageView view, Class<? extends Activity> target) {
            this.view = view;
            this.target = target;
        }
    }

    /**
     * Set up the bottom pill nav bar.
     *
     * @param activity       the host Activity
     * @param selectedIndex  zero-based index of the currently active nav item (-1 for none)
     * @param items          array of NavItems in display order
     */
    public static void setup(Activity activity, int selectedIndex, NavItem... items) {
        if (activity == null || items == null) return;

        for (int i = 0; i < items.length; i++) {
            NavItem item = items[i];
            if (item == null || item.view == null) continue;

            if (i == selectedIndex) {
                // Highlight the active item
                item.view.setBackgroundResource(R.drawable.bg_nav_selected);
                item.view.setColorFilter(activity.getColor(R.color.surface));
                item.view.setOnClickListener(null);
            } else {
                // Deselect — remove background, set muted tint
                item.view.setBackground(null);
                item.view.setColorFilter(activity.getColor(R.color.text_secondary));

                // Attach click listener for non-selected items
                if (item.target != null) {
                    item.view.setOnClickListener(v -> {
                        Intent intent = new Intent(activity, item.target);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        activity.startActivity(intent);
                        // Disable the default transition animation for tab-like behavior
                        if (Build.VERSION.SDK_INT >= 34) {
                            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0);
                        } else {
                            activity.overridePendingTransition(0, 0);
                        }
                    });
                } else {
                    item.view.setOnClickListener(null);
                }
            }
        }
    }

    /**
     * Setup bottom navigation for Super Admin destinations:
     * 0: AdminDashboardActivity (Home)
     * 1: AdminTasksActivity (Tasks)
     * 2: QrScannerActivity (QR Scanner)
     * 3: ReimbursementReviewActivity (Reimbursements)
     * 4: StrikeDashboardActivity (Strikes)
     */
    public static void setupAdminNav(Activity activity, int selectedIndex,
                                     ImageView navHome, ImageView navTasks,
                                     ImageView navScanner, ImageView navExpenses,
                                     ImageView navStrikes) {
        setup(activity, selectedIndex,
                new NavItem(navHome, AdminDashboardActivity.class),
                new NavItem(navTasks, AdminTasksActivity.class),
                new NavItem(navScanner, QrScannerActivity.class),
                new NavItem(navExpenses, ReimbursementReviewActivity.class),
                new NavItem(navStrikes, StrikeDashboardActivity.class)
        );
    }

    /**
     * Setup bottom navigation for Core Member destinations:
     * 0: MemberDashboardActivity (Home)
     * 1: MemberTasksActivity (Tasks)
     * 2: MemberAgendaActivity (Agendas)
     * 3: ExpenseHistoryActivity (Expenses)
     * 4: ProfileActivity (Profile)
     */
    public static void setupMemberNav(Activity activity, int selectedIndex,
                                      ImageView navHome, ImageView navTasks,
                                      ImageView navAgenda, ImageView navExpenses,
                                      ImageView navProfile) {
        setup(activity, selectedIndex,
                new NavItem(navHome, MemberDashboardActivity.class),
                new NavItem(navTasks, MemberTasksActivity.class),
                new NavItem(navAgenda, MemberAgendaActivity.class),
                new NavItem(navExpenses, ExpenseHistoryActivity.class),
                new NavItem(navProfile, ProfileActivity.class)
        );
    }
}
