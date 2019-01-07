package com.dgusev.hlcup2018.accountsapp.netty;

import com.dgusev.hlcup2018.accountsapp.model.BadRequest;
import com.dgusev.hlcup2018.accountsapp.model.NotFoundRequest;
import com.dgusev.hlcup2018.accountsapp.parse.QueryParser;
import com.dgusev.hlcup2018.accountsapp.pool.ObjectPool;
import com.dgusev.hlcup2018.accountsapp.rest.AccountsController;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;

@Component
public class RequestHandler {

    private static final byte[] RESPONSE_201 = "HTTP/1.0 201 Created\r\nConnection: keep-alive\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}".getBytes();
    private static final byte[] RESPONSE_202 = "HTTP/1.0 202 Accepted\r\nConnection: keep-alive\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}".getBytes();
    private static final byte[] BAD_REQUEST = "HTTP/1.0 400 Bad Request\r\nConnection: keep-alive\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}".getBytes();
    private static final byte[] NOT_FOUND = "HTTP/1.0 404 Not Found\r\nConnection: keep-alive\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}".getBytes();
    private static final byte[] OK_START = "HTTP/1.0 200 OK\r\nConnection: keep-alive\r\nContent-Type: application/json\r\nContent-Length: ".getBytes();
    private static final byte[] HEADERS_TERMINATOR = "\r\n\r\n".getBytes();

    public static final TIntList INT_LIST = new TIntArrayList(23000);


    private static final PooledByteBufAllocator POOLED_BYTE_BUF_ALLOCATOR = new PooledByteBufAllocator();

    @Autowired
    private AccountsController accountsController;

