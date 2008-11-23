/*
 * Bombus.java
 *
 * Created on 5.01.2005, 21:46
 *
 * Copyright (c) 2005-2007, Eugene Stahov (evgs), http://bombus-im.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * You can also redistribute and/or modify this program under the
 * terms of the Psi License, specified in the accompanied COPYING
 * file, as published by the Psi Project; either dated January 1st,
 * 2005, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/**
 *
 * @author Eugene Stahov
 */
package midlet;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import locale.SR;

import ui.*;

import Client.*;
import Info.Version;


/** Entry point class
 *
 * @author  Eugene Stahov
 * @version
 */
public class Bombus extends MIDlet implements Runnable{
    
    private Display display;    // The display for this MIDlet
    private boolean isRunning;
    private boolean isMinimized;
    StaticData sd;
    
    public static Image programIcon;
    public static Image splash;
    //IconTextList l;
    
    private static Bombus instance; 
        
    /** Bombus constructor. starts splashscreen */
    public Bombus() {
	instance=this; 
        display = Display.getDisplay(this);
        SplashScreen s= SplashScreen.getInstance();
        display.setCurrent(s);
        s.setProgress(/*SR.MS_LOADING*/ "Loading",3); // this message will not be localized
        sd=StaticData.getInstance();
    }
    
    /** Entry point  */
    public void startApp() {
        
//#ifdef DefaultConfiguration
        if (modules.Modules.modprobe(modules.TestModule.MODID)) {
            new modules.TestModule().helloWorld();
        }
//#endif        
        
        if (isRunning) {
	    hideApp(false);
            return;
        }
        
        isRunning=true;

        new Thread(this).start();
    }
    
    
    /**
     * Pause is a no-op since there are no background activities or
     * record stores that need to be closed.
     */
    public void pauseApp() { }

    public void run(){
        
        SplashScreen s= SplashScreen.getInstance();
        s.setProgress(5);
        
        ui.Colors.initColors();
        
        try {
            s.img=Image.createImage("/images/splash.png");
            
            if (Version.getPlatformName().startsWith("Nokia")) {
                splash=s.img;
                programIcon=Image.createImage("/_icon.png");
            }
            
            s.setProgress(Version.getNameVersion(),7);
        } catch (Exception e) {
            e.printStackTrace();
        }

        s.setProgress(10);
	Config cf=Config.getInstance();

	s.setProgress(17);
        boolean selAccount=( (cf.accountIndex<0) || s.keypressed!=0);
        if (selAccount) s.setProgress("Entering setup",20);

        s.setProgress(23);
        sd.roster=new Roster(display);
        
        if (!selAccount) {
            // connect whithout account select
	    boolean autologin=cf.autoLogin;
            selAccount=(Account.loadAccount(autologin)==null);
	    if (!autologin) s.close();
        }
        if (selAccount) { new AccountSelect(display, true); }
    }
    
    /**
     * Destroy must cleanup everything not handled by the garbage collector.
     * In this case there is nothing to cleanup.
     */
    public void destroyApp(boolean unconditional) {
    }

    public void hideApp(boolean hide) {
	if (hide) {
	    display.setCurrent(null);
	} else {
            if (isMinimized) {
                display.setCurrent(/*sd.roster*/ display.getCurrent());
            }
	}
        isMinimized=hide;
    }
    
    public static Bombus getInstance() {
        return instance;
    }
  
}
