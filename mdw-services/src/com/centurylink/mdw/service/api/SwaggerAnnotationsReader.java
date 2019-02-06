/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.service.api;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.limberest.api.SwaggerModelConverter;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.converter.ModelConverters;
import io.swagger.models.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.servlet.ReaderContext;
import io.swagger.servlet.extensions.ReaderExtension;
import io.swagger.util.BaseReaderUtils;
import io.swagger.util.PathUtils;
import io.swagger.util.ReflectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Duplicated from io.swagger.servlet.Reader except where noted below to avoid auto-inclusion of
 * of Swagger's ServletResourceReaderExtension.
 */
public class SwaggerAnnotationsReader {

    private final Swagger swagger;

    private SwaggerAnnotationsReader(Swagger swagger) {
        this.swagger = swagger;
        ModelConverters.getInstance().addConverter(new SwaggerModelConverter() {
            protected boolean shouldIgnoreClass(Type type) {
                if (type instanceof SimpleType && JSONObject.class.equals(((SimpleType)type).getRawClass())) {
                    return true;
                }
                return super.shouldIgnoreClass(type);
            }
        });
    }

    /**
     * Scans a set of classes for Swagger annotations.
     *
     * @param swagger is the Swagger instance
     * @param classes are a set of classes to scan
     */
    public static void read(Swagger swagger, Set<Class<?>> classes) {
        final SwaggerAnnotationsReader reader = new SwaggerAnnotationsReader(swagger);
        for (Class<?> cls : classes) {
            final ReaderContext context = new ReaderContext(swagger, cls, "", null, false, new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            reader.read(context);
        }
    }

    private void read(ReaderContext context) {
        final SwaggerDefinition swaggerDefinition = context.getCls().getAnnotation(SwaggerDefinition.class);
        if (swaggerDefinition != null) {
            readSwaggerConfig(swaggerDefinition);
        }
        for (Method method : context.getCls().getMethods()) {
            if (ReflectionUtils.isOverriddenMethod(method, context.getCls())) {
                continue;
            }
            final Operation operation = new Operation();
            String operationPath = null;
            String httpMethod = null;

            final Type[] genericParameterTypes = method.getGenericParameterTypes();
            final Annotation[][] paramAnnotations = method.getParameterAnnotations();

            // Avoid java.util.ServiceLoader mechanism which finds ServletReaderExtension
            // for (ReaderExtension extension : ReaderExtensions.getExtensions()) {
            for (ReaderExtension extension : getExtensions()) {
                if (operationPath == null) {
                    operationPath = extension.getPath(context, method);
                }
                if (httpMethod == null) {
                    httpMethod = extension.getHttpMethod(context, method);
                }
                if (operationPath == null || httpMethod == null) {
                    continue;
                }

                if (extension.isReadable(context)) {
                    extension.setDeprecated(operation, method);
                    extension.applyConsumes(context, operation, method);
                    extension.applyProduces(context, operation, method);
                    extension.applyOperationId(operation, method);
                    extension.applySummary(operation, method);
                    extension.applyDescription(operation, method);
                    extension.applySchemes(context, operation, method);
                    extension.applySecurityRequirements(context, operation, method);
                    extension.applyTags(context, operation, method);
                    extension.applyResponses(context, operation, method);
                    extension.applyImplicitParameters(context, operation, method);
                    for (int i = 0; i < genericParameterTypes.length; i++) {
                        extension.applyParameters(context, operation, genericParameterTypes[i], paramAnnotations[i]);
                    }
                }
            }

            if (httpMethod != null && operationPath != null) {
                if (operation.getResponses() == null) {
                    operation.defaultResponse(new Response().description("OK"));
                }
                else {
                    for (String k : operation.getResponses().keySet()) {
                        if (k.equals("200")) {
                            Response response = operation.getResponses().get(k);
                            if ("successful operation".equals(response.getDescription()))
                                response.setDescription("OK");
                        }
                    }
                }

                final Map<String,String> regexMap = new HashMap<>();
                final String parsedPath = PathUtils.parsePath(operationPath, regexMap);

                if (parsedPath != null) {
                    // check for curly path params
                    for (String seg : parsedPath.split("/")) {
                        if (seg.startsWith("{") && seg.endsWith("}")) {
                            String segName = seg.substring(1, seg.length() - 1);
                            boolean declared = false;
                            for (Parameter opParam : operation.getParameters()) {
                                if ("path".equals(opParam.getIn()) && segName.equals(opParam.getName())) {
                                    declared = true;
                                    break;
                                }
                            }
                            if (!declared) {
                                // add it for free
                                PathParameter pathParam = new PathParameter();
                                pathParam.setName(segName);
                                pathParam.setRequired(false);
                                pathParam.setDefaultValue("");
                                operation.parameter(pathParam);
                            }
                        }
                    }
                }


                Path path = swagger.getPath(parsedPath);
                if (path == null) {
                    path = new Path();
                    swagger.path(parsedPath, path);
                }
                path.set(httpMethod.toLowerCase(), operation);
            }
        }
    }

    private void readSwaggerConfig(SwaggerDefinition config) {
        readInfoConfig(config);

        if (StringUtils.isNotBlank(config.basePath())) {
            swagger.setBasePath(config.basePath());
        }

        if (StringUtils.isNotBlank(config.host())) {
            swagger.setHost(config.host());
        }

        for (String consume : config.consumes()) {
            if (StringUtils.isNotBlank(consume)) {
                swagger.addConsumes(consume);
            }
        }

        for (String produce : config.produces()) {
            if (StringUtils.isNotBlank(produce)) {
                swagger.addProduces(produce);
            }
        }

        if (StringUtils.isNotBlank(config.externalDocs().value())) {
            ExternalDocs externalDocs = swagger.getExternalDocs();
            if (externalDocs == null) {
                externalDocs = new ExternalDocs();
                swagger.setExternalDocs(externalDocs);
            }

            externalDocs.setDescription(config.externalDocs().value());

            if (StringUtils.isNotBlank(config.externalDocs().url())) {
                externalDocs.setUrl(config.externalDocs().url());
            }
        }

        for (io.swagger.annotations.Tag tagConfig : config.tags()) {
            if (StringUtils.isNotBlank(tagConfig.name())) {
                final Tag tag = new Tag();
                tag.setName(tagConfig.name());
                tag.setDescription(tagConfig.description());

                if (StringUtils.isNotBlank(tagConfig.externalDocs().value())) {
                    tag.setExternalDocs(new ExternalDocs(tagConfig.externalDocs().value(),
                            tagConfig.externalDocs().url()));
                }

                tag.getVendorExtensions().putAll(BaseReaderUtils.parseExtensions(tagConfig.extensions()));

                swagger.addTag(tag);
            }
        }

        for (SwaggerDefinition.Scheme scheme : config.schemes()) {
            if (scheme != SwaggerDefinition.Scheme.DEFAULT) {
                swagger.addScheme(Scheme.forValue(scheme.name()));
            }
        }
    }

    private void readInfoConfig(SwaggerDefinition config) {
        final Info infoConfig = config.info();
        io.swagger.models.Info info = swagger.getInfo();
        if (info == null) {
            info = new io.swagger.models.Info();
            swagger.setInfo(info);
        }

        if (StringUtils.isNotBlank(infoConfig.description())) {
            info.setDescription(infoConfig.description());
        }

        if (StringUtils.isNotBlank(infoConfig.termsOfService())) {
            info.setTermsOfService(infoConfig.termsOfService());
        }

        if (StringUtils.isNotBlank(infoConfig.title())) {
            info.setTitle(infoConfig.title());
        }

        if (StringUtils.isNotBlank(infoConfig.version())) {
            info.setVersion(infoConfig.version());
        }

        if (StringUtils.isNotBlank(infoConfig.contact().name())) {
            Contact contact = info.getContact();
            if (contact == null) {
                contact = new Contact();
                info.setContact(contact);
            }

            contact.setName(infoConfig.contact().name());
            if (StringUtils.isNotBlank(infoConfig.contact().email())) {
                contact.setEmail(infoConfig.contact().email());
            }

            if (StringUtils.isNotBlank(infoConfig.contact().url())) {
                contact.setUrl(infoConfig.contact().url());
            }
        }

        if (StringUtils.isNotBlank(infoConfig.license().name())) {
            License license = info.getLicense();
            if (license == null) {
                license = new License();
                info.setLicense(license);
            }

            license.setName(infoConfig.license().name());
            if (StringUtils.isNotBlank(infoConfig.license().url())) {
                license.setUrl(infoConfig.license().url());
            }
        }

        info.getVendorExtensions().putAll(BaseReaderUtils.parseExtensions(infoConfig.extensions()));
    }

    private List<ReaderExtension> getExtensions() {
        return Arrays.asList(new ReaderExtension[]{new ResourceReaderExtension()});
    }
}
