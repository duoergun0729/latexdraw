package net.sf.latexdraw.glib.views.Java2D;

import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

import net.sf.latexdraw.glib.models.interfaces.IArc;

/**
 * Defines a view of the IArc model.<br>
 * <br>
 * This file is part of LaTeXDraw.<br>
 * Copyright (c) 2005-2011 Arnaud BLOUIN<br>
 * <br>
 * LaTeXDraw is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 * <br>
 * LaTeXDraw is distributed without any warranty; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.<br>
 * <br>
 * 03/20/2008<br>
 * @author Arnaud BLOUIN
 * @since 3.0
 */
public class LArcView extends LEllipseView<IArc> {
	/**
	 * Creates an initialises the Java view of a LArc.
	 * @param model The model to view.
	 * @since 3.0
	 */
	public LArcView(final IArc model) {
		super(model);
		update();
	}



	@Override
	protected void setRectangularShape(final Path2D path, final double tlx, final double tly, final double width, final double height) {
		setArcPath(path, tlx, tly, width, height, shape.getAngleStart(), shape.getAngleEnd());
	}



	/**
	 * Appends an arc to the given path.
	 * @param path The path to fill.
	 * @param tlx The top-left point of the arc.
	 * @param tly The bottom right point of the arc.
	 * @param width The width of the arc.
	 * @param height The height of the arc.
	 * @param startAngle The start angle of the arc.
	 * @param endAngle The end angle of the arc.
	 * @since 3.0
	 */
	public static void setArcPath(final Path2D path, final double tlx, final double tly, final double width, final double height, final double startAngle, final double endAngle) {
		if(path!=null) {
			final double sAngle = Math.toDegrees(startAngle%(Math.PI*2.));
			final double eAngle = Math.toDegrees(endAngle%(Math.PI*2.));
			path.append(new Arc2D.Double(tlx, tly, width, height, sAngle<eAngle ? sAngle : eAngle, eAngle>sAngle ? eAngle : sAngle, Arc2D.OPEN), false);
		}
	}


//	@Override
//	public void update() {
//		super.update();
//
//		IArc arc  = (IArc)shape;
//		IPoint gc = arc.getGravityCentre();
//		IPoint pt = arc.getStartPoint();
//
//		startHandler.setCentre((gc.getX()+pt.getX())/2.,(gc.getY()+pt.getY())/2.);
//		pt = arc.getEndPoint();
//		endHandler.setCentre((gc.getX()+pt.getX())/2.,(gc.getY()+pt.getY())/2.);
//	}
//
//
//
//	@Override
//	public void paint(Graphics2D g) {
//		super.paint(g);
//
//		if(isSelected) {
//			IPoint p = beginRotation(g);
//
//			startHandler.paint(g);
//			endHandler.paint(g);
//
//			if(p!=null)
//				endRotation(g, p);
//		}
//	}
//
//
//	/**
//	 * Sets the starting or ending angle of the arc.
//	 * @param isStartAngle True: the starting angle will be set. Otherwise it is the ending angle.
//	 * @param pos The new position of the angle.
//	 * @since 3.0
//	 */
//	public void setAngle(boolean isStartAngle, IPoint pos)
//	{
//		if(!GLibUtilities.INSTANCE.isValidPoint(pos))
//			return ;
//
//		IArc arc = (IArc)shape;
//
//		ILine line = new LLine(arc.getGravityCentre(), pos);
//		IPoint[] inters = arc.getIntersection(line);
//
//		if(inters==null)
//			return ;
//
//		IPoint inter = inters.length==1 ? inters[0] :
//					   inters[0].distance(pos)<inters[1].distance(pos) ? inters[0] : inters[1];
//
//		double angle = Math.acos((inter.getX()-arc.getGravityCentre().getX())/arc.getA());
//
//		if(pos.getY()>arc.getGravityCentre().getY())
//			angle = 2*Math.PI - angle;
//
//		if(isStartAngle)
//			arc.setAngleStart(angle);
//		else
//			arc.setAngleEnd(angle);
//
//		update();
//	}
}
