package no.bouvet.sap.neo.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.persistence.FeedEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bouvet Experiment: REST API for the FeedEntry class
 * 
 * Uses annotations from JAX-RS and JAXB
 * Is called from the Jersey servlet com.sun.jersey.spi.container.servlet.ServletContainer
 * 
 * This class is processed if url is http(s)://<server>/<appl_context_root>/<jersey_root>/feed/*
 * 
 * @author dagfinn.parnas@bouvet.no
 */
@Path("/feed")
public class FeedResourceEndpoint {
	final Logger logger = LoggerFactory.getLogger(FeedResourceEndpoint.class);
	
	//Ask jersey to populate this parameter for one of the REST methods
	@Context
	UriInfo uriInfo;
	
	//attributes used for reading/writing to JPA persistence
	private static DataSource ds;
	private static EntityManagerFactory emf;
	
	/**
	 * Constructor needs to have no parameters.
	 * It will initialize the datasource we are using (JPA)
	 * 
	 */
	public FeedResourceEndpoint(){
		try {
			initPersistencyLayer();
		}catch (Exception e) {
			//TODO: Handle better
			logger.error("Failed to initialize persistency layer", e);
		}
	}

	/**
	 * Main method that returns all feeds in the persistency layer.
	 * It can produce the content in either JSON or XML (based on client preferences).
	 * Jersey handles the marshalling automatically.
	 * 
	 * Curl example (return all feeds in json format):
	 * $ curl  -i -H "Accept: application/json" 
	 * http://localhost:8080/nwcloud-rest_example/api/feed/
	 */
	@GET
	@Produces( { MediaType.APPLICATION_JSON ,  MediaType.APPLICATION_XML})
	public List<FeedEntry> getAllFeedEntries() {
		//Get all feed entries from persistency layer
		EntityManager em = emf.createEntityManager();
		List<FeedEntry> resultList = em.createNamedQuery("AllFeedEntries",
				FeedEntry.class).getResultList();
		
		//Logging
		String message = (resultList==null)? "getAllFeedEntries returning null": "getAllFeedEntries returning " + resultList.size() + " entries";
		logger.info(message);
		
		return resultList;
	}

	/**
	 * Method that returns all feeds in the persistency layer.
	 * Can be used for testing in the browser, as it request
	 * the media type we expose in this method
	 */
	@GET
	@Produces( { MediaType.TEXT_XML })
	public List<FeedEntry> getAllFeedEntriesForHTML() {

		EntityManager em = emf.createEntityManager();
		List<FeedEntry> resultList = em.createNamedQuery("AllFeedEntries",
				FeedEntry.class).getResultList();
		
		//Logging
		String message = (resultList==null)? "getAllFeedEntries returning null": "getAllFeedEntries returning " + resultList.size() + " entries";
		logger.info(message);
		return resultList;
	}	
	
	/**
	 * Return a single feed entry based on ID
	 * Will be called if request has syntax /feed/<feed id>
	 * It can produce the content in either JSON or XML (based on client preferences)
	 * 
	 * Curl example (return feed with id 2)
	 * $ curl  -i -H "Accept: application/json" 
	 * http://localhost:8080/feed_stream/api/feed/2
	 */
	@GET
	@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("{feedid}/")
    public FeedEntry getSingleFeed(@PathParam("feedid") String strFeedId) {
		EntityManager em = emf.createEntityManager();
		//Logging
		logger.error("getSingleFeed with id:"+ strFeedId + " called");
		
		try {
			long feedId = Long.parseLong(strFeedId);
			FeedEntry feedEntry = em.find(FeedEntry.class, feedId);
			return feedEntry;
		}catch (NumberFormatException e1) {
			// TODO: Input parameter is not a long and therefore not a valid primary key
			logger.warn("getSingleFeed for " + strFeedId + " is not a valid key", e1);
		}catch (IllegalArgumentException e2){
			//Invalid type of parameter . Should not happen normally
			logger.warn("getSingleFeed for " + strFeedId + " gave exception", e2);
		}
		return null;
    }
	
