/* 
Copyright (C) 2022 poteau

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class Gamevacious implements NativeKeyListener {
    Thread recordingThread;
    private final TargetDataLine tdl = initAudioLine();
    private BufferedImage img = null;
    private boolean RECORDING_STATE = false;

    private static String outPath = null; // Is read from config
    private static String gameName = null; // Is read from config
    private String audioFilename = null;
    private String ssFilename = null;

    public static void main(String[] args) throws Exception {
        initConfig();
        Gamevacious sg = new Gamevacious(); // Runs initAudioLine() during member initialization
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(sg);
    }

    private static void initConfig() {
        Config cfg = new Config();
        outPath = cfg.outPath;
        gameName = cfg.gameName;
    }

    // TODO: determine hotkeys from config file
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // TODO: Add a hotkey that resets the filename variables to null
        // TODO: Allow for picture only or audio only to be sent

        // Intended inputs
        if (e.getKeyChar() == 's' && RECORDING_STATE == false) {
            System.out.println("Starting recording.");
            startRecordingFromLine();
            RECORDING_STATE = true;
        } else if (e.getKeyChar() == 'e' && RECORDING_STATE == true) {
            System.out.println("Ending recording.");
            stopRecordingFromLine();
            RECORDING_STATE = false;
        } else if (e.getKeyChar() == 'c') {
            System.out.println("Taking a screenshot.");
            imageGrab();
        } else if (e.getKeyChar() == 'm' && audioFilename != null && ssFilename != null) {
            System.out.println("Sending data to anki...");
            AnkiUtils.updateLastNote(false, ssFilename, audioFilename, outPath);
        } else if (e.getKeyChar() == 'M' && audioFilename != null && ssFilename != null) {
            // System.out.println("Sending card to anki... (overwrite)");
            // AnkiConnectUtils.updateLastNote(true, ssFilename, audioFilename, outPath);
        } else if (e.getKeyChar() == 'q') {
            System.out.println("Quitting program.");
            System.exit(0);
        }
        // Unintended inputs
        else if ((e.getKeyChar() == 'm' || e.getKeyChar() == 'M') && audioFilename == null) {
            System.out.println("Need an audio file before updating note.");
        } else if ((e.getKeyChar() == 'm' || e.getKeyChar() == 'M') && ssFilename == null) {
            System.out.println("Need a screenshot file before updating note.");
        } else if (e.getKeyChar() == 's' && RECORDING_STATE == true) {
            System.out.println("Already recording.");
        } else if (e.getKeyChar() == 'e' && RECORDING_STATE == false) {
            System.out.println("Not yet recording.");
        }
    }

    void startRecordingFromLine() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm_ssSSS").format(new Date());
        audioFilename = gameName + "-" + timestamp + ".wav";
        try {
            tdl.open();
        } catch (LineUnavailableException exc) {
            exc.printStackTrace();
        }
        tdl.start(); // Just prepares the line, doesn't start writing anything

        recordingThread = new Thread() {
            @Override
            public void run() {
                try {
                    AudioSystem.write(new AudioInputStream(tdl), AudioFileFormat.Type.WAVE,
                            new File("out/" + audioFilename));
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        };
        recordingThread.start();
        return;
    }

    void stopRecordingFromLine() {
        tdl.stop(); // This is what actually causes the AudioSystem.write() call in the thread to
                    // stop executing
        tdl.close();
        // recordingThread.join()
        // TODO: Block and wait for thread completion just to be safe, then convert the
        // captured wav file to mp3 here
    }

    /*
     * Chooses a microphone to grab audio from when recording.
     * Intended for use with a virtual audio cable that acts
     * as a microphone but actually repeats speaker output.
     */
    static TargetDataLine initAudioLine() {
        // Prepares audio functionality. Gets desired data line and returns it.

        // Gets list of system mixers
        Mixer selectedMixer = null;
        Mixer.Info[] allMixers = AudioSystem.getMixerInfo();
        ArrayList<Mixer.Info> outputMixers = new ArrayList<Mixer.Info>();
        // Filter capture devices only
        for (int i = 0; i < allMixers.length; i++) {
            // System.out.println(i+1 + ". " + allMixers[i].getName() + " | " +
            // allMixers[i].getDescription());

            // Filters so that only input devices appear
            if (allMixers[i].getDescription().equals("Direct Audio Device: DirectSound Capture"))
                outputMixers.add(allMixers[i]);
        }

        // Prompt user to select device
        // First option seems to always be the Windows "default device"

        // System.out.println("List of input devices: ");
        // for (int i = 0; i < outputMixers.size(); i++) {
        // System.out.println(i + 1 + ". " + outputMixers.get(i).getName());
        // }
        // if (outputMixers.size() == 0) {
        // System.out.println("No input devices detected!");
        // return null;
        // }
        // Scanner in = new Scanner(System.in);
        // System.out.print("Select output device: ");
        // int input = in.nextInt();
        // in.close();
        // System.out.println("You selected device " + outputMixers.get(input -
        // 1).getName());
        // selectedMixer = AudioSystem.getMixer(outputMixers.get(input - 1));

        // Alternative to above commented code, just picks the default device (position
        // 0)
        System.out.println("Automatically selected device " +
                outputMixers.get(0).getName());
        selectedMixer = AudioSystem.getMixer(outputMixers.get(0));

        Line.Info[] targets = selectedMixer.getTargetLineInfo();

        if (targets.length == 0) {
            return null;
        }

        System.out.println("Selecting a target line...");

        try {
            // Blindly picks the first target line. Potentially bad
            final TargetDataLine tline = (TargetDataLine) AudioSystem.getLine(targets[0]);
            System.out.println("Automatically selected target line " + targets[0].toString());
            return tline;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Takes a screenshot of the current active window.
    // TODO: make this cross-platform and less hacky
    void imageGrab() {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        img = null;
        Robot bot = null;
        try {
            bot = new Robot();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        cb.setContents(new StringSelection("gamevacious"), null);

        // Presses a Win10-specific key shortcut, which captures the current window as
        // an image and copies it to the clipboard.
        if (bot != null) {
            bot.keyPress(KeyEvent.VK_ALT);
            bot.keyPress(KeyEvent.VK_PRINTSCREEN);
            bot.keyRelease(KeyEvent.VK_PRINTSCREEN);
            bot.keyRelease(KeyEvent.VK_ALT);
        }

        // Waits for image to appear in clipboard. 1 second wait between attempts
        int i = 0;
        while (!cb.isDataFlavorAvailable(DataFlavor.imageFlavor)) { // TODO: use clipboard flavor listener instead
            try {
                Thread.sleep(1000);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            if (i >= 4) {
                System.out.println("Unexpected: Image could not be found in clipboard!");
                return;
            }
            i++;
        }

        // Image is available, grabs it from the clipboard
        try {
            img = (BufferedImage) cb.getData(DataFlavor.imageFlavor);
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        // TODO: use jpeg or webp
        // Gives image a filename and writes it to disk.
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm_ssSSS").format(new Date());
        ssFilename = gameName + "-" + timestamp + ".png";
        File outputfile = new File("out/" + ssFilename);
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }

}