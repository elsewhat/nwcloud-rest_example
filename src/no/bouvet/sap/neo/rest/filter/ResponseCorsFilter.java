package no.bouvet.sap.neo.rest.filter;
 
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
 
/**
 * Filter for adding Access-Control-Allow-Origin headers for all API methods
 * 
 * Is registered in the properties to Jersey in web.xml
 * 
 * @author dagfinn.parnas
 *
 */
public class ResponseCorsFilter implements ContainerResponseFilter {
 
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    	response.getHttpHeaders().add("Access-Control-Allow-Origin", "*");
    	response.getHttpHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");

    	//allow any request headers sent by the client
		String requestACRHeaders = request.getHeaderValue("Access-Control-Request-Headers");
		if(requestACRHeaders!=null && !"".equals(requestACRHeaders.trim())){
			response.getHttpHeaders().add("Access-Control-Allow-Headers", requestACRHeaders);
		}
    	
        return response;
    }
 
}