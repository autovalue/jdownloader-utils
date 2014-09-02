/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.resources
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.resources;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.swing.Icon;

import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.appwork.utils.logging.Log;

/**
 * 
 * @author thomas
 * 
 */
public class Theme implements MinTimeWeakReferenceCleanup {
    private String                                              path;

    // private final HashMap<String, MinTimeWeakReference<BufferedImage>>
    // imageCache = new HashMap<String, MinTimeWeakReference<BufferedImage>>();

    protected final HashMap<String, MinTimeWeakReference<Icon>> imageIconCache = new HashMap<String, MinTimeWeakReference<Icon>>();

    private long                                                cacheLifetime  = 20000l;

    private String                                              theme;

    private String                                              nameSpace;

    public Theme(final String namespace) {
        this.setNameSpace(namespace);
        this.setTheme("standard");
    }

    public void cache(final Icon ret, final String key) {
        synchronized (this.imageIconCache) {
            this.imageIconCache.put(key, new MinTimeWeakReference<Icon>(ret, this.getCacheLifetime(), key, this));
        }
    }

    private Theme delegate;

    /**
     * @param i
     */
    public void setDelegate(Theme i) {
        this.delegate = i;

    }

    /**
     * 
     */
    public void clearCache() {
        synchronized (this.imageIconCache) {
            this.imageIconCache.clear();
        }
    }

    public Icon getCached(final String key) {
        synchronized (this.imageIconCache) {
            final MinTimeWeakReference<Icon> cache = this.imageIconCache.get(key);
            if (cache != null) { return cache.get(); }
            return null;
        }
    }

    /**
     * @param relativePath
     * @param size
     * @return
     */
    protected String getCacheKey(final Object... objects) {
        if (objects.length == 1) { return objects[0].toString(); }
        final StringBuilder sb = new StringBuilder();
        for (final Object o : objects) {
            if (sb.length() > 0) {
                sb.append("_");
            }

            sb.append(o.toString());
        }
        return sb.toString();
    }

    public long getCacheLifetime() {
        return this.cacheLifetime;
    }

