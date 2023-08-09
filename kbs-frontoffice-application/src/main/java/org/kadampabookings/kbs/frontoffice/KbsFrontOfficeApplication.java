package org.kadampabookings.kbs.frontoffice;

import one.modality.base.client.application.ModalityClientApplication;

/**
 * @author Bruno Salmon
 */
public class KbsFrontOfficeApplication extends ModalityClientApplication {

    public KbsFrontOfficeApplication() {
        super(new ModalityFrontOfficeStarterActivity());
    }
}
