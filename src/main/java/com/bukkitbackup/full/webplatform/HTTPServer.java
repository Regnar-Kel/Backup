package com.bukkitbackup.full.webplatform;

import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.utils.LogUtils;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.bukkit.plugin.Plugin;

public final class HTTPServer implements Runnable {

    private int serverPort;
    private Socket socket;
    private Plugin plugin;
    private Strings strings;

    // @TODO Need to initalize variables here.
    public HTTPServer(Plugin plugin, Settings settings, Strings strings) {

        this.plugin = plugin;
        this.strings = strings;

        // Port setting.
        serverPort = settings.getIntProperty("httpserver-port", 8765);

    }

    public void run() {

        // Server Socket initalize.
        ServerSocket serverSocket = null;

        // Attempt to bind to port.
        try {
            LogUtils.sendLog("Started HTTP Server.");
            serverSocket = new ServerSocket(serverPort);
        } catch (Exception ex) {
            LogUtils.exceptionLog(ex, "Exception binding to port.");
        }

        // Process loop for requests.
        try {
            while (true) {
                this.socket = serverSocket.accept();
                process();
            }
        } catch (Exception ex) {
            LogUtils.exceptionLog(ex, "Exception processing requests.");
        }
    }

    public void process() throws Exception {
        BufferedReader d = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        OutputStream ot = socket.getOutputStream();
        PrintWriter pw = new PrintWriter(ot);

        BufferedOutputStream out = new BufferedOutputStream(ot);

        String request = d.readLine().trim();
        StringTokenizer st = new StringTokenizer(request);

        String header = st.nextToken();

        if (header.equals("GET")) {

            // Request file name. ex: /index.html
            String fileName = st.nextToken();

            // Default file to send.
            if (fileName.equals("/")) {
                fileName = "/index.html";
            }

            // Initalize the filestream.
            InputStream inputStream = null;

            String httpStat = "200 OK";
            String contentLength = "0";
            String mimeType = "text/plain";
            String contentSend = "";
            if ((inputStream = getClass().getResourceAsStream("/web" + fileName)) != null) {
                contentLength = (new Integer(inputStream.available()).toString());
                mimeType = getMimeType(fileName);
            } else if (fileName.contains("/ajax/")) {
                String[] getResult = ajaxHandle(fileName);
                contentSend = getResult[0];
                contentLength = (new Integer(contentSend.length()).toString());
                httpStat = getResult[1];
                mimeType = getResult[2];
            } else {
                httpStat = "404 Not Found";
                contentSend = "Page not found.";
                contentLength = (new Integer(contentSend.length()).toString());
            }

            // Send HTTP Headers.
            pw.print("HTTP/1.0 " + httpStat + "\r\n");
            pw.print("Server: Backup Internal HTTP Server\r\n");
            pw.print("Content-type: " + mimeType + "\r\n");
            pw.print("Content-Length: " + contentLength + "\r\n");
            pw.print("\r\n");
            pw.flush();


            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytes = 0;
                while ((bytes = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytes);
                }
                inputStream.close();
                out.flush();
            } else {
                out.write(contentSend.getBytes());
            }


            out.close();
            socket.close();
        }
    }

    private String[] ajaxHandle(String fileName) {
        String[] postBack;
        fileName = fileName.substring(6); // "/axax/backups" -> "backups"
        
        if(fileName.equals("main")) {
            postBack = new String[]{"<p>Please use the tabs above to navigate this page.</p><p>Backup v2.1-dev (Development Build) + &lt; MD5HASH HERE &gt; By Domenic Horner</p>", "200 OK", "text/html"};
        }
        
        else if(fileName.equals("backups")) {
            postBack = new String[]{"Backups Listing :)", "200 OK", "text/html"};
        }
        
        else if(fileName.equals("settings")) {
            postBack = new String[]{"Backup Settings :)", "200 OK", "text/html"};
        }
        
        else if(fileName.equals("controls")) {
            postBack = new String[]{"Backup/Server Control :)", "200 OK", "text/html"};
        }
        
        else if(fileName.equals("stats")) {
            postBack = new String[]{"Statistics :)", "200 OK", "text/html"};
        }
                
        else if(fileName.equals("logs")) {
            postBack = new String[]{"Backup/Server Logs :)", "200 OK", "text/html"};
        }
        
        else {
            postBack = new String[]{"Internal Server Error.", "500 Internal Server Error", "text/plain"};
        }
        
        return postBack;
    }
    
    
    
    
    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    private static Hashtable theMimeTypes = new Hashtable();

    static {
        StringTokenizer st = new StringTokenizer(
                "css		text/css "
                + "htm		text/html "
                + "html		text/html "
                + "xml		text/xml "
                + "txt		text/plain "
                + "asc		text/plain "
                + "gif		image/gif "
                + "jpg		image/jpeg "
                + "jpeg		image/jpeg "
                + "png		image/png "
                + "mp3		audio/mpeg "
                + "m3u		audio/mpeg-url "
                + "mp4		video/mp4 "
                + "ogv		video/ogg "
                + "flv		video/x-flv "
                + "mov		video/quicktime "
                + "swf		application/x-shockwave-flash "
                + "js		application/javascript "
                + "pdf		application/pdf "
                + "doc		application/msword "
                + "ogg		application/x-ogg "
                + "zip		application/octet-stream "
                + "exe		application/octet-stream "
                + "class	application/octet-stream ");
        while (st.hasMoreTokens()) {
            theMimeTypes.put(st.nextToken(), st.nextToken());
        }
    }

    private String getMimeType(String fileName) {
        String mime = null;
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            mime = (String) theMimeTypes.get(fileName.substring(dot + 1).toLowerCase());
        }
        if (mime == null) {
            mime = "text/plain";
        }
        return mime;
    }
}