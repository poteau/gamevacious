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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Config {
    String outPath;
    String gameName;

    public Config() {
        String configPath = System.getProperty("user.dir"); // C:\Users\USERNAME\...\...\gamevacious
        configPath = configPath + '\\' + "config.ini";

        try {
            List<String> lines = Files.readAllLines(Paths.get(configPath));
            for (String line : lines) {
                System.out.println(line);

                int ind = line.indexOf("=");
                if (ind != -1) {
                    String varName = line.substring(0, ind);
                    String value = line.substring(ind + 1, line.length());

                    if (varName.equals("output_file_path")) { // TODO: Create a more extensible solution, probably using
                                                              // a map
                        outPath = value;
                    } else if (varName.equals("game_name")) {
                        gameName = value;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
