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
package org.appwork.shutdown;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.Exceptions;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.LoggerFactory;

public class ShutdownController extends Thread {
    class ShutdownEventWrapper extends ShutdownEvent {

        private final Thread orgThread;

        /**
         * @param value
         */
        public ShutdownEventWrapper(final Thread value) {
            this.orgThread = value;
            // call "Nativ" hooks at the end.
            this.setHookPriority(Integer.MIN_VALUE);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ShutdownEventWrapper) {
                return this.orgThread == ((ShutdownEventWrapper) obj).orgThread;
            }
            return false;
        }

        @Override
        public int hashCode() {

            return this.orgThread.hashCode();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.appwork.shutdown.ShutdownEvent#run()
         */
        @Override
        public void onShutdown(final ShutdownRequest shutdownRequest) {
            this.orgThread.run();
        }

        @Override
        public String toString() {
            return "ShutdownEventWrapper " + this.orgThread + " - " + this.orgThread.getClass().getName() + " Priority: " + this.getHookPriority();
        }

    }

    private static final ShutdownController INSTANCE = new ShutdownController();

    static {
        org.appwork.utils.Application.warnInit();
    }

    /**
     * get the only existing instance of ShutdownController. This is a singleton
     *
     * @return
     */
    public static ShutdownController getInstance() {
        return ShutdownController.INSTANCE;
    }

    private final ArrayList<ShutdownEvent>             hooks;
    private final ArrayList<ShutdownEvent>             originalShutdownHooks;
    private final java.util.List<ShutdownVetoListener> vetoListeners;

    private int                                        exitCode           = 0;
    private final AtomicInteger                        requestedShutDowns = new AtomicInteger(0);

    private volatile Thread                            exitThread         = null;

    private final AtomicBoolean                        shutDown           = new AtomicBoolean(false);
    protected ShutdownRequest                          shutdownRequest;

    /**
     * Create a new instance of ShutdownController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ShutdownController() {
        super(ShutdownController.class.getSimpleName());

        this.hooks = new ArrayList<ShutdownEvent>();
        this.originalShutdownHooks = new ArrayList<ShutdownEvent>();
        this.vetoListeners = new ArrayList<ShutdownVetoListener>();
        try {
            // first try to hook in the original hooks manager
            // to "disable" the original hook manager, we overwrite the actual
            // hook list with our own one, and redirect all registered hooks to
            // ShutdownController.
            // this may fail (reflektion). As a fallback we just add
            // Shutdowncontroller as a normal hook.

            final IdentityHashMap<Thread, Thread> hookDelegater = new IdentityHashMap<Thread, Thread>() {
                /**
                 *
                 */
                private static final long serialVersionUID = 8334628124340671103L;

                {
                    // SHutdowncontroller should be the only hook!!
                    super.put(ShutdownController.this, ShutdownController.this);
                }

                @Override
                public Thread put(final Thread key, final Thread value) {
                    final ShutdownEventWrapper hook = new ShutdownEventWrapper(value);

                    ShutdownController.this.addShutdownEvent(hook);
                    return null;
                }

