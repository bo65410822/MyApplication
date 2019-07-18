package com.lzb.apt.precessor;

import com.google.auto.service.AutoService;
import com.lzb.annotation.RouteMeta;
import com.lzb.annotation.Router;
import com.lzb.apt.utils.Consts;
import com.lzb.apt.utils.Log;
import com.lzb.apt.utils.Utils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import io.reactivex.Observable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * author: Lzhb
 * created on: 2019/7/16 16:35
 * description:
 */
@AutoService(Processor.class)
/**
 * 处理器接收的参数 替代 {@link AbstractProcessor#getSupportedOptions()} 函数
 */
@SupportedOptions(Consts.ARGUMENTS_NAME)
/**
 *  指定使用的Java版本 替代 {@link AbstractProcessor#getSupportedSourceVersion()} 函数
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
/**
 * 注册给哪些注解的  替代 {@link AbstractProcessor#getSupportedAnnotationTypes()} 函数
 */
@SupportedAnnotationTypes({Consts.ANN_TYPE_ROUTE})
public class RouterProcessor extends AbstractProcessor {
    /*
     * 节点工具类 (类、函数、属性都是节点)
     */
    private Elements mElements;
    /*
     * 文件生成器 类/资源
     */
    private Filer mFiler;
    /*
     * type(类信息)工具类
     */
    private Types types;


