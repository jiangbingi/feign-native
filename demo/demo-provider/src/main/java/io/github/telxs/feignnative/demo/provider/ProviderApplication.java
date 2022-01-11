package io.github.telxs.feignnative.demo.provider;

import io.github.gfd.feignnative.anno.EnableFeignProvider;
import io.github.gfd.feignnative.constant.DebugConstant;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableFeignProvider(basePackages = "io.github.telxs.feignnative.demo.provider")
@SpringBootApplication
public class ProviderApplication {
    public static void main(String[] args) {
        // 设置动态生成的class文件路径
        System.setProperty(DebugConstant.CLASS_LOCATION, "D:\\feign");
        SpringApplication.run(ProviderApplication.class, args);
    }
}
