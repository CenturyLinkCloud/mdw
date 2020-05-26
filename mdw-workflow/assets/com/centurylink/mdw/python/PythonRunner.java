package com.centurylink.mdw.python;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.model.asset.Asset;
import org.apache.commons.lang3.ArrayUtils;
import org.python.core.Py;
import org.python.core.PySystemState;
import org.python.jsr223.PyScriptEngineFactory;

import javax.script.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PythonRunner {

    private static ScriptEngine scriptEngine;
    private static synchronized ScriptEngine getScriptEngine() throws ScriptException {
        if (scriptEngine == null) {
            PySystemState engineSys = new PySystemState();
            String assetRoot = ApplicationContext.getAssetRoot().getAbsolutePath();
            engineSys.path.append(Py.newString(assetRoot));
            Py.setSystemState(engineSys);
            scriptEngine = new PyScriptEngineFactory().getScriptEngine();
            // custom module finder for assets
            String assetPath = "com.centurylink.mdw.python/ModuleFinder.py";
            try {
                Asset finderAsset = AssetCache.getAsset(assetPath);
                Map<String, Object> values = new HashMap<>();
                values.put("assetRoot", assetRoot);
                Bindings bindings = new SimpleBindings(values);
                ScriptContext scriptContext = new SimpleScriptContext();
                scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
                scriptEngine.eval(finderAsset.getText(), scriptContext);
            } catch (IOException ex) {
                throw new ScriptException(ex);
            }
        }
        return scriptEngine;
    }

    private ScriptContext scriptContext = new SimpleScriptContext();

    public Object exec(String script, Map<String,Object> values) throws ScriptException {
        // handle return statement
        String[] lines = script.split("\\r?\\n");
        String returnExpr = null;
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if (line.startsWith("return")) {
                    returnExpr = line.substring(6).trim();
                    lines = ArrayUtils.remove(lines, i);
                    script = String.join("\n", lines);
                }
                break;
            }
        }

        Bindings bindings = new SimpleBindings(values);
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        ScriptEngine scriptEngine = getScriptEngine();
        scriptEngine.eval(script, scriptContext);

        if (returnExpr != null) {
            return scriptEngine.eval(returnExpr, scriptContext);
        }
        else {
            return null;
        }
    }

    public Object eval(String script, Map<String,Object> values) throws ScriptException {
        Bindings bindings = new SimpleBindings(values);
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return getScriptEngine().eval(script, scriptContext);
    }
}
