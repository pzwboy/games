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

//------------------------------------------------------------------------------
// FrontPanel.java:
//		Implementation of "control creator" class FrontPanel
//------------------------------------------------------------------------------
import java.awt.*;
import java.net.URL;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import pdp1;

public class FrontPanel
{
	Container    m_Parent       = null;
	boolean      m_fInitialized = false;
 	int runpc = -1;

	static final int AC=1, IO=2, PC=3, MA=4, MB=5, BPT=6, SSW=7;
	
	register rac = new register(AC, 0777777);
	register rio = new register(IO, 0777777);
	register rpc = new register(PC, 07777);
	register rma = new register(MA, 07777);
	register rmb = new register(MB, 0777777);
	register rbpt= new register(BPT, 07777);
	register rss = new register(SSW, 077, 2);
	int [] memory = pdp1.memory;
	// Control definitions
	//--------------------------------------------------------------------------
	Button        Run;
	Button        SS;
	Button        SS2;
	Button        SS3;
	Label         PC1;
	Label         MA1;
	Label         IO1;
	Label         AC1;
	Label         MB1;
	Label         BP;
	Label		  SW;
	Display		display;

	// Constructor
	//--------------------------------------------------------------------------
	public FrontPanel (Container parent)
	{
		m_Parent = parent;
	}

	// Initialization.
	//--------------------------------------------------------------------------
	public boolean CreateControls()
	{
		// CreateControls should be called only once
		//----------------------------------------------------------------------
		if (m_fInitialized || m_Parent == null)
			return false;

		// m_Parent must be extended from the Container class
		//----------------------------------------------------------------------
		if (!(m_Parent instanceof Container))
			return false;

		// All position and sizes are in dialog logical units, so we use a
		// DialogLayout as our layout manager.
		//----------------------------------------------------------------------
//		m_Layout = new DialogLayout(m_Parent, 400, 200);
		m_Parent.resize(700, 600);
		m_Parent.setLayout(null);
		m_Parent.addNotify();

		// Control creation
		//----------------------------------------------------------------------
		Run = new Button ("Run");
		m_Parent.add(Run);
		Run.reshape(15, 212, 75, 28);

/*		SS = new Button ("Single Step");
		m_Parent.add(SS);
		SS.reshape(15, 244, 75, 28);

		SS2 = new Button ("Step Over");
		m_Parent.add(SS2);
		SS2.reshape(15, 276, 75, 28);

		SS3 = new Button ("Reset");
		m_Parent.add(SS3);
		SS3.reshape(15, 308, 75, 28);

		PC1 = new Label ("PC", Label.CENTER);
		m_Parent.add(PC1);
		PC1.reshape(0, 24, 34, 16);

		m_Parent.add(rpc);
		rpc.reshape(36, 22, 60, 24);

		m_Parent.add(rmb);
		rmb.reshape(36, 46, 60, 24);

		m_Parent.add(rac);
		rac.reshape(36, 70, 60, 24);

		m_Parent.add(rio);
		rio.reshape(36, 94, 60, 24);

		m_Parent.add(rma);
		rma.reshape(36, 118, 60, 24);

		MB1 = new Label ("MB", Label.CENTER);
		m_Parent.add(MB1);
		MB1.reshape(0, 48, 34, 16);

		AC1 = new Label ("AC", Label.CENTER);
		m_Parent.add(AC1);
		AC1.reshape(0, 72, 34, 16);

		IO1 = new Label ("IO", Label.CENTER);
		m_Parent.add(IO1);
		IO1.reshape(0, 96, 34, 16);

		MA1 = new Label ("MA", Label.CENTER);
		m_Parent.add(MA1);
		MA1.reshape(0, 120, 34, 16);

		m_Parent.add(rbpt);
		rbpt.reshape(36, 142, 60, 24);

		BP = new Label ("BP", Label.CENTER);
		m_Parent.add(BP);
		BP.reshape(0, 142, 34, 16);

		m_Parent.add(rss);
		rss.reshape(36, 164, 60, 24);

		SW = new Label ("SW", Label.CENTER);
		m_Parent.add(SW);
		SW.reshape(0, 164, 34, 16);
*/
		display = new Display();
		m_Parent.add(display);
		display.reshape(106, 6, 580, 580);

		m_fInitialized = true;
		return true;
	}
	
	void changed(int r) {
		switch(r) {
			case PC: rmb.setVal(memory[rpc.getVal()]); break;	
			case MA: rmb.setVal(memory[rma.getVal()]); break;
			case MB: memory[rma.getVal()]=rmb.getVal(); break;
		}	
	}
	
