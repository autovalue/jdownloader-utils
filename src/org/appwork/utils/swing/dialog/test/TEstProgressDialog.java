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
package org.appwork.utils.swing.dialog.test;

import org.appwork.resources.AWUTheme;
import org.appwork.uio.UIOManager;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;

/**
 * @author Thomas
 *
 */
public class TEstProgressDialog {
    public static void main(String[] args) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {

                final ProgressGetter pg = new ProgressGetter() {

                    private long loaded = 0;
                    private long total  = 100;

                    @Override
                    public int getProgress() {

                        if (this.total == 0) {
                            return 0;
                        }
                        return (int) (this.loaded * 100 / this.total);
                    }

                    @Override
                    public String getString() {

                        if (this.total <= 0) {
                            return _AWU.T.connecting();
                        }
                        String ret = _AWU.T.progress(SizeFormatter.formatBytes(this.loaded), SizeFormatter.formatBytes(this.total), (this.loaded * 10000 / this.total) / 100.0);
                        return ret;
                    }

                    @Override
                    public void run() throws Exception {
                        for (int i = 0; i < 100; i++) {
                            Thread.sleep(1000);
                            loaded++;
                        }

                    }

                    @Override
                    public String getLabelString() {
                        return getProgress() + " %";
                    }

                };
                final ProgressDialog dialog = new ProgressDialog(pg, UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.BUTTONS_HIDE_OK, _AWU.T.download_title(), _AWU.T.download_msg(), AWUTheme.getInstance().getIcon("download", 32)) {
                    /**
                     *
                     */
                    protected boolean isLabelEnabled() {
                        // TODO Auto-generated method stub
                        return true;
                    }
                };
                try {
                    Dialog.getInstance().showDialog(dialog);
                } catch (DialogCanceledException ee) {

                } catch (DialogClosedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        };
    }
}