                @Override
                public Thread remove(final Object key) {
                    ShutdownController.this.removeShutdownEvent(new ShutdownEventWrapper((Thread) key));
                    return (Thread) key;
                }
            };

            final Field field = Class.forName("java.lang.ApplicationShutdownHooks").getDeclaredField("hooks");
            field.setAccessible(true);
            final Map<Thread, Thread> hooks = (Map<Thread, Thread>) field.get(null);
            synchronized (hooks) {

                final Set<Thread> threads = hooks.keySet();

                for (final Thread hook : threads) {
                    this.addShutdownEvent(new ShutdownEventWrapper(hook));

                }
                field.set(null, hookDelegater);
            }

        } catch (final Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
            Runtime.getRuntime().addShutdownHook(this);
        }

    }

    public void addShutdownEvent(final ShutdownEvent event) {
        if (this.isAlive()) {
            LoggerFactory.getDefaultLogger().log(new IllegalStateException("Cannot add hooks during shutdown"));
            return;
        }
        if (event instanceof ShutdownEventWrapper) {
            synchronized (this.originalShutdownHooks) {
                this.originalShutdownHooks.add(event);
            }
        } else {
            synchronized (this.hooks) {
                ShutdownEvent next;
                int i = 0;
                // add event sorted
                for (final Iterator<ShutdownEvent> it = this.hooks.iterator(); it.hasNext();) {
                    next = it.next();
                    if (next.getHookPriority() <= event.getHookPriority()) {
                        this.hooks.add(i, event);
                        return;
                    }
                    i++;
                }
                this.hooks.add(event);
            }
        }

    }

    public void addShutdownVetoListener(final ShutdownVetoListener listener) {
        synchronized (this.vetoListeners) {
            if (this.vetoListeners.contains(listener)) {
                return;
            }
            log("ADD " + listener);
            this.vetoListeners.add(listener);
            try {
                java.util.Collections.sort(this.vetoListeners, new Comparator<ShutdownVetoListener>() {

                    @Override
                    public int compare(final ShutdownVetoListener o1, final ShutdownVetoListener o2) {

                        return new Long(o1.getShutdownVetoPriority()).compareTo(new Long(o2.getShutdownVetoPriority()));
                    }
                });
            } catch (final Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
        }
    }

    /**
     * @return
     * @return
     */
    public ShutdownRequest collectVetos(final ShutdownRequest request) {
        // final java.util.List<ShutdownVetoException> vetos = new
        // ArrayList<ShutdownVetoException>();
        ShutdownVetoListener[] localList = null;
        synchronized (this.vetoListeners) {
            localList = this.vetoListeners.toArray(new ShutdownVetoListener[] {});
        }
        for (final ShutdownVetoListener v : localList) {
            try {
                if (request != null && request.askForVeto(v) == false) {
                    continue;
                }

                v.onShutdownVetoRequest(request);

            } catch (final ShutdownVetoException e) {

                if (request != null) {
                    try {
                        request.addVeto(e);
                    } catch (final Throwable e2) {
                        e2.printStackTrace();
                    }
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        return request;

    }

    public int getExitCode() {
        return this.exitCode;
    }

    public ShutdownRequest getShutdownRequest() {
        return this.shutdownRequest;
    }

    public List<ShutdownVetoListener> getShutdownVetoListeners() {
        synchronized (this.vetoListeners) {
            return new ArrayList<ShutdownVetoListener>(this.vetoListeners);
        }
    }

    /**
     * Same function as org.appwork.utils.Exceptions.getStackTrace(Throwable)<br>
     * <b>DO NOT REPLACE IT EITHER!</b> Exceptions.class my be unloaded. This would cause Initialize Exceptions during shutdown.
     *
     * @param thread
     * @return
     */
    private String getStackTrace(final Thread thread) {
        try {
            final StackTraceElement[] st = thread.getStackTrace();
            final StringBuilder sb = new StringBuilder("");
            for (final StackTraceElement element : st) {
                sb.append(element);
                sb.append("\r\n");
            }
            return sb.toString();
        } catch (final Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param instance2
     * @return
     */
    public boolean hasShutdownEvent(final ShutdownEvent instance2) {
        synchronized (this.hooks) {
            return this.hooks.contains(instance2);
        }
    }

    public boolean isShutDownRequested() {
        return this.requestedShutDowns.get() > 0;
    }

    public void removeShutdownEvent(final ShutdownEvent event) {
        if (this.isAlive()) {
            throw new IllegalStateException("Cannot add hooks during shutdown");
        }
        synchronized (this.hooks) {
            ShutdownEvent next;

            // add event sorted
            for (final Iterator<ShutdownEvent> it = this.hooks.iterator(); it.hasNext();) {
                next = it.next();
                if (next == event) {
                    it.remove();
                }

            }
        }
    }

    public void removeShutdownVetoListener(final ShutdownVetoListener listener) {
        synchronized (this.vetoListeners) {
            log("Remove " + listener);
            this.vetoListeners.remove(listener);
        }
    }

    public boolean requestShutdown() {
        return this.requestShutdown(false);
    }

    // /**
    // *
    // */
    public boolean requestShutdown(final boolean silent) {
        return this.requestShutdown(new BasicShutdownRequest(silent));
    }

    public boolean isShuttingDown() {
        return shutDown.get();
    }

    public boolean requestShutdown(final ShutdownRequest request) {
        if (request == null) {
            throw new NullPointerException();
        }
        this.log("Request Shutdown: " + request);
        this.requestedShutDowns.incrementAndGet();
        try {

            this.collectVetos(request);
            final java.util.List<ShutdownVetoException> vetos = request.getVetos();

            if (vetos.size() == 0) {

                LoggerFactory.getDefaultLogger().info("No Vetos");
                ShutdownVetoListener[] localList = null;
                synchronized (this.vetoListeners) {
                    localList = this.vetoListeners.toArray(new ShutdownVetoListener[] {});
                }

                LoggerFactory.getDefaultLogger().info("Fire onShutDownEvents");
                for (final ShutdownVetoListener v : localList) {
                    try {
                        LoggerFactory.getDefaultLogger().info("Call onShutdown: " + v);
                        v.onShutdown(request);
                    } catch (final Throwable e) {
                        LoggerFactory.getDefaultLogger().log(e);

                    } finally {
                        LoggerFactory.getDefaultLogger().info("Call onShutdown done: " + v);
                    }
                }
                if (this.shutDown.compareAndSet(false, true)) {
                    LoggerFactory.getDefaultLogger().info("Create ExitThread");
                    try {
                        request.onShutdown();
                    } catch (final Throwable e) {
                        LoggerFactory.getDefaultLogger().severe(Exceptions.getStackTrace(e));
                    }
                    this.exitThread = new Thread("ShutdownThread") {

                        @Override
                        public void run() {
                            ShutdownController.this.shutdownRequest = request;

                            LoggerFactory.getDefaultLogger().info("Exit Now: Code: " + ShutdownController.this.getExitCode());
                            System.exit(ShutdownController.this.getExitCode());
                        }
                    };
                    this.exitThread.start();
                }
                LoggerFactory.getDefaultLogger().info("Wait");
                while (this.exitThread.isAlive()) {
                    try {
                        Thread.sleep(500);
                    } catch (final InterruptedException e) {
                        return true;
                    }
                }
                log("DONE");
                return true;
            } else {
                LoggerFactory.getDefaultLogger().info("Vetos found");
                ShutdownVetoListener[] localList = null;
                synchronized (this.vetoListeners) {
                    localList = this.vetoListeners.toArray(new ShutdownVetoListener[] {});
                }
                for (final ShutdownVetoListener v : localList) {
                    try {
                        /* make sure noone changes content of vetos */
                        v.onShutdownVeto(request);
                    } catch (final Throwable e) {
                        LoggerFactory.getDefaultLogger().log(e);
                    }
                }
                request.onShutdownVeto();
                return false;
            }
        } finally {
            this.requestedShutDowns.decrementAndGet();
        }
    }

    private LogInterface logger;

    public LogInterface getLogger() {
        return this.logger;
    }

    public void setLogger(final LogInterface logger) {
        this.logger = logger;
    }

    /**
     * @param string
     */
    private void log(final String string) {
        try {
            // code may run in the shutdown hook. Classloading problems are
            // possible

            if (this.logger != null) {
                this.logger.info(string);
            } else {
                System.out.println(string);
            }
        } catch (final Throwable e) {

        }
    }

    @Override
    public void run() {
        /*
         * Attention. This runs in shutdownhook. make sure, that we do not have to load previous unloaded classes here.
         */
        try {

            java.util.List<ShutdownEvent> list;
            synchronized (this.hooks) {
                list = new ArrayList<ShutdownEvent>(this.hooks);

            }
            synchronized (this.originalShutdownHooks) {
                list.addAll(this.originalShutdownHooks);
            }
            int i = 0;
            for (final ShutdownEvent e : list) {
                try {
                    i++;

                    final long started = System.currentTimeMillis();

                    log("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: start item->" + e);

                    final Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            e.onShutdown(ShutdownController.this.shutdownRequest);
                        }
                    });
                    thread.setName("ShutdownHook [" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]");

                    thread.start();
                    try {
                        e.waitFor();
                        thread.join(Math.max(0, e.getMaxDuration()));
                    } catch (final Throwable e1) {
                        e1.printStackTrace();

                    }
                    if (thread.isAlive()) {
                        log("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: " + e + "->is still running after " + e.getMaxDuration() + " ms");
                        log("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: " + e + "->StackTrace:\r\n" + this.getStackTrace(thread));
                    } else {
                        log("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: item ended after->" + (System.currentTimeMillis() - started));
                    }
                    log("[Done:" + i + "/" + list.size() + "]");
                } catch (final Throwable e1) {
                    e1.printStackTrace();
                }
            }
            log("Shutdown Hooks Finished");

        } catch (final Throwable e1) {
            // do not use Log here. If LoggerFactory.getDefaultLogger().log(e1); throws an
            // exception,
            // we have to catch it here without the risk of another exception.

        }
    }

    /**
     * @param i
     */
    public void setExitCode(final int i) {
        this.exitCode = i;

    }
}
