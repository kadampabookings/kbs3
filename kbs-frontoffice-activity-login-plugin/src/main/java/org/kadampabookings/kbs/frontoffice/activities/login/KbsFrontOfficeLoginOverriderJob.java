package org.kadampabookings.kbs.frontoffice.activities.login;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.util.function.Factory;
import dev.webfx.stack.orm.domainmodel.activity.viewdomain.impl.ViewDomainActivityContextFinal;
import dev.webfx.stack.routing.activity.Activity;
import one.modality.crm.client.activities.login.LoginRouting;

/**
 * @author Bruno Salmon
 */
public class KbsFrontOfficeLoginOverriderJob implements ApplicationJob {

    @Override
    public void onInit() {
        Factory<Activity<ViewDomainActivityContextFinal>> kbsLoginActivityFactory = KbsFrontOfficeLoginActivity::new;
        LoginRouting.LOGIN_ACTIVITY_FACTORY = kbsLoginActivityFactory;
    }
}
