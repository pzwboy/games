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

import java.util.Vector;
import FrontPanel;
import pdp1b;

public class pdp1 {
  static int ac=0, io=0, pc=0, y, ib, ov=0; 
  static int[] memory= new int[010000];
  static boolean[] flag=new boolean[7];
  static boolean[] sense=new boolean[7];
  static FrontPanel panel;
  static boolean standAlone = false;
  static pdp1 sync = new pdp1();
  static int frame=0, delay=56;
  static long lastFrameTime=System.currentTimeMillis();

  static final int 
    AND=001, IOR=002, XOR=003, XCT=004, CALJDA=007,
    LAC=010, LIO=011, DAC=012, DAP=013, DIO=015, DZM=016,
    ADD=020, SUB=021, IDX=022, ISP=023, SAD=024, SAS=025, MUS=026, DIS=027,
    JMP=030, JSP=031, SKP=032, SFT=033, LAW=034, IOT=035, OPR=037;

  	static void wake() {
		sync.wake1();
	}

	synchronized void wake1() {
		notifyAll();
	}

	static void waitFor() throws Exception {
		sync.wait1();
	}

	synchronized void wait1() throws Exception {
		wait();
	}

	static void step() {
		if(pc==02051) nextframe();
		sync.step1(memory[pc++]);
	}

