package rest;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import beans.ApplicationBean;
import beans.ApplicationUserDepartmentRoleBean;
import beans.AuthenticationProfileBean;
import beans.DepartmentBean;
import beans.OrganizationBean;
import beans.RoleBean;
import beans.VerificationCodeBean;
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
    	String textPassword = jsonInputObject.getString( "password" );
		
        AuthenticationProfileBean profile = new AuthenticationProfileBean();
        
        profile.setUserId( userId );
        profile.setTextPassword( textPassword );
        
        boolean userAuthenticated = false;
        boolean twoFactorAuthNeeded = false;
        String returnPayload = "";
        if ( authenticate( authString ) )
        {
			Connection connection = null;
	       	try 
	       	{
				connection = SecurityServicesUtilities.getJndiConnection( "SECURITY_MYSQL_DB" );
				if ( connection != null )
				{
		       		SecurityServicesUtilities.getSecurityServicesProfile( profile, connection );
		       		// Authenticate
		       		if ( profile.getAuthenticationProfileId() != 0 )
		       		{
		       			userAuthenticated = SecurityServicesUtilities.authenticateUserLogin( textPassword, 
		       																				 profile.getSalt(),
		       																				 profile.getPassword() );
		       			// Check to see if 2FA needed
		       			if ( userAuthenticated && ! SecurityServicesUtilities.isEmpty( profile.getVerificationCodeMethod() ) )
		       				if ( ! profile.getVerificationCodeMethod().equalsIgnoreCase( "SELECT" ) )
			       			{
			       				twoFactorAuthNeeded = true;
			       				SecurityServicesUtilities.twoFactorAuthentication( profile, connection );
			       			}
		       		}
		       		connection.close();
				}
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
        if ( userAuthenticated && ! twoFactorAuthNeeded )
        	clientResult = "AUTHENTICATION SUCCESSFUL";
        else
        if ( ! userAuthenticated )
        	clientResult = "INCORRECT PASSWORD";
        else
        if ( twoFactorAuthNeeded )
        	clientResult = "WAIT FOR 2 FACTOR AUTHENTICATION";
        
		ArrayList<JsonObject> pload = new ArrayList<JsonObject>();
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonObject jsonReturnObject = objectBuilder
                .add("authenticationProfileId", profile.getAuthenticationProfileId())
                .add("organizationId", profile.getOrganizationId())
                .add("userId", profile.getUserId() != null ? Json.createValue( profile.getUserId() ) : JsonValue.NULL)
                .add("firstName", profile.getFirstName() != null ? Json.createValue( profile.getFirstName() ) : JsonValue.NULL)
                .add("lastName", profile.getLastName() != null ? Json.createValue( profile.getLastName() ) : JsonValue.NULL)
                .add("email", profile.getEmail() != null ? Json.createValue( profile.getEmail() ) : JsonValue.NULL)
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
		
    	return returnPayload;
    }
    
	/* GET method
	 * New verification code will be returned in JSON String format.
	 * When using the GET method, include the input JSON in the header as indicated here:
	 * Name = 'input'
	 * Example value = {"authenticationProfileId":1234,"verificationCodeMethod":"TEXT or EMAIL"}
	 */
	@Path("requestNewCode")
    @GET
    @Produces("application/json; charset=UTF-8")  
    public synchronized String getNewCode( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
    {
    	if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
    	{
    		authString = authString.substring( 6 );
    	}
    	JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
    	int authenticationProfileId = jsonInputObject.getInt( "authenticationProfileId" );
    	String verificationCodeMethod = jsonInputObject.getString( "verificationCodeMethod" );
    	String mobilePhoneNumber = jsonInputObject.getString( "mobilePhone" );
    	String email = jsonInputObject.getString( "email" );
		
    	AuthenticationProfileBean user = new AuthenticationProfileBean();
		user.setAuthenticationProfileId( authenticationProfileId );
		user.setVerificationCodeMethod( verificationCodeMethod );
		user.setMobilePhone( mobilePhoneNumber );
		user.setEmail( email );
		
        String clientResult = "NEW CODE FAILED";
        if ( authenticate( authString ) )
        {
			// insert the new code in the table.
			Connection connection = null;
	       	try 
	       	{
				connection = SecurityServicesUtilities.getJndiConnection( "SECURITY_MYSQL_DB" );
				if ( connection != null )
				{
					SecurityServicesUtilities.twoFactorAuthentication( user, connection );
					clientResult = "WAIT FOR 2 FACTOR AUTHENTICATION";
		       		connection.close();
				}
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
        
		JSONArray newCodeResultArray = new JSONArray();
		JSONObject newCodeResult = new JSONObject();
		newCodeResult.put( "newCodeResult", clientResult );
		newCodeResultArray.add( newCodeResult );

		return newCodeResultArray.toJSONString();
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
        
    	return returnPayload;
    }
	   
	/* GET method
	 * Name = "input"
	 * Example value = {"authenticationProfileId":1,"verificationCode":"012345"}
	 */
	@Path("validate")
    @GET
    @Produces("application/json; charset=UTF-8")  
    public synchronized String validateCode( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
    {
    	if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
    	{
    		authString = authString.substring( 6 );
    	}
    	JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
    	int authenticationProfileId = jsonInputObject.getInt( "authenticationProfileId" );
    	String verificationCode = jsonInputObject.getString( "verificationCode" );
		
    	boolean isEnteredCodeCorrect = false;
    	
    	VerificationCodeBean verification = null;;
        if ( authenticate( authString ) )
        {
        	Connection connection = null;
	       	try 
	       	{
				connection = SecurityServicesUtilities.getJndiConnection( "SECURITY_MYSQL_DB" );
				if ( connection != null )
				{
					verification = SecurityServicesUtilities.getVerificationCode( authenticationProfileId, connection );
					if ( verification != null )
					{
						if ( verificationCode.equals( verification.getVerificationCode() ) )
						{
							isEnteredCodeCorrect = true;
							SecurityServicesUtilities.deleteVerificationCode( verification.getVerificationCodeId(), connection );
						}
					}
					connection.close();
				}
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
        if ( isEnteredCodeCorrect )
        	clientResult = "VALIDATION SUCCESSFUL";
        else
        	clientResult = "VALIDATION FAILED";
		
		JSONArray validationResultArray = new JSONArray();
        JSONObject validationResult = new JSONObject();
        validationResult.put( "validationResult", clientResult );
        validationResultArray.add( validationResult );
  
        String returnPayload = validationResultArray.toJSONString();
        
    	return returnPayload;
    }
	   
	/* GET method to look up an organization in the APPLICATION_SECURITY database by 'organizationName'
	 * Name = "input"
	 * Example value = {"organizationName":"Stockman Customers"}
	 */
	@Path("getOrganization")
    @GET
    @Produces("application/json; charset=UTF-8")  
    public synchronized String getOrganization( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
    {
    	if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
    	{
    		authString = authString.substring( 6 );
    	}
    	JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
    	String organizationName = jsonInputObject.getString( "organizationName" );
		
    	boolean isRestrictedOrganization = false;
    	
    	if ( "Watctower".equalsIgnoreCase( organizationName ) )
    		isRestrictedOrganization = true;
    	
    	OrganizationBean organization = null;
        if ( authenticate( authString ) && ! isRestrictedOrganization )
        {
        	Connection connection = null;
	       	try 
	       	{
				connection = SecurityServicesUtilities.getJndiConnection( "SECURITY_MYSQL_DB" );
				if ( connection != null )
				{
					organization = SecurityServicesUtilities.getOrganization( organizationName, connection );
					connection.close();
				}
	       	}
	        catch(Exception e) 
	       	{
	        	System.out.println( "getOrganization() exception" );
	            e.printStackTrace();
	        }
        }
        else
        {
        	System.out.println( "Bad authentication data or restricted organization.  Cannot process request.  Authentication failed." );
        	throw new WebApplicationException( 401 ); // Unauthorized
        }
        
        
        String clientResult = null;
        if ( organization == null )
        	clientResult = "ERROR GETTING ORGANIZATION " + organizationName + "FROM DATABASE";
        else
        	clientResult = "SUCCESS";
		
		JSONArray organizationArray = new JSONArray();
        JSONObject organizationObject = new JSONObject();
        organizationObject.put( "organizationId", organization.getOrganizationId() );
        organizationObject.put( "organizationName", organization.getOrganizationName() );
        organizationObject.put( "organizationAddress", organization.getOrganizationAddress() );
        organizationObject.put( "organizationCity", organization.getOrganizationCity() );
        organizationObject.put( "organizationState", organization.getOrganizationState() );
        organizationObject.put( "organizationZip", organization.getOrganizationZip() );
        organizationObject.put( "organizationZipExt", organization.getOrganizationZipExt() );
        organizationObject.put( "primaryUrl", organization.getPrimaryUrl() );
        organizationObject.put( "organizationDescription", organization.getOrganizationDescription() );
        organizationArray.add( organizationObject );
  
        String returnPayload = organizationArray.toJSONString();
        
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
			System.out.println( "SecurityProfileCrud sendText: authentication successful." );
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
	   
	/* POST method
	 * Add a new authentication profile for the designated application.
	 */
	@Path("newCustomerProfile")
	@POST
	@Produces("application/json; charset=UTF-8")  
	public synchronized String newCustomerProfile( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
	{
		if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
		{
			authString = authString.substring( 6 );
		}
		AuthenticationProfileBean newCustomerProfile = new AuthenticationProfileBean();
		JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
		newCustomerProfile.setAuthenticationProfileId( jsonInputObject.getInt( "authenticationProfileId" ) );
		newCustomerProfile.setOrganizationId( jsonInputObject.getInt( "organizationId" ) );
		newCustomerProfile.setUserId( jsonInputObject.getString( "userId" ) );
		newCustomerProfile.setTextPassword( jsonInputObject.getString( "textPassword" ) );
//		newCustomerProfile.setPassword( jsonInputObject.getString( "password" ) );
//		newCustomerProfile.setSalt( jsonInputObject.getString( "salt" ) );
		newCustomerProfile.setFirstName( jsonInputObject.getString( "firstName" ) );
		newCustomerProfile.setLastName( jsonInputObject.getString( "lastName" ) );
		newCustomerProfile.setEmail( jsonInputObject.getString( "email" ) );
		newCustomerProfile.setMobilePhone( jsonInputObject.getString( "mobilePhone" ) );
		newCustomerProfile.setOfficePhone( jsonInputObject.getString( "officePhone" ) );
		newCustomerProfile.setOfficePhoneExt( jsonInputObject.getString( "officePhoneExt" ) );
		newCustomerProfile.setHomePhone( jsonInputObject.getString( "homePhone" ) );
		newCustomerProfile.setFailedLoginAttempts( jsonInputObject.getInt( "failedLoginAttempts" ) );
//		newCustomerProfile.setVerificationCodeMethod( jsonInputObject.getString( "verificationCodeMethod" ) );
//		newCustomerProfile.setClientResult( jsonInputObject.getString( "clientResult" ) );

	    ArrayList<String> errors = new ArrayList<String>();

	    if ( authenticate( authString ) )
	    {
			Connection connection = null;
	       	try 
	       	{
				connection = SecurityServicesUtilities.getJndiConnection( "SECURITY_MYSQL_DB" );
				if ( connection != null )
				{
				    SecurityServicesUtilities.validateAuthenticationProfile( newCustomerProfile, errors, connection );
				    if ( errors.size() == 0 )
				    {
						// Update the bean with secure password and salt.
						SecurityServicesUtilities.createSecurePasswordAndSalt( newCustomerProfile );
				    	SecurityServicesUtilities.addNewAuthenticationProfile( newCustomerProfile, connection );
			       		newCustomerProfile.setClientResult("SUCCESS");
				    }
		       		connection.close();
				}
	       	}
	        catch(Exception e) 
	       	{
	       		newCustomerProfile.setClientResult("FAILED: DATABASE ERROR");
	        	System.out.println( "new customer profile exception" );
	            e.printStackTrace();
	        }

	     }
	     else
	     {
	     	System.out.println( "Bad authentication data.  Cannot process request.  Authentication failed." );
	     	throw new WebApplicationException( 401 ); // Unauthorized
	     }
	     
	     if ( errors.size() > 0 )
	    	 newCustomerProfile.setClientResult("FAILED: " + errors.get( 0 ) );  
	     
		 JSONArray messageSendResultArray = new JSONArray();
         JSONObject messageSendResult = new JSONObject();
         messageSendResult.put( "result", newCustomerProfile.getClientResult() );
         messageSendResult.put( "authenticationProfileId", newCustomerProfile.getAuthenticationProfileId() );
         messageSendResultArray.add( messageSendResult );
	
	 	 return messageSendResultArray.toJSONString();
	 }
	   
	   
	/* POST method
	 * Add a new role for the designated application.
	 */
	@Path("newCustomerRole")
	@POST
	@Produces("application/json; charset=UTF-8")  
	public synchronized String newCustomerRole( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
	{
		if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
		{
			authString = authString.substring( 6 );
		}
		JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
		int organizationId = jsonInputObject.getInt( "organizationId" );
		int authenticationProfileId = jsonInputObject.getInt( "authenticationProfileId" );
		String applicationCode =  jsonInputObject.getString( "applicationCode" );
		String departmentCode =  jsonInputObject.getString( "departmentCode" );
		String roleName =  jsonInputObject.getString( "roleName" );
		
		
		ApplicationUserDepartmentRoleBean newCustomerRole = new ApplicationUserDepartmentRoleBean();
		newCustomerRole.setClientResult( "" );

	    ArrayList<String> errors = new ArrayList<String>();

	    if ( authenticate( authString ) )
	    {
			Connection connection = null;
	       	try 
	       	{
				connection = SecurityServicesUtilities.getJndiConnection( "SECURITY_MYSQL_DB" );
				if ( connection != null )
				{
					newCustomerRole.setAuthenticationProfileId( authenticationProfileId );
					ApplicationBean application = SecurityServicesUtilities.getApplication( applicationCode, connection );
					newCustomerRole.setApplicationId( application.getApplicationId() );
					DepartmentBean department = SecurityServicesUtilities.getDepartment( departmentCode, organizationId, connection );
					newCustomerRole.setDepartmentId( department.getDepartmentId() );
					RoleBean role = SecurityServicesUtilities.getRole( roleName, application.getApplicationId(), connection );
					newCustomerRole.setRoleId( role.getRoleId() );
			    	SecurityServicesUtilities.addNewRoleAssignment( newCustomerRole, connection );
		       		newCustomerRole.setClientResult("SUCCESS");
		       		connection.close();
				}
	       	}
	        catch(Exception e) 
	       	{
	       		newCustomerRole.setClientResult("FAILED: DATABASE ERROR");
	        	System.out.println( "new customer profile exception" );
	            e.printStackTrace();
	        }
	     }
	     else
	     {
	     	System.out.println( "Bad authentication data.  Cannot process request.  Authentication failed." );
	     	throw new WebApplicationException( 401 ); // Unauthorized
	     }
	     
	     if ( errors.size() > 0 )
	    	 newCustomerRole.setClientResult("FAILED: " + errors.get( 0 ) );  
	     
		 JSONArray messageSendResultArray = new JSONArray();
		 JSONObject messageSendResult = new JSONObject();
		 messageSendResult.put( "result", newCustomerRole.getClientResult() );
		 messageSendResultArray.add( messageSendResult );
	
	 	 return messageSendResultArray.toJSONString();
	 }
	   
	/* POST method
	 * Send an email.  A JNDI entry for the desired smtp host must exist.
	 */
	@Path("sendEmail")
	@POST
	@Produces("application/json; charset=UTF-8")  
	public synchronized String sendEmail( @HeaderParam("input") String input, @HeaderParam("authorization") String authString )
	{
		if ( ! SecurityServicesUtilities.isEmpty( authString ) && authString.contains( "Basic " ) )
		{
			authString = authString.substring( 6 );
		}
		JsonObject jsonInputObject = Json.createReader(new StringReader(input)).readObject();
		String sessionJndi = jsonInputObject.getString( "sessionJndi" );
		String from = jsonInputObject.getString( "from" );
		String to = jsonInputObject.getString( "to" );
		String subject = jsonInputObject.getString( "subject" );
		String text = jsonInputObject.getString( "text" );

	    ArrayList<String> errors = new ArrayList<String>();
	    
    	if ( authenticate( authString ) )
    	{
		    try
		    {
	    		SecurityServicesUtilities.sendEmail( sessionJndi, from, to, subject, text );
		    }
		    catch( Exception e )
		    {
		    	errors.add( "Failed to send email: " + to + ".  See stack trace." );
		    	e.printStackTrace();
		    }
    	}
        else
        {
        	System.out.println( "Bad authentication data.  Cannot process request.  Authentication failed." );
        	throw new WebApplicationException( 401 ); // Unauthorized
        }
	

	     String result = null;
	     if ( errors.size() > 0 )
	    	 result = "FAILED: " + errors.get( 0 );
	     else
	    	 result = "SUCCESS";
	     
		 JSONArray messageSendResultArray = new JSONArray();
		 JSONObject messageSendResult = new JSONObject();
		 messageSendResult.put( "result", result );
		 messageSendResultArray.add( messageSendResult );
	
	 	 return messageSendResultArray.toJSONString();
	 }

    // This method will process the rest service authentication as desired.
    private static boolean authenticate( String authString )
    {
    	boolean authenticated = false;
    	try
    	{
	   	    Context ctx = new InitialContext();
	   	    String restKey = ( String ) ctx.lookup("java:global/restKey");
	   	    if ( restKey.equals( authString ) )
	   	    	authenticated = true;
    	}
    	catch( Exception e )
    	{
    		e.printStackTrace();
    	}

    	return authenticated;
    }
}
