package org.appwork.tools.ide.iconsetmaker.gui;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Icon8Resource {

    private String id;
    private String name;
    private String platform;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getSvg() {
        return svg;
    }

    private String url;
    private String svg;

    public Icon8Resource(String id, String name, String platform, String url, String svg) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.url = url;
        this.svg = svg;
    }

    // public byte[] createPNG(int size, Color color) throws UnsupportedEncodingException, IOException, SVGException {
    //
    // SVGUniverse universe = new SVGUniverse();
    //
    // URI uri = universe.loadSVG(new ByteArrayInputStream(svg.getBytes("ASCII")), name);
    //
    // SVGDiagram diagram = universe.getDiagram(uri);
    // SVGElement root = diagram.getRoot();
    // // set color
    // String hex = "#" + String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    // int alpha = color.getAlpha();
    // root.addAttribute("fill", AnimationElement.AT_CSS, hex);
    // root.addAttribute("fill-opacity", AnimationElement.AT_CSS, hex);
    //
    // diagram.updateTime(0d);
    // diagram.setIgnoringClipHeuristic(true);
    //
    // BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    // Graphics2D g = bi.createGraphics();
    // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // int x = 0;
    // int y = 0;
    //
    // int width = size;
    // int height = size;
    // g.translate(x, y);
    // final Rectangle2D.Double rect = new Rectangle2D.Double();
    // diagram.getViewRect(rect);
    // AffineTransform scaleXform = new AffineTransform();
    // scaleXform.setToScale(width / rect.width, height / rect.height);
    //
    // AffineTransform oldXform = g.getTransform();
    // g.transform(scaleXform);
    //
    // diagram.render(g);
    //
    // g.setTransform(oldXform);
    //
    // g.translate(-x, -y);
    //
    // // diagram.render(g);
    // g.dispose();
    // ByteArrayOutputStream bao;
    // ImageIO.write(bi, "png", bao = new ByteArrayOutputStream());
    // return bao.toByteArray();
    // }

    public byte[] createSVG(Color color) throws UnsupportedEncodingException {
        String hex = "#" + String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setValidating(false);

            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new EntityResolver() {

                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return null;
                }
            });

            docBuilder.setErrorHandler(new ErrorHandler() {

                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    LoggerFactory.getDefaultLogger().log(exception);
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    LoggerFactory.getDefaultLogger().log(exception);
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    LoggerFactory.getDefaultLogger().log(exception);
                }
            });
            Document doc = docBuilder.parse(new ByteArrayInputStream(svg.getBytes("iso-8859-1")));
            float alpha = color.getAlpha() / 255f;
            Node svg = doc.getElementsByTagName("svg").item(0);
            NamedNodeMap attributes = svg.getAttributes();
            Node style = attributes.getNamedItem("style");
            if (style != null) {
                String css = style.getNodeValue();
                if (StringUtils.isNotEmpty(css) && !css.trim().endsWith(";")) {
                    css = css.trim() + ";";
                }
                style.setNodeValue(css + "fill:" + hex + ";fill-opacity:" + alpha);

            } else {
                ((Element) svg).setAttribute("style", "fill:" + hex + ";fill-opacity:" + alpha);
            }

            // root.addAttribute("fill", AnimationElement.AT_CSS, hex);
            // root.addAttribute("fill-opacity", AnimationElement.AT_CSS, hex);

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream bao;
            StreamResult result = new StreamResult(bao = new ByteArrayOutputStream());
            transformer.transform(source, result);
            // String str = new String(bao.toByteArray(), "ASCII");
            return bao.toByteArray();

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (SAXException sae) {
            sae.printStackTrace();
        }

        String svg = this.svg.replaceAll("/svg\" ", "/svg\" style=\"fill:" + hex + "\" ");
        return svg.getBytes("ASCII");

    }

    public String getInfoString() {
        return "id=" + id + "\r\nname=" + name;
    }

    // based on http://rosettacode.org/wiki/Levenshtein_distance
    private static int levenshtein(String strA, String strB) {
        strA = strA.toLowerCase();
        strB = strB.toLowerCase();
        // init grid
        int[] costs = new int[strB.length() + 1];
        for (int i = 0; i < costs.length; i++) {
            costs[i] = i;
        }
        for (int i = 1; i <= strA.length(); i++) {
            int weigther = i - 1;
            costs[0] = i;

            for (int ii = 1; ii <= strB.length(); ii++) {
                int cj = Math.min(1 + Math.min(costs[ii], costs[ii - 1]), strA.charAt(i - 1) == strB.charAt(ii - 1) ? weigther : weigther + 1);
                weigther = costs[ii];
                costs[ii] = cj;
            }
        }
        return costs[strB.length()];
    }

    public Number getRelevance(String lastSearchString) {

        int dist = levenshtein(" " + lastSearchString + " ".toLowerCase(Locale.ENGLISH), " " + name + " ".toLowerCase(Locale.ENGLISH));

        return dist;
    }
}
