package org.jcodec.player.filters;

import static org.jcodec.common.io.NIOUtils.readableFileChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecPacketSource {

    private MP4Demuxer demuxer;
    private List<Track> tracks;
    private SeekableByteChannel is;

    public JCodecPacketSource(File file) throws IOException {
        is = NIOUtils.readableChannel(file);
        demuxer = MP4Demuxer.createMP4Demuxer(is);

        tracks = new ArrayList<Track>();
        for (AbstractMP4DemuxerTrack demuxerTrack : demuxer.getTracks()) {
            if (demuxerTrack.getBox().isVideo() || demuxerTrack.getBox().isAudio())
                tracks.add(new Track(demuxerTrack));
        }
    }

    public List<? extends PacketSource> getTracks() {
        return tracks;
    }

    public PacketSource getVideo() {
        for (Track track : tracks) {
            if (track.track.getBox().isVideo())
                return track;
        }
        return null;
    }

    public List<PacketSource> getAudio() {
        List<PacketSource> result = new ArrayList<PacketSource>();
        for (Track track : tracks) {
            if (track.track.getBox().isAudio())
                result.add(track);
        }
        return result;
    }

    public class Track implements PacketSource {
        private AbstractMP4DemuxerTrack track;
        private boolean closed;

        public Track(AbstractMP4DemuxerTrack track) {
            this.track = track;
        }

        public ByteBuffer getDecoderDesc() {
        	EsdsBox esds = track.getBox().findEsds();
        	if(null != esds) {
        		return esds.getStreamInfo();
        	}
        	return null;
        }
        
        public Packet getPacket(ByteBuffer buffer) {
            try {
                return track.nextFrame();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public MediaInfo getMediaInfo() {
            RationalLarge duration = track.getDuration();
            if (track.getBox().isVideo()) {
                VideoSampleEntry se = (VideoSampleEntry) track.getSampleEntries()[0];
                return new MediaInfo.VideoInfo(se.getFourcc(), (int) duration.getDen(), duration.getNum(),
                        track.getFrameCount(), track.getName(), null, track.getBox().getPAR(), new Size(
                                (int) se.getWidth(), (int) se.getHeight()));
            } else if (track.getBox().isAudio()) {
                AudioSampleEntry se = (AudioSampleEntry) track.getSampleEntries()[0];
                return new MediaInfo.AudioInfo(se.getFourcc(), (int) duration.getDen(), duration.getNum(),
                        track.getFrameCount(), track.getName(), null, se.getFormat(), se.getLabels());
            }
            throw new RuntimeException("This shouldn't happen");
        }

        public boolean drySeek(RationalLarge second) {
            return track.canSeek(second.multiplyS(track.getTimescale()));
        }

        public void seek(RationalLarge second) {
            track.seek(second.multiplyS(track.getTimescale()));
        }

        public void close() throws IOException {
            this.closed = true;
            checkClose();
        }

        @Override
        public void gotoFrame(int frameNo) {
            track.gotoFrame(frameNo);
        }
    }

    private void checkClose() throws IOException {
        boolean closed = true;
        for (Track track : tracks) {
            closed &= track.closed;
        }
        if (closed)
            is.close();
    }
}