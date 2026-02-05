package org.example.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;

import java.io.Serializable;

@Named("menuBean")
@SessionScoped
public class MenuBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private MenuModel model; // Menu model for Settings panel
    private MenuModel homeModel; // Menu model for Home menu item
    private MenuModel sectionsModel; // Menu model for Sections panel
    private String currentPage = "/home.xhtml";

    @PostConstruct
    public void init() {
        // Home menu model (single item)
        homeModel = new DefaultMenuModel();
        DefaultMenuItem homeItem = new DefaultMenuItem();
        homeItem.setValue("Home");
        homeItem.setIcon("pi pi-home");
        homeItem.setCommand("#{menuBean.loadPage('/home.xhtml')}");
        homeItem.setUpdate(":contentPanel");
        homeItem.setAjax(true);
        homeItem.setProcess("@this");
        homeModel.getElements().add(homeItem);

        // Settings menu model (Section Image only)
        model = new DefaultMenuModel();

        // Image menu item
        DefaultMenuItem sectionImageItem = new DefaultMenuItem();
        sectionImageItem.setValue("Image");
        sectionImageItem.setIcon("pi pi-image");
        sectionImageItem.setCommand("#{menuBean.loadPage('/upload.xhtml')}");
        sectionImageItem.setUpdate(":contentPanel");
        sectionImageItem.setAjax(true);
        sectionImageItem.setProcess("@this");
        model.getElements().add(sectionImageItem);

        // Sections menu model
        sectionsModel = new DefaultMenuModel();
        
        // Tools menu item
        DefaultMenuItem toolsItem = new DefaultMenuItem();
        toolsItem.setValue("Tools");
        toolsItem.setIcon("pi pi-wrench");
        toolsItem.setCommand("#{menuBean.loadPage('/tools.xhtml')}");
        toolsItem.setUpdate(":contentPanel");
        toolsItem.setAjax(true);
        toolsItem.setProcess("@this");
        sectionsModel.getElements().add(toolsItem);
    }

    public String loadPage(String page) {
        this.currentPage = page;
        return null;
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public MenuModel getModel() {
        return model;
    }

    public void setModel(MenuModel model) {
        this.model = model;
    }

    public MenuModel getHomeModel() {
        return homeModel;
    }

    public void setHomeModel(MenuModel homeModel) {
        this.homeModel = homeModel;
    }

    public MenuModel getSectionsModel() {
        return sectionsModel;
    }

    public void setSectionsModel(MenuModel sectionsModel) {
        this.sectionsModel = sectionsModel;
    }
}
