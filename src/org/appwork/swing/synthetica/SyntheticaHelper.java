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
package org.appwork.swing.synthetica;

import java.awt.Font;
import java.awt.Window;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.UIManager;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;

public class SyntheticaHelper {

    public final SyntheticaSettings config;

    /**
     *
     */
    public SyntheticaHelper() {
        this(JsonConfig.create(SyntheticaSettings.class));
    }

    /**
     * @param create
     */
    public SyntheticaHelper(SyntheticaSettings settings) {
        this.config = settings;
    }

    public String getDefaultFont() {
        switch (CrossSystem.getOS()) {
        case WINDOWS_7:
        case WINDOWS_8:
        case WINDOWS_VISTA:
            return "Segoe UI";
        }
        return null;
    }

    /**
     * @param config
     * @param locale
     * @return
     */
    public String getFontName(final SyntheticaSettings config, final LanguageFileSetup locale) {
        final String fontName = config.getFontName();
        final String fontFromTranslation = locale.config_fontname();
        String newFontName = null;
        if (fontFromTranslation != null && !"default".equalsIgnoreCase(fontFromTranslation)) {
            /* we have customized fontName in translation */
            /* lower priority than fontName in settings */
            newFontName = fontFromTranslation;
        }
        if (fontName != null && !"default".equalsIgnoreCase(fontName)) {
            /* we have customized fontName in settings, it has highest priority */
            newFontName = fontName;
        }
        if (newFontName == null) {
            newFontName = getDefaultFont();
        }
        return newFontName;
    }

    public int getFontScaleFaktor(final SyntheticaSettings config, final LanguageFileSetup translationFileConfig) {
        int fontScale = -1;
        try {
            fontScale = Integer.parseInt(translationFileConfig.config_fontscale_faktor());
        } catch (final Exception e) {
        }
        if (config.getFontScaleFactor() != 100 || fontScale <= 0) {
            fontScale = config.getFontScaleFactor();
        }
        return fontScale;
    }

    public static void init() throws IOException {
        new SyntheticaHelper().load();
    }

    /**
     * @throws IOException
     *
     */
    public void load() throws IOException {
        load("de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel");
    }

    public void load(final String laf) throws IOException {
        load(laf, readLicense());
    }

