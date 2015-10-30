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
package org.appwork.swing.exttable;

import java.awt.Color;

import javax.swing.JLabel;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.utils.swing.SwingUtils;

/**
 * @author thomas
 * 
 */
public class ToolTip extends ExtTooltip {

    /**
     * 
     */
    private static final long serialVersionUID = -7756738003708525595L;
    private JLabel            tf;

    public ToolTip() {
        super();
    }

    @Override
    public void onShow() {
        // TODO Auto-generated method stub
        super.onShow();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.components.tooltips.ExtTooltip#createContent()
     */
    @Override
    public TooltipPanel createContent() {
        final TooltipPanel p = new TooltipPanel("ins 2,wrap 1", "[]", "[]");
        tf = new JLabel();
        Color col;
        // this.tf.setEnabled(false);
        tf.setForeground(col = new Color(getConfig().getForegroundColor()));
        tf.setBackground(null);

        SwingUtils.setOpaque(tf, false);

        p.add(tf);

        return p;
    }

    /**
     * @param txt
     */

    public void setTipText(String txt) {
        if (txt != null) {
            if (txt.contains("\r") || txt.contains("\n") && !txt.startsWith("<html>")) {
                txt = "<html>" + txt.replaceAll("[\r\n]{1,2}", "<br>") + "</html>";
            }
        }

        tf.setText(txt);
        panel.invalidate();

    }

    /**
     * DO NOT USE. use #toText instead
     */
    @Deprecated
    public String getTipText() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.components.tooltips.ExtTooltip#toText()
     */
    @Override
    public String toText() {

        return tf.getText();
    }

}
