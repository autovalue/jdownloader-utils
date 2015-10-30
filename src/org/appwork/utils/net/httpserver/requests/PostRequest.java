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
package org.appwork.utils.net.httpserver.requests;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.net.ChunkedInputStream;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.LimitedInputStream;
import org.appwork.utils.net.httpserver.HttpConnection;

/**
 * @author daniel
 * 
 */
public class PostRequest extends HttpRequest {

    public static enum CONTENT_TYPE {
        X_WWW_FORM_URLENCODED,
        JSON,
        UNKNOWN
    }

    protected InputStream        inputStream         = null;

    protected boolean            postParameterParsed = false;
    protected List<KeyValuePair> postParameters      = null;

    /**
     * @param connection
     */
    public PostRequest(final HttpConnection connection) {
        super(connection);
    }

    /**
     * TODO: modify these to check if we need to wrap the inputstream again
     * 
     * @return
     * @throws IOException
     */
    public synchronized InputStream getInputStream() throws IOException {
        if (this.inputStream == null) {
            final HTTPHeader transferEncoding = this.getRequestHeaders().get(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING);
            if (transferEncoding != null) {
                if ("chunked".equalsIgnoreCase(transferEncoding.getValue())) {
                    this.inputStream = new ChunkedInputStream(this.connection.getInputStream()) {

                        volatile boolean closed = false;

                        @Override
                        public void close() throws IOException {
                            this.closed = true;
                            if (PostRequest.this.connection.closableStreams()) {
                                super.close();
                            }
                        }

                        @Override
                        public int read() throws IOException {
                            if (this.closed) {
                                return -1;
                            }
                            return super.read();
                        }

                        @Override
                        public int read(final byte[] b) throws IOException {
                            if (this.closed) {
                                return -1;
                            }
                            return super.read(b);
                        }

                        @Override
                        public int read(final byte[] b, final int off, final int len) throws IOException {
                            if (this.closed) {
                                return -1;
                            }
                            return super.read(b, off, len);
                        }
                    };
                } else {
                    throw new IOException("Unknown Transfer-Encoding " + transferEncoding.getValue());
                }
            } else {
                final HTTPHeader contentLength = this.getRequestHeaders().get(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH);
                if (contentLength == null) {
                    throw new IOException("No Content-Length given!");
                }
                this.inputStream = new LimitedInputStream(this.connection.getInputStream(), Long.parseLong(contentLength.getValue())) {

                    volatile boolean closed = false;

                    @Override
                    public void close() throws IOException {
                        this.closed = true;
                        if (PostRequest.this.connection.closableStreams()) {
                            super.close();
                        }
                    }

                    @Override
                    public int read() throws IOException {
                        if (this.closed) {
                            return -1;
                        }
                        return super.read();
                    }

                    @Override
                    public int read(final byte[] b) throws IOException {
                        if (this.closed) {
                            return -1;
                        }
                        return super.read(b);
                    }

                    @Override
                    public int read(final byte[] b, final int off, final int len) throws IOException {
                        if (this.closed) {
                            return -1;
                        }
                        return super.read(b, off, len);
                    }

                };
            }
        }
        return this.inputStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.net.httpserver.requests.HttpRequestInterface# getParameterbyKey(java.lang.String)
     */
    @Override
    public String getParameterbyKey(final String key) throws IOException {
        List<KeyValuePair> params = this.getRequestedURLParameters();
        if (params != null) {
            for (final KeyValuePair param : params) {
                if (key.equalsIgnoreCase(param.key)) {
                    return param.value;
                }
            }
        }
        params = this.getPostParameter();
        if (params != null) {
            for (final KeyValuePair param : params) {
                if (key.equalsIgnoreCase(param.key)) {
                    return param.value;
                }
            }
        }

        return null;

    }

    /**
     * parse existing application/x-www-form-urlencoded PostParameters
     * 
     * @return
     * @throws IOException
     */
    public synchronized List<KeyValuePair> getPostParameter() throws IOException {
        if (this.postParameterParsed) {
            return this.postParameters;
        }
        final String type = this.getRequestHeaders().getValue(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE);
        CONTENT_TYPE content_type = null;
        if (new Regex(type, "(application/x-www-form-urlencoded)").matches()) {
            content_type = CONTENT_TYPE.X_WWW_FORM_URLENCODED;
        } else if (new Regex(type, "(application/json)").matches()) {
            content_type = CONTENT_TYPE.JSON;
        }
        JSonRequest jsonRequest = null;
        if (content_type != null) {
            String charSet = new Regex(type, "charset=(.*?)($| )").getMatch(0);
            if (charSet == null) {
                charSet = "UTF-8";
            }
            switch (content_type) {
            case JSON: {
                final byte[] jsonBytes = IO.readStream(-1, this.getInputStream());
                String json = new String(jsonBytes, charSet);
                json = modifyByContentType(content_type, json);
                jsonRequest = JSonStorage.restoreFromString(json, JSonRequest.TYPE_REF);
            }
                break;
            case X_WWW_FORM_URLENCODED: {
                final byte[] jsonBytes = IO.readStream(-1, this.getInputStream());
                String params = new String(jsonBytes, charSet);
                params = modifyByContentType(content_type, params);
                this.postParameters = HttpConnection.parseParameterList(params);
            }
                break;
            }
        }
        if (jsonRequest != null && jsonRequest.getParams() != null) {
            this.postParameters = new LinkedList<KeyValuePair>();
            for (final Object parameter : jsonRequest.getParams()) {
                if (parameter instanceof JSonObject) {
                    /*
                     * JSonObject has customized .toString which converts Map to Json!
                     */
                    this.postParameters.add(new KeyValuePair(null, parameter.toString()));
                } else {
                    final String jsonParameter = JSonStorage.serializeToJson(parameter);
                    this.postParameters.add(new KeyValuePair(null, jsonParameter));
                }
            }
        }
        this.postParameterParsed = true;
        return this.postParameters;
    }

    /**
     * @param content_type
     * @param json
     * @return
     * @throws Exception
     */
    protected String modifyByContentType(CONTENT_TYPE content_type, String json) throws IOException {
        // TODO Auto-generated method stub
        return json;
    }

    /**
     * @param params
     */
    public void setPostParameter(final List<KeyValuePair> params) {
        this.postParameterParsed = true;
        this.postParameters = params;
    }

    @Override
    public String toString() {
        try {
            final StringBuilder sb = new StringBuilder();

            sb.append("\r\n----------------Request-------------------------\r\n");

            sb.append("POST ").append(this.getRequestedURL()).append(" HTTP/1.1\r\n");

            for (final HTTPHeader key : this.getRequestHeaders()) {

                sb.append(key.getKey());
                sb.append(": ");
                sb.append(key.getValue());
                sb.append("\r\n");
            }
            sb.append("\r\n");
            final List<KeyValuePair> postParams = this.getPostParameter();
            if (postParams != null) {
                for (final KeyValuePair s : postParams) {
                    sb.append(s.key);
                    sb.append(": ");
                    sb.append(s.value);
                    sb.append("\r\n");
                }
            }
            return sb.toString();
        } catch (final Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
