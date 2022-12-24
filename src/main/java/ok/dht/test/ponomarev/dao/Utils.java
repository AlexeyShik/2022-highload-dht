package ok.dht.test.ponomarev.dao;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;

public final class Utils {
    private Utils() {}
    
    public static final Comparator<MemorySegment> COMPARATOR = (MemorySegment s1, MemorySegment s2) -> {
        final long mismatch = s1.mismatch(s2);
        if (mismatch == -1) {
            return 0;
        }

        if (mismatch == s1.byteSize()) {
            return -1;
        }

        if (mismatch == s2.byteSize()) {
            return 1;
        }

        return Byte.compare(
                MemoryAccess.getByteAtOffset(s1, mismatch),
                MemoryAccess.getByteAtOffset(s2, mismatch)
        );
    };
    
    public static int compare(MemorySegment key1, MemorySegment key2) {
        return COMPARATOR.compare(key1, key2);
    }

    public static String memorySegmentToString(MemorySegment data) {
        if (data == null) {
            return null;
        }

        return StandardCharsets.UTF_8.decode(data.asByteBuffer()).toString();
    }

    public static MemorySegment memorySegmentFromString(String data) {
        if (data == null) {
            return null;
        }

        return MemorySegment.ofArray(data.getBytes(StandardCharsets.UTF_8));
    }
}
