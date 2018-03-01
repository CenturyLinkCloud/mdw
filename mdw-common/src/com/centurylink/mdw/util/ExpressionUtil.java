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
package com.centurylink.mdw.util;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtilsBean;

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.model.variable.VariableInstance;

public class ExpressionUtil {

    private static PropertyUtilsBean propUtilsBean = new PropertyUtilsBean();
    private static Pattern tokenPattern = Pattern.compile("([\\$#]\\{.*?\\})");

    /**
     * Substitutes dynamic values for expressions in the input string.
     * @param input raw input string
     * @param model object containing the values to substitute
     * @return string with values substituted
     */
    public static String substitute(String input, Object model)
    throws MdwException {
        return substitute(input, model, null, false);
    }

    public static String substitute(String input, Object model, boolean lenient)
    throws MdwException {
        return substitute(input, model, null, lenient);
    }

    /**
     * Substitutes dynamic values for expressions in the input string.
     * @param input raw input string
     * @param model object containing the values to substitute
     * @param map of images to populate based on special ${image:*.gif} syntax
     * @return string with values substituted
     */
    public static String substitute(String input, Object model, Map<String,String> imageMap, boolean lenient)
    throws MdwException {
        StringBuffer substituted = new StringBuffer(input.length());
        try {
            Matcher matcher = tokenPattern.matcher(input);
            int index = 0;
            while (matcher.find()) {
                String match = matcher.group();
                substituted.append(input.substring(index, matcher.start()));
                if (imageMap != null && (match.startsWith("${image:") || match.startsWith("#{image:"))) {
                    String imageFile = match.substring(8, match.length() - 1);
                    String imageId = imageFile.substring(0, imageFile.lastIndexOf('.'));
                    substituted.append("cid:" + imageId);
                    imageMap.put(imageId, imageFile);
                }
                else if (match.startsWith("#{")) { // ignore #{... in favor of facelets (except images)
                    substituted.append(match);
                }
                else {
                    Object value;
                    if (lenient) {
                        try {
                            value = propUtilsBean.getProperty(model, match.substring(2, match.length() - 1));
                            if (value == null)
                                value = match;
                        } catch (Exception e) {
                            value = match;
                        }
                    } else {
                        value = propUtilsBean.getProperty(model, match.substring(2, match.length() - 1));
                    }
                    if (value != null)
                        substituted.append(value);
                }
                index = matcher.end();
            }
            substituted.append(input.substring(index));
            return substituted.toString();
        }
        catch (Exception ex) {
            throw new MdwException("Error substituting expression value(s)", ex);
        }
    }

    /**
     * Substitutes dynamic values for expressions in the input string.
     * @param input raw input string
     * @param variables variable instances to use in substitutions
     * @return string with values substituted
     */
    public static String substitute(String input, List<VariableInstance> variables)
    throws MdwException {
        StringBuffer substituted = new StringBuffer(input.length());
        try {
            Matcher matcher = tokenPattern.matcher(input);
            int index = 0;
            while (matcher.find()) {
                String match = matcher.group();
                substituted.append(input.substring(index, matcher.start()));
                Object value = getVariableValue(match.substring(2, match.length() - 1), variables);
                if (value != null)
                    substituted.append(value);
                index = matcher.end();
            }
            substituted.append(input.substring(index));
            return substituted.toString();
        }
        catch (Exception ex) {
            throw new MdwException("Error substituting expression value(s) in input: '" + input + "'", ex);
        }
    }

    private static Object getVariableValue(String name, List<VariableInstance> variables) {
        for (int i = 0; i < variables.size(); i++) {
            if (name.equalsIgnoreCase(variables.get(i).getName())) {
                return variables.get(i).getData();
            }
        }
        return null;
    }

    /**
     * Input is email template with image tags:
     * <code>
     * &lt;img src="${image:com.centurylink.mdw.base/mdw.png}" alt="MDW"&gt;
     * </code>
     * Uses the unqualified image name as its CID.  Populates imageMap with results.
     */
    public static String substituteImages(String input, Map<String,String> imageMap) {
        StringBuffer substituted = new StringBuffer(input.length());
        Matcher matcher = tokenPattern.matcher(input);
        int index = 0;
        while (matcher.find()) {
            String match = matcher.group();
            substituted.append(input.substring(index, matcher.start()));
            if (imageMap != null && (match.startsWith("${image:"))) {
                String imageFile = match.substring(8, match.length() - 1);
                String imageId = imageFile.substring(imageFile.lastIndexOf('/') + 1);
                substituted.append("cid:" + imageId);
                imageMap.put(imageId, imageFile);
            }
            else {
                // ignore everything but images
                substituted.append(match);
            }
            index = matcher.end();
        }
        substituted.append(input.substring(index));
        return substituted.toString();
    }
}
