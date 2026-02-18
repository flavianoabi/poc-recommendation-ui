package org.example.bean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import org.primefaces.event.ReorderEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Named("sectionManagementBean")
@SessionScoped
public class SectionManagementBean implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private ImageUploadBean imageUploadBean;
    
    private String recommendationsPayload;
    private List<SectionInfo> sections;
    private Map<String, String> sectionImageMap; // Maps section ID to image filename

    private String sectionsLibrarySectionId;
    private String sectionsLibraryImageGroup;
    private List<String> previewImageFileNames = new ArrayList<>();
    private List<PreviewSectionItem> orderedPreviewSections = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        sections = new ArrayList<>();
        sectionImageMap = new HashMap<>();
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            imageUploadBean = facesContext.getApplication()
                    .evaluateExpressionGet(facesContext, "#{imageUploadBean}", ImageUploadBean.class);
        }
        loadSectionImageMappings();
    }
    
    public void parsePayload() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        
        if (recommendationsPayload == null || recommendationsPayload.trim().isEmpty()) {
            System.out.println("Payload is null or empty");
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, 
                    "Warning", "Please enter a payload to parse."));
            }
            return;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(recommendationsPayload);
            
            sections.clear();
            System.out.println("Cleared sections list");
            
            if (rootNode.isArray()) {
                System.out.println("Root node is an array with " + rootNode.size() + " elements");
                for (JsonNode recommendationNode : rootNode) {
                    parseRecommendationNode(recommendationNode);
                }
            } else {
                System.out.println("Root node is a single object, parsing as single recommendation");
                parseRecommendationNode(rootNode);
            }
            
            System.out.println("Total sections parsed: " + sections.size());
            
            // Refresh image mappings after parsing
            loadSectionImageMappings();
            System.out.println("Section image mappings loaded: " + sectionImageMap.size());
            sectionImageMap.forEach((sectionId, imageName) -> 
                System.out.println("  - " + sectionId + " -> " + imageName));
            
            // Populate ordered preview sections from parsed sections that have images
            populateOrderedPreviewSectionsFromParsedSections();
            
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Success", "Payload parsed successfully. Found " + sections.size() + " sections."));
            }
        } catch (Exception e) {
            System.err.println("Error parsing payload: " + e.getMessage());
            e.printStackTrace();
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", "Failed to parse payload: " + e.getMessage()));
            }
        }
    }
    
    /**
     * Populates orderedPreviewSections from parsed sections that have associated images.
     */
    private void populateOrderedPreviewSectionsFromParsedSections() {
        orderedPreviewSections.clear();
        previewImageFileNames.clear();
        
        if (sections == null || sections.isEmpty()) {
            System.out.println("No sections to populate from");
            return;
        }
        
        // Ensure image mappings are loaded
        if (sectionImageMap.isEmpty()) {
            loadSectionImageMappings();
        }
        
        System.out.println("Section image map size: " + sectionImageMap.size());
        sectionImageMap.forEach((id, fileName) -> 
            System.out.println("  Mapping: " + id + " -> " + fileName));
        
        // Get ordered sections (respects header_background ordering)
        List<SectionInfo> orderedSections = getOrderedSections();
        System.out.println("Ordered sections count: " + orderedSections.size());
        
        // Track added file names to avoid duplicates
        java.util.Set<String> addedFileNames = new java.util.HashSet<>();
        
        for (SectionInfo section : orderedSections) {
            String sectionId = section.getId();
            System.out.println("Processing section: " + sectionId);
            
            // Try exact match first
            String imageFileName = sectionImageMap.get(sectionId);
            
            // If no exact match, try case-insensitive match
            if (imageFileName == null) {
                for (Map.Entry<String, String> entry : sectionImageMap.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(sectionId)) {
                        imageFileName = entry.getValue();
                        System.out.println("Found case-insensitive match: " + entry.getKey() + " -> " + imageFileName);
                        break;
                    }
                }
            }
            
            if (imageFileName != null && !addedFileNames.contains(imageFileName)) {
                System.out.println("Adding section to preview: " + sectionId + " -> " + imageFileName);
                // Add to preview file names
                previewImageFileNames.add(imageFileName);
                addedFileNames.add(imageFileName);
                
                // Add to ordered preview sections
                String sectionName = sectionId;
                PreviewSectionItem item = new PreviewSectionItem(imageFileName, sectionName);
                item.setIndex(orderedPreviewSections.size() + 1);
                orderedPreviewSections.add(item);
            } else {
                System.out.println("No image found for section: " + sectionId);
            }
        }
        
        System.out.println("Populated " + orderedPreviewSections.size() + " preview sections from parsed sections");
    }
    
    private void parseRecommendationNode(JsonNode recommendationNode) {
        JsonNode itemsNode = recommendationNode.get("items");
        if (itemsNode != null && itemsNode.isArray()) {
            System.out.println("Found items array with " + itemsNode.size() + " items");
            for (JsonNode itemNode : itemsNode) {
                JsonNode additionalProperties = itemNode.get("additionalProperties");
                if (additionalProperties != null) {
                    JsonNode sectionsNode = additionalProperties.get("sections");
                    if (sectionsNode != null && sectionsNode.isArray()) {
                        System.out.println("Found sections array with " + sectionsNode.size() + " sections");
                        for (JsonNode sectionNode : sectionsNode) {
                            SectionInfo sectionInfo = parseSection(sectionNode);
                            if (sectionInfo != null) {
                                sections.add(sectionInfo);
                                System.out.println("Added section: " + sectionInfo.getId());
                            }
                        }
                    } else {
                        System.out.println("Sections node is null or not an array");
                    }
                } else {
                    System.out.println("AdditionalProperties is null");
                }
            }
        } else {
            System.out.println("Items node is null or not an array");
        }
    }
    
    private SectionInfo parseSection(JsonNode sectionNode) {
        if (sectionNode == null) {
            return null;
        }
        
        String sectionId = sectionNode.has("id") ? sectionNode.get("id").asText() : null;
        if (sectionId == null) {
            return null;
        }
        
        SectionInfo sectionInfo = new SectionInfo();
        sectionInfo.setId(sectionId);
        
        JsonNode configNode = sectionNode.get("configuration");
        if (configNode != null) {
            sectionInfo.setConfiguration(configNode.toString());
            
            if (configNode.has("header")) {
                JsonNode headerNode = configNode.get("header");
                if (headerNode.has("titleResource")) {
                    sectionInfo.setTitleResource(headerNode.get("titleResource").asText());
                }
            }
            
            if (configNode.has("layout")) {
                JsonNode layoutNode = configNode.get("layout");
                if (layoutNode.has("type")) {
                    sectionInfo.setLayoutType(layoutNode.get("type").asText());
                }
                if (layoutNode.has("columns")) {
                    sectionInfo.setColumns(layoutNode.get("columns").asText());
                }
            }
            
            if (configNode.has("segment")) {
                sectionInfo.setSegment(configNode.get("segment").asText());
            }
            
            if (configNode.has("target")) {
                JsonNode targetNode = configNode.get("target");
                if (targetNode.has("useCase")) {
                    sectionInfo.setUseCase(targetNode.get("useCase").asText());
                }
                if (targetNode.has("componentType")) {
                    sectionInfo.setComponentType(targetNode.get("componentType").asText());
                }
                if (targetNode.has("entityType")) {
                    sectionInfo.setEntityType(targetNode.get("entityType").asText());
                }
            }
        }
        
        return sectionInfo;
    }
    
    public String getImageUrlForSection(String sectionId) {
        if (sectionId == null) {
            return null;
        }
        
        // Ensure mappings are loaded
        if (sectionImageMap.isEmpty()) {
            loadSectionImageMappings();
        }
        
        String imageFileName = sectionImageMap.get(sectionId);
        if (imageFileName != null && imageUploadBean != null) {
            String url = imageUploadBean.getImageUrl(imageFileName);
            System.out.println("Getting image URL for section '" + sectionId + "': " + url);
            return url;
        }
        
        System.out.println("No image found for section '" + sectionId + "'");
        return null;
    }
    
    public boolean hasImage(String sectionId) {
        if (sectionId == null) {
            return false;
        }
        
        // Ensure mappings are loaded
        if (sectionImageMap.isEmpty()) {
            loadSectionImageMappings();
        }
        
        boolean hasImage = sectionImageMap.containsKey(sectionId) && 
                          sectionImageMap.get(sectionId) != null;
        System.out.println("hasImage('" + sectionId + "') = " + hasImage);
        return hasImage;
    }
    
    public void mapSectionToImage(String sectionId, String imageFileName) {
        if (sectionId != null && imageFileName != null) {
            sectionImageMap.put(sectionId, imageFileName);
            saveSectionImageMappings();
        }
    }
    
    private void loadSectionImageMappings() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && imageUploadBean == null) {
            imageUploadBean = facesContext.getApplication()
                    .evaluateExpressionGet(facesContext, "#{imageUploadBean}", ImageUploadBean.class);
        }
        
        if (imageUploadBean != null) {
            List<ImageUploadBean.ImageInfo> allImages = imageUploadBean.getAllImages();
            sectionImageMap.clear();
            for (ImageUploadBean.ImageInfo imageInfo : allImages) {
                String section = imageInfo.getSection();
                if (section != null && !section.trim().isEmpty()) {
                    sectionImageMap.put(section.trim(), imageInfo.getFileName());
                }
            }
        }
    }
    
    public void refreshSectionMappings() {
        loadSectionImageMappings();
    }
    
    private void saveSectionImageMappings() {
        // In a real application, you would persist this to a database or file
        // For now, we'll rely on the ImageUploadBean's section field
    }
    
    public List<String> getAllSectionIds() {
        return sections.stream()
                .map(SectionInfo::getId)
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<SectionInfo> getSections() {
        return sections;
    }
    
    /**
     * Gets the number of partner logos to display based on columns value.
     * If columns is a decimal like "4.5", it will be rounded to the nearest integer.
     */
    public int getPartnerLogosCount(SectionInfo section) {
        if (section == null || section.getColumns() == null || section.getColumns().trim().isEmpty()) {
            return 5; // Default to 5 logos
        }
        try {
            double columnsValue = Double.parseDouble(section.getColumns().trim());
            return (int) Math.round(columnsValue);
        } catch (NumberFormatException e) {
            return 5; // Default to 5 logos if parsing fails
        }
    }
    
    /**
     * Gets a list of integers from 0 to count-1 for iterating over partner logos.
     */
    public List<Integer> getPartnerLogosList(SectionInfo section) {
        int count = getPartnerLogosCount(section);
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        return list;
    }
    
    /**
     * Gets a SectionInfo by section ID.
     */
    public SectionInfo getSectionById(String sectionId) {
        if (sectionId == null || sections == null) {
            return null;
        }
        return sections.stream()
                .filter(s -> sectionId.equals(s.getId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets the logo class name for a specific index.
     * Returns different logo styles: ambev, nestle, haleon, vilanova, generic
     */
    public String getPartnerLogoClass(int index) {
        String[] logoClasses = {"ambev", "nestle", "haleon", "vilanova", "generic"};
        return logoClasses[index % logoClasses.length];
    }
    
    /**
     * Returns sections ordered with main_header_background first, if it exists.
     */
    public List<SectionInfo> getOrderedSections() {
        if (sections == null || sections.isEmpty()) {
            return sections;
        }

        List<SectionInfo> headerBackgrounds = new ArrayList<>();
        List<SectionInfo> ordered = new ArrayList<>();

        for (SectionInfo section : sections) {
            if (isHeaderBackground(section != null ? section.getId() : null)) {
                headerBackgrounds.add(section);
            } else {
                ordered.add(section);
            }
        }

        if (!headerBackgrounds.isEmpty()) {
            headerBackgrounds.addAll(ordered);
            return headerBackgrounds;
        }

        return ordered;
    }
    
    /**
     * Returns the main_header_background section if it exists.
     */
    public SectionInfo getHeaderBackgroundSection() {
        if (sections == null) {
            return null;
        }
        return sections.stream()
                .filter(s -> isHeaderBackground(s != null ? s.getId() : null))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Returns the overlay sections (next 2 sections after main_header_background).
     * These sections should be rendered on top of the header background.
     * Returns a list with up to 2 sections (accounts and search).
     * Partners and subsequent sections are rendered as normal sections below.
     */
    public List<SectionInfo> getOverlaySections() {
        List<SectionInfo> ordered = getOrderedSections();
        if (ordered == null || ordered.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Find index of first header background
        int headerIndex = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (isHeaderBackground(ordered.get(i).getId())) {
                headerIndex = i;
                break;
            }
        }
        
        // If no header background found, return empty list
        if (headerIndex == -1) {
            return new ArrayList<>();
        }
        
        // Get next 2 sections (accounts and search) to render as overlay
        // Partners and subsequent sections will be rendered as normal sections
        List<SectionInfo> overlaySections = new ArrayList<>();
        int startIndex = headerIndex + 1;
        int endIndex = Math.min(startIndex + 2, ordered.size()); // Get up to 2 sections
        
        for (int i = startIndex; i < endIndex; i++) {
            overlaySections.add(ordered.get(i));
        }
        
        return overlaySections;
    }
    
    /**
     * Checks if a section should be rendered as half (deprecated - no longer used).
     * All overlay sections are now rendered fully.
     */
    public boolean isHalfOverlaySection(String sectionId) {
        // No longer using half sections - all overlay sections are rendered fully
        return false;
    }
    
    /**
     * Returns sections that should be rendered normally (not as overlay).
     */
    public List<SectionInfo> getNormalSections() {
        List<SectionInfo> ordered = getOrderedSections();
        List<SectionInfo> overlaySections = getOverlaySections();
        
        if (ordered == null || ordered.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SectionInfo> normalSections = new ArrayList<>();
        
        for (SectionInfo section : ordered) {
            if (isHeaderBackground(section.getId())) {
                // Skip header backgrounds - they're rendered separately as background
                continue;
            }
            
            // Skip overlay sections - they're rendered separately
            boolean isOverlay = overlaySections.stream()
                    .anyMatch(overlay -> overlay.getId().equals(section.getId()));
            
            if (!isOverlay) {
                normalSections.add(section);
            }
        }
        
        return normalSections;
    }
    
    /**
     * Checks if a section is the main_header_background.
     */
    public boolean isHeaderBackground(String sectionId) {
        return sectionId != null && sectionId.endsWith("header_background");
    }
    
    /**
     * Checks if a section name matches the header_background pattern (*_header_background).
     * This method is used to apply CSS styling to header background images.
     */
    public boolean isHeaderBackgroundSection(String sectionName) {
        return sectionName != null && sectionName.endsWith("header_background");
    }
    
    /**
     * Checks if a section is the partners section.
     */
    public boolean isPartnersSection(String sectionName) {
        return sectionName != null && sectionName.equals("partners");
    }
    
    /**
     * Handles change event when user switches between CSS and Image for partners section.
     */
    public void onPartnersTypeChange(int index) {
        if (index >= 0 && index < orderedPreviewSections.size()) {
            PreviewSectionItem item = orderedPreviewSections.get(index);
            System.out.println("Partners type changed for section: " + item.getSectionName() +
                             ", Use CSS: " + item.isUseCssForPartners());
            // Force update of preview URLs to reflect the change
            // The AJAX update will refresh the mobile preview
        }
    }
    
    /**
     * Handles change event when user switches between Image and CSS for header background.
     */
    public void onHeaderBackgroundTypeChange(int index) {
        if (index >= 0 && index < orderedPreviewSections.size()) {
            PreviewSectionItem item = orderedPreviewSections.get(index);
            System.out.println("Header background type changed for section: " + item.getSectionName() + 
                             ", Use CSS: " + item.isUseCssForHeaderBackground());
            // Force update of preview URLs to reflect the change
            // The AJAX update will refresh the mobile preview
        }
    }
    
    /**
     * Checks if a section should be rendered as overlay.
     */
    public boolean isOverlaySection(String sectionId) {
        return getOverlaySections().stream()
                .anyMatch(s -> s.getId().equals(sectionId));
    }
    
    public void setSections(List<SectionInfo> sections) {
        this.sections = sections;
    }
    
    public String getRecommendationsPayload() {
        return recommendationsPayload;
    }
    
    public void setRecommendationsPayload(String recommendationsPayload) {
        this.recommendationsPayload = recommendationsPayload;
    }
    
    public Map<String, String> getSectionImageMap() {
        return sectionImageMap;
    }

    public String getSectionsLibrarySectionId() {
        return sectionsLibrarySectionId;
    }

    public void setSectionsLibrarySectionId(String sectionsLibrarySectionId) {
        this.sectionsLibrarySectionId = sectionsLibrarySectionId;
    }

    public String getSectionsLibraryImageGroup() {
        return sectionsLibraryImageGroup;
    }

    public void setSectionsLibraryImageGroup(String sectionsLibraryImageGroup) {
        this.sectionsLibraryImageGroup = sectionsLibraryImageGroup;
    }

    public void previewSelectedImage() {
        if (sectionsLibraryImageGroup == null || sectionsLibraryImageGroup.isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "Select an image group to preview."));
            return;
        }
        String fileName = resolveLatestImageFileNameForGroup(sectionsLibraryImageGroup);
        if (fileName == null || fileName.isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "No image found for selected group."));
            return;
        }

        if (previewImageFileNames.stream().noneMatch(existing -> Objects.equals(existing, fileName))) {
            previewImageFileNames.add(fileName);
            // Add to ordered list
            String sectionName = extractSectionFromFileName(fileName);
            PreviewSectionItem item = new PreviewSectionItem(fileName, sectionName != null ? sectionName : fileName);
            item.setIndex(orderedPreviewSections.size() + 1);
            orderedPreviewSections.add(item);
        }
    }
    
    private String extractSectionFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        // Extract section from filename (format: section_originalname_timestamp_uuid.ext)
        // Or from ImageInfo if available
        if (imageUploadBean != null) {
            List<ImageUploadBean.ImageInfo> allImages = imageUploadBean.getAllImages();
            for (ImageUploadBean.ImageInfo imageInfo : allImages) {
                if (fileName.equals(imageInfo.getFileName()) && imageInfo.getSection() != null && !imageInfo.getSection().isBlank()) {
                    return imageInfo.getSection();
                }
            }
        }
        // Fallback: extract from filename
        int firstUnderscore = fileName.indexOf('_');
        if (firstUnderscore > 0) {
            return fileName.substring(0, firstUnderscore);
        }
        return fileName;
    }

    /**
     * Checks if a filename contains header_background.
     */
    public boolean isHeaderBackgroundFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        // Check if filename contains header_background (case-insensitive)
        return fileName.toLowerCase().contains("header_background");
    }
    
    /**
     * Gets header background image URLs (for background layer).
     */
    public List<String> getHeaderBackgroundImageUrls() {
        if (orderedPreviewSections == null || orderedPreviewSections.isEmpty()) {
            System.out.println("getHeaderBackgroundImageUrls: orderedPreviewSections is empty");
            return List.of();
        }
        
        List<String> headerBackgroundFileNames = orderedPreviewSections.stream()
                .map(PreviewSectionItem::getFileName)
                .filter(Objects::nonNull)
                .filter(this::isHeaderBackgroundFileName)
                .distinct()
                .collect(Collectors.toList());
        
        System.out.println("getHeaderBackgroundImageUrls: Found " + headerBackgroundFileNames.size() + " header background files");
        headerBackgroundFileNames.forEach(fileName -> System.out.println("  - " + fileName));
        
        if (headerBackgroundFileNames.isEmpty()) {
            return List.of();
        }

        if (imageUploadBean == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                imageUploadBean = facesContext.getApplication()
                        .evaluateExpressionGet(facesContext, "#{imageUploadBean}", ImageUploadBean.class);
            }
        }

        if (imageUploadBean == null) {
            System.out.println("getHeaderBackgroundImageUrls: imageUploadBean is null");
            return List.of();
        }

        List<String> urls = headerBackgroundFileNames.stream()
                .filter(Objects::nonNull)
                .map(imageUploadBean::getImageUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        System.out.println("getHeaderBackgroundImageUrls: Generated " + urls.size() + " URLs");
        return urls;
    }
    
    /**
     * Gets regular (non-header-background) image URLs (for foreground layer).
     */
    public List<String> getRegularPreviewImageUrls() {
        if (orderedPreviewSections == null || orderedPreviewSections.isEmpty()) {
            return List.of();
        }
        
        List<String> regularFileNames = orderedPreviewSections.stream()
                .map(PreviewSectionItem::getFileName)
                .filter(Objects::nonNull)
                .filter(fileName -> !isHeaderBackgroundFileName(fileName))
                .distinct()
                .collect(Collectors.toList());
        
        if (regularFileNames.isEmpty()) {
            return List.of();
        }

        if (imageUploadBean == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                imageUploadBean = facesContext.getApplication()
                        .evaluateExpressionGet(facesContext, "#{imageUploadBean}", ImageUploadBean.class);
            }
        }

        if (imageUploadBean == null) {
            return List.of();
        }

        return regularFileNames.stream()
                .filter(Objects::nonNull)
                .map(imageUploadBean::getImageUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    public List<String> getPreviewImageUrls() {
        // Return all URLs in order from orderedPreviewSections
        if (orderedPreviewSections == null || orderedPreviewSections.isEmpty()) {
            return List.of();
        }

        if (imageUploadBean == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                imageUploadBean = facesContext.getApplication()
                        .evaluateExpressionGet(facesContext, "#{imageUploadBean}", ImageUploadBean.class);
            }
        }

        if (imageUploadBean == null) {
            return List.of();
        }

        return orderedPreviewSections.stream()
                .map(PreviewSectionItem::getFileName)
                .filter(Objects::nonNull)
                .map(imageUploadBean::getImageUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the index in orderedPreviewSections for a regular image at the given position.
     * This accounts for header background images that are not in the regular images list.
     */
    public int getRegularImageIndex(int regularImagePosition) {
        if (orderedPreviewSections == null || orderedPreviewSections.isEmpty()) {
            return regularImagePosition;
        }
        
        // Find the actual index in orderedPreviewSections for the regular image at this position
        int regularImageCount = 0;
        
        for (int i = 0; i < orderedPreviewSections.size(); i++) {
            PreviewSectionItem item = orderedPreviewSections.get(i);
            if (!isHeaderBackgroundFileName(item.getFileName())) {
                if (regularImageCount == regularImagePosition) {
                    return i; // Return the actual index in orderedPreviewSections
                }
                regularImageCount++;
            }
        }
        
        return regularImagePosition; // Fallback
    }
    
    /**
     * Returns the ordered list of preview sections for the grid.
     */
    public List<PreviewSectionItem> getOrderedPreviewSections() {
        // Sync with previewImageFileNames if needed
        if (orderedPreviewSections.isEmpty() && !previewImageFileNames.isEmpty()) {
            for (int i = 0; i < previewImageFileNames.size(); i++) {
                String fileName = previewImageFileNames.get(i);
                String sectionName = extractSectionFromFileName(fileName);
                PreviewSectionItem item = new PreviewSectionItem(fileName, sectionName != null ? sectionName : fileName);
                item.setIndex(i + 1);
                orderedPreviewSections.add(item);
            }
        }
        // Ensure indices are correct
        updateIndices();
        return orderedPreviewSections;
    }
    
    /**
     * Deletes a section from the preview by index.
     */
    public void deletePreviewSection(int index) {
        if (index >= 0 && index < orderedPreviewSections.size()) {
            PreviewSectionItem item = orderedPreviewSections.remove(index);
            previewImageFileNames.remove(item.getFileName());
            // Re-index remaining items
            for (int i = 0; i < orderedPreviewSections.size(); i++) {
                orderedPreviewSections.get(i).setIndex(i + 1);
            }
        }
    }
    
    /**
     * Moves a section up in the order.
     */
    public void moveSectionUp(int index) {
        System.out.println("moveSectionUp called with index: " + index + ", list size: " + orderedPreviewSections.size());
        if (index > 0 && index < orderedPreviewSections.size()) {
            PreviewSectionItem item = orderedPreviewSections.remove(index);
            orderedPreviewSections.add(index - 1, item);
            updateIndices();
            syncPreviewImageFileNames();
            System.out.println("Section moved up successfully");
        } else {
            System.out.println("moveSectionUp: Invalid index or cannot move up");
        }
    }
    
    /**
     * Moves a section down in the order.
     */
    public void moveSectionDown(int index) {
        System.out.println("moveSectionDown called with index: " + index + ", list size: " + orderedPreviewSections.size());
        if (index >= 0 && index < orderedPreviewSections.size() - 1) {
            PreviewSectionItem item = orderedPreviewSections.remove(index);
            orderedPreviewSections.add(index + 1, item);
            updateIndices();
            syncPreviewImageFileNames();
            System.out.println("Section moved down successfully");
        } else {
            System.out.println("moveSectionDown: Invalid index or cannot move down");
        }
    }
    
    /**
     * Reorders sections via drag and drop.
     */
    public void reorderSections(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < orderedPreviewSections.size() &&
            toIndex >= 0 && toIndex < orderedPreviewSections.size() &&
            fromIndex != toIndex) {
            PreviewSectionItem item = orderedPreviewSections.remove(fromIndex);
            orderedPreviewSections.add(toIndex, item);
            updateIndices();
            syncPreviewImageFileNames();
        }
    }
    
    /**
     * Handles row reorder event from PrimeFaces dataTable.
     */
    public void onRowReorder(ReorderEvent event) {
        int fromIndex = event.getFromIndex();
        int toIndex = event.getToIndex();
        reorderSections(fromIndex, toIndex);
    }
    
    /**
     * Reorders preview images based on drag and drop in mobile preview.
     * This method is called when images are reordered in the mobile preview.
     */
    public void reorderPreviewImages(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < orderedPreviewSections.size() &&
            toIndex >= 0 && toIndex < orderedPreviewSections.size() &&
            fromIndex != toIndex) {
            PreviewSectionItem item = orderedPreviewSections.remove(fromIndex);
            orderedPreviewSections.add(toIndex, item);
            updateIndices();
            syncPreviewImageFileNames();
            
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Preview images reordered."));
        }
    }
    
    /**
     * Updates indices for all items.
     */
    private void updateIndices() {
        for (int i = 0; i < orderedPreviewSections.size(); i++) {
            orderedPreviewSections.get(i).setIndex(i + 1);
        }
    }
    
    /**
     * Syncs previewImageFileNames list with orderedPreviewSections to maintain order.
     */
    private void syncPreviewImageFileNames() {
        previewImageFileNames.clear();
        for (PreviewSectionItem item : orderedPreviewSections) {
            if (item.getFileName() != null && !previewImageFileNames.contains(item.getFileName())) {
                previewImageFileNames.add(item.getFileName());
            }
        }
    }

    /**
     * Returns the list of preview image filenames for iteration in the UI.
     */
    public List<String> getPreviewImageFileNames() {
        if (previewImageFileNames == null) {
            previewImageFileNames = new ArrayList<>();
        }
        return previewImageFileNames;
    }

    /**
     * Removes an image from the preview by filename.
     */
    public void removePreviewImage(String fileName) {
        if (fileName != null && previewImageFileNames != null) {
            previewImageFileNames.remove(fileName);
        }
    }

    /**
     * Gets the image URL for a given filename.
     */
    public String getPreviewImageUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        if (imageUploadBean == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                imageUploadBean = facesContext.getApplication()
                        .evaluateExpressionGet(facesContext, "#{imageUploadBean}", ImageUploadBean.class);
            }
        }

        if (imageUploadBean == null) {
            return null;
        }

        return imageUploadBean.getImageUrl(fileName);
    }

    private String resolveLatestImageFileNameForGroup(String group) {
        if (group == null || group.isBlank()) {
            return null;
        }

        if (imageUploadBean == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                imageUploadBean = facesContext.getApplication()
                        .evaluateExpressionGet(facesContext, "#{imageUploadBean}", ImageUploadBean.class);
            }
        }

        if (imageUploadBean == null) {
            return null;
        }

        return imageUploadBean.getAllImages().stream()
                .filter(img -> group.equals(img.getSection())
                        || (img.getFileName() != null && img.getFileName().startsWith(group + "&")))
                .findFirst()
                .map(ImageUploadBean.ImageInfo::getFileName)
                .orElse(null);
    }

    public List<String> getUploadedImageGroups() {
        if (imageUploadBean == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                imageUploadBean = facesContext.getApplication()
                        .evaluateExpressionGet(facesContext, "#{imageUploadBean}", ImageUploadBean.class);
            }
        }

        if (imageUploadBean == null) {
            return List.of();
        }

        return imageUploadBean.getAllImages().stream()
                .map(ImageUploadBean.ImageInfo::getFileName)
                .map(this::extractImageGroupName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String extractImageGroupName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        int ampersandIndex = fileName.indexOf('&');
        if (ampersandIndex <= 0) {
            return fileName;
        }

        return fileName.substring(0, ampersandIndex);
    }
    
    /**
     * Represents a preview section item with index, section name, and filename.
     */
    public static class PreviewSectionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private int index;
        private String fileName;
        private String sectionName;
        private boolean useCssForHeaderBackground = false; // New property: true = use CSS, false = use image
        private boolean useCssForPartners = false; // New property: true = use CSS logos, false = use uploaded image
        
        public PreviewSectionItem(String fileName, String sectionName) {
            this.fileName = fileName;
            this.sectionName = sectionName;
            this.index = 0; // Will be set by parent
        }
        
        public int getIndex() {
            return index;
        }
        
        public void setIndex(int index) {
            this.index = index;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public String getSectionName() {
            return sectionName;
        }
        
        public void setSectionName(String sectionName) {
            this.sectionName = sectionName;
        }
        
        public boolean isUseCssForHeaderBackground() {
            return useCssForHeaderBackground;
        }
        
        public void setUseCssForHeaderBackground(boolean useCssForHeaderBackground) {
            this.useCssForHeaderBackground = useCssForHeaderBackground;
        }
        
        public boolean isUseCssForPartners() {
            return useCssForPartners;
        }
        
        public void setUseCssForPartners(boolean useCssForPartners) {
            this.useCssForPartners = useCssForPartners;
        }
    }
    
    public static class SectionInfo implements Serializable {
        private String id;
        private String configuration;
        private String titleResource;
        private String layoutType;
        private String columns;
        private String segment;
        private String useCase;
        private String componentType;
        private String entityType;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getConfiguration() {
            return configuration;
        }
        
        public void setConfiguration(String configuration) {
            this.configuration = configuration;
        }
        
        public String getTitleResource() {
            return titleResource;
        }
        
        public void setTitleResource(String titleResource) {
            this.titleResource = titleResource;
        }
        
        public String getLayoutType() {
            return layoutType;
        }
        
        public void setLayoutType(String layoutType) {
            this.layoutType = layoutType;
        }
        
        public String getColumns() {
            return columns;
        }
        
        public void setColumns(String columns) {
            this.columns = columns;
        }
        
        public String getComponentType() {
            return componentType;
        }
        
        public void setComponentType(String componentType) {
            this.componentType = componentType;
        }
        
        public String getEntityType() {
            return entityType;
        }
        
        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }
        
        public String getSegment() {
            return segment;
        }
        
        public void setSegment(String segment) {
            this.segment = segment;
        }
        
        public String getUseCase() {
            return useCase;
        }
        
        public void setUseCase(String useCase) {
            this.useCase = useCase;
        }
    }
}
