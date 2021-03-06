package de.fu_berlin.inf.ag_se.browser.utils;

import de.fu_berlin.inf.ag_se.browser.utils.colors.ColorUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ImageUtils {

	private static Logger LOGGER = Logger.getLogger(ImageUtils.class);

	public static Dimension getImageDimensions(final File file)
			throws IOException {
		Dimension result = null;

		ImageInputStream imageInputStream = new FileImageInputStream(file);

		Iterator<ImageReader> iter = ImageIO.getImageReaders(imageInputStream);
		if (iter.hasNext()) {
			ImageReader reader = iter.next();
			try {
				reader.setInput(imageInputStream);
				int width = reader.getWidth(reader.getMinIndex());
				int height = reader.getHeight(reader.getMinIndex());
				result = new Dimension(width, height);
			} catch (IOException e) {
				throw e;
			} finally {
				reader.dispose();
			}
		} else {
			System.err.println("No reader found for given file: " + file);
		}
		return result;
	}

	/**
	 * Returns a new scaled image. new Image must be disposed after use.
	 * 
	 * @param image
	 * @param width
	 * @param height
	 * @return
	 */
	public static Image resize(Image image, int width, int height) {
		if (image == null) {
			return null;
		}

		final Image scaled = new Image(Display.getDefault(), width, height);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.drawImage(image, 0, 0, image.getBounds().width,
				image.getBounds().height, 0, 0, width, height);
		gc.dispose();

		return scaled;
	}

	/**
	 * Returns a new scaled image. The new Image must be disposed after use.
	 * 
	 * @param image
	 * @param width
	 * @param height
	 * @return
	 */
	public static Image resize(ImageData imageData, int width, int height) {
		if (imageData == null) {
			return null;
		}

		if (imageData.width == width && imageData.height == height) {
			return new Image(Display.getDefault(), imageData);
		}

		Image tmpImage = null;
		Image fullImage = new Image(Display.getCurrent(), imageData);
		tmpImage = resize(fullImage, width, height);

		fullImage.dispose();
		return tmpImage;
	}

	public static ImageData resize(ImageData imageData, int width, int height,
			boolean antiAliasing) {
		if (antiAliasing) {
			Image tmpImage = resize(imageData, width, height);
			ImageData result = tmpImage.getImageData();
			tmpImage.dispose();
			return result;
		}
		return imageData.scaledTo(width, height);
	}

	/**
	 * Converts a SWT {@link org.eclipse.swt.graphics.Image} to an AWT {@link java.awt.image.BufferedImage}.
	 *
	 * @param image
	 * @return
	 */
	public static BufferedImage convertToAWT(Image image) {
		return convertToAWT(image.getImageData());
	}

	/**
	 * Converts a SWT {@link org.eclipse.swt.graphics.ImageData} to an AWT {@link java.awt.image.BufferedImage}.
	 *
	 * @param data
	 * @return
	 */
	public static BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask,
					palette.greenMask, palette.blueMask);
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(data.width,
							data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					pixelArray[0] = rgb.red;
					pixelArray[1] = rgb.green;
					pixelArray[2] = rgb.blue;
					raster.setPixels(x, y, 1, 1, pixelArray);
				}
			}
			return bufferedImage;
		} else {
			RGB[] rgbs = palette.getRGBs();
			byte[] red = new byte[rgbs.length];
			byte[] green = new byte[rgbs.length];
			byte[] blue = new byte[rgbs.length];
			for (int i = 0; i < rgbs.length; i++) {
				RGB rgb = rgbs[i];
				red[i] = (byte) rgb.red;
				green[i] = (byte) rgb.green;
				blue[i] = (byte) rgb.blue;
			}
			if (data.transparentPixel != -1) {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red,
						green, blue, data.transparentPixel);
			} else {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red,
						green, blue);
			}
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(data.width,
							data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					pixelArray[0] = pixel;
					raster.setPixel(x, y, pixelArray);
				}
			}
			return bufferedImage;
		}
	}

	public static Point resizeWithinArea(Point size, Point maxSize) {

		int finalWidth = maxSize.x;
		int finalHeight = maxSize.y;

		if (size.y > maxSize.y || size.x > maxSize.x) {
			float ratio = (float) size.x / (float) size.y;
			finalHeight = maxSize.y;
			finalWidth = Math.round(finalHeight * ratio);

			if (finalWidth > maxSize.x) {
				finalHeight = Math.round(maxSize.x / ratio);
				finalWidth = maxSize.x;
			}
		}

		return new Point(finalWidth, finalHeight);
	}

	/**
	 * Writes the given image to a temp file and returns it.
	 * <p>
	 * Note: The temporary file gets deleted when the virtual machine
	 * terminates.
	 *
	 * @param bufferedImage
	 * @param format
	 *            e.g. "png"
	 * @return
	 * @throws java.io.IOException
	 */
	public static File saveImageToTempFile(BufferedImage bufferedImage,
			String format) throws IOException {
		File tempFile = File.createTempFile("image", "." + format);
		tempFile.deleteOnExit();
		saveImageToFile(bufferedImage, format, tempFile);
		return tempFile;
	}

	/**
	 * Writes the given image to the given location.
	 * <p>
	 * Note: The temporary file gets deleted when the virtual machine
	 * terminates.
	 *
	 * @param bufferedImage
	 * @param format
	 *            e.g. "png"
	 * @return
	 * @throws java.io.IOException
	 */
	public static void saveImageToFile(BufferedImage bufferedImage,
			String format, File location) throws IOException {
		ImageIO.write(bufferedImage, format, location);
	}

	public static File saveImageToTempFile(Image image, String format)
			throws IOException {
		BufferedImage bufferedImage = convertToAWT(image);
		return saveImageToTempFile(bufferedImage, format);
	}

	private static Map<Image, URI> imageUris = new HashMap<Image, URI>();

	public static URI createUriFromImage(Image image) {
		if (image == null) {
			return null;
		}
		if (!imageUris.containsKey(image)) {
			try {
				File file = File.createTempFile(
                        ImageUtils.class.getSimpleName(), ".png");
				file.deleteOnExit();
				FileOutputStream outputStream = new FileOutputStream(file);
				ImageLoader loader = new ImageLoader();
				loader.data = new ImageData[] { image.getImageData() };
				loader.save(outputStream, SWT.IMAGE_PNG);
				outputStream.close();
				imageUris.put(image,
						new URI("file://" + file.getAbsolutePath()));
			} catch (IOException e) {
				LOGGER.error("Error making copy of image", e);
			} catch (URISyntaxException e) {
				LOGGER.error("Error generating " + URI.class.getSimpleName()
						+ " to image", e);
			}
		}
		return imageUris.get(image);
	}

	/**
	 * Returns a new {@link org.eclipse.swt.graphics.Image} where all pixels with the color of the pixel
	 * at the specified position are transparent.
	 * <p>
	 * <strong>Attention: This creates a new {@link org.eclipse.swt.graphics.Image} that needs to be
	 * disposed independently of the given one.</strong>
	 * 
	 * @param image
	 *            that serves as the basis for the generated copy
	 * @param x
	 *            coordinate of the pixel to be taken the color from
	 * @param y
	 *            coordinate of the pixel to be taken the color from
	 * @return
	 */
	public static Image createTransparentImage(Image image, int x, int y) {
		ImageData transparentImageData = image.getImageData();
		transparentImageData.transparentPixel = transparentImageData.palette
				.getPixel(transparentImageData.palette
						.getRGB(transparentImageData.getPixel(0, 0)));
		return new Image(Display.getCurrent(), transparentImageData);
	}

	public static Image getDot(int width, int height,
			de.fu_berlin.inf.ag_se.browser.utils.colors.RGB color,
			de.fu_berlin.inf.ag_se.browser.utils.colors.RGB rgb) {
		Image image = new Image(Display.getCurrent(), width, height);
		GC gc = new GC(image);
		gc.setAdvanced(true);

		Color backgroundColor = new Color(Display.getCurrent(),
				color.toClassicRGB());
		Color borderColor = rgb != null ? new Color(Display.getCurrent(),
				rgb.toClassicRGB()) : null;
		if (rgb != null) {
			PaintUtils.drawRoundedRectangle(gc, new Rectangle(0, 0, width,
                    height), backgroundColor, borderColor);
		} else {
			PaintUtils.drawRoundedRectangle(gc, new Rectangle(0, 0, width,
                    height), backgroundColor);
		}

		if (borderColor != null) {
			borderColor.dispose();
		}
		backgroundColor.dispose();
		gc.dispose();

		Image transparentImage = createTransparentImage(image, 0, 0);
		image.dispose();

		return transparentImage;
	}

	public static Image getDot(int width, int height,
			de.fu_berlin.inf.ag_se.browser.utils.colors.RGB background,
			Float borderLightness) {
		de.fu_berlin.inf.ag_se.browser.utils.colors.RGB border = borderLightness != null ? ColorUtils
				.addLightness(background, borderLightness) : null;
		return getDot(width, height, background, border);
	}
}