	synchronized void step1(int md) {
    y=md&07777; ib=(md>>12)&1;

    switch(md>>13) {
    case AND: ea(); ac&=memory[y]; break;
    case IOR: ea(); ac|=memory[y]; break;
    case XOR: ea(); ac^=memory[y]; break;
	 case XCT: ea(); step1(memory[y]); break;
    case CALJDA: int target=(ib==0)?64:y;
      memory[target]=ac;
      ac=(ov<<17)+pc;
      pc=target+1;
      break;
    case LAC: ea(); ac=memory[y]; break;
    case LIO: ea(); io=memory[y]; break;
    case DAC: ea(); memory[y]=ac; break;
    case DAP: ea(); memory[y]=(memory[y]&0770000)+(ac&07777); break;
    case DIO: ea(); memory[y]=io; break;
    case DZM: ea(); memory[y]=0; break;
    case ADD:	ea();
      ac=ac+memory[y];
      ov=ac>>18;
      ac=(ac+ov)&0777777;
      if (ac==0777777) ac=0;
      break;
    case SUB:	ea();
		 boolean diffsigns=((ac>>17)^(memory[y]>>17))==1;
      ac=ac+(memory[y]^0777777);
      ac=(ac+(ac>>18))&0777777;
      if (ac==0777777) ac=0;
		 if (diffsigns&&(memory[y]>>17==ac>>17)) ov=1;
      break;
    case IDX:	ea(); ac=memory[y]+1; 
      if(ac==0777777) ac=0;
      memory[y]=ac;
      break;
    case ISP: 	ea(); ac=memory[y]+1; 
      if(ac==0777777) ac=0;
      memory[y]=ac;
      if((ac&0400000)==0) pc++;
      break;
    case SAD: ea(); if(ac!=memory[y]) pc++; break;
    case SAS: ea(); if(ac==memory[y]) pc++; break;
    case MUS: ea();      
		if ((io&1)==1){
			ac=ac+memory[y];
      	ac=(ac+(ac>>18))&0777777;
      	if (ac==0777777) ac=0;}
		io=(io>>1|ac<<17)&0777777;
		ac>>=1;
      break;
    case DIS: ea();
		int acl=ac>>17;
		ac=(ac<<1|io>>17)&0777777;
		io=((io<<1|acl)&0777777)^1;
		if ((io&1)==1){
		   ac=ac+(memory[y]^0777777);
      	ac=(ac+(ac>>18))&0777777;}
		else {
			ac=ac+1+memory[y];
      	ac=(ac+(ac>>18))&0777777;}
    	if (ac==0777777) ac=0;
      break;
    case JMP: ea(); pc=y; break;
    case JSP: ea(); ac=(ov<<17)+pc; pc=y; break;
    case SKP: 	boolean cond =
		  (((y&0100)==0100)&&(ac==0)) ||
		  (((y&0200)==0200)&&(ac>>17==0)) ||
		  (((y&0400)==0400)&&(ac>>17==1)) ||
		  (((y&01000)==01000)&&(ov==0)) ||
		  (((y&02000)==02000)&&(io>>17==0))||
		  (((y&7)!=0)&&!flag[y&7])||
		  (((y&070)!=0)&&!sense[(y&070)>>3])||
		  ((y&070)==010);
    if (ib==0) {if (cond) pc++;}
    else {if (!cond) pc++;}
	 if ((y&01000)==01000) ov=0;
    break;
    case SFT:	
      int nshift=0, mask=md&0777;
      while (mask!=0) {nshift+=mask&1; mask=mask>>1;}
      switch((md>>9)&017){
      case 1: for(int i=0;i<nshift;i++) ac=(ac<<1|ac>>17)&0777777; break;
      case 2: for(int i=0;i<nshift;i++) io=(io<<1|io>>17)&0777777; break;
      case 3:	for(int i=0;i<nshift;i++) 
			{long both=((long)ac)<<19|((long)io)<<1|((long)ac)>>17;
			ac=(int)(both>>18&0777777); 
			io=(int)(both&0777777);}
      	break;
      case 5: for(int i=0;i<nshift;i++) 
			ac=((ac<<1|ac>>17)&0377777)+(ac&0400000);
      	break;
      case 6: for(int i=0;i<nshift;i++) 
			io=((io<<1|io>>17)&0377777)+(io&0400000);
      	break;
      case 7:	for(int i=0;i<nshift;i++) 
			{long both=((long)ac)<<19|((long)io)<<1|((long)ac)>>17;
			both=(both&0377777777777L)+(((long)ac&0400000L)<<18);
			ac=(int)(both>>18); 
			io=(int)(both&0777777);}
      	break;
      case 9: for(int i=0;i<nshift;i++) ac=(ac>>1|ac<<17)&0777777; break;
      case 10: for(int i=0;i<nshift;i++) io=(io>>1|io<<17)&0777777; break;
      case 11: for(int i=0;i<nshift;i++) 
			{long both=((long)ac)<<17|((long)io)>>1|((long)io)<<35;
			ac=(int)(both>>18&0777777); 
			io=(int)(both&0777777);}
      	break;
      case 13: for(int i=0;i<nshift;i++) ac=(ac>>1)+(ac&0400000); break;
      case 14: for(int i=0;i<nshift;i++) io=(io>>1)+(io&0400000); break;
      case 15: for(int i=0;i<nshift;i++) 
			{long both=((long)ac)<<17|((long)io)>>1|
	   	((long)(ac&0400000))<<18;
			ac=(int)(both>>18); 
			io=(int)(both&0777777);}
      	break;
      default:	System.out.println("Undefined shift: "+os(md)
				   +" at "+os(pc-1));
      Runtime.getRuntime().exit(0);
      }	break;
    case LAW: 
		ac=(ib==0)?y:y^0777777; break;
    case IOT: 
		if ((y&077)==7) {dpy();break;};
		if ((y&077)==011) {io = panel.display.control; break;}
		break;
    case OPR:	if((y&0200)==0200) ac=0;
      if((y&04000)==04000) io=0;
      if((y&01000)==01000) ac^=0777777;
		if((y&0400)==0400){
		  panel.runpc = -1;
	  }
      int nflag=y&7; 
      if (nflag<2) break;
      boolean state=(y&010)==010;
      if (nflag==7) {
		for (int i=2;i<7;i++) flag[i]=state;
		break;
      }
      flag[nflag]=state;
      break;
    default:	System.out.println("Undefined instruction: "+os(md)
				   +" at "+os(pc-1));
    Runtime.getRuntime().exit(0);
    }
  }

  void ea() {
    while(true) {if (ib==0) return;
    ib=(memory[y]>>12)&1;
    y=memory[y]&07777;
    }
  }
    
  static String os(int n) {
    return Integer.toString(n+01000000,8).substring(1);
  }
  
  static void updatepanel(){
	  sync.updatepanel1();
  }

  synchronized void updatepanel1 () {
	panel.updatepanel();
  }

	static void dpy(){
		int x=(ac+0400000)&0777777;
		int y=(io+0400000)&0777777;
		panel.display.plot(x,y);
	}
	
	static void nextframe(){
		frame++;
		if ((frame&1)!=0) return; 
		panel.display.nextframe();
		long now=System.currentTimeMillis();
		if ((now-lastFrameTime)<3*delay) lastFrameTime+=delay; 
		else lastFrameTime=now;
   		while (System.currentTimeMillis() < lastFrameTime) {Thread.yield();};
	}
}


