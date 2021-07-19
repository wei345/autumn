package io.liuwei.autumn.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * 一个基于多个字节数组的输入流。
 */
@SuppressWarnings("WeakerAccess")
public class MultiByteArrayInputStream extends InputStream {
    private final byte[][] arrays;
    private int index = 0;
    private int i = 0;

    public MultiByteArrayInputStream(byte[]... arrays) {
        this.arrays = arrays;
    }

    @Override
    public int read() throws IOException {
        if (index == arrays.length) {
            return -1;
        }
        byte[] array = arrays[index];
        if (i < array.length) {
            return array[i++];
        } else {
            index++;
            i = 0;
            return read();
        }
    }
}