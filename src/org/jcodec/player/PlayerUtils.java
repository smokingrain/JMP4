package org.jcodec.player;

import java.nio.ByteBuffer;

import net.sourceforge.jaad.aac.AACException;

import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.pcm.PCMDecoder;
import org.jcodec.codecs.s302.S302MDecoder;
import org.jcodec.common.AudioDecoder;
import org.jcodec.player.filters.JCodecPacketSource.Track;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.PacketSource;

public class PlayerUtils {
    public static AudioDecoder getAudioDecoder(String fourcc, MediaInfo.AudioInfo info, PacketSource pkt) {
        if ("sowt".equals(fourcc) || "in24".equals(fourcc) || "twos".equals(fourcc) || "in32".equals(fourcc)) {
        	return new PCMDecoder(info);
        }
            
        else if ("s302".equals(fourcc)) {
        	return new S302MDecoder();
        }
        else if("mp4a".equals(fourcc)) {
        	if(pkt instanceof Track) {
        		Track track = (Track) pkt;
        		ByteBuffer desc = track.getDecoderDesc();
        		if(null != desc) {
        			try {
						return new AACDecoder(desc);
					} catch (AACException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        		
        	}
        	
        }
        return null;
    }
}
