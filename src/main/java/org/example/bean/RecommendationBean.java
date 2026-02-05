package org.example.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Named("recommendationBean")
@SessionScoped
public class RecommendationBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String VERSION = "13.0.0";

    private String name;
    private String message;
    private Integer rating;
    private LocalDate selectedDate;
    private boolean submitted = false;

    private List<Item> items;

    public RecommendationBean() {
        initializeItems();
    }

    private void initializeItems() {
        items = new ArrayList<>();
        items.add(new Item(1, "Item 1", "Description for item 1"));
        items.add(new Item(2, "Item 2", "Description for item 2"));
        items.add(new Item(3, "Item 3", "Description for item 3"));
        items.add(new Item(4, "Item 4", "Description for item 4"));
        items.add(new Item(5, "Item 5", "Description for item 5"));
    }

    public String submit() {
        if (name == null || name.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "Please enter a name."));
            return null;
        }

        submitted = true;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Form submitted successfully!"));
        return null;
    }

    public String clear() {
        name = null;
        message = null;
        rating = null;
        selectedDate = null;
        submitted = false;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Form cleared."));
        return null;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getVersion() {
        return VERSION;
    }

    // Inner class for demonstration
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private String name;
        private String description;

        public Item() {
        }

        public Item(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
