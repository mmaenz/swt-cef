/*********************************************************************
 * Copyright (c) 2019 QNX Software Systems and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *********************************************************************/
package org.eclipse.swt.cef;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.internal.Platform;
//import org.eclipse.swt.internal.gtk.GDK;
//import org.eclipse.swt.internal.gtk.GTK;
//import org.eclipse.swt.internal.gtk.GdkWindowAttr;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;

public class Chromium extends Canvas {
	static final String USE_OWNDC_KEY = "org.eclipse.swt.internal.win32.useOwnDC";
	static long window = 0;
	static boolean init = true;
	
	static {
		NativeLoader.load();
	}

	static int checkStyle(Composite parent, int style) {
		if (parent != null) {
			parent.getDisplay().setData(USE_OWNDC_KEY, true);
		}
		return style;
	}

	public Chromium(Composite parent, int style) {
		super(parent, checkStyle(parent, style));
		parent.getDisplay().setData(USE_OWNDC_KEY, false);
		window = getHandle();

		addListener(SWT.Resize, resizeEvent -> {
			Rectangle rect = parent.getClientArea(); 
			setBounds(rect);
			update();
			redraw();
		});
		
		addDisposeListener(disposeEvent -> {
			if (window != 0) {
				release();
				dispose();
				window = 0;
			}
		});

		addPaintListener(paintevent -> {
			if (init) {
				init = false;
				final Rectangle rectangle = getClientArea();
				try {
					init(window);
					long t = CreateBrowser("iconten.de");
				} catch (final Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				}});
		//init(window);
	}

	public long CreateBrowser(String url) {
		return create(window, url);
	}
	
	public void Work() {
		work();
	}
	
	native static int init(long parentId);
	//native static int init(long parentId, int x, int y, int width, int height);
	native static long create(long parentId, String url);
	
	native static int work();

	native static int release();

	/**
	 * The handle to the native window. <b>IMPORTANT:</b> This field is <em>not</em>
	 * part of the SWT public API and it is available only on Windows-OS.
	 * 
	 * 
	 * @return native HWnd on Windows-OS
	 * @throws Exception
	 */
	public long getHandle() {
		long handle = 0;
		java.lang.reflect.Field _viewField;
		java.lang.reflect.Field _idField;
		try {
			if (NativeLoader.isMac()) {
				_viewField = Control.class.getDeclaredField("view");
				final Object view = _viewField.get(this);
				final Class<?> idClass = Class.forName("org.eclipse.swt.internal.cocoa.id");
				_idField = idClass.getDeclaredField("id");
				handle = _idField.getLong(view);
			} else {
				_idField = Control.class.getDeclaredField("handle");
				handle = _idField.getLong(this);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return handle;
	}

}