    public void handleRead(SelectionKey selectionKey, byte[] buf, int length, ByteBuffer byteBuffer) throws IOException {
        if (length ==0) {
            System.out.println("1Length = 0");
        }
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuf tmpByteBuffer = null;
        try {
            /*ByteBuf fragmentByteBuf = (ByteBuf) selectionKey.attachment();
            if (fragmentByteBuf != null) {
                fragmentByteBuf.writeBytes(buf, 0, length);
                int newLength = fragmentByteBuf.writerIndex();
                fragmentByteBuf.readerIndex(0);
                fragmentByteBuf.readBytes(buf, 0, newLength);
                length = newLength;
                if (length ==0) {
                    System.out.println("2Length = 0");
                }
                ReferenceCountUtil.release(fragmentByteBuf);
                selectionKey.attach(null);
            }
            if ((buf[0] == 'G' && (buf[length - 1] != '\n' || buf[length - 2] != '\r' || buf[length - 3] != '\n' || buf[length - 4] != '\r')) || (buf[0] == 'P' && isFragmentedPost(buf, length))) {
                ByteBuf fragment = POOLED_BYTE_BUF_ALLOCATOR.directBuffer(5000);
                fragment.clear();
                fragment.writeBytes(buf, 0, length);
                selectionKey.attach(fragment);
                return;
            }*/
            ByteBuffer fragmentByteBuf = (ByteBuffer) selectionKey.attachment();
            if (fragmentByteBuf != null) {
                fragmentByteBuf.put(buf, 0, length);
                int newLength = fragmentByteBuf.position();
                fragmentByteBuf.flip();
                fragmentByteBuf.get(buf, 0, newLength);
                length = newLength;
                if (length ==0) {
                    System.out.println("2Length = 0");
                }
                ObjectPool.releaseBuffer(fragmentByteBuf);
                selectionKey.attach(null);
            }
            if ((buf[0] == 'G' && (buf[length - 1] != '\n' || buf[length - 2] != '\r' || buf[length - 3] != '\n' || buf[length - 4] != '\r')) || (buf[0] == 'P' && isFragmentedPost(buf, length))) {
                ByteBuffer fragment = ObjectPool.acquireBuffer();
                fragment.put(buf, 0, length);
                selectionKey.attach(fragment);
                return;
            }

            if (buf[0] == 'G') {
                int queryStart = indexOf(buf, 0, length, ' ') + 1;
                int queryFinish = indexOf(buf, queryStart, length, ' ');
                int endPathIndex = findPathEndIndex(buf, queryStart, queryFinish);
                int startParameters = endPathIndex + 1;
                if (equals(buf, queryStart, endPathIndex, "/accounts/filter/")) {
                    Map<String, String> params = QueryParser.parse(buf, startParameters, queryFinish);
                    tmpByteBuffer = POOLED_BYTE_BUF_ALLOCATOR.directBuffer(10000);
                    accountsController.accountsFilter(params, tmpByteBuffer);
                    int bodyLength = tmpByteBuffer.writerIndex();
                    byteBuffer.put(OK_START);
                    encodeInt(bodyLength, byteBuffer);
                    byteBuffer.put(HEADERS_TERMINATOR);
                    byteBuffer.limit(byteBuffer.position() + bodyLength);
                    tmpByteBuffer.readBytes(byteBuffer);
                    writeResponse(socketChannel, byteBuffer);
                } else if (equals(buf, queryStart, endPathIndex, "/accounts/group/")) {
                    Map<String, String> params = QueryParser.parse(buf, startParameters, queryFinish);
                    tmpByteBuffer = POOLED_BYTE_BUF_ALLOCATOR.directBuffer(10000);
                    accountsController.group(params, tmpByteBuffer);
                    int bodyLength = tmpByteBuffer.writerIndex();
                    byteBuffer.put(OK_START);
                    encodeInt(bodyLength, byteBuffer);
                    byteBuffer.put(HEADERS_TERMINATOR);
                    byteBuffer.limit(byteBuffer.position() + bodyLength);
                    tmpByteBuffer.readBytes(byteBuffer);
                    writeResponse(socketChannel, byteBuffer);
                } else if (contains(buf, queryStart, endPathIndex, "recommend")) {
                    int fin = indexOf(buf, queryStart + 10, queryFinish, '/');
                    int id = decodeInt(buf, queryStart + 10, fin - queryStart - 10);
                    tmpByteBuffer = POOLED_BYTE_BUF_ALLOCATOR.directBuffer(10000);
                    accountsController.recommend(QueryParser.parse(buf, startParameters, queryFinish), id, tmpByteBuffer);
                    int bodyLength = tmpByteBuffer.writerIndex();
                    byteBuffer.put(OK_START);
                    encodeInt(bodyLength, byteBuffer);
                    byteBuffer.put(HEADERS_TERMINATOR);
                    byteBuffer.limit(byteBuffer.position() + bodyLength);
                    tmpByteBuffer.readBytes(byteBuffer);
                    writeResponse(socketChannel, byteBuffer);
                } else if (contains(buf, queryStart, endPathIndex, "suggest")) {
                    int fin = indexOf(buf, queryStart + 10, queryFinish, '/');
                    int id = decodeInt(buf, queryStart + 10, fin - queryStart - 10);
                    tmpByteBuffer = POOLED_BYTE_BUF_ALLOCATOR.directBuffer(10000);
                    accountsController.suggest(QueryParser.parse(buf, startParameters, queryFinish), id, tmpByteBuffer);
                    int bodyLength = tmpByteBuffer.writerIndex();
                    byteBuffer.put(OK_START);
                    encodeInt(bodyLength, byteBuffer);
                    byteBuffer.put(HEADERS_TERMINATOR);
                    byteBuffer.limit(byteBuffer.position() + bodyLength);
                    tmpByteBuffer.readBytes(byteBuffer);
                    writeResponse(socketChannel, byteBuffer);
                } else  {
                    throw NotFoundRequest.INSTANCE;
                }

            } else if (buf[0] == 'P') {
                int queryStart = indexOf(buf, 0, length, ' ') + 1;
                int queryFinish = indexOf(buf, queryStart, length, ' ');
                int endPathIndex = findPathEndIndex(buf, queryStart, queryFinish);
                int pointer = endPathIndex;
                while (!(buf[pointer] == '\n' && buf[pointer - 1] == '\r' && buf[pointer - 2] == '\n' && buf[pointer - 3] == '\r')) {
                    pointer++;
                }
                pointer++;
                int endIndex = length - 1;
                while (buf[endIndex] == '\r' || buf[endIndex] == '\n') {
                    endIndex--;
                }
                endIndex++;
                if (equals(buf, queryStart, endPathIndex, "/accounts/new/")) {
                    accountsController.create(buf, pointer, endIndex);
                    write201Response(socketChannel, byteBuffer, buf, length);
                } else if (equals(buf, queryStart, endPathIndex, "/accounts/likes/")) {
                    accountsController.like(buf, pointer, endIndex);
                    writeResponse(socketChannel, byteBuffer, RESPONSE_202);
                } else {
                    int fin = indexOf(buf, queryStart + 10, queryFinish, '/');
                    int id = 0;
                    try {
                         id = decodeInt(buf, queryStart + 10, fin - queryStart - 10);
                    } catch (BadRequest ex) {
                        throw NotFoundRequest.INSTANCE;
                    }
                    accountsController.update(buf, pointer, endIndex, id);
                    writeResponse(socketChannel, byteBuffer, RESPONSE_202);
                }
            } else {
                System.out.println("Bad first byte " + (byte)buf[0] + " with length=" + length + " |" + new String(buf, 0, length));
                byteBuffer.put(BAD_REQUEST);
                byteBuffer.flip();
                socketChannel.write(byteBuffer);
            }
        } catch (BadRequest badRequest) {
            writeResponse(socketChannel, byteBuffer, BAD_REQUEST);
        } catch (NumberFormatException nfex) {
            if (buf[0] == 'P') {
                System.out.println("1" + new String(buf, 0, length));
            }
            writeResponse(socketChannel, byteBuffer, BAD_REQUEST);
        } catch (NotFoundRequest notFoundRequest) {
            writeResponse(socketChannel, byteBuffer, NOT_FOUND);
        } catch (Exception ex) {
            //System.out.println(request.content().toString(StandardCharsets.UTF_8));
            //ex.printStackTrace();
            if (buf[0] == 'P') {
                ex.printStackTrace();
                System.out.println("2" + new String(buf, 0, length));

            }
            writeResponse(socketChannel, byteBuffer, BAD_REQUEST);
        } finally {
            if (tmpByteBuffer != null) {
                ReferenceCountUtil.release(tmpByteBuffer);
            }
        }

    }


