package com.hdcookbook.gunbunny.util;

import java.net.URL;

import javax.media.Manager;
import javax.media.Player;

import org.bluray.net.BDLocator;
import org.davic.media.MediaLocator;
import org.havi.ui.HSound;

import com.hdcookbook.gunbunny.BaseXlet;

/**
 * 
 * @author Shant Mardigian
 * @version April 16, 2007
 *
 */
public class SoundUtil {

    public static void playSoundFromAUXATA(int soundId) {
        try {
            String id = soundId < 10?"0"+soundId:""+soundId;
            String url = "bd://SOUND:" + id;
            Player player = Manager.createPlayer(new MediaLocator(new BDLocator(url)));
            player.start();
        }
        catch(Throwable thr) {
            thr.printStackTrace();
        }
    }    
}
