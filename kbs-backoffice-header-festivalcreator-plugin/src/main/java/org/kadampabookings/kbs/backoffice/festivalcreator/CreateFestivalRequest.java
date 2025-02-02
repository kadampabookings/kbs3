package org.kadampabookings.kbs.backoffice.festivalcreator;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.stack.i18n.HasI18nKey;
import dev.webfx.stack.ui.operation.HasOperationCode;
import dev.webfx.stack.ui.operation.HasOperationExecutor;

/**
 * @author Bruno Salmon
 */
final class CreateFestivalRequest implements HasOperationCode, HasI18nKey,
    HasOperationExecutor<CreateFestivalRequest, Void>  {

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
    public AsyncFunction<CreateFestivalRequest, Void> getOperationExecutor() {
        return req -> new CreateFestivalExecutor().openNKTFestivalCreatorDialog();
    }
}
