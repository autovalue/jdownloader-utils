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
package org.appwork.storage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.crypto.Crypto;
import org.appwork.utils.reflection.Clazz;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

public class JSonStorage {
    /* hash map contains file location as string and the storage instance */
    private static final HashMap<String, Storage>     MAP         = new HashMap<String, Storage>();

    private static JSONMapper                         JSON_MAPPER = new SimpleMapper();
    /* default key for encrypted json */
    static public byte[]                              KEY         = new byte[] { 0x01, 0x02, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };
    private static final HashMap<File, AtomicInteger> LOCKS       = new HashMap<File, AtomicInteger>();

    static {
        /* shutdown hook to save all open Storages */
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public long getMaxDuration() {
                return 0;
            }

            @Override
            public int getHookPriority() {
                return 0;
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                JSonStorage.close();
            }

            @Override
            public String toString() {
                return "ShutdownEvent: Save JSonStorages";
            }

        });

    }

    /**
     * Cecks of the JSOn Mapper can map this Type
     *
     * @param allowNonStorableObjects
     *            TODO
     * @param genericReturnType
     *
     * @throws InvalidTypeException
     */
    public static void canStore(final Type gType, final boolean allowNonStorableObjects) throws InvalidTypeException {
        HashSet<Object> dupeID = new HashSet<Object>();
        JSonStorage.canStoreIntern(gType, gType.toString(), allowNonStorableObjects, dupeID);
    }

    /**
     * @param gType
     * @param allowNonStorableObjects
     *            TODO
     * @param dupeID
     * @param string
     * @throws InvalidTypeException
     */
    private static void canStoreIntern(final Type gType, final String path, final boolean allowNonStorableObjects, HashSet<Object> dupeID) throws InvalidTypeException {
        if (!dupeID.add(gType)) {
            return;
        }
        if (gType == Object.class) {
            if (allowNonStorableObjects) {
                return;
            }
            throw new InvalidTypeException(gType, "Cannot store Object: " + path);
        }
        if (gType instanceof Class) {
            final Class<?> type = (Class<?>) gType;
            if (type == void.class) {
                throw new InvalidTypeException(gType, "Void is not accepted: " + path);
            }
            if (type.isPrimitive()) {

                return;
            }
            if (type == Boolean.class || type == Long.class || type == Integer.class || type == Byte.class || type == Double.class || type == Float.class || type == String.class) {
                return;
            }
            if (type.isEnum()) {
                return;
            }

            if (type.isArray()) {
                final Class<?> arrayType = type.getComponentType();

                JSonStorage.canStoreIntern(arrayType, path + "[" + arrayType + "]", allowNonStorableObjects, dupeID);

                return;
            }
            // we need an empty constructor

            if (List.class.isAssignableFrom(type)) {

                return;

            }
            if (Map.class.isAssignableFrom(type)) {

                return;

            }

            if (HashSet.class.isAssignableFrom(type)) {

                return;

            }
            if (Storable.class.isAssignableFrom(type) || allowNonStorableObjects) {
                try {

                    type.getDeclaredConstructor(new Class[] {});
                    for (final Method m : type.getDeclaredMethods()) {
                        if (m.getName().startsWith("get")) {

                            if (m.getParameterTypes().length > 0) {
                                throw new InvalidTypeException(gType, "Getter " + path + "." + m + " has parameters.");
                            }
                            JSonStorage.canStoreIntern(m.getGenericReturnType(), path + "->" + m.getGenericReturnType(), allowNonStorableObjects, dupeID);

                        } else if (m.getName().startsWith("set")) {
                            if (m.getParameterTypes().length != 1) {
                                throw new InvalidTypeException(gType, "Setter " + path + "." + m + " has != 1 Parameters.");
                            }

                        }
                    }

                    return;
                } catch (final NoSuchMethodException e) {
                    throw new InvalidTypeException(gType, "Storable " + path + " has no empty Constructor");
                }

            }
        } else if (gType instanceof ParameterizedType) {
            final ParameterizedType ptype = (ParameterizedType) gType;

            final Type raw = ((ParameterizedType) gType).getRawType();
            JSonStorage.canStoreIntern(raw, path, allowNonStorableObjects, dupeID);
            for (final Type t : ptype.getActualTypeArguments()) {
                JSonStorage.canStoreIntern(t, path + "(" + t + ")", allowNonStorableObjects, dupeID);
            }

            return;

        } else if (gType instanceof GenericArrayType) {
            final GenericArrayType atype = (GenericArrayType) gType;
            final Type t = atype.getGenericComponentType();
            JSonStorage.canStoreIntern(t, path + "[" + t + "]", allowNonStorableObjects, dupeID);

            return;
        } else {
            throw new InvalidTypeException(gType, "Generic Type Structure not implemented: " + gType.getClass() + " in " + path);
        }

        throw new InvalidTypeException(gType, "Type " + path + " is not supported.");

    }

    /**
     * @param returnType
     * @return
     */
    public static boolean canStorePrimitive(final Class<?> type) {
        return Clazz.isPrimitive(type) || type == String.class || type.isEnum();
    }

    public static JSONMapper getMapper() {
        return JSonStorage.JSON_MAPPER;
    }

    /**
     * TODO: Difference to {@link #getStorage(String)} ?
     */
    public static Storage getPlainStorage(final String name) throws StorageException {
        final String id = name + "_plain";
        Storage ret = null;
        synchronized (JSonStorage.MAP) {
            ret = JSonStorage.MAP.get(id);
            if (ret != null) {
                return ret;
            }
        }
        ret = new JsonKeyValueStorage(name, true);
        synchronized (JSonStorage.MAP) {
            final Storage ret2 = JSonStorage.MAP.get(id);
            if (ret2 != null) {
                return ret2;
            }
            JSonStorage.MAP.put(id, ret);
        }
        return ret;
    }

    public static Storage getStorage(final String name) throws StorageException {
        final String id = name + "_crypted";
        Storage ret = null;
        synchronized (JSonStorage.MAP) {
            ret = JSonStorage.MAP.get(id);
            if (ret != null) {
                return ret;
            }
        }
        ret = new JsonKeyValueStorage(name);
        synchronized (JSonStorage.MAP) {
            final Storage ret2 = JSonStorage.MAP.get(id);
            if (ret2 != null) {
                return ret2;
            }
            JSonStorage.MAP.put(id, ret);
        }
        return ret;
    }

    private static synchronized Object requestLock(final File file) {
        AtomicInteger lock = JSonStorage.LOCKS.get(file);
        if (lock == null) {
            lock = new AtomicInteger(0);
            JSonStorage.LOCKS.put(file, lock);
        }
        lock.incrementAndGet();
        return lock;
    }

    public static <E> E restoreFrom(final File file, final boolean plain, final byte[] key, final TypeRef<E> type, final E def) {
        final Object lock = JSonStorage.requestLock(file);
        synchronized (lock) {
            String stri = null;
            byte[] str = null;
            try {
                // I doubt that this helps to solve any bug. remove it if we do not miss it until 2016 ;-P
                // final File tmpfile = new File(file.getAbsolutePath() + ".tmp");
                // if (tmpfile.exists() && tmpfile.length() > 0) {
                // /* tmp files exists, try to restore */
                // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("TMP file " +
                // tmpfile.getAbsolutePath() + " found");
                // try {
                // // load it
                // str = IO.readFile(tmpfile);
                // E ret;
                // // try to parse it
                // if (plain) {
                // ret = JSonStorage.restoreFromString(stri = new String(str, "UTF-8"), type, def);
                // } else {
                // ret = JSonStorage.restoreFromString(stri = Crypto.decrypt(str, key), type, def);
                // }
                //
                // if (ret != def) {
                // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Could restore tmp file");
                // // replace normal file with tmp file
                // file.delete();
                // tmpfile.renameTo(file);
                // if (ret == null) {
                // return def;
                // }
                // return ret;
                // } else {
                // // probably bad tmp file
                // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Could not restore tmp file. json restore method returned default value.");
                // }
                // } catch (final Exception e) {
                // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Could not restore tmp file");
                // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                // } finally {
                // /* tmp file must be gone after read */
                // tmpfile.delete();
                // }
                // }
                final File res = file;
                if (!res.exists() || res.length() == 0) {
                    return def;
                }
                str = IO.readFile(res);
                if (plain) {
                    return JSonStorage.restoreFromString(stri = new String(str, "UTF-8"), type, def);
                } else {
                    return JSonStorage.restoreFromString(stri = Crypto.decrypt(str, key), type, def);
                }

            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning(file.getAbsolutePath() + ":read:" + stri);
                try {
                    if (str != null) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe(file.getAbsolutePath() + ":original:" + new String(str, "UTF-8"));
                    }
                } catch (final Throwable e2) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e2);
                }
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            } finally {
                JSonStorage.unLock(file);
            }
            return def;
        }
    }

    /**
     * restores a store json object
     *
     * @param <E>
     * @param string
     *            name of the json object. example: cfg/savedobject.json
     * @param type
     *            TypeRef instance. This is important for generic classes. for example: new TypeRef<ArrayList<Contact>>(){} to restore type
     *            java.util.List<Contact>
     * @param def
     *            defaultvalue. if typeref is not set, the method tries to use the class of def as restoreclass
     * @return
     */

    public static <E> E restoreFrom(final String string, final TypeRef<E> type, final E def) {
        final boolean plain = string.toLowerCase().endsWith(".json");
        return JSonStorage.restoreFrom(Application.getResource(string), plain, JSonStorage.KEY, type, def);
    }

    public static <E> E restoreFromFile(final File file, final E def) {
        final E ret = JSonStorage.restoreFrom(file, true, null, null, def);
        if (ret == null) {
            return def;
        }
        return ret;
    }

    public static <E> E restoreFromFile(final String relPath, final E def) {
        final boolean plain = relPath.toLowerCase().endsWith(".json");
        return JSonStorage.restoreFrom(Application.getResource(relPath), plain, JSonStorage.KEY, null, def);
    }

    public static <E> E restoreFromString(final byte[] data, final boolean plain, final byte[] key, final TypeRef<E> type, final E def) {
        if (data == null) {
            return def;
        }
        String string = null;
        try {
            if (!plain) {
                string = Crypto.decrypt(data, key);
            } else {
                string = new String(data, "UTF-8");
            }
            if (type != null) {
                return JSonStorage.JSON_MAPPER.stringToObject(string, type);
            } else {
                return (E) JSonStorage.JSON_MAPPER.stringToObject(string, def.getClass());
            }
        } catch (final Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning(string);
            return def;
        }
    }

    /**
     * @param <T>
     * @param string
     * @param class1
     * @throws IOException
     */
    public static <T> T restoreFromString(final String string, final Class<T> class1) throws StorageException {
        try {
            return JSonStorage.JSON_MAPPER.stringToObject(string, class1);
        } catch (final Exception e) {
            throw new StorageException(string, e);
        }
    }

    public static <E> E restoreFromString(final String string, final TypeRef<E> type) {
        if (string == null || "".equals(string)) {
            return null;
        }
        return JSonStorage.JSON_MAPPER.stringToObject(string, type);
    }

    @SuppressWarnings("unchecked")
    public static <E> E restoreFromString(final String string, final TypeRef<E> type, final E def) {
        if (string == null || "".equals(string)) {
            return def;
        }
        try {
            if (type != null) {
                return JSonStorage.JSON_MAPPER.stringToObject(string, type);
            } else {
                return (E) JSonStorage.JSON_MAPPER.stringToObject(string, def.getClass());
            }
        } catch (final Exception e) {
            if (string.length() < 32767) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Error parsing String: " + string);
            }
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            return def;
        }
    }

    private static void close() {
        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Start Saving Storage");
        final List<Storage> storages;
        synchronized (JSonStorage.MAP) {
            storages = new ArrayList<Storage>(JSonStorage.MAP.values());
        }
        for (final Storage storage : storages) {
            try {
                storage.save();
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            } finally {
                try {
                    storage.close();
                } catch (final Throwable e2) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e2);
                }
            }
        }
        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("ENDED Saving Storage");
    }

    public static void saveTo(final File file, final boolean plain, final byte[] key, final String json) throws StorageException {
        final Object lock = JSonStorage.requestLock(file);
        synchronized (lock) {
            final File tmp = new File(file.getAbsolutePath() + ".tmp");
            try {
                tmp.getParentFile().mkdirs();
                tmp.delete();
                if (plain) {
                    /* uncrypted */
                    IO.writeToFile(tmp, json.getBytes("UTF-8"));
                } else {
                    /* encrypted */
                    IO.writeToFile(tmp, Crypto.encrypt(json, key));
                }
                if (file.exists()) {
                    if (!file.delete()) {
                        throw new StorageException("Could not overwrite file: " + file.getAbsolutePath());
                    }
                }
                if (!tmp.renameTo(file)) {
                    throw new StorageException("Could not rename file: " + tmp + " to " + file);
                }
            } catch (final Exception e) {
                throw new StorageException("Can not write to " + tmp.getAbsolutePath(), e);
            } finally {
                JSonStorage.unLock(file);
            }
        }
    }

    /**
     * @param file
     * @param packageData
     */
    public static void saveTo(final File file, final Object packageData) {
        final boolean plain = file.getName().toLowerCase().endsWith(".json");
        JSonStorage.saveTo(file, plain, JSonStorage.KEY, JSonStorage.serializeToJson(packageData));
    }

    /**
     * @param pathname
     * @param json
     * @throws StorageException
     */
    public static void saveTo(final String pathname, final String json) throws StorageException {
        JSonStorage.saveTo(pathname, json, JSonStorage.KEY);
    }

    /**
     * @param pathname
     * @param json
     * @param kEY2
     */
    public static void saveTo(final String pathname, final String json, final byte[] key) {
        final File file = Application.getResource(pathname);
        final Object lock = JSonStorage.requestLock(file);
        synchronized (lock) {
            try {
                final File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
                tmp.getParentFile().mkdirs();
                tmp.delete();
                if (new Regex(pathname, ".+\\.json").matches()) {
                    /* uncrypted */
                    IO.writeToFile(tmp, json.getBytes("UTF-8"));
                } else {
                    /* encrypted */
                    IO.writeToFile(tmp, Crypto.encrypt(json, key));
                }
                if (file.exists()) {
                    if (!file.delete()) {
                        throw new StorageException("Could not overwrite file: " + file);
                    }
                }
                if (!tmp.renameTo(file)) {
                    throw new StorageException("Could not rename file: " + tmp + " to " + file);
                }
            } catch (final IOException e) {
                throw new StorageException(e);
            } finally {
                JSonStorage.unLock(file);
            }
        }
    }

    /**
     * @param list
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     */
    public static String serializeToJson(final Object list) throws StorageException {
        try {
            return JSonStorage.JSON_MAPPER.objectToString(list);
        } catch (final Exception e) {
            throw new StorageException(e);
        }
    }

    public static void setMapper(final JSONMapper mapper) {
        JSonStorage.JSON_MAPPER = mapper;
    }

    /**
     * @param string
     * @param list
     */
    public static void storeTo(final String string, final Object list) {
        try {
            JSonStorage.saveTo(string, JSonStorage.serializeToJson(list));
        } catch (final Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * This method throws Exceptions
     *
     * @param string
     * @param type
     * @param def
     * @return
     */
    public static <E> E stringToObject(final String string, final TypeRef<E> type, final E def) {
        if (StringUtils.isEmpty(string)) {
            throw new IllegalArgumentException("cannot stringToObject from empty string");
        }
        if (type != null) {
            return JSonStorage.JSON_MAPPER.stringToObject(string, type);
        } else {
            return (E) JSonStorage.JSON_MAPPER.stringToObject(string, def.getClass());
        }
    }

    /**
     * USe this method for debug code only. It is NOT guaranteed that this method returns json formated text. Use
     * {@link #serializeToJson(Object)} instead
     *
     * @param list
     * @return
     */
    public static String toString(final Object list) {
        try {

            return JSonStorage.JSON_MAPPER.objectToString(list);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return list.toString();
    }

    private static synchronized void unLock(final File file) {
        final AtomicInteger lock = JSonStorage.LOCKS.get(file);
        if (lock != null) {
            if (lock.decrementAndGet() == 0) {
                JSonStorage.LOCKS.remove(file);
            }
        }
    }

    /**
     * @param <E>
     * @param o
     * @param typeRef
     * @return
     */
    public static <E> E convert(Object o, TypeRef<E> typeRef) {
        // TODO Auto-generated method stub SimpleMapper m = new SimpleMapper();

        return JSON_MAPPER.convert(o, typeRef);
    }

}