    /*
     * key:组名 value:类名
     */
    private Map<String, String> rootMap = new TreeMap<>();
    /*
     * 分组 key:组名 value:对应组的路由信息
     */
    private Map<String, List<RouteMeta>> groupMap = new HashMap<>();
    //日志
    private Log log;
    /**
     * 参数
     */
    private String moduleName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        log = Log.newLog(processingEnvironment.getMessager());
        mFiler = processingEnvironment.getFiler();
        mElements = processingEnvironment.getElementUtils();
        types = processingEnvironment.getTypeUtils();
        Map<String, String> options = processingEnvironment.getOptions();
        if (!Utils.isEmpty(options)) {
            moduleName = options.get(Consts.ARGUMENTS_NAME);
        }
        if (Utils.isEmpty(moduleName)) {
            throw new RuntimeException("Not set Processor Parameters.");
        }
        log.i("init");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (!set.isEmpty()) {
            //得到所有Router注解的元素
            Set<? extends Element> routers = roundEnvironment.getElementsAnnotatedWith(Router.class);
            if (!Utils.isEmpty(routers)) {
                parseRoutes(routers);
            }
        }
        log.i("process");
        return false;
    }

    private void parseRoutes(Set<? extends Element> routers) {
        //支持配置路由类的类型
        TypeElement activity = mElements.getTypeElement(Consts.ACTIVITY);
        //节点自描述 Mirror
        TypeMirror type_Activity = activity.asType();
        for (Element element : routers) {
            //路由信息
            RouteMeta routeMeta;
            // 使用Route注解的类信息
            TypeMirror mirror = element.asType();
            Router router = element.getAnnotation(Router.class);
            //是否是 Activity 使用了Route注解
            if (types.isSubtype(mirror, type_Activity)) {
                routeMeta = new RouteMeta(RouteMeta.Type.ACTIVITY, router, element);
            } else {
                throw new RuntimeException("[Just Support Activity/IService Route] :" + element);
            }
            //分组信息记录  groupMap <Group分组,RouteMeta路由信息> 集合
            categories(routeMeta);
        }
        //生成类需要实现的接口
        TypeElement routeGroup = mElements.getTypeElement(Consts.I_ROUTE_GROUP);
        TypeElement routeRoot = mElements.getTypeElement(Consts.I_ROUTE_ROOT);
        /**
         *  生成Group类 作用:记录 <地址,RouteMeta路由信息(Class文件等信息)>
         */
        generatedGroup(routeGroup);
        /**
         * 生成Root类 作用:记录 <分组，对应的Group类>
         */
        generatedRoot(routeRoot, routeGroup);
        log.i("parseRoutes");
    }

    private void generatedRoot(TypeElement routeRoot, TypeElement routeGroup) {
        //类型 Map<String,Class<? extends IRouteGroup>> routes>
        //Wildcard 通配符
        ParameterizedTypeName routes = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(routeGroup))));
        //参数 Map<String,Class<? extends IRouteGroup>> routes> routes
        ParameterSpec parameterSpec = ParameterSpec.builder(routes, "routes").build();
        //函数 public void loadInfo(Map<String,Class<? extends IRouteGroup>> routes> routes)
        MethodSpec.Builder builder = MethodSpec.methodBuilder(Consts.METHOD_LOAD_INTO)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterSpec);
        //函数体

        Observable.fromIterable(rootMap.entrySet()).subscribe(entry -> {
            log.i("3");
            builder.addStatement("routes.put($S, $T.class)", entry.getKey(),
                    ClassName.get(Consts.PACKAGE_OF_GENERATE_FILE, entry.getValue()));
            //生成 $Root$类
            log.i("4");
            String rootClassName = Consts.NAME_OF_ROOT + moduleName;
            JavaFile.builder(Consts.PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(rootClassName)
                            .addModifiers(Modifier.PUBLIC)
                            .addSuperinterface(ClassName.get(routeRoot))
                            .addMethod(builder.build()).build())
                    .build().writeTo(mFiler);
        });
        log.i("generatedRoot");
    }

    private void generatedGroup(TypeElement routeGroup) {
        //设置参数  Map<String,RouteMeta>
        ParameterizedTypeName params = ParameterizedTypeName.get(ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouteMeta.class));
        //参数 Map<String,RouteMeta> params
        ParameterSpec parameterSpec = ParameterSpec.builder(params, "params").build();
        //遍历分组,每一个分组创建一个 $$Group$$ 类
        Observable.fromIterable(groupMap.entrySet()).subscribe(entry -> {
            /**
             * 类成员函数loadInfo声明构建
             */
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Consts.METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)  // 设置注解
                    .addModifiers(Modifier.PUBLIC) // 设置访问权限  public
                    .addParameter(parameterSpec);   // 设置参数
            //分组名 与 对应分组中的信息
            String key = entry.getKey();
            List<RouteMeta> routeMetas = entry.getValue();
            Observable.fromIterable(routeMetas).subscribe(routeMeta -> {
                // 组装函数体:
                methodBuilder.addStatement("params.put($S,$T.build($T.$L,$T.class,$S,$S))",
                        routeMeta.getGroup(),
                        ClassName.get(RouteMeta.class),
                        ClassName.get(RouteMeta.Type.class),
                        routeMeta.getType(),
                        ClassName.get((TypeElement) routeMeta.getElement()),
                        routeMeta.getPath().toLowerCase(),
                        routeMeta.getGroup().toLowerCase());
            });
            // 创建java文件($$Group$$)  组
            String groupClassName = Consts.NAME_OF_GROUP + key;
            JavaFile.builder(Consts.PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(groupClassName)//生成类
                            .addSuperinterface(ClassName.get(routeGroup))//添加生成类实现的接口
                            .addModifiers(Modifier.PUBLIC)//设置生成类的访问权限 public
                            .addMethod(methodBuilder.build()).build())//设置生成类中的函数
                    .build().writeTo(mFiler);//执行写入文件
            //分组名和生成的对应的Group类类名
            rootMap.put(key, groupClassName);
        });
        log.i("generatedGroup");
    }

    private void categories(RouteMeta routeMeta) {
        routeVerify(routeMeta);
        List<RouteMeta> routeMetas = groupMap.get(routeMeta.getGroup());
        //如果未记录分组则创建
        if (Utils.isEmpty(routeMetas)) {
            List<RouteMeta> routeMetaSet = new ArrayList<>();
            routeMetaSet.add(routeMeta);
            groupMap.put(routeMeta.getGroup(), routeMetaSet);
        } else {
            routeMetas.add(routeMeta);
        }
        log.i("categories");
    }

    /**
     * 验证路由信息必须存在path(并且设置分组)
     *
     * @param meta raw meta
     */
    private void routeVerify(RouteMeta meta) {
        String path = meta.getPath();
        String group = meta.getGroup();
        //路由地址必须以 / 开头
        if (Utils.isEmpty(path)) {
            throw new NullPointerException("Router's path must not be null or empty.");
        } else {
            if (!path.startsWith("/")) {
                throw new RuntimeException("Router's path must start with '/'.");
            }
        }
        //如果没有设置分组,以第一个 / 后的节点为分组(所以必须path两个/)
        if (Utils.isEmpty(group)) {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (Utils.isEmpty(defaultGroup)) {
                throw new RuntimeException("Two-level directories must be split by '/'.");
            }
            meta.setGroup(defaultGroup);
        }
        log.i("routeVerify");
    }
}
