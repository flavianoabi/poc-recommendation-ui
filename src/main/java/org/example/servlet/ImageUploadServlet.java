package org.example.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@WebServlet(name = "ImageUploadServlet", urlPatterns = {"/upload-image"})
@MultipartConfig(
    maxFileSize = 52428800,      // 50 MB
    maxRequestSize = 104857600,   // 100 MB
    fileSizeThreshold = 0
)
public class ImageUploadServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private static final String UPLOAD_DIR = "uploads";
    
    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("========================================");
        System.out.println("=== ImageUploadServlet INITIALIZED ===");
        System.out.println("========================================");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        System.out.println("========================================");
        System.out.println("=== ImageUploadServlet POST called ===");
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Content Type: " + request.getContentType());
        System.out.println("========================================");
        
        try {
            Part filePart = request.getPart("file");
            
            if (filePart == null || filePart.getSize() == 0) {
                System.err.println("No file part found");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file uploaded");
                return;
            }
            
            String fileName = filePart.getSubmittedFileName();
            System.out.println("Original filename: " + fileName);
            System.out.println("File size: " + filePart.getSize());
            System.out.println("Content type: " + filePart.getContentType());
            
            // Get upload directory
            String webappPath = getServletContext().getRealPath("/");
            Path uploadPath = Paths.get(webappPath, UPLOAD_DIR);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath);
            }
            
            // Generate unique filename
            String uniqueFileName = generateUniqueFileName(fileName);
            Path targetFile = uploadPath.resolve(uniqueFileName);
            
            System.out.println("Saving to: " + targetFile);
            
            // Save file
            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            System.out.println("File saved successfully!");
            System.out.println("File exists: " + Files.exists(targetFile));
            System.out.println("File size on disk: " + (Files.exists(targetFile) ? Files.size(targetFile) : 0));
            
            // Redirect back to upload page
            response.sendRedirect(request.getContextPath() + "/upload.xhtml?uploaded=true");
            
        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Upload failed: " + e.getMessage());
        }
    }
    
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFileName.substring(lastDot);
        }
        String baseName = originalFileName.substring(0, lastDot > 0 ? lastDot : originalFileName.length());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return baseName + "_" + timestamp + "_" + uniqueId + extension;
    }
}
