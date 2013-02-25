package net.sf.latexdraw.glib.views.Java2D.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.latexdraw.badaboom.BadaboomCollector;
import net.sf.latexdraw.filters.PDFFilter;
import net.sf.latexdraw.filters.PSFilter;
import net.sf.latexdraw.filters.TeXFilter;
import net.sf.latexdraw.glib.models.interfaces.DrawingTK;
import net.sf.latexdraw.glib.models.interfaces.IPoint;
import net.sf.latexdraw.glib.models.interfaces.IShape;
import net.sf.latexdraw.glib.models.interfaces.IText;
import net.sf.latexdraw.glib.models.interfaces.IText.TextPosition;
import net.sf.latexdraw.glib.views.Java2D.interfaces.IViewText;
import net.sf.latexdraw.glib.views.latex.DviPsColors;
import net.sf.latexdraw.glib.views.latex.LaTeXGenerator;
import net.sf.latexdraw.glib.views.pst.PSTricksConstants;
import net.sf.latexdraw.util.ImageCropper;
import net.sf.latexdraw.util.LFileUtils;
import net.sf.latexdraw.util.LNumber;
import net.sf.latexdraw.util.LResources;
import net.sf.latexdraw.util.LSystem;
import net.sf.latexdraw.util.LSystem.OperatingSystem;
import net.sf.latexdraw.util.StreamExecReader;
import sun.font.FontDesignMetrics;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

/**
 * Defines a view of the IText model.<br>
 * <br>
 * This file is part of LaTeXDraw.<br>
 * Copyright (c) 2005-2013 Arnaud BLOUIN<br>
 * <br>
 * LaTeXDraw is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 * <br>
 * LaTeXDraw is distributed without any warranty; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.<br>
 * <br>
 * 05/23/2010<br>
 * @author Arnaud BLOUIN
 * @since 3.0
 */
class LTextView extends LShapeView<IText> implements IViewText {
	/** The picture. */
	protected Image image;

	/** The log of the compilation. */
	protected String log;

	/** The path of the files: for instance on Unix is can be /tmp/latexdraw180980 (without any extension). */
	private String pathPic;

	/** Used to detect if the last version of the text is different from the view. It helps to update the picture. */
	private String lastText;

	/** Used to detect if the last version of the text is different from the view. It helps to update the picture. */
	private Color lastColour;

	/** Used to detect if the last version of the text is different from the view. It helps to update the picture. */
	private TextPosition lastTextPos;

	public static final Font FONT = new Font("Times New Roman", Font.PLAIN, 18); //$NON-NLS-1$

	public static final FontMetrics FONT_METRICS = FontDesignMetrics.getMetrics(FONT);

	/** A ratio used to create bigger thumbnails to improve the quality of the displayed image. */
	protected static final double SCALE_IMAGE = 2.;


	/**
	 * Creates and initialises a text view.
	 * @param model The model to view.
	 * @throws IllegalArgumentException If the given model is null.
	 * @since 3.0
	 */
	protected LTextView(final IText model) {
		super(model);

		log			= ""; //$NON-NLS-1$
		lastText 	= ""; //$NON-NLS-1$
		lastColour 	= null;
		lastTextPos	= null;
		update();
	}


	@Override
	public void update() {
		if(image==null && log.length()==0 || !lastText.equals(shape.getText()) ||
			lastColour==null || !lastColour.equals(shape.getLineColour()) ||
			lastTextPos==null || lastTextPos!=shape.getTextPosition()) {
			updateImage();
			lastText 	= shape.getText();
			lastColour 	= shape.getLineColour();
			lastTextPos	= shape.getTextPosition();
		}

		super.update();
	}


	@Override
	protected void finalize() {
		flush();
	}



	@Override
	public void flush() {
		super.flush();

		flushPictures();
		lastText = ""; //$NON-NLS-1$
	}


	/**
	 * Flushes the pictures of the text and all the related resources.
	 * @since 3.0
	 */
	protected void flushPictures() {
		if(image!=null)
			image.flush();

		if(pathPic!=null) {
			final File file = new File(pathPic);
			if(file.exists() && file.canWrite())
				file.delete();
		}

		pathPic  = null;
		image 	 = null;
	}


	@Override
	public void updateImage() {
		flushPictures();
		image = createImage();
	}


	@Override
	public Image getImage() {
		return image;
	}



