package org.jcodec.codecs.mpeg4.es;

import java.nio.ByteBuffer;

import static org.jcodec.common.Preconditions.checkState;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SL extends Descriptor {
    
    public SL() {
        super(tag(), 0);
    }

    protected void doWrite(ByteBuffer out) {
        out.put((byte)0x2);
    }

    public static int tag() {
        return 0x06;
    }
}
