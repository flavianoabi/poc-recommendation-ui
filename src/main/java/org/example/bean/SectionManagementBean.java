package org.example.bean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
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
        if (recommendationsPayload == null || recommendationsPayload.trim().isEmpty()) {
            System.out.println("Payload is null or empty");
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
        } catch (Exception e) {
            System.err.println("Error parsing payload: " + e.getMessage());
            e.printStackTrace();
        }
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
            
            if (configNode.has("target")) {
                JsonNode targetNode = configNode.get("target");
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
     * Returns sections ordered with main_header_background first, if it exists.
     */
    public List<SectionInfo> getOrderedSections() {
        if (sections == null || sections.isEmpty()) {
            return sections;
        }
        
        List<SectionInfo> ordered = new ArrayList<>();
        SectionInfo headerBackground = null;
        
        // Find and separate main_header_background
        for (SectionInfo section : sections) {
            if ("main_header_background".equals(section.getId())) {
                headerBackground = section;
            } else {
                ordered.add(section);
            }
        }
        
        // Put main_header_background first if found
        if (headerBackground != null) {
            ordered.add(0, headerBackground);
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
                .filter(s -> "main_header_background".equals(s.getId()))
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
        
        // Find index of main_header_background
        int headerIndex = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if ("main_header_background".equals(ordered.get(i).getId())) {
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
        boolean skipHeader = false;
        
        for (SectionInfo section : ordered) {
            if ("main_header_background".equals(section.getId())) {
                // Skip header background - it's rendered separately as background
                skipHeader = true;
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
        return "main_header_background".equals(sectionId);
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
    
    public static class SectionInfo implements Serializable {
        private String id;
        private String configuration;
        private String titleResource;
        private String layoutType;
        private String columns;
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
    }
}
