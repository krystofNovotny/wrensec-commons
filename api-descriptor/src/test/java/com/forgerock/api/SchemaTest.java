/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package com.forgerock.api;

import com.forgerock.api.markdown.MarkdownReader;
import com.forgerock.api.markdown.PropertyRecord;
import com.forgerock.api.markdown.TypeDescriptor;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Tests the beans and builders against the markdown document
 */
public class SchemaTest {

    public static final String PUT = "put";
    public static final String ENUMS_PACKAGE = "com.forgerock.api.enums";
    public static final String ARRAY_CLASS_NAME_FORMAT = "[L%s";
    private MarkdownReader mdReader;
    private List<TypeDescriptor> typeDescriptors;
    private static final String NO_SUCH_METHOD_EXCEPTION_MESSAGE = "# %s(%s) method not available in the schema.";
    private static final String MAP_PARAMETER_TYPE_MESSAGE = "java.util.Map<java.lang.String,%s>";

    @BeforeTest
    public void before() {
        mdReader = new MarkdownReader();
        typeDescriptors = mdReader.generateTypeDescriptorList();
    }

    @Test
    public void schemaObjectCheckAgainstMarkdownDocumentTest() throws ClassNotFoundException, NoSuchMethodException {
        for (TypeDescriptor typeDescriptor : typeDescriptors) {
            //We are not checking the Schema bean at the moment
            if (!typeDescriptor.getName().equals("Schema")) {
                checkApiBean(typeDescriptor);
            }
        }
    }

    public void checkApiBean(TypeDescriptor typeDescriptor) throws ClassNotFoundException, NoSuchMethodException {
        Class builderClass = Class.forName(
                MarkdownReader.API_DESCRIPTION_BEANS_PACKAGE + typeDescriptor.getName() + "$Builder");
        Method[] methods = builderClass.getMethods();
        checkSuperclass(typeDescriptor);
        try {
            assertThat(checkMethodNamesAndParameters(methods, typeDescriptor.getProperties()));
        } catch (NoSuchMethodException nsme) {
            throw new NoSuchMethodException(typeDescriptor.getName() + " " + nsme.getMessage());
        }
    }

    private void checkSuperclass(TypeDescriptor typeDescriptor) throws ClassNotFoundException {
        if (typeDescriptor.getSuperClass() != null) {
            String superClass = Class.forName(MarkdownReader.API_DESCRIPTION_BEANS_PACKAGE + typeDescriptor.getName())
                    .getSuperclass().getName();
            if (!superClass.equals(typeDescriptor.getSuperClass())) {
                throw new java.lang.ClassNotFoundException(
                        typeDescriptor.getName() + " does not extend " + typeDescriptor.getSuperClass());
            }
        }
    }

    private boolean checkMethodNamesAndParameters(Method[] methods, List<PropertyRecord> properties)
            throws NoSuchMethodException, ClassNotFoundException {
        for (PropertyRecord property : properties) {
            String methodName = isMapType(property) ? PUT : property.getKey();
            Method method = findMethod(methods, methodName);
            checkIfMethodAvailable(property, method);
        }
        return true;
    }

    private boolean isMapType(PropertyRecord property) {
        return property.getKey().contains("*") || property.getKey().contains("[");
    }

    private void checkIfMethodAvailable(PropertyRecord property, Method method)
            throws NoSuchMethodException, ClassNotFoundException {
        if (method == null) {
            throw new NoSuchMethodException(
                    String.format(NO_SUCH_METHOD_EXCEPTION_MESSAGE, property.getKey(), property.getType()));
        } else if (!method.getName().equalsIgnoreCase(PUT)
                && !isValidSimpleType(method, property.getType(), property.isEnumType())) {
            throw new NoSuchMethodException(
                    String.format(NO_SUCH_METHOD_EXCEPTION_MESSAGE, property.getKey(), property.getType()));
        } else if (method.getName().equalsIgnoreCase(PUT)
                && !isValidMapType(method, property.getType())) {
            throw new NoSuchMethodException(
                    String.format(NO_SUCH_METHOD_EXCEPTION_MESSAGE, PUT,
                            String.format(MAP_PARAMETER_TYPE_MESSAGE, property.getType())));
        }
    }

    private boolean isValidSimpleType(Method method, String type, boolean isEnumType) throws ClassNotFoundException {
        Class<?> methodParameter = method.getParameterTypes()[0];
        String methodParamName = methodParameter.getName();
        String methodParamPckgName = (methodParameter.getPackage() != null)
                ? methodParameter.getPackage().getName() : "";

        if (type.endsWith("[]")) {
            return methodParamName.equals(List.class.getName())
                    || (!isEnumType && methodParamName.equals(toArrayClassName(type)))
                    || (isEnumType && methodParamName.startsWith(
                    String.format(ARRAY_CLASS_NAME_FORMAT, ENUMS_PACKAGE)));
        } else {
            return (!isEnumType && methodParamName.equals(type))
                    || (isEnumType && methodParamPckgName.startsWith(ENUMS_PACKAGE));
        }
    }

    private String toArrayClassName(String type) {
        return String.format(ARRAY_CLASS_NAME_FORMAT, type.substring(0, type.indexOf("["))) + ";";
    }

    private boolean isValidMapType(Method method, String type) throws ClassNotFoundException {
        return method.getParameterTypes()[0].getName().equals(String.class.getName())
                && (method.getParameterTypes()[1].getName().equals(type)
                    || method.getParameterTypes()[1].getName().equals(
                        Class.forName(type).getInterfaces()[0].getName()));
    }

    private Method findMethod(Method[] methods, String key) {
        Method method = null;
        for (Method m : methods) {
            if (m.getName().equals(key)) {
                method = m;
            }
        }
        return method;
    }
}