package io.github.gfd.feignnative;

import feign.MethodMetadata;

import java.lang.reflect.Type;

/**
 * 元数据扩展
 */
public class MethodMetadataExt {

    private final MethodMetadata metadata;

    private final Type originReturnType;

    public MethodMetadataExt(MethodMetadata metadata) {
        this.metadata = metadata;
        this.originReturnType = metadata.returnType();
    }

    public MethodMetadata metadata(){
        return metadata;
    }

    public Type originReturnType(){
        return originReturnType;
    }
}
