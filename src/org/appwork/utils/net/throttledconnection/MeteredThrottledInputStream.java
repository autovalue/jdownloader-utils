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
package org.appwork.utils.net.throttledconnection;

import java.io.InputStream;

import org.appwork.utils.speedmeter.SpeedMeterInterface;

/**
 * @author daniel
 * 
 */
public class MeteredThrottledInputStream extends ThrottledInputStream implements SpeedMeterInterface {

    private SpeedMeterInterface speedmeter = null;
    private long                time       = 0;
    private long                speed      = 0;
    private long                lastTime;
    private long                lastTrans;
    private long                transferedCounter3;

    /**
     * @param in
     */
    public MeteredThrottledInputStream(final InputStream in) {
        super(in);
    }

    public MeteredThrottledInputStream(final InputStream in, final SpeedMeterInterface speedmeter) {
        super(in);
        this.speedmeter = speedmeter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.SpeedMeterInterface#getSpeedMeter()
     */
    public synchronized long getSpeedMeter() {
        if (this.time == 0) {
            this.transferedCounter3 = this.transferedCounter;
            this.time = System.currentTimeMillis();
            return 0;
        }
        if (System.currentTimeMillis() - this.time < 1000) {
            if (this.speedmeter != null) { return this.speedmeter.getSpeedMeter(); }
            return this.speed;
        }
        final long tmp2 = this.transferedCounter;
        this.lastTrans = tmp2 - this.transferedCounter3;
        final long tmp = System.currentTimeMillis();
        this.lastTime = tmp - this.time;
        this.transferedCounter3 = tmp2;
        this.time = tmp;
        if (this.speedmeter != null) {
            this.speedmeter.putSpeedMeter(this.lastTrans, this.lastTime);
            this.speed = this.speedmeter.getSpeedMeter();
            return this.speed;
        } else {
            this.speed = this.lastTrans / this.lastTime * 1000;
            return this.speed;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.SpeedMeterInterface#putSpeedMeter(long, long)
     */

    public void putSpeedMeter(final long bytes, final long time) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.SpeedMeterInterface#resetSpeedMeter()
     */

    public synchronized void resetSpeedMeter() {
        if (this.speedmeter != null) {
            this.speedmeter.resetSpeedMeter();
        }
        this.time = System.currentTimeMillis();
        this.speed = 0;
        this.transferedCounter3 = this.transferedCounter;
    }
}