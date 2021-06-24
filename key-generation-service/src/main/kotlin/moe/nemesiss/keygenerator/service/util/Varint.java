package moe.nemesiss.keygenerator.service.util;

import org.jetbrains.annotations.NotNull;

public class Varint {

    /**
     * encode int value to varint representation.
     *
     * @param value original int value.
     * @return encoded result, should not be longer than 5.
     */
    public static byte[] encodeInt(int value) {
        final int begin = 0, end = getHighestBitPosition(value);
        byte[] raw = new byte[4];
        for (int i = 0; i < 32; i += 8) {
            raw[i / 8] = (byte) ((value >>> i) & 0xFF);
        }
        return convertToVarintByteArray(raw, begin, end);
    }

    /**
     * encode long value to varint representation.
     *
     * @param value original long value.
     * @return encoded result, should not be longer than 10.
     */
    public static byte[] encodeLong(long value) {
        final int begin = 0, end = getHighestBitPosition(value);
        byte[] raw = new byte[8];
        for (int i = 0; i < 64; i += 8) {
            raw[i / 8] = (byte) ((value >>> i) & 0xFF);
        }
        return convertToVarintByteArray(raw, begin, end);
    }

    public static int decodeInt(@NotNull byte[] data) {
        checkDataArgs(data);
        int result = 0;
        int size = Math.min(32, getPayloadLength(data) * 7);
        for (int i = 0; i < size; ++i) {
            int bit = (data[i / 7] >>> (i % 7)) & 1;
            result |= bit << i;
        }
        return result;
    }

    public static long decodeLong(@NotNull byte[] data) {
        checkDataArgs(data);
        long result = 0;
        int size = Math.min(64, getPayloadLength(data) * 7);
        for (int i = 0; i < size; ++i) {
            int bit = (data[i / 7] >>> (i % 7)) & 1;
            result |= ((long) bit) << i;
        }
        return result;
    }

    private static @NotNull
    byte[] convertToVarintByteArray(@NotNull byte[] raw, int begin, int end) {
        int bitRangeLength = end - begin + 1;
        int resultByteLength = (bitRangeLength / 7) + ((bitRangeLength % 7) == 0 ? 0 : 1);
        byte[] result = new byte[resultByteLength];
        for (int i = 0; i <= end; ++i) {
            int bit = (raw[i / 8] >>> (i % 8)) & 1;
            result[i / 7] |= bit << (i % 7);
        }
        for (int i = 0; i < resultByteLength - 1; ++i) {
            result[i] |= 0x80; // flag up msb
        }
        result[resultByteLength - 1] &= 0x7F; // flag down msb.
        return result;
    }

    private static void checkDataArgs(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data should not be null!");
        }
    }

    private static int getPayloadLength(@NotNull byte[] data) {
        int index = 0;
        while (((data[index] >>> 7) & 1) == 1) {
            index++;
        }
        return index + 1;
    }

    private static int getHighestBitPosition(long value) {
        int start = (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) ? 31 : 63;
        for (int i = start; i >= 0; --i) {
            if (((value >>> i) & 1) == 1) {
                return i;
            }
        }
        return 0;
    }
}
