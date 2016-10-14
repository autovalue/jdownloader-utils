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
 *     The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact as.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.resources;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.appwork.utils.FileHandler;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ide.IDEUtils;
import org.appwork.utils.os.CrossSystem;

/**
 * @author thomas
 * @date 14.10.2016
 *
 */
public class AWIconCleanUP {
    public static void main(String[] args) throws ClassNotFoundException {
        cleanup("themes/themes/standard/org/appwork/images", AWIcon.class);
    }

    /**
     * @param clazz
     * @param class1
     * @param string
     * @throws ClassNotFoundException
     */
    public static void cleanup(String rel, Class<? extends IconRef>... classes) throws ClassNotFoundException {
        File project = IDEUtils.getProjectFolder(Class.forName(new Exception().getStackTrace()[1].getClassName()));
        final File themesFolder = new File(project, rel);
        final HashSet<String> icons = new HashSet<String>();
        HashMap<String, Collection<Class<? extends IconRef>>> clsMap = new HashMap<String, Collection<Class<? extends IconRef>>>();
        for (Class<? extends IconRef> cl : classes) {
            for (IconRef e : cl.getEnumConstants()) {
                icons.add(e.path());
                Collection<Class<? extends IconRef>> ls = clsMap.get(e.path());
                if (ls == null) {
                    ls = new HashSet<Class<? extends IconRef>>();
                    clsMap.put(e.path(), ls);
                }
                ls.add(cl);
            }
        }
        for (String icon : icons) {
            File png = new File(themesFolder, icon + ".png");
            File svg = new File(themesFolder, icon + ".svg");
            if (!CrossSystem.caseSensitiveFileExists(png) && !CrossSystem.caseSensitiveFileExists(svg)) {
                System.err.println("Missing: " + png + " " + clsMap.get(icon));
            }
        }
        org.appwork.utils.Files.walkThroughStructure(new FileHandler<RuntimeException>() {
            @Override
            public void intro(File f) throws RuntimeException {
                // TODO Auto-generated method stub
            }

            @Override
            public boolean onFile(File f, int depths) throws RuntimeException {
                String rel = Files.getRelativePath(themesFolder, f);
                rel = rel.replace("\\", "/");
                if (StringUtils.isNotEmpty(rel) && (rel.endsWith(".svg") || rel.endsWith(".png"))) {
                    rel = rel.replaceAll("\\.(png|svg)$", "");
                    if (!icons.contains(rel)) {
                        System.err.println("Useless Icon: " + f + " ( " + rel + " ) ");
                    }
                }
                return true;
            }

            @Override
            public void outro(File f) throws RuntimeException {
                // TODO Auto-generated method stub
            }
        }, themesFolder);
    }
}
