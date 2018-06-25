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

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.model.asset.AssetRequest;
import com.centurylink.mdw.model.asset.AssetRequest.Parameter;
import com.centurylink.mdw.model.asset.AssetRequest.ParameterType;
import com.centurylink.mdw.model.asset.RequestKey;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.util.PrimitiveType;

public class SwaggerWorkflowReader {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Swagger swagger;
    SwaggerWorkflowReader(Swagger swagger) {
        this.swagger = swagger;
    }

    protected void read(RequestKey requestKey, AssetRequest processRequest) {
        String servicePath = requestKey.getPath();
        if (!servicePath.startsWith("/"))
            servicePath = "/" + servicePath;
        Path path = swagger.getPath(servicePath);
        if (path == null) {
            path = new Path();
            swagger.path(servicePath, path);
        }

        Operation operation = new Operation();
        operation.setSummary(processRequest.getSummary());
        if (processRequest.getParameters() != null) {
            for (Parameter parameter : processRequest.getParameters()) {
                io.swagger.models.parameters.Parameter swaggerParam = createParam(parameter.getType());
                swaggerParam.setName(parameter.getName());
                swaggerParam.setRequired(parameter.isRequired());
                swaggerParam.setDescription(parameter.getDescription());
                if (parameter.getDataType() != null) {
                    if (swaggerParam instanceof SerializableParameter) {
                        ((SerializableParameter)swaggerParam).setType(parameter.getDataType());
                    }
                    final Type type = typeFromString(parameter.getDataType());
                    if (type != null) {
                        final Property property = ModelConverters.getInstance().readAsProperty(type);
                        if (property != null) {
                            final Map<PropertyBuilder.PropertyId,Object> args = new EnumMap<>(PropertyBuilder.PropertyId.class);
                            for (Map.Entry<String,Model> entry : ModelConverters.getInstance().readAll(type).entrySet()) {
                                swagger.addDefinition(entry.getKey(), entry.getValue());
                            }
                            if (swaggerParam instanceof BodyParameter) {
                                ((BodyParameter)swaggerParam).setSchema(PropertyBuilder.toModel(PropertyBuilder.merge(property, args)));
                            }
                        }
                    }
                }
                operation.addParameter(swaggerParam);
            }
        }

        path.set(requestKey.getMethod().toString().toLowerCase(), operation);
    }

    private io.swagger.models.parameters.Parameter createParam(ParameterType paramType) {
        if (paramType == ParameterType.Path)
            return new PathParameter();
        else if (paramType == ParameterType.Query)
            return new QueryParameter();
        else if (paramType == ParameterType.Form)
            return new FormParameter();
        else if (paramType == ParameterType.Header)
            return new HeaderParameter();
        else if (paramType == ParameterType.Body)
            return new BodyParameter();
        return null;
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

}
