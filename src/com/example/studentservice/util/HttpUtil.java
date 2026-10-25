package com.example.studentservice.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * HTTP 工具类 · 组长 A 维护, D 组员主要用
 * <p>
 * 严格 JDK 8 兼容, 使用 HttpURLConnection, 不依赖任何第三方 HTTP 库.
 * 提供两个核心方法: doGet (带 query 参数) 和 doPostJson.
 * 支持: Bearer Token 鉴权, 超时设置, 失败友好提示.
 */
public class HttpUtil {

    private static int CONNECT_TIMEOUT = 5000;
    private static int READ_TIMEOUT    = 15000;

    static {
        Properties props = new Properties();
        try (InputStream is = HttpUtil.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is != null) { props.load(is); }
        } catch (IOException ignored) {}
        try { CONNECT_TIMEOUT = Integer.parseInt(props.getProperty("http.connect.timeout", "5000")); } catch (NumberFormatException ignored) {}
        try { READ_TIMEOUT    = Integer.parseInt(props.getProperty("http.read.timeout",    "15000")); } catch (NumberFormatException ignored) {}
    }

    // ============== GET ==============

    /**
     * 发送 GET 请求 (无 query 参数, 无 header 可传)
     */
    public static String doGet(String url) throws IOException {
        return doGet(url, null, null);
    }

    /**
     * 发送 GET 请求
     * @param url      URL, 自带 http:// 或 https://
     * @param headers  自定义请求头 (可为 null), 例如 {"Authorization":"Bearer xxx"}
     * @return 响应体字符串
     */
    public static String doGet(String url, Map<String,String> headers) throws IOException {
        return doGet(url, null, headers);
    }

    /**
     * 发送 GET 请求 (完整签名)
     * @param url      base url, 如 "https://api.example.com/search"
     * @param params   query 参数 map (可为 null), 会自动拼到 url 上
     * @param headers  自定义请求头 (可为 null)
     */
    public static String doGet(String url, Map<String,String> params,
                               Map<String,String> headers) throws IOException {
        String fullUrl = url;
        if (params != null && !params.isEmpty()) {
            StringBuilder sb = new StringBuilder(url).append('?');
            for (Map.Entry<String,String> e : params.entrySet()) {
                sb.append(e.getKey()).append('=')
                  .append(e.getValue() == null ? "" : encode(e.getValue()))
                  .append('&');
            }
            fullUrl = sb.substring(0, sb.length() - 1);
        }
        System.out.println("[HttpUtil] GET " + fullUrl);

        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", "StudentCS-System/1.0");
            if (headers != null) {
                for (Map.Entry<String,String> h : headers.entrySet()) {
                    conn.setRequestProperty(h.getKey(), h.getValue());
                }
            }
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    // ============== POST ==============

    /** 发送 JSON POST (最常用, D 组员 AI 问答会大量用) */
    public static String doPostJson(String url, String jsonBody,
                                    Map<String,String> headers) throws IOException {
        return doPost(url, jsonBody, "application/json", headers);
    }

    /** 通用 POST */
    public static String doPost(String url, String body, String contentType,
                                Map<String,String> headers) throws IOException {
        System.out.println("[HttpUtil] POST " + url);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Content-Type", contentType + "; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "StudentCS-System/1.0");
            if (headers != null) {
                for (Map.Entry<String,String> h : headers.entrySet()) {
                    conn.setRequestProperty(h.getKey(), h.getValue());
                }
            }

            // 写请求体
            if (body != null) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    // ============== 内部工具 ==============

    private static String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream is;
        if (code >= 200 && code < 300) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
            if (is == null) {
                throw new IOException("HTTP " + code + ": " + conn.getResponseMessage());
            }
            // 非 2xx 也读一下, 方便调试
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (code >= 300) {
                System.err.println("[HttpUtil] HTTP " + code + " 响应: " + sb);
            }
            return sb.toString();
        }
    }

    /** URL 编码 (手写, 避免 URLEncoder 的空格转成 + 的问题) */
    private static String encode(String s) {
        try {
            // 简单使用 URLEncoder 但把 + 换成 %20
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}
