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
package org.appwork.utils.images;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferByte;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.Kernel;
import java.awt.image.RGBImageFilter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.exceptions.WTFException;
import org.appwork.swing.components.IDIcon;
import org.appwork.swing.components.IconIdentifier;
import org.appwork.utils.StringUtils;
import org.appwork.utils.URLStream;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.Base64OutputStream;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

public class IconIO {

    public static class ScaledIcon implements Icon, IDIcon {

        private final Icon source;

        protected Icon getSource() {
            return source;
        }

        private final int           width;
        private final int           height;
        private final Interpolation interpolation;
        private final double        faktor;

        /**
         * @param icon
         * @param w
         * @param h
         * @param hint
         */
        public ScaledIcon(final Icon icon, final int width, final int height, final Interpolation interpolation) {
            this.source = icon;
            this.faktor = 1d / Math.max((double) icon.getIconWidth() / width, (double) icon.getIconHeight() / height);
            this.width = Math.max((int) (icon.getIconWidth() * this.faktor), 1);
            this.height = Math.max((int) (icon.getIconHeight() * this.faktor), 1);
            this.interpolation = interpolation;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.Icon#getIconHeight()
         */
        @Override
        public int getIconHeight() {
            // TODO Auto-generated method stub
            return this.height;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.Icon#getIconWidth()
         */
        @Override
        public int getIconWidth() {
            // TODO Auto-generated method stub
            return this.width;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
         */
        @Override
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, this.interpolation.getHint());
            final AffineTransform old = g2.getTransform();
            g2.translate(x, y);
            g2.scale(this.faktor, this.faktor);
            this.source.paintIcon(c, g, 0, 0);
            g2.setTransform(old);
        }

        /*
         * (non-Javadoc)
         *
         * @see org.appwork.swing.components.IdentifierInterface#toIdentifier()
         */
        @Override
        public IconIdentifier getIdentifier() {
            if (source instanceof IDIcon) {
                return ((IDIcon) source).getIdentifier();
            }
            return new IconIdentifier("unknown", source.toString());
        }
    }

    static {
        ImageIO.setUseCache(false);
    }

    /**
     * @param image
     * @return
     */
    public static BufferedImage blur(final BufferedImage image) {
        final float[] matrix = new float[400];
        for (int i = 0; i < 400; i++) {
            matrix[i] = 1.0f / 400.0f;
        }

        final BufferedImageOp op = new ConvolveOp(new Kernel(20, 20, matrix), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, null);

    }

    public static BufferedImage colorRangeToTransparency(final BufferedImage image, final Color c1, final Color c2) {

        final int r1 = c1.getRed();
        final int g1 = c1.getGreen();
        final int b1 = c1.getBlue();
        final int r2 = c2.getRed();
        final int g2 = c2.getGreen();
        final int b2 = c2.getBlue();
        final ImageFilter filter = new RGBImageFilter() {
            @Override
            public final int filterRGB(final int x, final int y, final int rgb) {

                final int r = (rgb & 0xFF0000) >> 16;
        final int g = (rgb & 0xFF00) >> 8;
        final int b = rgb & 0xFF;
        if (r >= r1 && r <= r2 && g >= g1 && g <= g2 && b >= b1 && b <= b2) {
            // Set fully transparent but keep color
            // calculate a alpha value based on the distance between the
            // range borders and the pixel color
            final int dist = (Math.abs(r - (r1 + r2) / 2) + Math.abs(g - (g1 + g2) / 2) + Math.abs(b - (b1 + b2) / 2)) * 2;

            return new Color(r, g, b, Math.min(255, dist)).getRGB();
        }

        return rgb;
            }
        };

        final ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
        final Image img = Toolkit.getDefaultToolkit().createImage(ip);
        return IconIO.toBufferedImage(img);
    }

