package org.kadampabookings.kbs.backoffice;

import one.modality.base.client.application.ModalityClientApplication;

/**
 * @author Bruno Salmon
 */
public class KBSBackOfficeApplication extends ModalityClientApplication {

    public KBSBackOfficeApplication() {
        super(new ModalityBackOfficeStarterActivity());
    }
}
