
# Google Contacts API Integration - Spring Boot Application

**Author:** Math Lee L. Biacolo

**Course:** IT342 - G3 - 2ndSem2425

**Due Date:** March 5, 2025 10:30 AM

## Objective

This project is a Spring Boot web application that integrates with the Google Contacts (People) API. The goal is to demonstrate the ability to:

*   Authenticate with Google using OAuth 2.0.
*   Retrieve a list of contacts from the authenticated user's Google Contacts.
*   Perform CRUD (Create, Read, Update, Delete) operations on Google Contacts through a user-friendly web interface built with Thymeleaf.

## Instructions to Run the Application

Follow these steps to set up and run the application on your local machine.

**1. Set Up Google API Credentials**

Before running the application, you need to configure Google API credentials.

*   **1.1. Create a Google Cloud Project:**
    *   Go to the [Google Cloud Console](https://console.cloud.google.com/).
    *   If you don't have a project already, create a new project. Give it a descriptive name (e.g., "Google Contacts Integration Project").
    *   Take note of your **Project ID**. You'll need this later.

*   **1.2. Enable the People API:**
    *   In the Google Cloud Console, navigate to "APIs & Services" -> "Library".
    *   Search for "People API" and click on it.
    *   Click "Enable" to enable the People API for your project.

*   **1.3. Configure OAuth 2.0 Credentials:**
    *   Navigate to "APIs & Services" -> "Credentials".
    *   Click "+ CREATE CREDENTIALS" and select "OAuth client ID".
    *   If you are prompted to configure the consent screen, click "CONFIGURE CONSENT SCREEN".
        *   **User Type:** Choose "External" and click "CREATE".
        *   **App name:** Enter a name for your application (e.g., "Contact Integration App").
        *   **User support email:** Select your email address.
        *   **Developer contact information:** Enter your email address.
        *   Click "SAVE AND CONTINUE" through the remaining sections (Scopes, Optional info, Summary) and then "BACK TO DASHBOARD".
    *   Now, back in "Credentials", click "+ CREATE CREDENTIALS" and select "OAuth client ID" again.
        *   **Application type:** Select "Web application".
        *   **Name:** Give your client ID a name (e.g., "Web client").
        *   **Authorized JavaScript origins:** Leave this empty for local development.
        *   **Authorized redirect URIs:** Add the following URI: `http://localhost:8080/login/oauth2/code/google`
        *   Click "CREATE".
        *   A pop-up will appear with your **Client ID** and **Client Secret**. **Copy and securely store these values.** You will need to configure them in your Spring Boot application. Click "OK".

*   **1.4. Add Required Scopes to OAuth Consent Screen (If needed):**
    *   If you didn't configure scopes during the consent screen setup, go back to "APIs & Services" -> "OAuth consent screen".
    *   Click "EDIT APP".
    *   Go to the "Scopes" section.
    *   Click "ADD SCOPES".
    *   In the "Manually add scopes" section, add the following scope: `https://www.googleapis.com/auth/contacts` (for full contacts access) or `https://www.googleapis.com/auth/contacts.readonly` (for read-only access if you only implemented read functionality initially and are expanding).  For full CRUD functionality, use `https://www.googleapis.com/auth/contacts`.
    *   Click "Add".
    *   Click "SAVE AND CONTINUE" and then "BACK TO DASHBOARD".

**2. Configure the Spring Boot Application**

You need to provide your Google API credentials to the Spring Boot application.

*   **2.1. Open `src/main/resources/application.properties` (or `application.yml`):**
    *   Locate the `application.properties` (or `application.yml` if you are using YAML configuration) file in your project.
    *   If the file doesn't exist, create it in the `src/main/resources` directory.

*   **2.2. Add OAuth 2.0 Client Configuration:**
    *   Add the following properties to your `application.properties` file (or equivalent YAML structure in `application.yml`), replacing the placeholders with your actual Client ID and Client Secret from step 1.3:

    ```properties
    spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
    spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
    spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google
    spring.security.oauth2.client.registration.google.scope=openid,profile,email,https://www.googleapis.com/auth/contacts
    spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
    spring.security.oauth2.client.provider.google.token-uri=https://www.googleapis.com/oauth2/v4/token
    spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo
    spring.security.oauth2.client.provider.google.jwk-set-uri=https://www.googleapis.com/oauth2/v3/certs
    spring.security.oauth2.client.provider.google.user-name-attribute=sub
    ```

    *   **Important:** Ensure you replace `YOUR_CLIENT_ID` and `YOUR_CLIENT_SECRET` with the actual values you obtained from the Google Cloud Console.

**3. Build and Run the Spring Boot Application**

*   **3.1. Open a terminal or command prompt:**
    *   Navigate to the root directory of your Spring Boot project (where the `pom.xml` file is located).
    *   This repository is located at: [https://github.com/MasuRii/IT342-PeopleAPIIntegrationActivity-Biacolo](https://github.com/MasuRii/IT342-PeopleAPIIntegrationActivity-Biacolo)

*   **3.2. Build the application using Maven:**
    *   Run the following Maven command:
        ```bash
        mvn clean install
        ```
        This command will clean the project, compile the code, and package it into a JAR file.

*   **3.3. Run the Spring Boot application:**
    *   After successful build, run the application using the Spring Boot Maven plugin:
        ```bash
        mvn spring-boot:run
        ```
        Or, you can run the JAR file directly from the `target` directory:
        ```bash
        java -jar target/contactintegration-0.0.1-SNAPSHOT.jar
        ```
    *   The application should start and run on `http://localhost:8080`.

**4. Access the Application in Your Browser**

*   **4.1. Open your web browser:**
    *   Go to the following URL: `http://localhost:8080`

*   **4.2. Google Authentication:**
    *   You should be redirected to the Google login page.
    *   Log in with the Google account whose contacts you want to access.
    *   You will be asked to grant permissions to your application to access your Google Contacts. Click "Allow".

*   **4.3. Use the Contact Integration Application:**
    *   After successful authentication, you should be redirected back to your application.
    *   You should see the user interface displaying your Google Contacts (or a message if you have no contacts).
    *   Use the provided UI elements (forms, buttons) to:
        *   **View Contacts:** Browse the list of your contacts.
        *   **Add Contact:** Fill out the form to create a new contact.
        *   **Edit Contact:** Select a contact to edit and modify its details.
        *   **Delete Contact:** Select a contact to delete it from your Google Contacts.

**Functionality and Features**

This application provides the following functionalities:

*   **Google OAuth 2.0 Authentication:** Securely authenticates users with their Google accounts to access their contacts.
*   **Display Contact List:** Fetches and displays a list of contacts from the authenticated user's Google Contacts.
*   **Create Contact:** Allows users to add new contacts to their Google Contacts.
*   **Update Contact:** Enables users to modify existing contacts in their Google Contacts.
*   **Delete Contact:** Permits users to remove contacts from their Google Contacts.
*   **User-Friendly Thymeleaf UI:** Provides a simple and intuitive web interface for interacting with Google Contacts.

**Dependencies**

The project utilizes the following key technologies and libraries:

*   **Spring Boot:**  Framework for building the backend application.
*   **Spring Security OAuth2 Client:** For handling OAuth 2.0 authentication with Google.
*   **Spring Web:** For building REST controllers and web endpoints.
*   **Thymeleaf:**  Template engine for creating the user interface.
*   **Thymeleaf Extras Spring Security:** For integrating Spring Security with Thymeleaf.
*   **Google People API Client Libraries:**  `com.google.apis:google-api-services-people` and related libraries for interacting with the Google People API.
*   **Webjars (Bootstrap, Bootstrap Icons):** For frontend styling and icons.

**Known Issues and Limitations**

*   [**List any known bugs, incomplete features, or limitations of your application here.** ]
    *   Basic error handling is implemented, and could be expanded for more production-ready scenarios.
    *   Input validation on contact fields is present but could be enhanced for stricter data quality.
    *   The UI styling is functional but could be further improved for enhanced user experience and visual appeal.
    *   **Cross-Device Testing:** The application has primarily been tested in a single development environment and has not yet been thoroughly tested across different browsers, operating systems, or devices (desktops, laptops, tablets, mobile). Compatibility and responsiveness across various platforms may need further verification.

**Submission Requirements Checklist**

Please ensure you have completed all the following submission requirements:

*   [x] **Source code hosted on GitHub:**  The complete source code of your project is pushed to a public (or accessible to the instructor) GitHub repository.
    *   **GitHub Repository URL:** [https://github.com/MasuRii/IT342-PeopleAPIIntegrationActivity-Biacolo](https://github.com/MasuRii/IT342-PeopleAPIIntegrationActivity-Biacolo)
*   [ ] **Short Report:** A short report explaining the implementation steps and challenges encountered is submitted separately (or linked in this README - see below).
    *   **Link to Report (if applicable):** [**If you are linking your report in the README, add the link here.**]
*   [ ] **Screenshots or Video Demo:** Screenshots or a short video demo showcasing the working application are provided (either in the report or as separate files).
