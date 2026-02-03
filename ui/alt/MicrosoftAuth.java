package nuclear.ui.alt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import nuclear.control.Manager;
import nuclear.utils.IMinecraft;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sun.net.httpserver.HttpServer;

public class MicrosoftAuth {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final String SCOPE = "XboxLive.signin offline_access";

    public static void login() {
        EXECUTOR.submit(() -> {
            try {
                // Генерируем state и code_verifier для PKCE
                String state = generateRandomString(32);
                String codeVerifier = generateRandomString(128);
                String codeChallenge = generateCodeChallenge(codeVerifier);

                // Открываем браузер для авторизации
                String authUrl = String.format(
                        "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?" +
                                "client_id=%s&" +
                                "response_type=code&" +
                                "redirect_uri=%s&" +
                                "response_mode=query&" +
                                "scope=%s&" +
                                "state=%s&" +
                                "code_challenge=%s&" +
                                "code_challenge_method=S256",
                        URLEncoder.encode(CLIENT_ID, "UTF-8"),
                        URLEncoder.encode(REDIRECT_URI, "UTF-8"),
                        URLEncoder.encode(SCOPE, "UTF-8"),
                        URLEncoder.encode(state, "UTF-8"),
                        URLEncoder.encode(codeChallenge, "UTF-8")
                );

                // Запускаем локальный сервер для получения callback
                int port = findFreePort();
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                CompletableFuture<String> codeFuture = new CompletableFuture<>();

                server.createContext("/callback", exchange -> {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        String code = extractCodeFromQuery(query);

                        if (code != null) {
                            codeFuture.complete(code);
                            String response = "<html><body><h1>Авторизация успешна!</h1><p>Вы можете закрыть это окно.</p></body></html>";
                            exchange.sendResponseHeaders(200, response.getBytes().length);
                            exchange.getResponseBody().write(response.getBytes());
                        } else {
                            String response = "<html><body><h1>Ошибка авторизации</h1></body></html>";
                            exchange.sendResponseHeaders(400, response.getBytes().length);
                            exchange.getResponseBody().write(response.getBytes());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        exchange.close();
                    }
                });

                server.setExecutor(EXECUTOR);
                server.start();

                // Обновляем redirect URI с локальным портом
                String localRedirectUri = "http://localhost:" + port + "/callback";
                authUrl = String.format(
                        "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?" +
                                "client_id=%s&" +
                                "response_type=code&" +
                                "redirect_uri=%s&" +
                                "response_mode=query&" +
                                "scope=%s&" +
                                "state=%s&" +
                                "code_challenge=%s&" +
                                "code_challenge_method=S256",
                        URLEncoder.encode(CLIENT_ID, "UTF-8"),
                        URLEncoder.encode(localRedirectUri, "UTF-8"),
                        URLEncoder.encode(SCOPE, "UTF-8"),
                        URLEncoder.encode(state, "UTF-8"),
                        URLEncoder.encode(codeChallenge, "UTF-8")
                );

                // Открываем браузер
                openBrowser(authUrl);

                // Ждем получения кода (таймаут 5 минут)
                String authCode = codeFuture.get();

                // Обменяем код на токен
                String accessToken = exchangeCodeForToken(authCode, codeVerifier, localRedirectUri);

                // Получаем Xbox Live токен
                String xboxToken = getXboxToken(accessToken);

                // Получаем XSTS токен
                String[] xstsTokens = getXSTSToken(xboxToken);
                String xstsToken = xstsTokens[0];
                String userHash = xstsTokens[1];

                // Получаем Minecraft токен
                String minecraftToken = getMinecraftToken(xstsToken, userHash);

                // Получаем GameProfile
                GameProfile profile = getGameProfile(minecraftToken);

                // Создаем сессию
                Minecraft.getInstance().execute(() -> {
                    IMinecraft.mc.session = new Session(profile.getName(), profile.getId().toString(), minecraftToken, "msa");

                    // Добавляем аккаунт в список, если его там нет
                    boolean exists = false;
                    for (Account account : Manager.ALT.accounts) {
                        if (account.accountName.equalsIgnoreCase(profile.getName())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        Account newAccount = new Account(profile.getName());
                        Manager.ALT.accounts.add(newAccount);
                        AltConfig.updateFile();
                    }
                });

                server.stop(0);
            } catch (Exception e) {
                e.printStackTrace();
                Minecraft.getInstance().execute(() -> {
                    // Можно показать уведомление об ошибке
                });
            }
        });
    }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    private static String generateCodeChallenge(String codeVerifier) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void openBrowser(String url) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();

        if (os.contains("win")) {
            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
        } else if (os.contains("mac")) {
            rt.exec("open " + url);
        } else {
            rt.exec("xdg-open " + url);
        }
    }

    private static String extractCodeFromQuery(String query) {
        if (query == null) return null;
        Pattern pattern = Pattern.compile("code=([^&]+)");
        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String exchangeCodeForToken(String code, String codeVerifier, String redirectUri) throws Exception {
        String url = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
        String params = String.format(
                "client_id=%s&" +
                        "code=%s&" +
                        "redirect_uri=%s&" +
                        "grant_type=authorization_code&" +
                        "code_verifier=%s",
                URLEncoder.encode(CLIENT_ID, "UTF-8"),
                URLEncoder.encode(code, "UTF-8"),
                URLEncoder.encode(redirectUri, "UTF-8"),
                URLEncoder.encode(codeVerifier, "UTF-8")
        );

        return makeHttpRequest(url, params, "application/x-www-form-urlencoded");
    }

    private static String getXboxToken(String accessToken) throws Exception {
        String url = "https://user.auth.xboxlive.com/user/authenticate";
        JsonObject request = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + accessToken);
        request.add("Properties", properties);
        request.addProperty("RelyingParty", "http://auth.xboxlive.com");
        request.addProperty("TokenType", "JWT");

        String response = makeHttpRequest(url, request.toString(), "application/json");
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        return json.get("Token").getAsString();
    }

    private static String[] getXSTSToken(String xboxToken) throws Exception {
        String url = "https://xsts.auth.xboxlive.com/xsts/authorize";
        JsonObject request = new JsonObject();
        JsonObject properties = new JsonObject();
        com.google.gson.JsonArray userTokens = new com.google.gson.JsonArray();
        userTokens.add(xboxToken);
        properties.add("UserTokens", userTokens);
        request.add("Properties", properties);
        request.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        request.addProperty("TokenType", "JWT");

        String response = makeHttpRequest(url, request.toString(), "application/json");
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        String token = json.get("Token").getAsString();
        String userHash = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
        return new String[]{token, userHash};
    }

    private static String getMinecraftToken(String xstsToken, String userHash) throws Exception {
        String url = "https://api.minecraftservices.com/authentication/login_with_xbox";
        JsonObject request = new JsonObject();
        request.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);

        String response = makeHttpRequest(url, request.toString(), "application/json");
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        return json.get("access_token").getAsString();
    }

    private static GameProfile getGameProfile(String minecraftToken) throws Exception {
        String url = "https://api.minecraftservices.com/minecraft/profile";
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + minecraftToken);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
            String name = json.get("name").getAsString();
            String id = json.get("id").getAsString();
            UUID uuid = UUID.fromString(id.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));

            return new GameProfile(uuid, name);
        }
    }

    private static String makeHttpRequest(String urlString, String data, String contentType) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(data.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        InputStream inputStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}

