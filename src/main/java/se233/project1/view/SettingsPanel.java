package se233.project1.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import se233.project1.model.ColorAnalysis;
import se233.project1.model.ColorMode;
import se233.project1.model.DetailLevel;
import se233.project1.model.VectorSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Right-hand panel: export, detail level, colors, background removal and image navigation. */
public class SettingsPanel extends VBox {
    private final Button exportAllButton = new Button("Export All");
    private final Label exportModeLabel = new Label();
    private final ToggleGroup detailGroup = new ToggleGroup();
    private final ToggleGroup colorModeGroup = new ToggleGroup();
    private final ToggleGroup colorCountGroup = new ToggleGroup();
    private final List<ToggleButton> colorCountButtons = new ArrayList<>();
    private final Label detectedLabel = new Label();
    private final FlowPane swatches = new FlowPane(4, 4);
    private final CheckBox removeBackground = new CheckBox("Remove background");
    private final Button applyToAllButton = new Button("Apply settings to all images");
    private final Button backButton = new Button("\u2190 Back");
    private final Button nextButton = new Button("Next \u2192");
    private final Label positionLabel = new Label();
    private final VBox settingsBox;
    private Consumer<VectorSettings> onSettingsChanged = s -> {
    };
    private boolean updating;

    public SettingsPanel() {
        getStyleClass().add("side-panel");
        setSpacing(8);
        setPrefWidth(270);
        setMinWidth(250);

        exportAllButton.getStyleClass().add("primary-button");
        exportAllButton.setMaxWidth(Double.MAX_VALUE);
        exportModeLabel.getStyleClass().add("export-mode");
        exportModeLabel.setWrapText(true);

        HBox detailButtons = segmented(detailGroup,
                toggle("Low", DetailLevel.LOW), toggle("Medium", DetailLevel.MEDIUM), toggle("High", DetailLevel.HIGH));
        HBox colorModeButtons = segmented(colorModeGroup,
                toggle("Unlimited", ColorMode.UNLIMITED), toggle("Custom", ColorMode.CUSTOM));
        for (int n = 1; n <= VectorSettings.MAX_CUSTOM_COLORS; n++) {
            colorCountButtons.add(toggle(Integer.toString(n), n));
        }
        HBox countButtons = segmented(colorCountGroup, colorCountButtons.toArray(new ToggleButton[0]));
        Label countLabel = new Label("Number of colors");
        countLabel.getStyleClass().add("field-label");
        VBox customBox = new VBox(4, countLabel, countButtons);
        colorModeGroup.selectedToggleProperty().addListener((o, oldToggle, newToggle) ->
                customBox.setDisable(newToggle == null || newToggle.getUserData() != ColorMode.CUSTOM));
        detectedLabel.getStyleClass().add("hint");
        detectedLabel.setWrapText(true);
        swatches.getStyleClass().add("swatches");

        applyToAllButton.setMaxWidth(Double.MAX_VALUE);
        applyToAllButton.getStyleClass().add("secondary-button");

        settingsBox = new VBox(8,
                sectionTitle("Improve result"),
                fieldLabel("Detail level"), detailButtons,
                fieldLabel("Colors"), colorModeButtons, customBox, detectedLabel, swatches,
                sectionTitle("Advanced"), removeBackground, applyToAllButton);

        backButton.setMaxWidth(Double.MAX_VALUE);
        nextButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(backButton, Priority.ALWAYS);
        HBox.setHgrow(nextButton, Priority.ALWAYS);
        HBox navigation = new HBox(6, backButton, nextButton);
        positionLabel.getStyleClass().add("position-label");
        positionLabel.setMaxWidth(Double.MAX_VALUE);
        positionLabel.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(exportAllButton, exportModeLabel, settingsBox, spacer, navigation, positionLabel);

        detailGroup.selectedToggleProperty().addListener((o, a, b) -> fireChange());
        colorModeGroup.selectedToggleProperty().addListener((o, a, b) -> fireChange());
        colorCountGroup.selectedToggleProperty().addListener((o, a, b) -> fireChange());
        removeBackground.selectedProperty().addListener((o, a, b) -> fireChange());
    }

