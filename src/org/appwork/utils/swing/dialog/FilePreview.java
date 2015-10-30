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
package org.appwork.utils.swing.dialog;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.Files;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTHelper;

/**
 * @author thomas
 */
public class FilePreview extends JPanel implements PropertyChangeListener {

    private static final long  serialVersionUID = 68064282036848471L;

    private final JFileChooser fileChooser;
    private final JPanel       panel;
    private final JLabel       label;

    private File               file;

    public FilePreview(final JFileChooser fileChooser) {
        this.fileChooser = fileChooser;
        this.fileChooser.addPropertyChangeListener(this);

        this.panel = new JPanel(new MigLayout("ins 5", "[grow,fill]", "[grow,fill]"));
        this.panel.add(this.label = new JLabel());

        this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        this.add(new JScrollPane(this.panel), "hidemode 3,gapleft 5");
        this.setPreferredSize(new Dimension(200, 100));
    }

    public void propertyChange(final PropertyChangeEvent e) {
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(e.getPropertyName())) {
            this.file = (File) e.getNewValue();
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(e.getPropertyName())) {
            this.file = (File) e.getNewValue();
        }
        new Thread() {
            @Override
            public void run() {
                FilePreview.this.update();
            }
        }.start();
    }

    private void update() {
        if (this.file != null && this.file.isFile()) {
            try {
                final String ext = Files.getExtension(this.file.getName());
                if (ext != null) {
                    BufferedImage image = null;

                    if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("gif")) {
                        image = ImageProvider.read(this.file);
                    }

                    if (image != null) {
                        final ImageIcon ii = new ImageIcon(ImageProvider.scaleBufferedImage(image, 160, 160));
                        new EDTHelper<Object>() {

                            @Override
                            public Object edtRun() {
                                FilePreview.this.label.setIcon(ii);
                                final int w = FilePreview.this.fileChooser.getWidth() / 3;
                                FilePreview.this.setPreferredSize(new Dimension(w, 100));
                                FilePreview.this.fileChooser.revalidate();
                                return null;
                            }

                        }.start();
                        return;

                    }
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }

        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                FilePreview.this.label.setIcon(null);
                FilePreview.this.label.setText("");
                FilePreview.this.setPreferredSize(new Dimension(0, 0));
                FilePreview.this.fileChooser.revalidate();
                return null;
            }

        }.start();
    }
}
