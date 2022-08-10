# gamevacious

gamevacious is mpvacious for video games (or any other desktop application).

It is designed to give you the ability to conveniently add screenshots and audio from games to your Anki cards.

## Requirements
* Windows 10
* Java
* Anki
* AnkiConnect

You should have fields in your Anki notes labeled "Image" and "SentAudio". These are where the screenshots and audio will be added. 

gamevacious does not include any OCR or text extraction tools at the moment. I recommend that you use other programs for these features, and run gamevacious alongside them. An example pathway for Japanese immersion would be:

OCR/Text hooker->Auto copy text to clipboard->Yomichan Search (clipboard monitor)->Add card to Anki->gamevacious

## Usage
### Before running
Install a virtual audio cable or similar input device that plays back audio from your speakers, and set it as your default microphone in the Windows settings. Create a config.ini file in the root directory of the application that specifies output_file_path and game_name. Ensure your build environment is set up to accept the two included library .jar files. Build the application and run it as a console program.
### While running
Add a card to your anki deck using Yomichan or some other tool. Take a screenshot ('c') and an audio recording ('s' and 'e'). Then, update the last added note in Anki ('m'). When you're finished, quit the application ('q').

## Default hotkeys
```
c - Take screenshot
s - Start audio recording
e - End audio recording
m - Send screenshot & audio recording to Anki
q - Quit program
```
## Sample config.ini file
output_file_path is where output files (audio recordings and screenshots) will be saved. This is not the Anki media directory.

game_name determines how output files will be named.
```
[General]
output_file_path=C:/Users/USERNAME/.../.../gamevacious/out/
game_name=Persona_4_Golden
```

## Tools used
* Java SE 17.0.1
* jnativehook-2.2.2
* gson-2.9.1

## Links
* https://github.com/FooSoft/anki-connect
* https://github.com/Ajatt-Tools/mpvacious
* https://github.com/kwhat/jnativehook
* https://github.com/google/gson