    private String getDefaultPath(final String pre, final String path, final String ext) {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.path);
        sb.append(pre);
        sb.append(path);
        sb.append(ext);
        return sb.toString();
    }

    public Icon getDisabledIcon(final Icon _getIcon) {
        if (_getIcon != null) {
            if (this.delegate != null) {
                this.delegate.getDisabledIcon(_getIcon);
            }
            final String key = this.getCacheKey(_getIcon, "disabled");
            Icon ret = this.getCached(key);
            if (ret == null) {
                final Icon ico = ImageProvider.getDisabledIcon(_getIcon);
                ret = ico;
                ret = this.modify(ret, key);
                this.cache(ret, key);
            }
            return ret;
        }
        return null;
    }

    public Icon getIcon(final String relativePath, final int size) {
        return this.getIcon(relativePath, size, true);

    }

    /**
     * @param relativePath
     * @param size
     * @param b
     * @return
     */
    public Icon getIcon(final String relativePath, final int size, final boolean useCache) {
        if (this.delegate != null) {
            this.delegate.getIcon(relativePath, size, useCache);
        }
        Icon ret = null;
        String key = null;
        if (useCache) {
            key = this.getCacheKey(relativePath, size);
            ret = this.getCached(key);
        }
        if (ret == null) {
            final URL url = this.getURL("images/", relativePath, ".png");
            ret = IconIO.getImageIcon(url, size);
            ret = this.modify(ret, relativePath);
            if (url == null) {

                Log.exception(new Exception("Icon missing: " + this.getPath("images/", relativePath, ".png")));
                // try {
                // // Dialog.getInstance().showConfirmDialog(0, "Icon Missing",
                // // "Please add the\r\n" + this.getPath("images/",
                // // relativePath, ".png") + " to the classpath", ret, null,
                // // null);
                // } catch (final DialogClosedException e) {
                // // TODO Auto-generated catch block
                // e.printStackTrace();
                // } catch (final DialogCanceledException e) {
                // // TODO Auto-generated catch block
                // e.printStackTrace();
                // }

            }
            if (useCache) {
                this.cache(ret, key);
            }
        }
        return ret;
    }

    //
    // public ImageIcon getIcon(final URL ressourceURL) {
    // final String key = getCacheKey(ressourceURL);
    // ImageIcon ret = getCached(key);
    // if (ret == null) {
    // ret = IconIO.getImageIcon(ressourceURL);
    // cache(ret, key);
    // }
    // return ret;
    // }

    /**
     * @param ret
     * @param relativePath
     */
    protected Icon modify(Icon ret, String relativePath) {
        return ret;

    }

    public Image getImage(final String relativePath, final int size) {

        return this.getImage(relativePath, size, false);
    }

    public Image getImage(final String key, final int size, final boolean useCache) {
        if (this.delegate != null) {
            this.delegate.getImage(key, size, useCache);
        }
        return IconIO.toBufferedImage(this.getIcon(key, size, useCache));
    }

    public URL getImageUrl(final String relativePath) {
        if (this.delegate != null) {
            this.delegate.getImageUrl(relativePath);
        }
        return this.getURL("images/", relativePath, ".png");
    }

    public String getNameSpace() {
        return this.nameSpace;
    }

    /**
     * @return
     */
    public String getPath() {

        return this.path;
    }

    private String getPath(final String pre, final String path, final String ext) {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.path);
        sb.append(pre);
        sb.append(path);
        sb.append(ext);
        return sb.toString();
    }

    public Icon getScaledInstance(final Icon imageIcon, final int size) {
        if (this.delegate != null) {
            this.delegate.getScaledInstance(imageIcon, size);
        }
        final String key = this.getCacheKey(imageIcon, size);
        Icon ret = this.getCached(key);
        if (ret == null) {
            ret = IconIO.getScaledInstance(imageIcon, size, size, Interpolation.BILINEAR);
            this.cache(ret, key);
        }
        return ret;
    }

    public String getText(final String string) {
        if (this.delegate != null) {
            this.delegate.getText(string);
        }
        final URL url = this.getURL("", string, "");
        if (url == null) { return null; }
        try {
            return IO.readURLToString(url);
        } catch (final IOException e) {
            Log.exception(e);
        }
        return null;
    }

    public String getTheme() {
        return this.theme;
    }

    /**
     * returns a valid resourceurl or null if no resource is available.
     * 
     * @param pre
     *            subfolder. for exmaple "images/"
     * @param relativePath
     *            relative resourcepath
     * @param ext
     *            resource extension
     * @return
     */
    public URL getURL(final String pre, final String relativePath, final String ext) {
        if (this.delegate != null) {
            this.delegate.getURL(pre, relativePath, ext);
        }
        final String path = this.getPath(pre, relativePath, ext);
        try {

            // first lookup in home dir. .jd_home or installdirectory
            final File file = Application.getResource(path);

            if (file.exists()) { return file.toURI().toURL(); }
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
        // afterwards, we lookup in classpath. jar or bin folders
        URL url = Theme.class.getResource(path);
        if (url == null) {
            url = Theme.class.getResource(this.getDefaultPath(pre, relativePath, ext));
        }
        return url;
    }

    public boolean hasIcon(final String string) {
        if (this.delegate != null) {
            this.delegate.hasIcon(string);
        }
        return this.getURL("images/", string, ".png") != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.storage.config.MinTimeWeakReferenceCleanup#
     * onMinTimeWeakReferenceCleanup
     * (org.appwork.storage.config.MinTimeWeakReference)
     */
    @Override
    public void onMinTimeWeakReferenceCleanup(final MinTimeWeakReference<?> minTimeWeakReference) {
        synchronized (this.imageIconCache) {
            this.imageIconCache.remove(minTimeWeakReference.getID());
        }
    }

    public void setCacheLifetime(final long cacheLifetime) {
        this.cacheLifetime = cacheLifetime;
    }

    public void setNameSpace(final String nameSpace) {
        this.nameSpace = nameSpace;
        this.path = "/themes/" + this.getTheme() + "/" + this.getNameSpace();
        this.clearCache();
    }

    public void setPath(final String path) {
        this.path = path;
        this.nameSpace = null;
        this.theme = null;
        this.clearCache();
    }

    /**
     * @param theme
     */
    public void setTheme(final String theme) {
        this.theme = theme;
        this.path = "/themes/" + this.getTheme() + "/" + this.getNameSpace();

        this.clearCache();
    }

}
