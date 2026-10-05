package core;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.json.simple.JSONObject;

import beans.AuthenticationProfileBean;
import beans.VerificationCodeBean;

public class SecurityServicesUtilities
{
	public static boolean isEmpty( String inString )
	{
		boolean isEmpty = false;
		if ( inString == null )
			isEmpty = true;
		else
		{
			inString = inString.trim();
			if ( inString.length() == 0 )
				isEmpty = true;
		}
		return isEmpty;
	}
	
	public static Connection getJndiConnection( String datasourceName ) throws Exception
	{
		Context initContext = new InitialContext();
	    DataSource ds = ( DataSource ) initContext.lookup( "java:jboss/datasources/" + datasourceName);
	    if ( ds == null )
	    	System.out.println( "ds NEW lookup not found" );
	    else
	    	System.out.println( "ds NEW lookup FOUND!" );
	    Connection connection = ds.getConnection();
	    if ( connection != null )
	    	System.out.println( "connection successful" );
	    else
	    	System.out.println( "connection NULL" );
	    	
	    return connection;
	}
	
	public static void getSecurityServicesProfile( AuthenticationProfileBean profile, Connection connection )
	{
		try
		{
			String sql = 
					"select * from APPLICATION_SECURITY.AUTHENTICATION_PROFILE"
				  + " where user_id = ?";
			PreparedStatement preparedStatement = null;
	        ResultSet resultSet = null;
	        preparedStatement = connection.prepareStatement( sql );
	        preparedStatement.setString( 1, profile.getUserId() );
	        resultSet = preparedStatement.executeQuery();
	        if ( resultSet.next() )
	        {
	        	profile.setAuthenticationProfileId( resultSet.getInt("authentication_profile_id") );
	        	profile.setOrganizationId( resultSet.getInt("organization_id") );
	        	profile.setUserId( resultSet.getString("user_id") );
	        	profile.setPassword( resultSet.getString("password") );
	        	profile.setSalt( resultSet.getString("salt") );
	        	profile.setFirstName( resultSet.getString("first_name") );
	        	profile.setLastName( resultSet.getString("last_name") );
	        	profile.setEmail( resultSet.getString("email") );
	        	profile.setMobilePhone( resultSet.getString("mobile_phone") );
	        	profile.setOfficePhone( resultSet.getString("office_phone") );
	        	profile.setOfficePhoneExt( resultSet.getString("office_phone_ext") );
	        	profile.setHomePhone( resultSet.getString("home_phone") );
	        	profile.setFailedLoginAttempts( resultSet.getInt( "failed_login_attempts" ) );
	        	profile.setVerificationCodeMethod( resultSet.getString("verification_code_method") );
	        }
	        resultSet.close();
	        preparedStatement.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static TreeMap<String,ArrayList<String>> getUserRoles( String userId, String applicationCode )
	{
        TreeMap<String,ArrayList<String>> allDepartments = new TreeMap<String,ArrayList<String>>();
		Connection connection = null;
		try
		{
			connection = getJndiConnection( "SECURITY_MYSQL_DB" );
			if ( connection != null )
			{
				String sql = 
						"SELECT d.department_code, r.role_name " 
					  +	"FROM DEPARTMENT d, ROLE r "
					  +	"RIGHT JOIN APPLICATION a                              ON a.application_id = r.application_id " 
					  +	"RIGHT JOIN APPLICATION_USER_DEPARTMENT_ROLE audr      ON a.application_id = audr.application_id "
					  +	"RIGHT JOIN AUTHENTICATION_PROFILE ap                  ON ap.authentication_profile_id = audr.authentication_profile_id "
					  +	"RIGHT JOIN ORGANIZATION o                             ON o.organization_id = ap.organization_id "
					  +	"WHERE d.department_id = audr.department_id "
					  +	"AND   d.organization_id = o.organization_id "
					  +	"AND   r.application_id = a.application_id "
					  +	"AND   audr.role_id = r.role_id "			
					  +	"AND   a.application_cd = ? "
					  +	"AND   ap.user_id = ? "
					  + "ORDER BY d.department_code";
				PreparedStatement preparedStatement = null;
		        ResultSet resultSet = null;
		        preparedStatement = connection.prepareStatement( sql );
		        preparedStatement.setString( 1, applicationCode );
		        preparedStatement.setString( 2, userId );
		        resultSet = preparedStatement.executeQuery();

		        ArrayList<String> rolesForDepartment = null;

		        String saveDepartment = null;
		        String currentDepartment = null;
		        String currentRole = null;
		        int pass = 0;
		        while ( resultSet.next() )
		        {
		        	pass++;
		        	currentDepartment = resultSet.getString("department_code");
		        	System.out.println( "current dept " + currentDepartment );
		        	if ( pass == 1 )
		        	{
		        		saveDepartment = currentDepartment;
		        		rolesForDepartment = new ArrayList<String>();
		        	}
		        	currentRole = resultSet.getString("role_name");
		        	System.out.println( "current role " + currentRole );
		        	if ( saveDepartment.equals( currentDepartment ) )
		        		rolesForDepartment.add( currentRole );
		        	else
		        	{
		        		allDepartments.put( saveDepartment, rolesForDepartment );
		        		rolesForDepartment = new ArrayList<String>();
		        		rolesForDepartment.add( currentRole );
		        		saveDepartment = currentDepartment;
		        	}
		        }
		        if ( rolesForDepartment.size() > 0 ) // Pick up the last record
		        {
	        		allDepartments.put( currentDepartment, rolesForDepartment );
		        }
		        resultSet.close();
		        preparedStatement.close();
		        connection.close();
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		
		System.out.println( "SecurityServicesUtilities: dump all departments right after query." );
        for ( Map.Entry<String, ArrayList<String>> entry : allDepartments.entrySet() ) 
        {
        	System.out.println( "department: " + entry.getKey() );
            for( String role : entry.getValue() )
            {
                System.out.println( "role: " + role );
            }
        }

		return allDepartments;
	}
	
    /**
     * Compares two byte arrays in constant time. 
     * This prevents attackers from guessing the hash byte-by-byte based on system response times.
     */
    private static boolean slowEquals( byte[] a, byte[] b ) 
    {
        if ( a.length != b.length ) 
        {
            return false;
        }
        int diff = a.length ^ b.length;
        for ( int i = 0; i < a.length; i++ ) 
        {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
    
    /**
     * Authenticates the user-entered password against the stored password details.
     * 
     * @param enteredPassword The raw password provided by the user during login.
     * @param storedSaltBase64 The Base64 encoded salt extracted from the database.
     * @param storedHashBase64 The Base64 encoded hash extracted from the database.
     * @param iterations The iteration count extracted from the database (e.g., 600000).
     * @return true if the passwords match, false otherwise.
     */
    public static boolean authenticateUserLogin( String enteredPassword, 
    		                                     String storedSaltBase64, 
    		                                     String storedHashBase64 ) 
    {
		final int ITERATIONS = 10000;
	    final int KEY_LENGTH = 256;
	    final String ALGORITHM = "PBKDF2WithHmacSHA256";

        try 
        {
            // 1. Decode the stored salt and hash from Base64
            byte[] salt = Base64.getDecoder().decode(storedSaltBase64);
            byte[] storedHash = Base64.getDecoder().decode(storedHashBase64);

            // 2. Hash the user-entered password using the exact same parameters
            char[] passwordChars = enteredPassword.toCharArray();
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] enteredHash = factory.generateSecret(spec).getEncoded();

            // 3. Clear the password character array from memory immediately
            spec.clearPassword();

            // 4. Perform a constant-time comparison to prevent timing attacks
            return slowEquals(storedHash, enteredHash);
        } 
        catch ( NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e ) 
        {
            // Handle exceptions appropriately (e.g., log the error)
        	e.printStackTrace();
            return false;
        }
    }
    
	public static synchronized String generateAuthenticationCode()
	{
		SecureRandom random = new SecureRandom();
        // Generate a number between 0 and 999999
        int number = random.nextInt( 1000000 ); 
        
        // Format to ensure 6 digits with leading zeros if necessary
        String code = String.format("%06d", number);
        
        System.out.println("Your 6-digit code: " + code);
        return code;
	}

	// This method gets an existing Verification Code for the natural PK: authentication_profile_id + application id
	public static VerificationCodeBean getVerificationCode( int authenticationProfileId,
			   												Connection connection ) throws Exception
	{
		String sql = 
		"select * from APPLICATION_SECURITY.VERIFICATION_CODE "
	  + "where authentication_profile_id = ? ";
		PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        preparedStatement = connection.prepareStatement( sql );
        preparedStatement.setInt( 1, authenticationProfileId );
        resultSet = preparedStatement.executeQuery();
        
        VerificationCodeBean verificationCode = null;
        if ( resultSet.next() )
        {
        	verificationCode = new VerificationCodeBean();
        	verificationCode.setVerificationCodeId( resultSet.getInt("verification_code_id") );
        	verificationCode.setAuthenticationProfileId( resultSet.getInt("authentication_profile_id") );
			verificationCode.setVerificationCode( resultSet.getString("verification_cd") );
			verificationCode.setCreatedTimestamp( resultSet.getString("created_ts") );
        }
        resultSet.close();
        preparedStatement.close();
        
        return verificationCode;
	}

	public static void deleteVerificationCode( int verificationCodeId, Connection connection ) throws SQLException
	{
		String sql = "delete from APPLICATION_SECURITY.VERIFICATION_CODE "
				   + "where verification_code_id = ?";
		PreparedStatement preparedStatement = connection.prepareStatement( sql );
        preparedStatement.setInt( 1, verificationCodeId );
        preparedStatement.executeUpdate();
        preparedStatement.close();
	}

	public static void clearVerificationCodeTable( int authenticationProfileId, Connection connection ) throws SQLException
	{
		String sql = "delete from APPLICATION_SECURITY.VERIFICATION_CODE "
				   + "where authentication_profile_id = ?";
		PreparedStatement preparedStatement = connection.prepareStatement( sql );
        preparedStatement.setInt( 1, authenticationProfileId );
        preparedStatement.executeUpdate();
        preparedStatement.close();
	}

	public static synchronized void addNewVerificationCode( VerificationCodeBean verification, Connection connection ) throws SQLException
	{
		clearVerificationCodeTable( verification.getAuthenticationProfileId(), connection );
		
		String sql = "insert into APPLICATION_SECURITY.VERIFICATION_CODE "
				   + "("
				   + "authentication_profile_id, "
				   + "verification_cd, "
				   + "created_ts "
				   + ") "
				   + "values( ?,?,? )";

		
		PreparedStatement preparedStatement = connection.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS );
        preparedStatement.setInt( 1, verification.getAuthenticationProfileId() );
        preparedStatement.setString( 2, verification.getVerificationCode() );
        String currentTimestamp = SecurityServicesUtilities.getCurrentTimestamp();
        verification.setCreatedTimestamp( currentTimestamp );
        preparedStatement.setString( 3, verification.getCreatedTimestamp() );
        preparedStatement.executeUpdate();
        
        ResultSet rs = preparedStatement.getGeneratedKeys();
        if ( rs.next() )
        {
	        int newPrimaryKey = rs.getInt( 1 );
	        verification.setVerificationCodeId( newPrimaryKey );
        }
        rs.close();
        preparedStatement.close();
	}
	
	
	public static void sendCodeEmail( String emailAddress, String message )
	{
		System.out.println( message );
	}
	
	public static String sendText( String phoneNumber, String message )
	{
		String response = null;
        try 
        {
            // 1. Define the URL of the REST endpoint
        	String endpointUrl = "http://localhost/securityservices/rest/security/text";
        	System.out.println( "SecurityUtilities: Security utilities endpoint that calls third party rest service for text: " + endpointUrl );
            @SuppressWarnings("deprecation")
			URL url = new URL( endpointUrl );

            // 2. Open a connection
            HttpURLConnection connection = ( HttpURLConnection ) url.openConnection();

            // 3. Set the request method (e.g., GET, POST, PUT, DELETE)
            connection.setRequestMethod( "POST" );

            // 4. Set request headers (optional, but often necessary for content type, authorization, etc.)
            connection.setRequestProperty("Accept", "application/json");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put( "phoneNumber", phoneNumber );
            jsonObject.put( "textMessage", message );
            jsonObject.put( "endpointUrl", "https://sendtext"  );
            
            connection.setRequestProperty( "input", jsonObject.toJSONString() );
            
            // 5. Get the response code
            System.out.println( "SecurityUtilities.sendText" );
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 6. Read the response
            if ( responseCode == HttpURLConnection.HTTP_OK ) 
            {
                BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ( ( inputLine = in.readLine() ) != null ) 
                {
                    content.append( inputLine );
                }
                in.close();
                response = content.toString();
                System.out.println( "Response Body: " + content.toString() );
            } 
            else 
            {
                System.out.println( "Error in GET request: " + responseCode );
            }

            // 7. Disconnect the connection
            connection.disconnect();

        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
		return response;
	}

	
	public static String sendEmail( String email, String message )
	{
		String response = null;
        try 
        {
            // 1. Define the URL of the REST endpoint
        	String endpointUrl = "http://localhost/securityservices/rest/security/email";
        	System.out.println( "SecurityUtilities: Security utilities endpoint that calls third party rest service for email: " + endpointUrl );
            @SuppressWarnings("deprecation")
			URL url = new URL( endpointUrl );

            // 2. Open a connection
            HttpURLConnection connection = ( HttpURLConnection ) url.openConnection();

            // 3. Set the request method (e.g., GET, POST, PUT, DELETE)
            connection.setRequestMethod( "POST" );

            // 4. Set request headers (optional, but often necessary for content type, authorization, etc.)
            connection.setRequestProperty("Accept", "application/json");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put( "email", email );
            jsonObject.put( "message", message );
            jsonObject.put( "endpointUrl", "https://sendemail"  );
            
            connection.setRequestProperty( "input", jsonObject.toJSONString() );
            
            // 5. Get the response code
            System.out.println( "SecurityUtilities.sendEmail" );
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 6. Read the response
            if ( responseCode == HttpURLConnection.HTTP_OK ) 
            {
                BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ( ( inputLine = in.readLine() ) != null ) 
                {
                    content.append( inputLine );
                }
                in.close();
                response = content.toString();
                System.out.println( "Response Body: " + content.toString() );
            } 
            else 
            {
                System.out.println( "Error in GET request: " + responseCode );
            }

            // 7. Disconnect the connection
            connection.disconnect();

        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
		return response;
	}

	public static void textTheCode( String phoneNumber, String authCode )
	{
		System.out.println( "TEXT: authentication code is " + authCode );
		System.out.println( "TEXT: send text to " + phoneNumber );
		String textMessage = "Please enter this verification code when prompted to complete login: " + authCode;
		SecurityServicesUtilities.sendText( phoneNumber, textMessage );
	}

	public static void emailTheCode( String email, String authCode )
	{
		System.out.println( "EMAIL: authentication code is " + authCode );
		System.out.println( "EMAIL: send email to " + email );
		String message = "Check your email and enter the verification code when prompted to complete login: " + authCode;
		SecurityServicesUtilities.sendEmail( email, message );
	}

	public static void twoFactorAuthentication( AuthenticationProfileBean user, Connection connection ) throws Exception
	{
		// Generate a 6 digit random code.
		String code = SecurityServicesUtilities.generateAuthenticationCode();
		// Send authentication code to user via chosen method (email or text)
		// add the code to the verification code table.
		VerificationCodeBean verification = new VerificationCodeBean();
		verification.setVerificationCode( code );
		verification.setAuthenticationProfileId( user.getAuthenticationProfileId() );
		SecurityServicesUtilities.addNewVerificationCode( verification, connection );

		if ( "TEXT".equals( user.getVerificationCodeMethod() ) )
			SecurityServicesUtilities.textTheCode( user.getMobilePhone(), code );
		else
		if ( "EMAIL".equals( user.getVerificationCodeMethod() ) )
			SecurityServicesUtilities.emailTheCode( user.getEmail(), code );
	}

	public static String getCurrentTimestamp()
	{
		Instant instant = Instant.now();
		ZoneId desiredZone = ZoneId.systemDefault();
		ZonedDateTime zonedDateTime = instant.atZone(desiredZone);
		
        System.out.println("ZonedDateTime in " + desiredZone + ": " + zonedDateTime);

		String convertedTimestamp = zonedDateTime.toString();
		return convertedTimestamp;
	}
}
