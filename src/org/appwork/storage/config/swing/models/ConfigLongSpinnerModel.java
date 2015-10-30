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
package org.appwork.storage.config.swing.models;

import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.LongKeyHandler;
import org.appwork.utils.swing.EDTRunner;

public class ConfigLongSpinnerModel extends SpinnerNumberModel implements GenericConfigEventListener<Long> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LongKeyHandler keyHandler;

    public ConfigLongSpinnerModel(LongKeyHandler keyHandler) {
        this.keyHandler = keyHandler;
        keyHandler.getEventSender().addListener(this, true);

        SpinnerValidator spinn = keyHandler.getAnnotation(SpinnerValidator.class);
        if (spinn != null) {
            setMinimum(spinn.min());
            setMaximum(spinn.max());
            setStepSize(spinn.step());
        }
    }

    @Override
    public void setMinimum(Comparable minimum) {
        super.setMinimum(((Number) minimum).longValue());

    }

    @Override
    public void setMaximum(Comparable maximum) {

        super.setMaximum(((Number) maximum).longValue());

    }

    @Override
    public void setStepSize(Number stepSize) {

        super.setStepSize(stepSize.longValue());

    }

    @Override
    public Number getNumber() {

        return (Long) keyHandler.getValue();

    }

    /**
     * Returns the next number in the sequence.
     * 
     * @return <code>value + stepSize</code> or <code>null</code> if the sum
     *         exceeds <code>maximum</code>.
     * 
     * @see SpinnerModel#getNextValue
     * @see #getPreviousValue
     * @see #setStepSize
     */
    public Object getNextValue() {
        return incrValue(+1);
    }

    public Object getPreviousValue() {
        return incrValue(-1);
    }

    protected Number incrValue(int i) {

        return ((Long) getValue()).longValue() + getStepSize().longValue() * i;

    }

    @Override
    public Object getValue() {

        return keyHandler.getValue();
    }

    @Override
    public void setValue(Object value) {
        try{
        keyHandler.setValue(((Number) value).longValue());
        }catch(ValidationException e){           
            java.awt.Toolkit.getDefaultToolkit().beep();          
     
        }
    }

    public void onConfigValidatorError(KeyHandler<Long> keyHandler, Long invalidValue, ValidationException validateException) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                fireStateChanged();
            }
        };
    }

    public void onConfigValueModified(KeyHandler<Long> keyHandler, Long newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                fireStateChanged();
            }
        };
    }

}
