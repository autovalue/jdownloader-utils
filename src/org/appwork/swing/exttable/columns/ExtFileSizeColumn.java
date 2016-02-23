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
package org.appwork.swing.exttable.columns;

import java.awt.Graphics;
import java.text.DecimalFormat;
import java.text.FieldPosition;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.swing.renderer.RenderLabel;

public abstract class ExtFileSizeColumn<E> extends ExtColumn<E> {

    /**
     *
     */
    private static final long     serialVersionUID = -5812486934156037376L;
    protected final RenderLabel   renderer;
    private final StringBuffer    sb;
    protected final DecimalFormat formatter;
    protected final String        zeroString       = "~";

    /**
     * @param createtablemodel_column_size
     */
    public ExtFileSizeColumn(final String name) {
        this(name, null);
    }

    public ExtFileSizeColumn(final String name, final ExtTableModel<E> table) {
        super(name, table);
        this.renderer = new RenderLabel() {
            /**
             *
             */
            private static final long serialVersionUID = 1045896241157027789L;

            protected void paintComponent(final Graphics g) {
                super.paintComponent(g);
            }

            @Override
            public boolean isVisible() {

                return false;
            }
        };
        this.renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        setRowSorter(new ExtDefaultRowSorter<E>() {
            /**
             * sorts the icon by hashcode
             */
            @Override
            public int compare(final E o1, final E o2) {
                final long s1 = ExtFileSizeColumn.this.getBytes(o1);
                final long s2 = ExtFileSizeColumn.this.getBytes(o2);
                if (s1 == s2) {
                    return 0;
                }
                if (getSortOrderIdentifier() != ExtColumn.SORT_ASC) {
                    return s1 > s2 ? -1 : 1;
                } else {
                    return s1 < s2 ? -1 : 1;
                }
            }

        });

        this.sb = new StringBuffer();

        this.formatter = new DecimalFormat("0.00") {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public StringBuffer format(final double number, final StringBuffer result, final FieldPosition pos) {
                ExtFileSizeColumn.this.sb.setLength(0);
                return super.format(number, ExtFileSizeColumn.this.sb, pos);
            }
        };
    }

    @Override
    public void configureEditorComponent(final E value, final boolean isSelected, final int row, final int column) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configureRendererComponent(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        final long sizeValue = this.getBytes(value);
        if (sizeValue < 0) {
            this.renderer.setText(this.getInvalidValue());
        } else {
            this.renderer.setText(this.getSizeString(sizeValue));
        }

    }

    abstract protected long getBytes(E o2);

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public JComponent getEditorComponent(final E value, final boolean isSelected, final int row, final int column) {
        return null;
    }

    /**
     * @return
     */
    protected String getInvalidValue() {
        return "";
    }

    /**
     * @return
     */
    @Override
    public JComponent getRendererComponent(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        return this.renderer;
    }

    protected String getSizeString(final long fileSize) {
        if (fileSize >= 1024 * 1024 * 1024 * 1024l) {
            return this.formatter.format(fileSize / (1024 * 1024 * 1024 * 1024.0)) + " TiB";
        }
        if (fileSize >= 1024 * 1024 * 1024l) {
            return this.formatter.format(fileSize / (1024 * 1024 * 1024.0)) + " GiB";
        }
        if (fileSize >= 1024 * 1024l) {
            return this.formatter.format(fileSize / (1024 * 1024.0)) + " MiB";
        }
        if (fileSize >= 1024l) {
            return this.formatter.format(fileSize / 1024.0) + " KiB";
        }
        if (fileSize == 0) {
            return "0 B";
        }
        if (fileSize < 0) {
            return zeroString;
        }
        return fileSize + " B";
    }

    @Override
    protected String getTooltipText(final E value) {
        final long sizeValue = this.getBytes(value);
        if (sizeValue < 0) {
            return this.getInvalidValue();
        } else {
            return this.getSizeString(sizeValue);
        }

    }

    @Override
    public boolean isEditable(final E obj) {
        return false;
    }

    @Override
    public boolean isEnabled(final E obj) {
        return true;
    }

    @Override
    public boolean isSortable(final E obj) {
        return true;
    }

    @Override
    public void resetEditor() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetRenderer() {

        this.renderer.setBorder(ExtColumn.DEFAULT_BORDER);

    }

    @Override
    public void setValue(final Object value, final E object) {

    }
}
