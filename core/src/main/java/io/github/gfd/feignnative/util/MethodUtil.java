package io.github.gfd.feignnative.util;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.util.Arrays;


public class MethodUtil {

    public static boolean isOverriddenFrom(Method method, Method candidate) {
        if (!candidate.getName().equals(method.getName()) ||
                candidate.getParameterCount() != method.getParameterCount()) {
            return false;
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
            return true;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] !=
                    ResolvableType.forMethodParameter(candidate, i, method.getDeclaringClass()).resolve()) {
                return false;
            }
        }
        return true;
    }
}
