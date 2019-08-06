package org.jcodec.common;

import javax.sound.sampled.AudioFormat.Encoding;

public class SoundUtil {
    public static AudioFormat fromJavaX(javax.sound.sampled.AudioFormat af) {
        return new AudioFormat((int)af.getSampleRate(), af.getSampleSizeInBits(), af.getChannels(), Encoding.PCM_SIGNED.equals(af.getEncoding()), af.isBigEndian());
    }

    public static javax.sound.sampled.AudioFormat toJavax(AudioFormat format) {
        return new javax.sound.sampled.AudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.isSigned(), format.isBigEndian());
    }
}
