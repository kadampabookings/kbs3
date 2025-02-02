package org.kadampabookings.kbs.backoffice.festivalcreator;

import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.ui.action.Action;
import dev.webfx.stack.ui.action.ActionBinder;
import dev.webfx.stack.ui.controls.button.ButtonFactoryMixin;
import dev.webfx.stack.ui.operation.action.OperationActionFactoryMixin;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import one.modality.base.backoffice.mainframe.fx.FXEventSelector;
import one.modality.base.backoffice.mainframe.headernode.MainFrameHeaderNodeProvider;
import one.modality.crm.backoffice.organization.fx.FXOrganizationId;

/**
 * @author Bruno Salmon
 */
public class MainFrameHeaderFestivalCreatorProvider implements MainFrameHeaderNodeProvider {

    @Override
    public String getName() {
        return "festivalCreator";
    }

    @Override
    public Node getHeaderNode(ButtonFactoryMixin buttonFactory, Pane frameContainer, DataSourceModel dataSourceModel) {
        // Getting the action corresponding to the CreateFestival operation (with all properties set, including disabled
        // and visible from authorizations => only authorized users will see this action).
        Action createFestivalAction = ((OperationActionFactoryMixin) buttonFactory).newOperationAction(CreateFestivalRequest::new);
        // We will turn it into a hyperlink, however, even for authorized users, we don't want to display it all the time
        // in the header, but only on the home page and for NKT only. To achieve this modified behaviour, we override the
        // action with an additional disabled property that is true in this case (making it also invisible when disabled).
        createFestivalAction = Action.overrideActionWithAdditionalDisabledProperty(createFestivalAction, new BooleanBinding() {
            // For now, we use the event selector to detect if we are on the home page (as it's not visible on that page)
            // but it's not perfect (ex: it's not visible on the Kitchen neither)
            // TODO: replace this with a dedicated property (to create) that is true only when home page is displayed
            { bind(FXEventSelector.eventSelectorVisibleProperty(), FXOrganizationId.organizationIdProperty()); }
            @Override
            protected boolean computeValue() {
                return FXEventSelector.isEventSelectorVisible()
                  || !Numbers.identicalObjectsOrNumberValues(Entities.getPrimaryKey(FXOrganizationId.getOrganizationId()), 1);  // restricted to NKT only for now (organizationId = 1) as we don't manage events other than NKT Festivals for now
            }
        });
        // Now that the action behaves like we want, we can create the hyperlink
        return ActionBinder.newActionHyperlink(createFestivalAction);
    }

}