	/**
	 * @return The LaTeX compiled picture of the text or null.
	 * @since 3.0
	 */
	private Image createImage() {
		Image bi = null;
		log = ""; //$NON-NLS-1$

		try {
			final String code = shape.getText();

			if(code!=null && !code.isEmpty()) {
				final File tmpDir 		= LFileUtils.INSTANCE.createTempDir();
				final String doc      	= getLaTeXDocument();
				pathPic					= tmpDir.getAbsolutePath() + LResources.FILE_SEP + "latexdrawTmpPic" + System.currentTimeMillis(); //$NON-NLS-1$
				final String pathTex  	= pathPic + TeXFilter.TEX_EXTENSION;
				final OperatingSystem os = LSystem.INSTANCE.getSystem();

				try {
					try(final FileOutputStream fos 	= new FileOutputStream(pathTex);
						final OutputStreamWriter osw= new OutputStreamWriter(fos)){
						osw.append(doc);
					}

					boolean ok = execute(new String[]{os.getLatexBinPath(), "--halt-on-error", "--interaction=nonstopmode", "--output-directory=" + tmpDir.getAbsolutePath(), pathTex}); //$NON-NLS-1$ //$NON-NLS-2$
					new File(pathTex).delete();
					new File(pathPic + ".aux").delete(); //$NON-NLS-1$
					new File(pathPic + ".log").delete(); //$NON-NLS-1$

					if(ok) {
						ok = execute(new String[]{os.getDvipsBinPath(), pathPic + ".dvi",  "-o", pathPic + PSFilter.PS_EXTENSION}); //$NON-NLS-1$ //$NON-NLS-2$
						new File(pathPic + ".dvi").delete(); //$NON-NLS-1$
					}
					if(ok)
						ok = execute(new String[]{os.getPs2pdfBinPath(), pathPic + PSFilter.PS_EXTENSION, pathPic + PDFFilter.PDF_EXTENSION}); //$NON-NLS-1$

					if(ok)
						try {
							try(RandomAccessFile raf = new RandomAccessFile(new File(pathPic+PDFFilter.PDF_EXTENSION), "r");
								FileChannel fc = raf.getChannel()){
								final PDFFile pdfFile = new PDFFile(fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()));

								if(pdfFile.getNumPages()==1) {
									final PDFPage page 		= pdfFile.getPage(1);
									final Rectangle2D bound = page.getBBox();
								    final Image img 		= page.getImage((int)bound.getWidth(), (int)bound.getHeight(), bound, null, false, true);

								    if(img instanceof BufferedImage)
									    bi = ImageCropper.cropImage((BufferedImage)img);

								    if(img!=null)
								    	img.flush();
								}
								else BadaboomCollector.INSTANCE.add(new IllegalArgumentException("Not a single page: " + pdfFile.getNumPages()));

								new File(pathPic + PDFFilter.PDF_EXTENSION).delete();
							}
						}catch(final Exception ex) { BadaboomCollector.INSTANCE.add(ex); }
				}catch(final IOException ex) { BadaboomCollector.INSTANCE.add(ex); }
			}
		}
		catch(final Exception e) {
			final StringWriter sw = new StringWriter();
		    final PrintWriter pw  = new PrintWriter(sw);

		    e.printStackTrace(pw);
			new File(pathPic + TeXFilter.TEX_EXTENSION).delete();
			new File(pathPic + PDFFilter.PDF_EXTENSION).delete();
			new File(pathPic + PSFilter.PS_EXTENSION).delete();
			new File(pathPic + ".dvi").delete(); //$NON-NLS-1$
			new File(pathPic + ".aux").delete(); //$NON-NLS-1$
			new File(pathPic + ".log").delete(); //$NON-NLS-1$
			BadaboomCollector.INSTANCE.add(new FileNotFoundException("Log:\n" + log + "\nException:\n" + sw));
			pw.flush();
		}

