package org.jcodec.player.ui;

import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.player.filters.VideoOutput;
import org.jcodec.scale.AWTUtil;

public class FileVO implements VideoOutput {

	@Override
	public void show(Picture pic, Rational rational) {
		BufferedImage bi = AWTUtil.toBufferedImage(pic);
		try {
			ImageIO.write(bi, "jpg", new File("d:/tttttest/" + System.currentTimeMillis() + ".jpg"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public ColorSpace getColorSpace() {
		return RGB;
	}

}
