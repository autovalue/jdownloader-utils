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
package org.appwork.utils.swing;

import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

import org.appwork.utils.StringUtils;

public class SwingUtils {
    /**
     * Calculates the position of a frame to be in the center of an other frame.
     *
     * @param parentFrame
     * @param frame
     * @return
     */
    public static Point getCenter(final Component parentFrame, final Window frame) {
        final Point point = new Point();
        int x = 0, y = 0;

        if (parentFrame == null || frame == null) {
            point.setLocation(x, y);
            return point;
        }

        x = parentFrame.getLocation().x + parentFrame.getSize().width / 2 - frame.getSize().width / 2;
        y = parentFrame.getLocation().y + parentFrame.getSize().height / 2 - frame.getSize().height / 2;

        point.setLocation(x, y);

        return point;
    }

    /**
     * @param frame
     * @param string
     */
    public static JComponent getComponentByName(final JComponent frame, final String name) {
        JComponent ret = null;
        for (final Component c : frame.getComponents()) {

            if (c instanceof JComponent) {
                if (c.getName() != null && c.getName().equals(name)) {
                    return (JComponent) c;
                } else {
                    ret = SwingUtils.getComponentByName((JComponent) c, name);
                    if (ret != null) {
                        return ret;
                    }

                }
            }
        }
        return null;

    }

    public static Window getWindowForComponent(final Component parentComponent) {
        if (parentComponent == null) {
            return JOptionPane.getRootFrame();
        }
        if (parentComponent instanceof Frame || parentComponent instanceof java.awt.Dialog) {
            return (Window) parentComponent;
        }
        return SwingUtils.getWindowForComponent(parentComponent.getParent());
    }

    /**
     * Sets a component's opaque status
     *
     * @param descriptionField
     * @param b
     */
    public static JComponent setOpaque(final JComponent descriptionField, final boolean b) {
        descriptionField.setOpaque(b);
        descriptionField.putClientProperty("Synthetica.opaque", b ? Boolean.TRUE : Boolean.FALSE);
        return descriptionField;
    }

    /**
     * @param btnDetails
     */
    public static <T extends AbstractButton> T toBold(final T button) {
        final Font f = button.getFont();
        button.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        return button;
    }

    /**
     * @param ret
     * @return
     * @return
     */
    public static <T extends JLabel> T toBold(final T label) {
        final Font f = label.getFont();
        label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        return label;
    }

    /**
     * @param label
     */
    public static <T extends JTextComponent> T toBold(final T label) {
        final Font f = label.getFont();
        label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        return label;
    }

    /**
     * @param fc
     */
    public static void printComponentTree(final JComponent fc) {
        printComponentTree(fc, "");

    }

    /**
     * @param fc
     * @param string
     */
    private static void printComponentTree(final JComponent fc, final String string) {

        // c.setVisible(false);
        for (int i = 0; i < fc.getComponentCount(); i++) {
            final Component cc = fc.getComponent(i);
            System.out.println(string + "[" + i + "]" + cc.getClass().getSuperclass().getSimpleName() + ":" + cc + " Opaque: " + cc.isOpaque());

            if (cc instanceof JComponent) {
                printComponentTree((JComponent) cc, string + "[" + i + "]");
            }

        }
    }

    /**
     * @param fc
     * @param i
     * @param j
     * @param k
     */
    public static JComponent getParent(JComponent parent, final int... path) {

        for (final int i : path) {
            parent = (JComponent) parent.getComponent(i);
        }
        return parent;

    }

    public static GraphicsDevice getScreenByLocation(Point p) {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        final GraphicsDevice[] screens = ge.getScreenDevices();

        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();

            if (bounds.contains(p)) {
                return screen;

            }

        }
        return null;

    }

    /**
     * @return
     */
    public static GraphicsDevice getScreenByLocation(int x, int y) {
        return getScreenByLocation(new Point(x, y));
    }

    /**
     * @param screen
     * @return
     */
    public static Rectangle getUsableScreenBounds(GraphicsDevice screen) {
        final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        return bounds;
    }

    /**
     * @param bounds
     * @return
     */
    public static GraphicsDevice getScreenByBounds(Rectangle bounds) {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        final GraphicsDevice[] screens = ge.getScreenDevices();
        Rectangle biggestIntersection = null;
        GraphicsDevice bestScreen = null;
        for (final GraphicsDevice screen : screens) {
            final Rectangle sb = screen.getDefaultConfiguration().getBounds();
            Rectangle intersection = sb.intersection(bounds);
            if (intersection != null) {
                if (biggestIntersection == null || intersection.width * intersection.height > biggestIntersection.width * biggestIntersection.height) {
                    biggestIntersection = intersection;
                    bestScreen = screen;
                    if (intersection.equals(bounds)) {
                        // it will not get better
                        break;
                    }
                }
            }

        }
        return (biggestIntersection == null || biggestIntersection.width * biggestIntersection.height == 0) ? null : bestScreen;
    }

    /**
     * @param screenID
     * @return
     */
    public static GraphicsDevice getScreenByID(String screenID) {
        if (StringUtils.isEmpty(screenID)) {
            return null;
        }
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        final GraphicsDevice[] screens = ge.getScreenDevices();

        for (final GraphicsDevice screen : screens) {

            if (StringUtils.equals(screen.getIDstring(), screenID)) {
                return screen;

            }

        }
        return null;
    }

}