		return bi;
	}


	@Override
	public String getLatexErrorMessageFromLog() {
		final Matcher matcher 		= Pattern.compile(".*\r?\n").matcher(log); //$NON-NLS-1$
		final StringBuilder errors 	= new StringBuilder();
		String line;

		while(matcher.find()) {
			line = matcher.group();

			if(line.startsWith("!")) { //$NON-NLS-1$
				errors.append(line.substring(2, line.length()));
				boolean ok = true;
				while(ok && matcher.find()) {
					line = matcher.group();

					if(line.startsWith("l.")) //$NON-NLS-1$
						ok = false;
					else
						errors.append(LResources.EOL).append(line).append(LResources.EOL);
				}
			}
		}

		return errors.toString();
	}


	/**
	 * Executes a given command and returns the log.
	 * @param cmd The command to execute.
	 * @return True if the command exit normally.
	 * @since 3.0
	 */
	private boolean execute(final String[] cmd) {
		try {
			final Process process = Runtime.getRuntime().exec(cmd);
			final StreamExecReader errReader = new StreamExecReader(process.getErrorStream());
			final StreamExecReader outReader = new StreamExecReader(process.getInputStream());

			errReader.start();
			outReader.start();

			if(process.waitFor()==0)
				return true;

			log += outReader.getLog() + LResources.EOL + errReader.getLog();
		}catch(final IOException | InterruptedException ex) {
			log += ex.getMessage();
		}

		return false;
	}


	@Override
	public String getLaTeXDocument() {
		final String code		= shape.getText();
		final StringBuilder doc = new StringBuilder();
		final Color textColour	= shape.getLineColour();
		final boolean coloured;

		// We must scale the text to fit its latex size: latexdrawDPI/latexDPI is the ratio to scale the
		// created png picture.
		final double scale = IShape.PPC*PSTricksConstants.INCH_VAL_CM/PSTricksConstants.INCH_VAL_PT*SCALE_IMAGE;

		doc.append("\\documentclass[10pt]{article}\\usepackage[usenames,dvipsnames]{pstricks}"); //$NON-NLS-1$
		doc.append(LaTeXGenerator.getPackages());
		doc.append("\\usepackage[left=0cm,top=0.1cm,right=0cm,nohead,nofoot,paperwidth=100cm,paperheight=100cm]{geometry}");
		doc.append("\\pagestyle{empty}\\begin{document}\\psscalebox{"); //$NON-NLS-1$
		doc.append((float)LNumber.INSTANCE.getCutNumber(scale)).append(' ');
		doc.append((float)LNumber.INSTANCE.getCutNumber(scale)).append('}').append('{');

		if(!textColour.equals(PSTricksConstants.DEFAULT_LINE_COLOR)) {
			String name = DviPsColors.INSTANCE.getColourName(textColour);
			coloured = true;

			if(name==null)
				name = DviPsColors.INSTANCE.addUserColour(textColour);

			doc.append(DviPsColors.INSTANCE.getUsercolourCode(name)).append("\\textcolor{").append(name).append('}').append('{'); //$NON-NLS-1$
		}
		else coloured = false;

		doc.append(code);

		if(coloured)
			doc.append('}');

		doc.append("}\\end{document}"); //$NON-NLS-1$

		return doc.toString();
	}


	@Override
	public boolean intersects(final Rectangle2D rec) {
		if(rec==null)
			return false;

		final Shape sh = getRotatedShape2D(shape.getRotationAngle(), border,
						DrawingTK.getFactory().createPoint(border.getMinX(), border.getMinY()),
						DrawingTK.getFactory().createPoint(border.getMaxX(), border.getMaxY()));
		return sh.contains(rec) || sh.intersects(rec);
	}


	@Override
	public boolean contains(final double x, final double y) {
		return border.contains(x, y);
	}


	private IPoint getTextPositionImage() {
		switch(shape.getTextPosition()) {
			case BOT : return DrawingTK.getFactory().createPoint(shape.getX()-image.getWidth(null)/2./SCALE_IMAGE, shape.getY()-image.getHeight(null)/SCALE_IMAGE);
			case TOP : return DrawingTK.getFactory().createPoint(shape.getX()-image.getWidth(null)/2./SCALE_IMAGE, shape.getY());
			case BOT_LEFT : return DrawingTK.getFactory().createPoint(shape.getX(), shape.getY()-image.getHeight(null)/SCALE_IMAGE);
			case TOP_LEFT : return DrawingTK.getFactory().createPoint(shape.getX(), shape.getY());
			case BOT_RIGHT : return DrawingTK.getFactory().createPoint(shape.getX()-image.getWidth(null)/SCALE_IMAGE, shape.getY()-image.getHeight(null)/SCALE_IMAGE);
			case TOP_RIGHT : return DrawingTK.getFactory().createPoint(shape.getX()-image.getWidth(null)/SCALE_IMAGE, shape.getY());
			case LEFT : return DrawingTK.getFactory().createPoint(shape.getX(), shape.getY()-image.getHeight(null)/SCALE_IMAGE/2.);
			case RIGHT : return DrawingTK.getFactory().createPoint(shape.getX()-image.getWidth(null)/SCALE_IMAGE, shape.getY()-image.getHeight(null)/SCALE_IMAGE/2.);
			case BASE: case BASE_LEFT: case BASE_RIGHT:
			case CENTER : return DrawingTK.getFactory().createPoint(shape.getX()-image.getWidth(null)/2./SCALE_IMAGE, shape.getY()-image.getHeight(null)/SCALE_IMAGE/2.);
		}

		return null;
	}


	private IPoint getTextPositionText() {
		final TextLayout tl = new TextLayout(shape.getText(), FONT, FONT_METRICS.getFontRenderContext());
		final Rectangle2D bounds = tl.getBounds();

		switch(shape.getTextPosition()) {
			case BOT : return DrawingTK.getFactory().createPoint(shape.getX()-bounds.getWidth()/2., shape.getY());
			case TOP : return DrawingTK.getFactory().createPoint(shape.getX()-bounds.getWidth()/2., shape.getY()+bounds.getHeight());
			case BOT_LEFT : return DrawingTK.getFactory().createPoint(shape.getX(), shape.getY());
			case TOP_LEFT : return DrawingTK.getFactory().createPoint(shape.getX(), shape.getY()+bounds.getHeight());
			case BOT_RIGHT : return DrawingTK.getFactory().createPoint(shape.getX()-bounds.getWidth(), shape.getY());
			case TOP_RIGHT : return DrawingTK.getFactory().createPoint(shape.getX()-bounds.getWidth(), shape.getY()+bounds.getHeight());
			case LEFT : return DrawingTK.getFactory().createPoint(shape.getX(), shape.getY()+bounds.getHeight()/2.);
			case RIGHT : return DrawingTK.getFactory().createPoint(shape.getX()-bounds.getWidth(), shape.getY()+bounds.getHeight()/2.);
			case BASE: case BASE_RIGHT: case BASE_LEFT:
			case CENTER : return DrawingTK.getFactory().createPoint(shape.getX()-bounds.getWidth()/2., shape.getY()+bounds.getHeight()/2.);
		}

		return null;
	}



	@Override
	public void paint(final Graphics2D g) {
		if(g==null)
			return ;

		final IPoint p = beginRotation(g);
		final IPoint position = image==null ? getTextPositionText() : getTextPositionImage();

		if(image==null) {
			g.setColor(shape.getLineColour());
			g.setFont(FONT);
			g.drawString(shape.getText(), (int)position.getX(), (int)position.getY());
		}
		else {
			g.scale(1/SCALE_IMAGE, 1/SCALE_IMAGE);
			g.drawImage(image, (int)(position.getX()*SCALE_IMAGE), (int)(position.getY()*SCALE_IMAGE), null);
			g.scale(SCALE_IMAGE, SCALE_IMAGE);
		}

		if(p!=null)
			endRotation(g, p);
	}



	@Override
	public void updateBorder() {
		final IPoint position = image==null ? getTextPositionText() : getTextPositionImage();
		final double angle = shape.getRotationAngle();
		double tlx;
		double tly;
		double widthBorder;
		double heightBorder;

		if(image==null) {
			final TextLayout tl = new TextLayout(shape.getText(), FONT, FONT_METRICS.getFontRenderContext());
			final Rectangle2D bounds = tl.getBounds();
			tlx = position.getX();
			tly = position.getY()-bounds.getHeight()+tl.getDescent();
			widthBorder = tl.getAdvance();
			heightBorder = bounds.getHeight();
		}
		else {
			tlx = position.getX();
			tly = position.getY();
			widthBorder = image.getWidth(null)*(1/SCALE_IMAGE);
			heightBorder = image.getHeight(null)*(1/SCALE_IMAGE);
		}

		if(LNumber.INSTANCE.equals(angle, 0.))
			border.setFrame(tlx, tly, widthBorder, heightBorder);
		else {
			IPoint tl = DrawingTK.getFactory().createPoint();
			IPoint br = DrawingTK.getFactory().createPoint();
			getRotatedRectangle(tlx, tly, widthBorder, heightBorder, angle, shape.getGravityCentre(), tl, br);
			// The border of the rotated rectangle is now the border of the rectangular view.
			border.setFrameFromDiagonal(tl.getX(), tl.getY(), br.getX(), br.getY());
		}
	}


	@Override
	protected void updateDblePathInside() {
		// Nothing to do.
	}

	@Override
	protected void updateDblePathMiddle() {
		// Nothing to do.
	}

	@Override
	protected void updateDblePathOutside() {
		// Nothing to do.
	}

	@Override
	protected void updateGeneralPathInside() {
		// Nothing to do.
	}

	@Override
	protected void updateGeneralPathMiddle() {
		// Nothing to do.
	}

	@Override
	protected void updateGeneralPathOutside() {
		// Nothing to do.
	}


	@Override
	public boolean isToolTipVisible(final double x, final double y) {
		return border.contains(x, y);
	}


	@Override
	public String getToolTip() {
		String msg;

		if(log==null || log.length()==0)
			msg = "";
		else {
			msg = getLatexErrorMessageFromLog();

			if(msg.length()==0)
				msg = log;
		}

		return msg;
	}
}
