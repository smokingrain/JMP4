package org.jcodec.player.filters;

import java.util.List;


public interface IMedia {

	
	public PacketSource getVideoTrack();
	
	public List<PacketSource> getAudioTracks();
}
