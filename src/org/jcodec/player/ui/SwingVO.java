package org.jcodec.player.ui;

import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Rect;
import org.jcodec.player.filters.VideoOutput;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.SwingUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Video output that draws on swing panel
 * 
 * @author The JCodec project
 * 
 */
public class SwingVO extends JPanel implements VideoOutput {

    private BufferedImage img;
    private Rational pasp;
    private Rect crop;
    

    public SwingVO() {
    	setDoubleBuffered(true);
    }
    
    public void show(Picture pic, Rational pasp) {

//        if (img != null && img.getWidth() != pic.getWidth() && img.getHeight() != pic.getHeight())
//            img = null;

//        if (img == null)
//            img = new BufferedImage(pic.getWidth(), pic.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

    	img = AWTUtil.toBufferedImage(pic);
        this.pasp = pasp;
        this.crop = pic.getCrop();

        repaint();
    }

    @Override
    public ColorSpace getColorSpace() {
        return RGB;
    }

    @Override
    public void paint(Graphics g) {
        if (img == null || pasp == null)
            return;
        BufferedImage bi = img;
        if (crop == null
                || (crop.getX() == 0 && crop.getY() == 0 && crop.getWidth() == bi.getWidth() && crop.getHeight() == bi
                        .getHeight())) {
            int width = pasp.getNum() * bi.getWidth() / pasp.getDen();
            int height = (getWidth() * bi.getHeight()) / width;

            int offY = (getHeight() - height) / 2;
            g.drawImage(bi, 0, 0, getWidth(), offY + height, this);
        } else {
            int width = pasp.getNum() * crop.getWidth() / pasp.getDen();
            int height = (getWidth() * crop.getHeight()) / width;

            int offY = (getHeight() - height) / 2;
            g.drawImage(bi, 0, 0, getWidth(), offY + height, crop.getX(), crop.getY(),
                    crop.getX() + crop.getWidth(), crop.getY() + crop.getHeight(), this);
        }
    }
}