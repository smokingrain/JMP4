package org.jcodec.api;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PictureWithMetadata {
    private Picture picture;
    private double timestamp;
    private double duration;
    private double timeScale;
    private DemuxerTrackMeta.Orientation orientation;

    public static PictureWithMetadata createPictureWithMetadata(Picture picture, double timestamp, double duration, double timeScale) {
        return new PictureWithMetadata(picture, timestamp, duration, DemuxerTrackMeta.Orientation.D_0, timeScale);
    }

    public PictureWithMetadata(Picture picture, double timestamp, double duration, DemuxerTrackMeta.Orientation orientation, double timeScale) {
        this.picture = picture;
        this.timestamp = timestamp;
        this.duration = duration;
        this.orientation = orientation;
        this.setTimeScale(timeScale);
    }

    public Picture getPicture() {
        return picture;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public double getDuration() {
        return duration;
    }

    public DemuxerTrackMeta.Orientation getOrientation() {
        return orientation;
    }

	public double getTimeScale() {
		return timeScale;
	}

	public void setTimeScale(double timeScale) {
		this.timeScale = timeScale;
	}
}
