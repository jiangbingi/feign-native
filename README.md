# feign-native

#### 介绍
目前微服务通信仍然有很多还是使用feign，但会有许多重复的工作量，为接近本地调用开发体验，做了以下几个方面处理：
1.  provider端无需编写控制器，自动暴露HTTP接口
2.  client端只需接口@FeignClient注解指定服务名等信息，方法和参数上可以不加注解
3.  自动包装和拆封统一结果对象，provider返回出去时包装为统一结果对象，client端拿到数据时自动拆封，上层调用得到的是原始数据对象

#### 架构说明

默认规则：
1.  默认以`/{@Feignclient中path}/{methodName}`作为url
2.  方法参数是自己写的Java或Map对象相当于加上@ReuqestBody，其余加上@RequestParam，因为Feign当中最多只有一个Body；如果有Body请求方式变为POST，否则是GET
3.  因为接口上的参数名称无法拿到，所以按参数顺序重名为arg0,arg1……

实现原理：
1.  provider端通过字节码技术，动态生成api实现类的子类，并加上@RestController注解和@RequestMapping注解（value等于@FeignClient中指定的path），实现HTTP接口暴露
2.  client端重写Contract解析方法元数据流程，增加无注解时元数据填充
3.  provider端返回数据通过`ResponseBodyAdvice`进行拦截返回统一结果对象，client端重写方法返回值类型，让Feign中的`Decoder`组件按统一结果对象解析


#### 使用说明

1.  添加POM依赖
```
<dependency>
    <groupId>io.github.telxs</groupId>
    <artifactId>feign-native</artifactId>
    <version>1.0.1</version>
 </dependency>
```
2.  定义服务接口
```
@FeignClient(name = "demo-provider", path = "/api")
public interface DemoApi {
    String sayHello(String name);
}

```
3.  服务提供方实现接口
```
@Service
public class DemoApiImpl implements DemoApi {
    @Override
    public String sayHello(String name) {
        return "hello " + name;
    }
}
```
4.  服务提供方使用@EnableFeignProvider暴露接口HTTP服务（包路径为实现类位置）
```
@EnableFeignProvider(basePackages = "io.github.telxs.demo.provider")
@SpringBootApplication
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
```
4.  服务消费方使用@EnableFeignClientsNative，并在需要的地方注入DemoApi
```
@EnableFeignClientsNative
@EnableFeignClients(basePackages = "io.github.telxs.demo.api")
@SpringBootApplication
public class ConsumerApplication {
    @Autowired
    DemoApi demoApi;
}
```
