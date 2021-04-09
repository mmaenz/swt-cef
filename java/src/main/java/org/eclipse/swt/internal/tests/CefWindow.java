package org.eclipse.swt.internal.tests;

import org.eclipse.swt.SWT;
import org.eclipse.swt.cef.Chromium;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class CefWindow {
	private static Chromium chromium;
	
	public static void main(String[] args) {
		System.out.println("CEF-Browser!");

		final Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setText("SWT based CEF / chromium browser");
		shell.setLayout(new FillLayout());
		shell.setSize(640, 480);

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FillLayout());

		chromium = new Chromium(composite, SWT.NONE);
		chromium.setBounds(20, 20, 300, 300);
		
		chromium.setFocus();
		shell.open();
		
		chromium.Work();
	}
}
