// Copyright (c) 1996 Barry Silverman, Brian Silverman, Vadim Gerasimov.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

//******************************************************************************
// pdp1b.java:	Applet
//
//******************************************************************************
import java.applet.*;
import java.awt.*;
import pdp1bFrame;
import FrontPanel;
import pdp1;
import java.net.URL;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;

//==============================================================================
// Main Class for applet pdp1b
//
//==============================================================================
public class pdp1b extends Applet implements Runnable
{
	// STANDALONE APPLICATION SUPPORT:
	//		m_fStandAlone will be set to true if applet is run standalone
	//--------------------------------------------------------------------------
	Thread	 m_pdp1b = null;
	boolean m_fStandAlone = false;
	FrontPanel panel;
	pdp1bFrame frame = null;
		
	int[] memory;
	
	// STANDALONE APPLICATION SUPPORT
	// 	The main() method acts as the applet's entry point when it is run
	// as a standalone application. It is ignored if the applet is run from
	// within an HTML page.
	//--------------------------------------------------------------------------
	public static void main(String args[])
	{
		// Create Toplevel Window to contain applet pdp1b
		//----------------------------------------------------------------------
		pdp1bFrame frame = new pdp1bFrame("pdp1b");

		// Must show Frame before we size it so insets() will return valid values
		//----------------------------------------------------------------------
		frame.show();
        frame.hide();

		pdp1b applet_pdp1b = new pdp1b();
		applet_pdp1b.frame = frame;

		frame.add("Center", applet_pdp1b);
		applet_pdp1b.m_fStandAlone = true;
		pdp1.standAlone = true;
		applet_pdp1b.init();
		applet_pdp1b.start();
        frame.show();
	}

	// pdp1b Class Constructor
	//--------------------------------------------------------------------------
	public pdp1b()
	{
		// TODO: Add constructor code here
	}

	// APPLET INFO SUPPORT:
	//		The getAppletInfo() method returns a string describing the applet's
	// author, copyright date, or miscellaneous information.
    //--------------------------------------------------------------------------
	public String getAppletInfo()
	{
		return "Name: pdp1b\r\n" +
		       "Author: Barry Silverman\r\n" +
		       "Created with Microsoft Visual J++ Version 1.0";
	}


	// The init() method is called by the AWT when an applet is first loaded or
	// reloaded.  Override this method to perform whatever initialization your
	// applet needs, such as initializing data structures, loading images or
	// fonts, creating frame windows, setting the layout manager, or adding UI
	// components.
    //--------------------------------------------------------------------------
	public void init()
	{
		panel = new FrontPanel( this );
		panel.CreateControls();
		initmachine();
	}

	// Place additional applet clean up code here.  destroy() is called when
	// when you applet is terminating and being unloaded.
	//-------------------------------------------------------------------------
	public void destroy()
	{
		// TODO: Place applet cleanup code here
	}

	//		The start() method is called when the page containing the applet
	// first appears on the screen. The AppletWizard's initial implementation
	// of this method starts execution of the applet's thread.
	//--------------------------------------------------------------------------
	public void start()
	{
		if (m_pdp1b == null)
		{
			m_pdp1b = new Thread(this);
			m_pdp1b.start();
		}
	}
	
	//		The stop() method is called when the page containing the applet is
	// no longer on the screen. The AppletWizard's initial implementation of
	// this method stops execution of the applet's thread.
	//--------------------------------------------------------------------------
	public void stop()
	{
		if (m_pdp1b != null)
		{
			m_pdp1b.stop();
			m_pdp1b = null;
		}
	}

		// THREAD SUPPORT
	//		The run() method is called when the applet's thread is started. If
	// your applet performs any ongoing activities without waiting for user
	// input, the code for implementing that behavior typically goes here. For
	// example, for an applet that performs animation, the run() method controls
	// the display of images.
	//--------------------------------------------------------------------------
	public void run()
	{
		panel.display.init();
		while (true)
		{
			try
			{
				pdp1.waitFor(); // wait for a "run"
				// Start the console update process

		//		kicker foo = new kicker();
		//		Thread kicker = new Thread(foo);
		//		kicker.start();

				while(panel.runpc >= 0 && pdp1.pc != panel.runpc){
					pdp1.step();
				}
				
		//		kicker.stop();
		//		kicker = null;
				panel.runpc = -1;
				panel.updatepanel();
			}
			catch (Exception e)
			{
				// TODO: Place exception-handling code here in case an
				//       InterruptedException is thrown by Thread.sleep(),
				//		 meaning that another thread has interrupted this one
				System.out.println(e);
				stop();
			}
		}
	}

	public void resize(int width, int height)
	{
		if(m_fStandAlone){
			frame.resize(frame.insets().left + frame.insets().right + width, 
					 frame.insets().top + frame.insets().bottom + height);
		} else {
			super.resize(width, height);
		}
	}

	void initmachine() {
		memory=pdp1.memory;
		loadtape("spacewar.bin");
		pdp1.pc=4; 
		pdp1.panel = panel;
		panel.updatepanel();
	}
	
	void loadtape(String tapefile) {
		URL base;
		try {
			if(m_fStandAlone){
				base=new URL("file", "", System.getProperty("user.dir")+"/");
			} else {
				base = getDocumentBase();
			}
			InputStream is = new URL(base, tapefile).openStream();
			DataInputStream f = new DataInputStream(is);
        	for (int i = 0; i < 010000; i++) memory[i] = f.readInt();
		 		is.close();}
		catch (IOException e){System.out.println(e);}
   }

   public boolean action(Event e, Object arg) {
   if (e.id==Event.ACTION_EVENT) {
			panel.updatemachine();
			panel.buttondispatch(((Button)e.target).getLabel());
			return true;
			}

    if (e.id!=Event.WINDOW_DESTROY) return false;
    Runtime.getRuntime().exit(0);
    return false;
   }
     
   String os(int n) {
    return Integer.toString(n+01000000,8).substring(1);
   }
}

class kicker implements Runnable {
	public void run()
	{
		while (true)
		{
			try
			{
				Thread.sleep(150);
				pdp1.updatepanel();	 // synchronized update
			}
			catch (Exception e)
			{
				System.out.println(e);
				break;
			}
		}
	}
}