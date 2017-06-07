/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

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
import io.swagger.servlet.extensions.ServletReaderExtension;
import io.swagger.util.ParameterProcessor;
import io.swagger.util.PathUtils;
import io.swagger.util.PrimitiveType;
import io.swagger.util.ReflectionUtils;

public class ResourceReaderExtension extends ServletReaderExtension implements ReaderExtension {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String BASE_REST_PKG = "com.centurylink.mdw.services.rest";

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean isReadable(ReaderContext context) {
        return true;
    }

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
            if (put != null) {
                if (method.getName().equals("patch"))
                    return "patch";
                return "put";
            }
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
    public void applyTags(ReaderContext context, Operation operation, Method method) {
        super.applyTags(context, operation, method);

        Class<?> declaringClass = method.getDeclaringClass();
        Api apiAnnotation = declaringClass.getAnnotation(Api.class);
        if (apiAnnotation != null && apiAnnotation.value() != null && !apiAnnotation.value().isEmpty()) {
            operation.addTag(apiAnnotation.value());
        }
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
                    if (p instanceof BodyParameter && param.format() != null)
                        p.getVendorExtensions().put("format", param.format());
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
        if (type == null || type.isEmpty()) {
            return null;
        }
        final PrimitiveType primitive = PrimitiveType.fromName(type);
        if (primitive != null) {
            return primitive.getKeyClass();
        }
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
                    try {
                        logger.debug("Loading type: " + type + " using " + pkg.getName() + "'s ClassLoader");
                        return pkg.getCloudClassLoader().loadClass(type);
                    }
                    catch (ClassNotFoundException cnfe2) {
                        logger.severeException(String.format("Failed to resolve '%s' into class", type), cnfe);
                    }
                }
            }
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
