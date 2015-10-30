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
package org.appwork.remoteapi.upload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.LimitedInputStream;
import org.appwork.utils.net.UploadProgress;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.appwork.utils.net.httpconnection.HTTPConnection;

public abstract class Upload {

    protected String     eTag                     = null;
    protected final File file;
    protected long       uploadChunkSize          = -1;
    protected long       remoteSize               = -1;
    protected long       knownErrorFreeRemoteSize = 0;

    public Upload(final File file) {
        this(file, null);
    }

    public Upload(final File file, final String eTag) {
        this.file = file;
        final String ret = new Regex(eTag, "\"(.*?)\"").getMatch(0);
        if (ret == null) {
            this.eTag = eTag;
        } else {
            this.eTag = ret;
        }
    }

    protected void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            try {
                this.getBasicHTTP().getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new InterruptedException();
        }
    }

    public abstract BasicHTTP getBasicHTTP();

    public String getETag() {
        final String ret = new Regex(this.eTag, "\"(.*?)\"").getMatch(0);
        if (ret != null) {
            return ret;
        }
        return this.eTag;
    }

    public File getFile() {
        return this.file;
    }

    public long getKnownErrorFreeRemoteSize() {
        return this.knownErrorFreeRemoteSize;
    }

    public long getLocalSize() {
        return this.getFile().length();
    }

    protected String getQuotedEtag() {
        final String ret = this.getETag();
        if (ret == null) {
            return null;
        }
        return "\"" + ret + "\"";
    }

    public long getRemoteSize(final boolean fetchOnline) throws FileNotFoundException, IOException, InterruptedException {
        if (fetchOnline == false && this.remoteSize > 0) {
            return this.remoteSize;
        }
        final BasicHTTP shttp = this.getBasicHTTP();
        HTTPConnection con = null;
        try {
            final HashMap<String, String> header = new HashMap<String, String>();
            final String eTag = this.getQuotedEtag();
            if (eTag != null) {
                header.put(HTTPConstants.HEADER_REQUEST_IF_MATCH, eTag);
            }
            header.put(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "application/octet-stream");
            header.put(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, "0");
            if (this.file.exists() == false) {
                throw new FileNotFoundException("Local file does not exist: " + this.file);
            }
            header.put(HTTPConstants.HEADER_RESPONSE_CONTENT_RANGE, "bytes */" + this.file.length());
            this.checkInterrupted();
            con = shttp.openPostConnection(this.getUploadURL(), null, new ByteArrayInputStream(new byte[0]), header);
            this.parseResponse(con);
            return this.remoteSize;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * @return the uploadChunkSize
     */
    public long getUploadChunkSize() {
        return this.uploadChunkSize;
    }

    public UploadProgress getUploadProgress() {
        return null;
    }

    protected abstract URL getUploadURL() throws IOException;

    public boolean isUploadComplete() throws FileNotFoundException {
        if (this.remoteSize <= 0) {
            return false;
        }
        if (this.remoteSize > this.file.length()) {
            throw new FileNotFoundException("RemoteSize > LocalSize");
        }
        return this.file.length() == this.remoteSize;
    }

    protected void parseResponse(final HTTPConnection con) throws IOException {
        if (con.getResponseCode() == 404) {
            throw new FileNotFoundException("Remote file does not exist: " + this.eTag);
        }
        if (con.getResponseCode() == 308 || con.getResponseCode() == 200) {
            this.eTag = con.getHeaderField(HTTPConstants.HEADER_ETAG);
            if (StringUtils.isEmpty(this.eTag)) {
                throw new IOException("No ETag!");
            }
            if (con.getResponseCode() == 308) {
                this.remoteSize = 0;
                final String range = con.getHeaderField(HTTPConstants.HEADER_REQUEST_RANGE);
                if (range != null) {
                    final String remoteSize = new Regex(range, "\\d+\\s*?-\\s*?(\\d+)").getMatch(0);
                    this.remoteSize = Long.parseLong(remoteSize) + 1;
                }
            } else {
                this.remoteSize = this.file.length();
            }
            return;
        }
        throw new IOException("Unknown responsecode " + con.getResponseCode());
    }

    public void setETag(final String eTag) {
        if (StringUtils.isEmpty(eTag)) {
            this.eTag = null;
            return;
        }
        final String ret = new Regex(eTag, "\"(.*?)\"").getMatch(0);
        if (ret == null) {
            this.eTag = eTag;
        } else {
            this.eTag = ret;
        }
    }

    public void setKnownErrorFreeRemoteSize(final long knownErrorFreeRemoteSize) {
        this.knownErrorFreeRemoteSize = knownErrorFreeRemoteSize;
    }

    /**
     * @param uploadChunkSize
     *            the uploadChunkSize to set
     */
    public void setUploadChunkSize(final long uploadChunkSize) {
        this.uploadChunkSize = uploadChunkSize;
    }

    public boolean uploadChunk() throws FileNotFoundException, IOException, InterruptedException, NoSuchAlgorithmException {
        final BasicHTTP shttp = this.getBasicHTTP();
        RandomAccessFile raf = null;
        HTTPConnection con = null;
        final UploadProgress uploadProgress = this.getUploadProgress();
        try {
            final HashMap<String, String> header = new HashMap<String, String>();
            raf = new RandomAccessFile(this.file, "r");
            final RandomAccessFile fraf = raf;
            long uploadSize = this.file.length();
            if (uploadProgress != null) {
                uploadProgress.setTotal(uploadSize);
            }
            long existingRemoteSize;
            long remoteSize = Math.min(existingRemoteSize = this.getRemoteSize(true), this.getKnownErrorFreeRemoteSize());
            if (remoteSize < 0) {
                remoteSize = 0;
            }
            if (uploadProgress != null) {
                uploadProgress.setUploaded(remoteSize);
            }
            if (existingRemoteSize > uploadSize || remoteSize > uploadSize) {
                throw new FileNotFoundException("RemoteSize > UploadSize");
            }
            if (remoteSize == uploadSize) {
                /* upload already finished */
                return true;
            }
            if (remoteSize > 0) {
                /* we resume the upload */
                raf.seek(remoteSize);
                uploadSize = this.file.length() - remoteSize;
            }
            final long maxChunkSize = this.getUploadChunkSize();
            if (maxChunkSize > 1024) {
                /* uploadChunkSize is set */
                uploadSize = Math.min(maxChunkSize, uploadSize);
            }
            this.checkInterrupted();
            final long rangeEnd = remoteSize + uploadSize - 1;
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            final InputStream fis = new InputStream() {

                @Override
                public int available() throws IOException {
                    if (fraf.length() - fraf.getFilePointer() >= Integer.MAX_VALUE) {
                        return Integer.MAX_VALUE;
                    } else {
                        return (int) (fraf.length() - fraf.getFilePointer());
                    }
                }

                @Override
                public void close() throws IOException {
                    fraf.close();
                }

                @Override
                public synchronized void mark(final int readlimit) {
                }

                @Override
                public boolean markSupported() {
                    return false;
                }

                @Override
                public int read() throws IOException {
                    return fraf.read();
                }

                @Override
                public int read(final byte[] b) throws IOException {
                    return fraf.read(b);
                }

                @Override
                public int read(final byte[] b, final int off, final int len) throws IOException {
                    return fraf.read(b, off, len);
                }

                @Override
                public synchronized void reset() throws IOException {
                    super.reset();
                }

                @Override
                public long skip(final long n) throws IOException {
                    return 0;
                }

            };
            final DigestInputStream is = new DigestInputStream(new LimitedInputStream(fis, uploadSize), md);
            header.put(HTTPConstants.HEADER_REQUEST_IF_MATCH, this.getQuotedEtag());
            header.put(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "application/octet-stream");
            header.put(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, "" + uploadSize);
            header.put(HTTPConstants.HEADER_RESPONSE_CONTENT_RANGE, "bytes " + remoteSize + "-" + rangeEnd + "/" + this.file.length());
            this.checkInterrupted();
            con = shttp.openPostConnection(this.getUploadURL(), uploadProgress, is, header);
            this.parseResponse(con);
            final String remoteHash = new String(IO.readStream(1024, con.getInputStream()), "UTF-8");
            final String localHash = HexFormatter.byteArrayToHex(is.getMessageDigest().digest());
            if (!localHash.equalsIgnoreCase(remoteHash)) {
                throw new UploadHashException("Upload error: hash missmatch");
            }
            this.setKnownErrorFreeRemoteSize(remoteSize + uploadSize);
            if (uploadProgress != null) {
                uploadProgress.setUploaded(remoteSize + uploadSize);
            }
            return this.isUploadComplete();
        } finally {
            try {
                raf.close();
            } catch (final Throwable e) {
            }
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }
}
