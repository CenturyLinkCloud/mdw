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
package com.centurylink.mdw.service.api.validator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.StringHelper;

import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.PrimitiveType;

/**
 * Dynamic Java workflow asset.
 */
public class SwaggerModelValidator implements java.io.Serializable {
    private List<Validator> validators = new ArrayList<Validator>();

    private ValidationResult validationResult;

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public SwaggerModelValidator() {
        super();
        addDefaultValidators();
    }

    public SwaggerModelValidator(List<Validator> validators) {
        super();
        this.validators = validators;
    }

    /**
     * @return the validators
     */
    public List<Validator> getValidators() {
        return validators;
    }

    /**
     * @param validators
     *            the validators to set
     */
    public void setValidators(List<Validator> validators) {
        this.validators = validators;
    }

    public void addValidator(Validator newValidator) {
        Validator matchedValidator = getValidators().stream().filter((validator) -> validator
                .getClass().getName().equals(newValidator.getClass().getName())).findFirst()
                .orElse(null);
        if (matchedValidator == null) {
            getValidators().add(newValidator);
        }

    }

    public void addDefaultValidators() {
        validators.clear();
        // validators.add(new BooleanValidator());
        validators.add(new ArrayValidator());
        validators.add(new StringValidator());
    }

    public ValidationResult validateModel(Jsonable json) throws ValidationException, JSONException {
        // Get all the models
        Map<String, Model> models = ModelConverters.getInstance()
                .readAll(typeFromString(json.getClass().getName()));
        // Get the main model
        Model mainModel = models.get(json.getClass().getSimpleName());

        ValidationResult result = new ValidationResult();
        result = validateFields(json.getJson(), mainModel, models);
        return result;

    }

    public ValidationResult validateModel(JSONObject json, Class<?> type)
            throws ValidationException, JSONException {
        return validateModel(json, type, false);
    }

    /**
     * @param json JSONObject to validate
     * @param type Java class with Swagger @Api annotations
     * @param strict if true, unknown properties fail validation
     */
    public ValidationResult validateModel(JSONObject json, Class<?> type, boolean strict)
            throws ValidationException, JSONException {
        ModelConverters modelConverters = ModelConverters.getInstance();
        ValidationResult result = new ValidationResult();
        Map<String,Model> models = modelConverters.readAll(type);
        Map<String,Model> mainModels = modelConverters.read(type);
        for (String modelName : mainModels.keySet()) {
            Model mainModel = mainModels.get(modelName);
            result.addValidationMessages(validateFields(json, mainModel, models));
            if (strict)
                result.addValidationMessages(validateFieldsInModel(json, mainModel, models));
        }
        return result;
    }

    public ValidationResult validateModel(JSONObject originalRequest, Jsonable modelObject)
            throws ValidationException, JSONException {
        // Get all the models
        Map<String, Model> models = ModelConverters.getInstance()
                .readAll(typeFromString(modelObject.getClass().getName()));
        // Get the main model
        Model mainModel = models.get(modelObject.getClass().getSimpleName());

        ValidationResult result = new ValidationResult();
        result = validateFieldsInModel(originalRequest, mainModel, models);
        return result;

    }

    /**
     * Validate fields in a JSONObject against the supported ones in a model
     *
     * @param json
     * @param mainModel
     * @param models
     * @return ValidationResult of invalid properties in the JSON
     * @throws JSONException
     */
    private ValidationResult validateFieldsInModel(JSONObject json, Model mainModel,
            Map<String,Model> models) throws JSONException {
        ValidationResult validationResult = new ValidationResult();
        String[] passedInProperties = JSONObject.getNames(json);
        for (int i = 0; i < passedInProperties.length; i++) {
            String propertyKey = passedInProperties[i];

            if (mainModel.getProperties() == null
                    || !mainModel.getProperties().containsKey(propertyKey)) {
                if (mainModel.getProperties().containsKey("get" + propertyKey)) {
                    propertyKey = "get" + propertyKey;
                }
                else {
                    String msg = "'" + passedInProperties[i] + "' not expected";
                    if (mainModel instanceof ModelImpl)
                        msg += " on " + ((ModelImpl) mainModel).getName();
                    validationResult.addValidationMessage(new ValidationMessage().message(msg));
                }
            }
            Property modelProperty = mainModel.getProperties().get(propertyKey);
            // If it's a RefProperty then look at that
            if (modelProperty instanceof RefProperty) {
                String name = ((RefProperty) modelProperty).getSimpleRef();
                validationResult.addValidationMessages(
                        (validateFieldsInModel(json.getJSONObject(passedInProperties[i]),
                                models.get(name), models)));
            }
        }
        return validationResult;

    }

    /**
     * Validates all the fields in a JSONObject based on the model
     * <p>
     * First checks if the property is required, then next, validates the actual
     * value
     * </p>
     *
     * @param json
     * @param model
     * @param swaggerModels
     * @return ValidationResult
     * @throws ValidationException
     */
    private ValidationResult validateFields(JSONObject json, Model model,
            Map<String,Model> swaggerModels) throws ValidationException {
        Map<String, Property> modelFields = model.getProperties();
        ValidationResult validationResult = new ValidationResult();
        for (Map.Entry<String, Property> prop : modelFields.entrySet()) {
            try {
                String property = prop.getKey();
                Property modelProperty = prop.getValue();
                if (!json.has(property)) {
                    // Hack for 911Address which seems to return get911Address
                    // for some reason
                    if (property.startsWith("get")) {
                        property = property.substring(3);
                    }
                }
                // if (!json.has(property)) {
                // throw new ValidationException().message(new
                // ValidationMessage().message("unable to find json property
                // "+property));
                // }
                // Check for requiredness
                if (!json.has(property) || json.get(property) == null
                        || StringHelper.isEmpty(json.getString(property))) {
                    if (modelProperty.getRequired()) {
                        String msg = "'" + property + "' is a required property";
                        if (model instanceof ModelImpl)
                            msg += " on " + ((ModelImpl)model).getName();
                        validationResult.addValidationMessage(new ValidationMessage().message(msg));
                    }
                }
                else {
                    if (modelProperty instanceof RefProperty) {
                        // Got a class, so process this class for required
                        // fields
                        String name = ((RefProperty) modelProperty).getSimpleRef();
                        validationResult
                                .addValidationMessages((validateFields(json.getJSONObject(property),
                                        swaggerModels.get(name), swaggerModels)));
                    }
                    // Validate formats only if not a required error
                    // and there is some kind of value
                    Iterator<Validator> itr = validators.iterator();

                    if (itr.hasNext()) {
                        Validator validator = itr.next();
                        validationResult.addValidationMessages(
                                validator.validate(json, property, modelProperty, itr));
                    }
                }
            }
            catch (Exception e) {
                // include e in ValidationException constructor so that root cause is not lost
                throw new ValidationException(
                        new ValidationMessage().message(e.getMessage()), e);
            }
        }
        return validationResult;

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

}
