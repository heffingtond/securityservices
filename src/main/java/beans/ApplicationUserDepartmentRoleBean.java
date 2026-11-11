package beans;

public class ApplicationUserDepartmentRoleBean
{
	private int applicationUserDepartmentRoleId;
	private int organizationId;
	private int authenticationProfileId;
	private int applicationId;
	private int departmentId;
	private int roleId;
	private String clientResult;

	
	
	public int getApplicationUserDepartmentRoleId()
	{
		return applicationUserDepartmentRoleId;
	}

	public void setApplicationUserDepartmentRoleId(int applicationUserDepartmentRoleId)
	{
		this.applicationUserDepartmentRoleId = applicationUserDepartmentRoleId;
	}

	public int getOrganizationId()
	{
		return organizationId;
	}

	public void setOrganizationId(int organizationId)
	{
		this.organizationId = organizationId;
	}

	public int getAuthenticationProfileId()
	{
		return authenticationProfileId;
	}
	
	public void setAuthenticationProfileId(int authenticationProfileId)
	{
		this.authenticationProfileId = authenticationProfileId;
	}
	
	public int getApplicationId()
	{
		return applicationId;
	}
	
	public void setApplicationId(int applicationId)
	{
		this.applicationId = applicationId;
	}
	
	public int getDepartmentId()
	{
		return departmentId;
	}
	
	public void setDepartmentId(int departmentId)
	{
		this.departmentId = departmentId;
	}
	
	public int getRoleId()
	{
		return roleId;
	}
	
	public void setRoleId(int roleId)
	{
		this.roleId = roleId;
	}

	public String getClientResult()
	{
		return clientResult;
	}

	public void setClientResult(String clientResult)
	{
		this.clientResult = clientResult;
	}
}