    private void writeResponse(SocketChannel socketChannel, ByteBuffer byteBuffer, byte[] response) throws IOException {
        byteBuffer.clear();
        byteBuffer.put(response);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
    }

    private void write201Response(SocketChannel socketChannel, ByteBuffer byteBuffer, byte[] buf, int length) throws IOException {
        String contentLengthHeader = "query_id=";
        int position = 0;
        char first = contentLengthHeader.charAt(0);
        int partSize = contentLengthHeader.length();
        int q = -1;
        while (position < length) {
            if (buf[position] == first) {
                if (position + partSize > length) {
                    break;
                } else {
                    int partOffset = 0;
                    while ((partOffset < partSize) && buf[position] == contentLengthHeader.charAt(partOffset)) {
                        position++;
                        partOffset++;
                    }
                    if (partOffset == partSize) {
                        if (q != -1) {
                            System.out.println("Two query_id!");
                        }
                        try {
                            q = decodeInt(buf, position, indexOf(buf, position, length, ' ') - position);
                        } catch (Exception ex) {
                            System.out.println("Cannot read query id");
                        }
                    }
                }
            } else {
                position++;
            }
        }
        if (q == -1) {
            System.out.println("There is no query_id");
        } else {
            INT_LIST.add(q);
        }
        byteBuffer.clear();
        byteBuffer.put(RESPONSE_201);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
    }

