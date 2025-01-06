package org.kadampabookings.kbs.client.festivaltypes;

import java.util.Arrays;

/**
 * @author Bruno Salmon
 */
public enum FestivalType {

    SPRING_FESTIVAL(35, FestivalTypeI18nKeys.SpringFestival, FestivalTypeI18nKeys.Spring, "spring-festival"),

    SUMMER_FESTIVAL(36, FestivalTypeI18nKeys.SummerFestival, FestivalTypeI18nKeys.Summer, "summer-festival"),

    FALL_FESTIVAL(37, FestivalTypeI18nKeys.FallFestival, FestivalTypeI18nKeys.Fall, "fall-festival");

    private final int typeId;
    private final String longI18nKey;
    private final String shortI18nKey;
    private final String styleClass;

    FestivalType(int typeId, String longI18nKey, String shortI18nKey, String cssClass) {
        this.typeId = typeId;
        this.longI18nKey = longI18nKey;
        this.shortI18nKey = shortI18nKey;
        this.styleClass = cssClass;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getLongI18nKey() {
        return longI18nKey;
    }

    public String getShortI18nKey() {
        return shortI18nKey;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public static FestivalType fromTypeId(int typeId) {
        return Arrays.stream(FestivalType.values()).filter(ft -> ft.typeId == typeId).findFirst().orElse(null);
    }
}
