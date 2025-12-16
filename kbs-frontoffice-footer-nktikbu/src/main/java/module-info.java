// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.footer.nktikbu {

    // Direct dependencies modules
    requires javafx.graphics;
    requires modality.base.frontoffice.mainframe.footernode;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.footer.nktikbu;

    // Resources packages
    opens org.kadampabookings.kbs.frontoffice.footer.nktikbu;

    // Provided services
    provides one.modality.base.frontoffice.mainframe.footernode.MainFrameFooterNodeProvider with org.kadampabookings.kbs.frontoffice.footer.nktikbu.NKTIKBUFooterProvider;

}