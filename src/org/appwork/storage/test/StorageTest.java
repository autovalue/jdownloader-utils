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
package org.appwork.storage.test;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author thomas
 */
public class StorageTest {

    @Test
    public void defaultTest() {
        try {
            // this test has to be executed several times, because it writes on
            // app exit data to disk and evaluates it on the next start
            final Storage s = JSonStorage.getPlainStorage("org.appwork.storage.test.StorageTest");
            s.put("LONG", Long.MAX_VALUE);
            final long myLong = s.get("LONG", 0l);
            Assert.assertTrue("Restore error", myLong == Long.MAX_VALUE);
            // should convert to -1 int
            final int myInt = s.get("LONG", 0);
            Assert.assertTrue("Restore error", myInt == -1);

            s.put("TINYLONG", 100l);
            final long mytinylong = s.get("TINYLONG", 0l);
            Assert.assertTrue("Restore error", mytinylong == 100l);
            // tiny long to int conversions should work
            final long mytinyint = s.get("TINYLONG", (long) 0);
            Assert.assertTrue("Restore error", mytinyint == 100l);
        } catch (final Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void NullTest() {
        try {
            // this test has to be executed several times, because it writes on
            // app exit data to disk and evaluates it on the next start
            final Storage s = JSonStorage.getPlainStorage("org.appwork.storage.test.StorageTest");

            s.get("GET", (String) null);
            s.get("JJ", (Integer) null);

            s.put("NULL", (String) null);
            s.put("NULL", "UNNULLER");
            s.put("NOTNULL", "imnotnull");

            // nullit
            s.put("NOTNULL", (String) null);

        } catch (final Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            Assert.fail(e.getMessage());
        }

    }
}
