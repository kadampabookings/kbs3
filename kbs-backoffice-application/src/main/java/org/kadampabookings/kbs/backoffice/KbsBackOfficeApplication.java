package org.kadampabookings.kbs.backoffice;

import one.modality.base.client.application.ModalityClientApplication;

/**
 * @author Bruno Salmon
 */
public class KbsBackOfficeApplication extends ModalityClientApplication {

    public KbsBackOfficeApplication() {
        super(new ModalityBackOfficeStarterActivity());
    }
}