	/**
	 * POST a new object and store it in the persistency layer
	 * Must be called with the HTTP POST method 
	 * and accepts input in both JSON and XML format.
	 * 
	 * Curl example (creates new feed):
	 * $ curl -i -X POST -H 'Content-Type: application/json' 
	 * -d '{"senderName":"Jane Doe","feedText":"test","isComment":false,"senderEmail":"dagfinn.parnas@bouvet.no"}' 
	 * http://localhost:8080/nwcloud-rest_example/api/feed/
	 * 
	 * @param feedEntry
	 * @return
	 */
	@POST
	@Consumes( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response createSingleFeed(FeedEntry feedEntry) {
		//The feedEntry is automatically populated based on the input. Yeah!
		logger.info("Creating new feed ");
		
		//persist the entry
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(feedEntry);
		em.getTransaction().commit();

		//The HTTP response should include the URL to the newly generated new entry.
		//Probably exist a better way of doing this, but it works
		try {
			URI createdURI = new URI(uriInfo.getAbsolutePath()+""+ feedEntry.getId());
			return Response.created(createdURI).build();
		} catch (URISyntaxException e) {
			logger.warn("Unable to create correct URI for newly created feed " + feedEntry, e);
			//fallback is to include the input path (which will be lacking the id of the new object)
			return Response.created(uriInfo.getAbsolutePath()).build();
		}	
	}
	
	/**
	 * Update one or more fields of a single feed entry
	 * 
	 * Curl example (updates senderEmail for feed with id 2) :
	 * $ curl -i -X POST -H 'Content-Type: application/json' 
	 * -d '{"senderEmail":"dagfinn.parnas@gmail.com"}' http://localhost:8080/feed_stream/api/feed/2
	 */
	@POST
	@Consumes( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Path("{feedid}/")
    public Response updateSingleFeed(@PathParam("feedid") String strFeedId, FeedEntry modifiedFeedEntry) {
		logger.info("updateSingleFeed with id:"+ strFeedId);
		
		try {
			long feedId = Long.parseLong(strFeedId);
			
			EntityManager em = emf.createEntityManager();
			FeedEntry currentFeedEntry = em.find(FeedEntry.class, feedId);
			if(currentFeedEntry==null){
				logger.warn("updateSingleFeed failed as " + strFeedId + " does not exist");
				return Response.notModified(strFeedId + " does not exist").build();
			}
			//allow the post to only have one or more fields updated
			if(modifiedFeedEntry.getParent()!=null){
				currentFeedEntry.setParent(modifiedFeedEntry.getParent());
			}
			if(modifiedFeedEntry.getSenderEmail()!=null){
				currentFeedEntry.setSenderEmail(modifiedFeedEntry.getSenderEmail());
			}
			if(modifiedFeedEntry.getSenderName()!=null){
				currentFeedEntry.setSenderName(modifiedFeedEntry.getSenderName());
			}
			if(modifiedFeedEntry.getFeedText()!=null){
				currentFeedEntry.setFeedText(modifiedFeedEntry.getFeedText());
			}
			if(modifiedFeedEntry.getTimeCreated()!=null){
				currentFeedEntry.setTimeCreated(modifiedFeedEntry.getTimeCreated());
			}
			//store in persistency store
			em.getTransaction().begin();
			em.persist(currentFeedEntry);
			em.getTransaction().commit();
			
			//return an ok response
			return Response.ok().build();
		}catch (NumberFormatException e1) {
			// TODO: Input parameter is not a long and therefore not a valid primary key
			logger.warn("getSingleFeed for " + strFeedId + " is not a valid key", e1);
			return Response.serverError().build();
		}catch (IllegalArgumentException e2){
			//Invalid type of parameter . Should not happen normally
			logger.warn("getSingleFeed for " + strFeedId + " gave exception", e2);
			return Response.serverError().build();
		}
    }	
	

	
	/**
	 * Initialize the persistency layer (JPA)
	 * 
	 * @throws Exception
	 */
	private void initPersistencyLayer() throws Exception  {
		
		try {
			logger.debug("Setting up persistency layer for FeedResource");
			InitialContext ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/jdbc/DefaultDB");
			Map properties = new HashMap();
			properties.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, ds);
			
			//IMPORTANT! The first parameter must match your JPA Model name in persistence.xml
			emf = Persistence.createEntityManagerFactory("FeedModel", properties);
		} catch (NamingException e) {
			//TODO: Handle exception better
			logger.error("FATAL: Could not intialize database", e);
			throw new Exception(e);
		}

		
	}


}

