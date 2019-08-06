package org.jcodec.player.filters.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.PacketSource;

public class FilePacketSource implements PacketSource {
	
	private MediaInfo mi;
	
	private FrameGrab fg;
	
	public FilePacketSource(String path, MediaInfo mi) {
		FileChannelWrapper ch = null;
		try {
			ch = NIOUtils.readableChannel(new File(path));
			fg = FrameGrab.createFrameGrab(ch);
		} catch (Exception e) {

		}
	}

	@Override
	public Packet getPacket(ByteBuffer buffer) throws IOException {
//		fg.getVideoTrack().
		return null;
	}

	@Override
	public MediaInfo getMediaInfo() throws IOException {
		// TODO Auto-generated method stub
		return mi;
	}

	@Override
	public void seek(RationalLarge second) throws IOException {
//		fg.getVideoTrack().seek(second);

	}

	@Override
	public void gotoFrame(int frameNo) {

	}

	@Override
	public boolean drySeek(RationalLarge second) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

}
