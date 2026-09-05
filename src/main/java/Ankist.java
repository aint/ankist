import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

private static final String LOCALE = "es-MX";
private static final String GEMINI_API_KEY = "";
private static final String GEMINI_FLASH_LITE_API = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent";
private static final String GEMINI_FLASH_LITE_IMAGE_API = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-image:generateContent";
private static final String GOOGLE_TTS_URL = "http://translate.google.com/translate_tts?tl=%s&client=tw-ob&q=%s";

private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

private record Translation(String spanish, String english) {}

void main() {
    var word = "desafortunadamente";
    tts(word);
    image(word);
    var example = example(word);
    tts(example.spanish);
}

private void tts(String text) {
    final var q = URLEncoder.encode(text, StandardCharsets.UTF_8);
    final var url = GOOGLE_TTS_URL.formatted(LOCALE, q);
    var request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();

    try {
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new RuntimeException("TTS request for '%s' failed with status %d".formatted(text, response.statusCode()));
        }

        Files.write(Paths.get(text.replace(" ", "_") + ".mp3"), response.body());
    } catch (InterruptedException | IOException e) {
        throw new RuntimeException("Failed to TTS audio for '" + text + "'", e);
    }
}

private Translation example(String text) {
    final var jsonPayload = """
        {
          "systemInstruction": {
            "parts": [{ "text": "You are a Spanish lexicographer." }]
          },
          "contents": [
            {
              "parts": [{ "text": "Give an example sentence for '%s' using a simple language, but more than 4 words,
              and an English translation of that sentence in the following json format:
              {'spanish': 'example', 'english': 'translation'}" }]
            }
          ],
          "generationConfig": {
            "responseMimeType": "application/json"
          }
        }
        """.formatted(text);
    final var endpoint = URI.create(GEMINI_FLASH_LITE_API);
    var responseBody = postJson(endpoint, jsonPayload);

    try {
        var rawExampleJson = new JSONObject(responseBody)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        var jsonObject = new JSONObject(rawExampleJson);

        var translation = new Translation(
                jsonObject.getString("spanish"),
                jsonObject.getString("english")
        );

        IO.println(translation.english);
        IO.println(translation.spanish);

        return translation;
    } catch (JSONException e) {
        throw new RuntimeException("Unexpected Gemini response shape for '%s': %s".formatted(text, responseBody), e);
    }
}

private void image(String text) {
    final var jsonPayload = """
     {
       "contents": [
         {
           "parts": [
             {
               "text": "Create a memorable mnemonic image to help me memorize the following Spanish word: %s"
             }
           ]
         }
       ],
       "generationConfig": {
         "responseModalities": ["TEXT", "IMAGE"],
         "mediaResolution": "MEDIA_RESOLUTION_LOW",
         "imageConfig": {
            "aspectRatio": "1:1"
         }
       }
     }
     """.formatted(text);
    final var endpoint = URI.create(GEMINI_FLASH_LITE_IMAGE_API);
    var responseBody = postJson(endpoint, jsonPayload);

    try {
        var base64Data = new JSONObject(responseBody)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getJSONObject("inlineData")
                .getString("data");

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        Files.write(Paths.get(text + ".png"), imageBytes);
        IO.println("Successfully saved image to " + text + ".png");
    } catch (JSONException e) {
        throw new RuntimeException("Unexpected Gemini response shape for '%s': %s".formatted(text, responseBody), e);
    } catch (IOException e) {
        throw new RuntimeException("Failed to save image for '" + text + "'", e);
    }
}

private String postJson(URI endpoint, String jsonPayload) {
    var request = HttpRequest.newBuilder()
            .uri(endpoint)
            .header("x-goog-api-key", GEMINI_API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

    try {
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Request to %s failed with status %d: %s".formatted(endpoint, response.statusCode(), response.body()));
        }

        return response.body();
    } catch (IOException | InterruptedException e) {
        throw new RuntimeException("Failed to call " + endpoint, e);
    }
}
