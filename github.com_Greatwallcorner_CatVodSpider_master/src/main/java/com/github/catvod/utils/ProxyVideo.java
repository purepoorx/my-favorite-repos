package com.github.catvod.utils;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import java.util.Map;

import com.github.catvod.spider.Proxy;
import io.ktor.http.HttpStatusCode;
import okhttp3.Response;

public class ProxyVideo {

    public static String buildCommonProxyUrl(String url, Map<String, String> headers){
        return Proxy.getProxyUrl()+"?do=proxy&url="+Utils.base64Encode(url)+"&header="+Utils.base64Encode(Json.toJson(headers));
    }

    public static Response proxy(String url, Map<String, String> headers) throws Exception {
        SpiderDebug.log("proxy url："+ url + " headers" + Json.toJson(headers));
        return OkHttp.newCall(url, headers);
    }

    public static class ProxyRespBuilder{
        public static Object[] redirect(String url){
            return new Object[]{HttpStatusCode.Companion.getFound().getValue(), "text/plain", url};
        }

        public static Object[] response(Response response){
            return new Object[]{response};
        }
    }

//    public static NanoHTTPD.Response proxy1(String url, Map<String, String> headers) throws Exception {
//        Response response = OkHttp.newCall(url, headers);
//        String contentType = response.headers().get("Content-Type");
//        String hContentLength = response.headers().get("Content-Length");
//        String contentDisposition = response.headers().get("Content-Disposition");
//        long contentLength = hContentLength != null ? Long.parseLong(hContentLength) : 0;
//        if (contentDisposition != null) contentType = getMimeType(contentDisposition);
//        NanoHTTPD.Response resp = newFixedLengthResponse(Status.PARTIAL_CONTENT, contentType, response.body().byteStream(), contentLength);
//        for (String key : response.headers().names()) resp.addHeader(key, response.headers().get(key));
//        return resp;
//    }
    private static String getMimeType(String contentDisposition) {
        if (contentDisposition.endsWith(".mp4")) {
            return "video/mp4";
        } else if (contentDisposition.endsWith(".webm")) {
            return "video/webm";
        } else if (contentDisposition.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (contentDisposition.endsWith(".wmv")) {
            return "video/x-ms-wmv";
        } else if (contentDisposition.endsWith(".flv")) {
            return "video/x-flv";
        } else if (contentDisposition.endsWith(".mov")) {
            return "video/quicktime";
        } else if (contentDisposition.endsWith(".mkv")) {
            return "video/x-matroska";
        } else if (contentDisposition.endsWith(".mpeg")) {
            return "video/mpeg";
        } else if (contentDisposition.endsWith(".3gp")) {
            return "video/3gpp";
        } else if (contentDisposition.endsWith(".ts")) {
            return "video/MP2T";
        } else if (contentDisposition.endsWith(".mp3")) {
            return "audio/mp3";
        } else if (contentDisposition.endsWith(".wav")) {
            return "audio/wav";
        } else if (contentDisposition.endsWith(".aac")) {
            return "audio/aac";
        } else {
            return null;
        }
    }
}
