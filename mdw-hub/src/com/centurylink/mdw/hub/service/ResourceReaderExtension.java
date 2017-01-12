/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.services.rest.RestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.servlet.ReaderContext;
import io.swagger.servlet.extensions.ReaderExtension;
import io.swagger.util.ParameterProcessor;
import io.swagger.util.PathUtils;
import io.swagger.util.PrimitiveType;
import io.swagger.util.ReflectionUtils;

public class ResourceReaderExtension implements ReaderExtension {

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean isReadable(ReaderContext context) {
        return true;
    }

    @Override
    public void applyConsumes(ReaderContext context, Operation operation, Method method) {
        // TODO Auto-generated method stub

    }

    @Override
    public void applyProduces(ReaderContext context, Operation operation, Method method) {
        // TODO Auto-generated method stub

    }

    public static final String BASE_REST_PKG = "com.centurylink.mdw.services.rest";

    @Override
    public String getHttpMethod(ReaderContext context, Method method) {
        ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        if (apiOperation != null && apiOperation.httpMethod() != null && !apiOperation.httpMethod().isEmpty()) {
            return apiOperation.httpMethod();
        }
        else {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass.getPackage().getName().equals(BASE_REST_PKG))
                return null;

            // JAX-RS annotations
            GET get = ReflectionUtils.getAnnotation(method, GET.class);
            if (get != null)
                return "get";
            PUT put = ReflectionUtils.getAnnotation(method, PUT.class);
            if (put != null)
                return "put";
            POST post = ReflectionUtils.getAnnotation(method, POST.class);
            if (post != null)
                return "post";
            DELETE delete = ReflectionUtils.getAnnotation(method, DELETE.class);
            if (delete != null)
                return "delete";

            // MDW REST
            if (RestService.class.isAssignableFrom(declaringClass))
                return method.getName();

            return null;
        }
    }

    @Override
    public String getPath(ReaderContext context, Method method) {
        String p = null;
        Api apiAnnotation = context.getCls().getAnnotation(Api.class);
        ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        String operationPath = apiOperation == null ? null : apiOperation.nickname();
        if (operationPath != null && !operationPath.isEmpty()) {
            // same logic as ServletReaderExtension
            p = PathUtils.collectPath(context.getParentPath(),
                    apiAnnotation == null ? null : apiAnnotation.value(), operationPath);
        }
        else {
            // try JAX-RS annotations
            Path parentPath = ReflectionUtils.getAnnotation(method.getDeclaringClass(), Path.class);
            if (parentPath != null && parentPath.value() != null && !parentPath.value().isEmpty()) {
                p = parentPath.value();
            }
            Path path = ReflectionUtils.getAnnotation(method, Path.class);
            if (path != null && path.value() != null && !path.value().isEmpty()) {
                if (p == null)
                    p = path.value();
                else {
                    if (path.value().startsWith("/"))
                        p += path.value();
                    else
                        p = p + "/" + path.value();
                }
            }
            // check dynamic java, which has package-based pathing
            java.lang.Package pkg = method.getDeclaringClass().getPackage();
            if (p != null && "MDW".equals(pkg.getImplementationTitle())) {
                if (p.startsWith("/"))
                    p = "/" + pkg.getName().replace('.', '/') + p;
                else
                    p = "/" + pkg.getName().replace('.', '/') + "/" + p;
            }

            if (apiOperation != null) {
                ApiImplicitParams implicitParams = ReflectionUtils.getAnnotation(method, ApiImplicitParams.class);
                if (implicitParams != null && implicitParams.value() != null && implicitParams.value().length == 1) {
                    ApiImplicitParam implicitParam = implicitParams.value()[0];
                    if (implicitParam.name() != null && !"body".equals(implicitParam.paramType()) && !"query".equals(implicitParam.paramType()))
                        p += "/{" + implicitParam.name() + "}";
                }
            }
        }
        return p;
    }

    @Override
    public void applyOperationId(Operation operation, Method method) {
//        Class<?> declaringClass = method.getDeclaringClass();
//        //if (RestService.class.isAssignableFrom(declaringClass))
//            operation.operationId(method.getName() + declaringClass.getSimpleName());
    }

    @Override
    public void applySummary(Operation operation, Method method) {
    }

    @Override
    public void applyDescription(Operation operation, Method method) {
    }

    @Override
    public void applySchemes(ReaderContext context, Operation operation, Method method) {
    }

    @Override
    public void setDeprecated(Operation operation, Method method) {
    }

    @Override
    public void applySecurityRequirements(ReaderContext context, Operation operation, Method method) {
    }

    @Override
    public void applyTags(ReaderContext context, Operation operation, Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        Api apiAnnotation = declaringClass.getAnnotation(Api.class);
        if (apiAnnotation != null && apiAnnotation.value() != null && !apiAnnotation.value().isEmpty()) {
            operation.addTag(apiAnnotation.value());
        }
    }

    @Override
    public void applyResponses(ReaderContext context, Operation operation, Method method) {
    }

    @Override
    public void applyParameters(ReaderContext context, Operation operation, Type type, Annotation[] annotations) {
    }

    /**
     * Implemented to allow loading of custom types using CloudClassLoader.
     */
    @Override
    public void applyImplicitParameters(ReaderContext context, Operation operation, Method method) {
        // copied from io.swagger.servlet.extensions.ServletReaderExtension
        final ApiImplicitParams implicitParams = method.getAnnotation(ApiImplicitParams.class);
        if (implicitParams != null && implicitParams.value().length > 0) {
            for (ApiImplicitParam param : implicitParams.value()) {
                final Parameter p = readImplicitParam(context.getSwagger(), param);
                if (p != null) {
                    operation.parameter(p);
                }
            }
        }
    }

    private Parameter readImplicitParam(Swagger swagger, ApiImplicitParam param) {
        final Parameter p = createParam(param.paramType());
        if (p == null) {
            return null;
        }
        final Type type = typeFromString(param.dataType());
        return ParameterProcessor.applyAnnotations(swagger, p, type == null ? String.class : type,
                Collections.<Annotation>singletonList(param));
    }

    private Type typeFromString(String type) {
        final PrimitiveType primitive = PrimitiveType.fromName(type);
        if (primitive != null) {
            return primitive.getKeyClass();
        }
        try {
            try {
                return Class.forName(type);
            }
            catch (ClassNotFoundException cnfe) {
                // use CloudClassLoader
                int lastDot = type.lastIndexOf('.');
                if (lastDot > 0) {
                    String pkgName = type.substring(0, lastDot);
                    Package pkg = PackageCache.getPackage(pkgName);
                    if (pkg != null) {
                        return pkg.getCloudClassLoader().loadClass(type);
                    }
                }
                System.err.println(String.format("Failed to resolve '%s' into class", type));
                cnfe.printStackTrace();
            }
        }
        catch (Exception ex) {
            System.err.println(String.format("Failed to resolve '%s' into class", type));
            ex.printStackTrace();
        }
        return null;
    }

    private Parameter createParam(String paramType) {
        if ("path".equals(paramType))
            return new PathParameter();
        else if ("query".equals(paramType))
            return new QueryParameter();
        else if ("form".equals(paramType))
            return new FormParameter();
        else if ("formData".equals(paramType))
            return new FormParameter();
        else if ("header".equals(paramType))
            return new HeaderParameter();
        else if ("body".equals(paramType))
            return new BodyParameter();
        return null;
    }

}
