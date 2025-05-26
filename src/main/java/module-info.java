module com.plociennik.copypasteanonymizer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.desktop;

    opens com.plociennik.copypasteanonymizer to javafx.fxml;
    exports com.plociennik.copypasteanonymizer;
}