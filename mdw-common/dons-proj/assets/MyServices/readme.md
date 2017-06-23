 ## Package MyServices

The examples here are described in the
MDW [Services Cookbook](http://cshare.ad.qintra.com/sites/MDW/Developer%20Resources/Tutorials/ServicesCookbook.html).

#### HTTP GET URL to access the Employees service:
http://localhost:8080/mdw/services/MyServices/Employees/dxoakes

#### HTTP POST URL for Employees service:
http://localhost:8080/mdw/services/MyServices/Employees

#### JSON Content for Employee Update:
```
{
  "cuid": "aa56486",
  "name": "Manoj Agrawal",
  "attributes": {
    "Email": "manoj.agrawal@centurylink.com",
    "Phone": "303 992 9980"
  }
}
```

#### JAX-RS Documentation:
 - https://jax-rs-spec.java.net/
 - http://docs.oracle.com/javaee/6/tutorial/doc/giepu.html
 
#### Swagger Documentation
 - http://swagger.io/
 - http://swagger.io/specification/
 - https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X