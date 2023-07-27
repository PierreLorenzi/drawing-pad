package fr.alphonse.drawingpad.document.utils;

import fr.alphonse.drawingpad.data.model.value.Graduation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Graduations {

    public static boolean isStrictlyLesserThanOne(Graduation graduation) {
        if (graduation == null) {
            return false;
        }
        return switch (graduation) {
            case ZERO -> true;
            case LOWEST -> true;
            case LOWER -> true;
            case ONE -> false;
            case GREATER -> false;
            case GREATEST -> false;
            case INFINITY -> false;
        };
    }

    public static boolean isStrictlyGreaterThanOne(Graduation graduation) {
        if (graduation == null) {
            return false;
        }
        return switch (graduation) {
            case ZERO -> false;
            case LOWEST -> false;
            case LOWER -> false;
            case ONE -> false;
            case GREATER -> true;
            case GREATEST -> true;
            case INFINITY -> true;
        };
    }
}
