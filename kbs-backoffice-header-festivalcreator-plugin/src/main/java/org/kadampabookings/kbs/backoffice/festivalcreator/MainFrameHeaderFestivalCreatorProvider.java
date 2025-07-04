package org.kadampabookings.kbs.backoffice.festivalcreator;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.extras.action.Action;
import dev.webfx.extras.action.ActionBinder;
import dev.webfx.extras.controlfactory.button.ButtonFactoryMixin;
import dev.webfx.extras.operation.action.OperationActionFactoryMixin;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import one.modality.base.backoffice.mainframe.headernode.MainFrameHeaderNodeProvider;

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
        // Getting the action corresponding to the CreateFestival operation. See CreateFestivalRequest to see how it
        // is tuned => only visible on the NKT home page for authorized users.
        Action createFestivalAction = ((OperationActionFactoryMixin) buttonFactory).newOperationAction(CreateFestivalRequest::new);
        // We create a hyperlink from it
        return ActionBinder.newActionHyperlink(createFestivalAction);
    }

}
