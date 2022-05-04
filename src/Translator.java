import Constants.Constants;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Translator {

    private static final String clientId = Constants.clientId;
    private static final String clientSecret = Constants.clientSecret;

    public static String translateTextToRussian(String text) {
        if (text.length() <= 1) return text;
        String detectedLanguage = detectLanguageNaver(text);
        if (detectedLanguage.equals("ru")) return text;
        String translatedText = "";
        translatedText = translateSplittedText(text.replace("\n", " \\n "), detectedLanguage)
                .replace("\\n", "\n");
        return translatedText;
    }

    private static String translateSplittedText(String text, String detectedLanguage) {
        if (text.length() <= 1) return text;
        if (detectedLanguage.equals("ko") || detectedLanguage.equals("ja")) {
            String fromNaver = translateNaver(detectedLanguage, "en", text);
            if (fromNaver != null) {
                String fromNaverToGoogle = translateGoogle("en", "ru", fromNaver);
                return fromNaverToGoogle;
            }
        }
        String fromGoogle = translateGoogle(detectedLanguage, "ru", text);
        if (fromGoogle.charAt(0) != '<')
            return fromGoogle;
        fromGoogle = translateGoogle("", "ru", text);
        return fromGoogle;
    }

    private static String translateGoogle(String langFrom, String langTo, String text)  {
        if (langFrom.equals(langTo)) return text;
        StringBuilder response = new StringBuilder();
        try {
            String urlStr = Constants.googleScriptUrl +
                    "?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                    "&target=" + langTo +
                    "&source=" + langFrom;
            URL url = new URL(urlStr);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private static String translateNaver(String langFrom, String langTo, String text) {
        if (langFrom.equals(langTo)) return text;
        String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
        text = URLEncoder.encode(text, StandardCharsets.UTF_8);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);

        String responseBody = postNaverTranslate(apiURL, requestHeaders, text, langFrom, langTo);
        //System.out.println(responseBody);
        if (responseBody.split("\"").length > 15)
            return responseBody.split("\"")[15];
        return null;
    }

    private static String detectLanguageNaver(String msg) {
        String query;
        query = URLEncoder.encode(msg, StandardCharsets.UTF_8);
        String apiURL = "https://openapi.naver.com/v1/papago/detectLangs";

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);

        String responseBody = postNaverDetect(apiURL, requestHeaders, query);
        return responseBody.split("\"")[3];
    }

    private static String postNaverTranslate(String apiUrl, Map<String, String> requestHeaders, String text, String langFrom, String langTo){
        HttpURLConnection con = connect(apiUrl);
        String postParams = "source=" + langFrom + "&target=" + langTo + "&text=" + text;
        try {
            con.setRequestMethod("POST");
            for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postParams.getBytes());
                wr.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 응답
                return readBody(con.getInputStream());
            } else {  // 에러 응답
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private static String postNaverDetect(String apiUrl, Map<String, String> requestHeaders, String text){
        HttpURLConnection con = connect(apiUrl);
        String postParams =  "query="  + text; //원본언어: 한국어 (ko) -> 목적언어: 영어 (en)
        try {
            con.setRequestMethod("POST");
            for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postParams.getBytes());
                wr.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 응답
                return readBody(con.getInputStream());
            } else {  // 에러 응답
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private static HttpURLConnection connect(String apiUrl){
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection)url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    private static String readBody(InputStream body){
        InputStreamReader streamReader = new InputStreamReader(body);
        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }
            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
        }
    }
}