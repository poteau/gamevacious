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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

// TODO: Boolean returns for all private functions that indicate whether the procedure was successful
public class AnkiUtils {

    // Class that represents a JSON object for AnkiConnect requests
    public class AnkiConnectRequest {
        String action;
        int version;
        Map<String, Object> params;

        public void setAction(String act) {
            action = act;
        }

        public void setVersion(int ver) {
            version = ver;
        }

        public void setParams(Map<String, Object> par) {
            params = par;
        }
    }

    // Class with fields to receive a JSON object from AnkiConnect
    public class AnkiConnectResponse {
        Object result; // is a List<?> in findNotes and notesInfo but is a String in storeMediaFile
        String error;
    }

    // Updates the last note that was added, adding a screenshot and a recording
    public static void updateLastNote(boolean overwrite, String ssFilename, String audioFilename, String pathName) {
        // TODO: have overwrite actually do something
        Gson gson = new Gson();
        AnkiUtils acu = new AnkiUtils();

        AnkiConnectRequest request = acu.new AnkiConnectRequest();
        AnkiConnectResponse response = acu.new AnkiConnectResponse();

        long lastID = getLastNoteID();

        if (lastID < minutesAgo(10)) {
            System.out.println("Couldn't find the target note.");
            return;
        }

        storeFile(ssFilename, pathName);
        storeFile(audioFilename, pathName);
        String ssFieldInput = String.format("<img alt=\"snapshot\" src=\"%s\">", ssFilename);
        String audioFieldInput = String.format("[sound:%s]", audioFilename);

        HashMap<String, Object> fields = new HashMap<String, Object>();
        fields.put("Image", ssFieldInput); // TODO: use config field name for ss and audio
        fields.put("SentAudio", audioFieldInput);
        HashMap<String, Object> note = new HashMap<String, Object>();
        note.put("id", lastID);
        note.put("fields", fields);

        request.setAction("updateNoteFields");
        request.setVersion(6);
        request.setParams(new HashMap<String, Object>() {
            {
                put("note", note);
            }
        });

        String execResponse = execute(request);
        response = gson.fromJson(execResponse, response.getClass());

        if (response.error != null) {
            System.out.println("Error updating note.");
        } else {
            System.out.println("Updated note!");
        }

    }

    // Copies the passed-in file to Anki's media folder
    private static void storeFile(String filename, String filePath) {
        Gson gson = new Gson();
        AnkiUtils acu = new AnkiUtils();

        AnkiConnectRequest request = acu.new AnkiConnectRequest();
        AnkiConnectResponse response = acu.new AnkiConnectResponse();

        request.setAction("storeMediaFile");
        request.setVersion(6);
        request.setParams(new HashMap<String, Object>() {
            {
                put("filename", filename);
                put("path", filePath + filename);
            }
        });

        String execResponse = execute(request);
        response = gson.fromJson(execResponse, response.getClass());

        if (response.error != null) {
            System.out.println("Error adding media.");
        }
    }

    // Gets the ID of the last note that was added
    private static long getLastNoteID() {
        Gson gson = new Gson();
        AnkiUtils acu = new AnkiUtils();

        AnkiConnectRequest request = acu.new AnkiConnectRequest();
        AnkiConnectResponse response = acu.new AnkiConnectResponse();

        request.setAction("findNotes");
        request.setVersion(6);
        request.setParams(new HashMap<String, Object>() {
            {
                put("query", "added:1"); // Shows notes that were added today
            }
        });

        String execResponse = execute(request);
        response = gson.fromJson(execResponse, response.getClass());

        List<Double> result = (List<Double>) response.result;

        if (result.isEmpty()) {
            System.out.println("Note not found.");
            return -1;
        }

        Collections.sort(result, Collections.reverseOrder());
        long lastNoteID = (long) ((double) result.get(0));
        System.out.println(result.get(0));
        System.out.println(lastNoteID);
        return lastNoteID;
    }

    // Sends an HTTP request to AnkiConnect by serializing the passed-in
    // AnkiConnectRequest
    private static String execute(AnkiConnectRequest request) {
        Gson gson = new Gson();
        String requestJSON = gson.toJson(request);
        String response = null;

        System.out.print("Requesting: ");
        System.out.println(requestJSON);

        HttpClient httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_2)
                .followRedirects(Redirect.NEVER)
                .build();
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8765/"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(requestJSON))
                .build();

        try {
            HttpResponse<String> httpResp = httpClient.send(httpReq, BodyHandlers.ofString());
            response = httpResp.body();

            System.out.print("Received: ");
            System.out.println(response);
        } catch (Exception exc) {
            System.out.println("Failed to connect to AnkiConnect."); // TODO: more detailed error handling here
        }

        return response;
    }

    // Returns the unix timestamp for m minutes ago
    private static double minutesAgo(int m) {
        return java.lang.System.currentTimeMillis() - 1000 * 60 * m;
    }

    // ---------- Unused Functions ----------

    // public static boolean isAnkiAlive() {
    // return false;
    // }

    // private static LinkedTreeMap<String, LinkedTreeMap> getNoteFields(long
    // note_id) {
    // Gson gson = new Gson();
    // AnkiConnectUtils acu = new AnkiConnectUtils();

    // AnkiConnectRequest request = acu.new AnkiConnectRequest();
    // AnkiConnectResponse response = acu.new AnkiConnectResponse();

    // ArrayList<Long> al = new ArrayList<Long>();
    // al.add(note_id);

    // request.setAction("notesInfo");
    // request.setVersion(6);
    // request.setParams(new HashMap<String, Object>() {
    // {
    // put("notes", al);
    // }
    // });

    // String execResponse = execute(request);
    // response = gson.fromJson(execResponse, response.getClass());
    // if (response.error == null) {
    // LinkedTreeMap<String, LinkedTreeMap> fields = ((LinkedTreeMap<String,
    // LinkedTreeMap>) ((List<?>) response.result)
    // .get(0)).get("fields");

    // fields.forEach((key, value) -> {
    // fields.remove("order");
    // });

    // return fields;
    // } else {
    // return null;
    // }

    // }

}