package core;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import beans.SecurityProfileBean;

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
	
	// Should not need this
	private static Connection getManualConnection() throws Exception
	{
		String url = "jdbc:mysql://localhost:3306/APPLICATION_SECURITY?allowPublicKeyRetrieval=true";
	    Properties info = new Properties();
	    info.put("user", "root");
	    info.put("password", "rootpswd");
	    Connection connection = DriverManager.getConnection( url,info );
	    return connection;
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
	
	public static String getSecurityServicesProfile( SecurityProfileBean profile )
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
		        	profile.setUserId( resultSet.getString("user_id") );
		        	profile.setAuthenticationProfileId( resultSet.getInt("authentication_profile_id") );
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
}
