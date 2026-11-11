package beans;

public class ApplicationBean
{
	private int applicationId;
	private int organizationId;
	private String applicationName;
	private String applicationCode;
	private String domainName;
	private String applicationDescription;
	private String owner;

	public int getApplicationId()
	{
		return applicationId;
	}
	
	public void setApplicationId(int applicationId)
	{
		this.applicationId = applicationId;
	}
	
	public int getOrganizationId()
	{
		return organizationId;
	}
	
	public void setOrganizationId(int organizationId)
	{
		this.organizationId = organizationId;
	}
	
	public String getApplicationName()
	{
		return applicationName;
	}
	
	public void setApplicationName(String applicationName)
	{
		if ( applicationName != null )
			applicationName = applicationName.trim();
		this.applicationName = applicationName;
	}
	
	public String getApplicationCode()
	{
		return applicationCode;
	}
	
	public void setApplicationCode(String applicationCode)
	{
		if ( applicationCode != null )
			applicationCode = applicationCode.trim();
		this.applicationCode = applicationCode;
	}
	
	public String getDomainName()
	{
		return domainName;
	}
	
	public void setDomainName(String domainName)
	{
		this.domainName = domainName;
	}
	
	public String getApplicationDescription()
	{
		return applicationDescription;
	}
	
	public void setApplicationDescription(String applicationDescription)
	{
		this.applicationDescription = applicationDescription;
	}
	
	public String getOwner()
	{
		return owner;
	}
	
	public void setOwner(String owner)
	{
		this.owner = owner;
	}
}
