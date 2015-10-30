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
package org.appwork.utils.speedmeter;

/**
 * @author daniel
 * 
 */
public class AverageSpeedMeter implements SpeedMeterInterface {

    private final long[] bytes;
    private final long[] times;
    private final int    size;
    private int          index;
    private boolean      changed = false;
    private long         speed   = 0;
    private final Object LOCK    = new Object();
    private long         stalled = 0;
    private long         timeout = -1;          /*
                                                  * no timeout for stalled
                                                  * connections
                                                  */

    /**
     * constructor for AverageSpeedMeter with default size 5
     */
    public AverageSpeedMeter() {
        this(5);
    }

    /**
     * constructor for AverageSpeedMeter with custom size
     * 
     * @param size
     */
    public AverageSpeedMeter(final int size) {
        this.size = size;
        this.bytes = new long[this.size];
        this.times = new long[this.size];
        this.index = 0;
        this.resetSpeedMeter();
    }

    public long getSpeedMeter() {
        synchronized (this.LOCK) {
            if (!this.changed) { return this.speed; }
            long totalValue = 0;
            long totalTime = this.stalled;
            for (int i = 0; i < this.size; i++) {
                if (this.bytes[i] < 0) {
                    continue;
                }
                totalValue += this.bytes[i];
                totalTime += this.times[i];
            }
            if (totalTime >= 1000) {
                this.speed = totalValue * 1000 / totalTime;
            }
            this.changed = false;
            return this.speed;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.speedmeter.SpeedMeterInterface#getSpeedMeter()
     */

    public void putSpeedMeter(final long x, final long time) {
        synchronized (this.LOCK) {
            final long put = Math.max(0, x);
            if (put == 0) {
                this.stalled += Math.max(0, time);
                if (this.timeout > 0 && this.stalled > this.timeout) {
                    this.resetSpeedMeter();
                }
            } else {
                this.bytes[this.index] = put;
                this.times[this.index] = Math.max(0, time) + this.stalled;
                this.stalled = 0;
                this.index++;
                if (this.index == this.size) {
                    this.index = 0;
                }
            }
            this.changed = true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.speedmeter.SpeedMeterInterface#putSpeedMeter(long,
     * long)
     */

    public void resetSpeedMeter() {
        synchronized (this.LOCK) {
            for (this.index = 0; this.index < this.size; this.index++) {
                this.bytes[this.index] = -1;
                this.times[this.index] = 0;
            }
            this.index = 0;
            this.speed = 0;
            this.changed = true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.speedmeter.SpeedMeterInterface#resetSpeedMeter()
     */

    public void setStallTimeout(final long timeout) {
        if (timeout <= 0) {
            this.timeout = -1;
        } else {
            this.timeout = timeout;
        }
    }

}
