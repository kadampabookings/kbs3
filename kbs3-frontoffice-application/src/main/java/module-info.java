// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs3.frontoffice.application {

    // Direct dependencies modules
    requires javafx.controls;
    requires javafx.graphics;
    requires modality.base.client.application;
    requires webfx.extras.flexbox;
    requires webfx.platform.os;
    requires webfx.stack.ui.action;

    // Exported packages
    exports org.kadampabookings.kbs3.frontoffice;

    // Provided services
    provides javafx.application.Application with org.kadampabookings.kbs3.frontoffice.KBS3FrontOfficeApplication;

}