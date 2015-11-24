package at.kju.datacollector.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.Random;

public class MultipartUtility {
    private static final String CRLF = "\r\n";
    private static final String CHARSET = "UTF-8";


    private  HttpURLConnection connection;
    private  OutputStream outputStream;
    private  PrintWriter writer;
    private  String boundary;
    private  long start;

    private final Random r;

    public MultipartUtility(HttpURLConnection connection) throws IOException {
        r = new Random();
        this.connection = connection;
    }

    private long currentTimeMillis() {
        return new Date().getTime()- r.nextInt();
    }
    public void prepareConnection() {
        start = currentTimeMillis();

        boundary = "---------------------------" + currentTimeMillis();

        connection.setRequestProperty("Content-Type","multipart/form-data; boundary=" + boundary);
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

    }

    public void addFormField(final String name, final String value) {
        writer.append("--").append(boundary).append(CRLF)
                .append("Content-Disposition: form-data; name=\"").append(name)
                .append("\"").append(CRLF)
                .append("Content-Type: text/plain; charset=").append(CHARSET)
                .append(CRLF).append(CRLF).append(value).append(CRLF);
    }

    public void addFilePart(final String fieldName, final File uploadFile, String mimeType)
            throws IOException {
        final String fileName = uploadFile.getName();
        writer.append("--").append(boundary).append(CRLF)
                .append("Content-Disposition: form-data; name=\"")
                .append(fieldName).append("\"; filename=\"").append(fileName)
                .append("\"").append(CRLF).append("Content-Type: ")
                .append(mimeType).append(CRLF)
                .append("Content-Transfer-Encoding: binary").append(CRLF)
                .append(CRLF);

        writer.flush();
        outputStream.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(CRLF);
    }

    public void addHeaderField(String name, String value) {
        writer.append(name).append(": ").append(value).append(CRLF);
    }

    public void finishMultipart() throws IOException {
        writer.append(CRLF).append("--").append(boundary).append("--")
                .append(CRLF);
        writer.close();
   }
}