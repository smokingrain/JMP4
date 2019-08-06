package org.jcodec.player.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.JFrame;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.player.Player;
import org.jcodec.player.Stepper;
import org.jcodec.player.filters.FrameGrabSource;
import org.jcodec.player.filters.IMedia;
import org.jcodec.player.filters.JCodecPacketSource;
import org.jcodec.player.filters.JCodecVideoSource;
import org.jcodec.player.filters.JSoundAudioOut;
import org.jcodec.player.filters.PacketSource;
import org.jcodec.player.filters.audio.AudioMixer;
import org.jcodec.player.filters.audio.AudioMixer.Pin;
import org.jcodec.player.filters.audio.AudioSource;
import org.jcodec.player.filters.audio.JCodecAudioSource;
import org.jcodec.player.ui.FileVO;
import org.jcodec.player.ui.SwingVO;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PlayerMain implements KeyListener {
    private Player player;
    private Stepper stepper;
    private JCodecVideoSource video;
    private SwingVO vo;
    private AudioMixer mixer;

    public static void main(String[] args) throws Exception {
//        if (args.length < 1) {
//            System.out.println("Syntax: <url>");
//            return;
//        }
//        new PlayerMain(new URL("http://fs.mv.web.kugou.com/201907231643/29d177c5c4f670e46f0df8febf8b9c79/G126/M07/1F/09/HocBAFr1djOAFrS4APCe7NpBBMk720.mp4"));
    
    	new PlayerMain(new File("f:\\git\\WeChat\\temp\\1565061130282-1.mp4"));
    }

    public PlayerMain(File file) throws IOException {
        JFrame frame = new JFrame("Player");

        vo = new SwingVO();
        frame.getContentPane().add(vo, BorderLayout.CENTER);

        // Finish setting up the frame, and show it.
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        vo.setVisible(true);

        File cacheWhere = new File(System.getProperty("user.home"), "Library/JCodec");
        IOUtils.forceMkdir(cacheWhere);

//        IMedia http = new HttpMedia(url, cacheWhere);
        
        JCodecPacketSource jcp = new JCodecPacketSource(file);

        final PacketSource videoTrack = jcp.getVideo();///http.getVideoTrack();
        if(null != videoTrack) {
        	video = new JCodecVideoSource(videoTrack);
        }

        List<PacketSource> audioTracks = jcp.getAudio();//http.getAudioTracks();
        AudioSource[] audio = new AudioSource[audioTracks.size()];
        for (int i = 0; i < audioTracks.size(); i++) {
        	audio[i] = new JCodecAudioSource(audioTracks.get(i));
        }
        mixer = new AudioMixer(audio.length, audio);
        
        
        FrameGrabSource fgs = new FrameGrabSource(file, videoTrack);
        player = new Player(fgs, audio[0], vo, new JSoundAudioOut());

        frame.addKeyListener(this);

        frame.pack();
        frame.setVisible(true);
        frame.setSize(new Dimension(100, 176));

        player.play();
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (player.getStatus() == Player.Status.PAUSED) {
                if (stepper != null) {
                    player.seek(stepper.getPos());
                    stepper = null;
                }
                player.play();
            } else
                player.pause();
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            RationalLarge pos = player.getPos();
            player.seek(new RationalLarge(pos.getNum() - pos.getDen() * 15, pos.getDen()));
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            RationalLarge pos = player.getPos();
            player.seek(new RationalLarge(pos.getNum() + pos.getDen() * 15, pos.getDen()));
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            // if (player.getStatus() != Player.Status.PAUSED) {
            // player.pause();
            // return;
            // }
            //
            // try {
            // if (stepper == null) {
            // stepper = new Stepper(video, mixer, vo, new JSoundAudioOut());
            // stepper.setListeners(player.getListeners());
            // stepper.gotoFrame(player.getFrameNo());
            // }
            // stepper.prev();
            // } catch (IOException e1) {
            // System.out.println("Couldn't step " + e1.getMessage());
            // }
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            if (player.getStatus() != Player.Status.PAUSED) {
                player.pause();
                return;
            }
            try {
                if (stepper == null) {
                    stepper = new Stepper(video, mixer, vo, new JSoundAudioOut());
                    stepper.setListeners(player.getListeners());
                    stepper.gotoFrame(player.getFrameNo());
                }
                stepper.next();
            } catch (IOException e1) {
                System.out.println("Couldn't step " + e1.getMessage());
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            player.destroy();
            System.exit(-1);
        } else if (e.getKeyChar() >= '0' && e.getKeyChar() < '9') {
            int ch = e.getKeyChar() - '0';
            for (Pin pin : mixer.getPins()) {
                if (ch < pin.getLabels().length) {
                    pin.toggle(ch);
                    break;
                } else
                    ch -= pin.getLabels().length;
            }
        }
    }
}