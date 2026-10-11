package se233.project1.model;

public enum ItemStatus {
    QUEUED("Queued"),
    PROCESSING("Processing"),
    READY("Ready"),
    EXPORTING("Exporting"),
    EXPORTED("Exported"),
    FAILED("Failed");

    private final String label;

    ItemStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
