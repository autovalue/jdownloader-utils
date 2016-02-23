package org.appwork.tools.ide.iconsetmaker.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.appwork.resources.AWUTheme;
import org.appwork.swing.action.BasicAction;

public class PseudoCombo<Type> extends JButton {

    protected volatile Type selectedItem = null;

    private Type[]          values;
    private boolean         popDown      = false;

    public PseudoCombo(Type[] values) {
        super();
        this.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onPopup();
            }
        });

        this.setHorizontalAlignment(SwingConstants.LEFT);
        setValues(values);

    }

    @Override
    public Dimension getMinimumSize() {
        return super.getPreferredSize();
    }

    public void setValues(Type[] values) {
        this.values = values;
        Insets m = getMargin();
        if (m == null) {
            m = new Insets(0, 0, 0, 0);
        }
        m.right = getPopIcon(true).getIconWidth() + 5;
        setMargin(m);
        int width = 0;
        for (Type v : values) {
            setText(getLabel(v, true));
            setIcon(getIcon(v, true));
            width = Math.max(width, getPreferredSize().width);
        }
        setPreferredSize(new Dimension(width, 24));
    }

    protected Icon getPopIcon(boolean closed) {
        if (closed) {
            if (isPopDown()) {
                return AWUTheme.I().getIcon("popDownSmall", -1);
            } else {
                return AWUTheme.I().getIcon("popUpSmall", -1);
            }
        } else {
            if (isPopDown()) {
                return AWUTheme.I().getIcon("popUpSmall", -1);
            } else {
                return AWUTheme.I().getIcon("popDownSmall", -1);

            }
        }

    }

    public boolean isPopDown() {
        return popDown;
    }

    public void setPopDown(boolean popDown) {
        this.popDown = popDown;
    }

    protected Icon getIcon(Type v, boolean closed) {
        return null;
    }

    protected String getLabel(Type v, boolean closed) {
        return v + "";
    }

    private long    lastHide = 0;

    private boolean closed   = true;

    protected void onPopup() {
        long timeSinceLastHide = System.currentTimeMillis() - lastHide;
        if (timeSinceLastHide < 250) {
            //
            return;

        }

        JPopupMenu popup = new JPopupMenu() {

            @Override
            public void setVisible(boolean b) {
                if (!b) {
                    lastHide = System.currentTimeMillis();
                }
                super.setVisible(b);
                closed = true;
                PseudoCombo.this.repaint();
            }

        };

        for (final Type sc : values) {
            if (sc == selectedItem && isHideSelf()) {
                continue;
            }
            popup.add(new BasicAction() {
                private Type value;

                {
                    value = sc;
                    setName(getLabel(sc, false));
                    setSmallIcon(PseudoCombo.this.getIcon(sc, false));
                }

                public void actionPerformed(ActionEvent e) {
                    setSelectedItem(value);

                }
            });
        }
        Insets insets = getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();

        popup.setPreferredSize(new Dimension((int) Math.max(getWidth() + insets.left + insets.right, pref.getWidth()), (int) pref.getHeight()));

        if (isPopDown()) {
            popup.show(this, -insets.left, getHeight() + insets.top);

        } else {
            popup.show(this, -insets.left, -popup.getPreferredSize().height + insets.bottom);

        }
        closed = false;
    }

    /**
     * @return
     */
    protected Insets getPopupBorderInsets() {
        return new Insets(0, 2, 4, 2);
    }

    protected boolean isHideSelf() {
        return true;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Icon icon = getPopIcon(closed);
        if (!isEnabled()) {
            icon = AWUTheme.I().getDisabledIcon(icon);
        }
        icon.paintIcon(this, g, getWidth() - icon.getIconWidth() - 5, (getHeight() - icon.getIconHeight()) / 2);

    }

    public void setSelectedItem(Type value) {
        if (selectedItem != value) {
            selectedItem = value;
            onChanged(value);
        }
        setText(getLabel(value, true));
        setIcon(getIcon(value, true));
        setToolTipText(getLabel(value, false));

    }

    public void onChanged(Type newValue) {
    }

    public Type getSelectedItem() {
        return selectedItem;
    }

}
