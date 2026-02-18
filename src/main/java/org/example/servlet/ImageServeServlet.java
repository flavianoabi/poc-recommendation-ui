package org.example.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageServeServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private static final String UPLOAD_DIR = "uploads";
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Get the requested file path
        String requestPath = request.getPathInfo();
        if (requestPath == null || requestPath.isEmpty() || requestPath.equals("/")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file specified");
            return;
        }
        
        // Remove leading slash
        if (requestPath.startsWith("/")) {
            requestPath = requestPath.substring(1);
        }
        
        // URL decode the filename
        String decodedFileName = URLDecoder.decode(requestPath, StandardCharsets.UTF_8);
        
        // Get the upload directory
        String webappPath = getServletContext().getRealPath("/");
        Path uploadPath = Paths.get(webappPath, UPLOAD_DIR);
        Path filePath = uploadPath.resolve(decodedFileName);
        
        // Security check: ensure the file is within the upload directory
        if (!filePath.normalize().startsWith(uploadPath.normalize())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        // Check if file exists
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + decodedFileName);
            return;
        }
        
        // Determine content type
        String contentType = getServletContext().getMimeType(decodedFileName);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        // Set response headers
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(filePath));
        response.setHeader("Cache-Control", "public, max-age=3600");
        
        // Stream the file
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             OutputStream os = response.getOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }
}
