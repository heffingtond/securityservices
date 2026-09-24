package rest;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import beans.AuthenticationProfileBean;
import core.SecurityServicesUtilities;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;

/**
 * POST, PUST, and DELETE shortly
 */

@Path("security")
public class SecurityServicesProfileCrud
{
	/* GET method
	 * Security profile will be returned in JSON String format.
	 * When using the GET method, include the input JSON in the header as indicated here:
	 * Name = 'input'
	 * Example value = {"userId":"THEUSER","password":"test_password"}
	 */
	@Path("authentication")
    @GET
    @Produces("application/json; charset=UTF-8")  
    public synchronized String getSecurityServicesProfile( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
    {
    	if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
    	{
    		authString = authString.substring( 6 );
    	}
    	JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
    	String userId = jsonInputObject.getString( "userId" );
    	System.out.println( "SecurityServicesProfileCrud: Login for user ID : " + userId );
    	String textPassword = jsonInputObject.getString( "password" );
		
        AuthenticationProfileBean profile = new AuthenticationProfileBean();
        
        profile.setUserId( userId );
        profile.setTextPassword( textPassword );
        
        boolean userAuthenticated = false;
        String returnPayload = "";
		System.out.println( "SecurityProfileCrud: authentication lookup from somewhere." );
        if ( authenticate( authString ) )
        {
			System.out.println( "SecurityProfileCrud: authentication successful." );
			System.out.println( "SecurityProfileCrud: Just OK" );
	       	try 
	       	{
	       		SecurityServicesUtilities.getSecurityServicesProfile( profile );
	       		// Authenticate
	       		System.out.println( "AuthenticationProfileId is " + profile.getAuthenticationProfileId() );
	       		if ( profile.getAuthenticationProfileId() != 0 )
	       			userAuthenticated = SecurityServicesUtilities.authenticateUserLogin( textPassword, 
	       																				 profile.getSalt(),
	       																				 profile.getPassword() );
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
        
        String clientResult = "NOT FOUND";
        if ( profile.getAuthenticationProfileId() == 0 )
        	clientResult = "USER NOT FOUND";
        else
        if ( userAuthenticated )
        	clientResult = "AUTHENTICATION SUCCESSFUL";
        else
        	clientResult = "INCORRECT PASSWORD";
        
		ArrayList<JsonObject> pload = new ArrayList<JsonObject>();
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonObject jsonReturnObject = objectBuilder
                .add("authenticationProfileId", profile.getAuthenticationProfileId())
                .add("organizationId", profile.getOrganizationId())
                .add("userId", profile.getUserId() != null ? Json.createValue( profile.getUserId() ) : JsonValue.NULL)
                .add("firstName", profile.getFirstName() != null ? Json.createValue( profile.getFirstName() ) : JsonValue.NULL)
                .add("lastName", profile.getLastName() != null ? Json.createValue( profile.getLastName() ) : JsonValue.NULL)
                .add("mobilePhone", profile.getMobilePhone() != null ? Json.createValue( profile.getMobilePhone() ) : JsonValue.NULL)
                .add("officePhone", profile.getOfficePhone() != null ? Json.createValue( profile.getOfficePhone() ) : JsonValue.NULL)
                .add("officePhoneExt", profile.getOfficePhoneExt() != null ? Json.createValue( profile.getOfficePhoneExt() ) : JsonValue.NULL)
                .add("homePhone", profile.getHomePhone() != null ? Json.createValue( profile.getHomePhone() ) : JsonValue.NULL)
                .add("verificationCodeMethod", profile.getVerificationCodeMethod() != null ? Json.createValue( profile.getVerificationCodeMethod() ) : JsonValue.NULL)
                .add("failedLoginAttempts", profile.getFailedLoginAttempts())
                .add("clientResult", clientResult)
                .build();
		pload.add( jsonReturnObject );

		returnPayload = pload.toString();
		
		System.out.println( "SecurityProfileCrud - Returning JSON payload." );
    	return returnPayload;
    }
    
	/* GET method
	 * All roles for requested user + application will be returned.
	 * When using the GET method, include the input JSON in the header as indicated here:
	 * Name = "input"
	 * Example value = {"userId":"THEUSER","applicationCode":"SPTR"}
	 */
	@Path("roles")
    @GET
    @Produces("application/json; charset=UTF-8")  
    public synchronized String getRoles( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
    {
    	if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
    	{
    		authString = authString.substring( 6 );
    	}
    	JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
    	String userId = jsonInputObject.getString( "userId" );
    	String applicationCode = jsonInputObject.getString( "applicationCode" );
		
    	TreeMap< String, ArrayList<String>> rolesForDepartment = null;
        if ( authenticate( authString ) )
        {
			System.out.println( "SecurityProfileCrud: authentication successful." );
	       	try 
	       	{
	       		rolesForDepartment = SecurityServicesUtilities.getUserRoles( userId, applicationCode );
	       	}
	        catch(Exception e) 
	       	{
	        	System.out.println( "getRoles() exception" );
	            e.printStackTrace();
	        }
        }
        else
        {
        	System.out.println( "Bad authentication data.  Cannot process request.  Authentication failed." );
        	throw new WebApplicationException( 401 ); // Unauthorized
        }
        
        String clientResult = null;
        if ( rolesForDepartment == null )
        	clientResult = "ERROR GETTING ROLES FROM DATABASE";
        else
        if ( rolesForDepartment.size() == 0 )
        	clientResult = "NO ROLES EXIST FOR THIS USER + APPLICATION";
        else
        	clientResult = "ROLES SUCCESS";
		
		JSONArray departmentArray = new JSONArray();
        for ( Map.Entry<String, ArrayList<String>> entry : rolesForDepartment.entrySet() ) 
        {
            JSONObject departmentObject = new JSONObject();
            departmentObject.put( "department", entry.getKey() );
    		JSONArray roleArray = new JSONArray();
            for( String role : entry.getValue() )
            {
                JSONObject roleObject = new JSONObject();
                roleObject.put( "role", role );
                roleArray.add( roleObject );
            }
            departmentObject.put( "roles", roleArray );
            departmentArray.add( departmentObject );
        }
  
        String returnPayload = departmentArray.toJSONString();
        
		System.out.println( "SecurityProfileCrud - Returning JSON payload." );
		System.out.println( returnPayload );
    	return returnPayload;
    }
	   
	/* POST method
	 * Send a text to the provided mobile phone.
	 * When using the POST method, include the input JSON in the header as indicated here:
	 * Name = "input"
	 * Example value = {"phoneNumber":"USER'S MOBILE NUMBER","message":"THE TEXT MESSAGE"}
	 */
	@Path("text")
    @POST
    @Produces("application/json; charset=UTF-8")  
    public synchronized String sendText( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
    {
    	if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
    	{
    		authString = authString.substring( 6 );
    	}
    	JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
    	String phoneNumber = jsonInputObject.getString( "phoneNumber" );
    	String textMessage = jsonInputObject.getString( "textMessage" );
    	String endpointUrl = jsonInputObject.getString( "endpointUrl" );
		
        if ( authenticate( authString ) )
        {
			System.out.println( "SecurityProfileCrud: authentication successful." );
        }
        else
        {
        	System.out.println( "Bad authentication data.  Cannot process request.  Authentication failed." );
        	throw new WebApplicationException( 401 ); // Unauthorized
        }
		System.out.println( "Call the third party service to actually send the text to the customer here." );
		System.out.println( "Endpoint URL to send text to: " + endpointUrl );
		System.out.println( "Pnone number for this message: " + phoneNumber );
		System.out.println( "Message: " + textMessage );
        
        String textResult = "SUCCESS";
		JSONArray messageSendResultArray = new JSONArray();
        JSONObject messageSendResult = new JSONObject();
        messageSendResult.put( "textResult", textResult );
        messageSendResultArray.add( messageSendResult );
  
    	return messageSendResultArray.toJSONString();
    }

    // This method will process the rest service authentication as desired.
    private static boolean authenticate( String authString )
    {
    	boolean authenticated = true;
    	return authenticated;
    }
}
