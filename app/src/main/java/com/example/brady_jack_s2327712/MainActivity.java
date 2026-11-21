//
// Name                 Jack Brady
// Student ID           S2327712
// Programme of Study   Software Development
//

package com.example.brady_jack_s2327712;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.brady_jack_s2327712.model.SituationCategory;
import com.example.brady_jack_s2327712.ui.SituationDetailFragment;
import com.example.brady_jack_s2327712.ui.SituationListFragment;
import com.example.brady_jack_s2327712.ui.SituationMapFragment;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements SituationListFragment.OnSituationSelectedListener, SituationDetailFragment.OnShowOnMapListener {

    private TrafficViewModel viewModel;
    // NavigationBarView covers both the bottom bar (portrait) and side bar (landscape)
    private NavigationBarView bottomNav;
    // True in landscape, where the detail has its own pane simultaneous to the
    // situation list
    private boolean twoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;   // handled here, so children don't pad again
        });

        // Only the landscape layout has a detail pane
        twoPane = findViewById(R.id.detail_container) != null;

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            clearBackStack();   // Leave the detail screen when switching tabs
            if (id == R.id.nav_list) {
                showFragment(new SituationListFragment());
            } else if (id == R.id.nav_map) {
                showFragment(new SituationMapFragment());
            }
            return true;   // true = show the item as selected
        });
        // Selecting "List" again while on the detail screen returns to the list
        bottomNav.setOnItemReselectedListener(item -> clearBackStack());

        // Show the toolbar back arrow only when there is a screen to go back to
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateUpArrow);
        updateUpArrow();

        if (savedInstanceState == null) {
            showFragment(new SituationListFragment());
        }
        setUpPanes();
    }


    /**
     * Makes the screen match the layout Android picked after a rotation, so the user keeps
     * seeing the same thing:
     * - landscape, list/map on the left, details always on the right
     * - portrait, one screen at a time, if details were showing, they stay showing
     */
    private void setUpPanes() {
        FragmentManager fm = getSupportFragmentManager();
        if (twoPane) {
            // Coming from portrait on the detail screen, put the list/map back on the left
            if (fm.getBackStackEntryCount() > 0) clearBackStack();
            // The right pane always holds the detail fragment (it shows a hint until something is selected)
            if (fm.findFragmentById(R.id.detail_container) == null) {
                fm.beginTransaction().replace(R.id.detail_container, new SituationDetailFragment()).commit();
            }
        } else {
            // Going from landscape to portrait, the right panel fragment is removed
            Fragment rightPane = fm.findFragmentById(R.id.detail_container);
            if (rightPane != null) {
                fm.beginTransaction().remove(rightPane).commitNow();
                // Keep showing the selected situation, if one was selected
                if (viewModel.getSelected().getValue() != null) {
                    onSituationSelected();
                }
            }
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }


    // Called by the list (and the map) through the interface when a situation is hit
    @Override
    public void onSituationSelected() {
        // In landscape the detail pane is already showing and follows the selection
        if (twoPane) return;
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SituationDetailFragment()).addToBackStack("detail")   // the Back button returns to the list
                .commit();
    }


    @Override
    public void onShowOnMap() {
        viewModel.requestMapFocus(); // Map zooms in on selected situation
        if (bottomNav.getSelectedItemId() == R.id.nav_map) {
            clearBackStack();
            if (twoPane) showFragment(new SituationMapFragment()); // revert zoom
        } else {
            bottomNav.setSelectedItemId(R.id.nav_map);   // switches tab (runs the listener above)
        }
    }

    private void clearBackStack() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void updateUpArrow() {
        boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);
        }
    }

    // The toolbar's back arrow behaves like the Back button
    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    // --- Options menu ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh || id == R.id.menu_all_data) {
            viewModel.refresh();
        } else if (id == R.id.action_key) {
            showIconKey();
        } else if (id == R.id.menu_incidents_1) {
            viewModel.loadExampleFile(R.raw.unplanned_example, false, "unplanned_example.xml");
        } else if (id == R.id.menu_incidents_2) {
            viewModel.loadExampleFile(R.raw.unplanned_example_2, false, "unplanned_example_2.xml");
        } else if (id == R.id.menu_roadworks_1) {
            viewModel.loadExampleFile(R.raw.roadworks_example, true, "roadworks_example.xml");
        } else if (id == R.id.menu_roadworks_2) {
            viewModel.loadExampleFile(R.raw.roadworks_example_2, true, "roadworks_example_2.xml");
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }


    // AlertDialog with a custom layout listing every category's icon and colour
    private void showIconKey() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        list.setPadding(padding, padding / 2, padding, 0);

        for (SituationCategory category : SituationCategory.values()) {
            View row = getLayoutInflater().inflate(R.layout.item_legend_row, list, false);
            ImageView rowIcon = row.findViewById(R.id.legendIcon);
            rowIcon.setImageResource(category.getIconRes());
            rowIcon.setColorFilter(ContextCompat.getColor(this, category.getColourRes()));
            ((TextView) row.findViewById(R.id.legendLabel)).setText(category.getLabel());
            list.addView(row);
        }

        TextView credit = new TextView(this);
        credit.setText(R.string.icon_credit);
        credit.setTextSize(12);
        credit.setPadding(0, padding / 2, 0, 0);
        list.addView(credit);

        new AlertDialog.Builder(this).setTitle(R.string.icon_key).setView(list).setPositiveButton(android.R.string.ok, null).show();
    }
}