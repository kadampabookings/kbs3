package org.kadampabookings.kbs.client.festivaltypes;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.EntityStoreQuery;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import one.modality.base.shared.entities.Event;


/**
 * @author Bruno Salmon
 */
public final class FXFestivals {

    private static final ObservableList<Event> LAST_FESTIVALS = FXCollections.observableArrayList(); // Will hold last Spring, Summer & Fall festivals

    private static final ObjectProperty<Event> LAST_SPRING_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();
    private static final ObjectProperty<Event> LAST_SUMMER_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();
    private static final ObjectProperty<Event> LAST_FALL_FESTIVAL_PROPERTY = new SimpleObjectProperty<>();

    public static ObservableList<Event> lastFestivals() {
        return LAST_FESTIVALS;
    }

    public static ReadOnlyObjectProperty<Event> lastSpringFestivalProperty() {
        return LAST_SPRING_FESTIVAL_PROPERTY;
    }

    public static Event getLastSpringFestival() {
        return LAST_SPRING_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastSummerFestivalProperty() {
        return LAST_SUMMER_FESTIVAL_PROPERTY;
    }

    public static Event getLastSummerFestival() {
        return LAST_SUMMER_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastFallFestivalProperty() {
        return LAST_FALL_FESTIVAL_PROPERTY;
    }

    public static Event getLastFallFestival() {
        return LAST_FALL_FESTIVAL_PROPERTY.get();
    }

    public static ReadOnlyObjectProperty<Event> lastFestivalProperty(FestivalType festivalType) {
        if (festivalType != null) {
            switch (festivalType) {
                case SPRING_FESTIVAL: return lastSpringFestivalProperty();
                case SUMMER_FESTIVAL: return lastSummerFestivalProperty();
                case FALL_FESTIVAL:   return lastFallFestivalProperty();
            }
        }
        return null;
    }

    private static void loadLastFestivals() {
        EntityStore entityStore = EntityStore.create(DataSourceModelService.getDefaultDataSourceModel());
        String select = "select name,type.name,startDate,endDate,kbs3,live,openingDate from Event where type in (?) order by startDate desc, name like '%Online%' ? 1 : 0 limit 1";
        entityStore.executeQueryBatch(
                new EntityStoreQuery(select, new Object[] { FestivalType.SPRING_FESTIVAL.getTypeId() }),
                new EntityStoreQuery(select, new Object[] { FestivalType.SUMMER_FESTIVAL.getTypeId() }),
                new EntityStoreQuery(select, new Object[] { FestivalType.FALL_FESTIVAL.getTypeId() }))
            .onFailure(Console::log)
            .onSuccess(festivalsLists -> UiScheduler.runInUiThread(() -> {
                LAST_SPRING_FESTIVAL_PROPERTY.set((Event) Collections.first(festivalsLists[0])); // Spring Festival
                LAST_SUMMER_FESTIVAL_PROPERTY.set((Event) Collections.first(festivalsLists[1])); // Summer Festival
                LAST_FALL_FESTIVAL_PROPERTY.set((Event) Collections.first(festivalsLists[2]));  // Fall Festival
                LAST_FESTIVALS.setAll(getLastSpringFestival(), getLastSummerFestival(), getLastFallFestival());
            }));
    }

    public static void init() {
    }

    static {
        loadLastFestivals();
    }
}
