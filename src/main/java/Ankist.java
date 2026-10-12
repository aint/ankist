import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

private static final String LOCALE = "es-MX";
private static final String GEMINI_API_KEY = "";
private static final String GEMINI_GENERATE_CONTENT_API = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
private static final String GEMINI_FLASH_LITE_LATEST = "gemini-flash-lite-latest";
private static final String GEMINI_FLASH_LITE_IMAGE_3_1 = "gemini-3.1-flash-lite-image";
private static final String GOOGLE_TTS_URL = "http://translate.google.com/translate_tts?tl=%s&client=tw-ob&q=%s";
private static final Path OUTPUT_DIR = Paths.get("output");

private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

private record Translation(String meaning, String spanish, String english) {}

void main() throws IOException {
    Files.createDirectories(OUTPUT_DIR);

    var word = "bobo";
    var wordAudio = tts(word);
    var wordImage = image(word);
    var example = example(word);
    var sentenceAudio = tts(example.spanish());
}

private Path tts(String text) {
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

        var audioPath = OUTPUT_DIR.resolve(text.replace(" ", "_") + ".mp3");
        Files.write(audioPath, response.body());
        return audioPath;
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
    final var endpoint = URI.create(GEMINI_GENERATE_CONTENT_API.formatted(GEMINI_FLASH_LITE_LATEST));
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

private Path image(String text) {
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
    final var endpoint = URI.create(GEMINI_GENERATE_CONTENT_API.formatted(GEMINI_FLASH_LITE_IMAGE_3_1));
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
        var imagePath = OUTPUT_DIR.resolve(text + ".png");
        Files.write(imagePath, imageBytes);
        IO.println("Successfully saved image to " + imagePath);
        return imagePath;
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
