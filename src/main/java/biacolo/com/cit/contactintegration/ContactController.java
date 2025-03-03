package biacolo.com.cit.contactintegration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value; // Add this import

@CrossOrigin
@Controller
@RequestMapping("/contacts")
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Contact Manager";

    @Value("${google.people.api.fields}")
    private String personFields;

    /**
     *  Endpoint for the login page.
     * @return login view name.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     *  Endpoint to list contacts from Google People API.
     * @param model Spring Model to add attributes for the view.
     * @param client OAuth2AuthorizedClient for Google API authentication.
     * @param oauth2User Authenticated OAuth2 user details.
     * @return display-contacts view name if successful, redirects to login if unauthenticated, error view if API fails.
     */
    @GetMapping
    public String listContacts(
        Model model,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        try {
            // Check if the user is authenticated and has an OAuth2 client.
            if (client == null || oauth2User == null) {
                logger.warn("Unauthenticated access attempt");
                return "redirect:/contacts/login"; // Redirect to login page if not authenticated.
            }

            // Build the Google People Service client.
            PeopleService service = new PeopleService.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                null)
                .setApplicationName(APPLICATION_NAME)
                .build();

            // Call the Google People API to list connections (contacts).
            ListConnectionsResponse response = service.people().connections()
                .list("people/me") // "people/me" refers to the authenticated user.
                .setPersonFields(personFields) // Fields to retrieve for each person, configured from application.properties.
                .setOauthToken(client.getAccessToken().getTokenValue()) // Set OAuth2 access token for authentication.
                .execute();

            List<Person> contacts = response.getConnections(); // Extract the list of Person objects from the response.

            logger.info("Fetched {} contacts", contacts != null ? contacts.size() : 0);

            model.addAttribute("contacts", contacts != null ? contacts : Collections.emptyList()); // Add contacts to the model.
            model.addAttribute("userName", oauth2User.getAttribute("name")); // Add user's name to the model.
            return "display-contacts"; // Return the view name to display contacts.

        } catch (GeneralSecurityException | IOException e) {
            logger.error("API Error: {}", e.getMessage());
            model.addAttribute("errorMessage", "Failed to retrieve contacts. Ensure you have contacts in your Google account."); // Error message for the view.
            return "error"; // Return error view name.
        } catch (Exception e) {
            logger.error("System Error: {}", e.getMessage());
            model.addAttribute("errorMessage", "Service temporarily unavailable"); // Generic service unavailable error.
            return "error"; // Return error view name.
        }
    }

    /**
     *  Helper method to create a Google PeopleService client.
     * @param accessToken OAuth2 access token.
     * @return PeopleService client instance.
     * @throws GeneralSecurityException if security provider initialization fails.
     * @throws IOException if an I/O error occurs during transport setup.
     */
    private PeopleService getPeopleService(String accessToken) throws GeneralSecurityException, IOException {
        logger.debug("Creating PeopleService with access token: {}", accessToken != null ? "[TOKEN PRESENT]" : "[TOKEN NULL]");
        try {
            // Build the Google People Service client with provided access token.
            return new PeopleService.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                request -> request.getHeaders().setAuthorization("Bearer " + accessToken)) // Set authorization header with the access token.
                .setApplicationName(APPLICATION_NAME)
                .build();
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Error creating PeopleService: {}", e.getMessage(), e);
            throw e; // Re-throw exception to be handled by the caller.
        }
    }

    /**
     *  Endpoint to handle general errors within the contacts section.
     * @param model Spring Model to add error message.
     * @return error view name.
     */
    @GetMapping("/error")
    public String handleError(Model model) {
        model.addAttribute("errorMessage", "An error occurred in contacts section"); // Set a generic error message.
        return "error"; // Return error view name.
    }

    /**
     *  Endpoint to display the form to add a new contact.
     * @param model Spring Model to add a new Person object for the form.
     * @return add-contact view name.
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("contact", new Person()); // Add an empty Person object to the model for the form.
        return "add-contact"; // Return view name for adding a contact.
    }

    /**
     *  Endpoint to handle the submission of the add contact form.
     * @param givenName Contact's given name from the form.
     * @param familyName Contact's family name from the form.
     * @param email Contact's email from the form.
     * @param phone Contact's phone number from the form.
     * @param organization Contact's organization from the form.
     * @param client OAuth2AuthorizedClient for Google API authentication.
     * @param model Spring Model (not directly used in this method, but included as standard Spring parameter).
     * @return Redirects to the contacts list view after successful addition.
     * @throws IOException on API communication error.
     * @throws GeneralSecurityException on security setup error.
     */
    @PostMapping("/add")
    public String addContact(
        @RequestParam("givenName") String givenName,
        @RequestParam("familyName") String familyName,
        @RequestParam("email") String email,
        @RequestParam("phone") String phone,
        @RequestParam("organization") String organization,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        Model model
    ) throws IOException, GeneralSecurityException {
        PeopleService service = getPeopleService(client.getAccessToken().getTokenValue()); // Get PeopleService client.

        Person newContact = new Person(); // Create a new Person object to hold contact details.

        // Set name if provided.
        if (givenName != null || familyName != null) {
            Name name = new Name();
            name.setGivenName(givenName);
            name.setFamilyName(familyName);
            newContact.setNames(Collections.singletonList(name)); // Add name to the new contact.
        }

        // Set email if provided.
        if (email != null) {
            EmailAddress emailAddress = new EmailAddress();
            emailAddress.setValue(email);
            newContact.setEmailAddresses(Collections.singletonList(emailAddress)); // Add email to the new contact.
        }

        // Set phone number if provided.
        if (phone != null) {
            PhoneNumber phoneNumber = new PhoneNumber();
            phoneNumber.setValue(phone);
            newContact.setPhoneNumbers(Collections.singletonList(phoneNumber)); // Add phone number to the new contact.
        }

        // Set organization if provided.
        if (organization != null) {
            Organization org = new Organization();
            org.setName(organization);
            newContact.setOrganizations(Collections.singletonList(org)); // Add organization to the new contact.
        }

        // Call Google People API to create a new contact.
        Person createdContact = service.people().createContact(newContact).execute();
        logger.info("Created contact with ID: {}", createdContact.getResourceName());
        return "redirect:/contacts"; // Redirect to the contacts list view.
    }

    /**
     *  Endpoint to display the form for editing an existing contact.
     * @param request HttpServletRequest to extract resourceName from URL path.
     * @param client OAuth2AuthorizedClient for Google API authentication.
     * @param model Spring Model to add contact details for the edit form.
     * @param redirectAttributes Used to add flash attributes for redirection.
     * @return edit-contact view name if successful, redirects to contacts list with error message if contact not found or API error.
     */
    @GetMapping("/edit/**")
    public String showEditForm(
        HttpServletRequest request,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        try {
            // Extract the resourceName from the URL path.
            String uri = request.getRequestURI();
            String resourceName = uri.substring(uri.indexOf("/edit/") + 6); // Assumes URL structure like /contacts/edit/people/resourceName

            logger.info("Fetching contact details for resourceName: {}", resourceName);

            PeopleService service = getPeopleService(client.getAccessToken().getTokenValue()); // Get PeopleService client.
            // Fetch contact details from Google People API using the resourceName.
            Person contact = service.people().get(resourceName)
                .setPersonFields(personFields) // Retrieve configured person fields.
                .execute();

            // Handle case where contact is not found.
            if (contact == null) {
                logger.error("Contact not found for resourceName: {}", resourceName);
                redirectAttributes.addFlashAttribute("errorMessage", "Contact not found"); // Error message for redirection.
                return "redirect:/contacts"; // Redirect back to contacts list.
            }

            model.addAttribute("contact", contact); // Add the fetched contact to the model for the edit form.
            return "edit-contact"; // Return view name for editing contact.
        } catch (Exception e) {
            logger.error("Error fetching contact for edit: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to load contact details"); // Error message for redirection.
            return "redirect:/contacts"; // Redirect back to contacts list in case of error.
        }
    }

    /**
     *  Endpoint to handle the submission of the edit contact form and update contact details.
     * @param resourceName Resource name of the contact to be updated.
     * @param givenName Updated given name from the form.
     * @param familyName Updated family name from the form.
     * @param email Updated email from the form.
     * @param phone Updated phone number from the form.
     * @param organization Updated organization from the form.
     * @param etag ETag for optimistic concurrency control (optional, but important for updates).
     * @param client OAuth2AuthorizedClient for Google API authentication.
     * @param redirectAttributes Used to add flash attributes for redirection messages.
     * @return Redirects to contacts list view after successful update or with error message on failure.
     */
    @PostMapping("/edit")
    public String updateContact(
        @RequestParam("resourceName") String resourceName,
        @RequestParam(value = "names[0].givenName", required = false) String givenName,
        @RequestParam(value = "names[0].familyName", required = false) String familyName,
        @RequestParam(value = "emailAddresses[0].value", required = false) String email,
        @RequestParam(value = "phoneNumbers[0].value", required = false) String phone,
        @RequestParam(value = "organizations[0].name", required = false) String organization,
        @RequestParam(value = "etag", required = false) String etag,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        RedirectAttributes redirectAttributes
    ) {
        try {
            logger.info("Updating contact with resourceName: {}", resourceName);

            PeopleService service = getPeopleService(client.getAccessToken().getTokenValue()); // Get PeopleService client.

            // Fetch the existing contact to get the latest ETag and existing data for metadata preservation.
            Person existing = service.people().get(resourceName)
                .setPersonFields("names,emailAddresses,phoneNumbers,organizations,metadata") // Fetch all relevant fields including metadata.
                .execute();

            // Prepare a new Person object for the update operation.
            Person contactToUpdate = new Person();
            contactToUpdate.setResourceName(resourceName);
            contactToUpdate.setEtag(existing.getEtag()); // Set ETag from fetched contact for optimistic locking.

            // Update name if provided, otherwise keep existing.
            if (givenName != null || familyName != null) {
                Name name = new Name()
                    .setGivenName(givenName != null ? givenName : "") // Use provided name or empty string if null.
                    .setFamilyName(familyName != null ? familyName : ""); // Use provided name or empty string if null.
                if (existing.getNames() != null && !existing.getNames().isEmpty()) {
                    name.setMetadata(existing.getNames().get(0).getMetadata()); // Preserve metadata if available.
                }
                contactToUpdate.setNames(Collections.singletonList(name)); // Set updated name.
            }

            // Update email if provided, otherwise keep existing.
            if (email != null) {
                EmailAddress emailAddress = new EmailAddress().setValue(email);
                if (existing.getEmailAddresses() != null && !existing.getEmailAddresses().isEmpty()) {
                    emailAddress.setMetadata(existing.getEmailAddresses().get(0).getMetadata()); // Preserve metadata if available.
                }
                contactToUpdate.setEmailAddresses(Collections.singletonList(emailAddress)); // Set updated email.
            }

            // Update phone if provided, clear if empty, otherwise keep existing.
            if (phone != null && !phone.trim().isEmpty()) {
                PhoneNumber phoneNumber = new PhoneNumber().setValue(phone);
                if (existing.getPhoneNumbers() != null && !existing.getPhoneNumbers().isEmpty()) {
                    phoneNumber.setMetadata(existing.getPhoneNumbers().get(0).getMetadata()); // Preserve metadata if available.
                }
                contactToUpdate.setPhoneNumbers(Collections.singletonList(phoneNumber)); // Set updated phone number.
            } else {
                contactToUpdate.setPhoneNumbers(null); // Clear phone number if input is empty.
            }

            // Update organization if provided, clear if empty, otherwise keep existing.
            if (organization != null && !organization.trim().isEmpty()) {
                Organization org = new Organization().setName(organization);
                if (existing.getOrganizations() != null && !existing.getOrganizations().isEmpty()) {
                    org.setMetadata(existing.getOrganizations().get(0).getMetadata()); // Preserve metadata if available.
                }
                contactToUpdate.setOrganizations(Collections.singletonList(org)); // Set updated organization.
            } else {
                contactToUpdate.setOrganizations(null); // Clear organization if input is empty.
            }

            contactToUpdate.setMetadata(existing.getMetadata()); // Re-set existing metadata to ensure it's not lost.

            // Execute the update operation through Google People API.
            service.people().updateContact(resourceName, contactToUpdate)
                .setUpdatePersonFields("names,emailAddresses,phoneNumbers,organizations") // Specify fields to be updated.
                .execute();

            logger.info("Successfully updated contact: {}", resourceName);
            redirectAttributes.addFlashAttribute("successMessage", "Contact updated successfully"); // Success message for redirection.
            return "redirect:/contacts"; // Redirect to contacts list view.
        } catch (GoogleJsonResponseException e) {
            logger.error("Google API error: {}", e.getDetails().getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update contact: " + e.getDetails().getMessage()); // API error message for redirection.
            return "redirect:/contacts"; // Redirect to contacts list view.
        } catch (Exception e) {
            logger.error("Update failed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update contact: " + e.getMessage()); // Generic error message for redirection.
            return "redirect:/contacts"; // Redirect to contacts list view.
        }
    }

    /**
     *  Endpoint to delete a contact.
     * @param contactId Contact ID extracted from the path variable.
     * @param client OAuth2AuthorizedClient for Google API authentication.
     * @param redirectAttributes Used to add flash attributes for redirection messages.
     * @return Redirects to contacts list view after successful deletion or with error message on failure.
     */
    @DeleteMapping("/people/{contactId:.+}")
    public String deleteContact(
        @PathVariable String contactId,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        RedirectAttributes redirectAttributes) {

        String resourceName = "people/" + contactId; // Construct resource name from contact ID.

        try {
            PeopleService service = getPeopleService(client.getAccessToken().getTokenValue()); // Get PeopleService client.
            service.people().deleteContact(resourceName).execute(); // Call Google People API to delete the contact.

            redirectAttributes.addFlashAttribute("successMessage", "Contact deleted successfully"); // Success message for redirection.
            return "redirect:/contacts"; // Redirect to contacts list view.
        } catch (Exception e) {
            logger.error("Delete failed for {}: {}", resourceName, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete contact"); // Error message for redirection.
            return "redirect:/contacts"; // Redirect to contacts list view.
        }
    }
}