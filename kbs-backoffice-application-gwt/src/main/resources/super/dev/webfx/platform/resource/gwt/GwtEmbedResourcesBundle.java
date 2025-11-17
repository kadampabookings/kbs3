// File managed by WebFX (DO NOT EDIT MANUALLY)
package dev.webfx.platform.resource.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import dev.webfx.platform.resource.spi.impl.web.WebResourceBundleBase;

public interface GwtEmbedResourcesBundle extends ClientBundle {

    GwtEmbedResourcesBundle R = GWT.create(GwtEmbedResourcesBundle.class);
    @Source("dev/webfx/extras/i18n/de.properties")
    TextResource r1();

    @Source("dev/webfx/extras/i18n/en.json")
    TextResource r2();

    @Source("dev/webfx/extras/i18n/es.properties")
    TextResource r3();

    @Source("dev/webfx/extras/i18n/fr.properties")
    TextResource r4();

    @Source("dev/webfx/extras/i18n/pt.properties")
    TextResource r5();

    @Source("dev/webfx/extras/i18n/vi.properties")
    TextResource r6();

    @Source("dev/webfx/extras/i18n/zh.properties")
    TextResource r7();

    @Source("dev/webfx/platform/conf/src-root.properties")
    TextResource r8();

    @Source("dev/webfx/platform/meta/exe/exe.properties")
    TextResource r9();

    @Source("one/modality/base/shared/domainmodel/DomainModelSnapshot.json")
    TextResource r10();



    final class ProvidedGwtResourceBundle extends WebResourceBundleBase {
        public ProvidedGwtResourceBundle() {
            registerResource("dev/webfx/extras/i18n/de.properties", () -> R.r1().getText());
            registerResource("dev/webfx/extras/i18n/en.json", () -> R.r2().getText());
            registerResource("dev/webfx/extras/i18n/es.properties", () -> R.r3().getText());
            registerResource("dev/webfx/extras/i18n/fr.properties", () -> R.r4().getText());
            registerResource("dev/webfx/extras/i18n/pt.properties", () -> R.r5().getText());
            registerResource("dev/webfx/extras/i18n/vi.properties", () -> R.r6().getText());
            registerResource("dev/webfx/extras/i18n/zh.properties", () -> R.r7().getText());
            registerResource("dev/webfx/platform/conf/src-root.properties", () -> R.r8().getText());
            registerResource("dev/webfx/platform/meta/exe/exe.properties", () -> R.r9().getText());
            registerResource("one/modality/base/shared/domainmodel/DomainModelSnapshot.json", () -> R.r10().getText());

        }
    }
}
