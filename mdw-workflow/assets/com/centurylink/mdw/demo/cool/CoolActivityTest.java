package com.centurylink.mdw.demo.cool;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.test.MockRuntimeContext;

/**
 * Dynamic Java JUnit test.
 */
public class CoolActivityTest implements java.io.Serializable {
    
    private CoolActivity coolActivity;
    private MockRuntimeContext runtimeContext;
    
    @Before
    public void setup() {
        coolActivity = new CoolActivity();
        runtimeContext = new MockRuntimeContext("Cool Activity");
        runtimeContext.getAttributes().put("coolLevel", "Very Cool");
        runtimeContext.getAttributes().put("important", "true");
        
        runtimeContext.getVariables().put("name", "Donald");
        // assigned variables need to be initialized
        runtimeContext.getVariables().put("something", null);
        runtimeContext.getVariables().put("greeting", null);
        
        // jaxb document variable
        CoolDoc doc = new CoolDoc();
        doc.setCoolRequired("iamcool");
        doc.setRequiredAttr("coolness");
        runtimeContext.getVariables().put("doc", doc);

        coolActivity.prepare(runtimeContext);
    }
    
    @Test
    public void testActivity() throws ActivityException {
        Object result = coolActivity.execute(runtimeContext);
        assertTrue(result == null);
        runtimeContext = (MockRuntimeContext)coolActivity.getRuntimeContext();
        assertTrue(runtimeContext.getVariables().get("something").equals("something"));
        assertTrue(runtimeContext.getVariables().get("greeting").equals("Hello, Donald"));
        
        CoolDoc doc = (CoolDoc)runtimeContext.getVariables().get("doc");
        assertTrue("coolness".equals(doc.getRequiredAttr())); // unchanged
        assertTrue("iamcooler".equals(doc.getCoolRequired()));
        assertTrue("coolness_optional".equals(doc.getOptionalAttr()));
    }
    
}
