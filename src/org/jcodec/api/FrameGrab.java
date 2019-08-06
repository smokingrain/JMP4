package org.jcodec.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.jcodec.api.specific.AVCMP4Adaptor;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Frame;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.scale.SwingUtil;

import static org.jcodec.common.Codec.H264;
import static org.jcodec.common.Format.MOV;
import static org.jcodec.common.Format.MPEG_PS;
import static org.jcodec.common.Format.MPEG_TS;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts frames from a movie into uncompressed images suitable for
 * processing.
 * 
 * Supports going to random points inside of a movie ( seeking ) by frame number
 * of by second.
 * 
 * NOTE: Supports only AVC ( H.264 ) in MP4 ( ISO BMF, QuickTime ) at this
 * point.
 * 
 * @author The JCodec project
 * 
 */
public class FrameGrab {

    private SeekableDemuxerTrack videoTrack;
    private ContainerAdaptor decoder;
    //ThreadLocal instances are typically private static fields in classes that wish to associate state with a thread
    //FIXME: potential memory leak: non-static ThreadLocal
    private final ThreadLocal<byte[][]> buffers;;
    
    public static void main(String[] args) {
    	try {
    		FileChannelWrapper ch = NIOUtils.readableChannel(new File("F:\\git\\WeChat\\temp\\1564988874286.mp4"));
    		FrameGrab fg = createFrameGrab(ch);
    		int frames = fg.sdt().getMeta().getTotalFrames();
    		long start = System.currentTimeMillis();
    		for(int i = 0; i < frames ; i++) {
//    			fg.seekToFramePrecise(i);
    			Picture pic = fg.getNativeFrame();
    			BufferedImage img = SwingUtil.toBufferedImage(pic);
    			ImageIO.write(img, "jpg", new File("d:/tttttest",i + ".jpg"));
    		}
//    		System.out.println("cost:" + (System.currentTimeMillis() - start));
//    		FrameGrab.getFrameAtSec(new File(""), 12d);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JCodecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
	}

    public static FrameGrab createFrameGrab(SeekableByteChannel _in) throws IOException, JCodecException {
        ByteBuffer header = ByteBuffer.allocate(65536);
        _in.read(header);
        header.flip();
        Format detectFormat = JCodecUtil.detectFormatBuffer(header);
		if (detectFormat == null) {
			throw new UnsupportedFormatException("Could not detect the format of the input video.");
		}
        SeekableDemuxerTrack videoTrack_;

		if (MOV == detectFormat) {
            MP4Demuxer d1 = MP4Demuxer.createMP4Demuxer(_in);
            videoTrack_ = (SeekableDemuxerTrack)d1.getVideoTrack();
        } else if (MPEG_PS == detectFormat) {
            throw new UnsupportedFormatException("MPEG PS is temporarily unsupported.");
        } else if (MPEG_TS == detectFormat) {
            throw new UnsupportedFormatException("MPEG TS is temporarily unsupported.");
        } else {
            throw new UnsupportedFormatException("Container format is not supported by JCodec");
        }
        FrameGrab fg = new FrameGrab(videoTrack_, detectDecoder(videoTrack_));
        fg.decodeLeadingFrames();
        return fg;
    }

    public FrameGrab(SeekableDemuxerTrack videoTrack, ContainerAdaptor decoder) {
        this.videoTrack = videoTrack;
        this.decoder = decoder;
        buffers = new ThreadLocal<byte[][]>();
    }

    private SeekableDemuxerTrack sdt() throws JCodecException {
        if (!(videoTrack instanceof SeekableDemuxerTrack))
            throw new JCodecException("Not a seekable track");

        return (SeekableDemuxerTrack) videoTrack;
    }

    /**
     * Position frame grabber to a specific second in a movie. As a result the
     * next decoded frame will be precisely at the requested second.
     * 
     * WARNING: potentially very slow. Use only when you absolutely need precise
     * seek. Tries to seek to exactly the requested second and for this it might
     * have to decode a sequence of frames from the closes key frame. Depending
     * on GOP structure this may be as many as 500 frames.
     * 
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public FrameGrab seekToSecondPrecise(double second) throws IOException, JCodecException {
        sdt().seek(second);
        decodeLeadingFrames();
        return this;
    }

    /**
     * Position frame grabber to a specific frame in a movie. As a result the
     * next decoded frame will be precisely the requested frame number.
     * 
     * WARNING: potentially very slow. Use only when you absolutely need precise
     * seek. Tries to seek to exactly the requested frame and for this it might
     * have to decode a sequence of frames from the closes key frame. Depending
     * on GOP structure this may be as many as 500 frames.
     * 
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public FrameGrab seekToFramePrecise(int frameNumber) throws IOException, JCodecException {
        sdt().gotoFrame(frameNumber);
        decodeLeadingFrames();
        return this;
    }

    /**
     * Position frame grabber to a specific second in a movie.
     * 
     * Performs a sloppy seek, meaning that it may actually not seek to exact
     * second requested, instead it will seek to the closest key frame
     * 
     * NOTE: fast, as it just seeks to the closest previous key frame and
     * doesn't try to decode frames in the middle
     * 
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public FrameGrab seekToSecondSloppy(double second) throws IOException, JCodecException {
        sdt().seek(second);
        goToPrevKeyframe();
        return this;
    }

    /**
     * Position frame grabber to a specific frame in a movie
     * 
     * Performs a sloppy seek, meaning that it may actually not seek to exact
     * frame requested, instead it will seek to the closest key frame
     * 
     * NOTE: fast, as it just seeks to the closest previous key frame and
     * doesn't try to decode frames in the middle
     * 
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public FrameGrab seekToFrameSloppy(int frameNumber) throws IOException, JCodecException {
        sdt().gotoFrame(frameNumber);
        goToPrevKeyframe();
        return this;
    }

    private void goToPrevKeyframe() throws IOException, JCodecException {
        sdt().gotoFrame(detectKeyFrame((int) sdt().getCurFrame()));
    }

    private void decodeLeadingFrames() throws IOException, JCodecException {
        SeekableDemuxerTrack sdt = sdt();

        int curFrame = (int) sdt.getCurFrame();
        int keyFrame = detectKeyFrame(curFrame);
        sdt.gotoFrame(keyFrame);

        Packet frame = sdt.nextFrame();
        if (decoder == null)
            decoder = detectDecoder(sdt);

        while (frame.getFrameNo() < curFrame) {
            decoder.decodeFrame(frame, getBuffer());
            frame = sdt.nextFrame();
        }
        sdt.gotoFrame(curFrame);
    }

    private byte[][] getBuffer() {
        byte[][] buf = buffers.get();
        if (buf == null) {
            buf = decoder.allocatePicture();
            buffers.set(buf);
        }
        return buf;
    }

    private int detectKeyFrame(int start) throws IOException {
        int[] seekFrames = videoTrack.getMeta().getSeekFrames();
        if (seekFrames == null)
            return start;
        int prev = seekFrames[0];
        for (int i = 1; i < seekFrames.length; i++) {
            if (seekFrames[i] > start)
                break;
            prev = seekFrames[i];
        }
        return prev;
    }

    private static ContainerAdaptor detectDecoder(SeekableDemuxerTrack videoTrack) throws JCodecException {
        DemuxerTrackMeta meta = videoTrack.getMeta();
        if (H264 == meta.getCodec()) {
            return new AVCMP4Adaptor(meta);
        } else {
            throw new UnsupportedFormatException("Codec is not supported");
        }
    }

    /**
     * Get frame at current position in JCodec native image
     * 
     * @return A decoded picture with metadata.
     * @throws IOException
     */
    public PictureWithMetadata getNativeFrameWithMetadata() throws IOException {
        Packet frame = videoTrack.nextFrame();
        if (frame == null)
            return null;

        Picture picture = decoder.decodeFrame(frame, getBuffer());
        return new PictureWithMetadata(picture, frame.getPtsD(), frame.getDurationD(), videoTrack.getMeta().getOrientation(), frame.getTimescale());
    }
    
    public Frame getRealFrame(Rational par, byte[][] buffer) throws IOException {
    	Packet frame = videoTrack.nextFrame();
        if (frame == null)
            return null;

        Picture picture = decoder.decodeFrame(frame, buffer);
        Frame frm = new Frame(picture, new RationalLarge(frame.getPts(), frame.getTimescale()),
                new RationalLarge(frame.getDuration(), frame.getTimescale()), par,
                (int) frame.getFrameNo(), frame.getTapeTimecode(), null);
        return frm;
    }

    /**
     * Get frame at current position in JCodec native image
     * 
     * @return
     * @throws IOException
     */
    public Picture getNativeFrame() throws IOException {
        Packet frame = videoTrack.nextFrame();
        if (frame == null) {
        	return null;
        }

        return decoder.decodeFrame(frame, getBuffer());
    }
    
    /**
     * 鑾峰彇涓�甯у師濮嬫暟鎹�
     * 浣滆�� 锛氳倴閫�
     * 鏃堕棿 锛�2019骞�7鏈�30鏃� 涓嬪崍3:42:21
     * @return
     * @throws IOException
     */
    public Packet getNativePacket() throws IOException {
    	return videoTrack.nextFrame();
    }

    /**
     * Get frame at a specified second as JCodec image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getFrameAtSec(File file, double second) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            return createFrameGrab(ch).seekToSecondPrecise(second).getNativeFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified second as JCodec image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getFrameFromChannelAtSec(SeekableByteChannel file, double second)
            throws JCodecException, IOException {
        return createFrameGrab(file).seekToSecondPrecise(second).getNativeFrame();
    }

    /**
     * Get frame at a specified frame number as JCodec image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getFrameFromFile(File file, int frameNumber) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            return createFrameGrab(ch).seekToFramePrecise(frameNumber).getNativeFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified frame number as JCodec image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getFrameFromChannel(SeekableByteChannel file, int frameNumber)
            throws JCodecException, IOException {
        return createFrameGrab(file).seekToFramePrecise(frameNumber).getNativeFrame();
    }

    /**
     * Get a specified frame by number from an already open demuxer track
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrameAtFrame(SeekableDemuxerTrack vt, ContainerAdaptor decoder, int frameNumber)
            throws IOException, JCodecException {
        return new FrameGrab(vt, decoder).seekToFramePrecise(frameNumber).getNativeFrame();
    }

    /**
     * Get a specified frame by second from an already open demuxer track
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrameAtSec(SeekableDemuxerTrack vt, ContainerAdaptor decoder, double second)
            throws IOException, JCodecException {
        return new FrameGrab(vt, decoder).seekToSecondPrecise(second).getNativeFrame();
    }

    /**
     * Get a specified frame by number from an already open demuxer track (
     * sloppy mode, i.e. nearest keyframe )
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrameSloppy(SeekableDemuxerTrack vt, ContainerAdaptor decoder, int frameNumber)
            throws IOException, JCodecException {
        return new FrameGrab(vt, decoder).seekToFrameSloppy(frameNumber).getNativeFrame();
    }

    /**
     * Get a specified frame by second from an already open demuxer track (
     * sloppy mode, i.e. nearest keyframe )
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrameAtSecSloppy(SeekableDemuxerTrack vt, ContainerAdaptor decoder, double second)
            throws IOException, JCodecException {
        return new FrameGrab(vt, decoder).seekToSecondSloppy(second).getNativeFrame();
    }

    /**
     * Gets info about the media
     * 
     * @return
     */
    public MediaInfo getMediaInfo() {
        return decoder.getMediaInfo();
    }

    /**
     * @return the videoTrack
     */
    public SeekableDemuxerTrack getVideoTrack() {
        return videoTrack;
    }

    /**
     * @return the decoder
     */
    public ContainerAdaptor getDecoder() {
        return decoder;
    }
}