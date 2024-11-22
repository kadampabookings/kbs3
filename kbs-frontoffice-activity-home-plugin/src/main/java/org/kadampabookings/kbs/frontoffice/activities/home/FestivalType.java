package org.kadampabookings.kbs.frontoffice.activities.home;

import java.util.Arrays;

/**
 * @author Bruno Salmon
 */
public enum FestivalType {

    SPRING_FESTIVAL(35, HomeI18nKeys.SpringFestival, "spring-festival"),

    SUMMER_FESTIVAL(36, HomeI18nKeys.SummerFestival, "summer-festival"),

    FALL_FESTIVAL(37, HomeI18nKeys.FallFestival, "fall-festival");

    private final int typeId;
    private final String i18nKey;
    private final String styleClass;

    FestivalType(int typeId, String i18nKey, String cssClass) {
        this.typeId = typeId;
        this.i18nKey = i18nKey;
        this.styleClass = cssClass;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public String getStyleClass() {
        return styleClass;
    }

    static FestivalType fromTypeId(int typeId) {
        return Arrays.stream(FestivalType.values()).filter(ft -> ft.typeId == typeId).findFirst().orElse(null);
    }
}