    /**
     * @return
     * @throws IOException
     */
    private String readLicense() throws IOException {
        final URL url = Application.getRessourceURL("cfg/synthetica-license.key");
        if (url == null) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Missing Look And Feel License. Reverted to your System Look And Feel!");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Missing Look And Feel License. Reverted to your System Look And Feel!");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Missing Look And Feel License.");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("You can only use Synthetica Look and Feel in official JDownloader versions.");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Reverted to your System Look And Feel!");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("If you are a developer, and want to do some gui work on the offical JDownloader Look And Feel, write e-mail@appwork.org to get a developer Look And Feel Key");
            throw new WTFException("No Synthetica License Found!");
        }
        return IO.readURLToString(url);
    }

    /**
     * @param string
     * @throws IOException
     */
    public void load(final String laf, String license) throws IOException {
        if (UIManager.get("Synthetica.animation.enabled") != null) {
            LoggerFactory.getDefaultLogger().info("Synthetica Look And Feel is already Set");
            return;
        }
        if (CrossSystem.isMac()) {
            if (checkIfMacInitWillFail()) {
                System.runFinalization();
                System.gc();
                if (checkIfMacInitWillFail()) {
                    throw new IOException("Cannot Init LookAndFeel. Windows Are Open");
                }
            }
        }
        if (StringUtils.isEmpty(license)) {
            license = readLicense();
        }
        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("LaF init: " + laf);
        final long start = System.currentTimeMillis();
        try {
            setLicense(license);
            JFrame.setDefaultLookAndFeelDecorated(false);
            JDialog.setDefaultLookAndFeelDecorated(false);
            final LanguageFileSetup locale = TranslationFactory.create(LanguageFileSetup.class);
            boolean decorated = config.isWindowDecorationEnabled();
            de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setWindowsDecorated(decorated);
            UIManager.put("Synthetica.window.decoration", decorated);
            UIManager.put("Synthetica.text.antialias", config.isTextAntiAliasEnabled());
            UIManager.put("Synthetica.extendedFileChooser.rememberPreferences", Boolean.FALSE);
            UIManager.put("Synthetica.extendedFileChooser.rememberLastDirectory", Boolean.FALSE);
            // /* http://www.jyloo.com/news/?pubId=1297681728000 */
            // /* we want our own FontScaling, not SystemDPI */
            UIManager.put("Synthetica.font.respectSystemDPI", config.isFontRespectsSystemDPI());
            final int fontScale = getFontScaleFaktor(config, locale);
            UIManager.put("Synthetica.font.scaleFactor", fontScale);
            if (config.isFontRespectsSystemDPI() && fontScale != 100) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("SystemDPI might interfere with JD's FontScaling");
            }
            UIManager.put("Synthetica.animation.enabled", config.isAnimationEnabled());
            if (CrossSystem.isWindows()) {
                /* only windows opaque works fine */
                UIManager.put("Synthetica.window.opaque", config.isWindowOpaque());
            } else {
                /* must be true to disable it..strange world ;) */
                UIManager.put("Synthetica.window.opaque", true);
            }
            UIManager.put("Synthetica.menu.toolTipEnabled", true);
            UIManager.put("Synthetica.menuItem.toolTipEnabled", true);
            if (!Application.isHeadless()) {
                de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel(laf);
            }
            de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setExtendedFileChooserEnabled(false);
            final String fontName = getFontName(config, locale);
            int fontSize = de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.getFont().getSize();
            fontSize = fontScale * fontSize / 100;
            if (fontName != null) {
                /* change Font */
                final int oldStyle = de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.getFont().getStyle();
                final Font newFont = new Font(fontName, oldStyle, fontSize);
                de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setFont(newFont, false);
            }
            ExtPasswordField.MASK = "*******";
        } finally {
            final long time = System.currentTimeMillis() - start;
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("LAF Init duration: " + time + "ms");
        }
    }

    public void setLicense(String license) {
        /* we save around x-400 ms here by not using AES */
        if (license == null) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Missing Look And Feel License. Reverted to your System Look And Feel!");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Missing Look And Feel License. Reverted to your System Look And Feel!");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Missing Look And Feel License.");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("You can only use Synthetica Look and Feel in official JDownloader versions.");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Reverted to your System Look And Feel!");
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("If you are a developer, and want to do some gui work on the offical JDownloader Look And Feel, write e-mail@appwork.org to get a developer Look And Feel Key");
            throw new WTFException("No Synthetica License Found!");
        }
        /*
         * NOTE: This Licensee Information may only be used by AppWork UG. If you like to create derived creation based on this sourcecode,
         * you have to remove this license key. Instead you may use the FREE Version of synthetica found on javasoft.de
         */
        String[] licenseLines = Regex.getLines(license);
        // final String[] li = { };
        final ArrayList<String> valids = new ArrayList<String>();
        for (final String s : licenseLines) {
            if (!s.trim().startsWith("#") && !s.trim().startsWith("//")) {
                valids.add(s);
            }
        }
        licenseLines = valids.toArray(new String[] {});
        final String key = licenseLines[0];
        if (key.split("-").length == 5) {
            throw new WTFException("Outdated Licensefile: " + Application.getRessourceURL("cfg/synthetica-license.key"));
        }
        final String[] li = new String[licenseLines.length - 1];
        System.arraycopy(licenseLines, 1, li, 0, li.length);
        if (key != null) {
            UIManager.put("Synthetica.license.info", li);
            UIManager.put("Synthetica.license.key", key);
        }
    }

    protected boolean checkIfMacInitWillFail() {
        // synthetica init fails on mac if there are already active windows
        final Window awindow[] = Window.getWindows();
        final int j = awindow.length;
        for (int i = 0; i < j; i++) {
            final Window window = awindow[i];
            final boolean flag = !(window instanceof JWindow) && !(window instanceof JFrame) && !(window instanceof JDialog);
            if (!window.getClass().getName().contains("Popup$HeavyWeightWindow") && !flag) {
                return true;
            }
        }
        return false;
    }

}
