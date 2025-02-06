package org.kadampabookings.kbs.backoffice.festivalcreator;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.stack.i18n.HasI18nKey;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.ui.action.Action;
import dev.webfx.stack.ui.action.tuner.ActionTuner;
import dev.webfx.stack.ui.operation.HasOperationCode;
import dev.webfx.stack.ui.operation.HasOperationExecutor;
import javafx.beans.binding.BooleanBinding;
import one.modality.base.backoffice.mainframe.fx.FXEventSelector;
import one.modality.crm.backoffice.organization.fx.FXOrganizationId;

/**
 * @author Bruno Salmon
 */
final class CreateFestivalRequest implements HasOperationCode, HasI18nKey, ActionTuner,
    HasOperationExecutor<CreateFestivalRequest, Void> {

    private static final String OPERATION_CODE = "CreateFestival";

    @Override
    public Object getI18nKey() {
        return FestivalCreatorI18nKeys.CreateFestival;
    }

    @Override
    public Object getOperationCode() {
        return OPERATION_CODE;
    }

    @Override
    public Action tuneAction(Action action) {
        // The passed action corresponds already to the CreateFestival operation (with all properties set, including
        // disabled and visible from authorizations => only authorized users will see this action).

        // But before turning it into a hyperlink (actually done in MainFrameHeaderFestivalCreatorProvider), we don't
        // want to display it all the time in the header (even for authorized users), but only on the home page and for
        // NKT only. To achieve this modified behaviour, we tune the action by overriding it with an additional disabled
        // property that is true in this case (making it also invisible when disabled).
        return Action.overrideActionWithAdditionalDisabledProperty(action, new BooleanBinding() {
            // For now, we use the event selector to detect if we are on the home page (as it's not visible on that page)
            // but it's not perfect (ex: it's not visible on the Kitchen neither)
            // TODO: replace this with a dedicated property (to create) that is true only when home page is displayed
            {
                bind(FXEventSelector.eventSelectorVisibleProperty(), FXOrganizationId.organizationIdProperty());
            }

            @Override
            protected boolean computeValue() {
                return FXEventSelector.isEventSelectorVisible()
                       || !Entities.samePrimaryKey(FXOrganizationId.getOrganizationId(), 1);  // restricted to NKT only for now (organizationId = 1) as we don't manage events other than NKT Festivals for now
            }
        });
    }

    @Override
    public AsyncFunction<CreateFestivalRequest, Void> getOperationExecutor() {
        return req -> new CreateFestivalExecutor().openNKTFestivalCreatorDialog();
    }
}
