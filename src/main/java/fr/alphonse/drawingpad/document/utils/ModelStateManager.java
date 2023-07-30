package fr.alphonse.drawingpad.document.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class ModelStateManager {

    private static final List<Class<?>> VALUE_CLASSES = List.of(Number.class, String.class, Enum.class);

    public static boolean areDeepEqual(Object model1, Object model2) {

        if (isNull(model1) && isNull(model2)) {
            return true;
        }
        if (isNull(model1) || isNull(model2)) {
            return false;
        }

        Class<?> type = model1.getClass();
        if (!type.equals(model2.getClass())) {
            return false;
        }

        if (isTypeValue(type)) {
            return model1.equals(model2);
        }

        if (model1 instanceof List<?> list1 && model2 instanceof List<?> list2) {
            int size = list1.size();
            if (size != list2.size()) {
                return false;
            }
            for (int i=0 ; i<size ; i++) {
                if (!areDeepEqual(list1.get(i), list2.get(i))) {
                    return false;
                }
            }
            return true;
        }

        if (model1 instanceof Set<?> set1 && model2 instanceof Set<?> set2) {
            if (set1.size() != set2.size()) {
                return false;
            }
            if (set1.isEmpty()) {
                return true;
            }
            if (isTypeValue(set1.stream().findAny().get().getClass())) {
                return set1.equals(set2);
            }
            return set1.stream().allMatch(element1 -> set2.stream().anyMatch(element2 -> areDeepEqual(element1, element2)));
        }

        if (model1 instanceof Map<?,?> map1 && model2 instanceof Map<?,?> map2) {
            if (map1.size() != map2.size()) {
                return false;
            }
            if (map1.isEmpty()) {
                return true;
            }
            if (isTypeValue(map1.keySet().stream().findAny().get().getClass()) && isTypeValue(map1.values().stream().findAny().orElseThrow().getClass())) {
                return map1.equals(map2);
            }
            return map1.keySet().stream().allMatch(key1 -> map2.keySet().stream().anyMatch(key2 -> areDeepEqual(key1, key2) && areDeepEqual(map1.get(key1), map2.get(key2))));
        }

        return areDeepEqualWithFields(model1, model2);
    }

    private static boolean isNull(Object object) {
        return object == null || object.equals("");
    }

    private static boolean areDeepEqualWithFields(Object model1, Object model2) {

        Class<?> currentClass = model1.getClass();

        while (currentClass != null) {

            Field[] fields = currentClass.getDeclaredFields();
            for (Field field: fields) {

                if (isFieldOutsideState(field)) {
                    continue;
                }

                Object fieldValue1 = readField(field, model1);
                Object fieldValue2 = readField(field, model2);
                if (!areDeepEqual(fieldValue1, fieldValue2)) {
                    return false;
                }
            }

            currentClass = currentClass.getSuperclass();
        }

        return true;
    }

    public static <T> List<T> deepCopy(List<T> model, Class<T> type) {
        return model.stream()
                .map(ModelStateManager::copyModel)
                .map(type::cast)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Object copyModel(Object model) {

        if (model == null) {
            return null;
        }

        if (isTypeValue(model.getClass())) {
            return model;
        }

        if (model instanceof List<?> list) {
            return list.stream()
                    .map(ModelStateManager::copyModel)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return copyModelWithFields(model);
    }

    private static boolean isTypeValue(Class<?> type) {
        if (type.isRecord()) {
            return true;
        }
        return VALUE_CLASSES.stream().anyMatch(valueClass -> valueClass.isAssignableFrom(type));
    }

    private static Object readField(Field field, Object model) {
        try {
            field.setAccessible(true);
            return field.get(model);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object copyModelWithFields(Object model) {

        Class<?> modelClass = model.getClass();
        Object newModel = makeNewInstance(modelClass);

        Class<?> currentClass = modelClass;

        while (currentClass != null) {

            Field[] fields = currentClass.getDeclaredFields();
            for (Field field: fields) {

                if (isFieldOutsideState(field)) {
                    continue;
                }

                Object fieldValue = readField(field, model);
                Object newFieldValue = copyModel(fieldValue);
                changeFieldValue(newModel, field, newFieldValue);
            }

            currentClass = currentClass.getSuperclass();
        }

        return newModel;
    }

    private static Object makeNewInstance(Class<?> modelClass) {
        try {
            return modelClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isFieldOutsideState(Field field) {
        return field.getAnnotation(JsonIgnore.class) != null;
    }

    private static void changeFieldValue(Object newModel, Field field, Object newFieldValue) {
        try {
            field.set(newModel, newFieldValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
