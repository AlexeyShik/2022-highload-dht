package ok.dht.dao.artyomdrozdov;

import java.util.Comparator;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;

public class MemorySegmentComparator implements Comparator<MemorySegment> {

    public static final Comparator<MemorySegment> INSTANCE = new MemorySegmentComparator();

    private MemorySegmentComparator() {
    }

    @Override
    public int compare(MemorySegment m1, MemorySegment m2) {
        long firstMismatch = m1.mismatch(m2);
        if (firstMismatch == -1) {
            return 0;
        }
        if (firstMismatch == m1.byteSize()) {
            return -1;
        }
        if (firstMismatch == m2.byteSize()) {
            return 1;
        }
        return Byte.compareUnsigned(
                MemoryAccess.getByteAtOffset(m1, firstMismatch),
                MemoryAccess.getByteAtOffset(m2, firstMismatch)
        );
    }
}
