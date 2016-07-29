package com.vmware.vcac.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.cafe.consumer.ConsumerService;
import com.vmware.vcac.catalog.rest.client.service.impl.ConsumerCatalogItemService;
import com.vmware.vcac.catalog.rest.client.service.impl.ConsumerRequestService;
import com.vmware.vcac.catalog.rest.client.service.impl.ConsumerResourceService;
import com.vmware.vcac.catalog.rest.stubs.CatalogItem;
import com.vmware.vcac.catalog.rest.stubs.CatalogItemRequest;
import com.vmware.vcac.catalog.rest.stubs.CatalogResource;
import com.vmware.vcac.componentregistry.rest.stubs.ServiceInfo;
import com.vmware.vcac.core.componentregistry.rest.client.service.RegistryService;
import com.vmware.vcac.platform.content.literals.Literal;
import com.vmware.vcac.platform.rest.client.RestClient;
import com.vmware.vcac.platform.rest.client.authentication.Authentication;
import com.vmware.vcac.platform.rest.client.query.FilterParam;
import com.vmware.vcac.platform.rest.client.query.OdataQuery;
import com.vmware.vcac.platform.rest.client.query.PageOdataRequest;
import com.vmware.vcac.platform.security.SslCertificateTrust;

/**
 * This example shows how to execute the following use case using the vRealize 
 * Automation (vCloud Automation Center) client SDK:
 * <ol>
 * <li>Log into vcac
 * <li>List catalog items
 * <li>Request a machine
 * <li>List provisioned machines
 * <li>View machine details
 * <li>Logout
 * </ol>
 * 
 * <b>You must have a {@code request.json}, which is available in the CLI user documentation</b>
 */
public class ClientSdkExample {

   /** vRealize Automation service name for the catalog. See {@link #login()} for how to find service names. */
   private static final String CATALOG_SERVICE_NAME = "catalog-service";

   /** The reader which provides input from the user. */
   private final BufferedReader reader;

   /** The place to write output for the user to view. */
   private final PrintWriter writer;

   /** The vRealize Automation URL (input by user via {@link #initialize()} */
   private String url = "https://vcac.rt.lab/vcac";

   /**
    * The vRealize Automation user name, fully qualified. ie: john@example.com (input by user via
    * {@link #initialize()}
    */
   private String username = "iaasadmin@rt";

   /** The vRealize Automation user's password. (input by user via {@link #initialize()} */
   private String password = "Rtlab508!";

   /**
    * The tenant to log into. If blank, it logs into default tenant. (input by user via
    * {@link #initialize()}
    */
   private String tenant = "vsphere.local";

   /** The consumer service is the main entry point into vRealize Automation services and their REST clients. */
   private ConsumerService consumerService = null;

   /**
    * Constructs example client sdk example using {@code System.in} for {@link #reader}, and
    * {@code System.out} for {@link #writer}.
    */
   public ClientSdkExample() {
      this.reader = new BufferedReader(new InputStreamReader(System.in));
      this.writer = new PrintWriter(System.out, true);
      initialize();
   }

   /**
    * Constructs example client sdk with the provided {@code reader} and {@code writer}.
    */
   public ClientSdkExample(Reader reader, OutputStream os) {
      this.reader = new BufferedReader(reader);
      this.writer = new PrintWriter(os, true);
      initialize();
   }

   /**
    * Gets input from user to initialize {@code url}, {@code username}, {@code password} and
    * {@code tenant}.
    */
   private void initialize() {
      // Only initializes if values haven't been hard-coded.
      if (url == null) {
         url = getInput("Enter your vcac URL: ");
         username = getInput("Enter your vcac username: ");
         password = getInput("Enter your vcac password: ");
         tenant = getInput("Enter the vcac tenant to login (leave blank for the default tenant): ");
      }
      // Output inputs (masking password)
      writer.format("Initialized variables: [url=%s] [username=%s] [password=%s][tenant=%s]\n",
            url, username, password.replaceAll("\\S", "\\*"), tenant);
   }

   /**
    * Gets input from user after providing {@code label}.
    * 
    * @param label
    *        the label to prompt the user with before waiting for user input
    * @return the user input
    * @throws RuntimeException
    *         if there's a problem reading user input
    */
   private String getInput(String label) {
      writer.format(label);
      try {
         return reader.readLine();
      } catch (IOException io) {
         System.err.println("Error processing input from user: " + io);
         throw new RuntimeException("Error processing input from user", io);
      }
   }

