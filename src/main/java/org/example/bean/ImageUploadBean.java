package org.example.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.file.UploadedFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named("imageUploadBean")
@SessionScoped
public class ImageUploadBean implements Serializable {

    private static final long serialVersionUID = 1L;
    // Store in project directory under src/main/webapp/uploads (accessible via web)
    // Alternative: Use System.getProperty("user.home") + "/project-uploads" for outside webapp
    private static final String UPLOAD_DIR = "uploads";
    
    private UploadedFile file;
    private UploadedFiles files;
    private String section; // Section name for the uploaded image
    private List<ImageInfo> uploadedImages; // Session-scoped: current session uploads
    private List<ImageInfo> allImages; // All images from directory

    public ImageUploadBean() {
        System.out.println("========================================");
        System.out.println("=== ImageUploadBean CONSTRUCTOR CALLED ===");
        System.out.println("========================================");
        uploadedImages = new ArrayList<>();
        allImages = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        System.out.println("========================================");
        System.out.println("=== ImageUploadBean @PostConstruct init() CALLED ===");
        System.out.println("Bean instance: " + this);
        System.out.println("Bean class: " + this.getClass().getName());
        System.out.println("========================================");
        createUploadDirectory();
        loadAllImages();
    }

    private void createUploadDirectory() {
        try {
            Path uploadPath = getUploadPath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            System.err.println("Error creating upload directory: " + e.getMessage());
        }
    }

    /**
     * Get the upload directory path
     * Always use the webapp realPath from FacesContext - this is where the server serves files from
     */
    private Path getUploadPath() {
        String customPath = System.getProperty("app.upload.dir");
        if (customPath != null && !customPath.isEmpty()) {
            return Paths.get(customPath);
        }
        
        // ALWAYS use webapp realPath - this is the only reliable way
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                String webappPath = facesContext.getExternalContext().getRealPath("/");
                if (webappPath != null && !webappPath.isEmpty()) {
                    Path uploadPath = Paths.get(webappPath, UPLOAD_DIR);
                    System.out.println("Using webapp realPath: " + uploadPath);
                    return uploadPath;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not get webapp path from FacesContext: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fallback: try to construct SmartTomcat path
        try {
            Path smartTomcatPath = Paths.get(System.getProperty("user.home"), ".SmartTomcat", "poc-recommendation-ui", "poc-recommendation-ui", "target", "poc-recommendation-ui", UPLOAD_DIR);
            System.out.println("FacesContext not available, using SmartTomcat path: " + smartTomcatPath);
            return smartTomcatPath;
        } catch (Exception e) {
            System.err.println("Could not get SmartTomcat path: " + e.getMessage());
        }
        
        // Final fallback: user home directory
        Path fallbackPath = Paths.get(System.getProperty("user.home"), "project-uploads");
        System.out.println("Using fallback path: " + fallbackPath);
        return fallbackPath;
    }

    /**
     * Generate unique filename to avoid conflicts
     */
    private String generateUniqueFileName(String originalFileName, String section) {
        String extension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFileName.substring(lastDot);
        }
        String baseName = originalFileName.substring(0, lastDot > 0 ? lastDot : originalFileName.length());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        // Prefix with section if provided: section_originalname_timestamp_uuid.ext
        String sectionPrefix = (section != null && !section.trim().isEmpty()) ? section.trim() + "_" : "";
        return sectionPrefix + baseName + "_" + timestamp + "_" + uniqueId + extension;
    }

    public void uploadSingle(FileUploadEvent event) {
        System.out.println("========================================");
        System.out.println("=== uploadSingle CALLED ===");
        System.out.println("Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("Thread: " + Thread.currentThread().getName());
        System.out.println("Event: " + event);
        System.out.println("Event class: " + (event != null ? event.getClass().getName() : "NULL"));
        System.out.println("Bean instance: " + this);
        System.out.println("FacesContext: " + FacesContext.getCurrentInstance());
        System.out.println("========================================");
        
        if (event == null) {
            System.err.println("ERROR: FileUploadEvent is NULL!");
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                            "File upload event is null"));
            return;
        }
        
