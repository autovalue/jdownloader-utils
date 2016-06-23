package org.appwork.swing.trayicon;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;

public class MenuHeader extends MigPanel implements MouseListener {

    private AbstractTray tray;

    public MenuHeader(final AbstractTray tray, Icon ico, String title, Color color) {
        super("ins 1 3 1 1, wrap 3", "[]20[]", "[24!]");
        this.tray = tray;
        JLabel icon;
        this.add(icon = new JLabel(ico));
        JLabel lbl;

        this.add(lbl = SwingUtils.toBold(new JLabel(title)));
        this.setBackground(color);
        this.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, this.getBackground().darker()));
        icon.addMouseListener(this);
        lbl.addMouseListener(this);
        this.addMouseListener(this);
        icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void mouseClicked(final MouseEvent mouseevent) {
        this.tray.showAbout();
    }

    @Override
    public void mousePressed(final MouseEvent mouseevent) {
    }

    @Override
    public void mouseReleased(final MouseEvent mouseevent) {
    }

    @Override
    public void mouseEntered(final MouseEvent mouseevent) {
    }

    @Override
    public void mouseExited(final MouseEvent mouseevent) {
    }

}