   /**
    * <p>
    * Logs into vRealize Automation server and prints out all available services.
    * </p>
    * <p>
    * To get a REST client for a specific service, such as catalog, you will use the following code:
    * 
    * <pre>
    * RestClient client = consumerService.getDefaultRestClientForService(serviceName);
    * </pre>
    * 
    * NOTE: To help find the {@code serviceName} to use, this method prints out all available vRealize Automation
    * services to {@code writer}. If you examine that output, you could see that catalog's service
    * name is {@code catalog-service}, leading to:
    * 
    * <pre>
    * RestClient catalogClient = consumerService.getDefaultRestClientForService(&quot;catalog-service&quot;);
    * </pre>
    * 
    * NOTE: The implementation also shows an example of how to use OData filtering.
    * </p>
    */
   private void login() {
      consumerService = new ConsumerService(url, SslCertificateTrust.ALWAYS_TRUST);

      Authentication auth = null;
      if (StringUtils.isEmpty(tenant)) {
         auth = consumerService.authenticate(username, password);
      } else {
         auth = consumerService.authenticate(tenant, username, password);
      }

      RestClient crClient = consumerService.getRestClientEndPointFactory()
            .getComponentRegistryRestClient(auth);
      RegistryService registryService = new RegistryService(crClient);

      Pageable page = PageOdataRequest.page(1, 1000);
      Collection<ServiceInfo> services = registryService.getAllServices(page).getContent();
      writer.println("\nThe following services are available");
      writer.format("%30s - %s\n", "Service Name", "Service Type Id");
      writer.println(StringUtils.center("-", 50, "-")); // print border to separate header
      for (ServiceInfo service : services) {
         writer.format("%30s - %s\n", service.getName(), service.getServiceTypeId());
      }

      // Our REST client supports Odata, which provides filtering and sorting abilities
      // IMPORTANT! You need to encompass the value of the filter param in quotes (as seen below)
      OdataQuery onlyCatalogService = OdataQuery.query();

      // "name" is the property of the JSON to filter on. "'catalog'" is the value to apply to the
      // filter. Again, be sure to note the required quotes surrounding the value.
      onlyCatalogService.addFilter(FilterParam.substringOf("name", "'catalog'"));
      Pageable page2 = PageOdataRequest.page(1, 100, onlyCatalogService);
      Collection<ServiceInfo> odataServices = registryService.getAllServices(page2).getContent();
      writer.format("\nFound %d services using OData with 'catalog' in the name field\n",
            odataServices.size());
   }

   /**
    * Logs out by forgetting reference to {@code consumerService}.
    */
   private void logout() {
      consumerService = null;
      writer.println("\nLogged out successfully.");
   }

   /**
    * Helper method to check if we have logged into vRealize Automation server.
    * 
    * @return {@code true} if we've authenticated successfully; {@code false}, otherwise.
    */
   private boolean isAuthenticated() {
      return consumerService != null && consumerService.isAuthenticated();
   }

   /**
    * Queries and returns the first 100 catalog items, ordered by name.
    * 
    * @return the first 100 catalog items, ordered by name
    */
   private Collection<CatalogItem> getCatalogItems() {
      if (!isAuthenticated()) {
         throw new IllegalStateException("You must call login() first to authenticate");
      }

      RestClient catalogClient = consumerService
            .getDefaultRestClientForService(CATALOG_SERVICE_NAME);

      ConsumerCatalogItemService catalogItemService = new ConsumerCatalogItemService(catalogClient);
      OdataQuery orderByName = OdataQuery.query().addAscOrderBy("name");
      Pageable page = PageOdataRequest.page(1, 100, orderByName);
      Collection<CatalogItem> items = catalogItemService.getCatalogItems(page).getContent();

      writer.println("\nFound the following catalog items:");
      for (CatalogItem item : items) {
         writer.format("[name=%s] [type=%s] [id=%s] [providerBindingId=%s]\n",
               item.getName(), item.getCatalogItemTypeRef().getId(), item.getId(),
               item.getProviderBinding().getBindingId());
      }
      return items;
   }

