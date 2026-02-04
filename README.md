# POC Recommendation UI

A Java Web Application built with Jakarta EE, JSF, and PrimeFaces.

## Technologies

- **Java 21**
- **Jakarta EE 10** (Jakarta Servlet 6.0, Jakarta Faces 4.0)
- **PrimeFaces 12.0.7** - Latest version with modern UI components
- **CDI (Weld)** - For dependency injection
- **Maven** - Build and dependency management

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── org/example/
│   │       └── bean/
│   │           └── RecommendationBean.java  # JSF Managed Bean
│   ├── resources/
│   └── webapp/
│       ├── WEB-INF/
│       │   ├── web.xml              # Web application configuration
│       │   ├── faces-config.xml     # JSF configuration
│       │   └── beans.xml            # CDI configuration
│       └── index.xhtml              # Main page with PrimeFaces components
└── test/
    └── java/
```

## Building the Project

```bash
mvn clean package
```

This will create a WAR file in the `target/` directory: `poc-recommendation-ui.war`

## Running the Application

### Option 1: Run in IntelliJ IDEA (Recommended for Development)

**Method A: Using IntelliJ's Built-in Tomcat**

1. **Configure Tomcat Server:**
   - Go to `Run` → `Edit Configurations...`
   - Click `+` → Select `Tomcat Server` → `Local`
   - Click `Configure...` and select your Tomcat 10+ installation directory
   - Set `Application server` to your Tomcat 10+ path

2. **Create Run Configuration:**
   - In the same dialog, go to the `Deployment` tab
   - Click `+` → Select `Artifact` → Choose `poc-recommendation-ui:war`
   - Set `Application context` to `/` (or leave empty)
   - Click `OK`

3. **Run:**
   - Click the green Run button or press `Shift+F10`
   - Access the application at: `http://localhost:8080`

**Method B: Using Maven Jetty Plugin (Simplest)**

1. Open the Maven tool window in IntelliJ (View → Tool Windows → Maven)
2. Expand `poc-recommendation-ui` → `Plugins` → `jetty`
3. Double-click `jetty:run`
4. Access the application at: `http://localhost:8080`

Or run from terminal:
```bash
mvn jetty:run
```

**Note:** For Tomcat, use IntelliJ's built-in Tomcat configuration (see Method 1 above or INTELLIJ_SETUP.md).

**Method C: Using IntelliJ's Run Configuration for Maven**

1. Go to `Run` → `Edit Configurations...`
2. Click `+` → Select `Maven`
3. Set:
   - **Name:** `Run Tomcat`
   - **Working directory:** Your project root
   - **Command line:** `tomcat10:run`
4. Click `OK` and run

### Option 2: Deploy to Application Server

Deploy the generated WAR file to any Jakarta EE compatible server:
- **Tomcat 10+** (with JSF and CDI support)
- **WildFly** / **JBoss EAP**
- **Payara** / **GlassFish**
- **OpenLiberty**

Build the WAR first:
```bash
mvn clean package
```

Then deploy `target/poc-recommendation-ui.war` to your server.

## Features

The application includes:
- **PrimeFaces UI Components**: Input fields, calendar, rating, data table
- **Form Handling**: Submit and clear functionality
- **Data Display**: Paginated data table
- **CDI Integration**: Session-scoped managed beans
- **Modern Theme**: PrimeFaces Saga theme with Font Awesome icons

## Configuration

### PrimeFaces Theme

The default theme is set to "saga" in `web.xml`. You can change it to:
- `saga` (default)
- `arya`
- `luna-amber`
- `luna-blue`
- `luna-green`
- `luna-pink`
- `nova-colored`
- `nova-dark`
- `nova-light`
- `rhea`

### Project Stage

Currently set to "Development" in `web.xml`. Change to "Production" when deploying.

## Dependencies

All dependencies are managed through Maven. Key dependencies:
- Jakarta Servlet API 6.0.0
- Jakarta Faces (Mojarra) 4.0.1
- PrimeFaces 12.0.7
- Jakarta CDI API 4.0.1
- Weld (CDI Implementation) 5.1.2.Final

## Notes

- The application uses Jakarta EE 10 specifications (formerly Java EE)
- PrimeFaces 12.0.7 is the latest stable version
- Java 21 is required for compilation
- The project is configured as a WAR (Web Application Archive)
# poc-recommendation-ui
