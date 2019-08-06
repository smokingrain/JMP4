package org.jcodec.player.filters;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Frame;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.player.filters.MediaInfo.VideoInfo;

public class FrameGrabSource implements VideoSource {
	
	private ReentrantLock seekLock = new ReentrantLock();
	private MediaInfo.VideoInfo mi;
	private PacketSource src;
	private FileChannelWrapper ch;
	private FrameGrab fg ;
	
	public FrameGrabSource(File path, PacketSource src) throws IOException {
		this.src = src;
		try {
			ch = NIOUtils.readableChannel(path);
			fg = FrameGrab.createFrameGrab(ch);
		} catch (Exception e) {
			System.out.println("Ω‚Œˆ ß∞‹...");
			System.exit(0);
		}
		mi = (MediaInfo.VideoInfo) src.getMediaInfo();
	}

	@Override
	public Frame decode(byte[][] buffer) throws IOException {
		seekLock.lock();
		Frame frm = fg.getRealFrame(mi.getPAR(), buffer);
        return frm;
	}
	
	public class FutureFrame extends Frame {

        private Future<Picture> job;

        public FutureFrame(Future<Picture> job, RationalLarge pts, RationalLarge duration, Rational pixelAspect,
                int frameNo, TapeTimecode tapeTimecode, List<String> messages) {
            super(null, pts, duration, pixelAspect, frameNo, tapeTimecode, messages);
            this.job = job;
        }

        @Override
        public boolean isAvailable() {
            return job.isDone();
        }

        @Override
        public Picture getPic() {
            try {
                return job.get();
            } catch (Exception e) {e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
	
	class FrameCallable implements Callable<Picture> {
        private Packet pkt;
        private byte[][] out;

        public FrameCallable(Packet pkt, byte[][] out) {
            this.pkt = pkt;
            this.out = out;
        }

        public Picture call() {

            Picture pic = null;
			try {
				pic = fg.getDecoder().decodeFrame(pkt, out);
			} catch (Exception e) {
			}
            
            return pic;
        }
    }

	@Override
	public boolean drySeek(RationalLarge second) throws IOException {
		
		return true;
	}

	@Override
	public void seek(RationalLarge second) throws IOException {
		try {
			fg.seekToSecondPrecise(second.getNum() * 1d / second.getDen());
		} catch (JCodecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}

	}

	@Override
	public void gotoFrame(int frame) {
		try {
			fg.seekToFramePrecise(frame);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JCodecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public VideoInfo getMediaInfo() throws IOException {
		return mi;
	}

	@Override
	public void close() throws IOException {
		if(null != src) {
			src.close();
		}
		if(null != ch) {
			ch.close();
		}
	}

}
