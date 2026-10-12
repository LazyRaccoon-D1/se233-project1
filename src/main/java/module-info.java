module se233.project1 {
    requires javafx.controls;

    opens se233.project1 to javafx.graphics;
    exports se233.project1;
    exports se233.project1.controller;
    exports se233.project1.exception;
    exports se233.project1.model;
    exports se233.project1.view;
}
