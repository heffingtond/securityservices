package beans;

public class DepartmentBean
{
	private int departmentId;
	private int organizationId;
	private String departmentName;
	private String departmentCode;
	private String departmentDescription;
	
	public int getDepartmentId()
	{
		return departmentId;
	}
	
	public void setDepartmentId(int departmentId)
	{
		this.departmentId = departmentId;
	}
	
	public int getOrganizationId()
	{
		return organizationId;
	}
	
	public void setOrganizationId(int organizationId)
	{
		this.organizationId = organizationId;
	}
	
	public String getDepartmentName()
	{
		return departmentName;
	}
	
	public void setDepartmentName(String departmentName)
	{
		this.departmentName = departmentName;
	}
	
	public String getDepartmentCode()
	{
		return departmentCode;
	}
	
	public void setDepartmentCode(String departmentCode)
	{
		if ( departmentCode != null )
			departmentCode = departmentCode.trim();
		this.departmentCode = departmentCode;
	}
	
	public String getDepartmentDescription()
	{
		return departmentDescription;
	}
	
	public void setDepartmentDescription(String departmentDescription)
	{
		this.departmentDescription = departmentDescription;
	}
}
