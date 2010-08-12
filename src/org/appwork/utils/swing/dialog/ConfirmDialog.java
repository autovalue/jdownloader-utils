/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.swing.dialog
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.swing.dialog;

import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.appwork.utils.BinaryLogic;
import org.appwork.utils.os.CrossSystem;

public class ConfirmDialog extends AbstractDialog<Integer> {

    private static final long serialVersionUID = -7647771640756844691L;

    private final String message;
    private JTextPane textField;

    public ConfirmDialog(final int flag, final String title, final String message, final ImageIcon icon, final String okOption, final String cancelOption) {
        super(flag, title, icon, okOption, cancelOption);
        this.message = message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.swing.dialog.AbstractDialog#getRetValue()
     */
    @Override
    protected Integer createReturnValue() {
        // TODO Auto-generated method stub
        return this.getReturnmask();
    }

    @Override
    public Dimension getPreferredSize() {
        if (!BinaryLogic.containsAll(this.flagMask, Dialog.STYLE_LARGE)) {
            return super.getPreferredSize();
        } else {
            return new Dimension(600, 450);
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        this.textField = new JTextPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return !BinaryLogic.containsAll(ConfirmDialog.this.flagMask, Dialog.STYLE_LARGE);
            }
        };
        if (BinaryLogic.containsAll(this.flagMask, Dialog.STYLE_HTML)) {
            this.textField.setContentType("text/html");
            this.textField.addHyperlinkListener(new HyperlinkListener() {

                public void hyperlinkUpdate(final HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        CrossSystem.openURL(e.getURL());
                    }
                }

            });
        } else {
            this.textField.setContentType("text");
            this.textField.setMaximumSize(new Dimension(450, 600));
        }

        this.textField.setText(this.message);
        this.textField.setEditable(false);
        this.textField.setBackground(null);
        this.textField.setOpaque(false);
        this.textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);

        if (BinaryLogic.containsAll(this.flagMask, Dialog.STYLE_LARGE)) {
            final JScrollPane sp = new JScrollPane(this.textField);
            return sp;
        } else {
            return this.textField;
        }
    }

    @Override
    public String toString() {
        return ("dialog-" + this.getTitle() + "_" + this.message).replaceAll("\\W", "_");
    }

}