        try {
            UploadedFile uploadedFile = event.getFile();
            System.out.println("Uploaded file object: " + uploadedFile);
            System.out.println("Uploaded file class: " + (uploadedFile != null ? uploadedFile.getClass().getName() : "NULL"));
            
            if (uploadedFile == null) {
                System.err.println("ERROR: uploadedFile is NULL!");
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                                "File upload failed: uploaded file is null"));
                return;
            }
            
            System.out.println("File name: " + uploadedFile.getFileName());
            System.out.println("File size: " + uploadedFile.getSize());
            System.out.println("Content type: " + uploadedFile.getContentType());
            
            if (uploadedFile.getSize() <= 0) {
                System.err.println("ERROR: File size is 0 or negative!");
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", 
                                "File is empty or invalid"));
                return;
            }
            
            if (uploadedFile.getSize() > 0) {
            try {
                String originalFileName = uploadedFile.getFileName();
                String imageSection = section != null && !section.trim().isEmpty() ? section.trim() : "";
                String uniqueFileName = generateUniqueFileName(originalFileName, imageSection);
                Path uploadPath = getUploadPath();
                
                // Ensure directory exists
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    System.out.println("Created upload directory: " + uploadPath);
                }
                
                Path targetFile = uploadPath.resolve(uniqueFileName);
                System.out.println("Uploading file to: " + targetFile);
                System.out.println("File size: " + uploadedFile.getSize() + " bytes");
                
                try (InputStream input = uploadedFile.getInputStream()) {
                    Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                
                System.out.println("File uploaded successfully: " + targetFile);
                System.out.println("File exists: " + Files.exists(targetFile));
                System.out.println("File size on disk: " + (Files.exists(targetFile) ? Files.size(targetFile) : 0));
                
                ImageInfo imageInfo = new ImageInfo(
                    uniqueFileName,
                    originalFileName,
                    uploadedFile.getSize(),
                    LocalDateTime.now(),
                    section != null ? section : ""
                );
                uploadedImages.add(imageInfo);
                loadAllImages(); // Refresh all images list
                
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                                "File '" + originalFileName + "' uploaded successfully!"));
            } catch (IOException e) {
                System.err.println("Upload error: " + e.getMessage());
                e.printStackTrace();
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                                "Failed to upload file: " + e.getMessage()));
            }
            } else {
                System.err.println("File size check failed - size: " + uploadedFile.getSize());
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in uploadSingle: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                            "Unexpected error: " + e.getMessage()));
        }
    }

    public void uploadMultiple(FileUploadEvent event) {
        System.out.println("=== uploadMultiple called ===");
        System.out.println("Event: " + event);
        UploadedFile uploadedFile = event.getFile();
        System.out.println("Uploaded file: " + uploadedFile);
        if (uploadedFile != null) {
            System.out.println("File name: " + uploadedFile.getFileName());
            System.out.println("File size: " + uploadedFile.getSize());
        }
        if (uploadedFile != null && uploadedFile.getSize() > 0) {
            try {
                String originalFileName = uploadedFile.getFileName();
                String imageSection = section != null && !section.trim().isEmpty() ? section.trim() : "";
                String uniqueFileName = generateUniqueFileName(originalFileName, imageSection);
                Path uploadPath = getUploadPath();
                
                // Ensure directory exists
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                Path targetFile = uploadPath.resolve(uniqueFileName);
                System.out.println("Uploading file (multiple) to: " + targetFile);
                
                    try (InputStream input = uploadedFile.getInputStream()) {
                        Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    System.out.println("File uploaded successfully: " + targetFile);
                    System.out.println("File exists: " + Files.exists(targetFile));
                
                ImageInfo imageInfo = new ImageInfo(
                    uniqueFileName,
                    originalFileName,
                    uploadedFile.getSize(),
                    LocalDateTime.now(),
                    section != null ? section : ""
                );
                uploadedImages.add(imageInfo);
                loadAllImages(); // Refresh all images list
                
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                                "File '" + originalFileName + "' uploaded successfully!"));
            } catch (IOException e) {
                System.err.println("Upload error for " + uploadedFile.getFileName() + ": " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                                "Failed to upload file: " + uploadedFile.getFileName()));
            }
        }
    }

    public void clearUploads() {
        System.out.println("========================================");
        System.out.println("=== clearUploads() CALLED ===");
        System.out.println("Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("========================================");
        uploadedImages.clear();
        file = null;
        files = null;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Upload list cleared."));
    }
    
    /**
     * Upload method for simple mode - called from commandButton
     */
    public void uploadSingleFromValue() {
        System.out.println("========================================");
        System.out.println("=== uploadSingleFromValue CALLED ===");
        System.out.println("File object: " + file);
        System.out.println("========================================");
        
        if (file != null && file.getSize() > 0) {
            // Create a mock FileUploadEvent
            try {
                String originalFileName = file.getFileName();
                String imageSection = section != null && !section.trim().isEmpty() ? section.trim() : "";
                String uniqueFileName = generateUniqueFileName(originalFileName, imageSection);
                Path uploadPath = getUploadPath();
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                Path targetFile = uploadPath.resolve(uniqueFileName);
                System.out.println("Uploading file to: " + targetFile);
                
                try (InputStream input = file.getInputStream()) {
                    Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                
                System.out.println("File uploaded successfully: " + targetFile);
                
                ImageInfo imageInfo = new ImageInfo(
                    uniqueFileName,
                    originalFileName,
                    file.getSize(),
                    LocalDateTime.now(),
                    section != null ? section : ""
                );
                uploadedImages.add(imageInfo);
                loadAllImages();
                
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                                "File '" + originalFileName + "' uploaded successfully!"));
            } catch (IOException e) {
                System.err.println("Upload error: " + e.getMessage());
                e.printStackTrace();
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                                "Failed to upload file: " + e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", 
                            "Please select a file first."));
        }
    }

    /**
     * Load all images from the upload directory
     */
    public void loadAllImages() {
        allImages = new ArrayList<>();
        try {
            Path uploadPath = getUploadPath();
            System.out.println("Loading images from: " + uploadPath);
            System.out.println("Path exists: " + Files.exists(uploadPath));
            System.out.println("Is directory: " + (Files.exists(uploadPath) ? Files.isDirectory(uploadPath) : "N/A"));
            
            if (Files.exists(uploadPath) && Files.isDirectory(uploadPath)) {
                allImages = loadImagesFromPath(uploadPath);
                
                // Section is now extracted from filename, no need to match with session list
                
                allImages.sort(Comparator.comparing(ImageInfo::getUploadDate).reversed());
                System.out.println("Total loaded images: " + allImages.size());
            } else {
                System.err.println("Upload directory does not exist: " + uploadPath);
            }
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to load images from a specific path
     */
    private List<ImageInfo> loadImagesFromPath(Path uploadPath) {
        List<ImageInfo> images = new ArrayList<>();
        try {
            if (Files.exists(uploadPath) && Files.isDirectory(uploadPath)) {
                try (Stream<Path> paths = Files.list(uploadPath)) {
                    images = paths
                            .filter(Files::isRegularFile)
                            .filter(path -> {
                                String fileName = path.getFileName().toString().toLowerCase();
                                return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                                       fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                                       fileName.endsWith(".bmp") || fileName.endsWith(".webp");
                            })
                            .map(path -> {
                                try {
                                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                                    String fileName = path.getFileName().toString();
                                    long size = attrs.size();
                                    FileTime fileTime = attrs.lastModifiedTime();
                                    LocalDateTime uploadDate = LocalDateTime.ofInstant(
                                            fileTime.toInstant(), ZoneId.systemDefault());
                                    
                                    String originalName = extractOriginalName(fileName);
                                    String extractedSection = extractSection(fileName);
                                    System.out.println("Found image: " + fileName + " (" + size + " bytes) in " + uploadPath + ", section: " + extractedSection);
                                    return new ImageInfo(fileName, originalName, size, uploadDate, extractedSection);
                                } catch (IOException e) {
                                    System.err.println("Error reading file: " + path + " - " + e.getMessage());
                                    return null;
                                }
                            })
                            .filter(imageInfo -> imageInfo != null)
                            .collect(Collectors.toList());
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading from " + uploadPath + ": " + e.getMessage());
        }
        return images;
    }

    /**
     * Extract section from filename pattern
     * Pattern: section_originalname_timestamp_uuid.extension (if section provided)
     * Pattern: originalname_timestamp_uuid.extension (if no section)
     */
    private String extractSection(String fileName) {
        // Find timestamp pattern: yyyyMMdd_HHmmss (8digits_6digits)
        java.util.regex.Pattern timestampPattern = java.util.regex.Pattern.compile("(\\d{8}_\\d{6})");
        java.util.regex.Matcher matcher = timestampPattern.matcher(fileName);
        if (matcher.find()) {
            int timestampStart = matcher.start();
            String beforeTimestamp = fileName.substring(0, timestampStart);
            // Remove trailing underscore
            if (beforeTimestamp.endsWith("_")) {
                beforeTimestamp = beforeTimestamp.substring(0, beforeTimestamp.length() - 1);
            }
            // Check if there's a section prefix (underscore before timestamp)
            int firstUnderscore = beforeTimestamp.indexOf('_');
            if (firstUnderscore > 0) {
                // Pattern: section_originalname, extract section
                return beforeTimestamp.substring(0, firstUnderscore);
            }
        }
        return "";
    }
    
    /**
     * Extract original filename from the unique filename pattern
     * Pattern: section_originalname_timestamp_uuid.extension (if section provided)
     * Pattern: originalname_timestamp_uuid.extension (if no section)
     */
    private String extractOriginalName(String fileName) {
        // Find timestamp pattern: yyyyMMdd_HHmmss
        java.util.regex.Pattern timestampPattern = java.util.regex.Pattern.compile("(\\d{8}_\\d{6})");
        java.util.regex.Matcher matcher = timestampPattern.matcher(fileName);
        if (matcher.find()) {
            int timestampStart = matcher.start();
            String beforeTimestamp = fileName.substring(0, timestampStart);
            // Remove trailing underscore
            if (beforeTimestamp.endsWith("_")) {
                beforeTimestamp = beforeTimestamp.substring(0, beforeTimestamp.length() - 1);
            }
            // Check if there's a section prefix
            int firstUnderscore = beforeTimestamp.indexOf('_');
            String originalName;
            if (firstUnderscore > 0) {
                // Pattern: section_originalname, extract original name
                originalName = beforeTimestamp.substring(firstUnderscore + 1);
            } else {
                // Pattern: originalname (no section)
                originalName = beforeTimestamp;
            }
            // Get extension
            int lastDot = fileName.lastIndexOf('.');
            String extension = lastDot > 0 ? fileName.substring(lastDot) : "";
            return originalName + extension;
        }
        // If pattern doesn't match, return filename as-is
        return fileName;
    }

    /**
     * Delete an image file
     */
    public void deleteImage(String fileName) {
        try {
            Path filePath = getUploadPath().resolve(fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                loadAllImages(); // Refresh list
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                                "Image '" + fileName + "' deleted successfully!"));
            }
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                            "Failed to delete image: " + e.getMessage()));
        }
    }

    // Getters and Setters
    public UploadedFile getFile() {
        System.out.println("=== getFile() called ===");
        System.out.println("File value: " + file);
        System.out.println("Timestamp: " + java.time.LocalDateTime.now());
        return file;
    }

    public void setFile(UploadedFile file) {
        System.out.println("========================================");
        System.out.println("=== setFile() called ===");
        System.out.println("Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("File parameter: " + file);
        System.out.println("File class: " + (file != null ? file.getClass().getName() : "NULL"));
        if (file != null) {
            System.out.println("File name: " + file.getFileName());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());
            
            // Process the file immediately since listener isn't firing
            System.out.println("Processing file in setFile() since listener isn't firing...");
            processUploadedFile(file);
        }
        System.out.println("========================================");
        this.file = file;
    }
    
    /**
     * Process uploaded file - extracted from uploadSingle to be reusable
     */
    private void processUploadedFile(UploadedFile uploadedFile) {
        if (uploadedFile == null || uploadedFile.getSize() <= 0) {
            System.err.println("ERROR: Invalid file in processUploadedFile");
            return;
        }
        
        try {
            String originalFileName = uploadedFile.getFileName();
            String imageSection = section != null && !section.trim().isEmpty() ? section.trim() : "";
            String uniqueFileName = generateUniqueFileName(originalFileName, imageSection);
            Path uploadPath = getUploadPath();
            
            // Ensure directory exists
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath);
            }
            
            Path targetFile = uploadPath.resolve(uniqueFileName);
            System.out.println("Uploading file to: " + targetFile);
            System.out.println("File size: " + uploadedFile.getSize() + " bytes");
            
            try (InputStream input = uploadedFile.getInputStream()) {
                Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            System.out.println("File uploaded successfully: " + targetFile);
            System.out.println("File exists: " + Files.exists(targetFile));
            System.out.println("File size on disk: " + (Files.exists(targetFile) ? Files.size(targetFile) : 0));
            
            // Section is already encoded in filename, extract it
            String extractedSection = extractSection(uniqueFileName);
            ImageInfo imageInfo = new ImageInfo(
                uniqueFileName,
                originalFileName,
                uploadedFile.getSize(),
                LocalDateTime.now(),
                extractedSection
            );
            uploadedImages.add(imageInfo);
            loadAllImages(); // Refresh all images list
            
            // Clear section field after successful upload
            section = null;
            
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                            "File '" + originalFileName + "' uploaded successfully!"));
        } catch (IOException e) {
            System.err.println("Upload error in processUploadedFile: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                            "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error in processUploadedFile: " + e.getMessage());
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                            "Unexpected error: " + e.getMessage()));
        }
    }

    public UploadedFiles getFiles() {
        return files;
    }

    public void setFiles(UploadedFiles files) {
        this.files = files;
    }

    public List<ImageInfo> getUploadedImages() {
        return uploadedImages;
    }

    public void setUploadedImages(List<ImageInfo> uploadedImages) {
        this.uploadedImages = uploadedImages;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getUploadDirectory() {
        return getUploadPath().toString();
    }

    /**
     * Get web-accessible URL for an image
     */
    public String getImageUrl(String fileName) {
        String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
        String url = contextPath + "/uploads/" + fileName;
        System.out.println("Generated image URL for " + fileName + ": " + url);
        return url;
    }

    /**
     * Get all images from the upload directory
     */
    public List<ImageInfo> getAllImages() {
        // Always reload to get latest images from disk
        loadAllImages();
        return allImages != null ? allImages : new ArrayList<>();
    }

    /**
     * Get count of all uploaded images
     */
    public int getAllImagesCount() {
        return allImages != null ? allImages.size() : 0;
    }

    /**
     * Inner class to store image metadata
     */
    public static class ImageInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String fileName;
        private String originalName;
        private long size;
        private LocalDateTime uploadDate;
        private String section;

        public ImageInfo(String fileName, String originalName, long size, LocalDateTime uploadDate, String section) {
            this.fileName = fileName;
            this.originalName = originalName;
            this.size = size;
            this.uploadDate = uploadDate;
            this.section = section;
        }

        public String getFileName() {
            return fileName;
        }

        public String getOriginalName() {
            return originalName;
        }

        public long getSize() {
            return size;
        }

        public String getSizeFormatted() {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.2f KB", size / 1024.0);
            } else {
                return String.format("%.2f MB", size / (1024.0 * 1024.0));
            }
        }

        public LocalDateTime getUploadDate() {
            return uploadDate;
        }

        public String getUploadDateFormatted() {
            return uploadDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }
    }
}
