//
// Name                 Jack Brady
// Student ID           S2327712
// Programme of Study   Software Development
//

package com.example.brady_jack_s2327712;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.brady_jack_s2327712.ui.SituationListFragment;
import com.example.brady_jack_s2327712.ui.SituationMapFragment;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private TrafficViewModel viewModel;

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_list) {
                showFragment(new SituationListFragment());
            } else if (id == R.id.nav_map) {
                showFragment(new SituationMapFragment());
            }
            return true;   // true = show the item as selected
        });

        if (savedInstanceState == null) {
            showFragment(new SituationListFragment());
        }
    }

    /** Fragment transaction: put this fragment into the placeholder. */
    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

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
}