    public static BufferedImage convertIconToBufferedImage(final Icon icon) {
        if (icon == null) {
            return null;
        }
        if (icon instanceof ImageIcon) {
            final Image ret = ((ImageIcon) icon).getImage();
            if (ret instanceof BufferedImage) {
                return (BufferedImage) ret;
            }
        }
        final int w = icon.getIconWidth();
        final int h = icon.getIconHeight();
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        final GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);

        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // g.setColor(Color.RED);
        // g.fillRect(0, 0, w, h);
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return image;

    }

    public static BufferedImage createEmptyImage(final int w, final int h) {
        if (org.appwork.utils.Application.isHeadless()) {
            final BufferedImage image = new BufferedImage(w, h, Transparency.BITMASK);
            return image;
        } else {
            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice gd = ge.getDefaultScreenDevice();
            final GraphicsConfiguration gc = gd.getDefaultConfiguration();
            final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);
            return image;
        }
    }

    public static BufferedImage debug(final BufferedImage img) {
        final Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.RED);
        g2.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
        return img;
    }

    /**
     * @param resource
     * @return
     */
    public static BufferedImage getImage(final URL resource) {
        return IconIO.getImage(resource, true);
    }

    public static BufferedImage getImage(final URL resource, final boolean allowDummy) {
        if (resource != null) {
            InputStream is = null;
            /*
             * workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7166379
             */
            /*
             * http://stackoverflow.com/questions/10441276/jdk-1-7-too-many-open- files-due-to-posix-semaphores
             */
            try {
                is = URLStream.openStream(resource);
                final BufferedImage ret = ImageIO.read(is);
                if (ret != null) {
                    // return getCroppedImage(ret);
                    return ret;
                }
            } catch (final IOException e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            } finally {
                try {
                    is.close();
                } catch (final Throwable e) {
                }
            }
        }
        if (allowDummy) {
            return ImageProvider.createIcon("DUMMY", 48, 48);
        }
        return null;
    }

    /**
     * from here http://stackoverflow.com/questions/3224561/crop-image-to-smallest-size-by-removing-transparent-pixels-in-java
     *
     * @param source
     * @return
     */
    public static BufferedImage getCroppedImage(BufferedImage source) {
        if (source != null && source.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            try {
                // Get our top-left pixel color as our "baseline" for cropping
                final byte[] pixels = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
                final int width = source.getWidth();
                final int height = source.getHeight();
                int x0, y0, x1, y1; // the new corners of the trimmed image
                int i, j; // i - horizontal iterator; j - vertical iterator
                leftLoop: for (i = 0; i < width; i++) {
                    for (j = 0; j < height; j++) {
                        if (pixels[(j * width + i) * 4] != 0) { // alpha is the very first byte and then every fourth one
                            break leftLoop;
                        }
                    }
                }
                x0 = i;
                topLoop: for (j = 0; j < height; j++) {
                    for (i = 0; i < width; i++) {
                        if (pixels[(j * width + i) * 4] != 0) {
                            break topLoop;
                        }
                    }
                }
                y0 = j;
                rightLoop: for (i = width - 1; i >= 0; i--) {
                    for (j = 0; j < height; j++) {
                        if (pixels[(j * width + i) * 4] != 0) {
                            break rightLoop;
                        }
                    }
                }
                x1 = i + 1;
                bottomLoop: for (j = height - 1; j >= 0; j--) {
                    for (i = 0; i < width; i++) {
                        if (pixels[(j * width + i) * 4] != 0) {
                            break bottomLoop;
                        }
                    }
                }
                y1 = j + 1;
                return source.getSubimage(x0, y0, x1 - x0, y1 - y0);
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        return source;
    }

    /**
     * @param resource
     * @return
     */
    public static ImageIcon getImageIcon(final URL resource) {
        return new ImageIcon(IconIO.getImage(resource));
    }

    /**
     * @param resource
     * @param i
     * @return
     */
    public static ImageIcon getImageIcon(final URL resource, final int size) {
        if (resource != null && StringUtils.endsWithCaseInsensitive(resource.getPath(), ".svg")) {
            try {
                return new ImageIcon(getImageFromSVG(resource, size, size));
            } catch (IOException e) {
                LoggerFactory.getDefaultLogger().log(e);
                return new ImageIcon(ImageProvider.createIcon("DUMMY", size, size));
            }
        }
        if (size <= 0) {
            return new ImageIcon(IconIO.getImage(resource));
        } else {
            return new ImageIcon(IconIO.getScaledInstance(IconIO.getImage(resource), size, size, Interpolation.BICUBIC, true));
        }
    }

    public static Icon getScaledInstance(final Icon icon, final int width, final int height) {
        return IconIO.getScaledInstance(icon, width, height, Interpolation.BICUBIC);
    }

    /**
     * @param icon
     * @param width
     * @param height
     * @param bicubic
     * @param higherQuality
     * @return
     */
    public static Icon getScaledInstance(final Icon icon, final int width, final int height, final Interpolation bicubic) {
        if (icon instanceof ScaledIcon) {
            return new ScaledIcon(((ScaledIcon) icon).getSource(), width, height, bicubic);
        } else {
            return new ScaledIcon(icon, width, height, bicubic);
        }

    }

    /**
     * @param image
     * @param i
     * @param j
     * @return
     */
    public static BufferedImage getScaledInstance(final Image img, final int width, final int height) {
        return IconIO.getScaledInstance(img, width, height, Interpolation.BICUBIC, true);
    }

    /**
     * Taken from http://today.java.net/pub/a/today/2007/04/03/perils-of-image- getscaledinstance.html License: unknown Convenience method
     * that returns a scaled instance of the provided {@code BufferedImage}.
     *
     * @param img
     *            the original image to be scaled
     * @param targetWidth
     *            the desired width of the scaled instance, in pixels
     * @param targetHeight
     *            the desired height of the scaled instance, in pixels
     * @param hint
     * @param higherQuality
     *            if true, this method will use a multi-step scaling technique that provides higher quality than the usual one-step
     *            technique (only useful in downscaling cases, where {@code targetWidth} or {@code targetHeight} is smaller than the
     *            original dimensions, and generally only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(final Image img, int width, int height, final Interpolation interpolation, final boolean higherQuality) {
        final double faktor = Math.max((double) img.getWidth(null) / width, (double) img.getHeight(null) / height);
        width = Math.max((int) (img.getWidth(null) / faktor), 1);
        height = Math.max((int) (img.getHeight(null) / faktor), 1);
        if (faktor == 1.0 && img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        Image ret = img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = Math.max(width, img.getWidth(null));
            h = Math.max(height, img.getHeight(null));
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = width;
            h = height;
        }
        do {
            if (higherQuality && w > width) {
                w /= 2;
                if (w < width) {
                    w = width;
                }
            }
            if (higherQuality && h > height) {
                h /= 2;
                if (h < height) {
                    h = height;
                }
            }
            // use 6 as default image type. java versions <16 u17 return type 0
            // for loaded pngs
            int type = 6;
            if (ret instanceof BufferedImage) {
                type = ((BufferedImage) ret).getType();
                if (type == 0) {
                    type = 6;
                }
            }
            if (w == 0) {
                final int o = 2;
            }
            final BufferedImage tmp = new BufferedImage(w, h, type);
            final Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation.getHint());
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
        } while (w != width || h != height);
        return (BufferedImage) ret;
    }

    /**
     * @param icon
     * @param f
     * @return
     */
    public static Image getTransparent(final Image src, final float f) {
        final int w = src.getWidth(null);
        final int h = src.getHeight(null);
        final BufferedImage image = new BufferedImage(w, h, Transparency.TRANSLUCENT);
        final Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f));
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return image;
    }

    /**
     * @param image
     * @param f
     * @return
     */
    public static ImageIcon getTransparentIcon(final Image src, final float f) {
        return new ImageIcon(IconIO.getTransparent(src, f));
    }

    /**
     * @param object
     * @param image
     * @param i
     * @param j
     * @return
     * @return
     */
    public static BufferedImage paint(final BufferedImage paintTo, final Image image, final int xoffset, final int yoffset) {
        final Graphics2D g2 = paintTo.createGraphics();
        g2.drawImage(image, xoffset, yoffset, null);
        g2.dispose();
        IconIO.debug(paintTo);
        return paintTo;

    }

    /**
     * This function removes the major color of the image and replaces it with transparency.
     *
     * @param image
     * @return
     */
    public static BufferedImage removeBackground(final BufferedImage image, final double tollerance) {
        final HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        int biggestValue = 0;
        int color = -1;
        for (final int rgb : image.getRGB(0, 0, image.getWidth() - 1, image.getHeight() - 1, null, 0, image.getWidth())) {
            Integer v = map.get(rgb);
            if (v == null) {
                v = 0;
            }
            v++;
            map.put(rgb, v);
            if (v > biggestValue) {
                biggestValue = v;
                color = rgb;
            }
        }
        final Color col = new Color(color);
        final int r = col.getRed();
        final int g = col.getGreen();
        final int b = col.getBlue();
        final int a = col.getAlpha();
        return IconIO.colorRangeToTransparency(image, new Color(Math.max((int) (r * (1d - tollerance)), 0), Math.max((int) (g * (1d - tollerance)), 0), Math.max((int) (b * (1d - tollerance)), 0), a), new Color(Math.min(255, (int) (r * (1d + tollerance))), Math.min(255, (int) (g * (1d + tollerance))), Math.min(255, (int) (b * (1d + tollerance))), a));
    }

    /**
     * @param drop
     * @param i
     * @return
     */
    public static BufferedImage rotate(final BufferedImage src, final int degree) {
        final int w = src.getWidth(null);
        final int h = src.getHeight(null);
        // final Graphics2D g = image.createGraphics();
        // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        // RenderingHints.VALUE_ANTIALIAS_ON);
        final AffineTransform at = new AffineTransform();
        at.rotate(degree * Math.PI / 180.0);
        Point2D p2din, p2dout;
        p2din = new Point2D.Double(0.0, 0.0);
        p2dout = at.transform(p2din, null);
        double ytrans = p2dout.getY();
        double xtrans = p2dout.getX();
        p2din = new Point2D.Double(0, h);
        p2dout = at.transform(p2din, null);
        ytrans = Math.min(ytrans, p2dout.getY());
        xtrans = Math.min(xtrans, p2dout.getX());
        p2din = new Point2D.Double(w, h);
        p2dout = at.transform(p2din, null);
        ytrans = Math.min(ytrans, p2dout.getY());
        xtrans = Math.min(xtrans, p2dout.getX());
        p2din = new Point2D.Double(w, 0);
        p2dout = at.transform(p2din, null);
        ytrans = Math.min(ytrans, p2dout.getY());
        xtrans = Math.min(xtrans, p2dout.getX());
        final AffineTransform tat = new AffineTransform();
        tat.translate(-xtrans, -ytrans);
        at.preConcatenate(tat);
        final AffineTransformOp bio = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        final Rectangle r = bio.getBounds2D(src).getBounds();
        BufferedImage image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
        image = bio.filter(src, image);
        // final Graphics g = image.getGraphics();
        // g.setColor(Color.RED);
        // g.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
        // g.dispose();
        // try {
        // Dialog.getInstance().showConfirmDialog(0, "", "", new
        // ImageIcon(image), null, null);
        // } catch (final DialogClosedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (final DialogCanceledException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        return image;
    }

    /**
     * @param fileIcon
     * @return
     */
    public static BufferedImage toBufferedImage(final Icon icon) {
        if (icon instanceof ImageIcon) {
            final Image img = ((ImageIcon) icon).getImage();
            if (img instanceof BufferedImage) {
                return (BufferedImage) img;
            }
        }
        final int w = icon.getIconWidth();
        final int h = icon.getIconHeight();
        if (org.appwork.utils.Application.isHeadless()) {
            final BufferedImage image = new BufferedImage(w, h, Transparency.TRANSLUCENT);
            final Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // g.setColor(Color.RED);
            // g.fillRect(0, 0, w, h);
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        } else {
            // not sure why we use this here, but this does not work in headless
            // mode.
            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice gd = ge.getDefaultScreenDevice();
            final GraphicsConfiguration gc = gd.getDefaultConfiguration();
            final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
            final Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // g.setColor(Color.RED);
            // g.fillRect(0, 0, w, h);
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }

    /**
     * Converts any image to a BufferedImage
     *
     * @param image
     * @return
     */
    public static BufferedImage toBufferedImage(final Image src) {
        if (src instanceof BufferedImage) {
            return (BufferedImage) src;
        }
        final int w = src.getWidth(null);
        final int h = src.getHeight(null);
        final BufferedImage image = new BufferedImage(w, h, Transparency.TRANSLUCENT);
        final Graphics2D g = image.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return image;
    }

    /**
     * @param ico
     * @return
     */
    public static ImageIcon toImageIcon(final Icon icon) {
        if (icon == null) {
            return null;
        }
        if (icon instanceof ImageIcon) {
            return (ImageIcon) icon;
        } else {
            return new ImageIcon(IconIO.toBufferedImage(icon));
        }
    }

    public static enum DataURLFormat {
        JPG,
        PNG
    }

    public static String toDataUrl(BufferedImage image, DataURLFormat dataURLFormat) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Base64OutputStream b64os = new Base64OutputStream(bos);
        final BufferedImage ouput;
        switch (dataURLFormat) {
        case JPG:
            // removes alpha channel
            ouput = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            break;
        default:
        case PNG:
            // keeps alpha channel
            ouput = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            break;
        }
        final Graphics g = ouput.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        final String ret;
        switch (dataURLFormat) {
        case JPG:
            ImageIO.write(ouput, "jpg", b64os);
            b64os.close();
            ret = "image/jpeg;base64," + bos.toString("UTF-8");
            break;
        default:
        case PNG:
            ImageIO.write(ouput, "png", b64os);
            b64os.close();
            ret = "image/png;base64," + bos.toString("UTF-8");
            break;
        }
        return ret;
    }

    /**
     * @param image
     * @param white
     * @param d
     * @return
     */
    public static BufferedImage colorRangeToTransparency(BufferedImage image, Color col, double tollerance) {
        final int r = col.getRed();
        final int g = col.getGreen();
        final int b = col.getBlue();
        final int a = col.getAlpha();
        return IconIO.colorRangeToTransparency(image, new Color(Math.max((int) (r * (1d - tollerance)), 0), Math.max((int) (g * (1d - tollerance)), 0), Math.max((int) (b * (1d - tollerance)), 0), a), new Color(Math.min(255, (int) (r * (1d + tollerance))), Math.min(255, (int) (g * (1d + tollerance))), Math.min(255, (int) (b * (1d + tollerance))), a));
    }

    /**
     * @param keepBrightness
     *            TODO
     * @param checkBoxImage
     * @param i
     * @return
     */
    public static Icon replaceColor(Icon icon, final Color search, final int tollerance, final Color replace, boolean keepBrightness) {
        return new ImageIcon(replaceColor(toBufferedImage(icon), search, tollerance, replace, keepBrightness));
    }

    /**
     * @param bufferedImage
     * @param color
     * @return
     */
    public static Image replaceColor(BufferedImage image, final Color search, final int tollerance, final Color replace, final boolean keepBrightness) {
        final int a1 = search.getAlpha();
        final int r1 = search.getRed();
        final int g1 = search.getGreen();
        final int b1 = search.getBlue();

        final ImageFilter filter = new RGBImageFilter() {
            @Override
            public final int filterRGB(final int x, final int y, final int rgb) {
                final int a = (rgb >> 24) & 0xff;
                final int r = (rgb >> 16) & 0xff;
                final int g = (rgb >> 8) & 0xff;
                final int b = (rgb >> 0) & 0xff;
                // System.out.println(a);
                if (r == 0 && b == 0 && g == 0) {
                    //
                }
                if (Math.abs(r - r1) <= tollerance && Math.abs(g - g1) <= tollerance && Math.abs(b - b1) <= tollerance && Math.abs(a - a1) <= tollerance) {
                    if (!keepBrightness) {
                        return replace.getRGB();
                    }
                    final double brightness = ((r + g + b) / 3) / (double) 255;

                    Color nc = new Color((int) (replace.getRed() * brightness), (int) (replace.getGreen() * brightness), (int) (replace.getBlue() * brightness), (int) (replace.getAlpha() * brightness));

                    return nc.getRGB();
                }

                return rgb;
            }
        };

        final ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
        final Image img = Toolkit.getDefaultToolkit().createImage(ip);
        return img;
    }

    /**
     * Save image as a compresssed jpeg and returns the bytes
     *
     * @param read
     * @return
     * @throws IOException
     */
    public static byte[] toJpgBytes(Image image) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BufferedImage jpg = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics g = jpg.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            ImageIO.write(jpg, "jpg", bos);
            bos.close();
            return bos.toByteArray();
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    public static Icon getIconFromDataUrl(String dataURL) throws IOException {
        return new ImageIcon(getImageFromDataUrl(dataURL));
    }

    /**
     * @param exampleDataUrl
     * @return
     * @throws IOException
     */
    public static Image getImageFromDataUrl(String dataURL) throws IOException {
        final int base64Index = dataURL.indexOf(";base64,");
        if (base64Index > 0 && base64Index + 8 < dataURL.length()) {
            dataURL = dataURL.substring(base64Index + 8);
        }
        final Base64InputStream is = new Base64InputStream(new ByteArrayInputStream(dataURL.getBytes("UTF-8")));
        return ImageIO.read(is);
    }

    public static Image getImageFromSVG(URL url, int w, int h) throws IOException {
        try {

            SVGUniverse universe = new SVGUniverse();
            // String svg = IO.readURLToString(url);
            InputStream is = null;
            try {
                URI uri = universe.loadSVG(is = url.openStream(), "dummy.svg");

                SVGDiagram diagram = universe.getDiagram(uri);
                // Rectangle dp = diagram.getDeviceViewport();
                // Rectangle2D vr = diagram.getViewRect();
                // Rectangle2D bb = diagram.getRoot().getBoundingBox();

                diagram.updateTime(0d);
                diagram.setIgnoringClipHeuristic(true);
                if (w <= 0) {
                    w = (int) diagram.getWidth();
                }
                if (h <= 0) {
                    h = (int) diagram.getHeight();
                }
                double faktor = 1d / Math.max((double) diagram.getWidth() / w, (double) diagram.getHeight() / h);
                int width = Math.max((int) (diagram.getWidth() * faktor), 1);
                int height = Math.max((int) (diagram.getHeight() * faktor), 1);

                BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                Graphics2D g = bi.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int x = 0;
                    int y = 0;

                    g.translate(x, y);
                    final Rectangle2D.Double rect = new Rectangle2D.Double();
                    diagram.getViewRect(rect);
                    AffineTransform scaleXform = new AffineTransform();
                    scaleXform.setToScale(width / rect.width, height / rect.height);

                    AffineTransform oldXform = g.getTransform();
                    g.transform(scaleXform);

                    diagram.render(g);

                    g.setTransform(oldXform);

                    g.translate(-x, -y);
                } finally {
                    g.dispose();
                }
                return bi;
            } finally {
                is.close();
            }
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

}
