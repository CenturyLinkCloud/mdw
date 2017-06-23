package MyServices;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Employees")
@Api("CenturyLink employees service")
public class Employees extends JsonRestService {

    @Override
    @Path("/{id}")
    @ApiOperation(value="Retrieve an employee by their ID",
        notes="Currently only retrieves a single employee, and only dxoakes.",
        response=Employee.class)
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        String id = getSegment(path, 2);
        if ("dxoakes".equals(id)) {
            User emp = new User(id);
            emp.setName("Donald Oakes");
            emp.setAttribute("Email", "donald.oakes@centurylink.com");
            emp.setAttribute("Phone", "303 992 9747");
            return emp.getJson();
        }
        else {
            throw new ServiceException(HTTP_404_NOT_FOUND, "Employee not found: " + id);
        }
    }

    @Override
    @ApiOperation(value="Create an employee",
        notes="Does not actually create anything.", 
        response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Employee", paramType="body", required=true, dataType="MyServices.Employee")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        User emp = new User(content);
        String id = emp.getCuid();
        if (id == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing user id");
        if (id.equals("dxoakes"))
            throw new ServiceException(HTTP_409_CONFLICT, "Employee id exists: " + id);
        System.out.println("Creating employee (not really): " + emp.getJson().toString(2));
        return null; // null indicates successful POST
    }
}
