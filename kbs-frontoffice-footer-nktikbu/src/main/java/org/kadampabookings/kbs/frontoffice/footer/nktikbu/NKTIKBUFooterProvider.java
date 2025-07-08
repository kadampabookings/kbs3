package org.kadampabookings.kbs.frontoffice.footer.nktikbu;

import dev.webfx.platform.resource.Resource;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import one.modality.base.frontoffice.mainframe.footernode.MainFrameFooterNodeProvider;

/**
 * @author Bruno Salmon
 */
public final class NKTIKBUFooterProvider implements MainFrameFooterNodeProvider {

    @Override
    public Node getFooterNode() {
        return new ImageView(new Image(Resource.toUrl("NKT-IKBU.svg", getClass()), true));
    }

}
