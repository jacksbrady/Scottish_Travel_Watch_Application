//
// Name                 Jack Brady
// Student ID           S2327712
// Programme of Study   Software Development
//

package com.example.brady_jack_s2327712;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.brady_jack_s2327712.model.Situation;
import com.example.brady_jack_s2327712.model.SituationCategory;
import com.example.brady_jack_s2327712.viewmodel.TrafficViewModel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Test to prove parsing successful
public class MainActivity extends AppCompatActivity implements OnClickListener {

    private TextView statusDisplay;
    private TextView rawDataDisplay;
    private Button refreshButton;
    private Button incidents1Button;
    private Button incidents2Button;
    private Button roadworks1Button;
    private Button roadworks2Button;
    private TrafficViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusDisplay = findViewById(R.id.salutation);
        rawDataDisplay = findViewById(R.id.rawDataDisplay);
        refreshButton = findViewById(R.id.startButton);
        incidents1Button = findViewById(R.id.incidents1Button);
        incidents2Button = findViewById(R.id.incidents2Button);
        roadworks1Button = findViewById(R.id.roadworks1Button);
        roadworks2Button = findViewById(R.id.roadworks2Button);

        refreshButton.setOnClickListener(this);
        incidents1Button.setOnClickListener(this);
        incidents2Button.setOnClickListener(this);
        roadworks1Button.setOnClickListener(this);
        roadworks2Button.setOnClickListener(this);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        viewModel.getStatus().observe(this, status -> statusDisplay.setText(status));
        viewModel.isLoading().observe(this, this::setButtonsEnabled);
        viewModel.getSituations().observe(this, this::showPreview);
    }

    @Override
    public void onClick(View view) {
        if (view == refreshButton) {
            viewModel.refresh();
        } else if (view == incidents1Button) {
            viewModel.loadExampleFile(R.raw.unplanned_example, false, "unplanned_example.xml");
        } else if (view == incidents2Button) {
            viewModel.loadExampleFile(R.raw.unplanned_example_2, false, "unplanned_example_2.xml");
        } else if (view == roadworks1Button) {
            viewModel.loadExampleFile(R.raw.roadworks_example, true, "roadworks_example.xml");
        } else if (view == roadworks2Button) {
            viewModel.loadExampleFile(R.raw.roadworks_example_2, true, "roadworks_example_2.xml");
        }
    }

    private void setButtonsEnabled(boolean loading) {
        refreshButton.setEnabled(!loading);
        incidents1Button.setEnabled(!loading);
        incidents2Button.setEnabled(!loading);
        roadworks1Button.setEnabled(!loading);
        roadworks2Button.setEnabled(!loading);
    }

    private void showPreview(List<Situation> situations) {
        Map<SituationCategory, Integer> counts = new EnumMap<>(SituationCategory.class);
        int missingRoad = 0;
        int missingPoints = 0;
        for (Situation s : situations) {
            counts.merge(s.getCategory(), 1, Integer::sum);
            if (s.getRoadName() == null) missingRoad++;
            if (s.getPoints().size() < 2) missingPoints++;
        }

        StringBuilder sb = new StringBuilder("By category:\n");
        for (Map.Entry<SituationCategory, Integer> entry : counts.entrySet()) {
            sb.append("   ").append(entry.getKey().getLabel())
                    .append(": ").append(entry.getValue()).append('\n');
        }
        sb.append("Missing road name: ").append(missingRoad).append('\n');
        sb.append("Missing from/to coordinates: ").append(missingPoints).append("\n\n");

        int shown = Math.min(10, situations.size());
        sb.append("First ").append(shown).append(" situations:\n\n");
        for (int i = 0; i < shown; i++) {
            sb.append(situations.get(i).toString()).append("\n\n");
        }
        rawDataDisplay.setText(sb.toString());
    }
}