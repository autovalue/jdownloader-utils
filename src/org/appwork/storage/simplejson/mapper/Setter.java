/**
 * 
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany   
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.storage.simplejson.mapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.annotations.ConvertValueFrom;
import org.appwork.storage.config.annotations.JSonFieldName;


/**
 * @author thomas
 * 
 */
public class Setter {

    private final String key;
    private final Method method;
    private final Type   type;
    private Class<?>     convertFromClass;

    /**
     * @param substring
     * @param m
     */
    public Setter(final String name, final Method m) {

        final JSonFieldName jsFieldName = m.getAnnotation(JSonFieldName.class);
        if (jsFieldName != null) {
            this.key = jsFieldName.value();
        } else {
            this.key = name;
        }
        final ConvertValueFrom convert = m.getAnnotation(ConvertValueFrom.class);
        if (convert != null) {
            this.convertFromClass = convert.value();
        }
        this.method = m;
        m.setAccessible(true);
        this.type = m.getGenericParameterTypes()[0];

    }

    public String getKey() {
        return this.key;
    }

    public Method getMethod() {
        return this.method;
    }

    public Type getType() {
        return this.type;
    }

    @SuppressWarnings("unchecked")
    public void setValue(final Object inst, Object parameter) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        if (this.type instanceof Class && ((Class<?>) this.type).isEnum() && parameter != null) {
            parameter = Enum.valueOf((Class<Enum>) this.type, parameter + "");
        }

        if (this.convertFromClass != null && parameter != null && parameter.getClass().isAssignableFrom(this.convertFromClass)) {
            if (this.convertFromClass == String.class) {

                if (this.type == Byte.class || this.type == byte.class) {
                    parameter = Byte.parseByte((String) parameter);

                } else if (this.type == Character.class || this.type == char.class) {
                    parameter = (char) Byte.parseByte((String) parameter);

                } else if (this.type == Short.class || this.type == short.class) {
                    parameter = Short.parseShort((String) parameter);

                } else if (this.type == Integer.class || this.type == int.class) {
                    parameter = Integer.parseInt((String) parameter);
                } else if (this.type == Long.class || this.type == long.class) {
                    parameter = Long.parseLong((String) parameter);
                } else if (this.type == Float.class || this.type == float.class) {
                    parameter = Float.parseFloat((String) parameter);

                } else if (this.type == Double.class || this.type == double.class) {
                    parameter = Double.parseDouble((String) parameter);

                } else {
                    throw new WTFException("Unsupported Convert " + this.convertFromClass + " to " + this.getType());
                }

            } else {
                throw new WTFException("Unsupported Convert " + this.convertFromClass + " to " + this.getType());
            }

        }
        // System.out.println(this.key + " = " + parameter + " " + this.type);
        try {
            this.method.invoke(inst, parameter);
        } catch (final IllegalArgumentException e) {
                  org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe(this.method + " " + parameter);
            throw e;
        }

    }
}
