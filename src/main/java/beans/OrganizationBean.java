package beans;

public class OrganizationBean
{
	private int organizationId;
	private String organizationName;
	private String organizationAddress;
	private String organizationCity;
	private String organizationState;
	private String organizationZip;
	private String organizationZipExt;
	private String primaryUrl;
	private String organizationDescription;

	public int getOrganizationId()
	{
		return organizationId;
	}
	
	public void setOrganizationId(int organizationId)
	{
		this.organizationId = organizationId;
	}
	
	public String getOrganizationName()
	{
		return organizationName;
	}
	
	public void setOrganizationName(String organizationName)
	{
		if ( organizationName != null )
			organizationName = organizationName.trim();
		this.organizationName = organizationName;
	}
	
	public String getOrganizationAddress()
	{
		return organizationAddress;
	}
	
	public void setOrganizationAddress(String organizationAddress)
	{
		this.organizationAddress = organizationAddress;
	}
	
	public String getOrganizationCity()
	{
		return organizationCity;
	}
	
	public void setOrganizationCity(String organizationCity)
	{
		this.organizationCity = organizationCity;
	}
	
	public String getOrganizationState()
	{
		return organizationState;
	}
	
	public void setOrganizationState(String organizationState)
	{
		this.organizationState = organizationState;
	}
	
	public String getOrganizationZip()
	{
		return organizationZip;
	}
	
	public void setOrganizationZip(String organizationZip)
	{
		this.organizationZip = organizationZip;
	}
	
	public String getOrganizationZipExt()
	{
		return organizationZipExt;
	}
	
	public void setOrganizationZipExt(String organizationZipExt)
	{
		this.organizationZipExt = organizationZipExt;
	}
	
	public String getPrimaryUrl()
	{
		return primaryUrl;
	}
	
	public void setPrimaryUrl(String primaryUrl)
	{
		this.primaryUrl = primaryUrl;
	}
	
	public String getOrganizationDescription()
	{
		return organizationDescription;
	}
	
	public void setOrganizationDescription(String organizationDescription)
	{
		this.organizationDescription = organizationDescription;
	}
}
