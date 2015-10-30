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
package org.appwork.utils.net.usenet;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class YEncInputStream extends InputStream {

    /**
     * http://www.yenc.org/yenc-draft.1.3.txt
     */

    private final InputStream           inputStream;
    private final ByteArrayOutputStream buffer;
    private final long                  size;
    private final String                name;
    private final int                   lineLength;
    private final boolean               isMultiPart;

    private final long                  partBegin;

    private long                        decodedBytes = 0;

    /**
     * returns the starting points, in bytes, of the block in the original file
     *
     * @return
     */
    public long getPartBegin() {
        return partBegin;
    }

    /**
     * returns the ending points, in bytes, of the block in the original file
     *
     * @return
     */
    public long getPartEnd() {
        return partEnd;
    }

    private final long partEnd;

    /**
     * is the original file multi-part yEnc encoded
     *
     * @return
     */
    public boolean isMultiPart() {
        return isMultiPart;
    }

    /**
     * returns the part-index of multi-part yEnc encoded original file
     *
     * @return
     */
    public int getPartIndex() {
        return partIndex;
    }

    private final int          partIndex;
    private final int          partTotal;
    private final SimpleUseNet client;

    /**
     * returns the number of total parts in a multi-part yEnc encoded original file
     *
     * return -1 for unknown total parts (older yEnc versions)
     *
     * @return
     */
    public int getPartTotal() {
        return partTotal;
    }

    protected YEncInputStream(SimpleUseNet client, ByteArrayOutputStream buffer) throws IOException {
        this.client = client;
        this.inputStream = client.getInputStream();
        this.buffer = buffer;
        String line = new String(buffer.toByteArray(), 0, buffer.size(), "ISO-8859-1");
        if (!line.startsWith("=ybegin")) {
            throw new IOException("missing =ybegin");
        }
        final String lineValue = getValue(line, "line");
        this.lineLength = lineValue != null ? Integer.parseInt(lineValue) : -1;
        name = getValue(line, "name");
        final String sizeValue = getValue(line, "size");
        this.size = sizeValue != null ? Long.parseLong(sizeValue) : -1l;
        final String partValue = getValue(line, "part");
        partIndex = partValue != null ? Integer.parseInt(partValue) : -1;
        isMultiPart = partIndex != -1;
        if (isMultiPart) {
            final String totalValue = getValue(line, "total");
            partTotal = totalValue != null ? Integer.parseInt(totalValue) : -1;
        } else {
            partTotal = -1;
        }
        if (isMultiPart) {
            buffer.reset();
            line = client.readLine(buffer);
            if (!line.startsWith("=ypart")) {
                throw new IOException("missing =ypart");
            }
            final String beginValue = getValue(line, "begin");
            partBegin = beginValue != null ? Long.parseLong(beginValue) : -1;
            final String endValue = getValue(line, "end");
            partEnd = endValue != null ? Long.parseLong(endValue) : -1;
        } else {
            partBegin = -1;
            partEnd = -1;
        }
    }

    protected final InputStream getInputStream() {
        return inputStream;
    }

    private boolean eof        = false;
    private String  crc32Value = null;

    public String getFileCRC32() {
        return crc32Value;
    }

    private String pcrc32Value = null;

    public String getPartCRC32() {
        return pcrc32Value;
    }

    /**
     * read() uses the performance optimized read(byte[] b, int off, int len) with a 1 byte buffer
     */
    @Override
    public synchronized int read() throws IOException {
        if (readBufferLength > 0) {
            return readSingleBuffer();
        }
        final byte[] readB = new byte[1];
        while (true) {
            final int read = read(readB, 0, 1);
            if (read == 1) {
                return readB[0] & 0xff;
            } else if (read == -1) {
                return -1;
            }
        }
    }

    private final int readSingleBuffer() throws IOException {
        final int ret = readBuffer[readBufferIndex++] & 0xff;
        if (readBufferIndex == readBufferLength) {
            readBufferLength = 0;
        }
        return ret;
    }

    private final byte[] readBuffer       = new byte[1024];
    private int          readBufferLength = 0;
    private int          readBufferIndex  = 0;

    /**
     * performanced optimized version for decoding yEnc
     */
    @Override
    public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
        if (readBufferLength > 0) {
            final int cpyLen = Math.min(len, readBufferLength - readBufferIndex);
            if (cpyLen == 1) {
                return readSingleBuffer();
            } else {
                System.arraycopy(readBuffer, readBufferIndex, b, off, cpyLen);
                readBufferIndex += cpyLen;
                if (readBufferIndex == readBufferLength) {
                    readBufferLength = 0;
                }
                return cpyLen;
            }
        }
        if (eof) {
            return -1;
        } else {
            if (yEncMarker_Index == 0) {
                return readDirect(b, off, len);
            } else if (len > yEncMarker_Index) {
                return readContinue(b, off, len);
            } else {
                return readBuffer(b, off, len);
            }
        }
    }

    private final int readDirect(final byte[] b, final int off, final int len) throws IOException {
        final int read = getInputStream().read(b, off, len);
        if (read == -1) {
            throw new EOFException("incomplete yenc stream");
        } else {
            return yEncDecoder(b, off, read + yEncMarker_Index);
        }
    }

    private final int readContinue(final byte[] b, final int off, final int len) throws IOException {
        // prefill b array with =yend
        if (yEncMarker_Index == 1) {
            b[off] = (byte) '=';
        } else if (yEncMarker_Index == 2) {
            b[off] = (byte) '=';
            b[off + 1] = (byte) 'y';
        } else if (yEncMarker_Index == 3) {
            b[off] = (byte) '=';
            b[off + 1] = (byte) 'y';
            b[off + 2] = (byte) 'e';
        } else if (yEncMarker_Index == 4) {
            b[off] = (byte) '=';
            b[off + 1] = (byte) 'y';
            b[off + 2] = (byte) 'e';
            b[off + 3] = (byte) 'n';
        }
        final int read = getInputStream().read(b, off + yEncMarker_Index, len - yEncMarker_Index);
        if (read == -1) {
            throw new EOFException("incomplete yenc stream");
        } else {
            return yEncDecoder(b, off, read + yEncMarker_Index);
        }
    }

    private final int readBuffer(final byte[] b, final int off, final int len) throws IOException {
        readBufferIndex = 0;
        readBufferLength = 0;
        // prefill readBuffer array with =yend
        if (yEncMarker_Index == 1) {
            readBuffer[off] = (byte) '=';
        } else if (yEncMarker_Index == 2) {
            readBuffer[off] = (byte) '=';
            readBuffer[off + 1] = (byte) 'y';
        } else if (yEncMarker_Index == 3) {
            readBuffer[off] = (byte) '=';
            readBuffer[off + 1] = (byte) 'y';
            readBuffer[off + 2] = (byte) 'e';
        } else if (yEncMarker_Index == 4) {
            readBuffer[off] = (byte) '=';
            readBuffer[off + 1] = (byte) 'y';
            readBuffer[off + 2] = (byte) 'e';
            readBuffer[off + 3] = (byte) 'n';
        }
        final int read = getInputStream().read(readBuffer, yEncMarker_Index, readBuffer.length - yEncMarker_Index);
        if (read == -1) {
            throw new EOFException("incomplete yenc stream");
        } else {
            final int decoded = yEncDecoder(readBuffer, 0, read + yEncMarker_Index);
            if (decoded > 0) {
                readBufferLength = decoded;
                return read(b, off, len);
            }
            return decoded;
        }
    }

    private int     yEncMarker_Index          = 0;
    private boolean skip_yEncMarker_Detection = false;

    private final int yEncDecoder(final byte[] b, final int off, final int len) throws IOException {
        yEncMarker_Index = 0;
        int written = 0;
        if (len > 0) {
            final int endReadIndex = off + len;
            int readIndex = off;
            int writeIndex = off;
            detectionLineLoop: while (readIndex < endReadIndex) {
                if (skip_yEncMarker_Detection) {
                    while (readIndex < endReadIndex) {
                        final int c = b[readIndex++] & 0xff;
                        if (c == 10 || c == 13) {
                            // newLine
                            yEncMarker_Index = 0;
                            skip_yEncMarker_Detection = false;
                            continue detectionLineLoop;
                        } else {
                            if (yEncMarker_Index == 0) {
                                // marker ''
                                if (c == 61) {
                                    // mark '='
                                    yEncMarker_Index = 1;
                                } else {
                                    // decode c-42
                                    b[writeIndex++] = (byte) (((byte) (c - 42)) & 0xff);
                                }
                            } else {
                                // decode c-106
                                b[writeIndex++] = (byte) (((byte) (c - 106)) & 0xff);
                                // reset marker
                                yEncMarker_Index = 0;
                            }
                        }
                    }
                } else {
                    while (readIndex < endReadIndex) {
                        final int c = b[readIndex++] & 0xff;
                        if (c == 10 || c == 13) {
                            // newLine
                            yEncMarker_Index = 0;
                            skip_yEncMarker_Detection = false;
                            continue detectionLineLoop;
                        } else {
                            if (yEncMarker_Index == 0) {
                                // marker ''
                                if (c == 61) {
                                    // mark '='
                                    yEncMarker_Index = 1;
                                } else {
                                    // decode c-42
                                    b[writeIndex++] = (byte) (((byte) (c - 42)) & 0xff);
                                }
                            } else if (yEncMarker_Index == 1) {
                                // marker '='
                                if (c == 121) {
                                    // mark '=y'
                                    yEncMarker_Index = 2;
                                } else {
                                    // decode c-106
                                    b[writeIndex++] = (byte) (((byte) (c - 106)) & 0xff);
                                    // reset marker
                                    yEncMarker_Index = 0;
                                    skip_yEncMarker_Detection = true;
                                    continue detectionLineLoop;
                                }
                            } else if (yEncMarker_Index == 2) {
                                // marker '=y'
                                if (c == 101) {
                                    // mark '=ye'
                                    yEncMarker_Index = 3;
                                } else {
                                    // decode '=y'
                                    b[writeIndex++] = (byte) (((byte) (121 - 106)) & 0xff);
                                    // reset marker
                                    yEncMarker_Index = 0;
                                    skip_yEncMarker_Detection = true;
                                    // continue with last c
                                    readIndex -= 1;
                                    continue detectionLineLoop;
                                }
                            } else if (yEncMarker_Index == 3) {
                                // marker '=ye'
                                if (c == 110) {
                                    // mark '=yen'
                                    yEncMarker_Index = 4;
                                } else {
                                    // decode '=y'
                                    b[writeIndex++] = (byte) (((byte) (121 - 106)) & 0xff);
                                    // reset marker
                                    yEncMarker_Index = 0;
                                    skip_yEncMarker_Detection = true;
                                    // continue with last cc
                                    readIndex -= 2;
                                    continue detectionLineLoop;
                                }
                            } else if (yEncMarker_Index == 4) {
                                // marker '=yen'
                                if (c == 100) {
                                    // mark '=yend'
                                    eof = true;
                                    written = writeIndex - off;
                                    decodedBytes += written;
                                    final int readLeft = endReadIndex - readIndex;
                                    if (readLeft > 0) {
                                        final PushbackInputStream inputStream = new PushbackInputStream(getInputStream(), readLeft);
                                        inputStream.unread(b, readIndex, readLeft);
                                        parseTrailer(inputStream);
                                    } else {
                                        parseTrailer(getInputStream());
                                    }
                                    if (written == 0) {
                                        return -1;
                                    } else {
                                        return written;
                                    }
                                } else {
                                    // decode '=y'
                                    b[writeIndex++] = (byte) (((byte) (121 - 106)) & 0xff);
                                    // reset marker
                                    yEncMarker_Index = 0;
                                    skip_yEncMarker_Detection = true;
                                    // continue with last ccc
                                    readIndex -= 3;
                                    continue detectionLineLoop;
                                }
                            }
                        }
                    }
                }
            }
            written = writeIndex - off;
        }
        decodedBytes += written;
        return written;
    }

    /**
     * read and parse yEnc trailer
     *
     * @param inputStream
     * @throws IOException
     */
    private void parseTrailer(final InputStream inputStream) throws IOException {
        buffer.reset();
        final int lineSize = client.readLine(inputStream, buffer);
        final byte[] lineBuffer = buffer.toByteArray();
        final String trailer = new String(lineBuffer, 0, lineSize, "ISO-8859-1");
        final String sizeValue = getValue(trailer, "size");
        final long size = sizeValue != null ? Long.parseLong(sizeValue) : -1;
        if (decodedBytes < size) {
            throw new IOException("decoded-size-error");
        }
        if (isMultiPart()) {
            if (size != getPartSize()) {
                throw new IOException("part-size-error");
            }
            final String partValue = getValue(trailer, "part");
            final int partIndex = partValue != null ? Integer.parseInt(partValue) : -1;
            if (partIndex != getPartIndex()) {
                throw new IOException("part-index-error");
            }
            pcrc32Value = getValue(trailer, "pcrc32");
            crc32Value = getValue(trailer, "crc32");
        } else {
            if (size != getSize()) {
                throw new IOException("size-error");
            }
        }
        readBodyEnd(inputStream);
    }

    /**
     * read body end until "."
     *
     * @param is
     * @throws IOException
     */
    private void readBodyEnd(final InputStream is) throws IOException {
        while (true) {
            buffer.reset();
            final int size = client.readLine(is, buffer);
            if (size > 0) {
                final String line = new String(buffer.toByteArray(), 0, size, "ISO-8859-1");
                if (!".".equals(line)) {
                    throw new IOException("missing body termination(end): " + line);
                }
                break;
            } else if (size == -1) {
                throw new EOFException();
            }
        }
    }

    @Override
    public void close() throws IOException {
    }

    /**
     * returns the name of the original file
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * returns the complete filesize of the original file
     *
     * @return
     */
    public long getSize() {
        return size;
    }

    /**
     * returns the size of the current part
     *
     * @return
     */
    public long getPartSize() {
        if (isMultiPart) {
            return getPartEnd() - getPartBegin() + 1;
        }
        return -1;
    }

    /**
     * returns the line length of yEnc encoding
     *
     * @return
     */
    public int getLineLength() {
        return lineLength;
    }

    protected String getValue(final String line, final String key) {
        final String search = key + "=";
        final int start = line.indexOf(search);
        final int end;
        if ("name".equals(key)) {
            /* special handling for name(last key/value to allow spaces) */
            end = line.length();
        } else {
            final int index = line.indexOf(" ", start);
            if (index == -1) {
                end = line.length();
            } else {
                end = index;
            }
        }
        if (start != -1) {
            return line.substring(start + search.length(), end);
        }
        return null;
    }
}
