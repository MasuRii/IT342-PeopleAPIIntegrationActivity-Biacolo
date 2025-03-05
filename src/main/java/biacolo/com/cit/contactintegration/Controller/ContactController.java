package biacolo.com.cit.contactintegration.Controller;

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
import org.springframework.beans.factory.annotation.Value;

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
     * Login endpoint.
     * @return login view.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Lists contacts from Google People API.
     * @param model Spring Model.
     * @param client OAuth2AuthorizedClient for Google API.
     * @param oauth2User Authenticated OAuth2 user.
     * @return display-contacts view, redirects to login if unauthenticated, error view on API failure.
     */
    @GetMapping
    public String listContacts(
        Model model,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        try {
            if (client == null || oauth2User == null) {
                logger.warn("Unauthenticated access attempt");
                return "redirect:/contacts/login";
            }

            PeopleService service = new PeopleService.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                null)
                .setApplicationName(APPLICATION_NAME)
                .build();

            ListConnectionsResponse response = service.people().connections()
                .list("people/me")
                .setPersonFields(personFields)
                .setOauthToken(client.getAccessToken().getTokenValue())
                .execute();

            List<Person> contacts = response.getConnections();

            logger.info("Fetched {} contacts", contacts != null ? contacts.size() : 0);

            model.addAttribute("contacts", contacts != null ? contacts : Collections.emptyList());
            model.addAttribute("userName", oauth2User.getAttribute("name"));
            model.addAttribute("profilePictureUrl", oauth2User.getAttribute("picture"));
            return "display-contacts";

        } catch (GeneralSecurityException | IOException e) {
            logger.error("API Error: {}", e.getMessage());
            model.addAttribute("errorMessage", "Failed to retrieve contacts. Ensure you have contacts in your Google account.");
            return "error";
        } catch (Exception e) {
            logger.error("System Error: {}", e.getMessage());
            model.addAttribute("errorMessage", "Service temporarily unavailable");
            return "error";
        }
    }

    /**
     * Creates a Google PeopleService client.
     * @param accessToken OAuth2 access token.
     * @return PeopleService client.
     * @throws GeneralSecurityException on security provider failure.
     * @throws IOException on transport setup error.
     */
    private PeopleService getPeopleService(String accessToken) throws GeneralSecurityException, IOException {
        logger.debug("Creating PeopleService with access token: {}", accessToken != null ? "[TOKEN PRESENT]" : "[TOKEN NULL]");
        try {
            return new PeopleService.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                .setApplicationName(APPLICATION_NAME)
                .build();
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Error creating PeopleService: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handles general errors in the contacts section.
     * @param model Spring Model.
     * @return error view.
     */
    @GetMapping("/error")
    public String handleError(Model model) {
        model.addAttribute("errorMessage", "An error occurred in contacts section");
        return "error";
    }

    /**
     * Displays the add contact form.
     * @param model Spring Model.
     * @return add-contact view.
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("contact", new Person());
        return "add-contact";
    }

    /**
     * Handles the submission of the add contact form.
     * @param givenName Contact's given name.
     * @param familyName Contact's family name.
     * @param email Contact's email.
     * @param phone Contact's phone number.
     * @param organization Contact's organization.
     * @param client OAuth2AuthorizedClient for Google API.
     * @param model Spring Model.
     * @return Redirects to contacts list on success.
     * @throws IOException on API error.
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
        PeopleService service = getPeopleService(client.getAccessToken().getTokenValue());

        Person newContact = new Person();

        if (givenName != null || familyName != null) {
            Name name = new Name();
            name.setGivenName(givenName);
            name.setFamilyName(familyName);
            newContact.setNames(Collections.singletonList(name));
        }

        if (email != null) {
            EmailAddress emailAddress = new EmailAddress();
            emailAddress.setValue(email);
            newContact.setEmailAddresses(Collections.singletonList(emailAddress));
        }

        if (phone != null) {
            PhoneNumber phoneNumber = new PhoneNumber();
            phoneNumber.setValue(phone);
            newContact.setPhoneNumbers(Collections.singletonList(phoneNumber));
        }

        if (organization != null) {
            Organization org = new Organization();
            org.setName(organization);
            newContact.setOrganizations(Collections.singletonList(org));
        }

        Person createdContact = service.people().createContact(newContact).execute();
        logger.info("Created contact with ID: {}", createdContact.getResourceName());
        return "redirect:/contacts";
    }

    /**
     * Displays the edit contact form.
     * @param request HttpServletRequest for resourceName.
     * @param client OAuth2AuthorizedClient for Google API.
     * @param model Spring Model.
     * @param redirectAttributes For flash attributes on redirect.
     * @return edit-contact view, redirects to contacts list with error if contact not found or API error.
     */
    @GetMapping("/edit/**")
    public String showEditForm(
        HttpServletRequest request,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        try {
            String uri = request.getRequestURI();
            String resourceName = uri.substring(uri.indexOf("/edit/") + 6);

            logger.info("Fetching contact details for resourceName: {}", resourceName);

            PeopleService service = getPeopleService(client.getAccessToken().getTokenValue());
            Person contact = service.people().get(resourceName)
                .setPersonFields(personFields)
                .execute();

            if (contact == null) {
                logger.error("Contact not found for resourceName: {}", resourceName);
                redirectAttributes.addFlashAttribute("errorMessage", "Contact not found");
                return "redirect:/contacts";
            }

            model.addAttribute("contact", contact);
            return "edit-contact";
        } catch (Exception e) {
            logger.error("Error fetching contact for edit: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to load contact details");
            return "redirect:/contacts";
        }
    }

    /**
     * Updates contact details.
     * @param resourceName Contact's resource name.
     * @param givenName Updated given name.
     * @param familyName Updated family name.
     * @param email Updated email.
     * @param phone Updated phone number.
     * @param organization Updated organization.
     * @param etag ETag for optimistic concurrency.
     * @param client OAuth2AuthorizedClient for Google API.
     * @param redirectAttributes For flash attributes on redirect.
     * @return Redirects to contacts list after update, or with error message on failure.
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

            PeopleService service = getPeopleService(client.getAccessToken().getTokenValue());

            Person existing = service.people().get(resourceName)
                .setPersonFields("names,emailAddresses,phoneNumbers,organizations,metadata")
                .execute();

            Person contactToUpdate = new Person();
            contactToUpdate.setResourceName(resourceName);
            contactToUpdate.setEtag(existing.getEtag());

            if (givenName != null || familyName != null) {
                Name name = new Name()
                    .setGivenName(givenName != null ? givenName : "")
                    .setFamilyName(familyName != null ? familyName : "");
                if (existing.getNames() != null && !existing.getNames().isEmpty()) {
                    name.setMetadata(existing.getNames().get(0).getMetadata());
                }
                contactToUpdate.setNames(Collections.singletonList(name));
            }

            if (email != null) {
                EmailAddress emailAddress = new EmailAddress().setValue(email);
                if (existing.getEmailAddresses() != null && !existing.getEmailAddresses().isEmpty()) {
                    emailAddress.setMetadata(existing.getEmailAddresses().get(0).getMetadata());
                }
                contactToUpdate.setEmailAddresses(Collections.singletonList(emailAddress));
            }

            if (phone != null && !phone.trim().isEmpty()) {
                PhoneNumber phoneNumber = new PhoneNumber().setValue(phone);
                if (existing.getPhoneNumbers() != null && !existing.getPhoneNumbers().isEmpty()) {
                    phoneNumber.setMetadata(existing.getPhoneNumbers().get(0).getMetadata());
                }
                contactToUpdate.setPhoneNumbers(Collections.singletonList(phoneNumber));
            } else {
                contactToUpdate.setPhoneNumbers(null);
            }

            if (organization != null && !organization.trim().isEmpty()) {
                Organization org = new Organization().setName(organization);
                if (existing.getOrganizations() != null && !existing.getOrganizations().isEmpty()) {
                    org.setMetadata(existing.getOrganizations().get(0).getMetadata());
                }
                contactToUpdate.setOrganizations(Collections.singletonList(org));
            } else {
                contactToUpdate.setOrganizations(null);
            }

            contactToUpdate.setMetadata(existing.getMetadata());

            service.people().updateContact(resourceName, contactToUpdate)
                .setUpdatePersonFields("names,emailAddresses,phoneNumbers,organizations")
                .execute();

            logger.info("Successfully updated contact: {}", resourceName);
            redirectAttributes.addFlashAttribute("successMessage", "Contact updated successfully");
            return "redirect:/contacts";
        } catch (GoogleJsonResponseException e) {
            logger.error("Google API error: {}", e.getDetails().getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update contact: " + e.getDetails().getMessage());
            return "redirect:/contacts";
        } catch (Exception e) {
            logger.error("Update failed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update contact: " + e.getMessage());
            return "redirect:/contacts";
        }
    }

    /**
     * Deletes a contact.
     * @param contactId Contact ID.
     * @param client OAuth2AuthorizedClient for Google API.
     * @param redirectAttributes For flash attributes on redirect.
     * @return Redirects to contacts list after deletion, or with error message on failure.
     */
    @DeleteMapping("/people/{contactId:.+}")
    public String deleteContact(
        @PathVariable String contactId,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        RedirectAttributes redirectAttributes) {

        String resourceName = "people/" + contactId;

        try {
            PeopleService service = getPeopleService(client.getAccessToken().getTokenValue());
            service.people().deleteContact(resourceName).execute();

            redirectAttributes.addFlashAttribute("successMessage", "Contact deleted successfully");
            return "redirect:/contacts";
        } catch (Exception e) {
            logger.error("Delete failed for {}: {}", resourceName, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete contact");
            return "redirect:/contacts";
        }
    }
}