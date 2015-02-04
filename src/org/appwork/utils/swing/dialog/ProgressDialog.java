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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;

/**
 * @author thomas
 *
 */
public class ProgressDialog extends AbstractDialog<Integer> implements ProgressInterface {
    public interface ProgressGetter {

        public int getProgress();

        public String getString();

        public void run() throws Exception;

        /**
         * @return
         */
        public String getLabelString();
    }

    private boolean              disposed;

    private Thread               executer;
    private final ProgressGetter getter;
    private final String         message;

    private Timer                updater;
    private long                 waitForTermination = 20000;
    protected Throwable          throwable          = null;

    private JLabel               lbl;

    protected JTextPane          textField;

    /**
     * @param progressGetter
     * @param flags
     *            TODO
     * @param icon
     *            TODO
     * @param s
     * @param s2
     */
    public ProgressDialog(final ProgressGetter progressGetter, final int flags, final String title, final String message, final Icon icon) {
        this(progressGetter, flags, title, message, icon, null, null);
    }

    public ProgressDialog(final ProgressGetter progressGetter, final int flags, final String title, final String message, final Icon icon, final String ok, final String cancel) {
        super(flags | UIOManager.BUTTONS_HIDE_OK, title, icon, ok, cancel);
        this.message = message;
        if (progressGetter == null && this instanceof ProgressGetter) {
            this.getter = (ProgressGetter) this;
        } else {
            this.getter = progressGetter;
        }
        this.setReturnmask(true);

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
    public void dispose() {
        if (this.disposed) { return; }
        System.out.println("Dispose Progressdialog");
        this.disposed = true;
        if (this.executer.isAlive()) {
            this.executer.interrupt();
            final long waitFor = this.getWaitForTermination();
            if (waitFor > 0) {
                try {
                    this.executer.join(waitFor);
                } catch (final InterruptedException e) {
                }
            }
        }
        super.dispose();

    }

    private JTextPane getTextfield() {
        final JTextPane textField = new JTextPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {

                return !BinaryLogic.containsAll(ProgressDialog.this.flagMask, Dialog.STYLE_LARGE);
            }
        };
        if (BinaryLogic.containsAll(this.flagMask, Dialog.STYLE_HTML)) {
            textField.setContentType("text/html");
            textField.addHyperlinkListener(new HyperlinkListener() {

                public void hyperlinkUpdate(final HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        CrossSystem.openURL(e.getURL());
                    }
                }

            });
        } else {
            textField.setContentType("text/plain");
            // this.textField.setMaximumSize(new Dimension(450, 600));
        }

        textField.setText(this.message);
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.setFocusable(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textField.setCaretPosition(0);
        return textField;

    }

    /**
     * @return the throwable
     */
    public Throwable getThrowable() {
        return this.throwable;
    }

    public long getWaitForTermination() {
        return this.waitForTermination;
    }

    @Override
    public JComponent layoutDialogContent() {
        this.getDialog().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 2", "[][]", "[][]"));

        this.textField = this.getTextfield();

        this.textField.setText(this.message);
        this.extendLayout(p);
        if (BinaryLogic.containsAll(this.flagMask, Dialog.STYLE_LARGE)) {

            p.add(new JScrollPane(this.textField), "pushx,growx,spanx");

        } else {
            // avoids that the textcomponent's height is calculated too big
            p.add(this.textField, "growx,pushx,spanx,wmin 350");

        }
        this.extendLayout(p);
        final JProgressBar bar;
        p.add(bar = new JProgressBar(0, 100), "growx,pushx" + (this.isLabelEnabled() ? "" : ",spanx"));
        bar.setStringPainted(true);
        if (this.isLabelEnabled()) {
            this.lbl = new JLabel();
            this.lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            p.add(this.lbl, "wmin 30");
        }

        this.updater = new Timer(50, new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (ProgressDialog.this.getter != null) {
                    final int prg = ProgressDialog.this.updateProgress(bar, ProgressDialog.this.getter);
                    ProgressDialog.this.updateText(bar, ProgressDialog.this.getter);
                    ProgressDialog.this.updateLabel();
                    if (prg >= 100) {
                        ProgressDialog.this.updater.stop();
                        ProgressDialog.this.dispose();
                        return;
                    }
                }
            }
        });
        this.updater.setRepeats(true);
        this.updater.setInitialDelay(50);
        this.updater.start();
        this.executer = new Thread("ProgressDialogExecuter") {

            @Override
            public void run() {
                try {
                    ProgressDialog.this.getter.run();
                } catch (final Throwable e) {
                    ProgressDialog.this.throwable = e;
                    e.printStackTrace();
                    ProgressDialog.this.setReturnmask(false);
                } finally {
                    new EDTHelper<Object>() {

                        @Override
                        public Object edtRun() {
                            ProgressDialog.this.dispose();
                            return null;
                        }

                    }.start();
                    ProgressDialog.this.updater.stop();
                }

            }
        };
        this.executer.start();

        return p;
    }

    /**
     * @param p
     */
    protected void extendLayout(JPanel p) {
        // TODO Auto-generated method stub

    }

    /**
     * @return
     */
    protected boolean isLabelEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setWaitForTermination(final long waitForTermination) {
        this.waitForTermination = waitForTermination;
    }

    protected void updateText(final JProgressBar bar, final ProgressGetter getter) {
        final String text = getter.getString();
        if (text == null) {
            bar.setStringPainted(false);
        } else {
            bar.setStringPainted(true);
            bar.setString(text);
        }
    }

    protected int updateProgress(final JProgressBar bar, final ProgressGetter getter) {
        final int prg = getter.getProgress();

        if (prg < 0) {
            bar.setIndeterminate(true);

        } else {
            bar.setIndeterminate(false);
            bar.setValue(prg);

        }
        return prg;
    }

    protected void updateLabel() {
        if (this.lbl != null) {
            this.lbl.setText(this.getter.getLabelString());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.swing.dialog.ProgressInterface#getMessage()
     */
    @Override
    public String getMessage() {
        // TODO Auto-generated method stub
        return this.message;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.swing.dialog.ProgressInterface#getValue()
     */
    @Override
    public int getProgress() {
        // TODO Auto-generated method stub
        return this.getter.getProgress();
    }

}