	void updatepanel() {
		int i = 0;
		int sense = 0;

		for(i=0; i<6; i++){
			if(pdp1.sense[i+1]){
				sense |= (1<<i);
			} else {
				sense &= ~(1<<i);
			}
		}

		rss.setVal(sense);
		rpc.setVal(pdp1.pc);
		rmb.setVal(memory[pdp1.pc]);
		rac.setVal(pdp1.ac);
		rio.setVal(pdp1.io);
		if(runpc < 0){
			Run.setLabel("Run");
		} else {
			Run.setLabel("Stop");
		}
	}
	
	public void updatemachine() {
		int i=0;
		int sense = rss.getVal();

		for(i=0; i<6; i++){
			pdp1.sense[i+1] = (((sense>>i)&1) == 1);
		}
		pdp1.pc=rpc.getVal();
		pdp1.ac=rac.getVal();
		pdp1.io=rio.getVal();
	}

	public void buttondispatch(String name){
		if (name.equals("Single Step")) {
			pdp1.step();
			runpc = -1;
		}
		
		if (name.equals("Step Over")) {
			runpc=pdp1.pc+1;
			while (pdp1.pc != runpc) {
				pdp1.step();
			};
			runpc = -1;
		}

		if (name.equals("Run")) {
			display.requestFocus();
			runpc=rbpt.getVal();
			if (pdp1.pc == runpc)
				pdp1.step();
			pdp1.wake(); // to get the monitor
		}
		
		if (name.equals("Stop")) {
			Run.setLabel("Run");
			runpc=-1;
		}

		if (name.equals("Reset")) {
			if(runpc >= 0){
				runpc = -1;
				pdp1.step(); // sync with the background thread
			}

			pdp1.pc = 4;
			pdp1.ac = 0;
			pdp1.io = 0;
		}

		updatepanel();
	}
}

class register extends TextField {

  int id, mask, radix;
  
  register(int id, int mask){
    super(); 
    this.id=id; this.mask=mask; this.radix = 8;
    setVal(0);
  }
  register(int id, int mask, int radix){
    super(); 
    this.id=id; this.mask=mask; this.radix = radix;
    setVal(0);
  }
  public boolean keyDown(Event e, int k){
    int i = getVal();
    if (k>=48&&k<(48+radix)) i=i*radix+k-48;
    if (k=='\n') i=0;
    if (k==Event.DOWN) i++;
    if (k==Event.UP) i--;
    i&=mask;	
    setVal(i);
    select(6,6);
    ((pdp1b)getParent()).panel.changed(id);
    return true;
  }
  
  int getVal() {
    return Integer.parseInt(getText(),radix);}
  
  void setVal(int n) {
	int temp = n;
	if(radix==8)temp += 01000000;
	if(radix==2)temp += 0100;
    setText(Integer.toString(temp,radix).substring(1));
  }
}

class Display extends Canvas {

	Image im[];
	int currentImage;
	int newImage;
	Graphics g[];
	int control;
	int width, height;

	public boolean keyDown(Event e, int k){
		if (k=='\'')control |= 040000;
		if (k==';') control |= 0100000;
		if (k=='k') control |= 0200000;
		if (k=='l') control |= 0400000;
		if (k=='f') control |= 01;
		if (k=='d') control |= 02;
		if (k=='a') control |= 04;
		if (k=='s') control |= 010;
		return true;
	}
	
	public boolean keyUp(Event e, int k){
		if (k=='\'')control &= ~040000;
		if (k==';') control &= ~0100000;
		if (k=='k') control &= ~0200000;
		if (k=='l') control &= ~0400000;
		if (k=='f') control &= ~01;
		if (k=='d') control &= ~02;
		if (k=='a') control &= ~04;
		if (k=='s') control &= ~010;
		return true;
	}
	
	void init(){
		int i = 0;
		width=size().width; height=size().height;
		im = new Image[2];
		g = new Graphics[2];
		for(i=0; i<2; i++){
			im[i]=createImage(width, height);
			g[i]=im[i].getGraphics();
			g[i].setColor(Color.black);
			g[i].fillRect(0, 0, width, height);
			g[i].setColor(Color.white);
		}
		currentImage = 0;
		newImage = 1;
		repaint();
	}
		
	void plot(int x, int y){
		x=(x)*width/0777777; y=(y)*height/0777777;
		g[newImage].fillRect(x,y,1,1);
	}		
		
	void nextframe(){
		int temp = currentImage;
		currentImage = newImage;
		newImage = temp;
		g[newImage].setColor(Color.black);
		g[newImage].fillRect(0, 0, width, height);
		g[newImage].setColor(Color.white);
		repaint();
	}

	public void update(Graphics g1){
		paint(g1);
	}
	
	public void paint(Graphics g1) {
	   if (im == null || im[currentImage] == null) {
		   g1.clearRect(0, 0, width, height);
		   return;
	   }
	   g1.drawImage(im[currentImage], 0, 0, this);
    }
}
