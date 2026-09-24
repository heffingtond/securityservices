package core;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import beans.AuthenticationProfileBean;

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
	
	public static String getSecurityServicesProfile( AuthenticationProfileBean profile )
	{
		String status = null;
		
		Connection connection = null;
		try
		{
			connection = getJndiConnection( "SECURITY_MYSQL_DB" );
			if ( connection != null )
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
		        	profile.setMobilePhone( resultSet.getString("mobile_phone") );
		        	profile.setOfficePhone( resultSet.getString("office_phone") );
		        	profile.setOfficePhoneExt( resultSet.getString("office_phone_ext") );
		        	profile.setHomePhone( resultSet.getString("home_phone") );
		        	profile.setFailedLoginAttempts( resultSet.getInt( "failed_login_attempts" ) );
		        	profile.setVerificationCodeMethod( resultSet.getString("verification_code_method") );
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
		
		return status;
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
}
