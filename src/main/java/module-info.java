module com.plociennik.copypasteanonymizer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.desktop;
    requires static lombok;
    requires jdk.compiler;
    requires org.apache.commons.lang3;

    opens com.plociennik.copypasteanonymizer to javafx.fxml;
    exports com.plociennik.copypasteanonymizer;
    exports com.plociennik.copypasteanonymizer.controller;
    opens com.plociennik.copypasteanonymizer.controller to javafx.fxml;
    exports com.plociennik.copypasteanonymizer.clipboard;
    opens com.plociennik.copypasteanonymizer.clipboard to javafx.fxml;
    exports com.plociennik.copypasteanonymizer.stage;
    opens com.plociennik.copypasteanonymizer.stage to javafx.fxml;
}