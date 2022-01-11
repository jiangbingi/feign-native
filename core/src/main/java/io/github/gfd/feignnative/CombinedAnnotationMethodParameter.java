package io.github.gfd.feignnative;

import io.github.gfd.feignnative.util.MethodUtil;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 拷贝HandleMethodParameter合并参数上所有的注解
 */
public class CombinedAnnotationMethodParameter extends SynthesizingMethodParameter {

    private volatile Annotation[] combinedAnnotations;

    private volatile List<Annotation[][]> interfaceParameterAnnotations;

    public CombinedAnnotationMethodParameter(Method method, int parameterIndex) {
        super(method, parameterIndex);
    }

    @Override
    public Annotation[] getParameterAnnotations() {
        Annotation[] anns = this.combinedAnnotations;
        if (anns == null) {
            anns = super.getParameterAnnotations();
            int index = getParameterIndex();
            if (index >= 0) {
                for (Annotation[][] ifcAnns : getInterfaceParameterAnnotations()) {
                    if (index < ifcAnns.length) {
                        Annotation[] paramAnns = ifcAnns[index];
                        if (paramAnns.length > 0) {
                            List<Annotation> merged = new ArrayList<>(anns.length + paramAnns.length);
                            merged.addAll(Arrays.asList(anns));
                            for (Annotation paramAnn : paramAnns) {
                                boolean existingType = false;
                                for (Annotation ann : anns) {
                                    if (ann.annotationType() == paramAnn.annotationType()) {
                                        existingType = true;
                                        break;
                                    }
                                }
                                if (!existingType) {
                                    merged.add(adaptAnnotation(paramAnn));
                                }
                            }
                            anns = merged.toArray(new Annotation[0]);
                        }
                    }
                }
            }
            this.combinedAnnotations = anns;
        }
        return anns;
    }

    private List<Annotation[][]> getInterfaceParameterAnnotations() {
        List<Annotation[][]> parameterAnnotations = this.interfaceParameterAnnotations;
        if (parameterAnnotations == null) {
            parameterAnnotations = new ArrayList<>();
            for (Class<?> ifc : ClassUtils.getAllInterfacesForClassAsSet(getMethod().getDeclaringClass())) {
                for (Method candidate : ifc.getMethods()) {
                    if (isOverrideFor(candidate)) {
                        parameterAnnotations.add(candidate.getParameterAnnotations());
                    }
                }
            }
            this.interfaceParameterAnnotations = parameterAnnotations;
        }
        return parameterAnnotations;
    }

    private boolean isOverrideFor(Method candidate) {
        Method method = getMethod();
        return MethodUtil.isOverriddenFrom(method, candidate);
    }
}
