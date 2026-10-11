package se233.project1.model;

public enum ColorMode {
    UNLIMITED("Unlimited"),
    CUSTOM("Custom");

    private final String label;

    ColorMode(String label) { this.label = label; }

    public String getLabel() { return label; }
}