    public void setOnSettingsChanged(Consumer<VectorSettings> listener) { onSettingsChanged = listener; }

    /** Shows an image's settings and color detection without firing change events. */
    public void showSettings(VectorSettings settings, ColorAnalysis analysis) {
        updating = true;
        try {
            if (settings == null || analysis == null) {
                settingsBox.setDisable(true);
                detectedLabel.setText("Detecting colors…");
                swatches.getChildren().clear();
                return;
            }
            int limit = analysis.getCustomColorLimit();
            for (ToggleButton b : colorCountButtons) {
                b.setDisable((Integer) b.getUserData() > limit);
            }
            select(detailGroup, settings.getDetail());
            select(colorModeGroup, settings.getColorMode());
            select(colorCountGroup, settings.getCustomColors());
            removeBackground.setSelected(settings.isRemoveBackground());

            int detected = analysis.getDetectedColors();
            detectedLabel.setText(detected > VectorSettings.MAX_CUSTOM_COLORS
                    ? "Detected " + detected + " colors — custom palette capped at " + VectorSettings.MAX_CUSTOM_COLORS
                    : "Detected " + detected + (detected == 1 ? " color" : " colors"));
            swatches.getChildren().clear();
            analysis.getDominantColors().stream().limit(12).forEach(rgb -> {
                Rectangle r = new Rectangle(16, 16, Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                r.getStyleClass().add("swatch");
                swatches.getChildren().add(r);
            });
        } finally {
            updating = false;
        }
    }

    public void setSettingsDisabled(boolean disabled) { settingsBox.setDisable(disabled); }

    public void setPosition(int index, int count) {
        positionLabel.setText(count == 0 ? "" : "Image " + (index + 1) + " of " + count);
        backButton.setDisable(index <= 0);
        nextButton.setDisable(index >= count - 1);
    }

    public void setExportMode(boolean parallel, int count) {
        exportModeLabel.getStyleClass().removeAll("parallel", "sequential");
        if (count <= 1) {
            exportModeLabel.setText("Exports " + count + " image");
        } else if (parallel) {
            exportModeLabel.getStyleClass().add("parallel");
            exportModeLabel.setText("Parallel export of " + count + " images (Medium detail + Custom colors)");
        } else {
            exportModeLabel.getStyleClass().add("sequential");
            exportModeLabel.setText("Sequential export of " + count
                    + " images: parallel needs Medium detail + Custom colors on every image");
        }
    }

    public Button getExportAllButton() { return exportAllButton; }
    public Button getApplyToAllButton() { return applyToAllButton; }
    public Button getBackButton() { return backButton; }
    public Button getNextButton() { return nextButton; }

    private void fireChange() {
        if (updating) {
            return;
        }
        Toggle d = detailGroup.getSelectedToggle(), m = colorModeGroup.getSelectedToggle(), c = colorCountGroup.getSelectedToggle();
        if (d == null || m == null || c == null) {
            return;
        }
        onSettingsChanged.accept(new VectorSettings((DetailLevel) d.getUserData(), (ColorMode) m.getUserData(),
                (Integer) c.getUserData(), removeBackground.isSelected()));
    }

    private static void select(ToggleGroup group, Object value) {
        for (Toggle t : group.getToggles()) {
            if (t.getUserData().equals(value)) {
                group.selectToggle(t);
                return;
            }
        }
    }

    private static ToggleButton toggle(String text, Object value) {
        ToggleButton b = new ToggleButton(text);
        b.setUserData(value);
        b.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(b, Priority.ALWAYS);
        return b;
    }

    /** A row of toggle buttons where exactly one is always selected. */
    private static HBox segmented(ToggleGroup group, ToggleButton... buttons) {
        HBox box = new HBox();
        box.getStyleClass().add("segmented");
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setToggleGroup(group);
            buttons[i].getStyleClass().add(i == 0 ? "first" : i == buttons.length - 1 ? "last" : "middle");
            box.getChildren().add(buttons[i]);
        }
        group.selectedToggleProperty().addListener((o, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                group.selectToggle(oldToggle);
            }
        });
        return box;
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }
}
