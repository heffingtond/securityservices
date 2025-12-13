package rest;

import java.io.StringReader;
import java.util.ArrayList;


import beans.SecurityProfileBean;
import core.SecurityServicesUtilities;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;


@Path("security")
public class SecurityServicesProfileCrud
{
	/* GET method
	 * Security profile will be returned in JSON String format.
	 * When using the GET method, include the input JSON in the header as indicated here:
	 * Name = 'input'
	 * Example value = {"userId":"THEUSER","password":"test_password"}
	 */
    @GET
    @Produces("application/json; charset=UTF-8")  
    public synchronized String getSecurityServicesProfile( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
    {
    	System.out.println( "SecurityProfileCrud input is " + input );
    	if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
    	{
    		authString = authString.substring( 6 );
    	}
    	JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
    	String userId = jsonInputObject.getString( "userId" );
    	String password = jsonInputObject.getString( "password" );
    	
		String serverType = "localhost";
		
        SecurityProfileBean profile = new SecurityProfileBean();
        
        profile.setUserId( userId.toString() );
        profile.setPassword( password.toString() );
        
        String returnPayload = "";
		System.out.println( "SecurityProfileCrud: authentication lookup from somewhere." );
        if ( authenticate( authString ) )
        {
			System.out.println( "SecurityProfileCrud: authentication successful." );
			System.out.println( "SecurityProfileCrud: Just OK" );
	       	try 
	       	{
	       		SecurityServicesUtilities.getSecurityServicesProfile( profile );
	        }
	        catch(Exception e) 
	       	{
	        	System.out.println( "getComments() exception" );
	            e.printStackTrace();
	        }
        }
        else
        {
        	System.out.println( "Bad authentication data.  Cannot process request.  Authentication failed." );
        	throw new WebApplicationException( 401 ); // Unauthorized
        }
		
		ArrayList<JsonObject> pload = new ArrayList<JsonObject>();
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonObject jsonReturnObject = objectBuilder
                .add("authenticationProfileId", profile.getAuthenticationProfileId())
                .add("userId", profile.getUserId() != null ? Json.createValue( profile.getUserId() ) : JsonValue.NULL)
                .add("firstName", profile.getFirstName() != null ? Json.createValue( profile.getFirstName() ) : JsonValue.NULL)
                .add("lastName", profile.getLastName() != null ? Json.createValue( profile.getLastName() ) : JsonValue.NULL)
                .add("mobilePhone", profile.getMobilePhone() != null ? Json.createValue( profile.getMobilePhone() ) : JsonValue.NULL)
                .add("officePhone", profile.getOfficePhone() != null ? Json.createValue( profile.getOfficePhone() ) : JsonValue.NULL)
                .add("officePhoneExt", profile.getOfficePhoneExt() != null ? Json.createValue( profile.getOfficePhoneExt() ) : JsonValue.NULL)
                .add("homePhone", profile.getHomePhone() != null ? Json.createValue( profile.getHomePhone() ) : JsonValue.NULL)
                .add("verificationCodeMethod", profile.getVerificationCodeMethod() != null ? Json.createValue( profile.getVerificationCodeMethod() ) : JsonValue.NULL)
                .add("failedLoginAttempts", profile.getFailedLoginAttempts())
                .build();
		pload.add( jsonReturnObject );

		returnPayload = pload.toString();
		
		System.out.println( "SecurityProfileCrud - Returning JSON payload." );
    	return returnPayload;
    }
    
    // This method will process the rest service authentication as desired.
    private static boolean authenticate( String authString )
    {
    	boolean authenticated = true;
    	return authenticated;
    }
}