    private void writeResponse(SocketChannel socketChannel, ByteBuffer byteBuffer) throws IOException {
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
    }

    private int indexOf(byte[] arr, int from, int to, char val) {
        for (int i = from; i < to; i++) {
            if (arr[i] == (byte) val) {
                return i;
            }
        }
        return -1;
    }

    private static int findPathEndIndex(byte[] arr, int from, int to) {
        for (int i = from; i < to; i++) {
            byte c = arr[i];
            if (c == '?' || c == '#') {
                return i;
            }
        }
        return to;
    }

    private boolean equals(byte[] arr, int from, int to, String value) {
        if (to - from != value.length()) {
            return false;
        }
        for (int i = from; i < to; i++) {
            if (arr[i] != value.charAt(i - from)) {
                return false;
            }
        }

        return true;
    }

    private  boolean contains(byte [] buf, int from, int to, String part) {
        int position = from;
        char first = part.charAt(0);
        int partSize = part.length();
        while (position < to) {
            if (buf[position] == first) {
                if (position + partSize > to) {
                    return false;
                } else {
                    int partOffset = 0;
                    while ((partOffset < partSize) && buf[position] == part.charAt(partOffset)) {
                        position++;
                        partOffset++;
                    }
                    if (partOffset == partSize) {
                        return true;
                    }
                }
            } else {
                position++;
            }
        }
        return false;
    }

    private static final int POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};


    public static void encodeInt(int value, ByteBuffer responseBuf) {
        boolean printZero = false;
        for (int i = 9; i>=0; i--) {
            int digit = (int)(value/POW10[i]);
            if (digit == 0 && !printZero) {
                continue;
            }
            responseBuf.put((byte)(48 + digit));
            printZero=true;
            value -= (value/POW10[i]) * POW10[i];
        }
    }

    private int decodeInt(byte[] buf, int from, int length) {
        if (length > 10) {
            throw NotFoundRequest.INSTANCE;
        }
        int result = 0;
        for (int i = from; i < from + length; i++) {
            int value = buf[i] - 48;
            if (value <0 || value > 9) {
                throw NotFoundRequest.INSTANCE;
            }
            result+=POW10[length - (i - from) - 1]* value;
        }
        return result;
    }

    private boolean isFragmentedPost(byte[] buf, int length) {
        int contentLength = 0;
        try {
            contentLength = readContentLength(buf, length);
        } catch (Exception ex) {
            System.out.println("Cannot read content length: " + new String(buf, 0, length));
            if (ex instanceof NotFoundRequest) {
                throw  ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
        if (contentLength == -1) {
            return true;
        }
        if (contentLength == 0) {
            return false;
        }
        int pointer = 0;
        while (!(buf[pointer] == '\n' && buf[pointer - 1] == '\r' && buf[pointer - 2] == '\n' && buf[pointer - 3] == '\r')) {
            pointer++;
        }
        pointer++;
        if (length - pointer - 2 > contentLength ) {
            System.out.println("Error, more data than needed, actual=" + (length - pointer) + " ,header=" + contentLength + ": " + new String(buf, 0, length));
        }
        if (length - pointer - 2  < contentLength ) {
            return true;
        }
        return false;
    }

    private int readContentLength(byte[] buf, int length) {
        String contentLengthHeader = "Content-Length: ";
        int position = 0;
        char first = contentLengthHeader.charAt(0);
        int partSize = contentLengthHeader.length();
        while (position < length) {
            if (buf[position] == first) {
                if (position + partSize > length) {
                    return -1;
                } else {
                    int partOffset = 0;
                    while ((partOffset < partSize) && buf[position] == contentLengthHeader.charAt(partOffset)) {
                        position++;
                        partOffset++;
                    }
                    if (partOffset == partSize) {
                        return decodeInt(buf, position, indexOf(buf, position, length, '\r') - position);
                    }
                }
            } else {
                position++;
            }
        }
        return -1;
    }


}