package beans;

public class RoleBean
{
	private int roleId;
	private int organizationId;
	private int applicationId;
	private String roleName;
	private String roleDescription;

	public int getRoleId()
	{
		return roleId;
	}
	
	public void setRoleId(int roleId)
	{
		this.roleId = roleId;
	}
	
	public int getOrganizationId()
	{
		return organizationId;
	}

	public void setOrganizationId(int organizationId)
	{
		this.organizationId = organizationId;
	}

	public int getApplicationId()
	{
		return applicationId;
	}
	
	public void setApplicationId(int applicationId)
	{
		this.applicationId = applicationId;
	}
	
	public String getRoleName()
	{
		return roleName;
	}
	
	public void setRoleName(String roleName)
	{
		if ( roleName != null )
			roleName = roleName.trim();
		this.roleName = roleName;
	}
	
	public String getRoleDescription()
	{
		return roleDescription;
	}
	
	public void setRoleDescription(String roleDescription)
	{
		this.roleDescription = roleDescription;
	}
}