   /**
    * <p>
    * Submits a machine request based on the provided {@code filename} (which should contain JSON
    * describing a {@link CatalogItemRequest}). This JSON was grabbed using Chrome's Developer
    * Tools while making a catalog item request through the UI, as detailed in the vRealize Automation
    * Programming Guide.
    * </p>
    * <p>
    * It's important to note this is a very basic example of how to request a machine using the
    * SDK. Your code will likely use a JSON template and dynamically inject appropriate values
    * (such as catalog item id, description, etc.) rather than just hard-coding the request like we
    * do here.
    * </p>
    */
   private void requestMachine(String filename) {
      if (!isAuthenticated()) {
         throw new IllegalStateException("You must call login() first to authenticate");
      }

      RestClient catalogClient = consumerService
            .getDefaultRestClientForService(CATALOG_SERVICE_NAME);
      ConsumerRequestService requestService = new ConsumerRequestService(catalogClient);

      CatalogItemRequest request = createRequest(filename);
      requestService.createRequest(request);
      writer.format("\nSubmitted new request! [catalogItemId=%s]\n", request.getCatalogItemRef()
            .getId());
   }

   /**
    * Helper method to create the request from the provided {@code filename}. See Javadocs in
    * {@link #requestMachine(String)} for more details.
    * 
    * @param filename
    *        the classpath filename containing the {@link CatalogItemRequest} JSON
    * @return the request, parsed by the provided JSON file
    */
   private CatalogItemRequest createRequest(String filename) {
      ObjectMapper mapper = new ObjectMapper();
      Resource requestJsonFile = new ClassPathResource(filename);

      CatalogItemRequest request;
      try {
         request = mapper.readValue(requestJsonFile.getFile(),
               CatalogItemRequest.class);
      } catch (Exception e) {
         writer.println("Error reading CatalogItemRequest from file: "
               + requestJsonFile.getFilename());
         throw new RuntimeException("Error creating request", e);
      }

      return request;
   }

   /**
    * Example use case that shows how to pull all resources from the catalog (along with how to
    * pull resources of only a specific resource type).
    * 
    * @return all virtual machines retrieved from the catalog service, ordered by name
    */
   private Collection<CatalogResource> getProvisionedMachines() {
      if (!isAuthenticated()) {
         throw new IllegalStateException("You must call login() first to authenticate");
      }

      RestClient catalogClient = consumerService
            .getDefaultRestClientForService(CATALOG_SERVICE_NAME);
      ConsumerResourceService resourceService = new ConsumerResourceService(catalogClient);
      OdataQuery orderByName = OdataQuery.query().addAscOrderBy("name");
      Pageable page = PageOdataRequest.page(1, 100, orderByName);

      // Pulls all resources, regardless of resource type
      Collection<CatalogResource> resources = resourceService.getResourcesList(page);

      writer.println("\nThe following resources exist");
      writer.format("%15s - %-30s - %s\n", "Resource Name", "Resource Type Id", "Resource Id");
      writer.println(StringUtils.center("-", 70, "-")); // print border to separate header
      for (CatalogResource resource : resources) {
         writer.format("%15s - %-30s - %s\n",
               resource.getName(), resource.getResourceTypeRef().getId(), resource.getId());
      }

      // Shows how to get resources by resource type (in this case, only VMs) using the REST
      // service method. NOTE: You could also use OData, as shown in login().
      Collection<CatalogResource> machineResources = resourceService.getResourcesByResourceType(
            page, "Infrastructure.Virtual");
      writer.format("\nFound %d virtual machines\n", machineResources.size());

      return machineResources;
   }

   /**
    * Returns details of the provided resource by printing out the
    * {@link CatalogResource#getResourceData()} map.
    * 
    * @param resource
    *        the catalog resource item to show details for.
    */
   private void viewDetails(CatalogResource resource) {
      if (!isAuthenticated()) {
         throw new IllegalStateException("You must call login() first to authenticate");
      }

      Map<String, Literal> map = resource.getResourceData().asMap();
      writer.format("Resource Details: [name = %s] [id = %s]\n", resource.getName(),
            resource.getId());
      writer.format("%30s - %s\n", "Key", "Value");
      writer.println(StringUtils.center("-", 50, "-")); // print border to separate header
      for (String key : map.keySet()) {
         writer.format("%30s - %s\n", key, map.get(key));
      }
   }

   /** Main entry point to example program showcasing how to use the vcac SDK client. */
   public static void main(String[] args) throws IOException {
      ClientSdkExample example = new ClientSdkExample();
      example.login();

      example.getCatalogItems();
      example.requestMachine("request.json"); // put this file on classpath

      Collection<CatalogResource> machines = example.getProvisionedMachines();
      if (machines.size() > 0) {
         CatalogResource machine = machines.iterator().next(); // get first machine
         example.viewDetails(machine);
      }

      example.logout();
   }
}
