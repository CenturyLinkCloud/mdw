/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.value;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableVO;

public class ProcessProxy
{
  private PackageVO packageVO;
  private ProcessVO processVO;

  public ProcessProxy(ProcessVO processVO)
  {
    this.processVO = processVO;
  }
  public ProcessProxy(PackageVO packageVO, ProcessVO processVO)
  {
    this.packageVO = packageVO;
    this.processVO = processVO;
  }

  public void generate(IFolder sourceFolder)
  {
    IFile sourceFile = generateSource(sourceFolder);
    compile(sourceFile);
  }

  private IFile generateSource(IFolder sourceFolder)
  {
    StringBuffer source = new StringBuffer();
    if (packageVO != null && !packageVO.getPackageName().equals(PackageVO.DEFAULT_PACKAGE_NAME))
      source.append("package " + getPackageName() + ";\n\n");
    source.append("public class " + getClassName() + " {\n\n");
    for (VariableVO variableVO : processVO.getVariables())
    {
      String varType = variableVO.getVariableType();
      String varName = variableVO.getVariableName();
      String accessorPart = varName.substring(0, 1).toUpperCase() + varName.substring(1);

      source.append("  private " + varType + " " + varName + ";\n");
      source.append("  public " + varType + " get" + accessorPart + "() { return " + varName + "; }\n");
      source.append("  public void set" + accessorPart + "(" + varType + " " + varName + ") { this." + varName + " = " + varName + "; }\n");
    }

    source.append("\n}");

    IFile sourceFile = sourceFolder.getFile(getClassName() + "." + "java");
    PluginUtil.writeFile(sourceFile, source.toString(), new NullProgressMonitor());
    return sourceFile;
  }

  public String getPackageName()
  {
    return FileHelper.stripDisallowedFilenameChars(packageVO.getPackageName()).replaceAll(" ", "");
  }

  public String getClassName()
  {
    return FileHelper.stripDisallowedFilenameChars(processVO.getProcessName()).replaceAll(" ", "");
  }

  private void compile(IFile sourceFile)
  {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

//    JavaFileObject file = new JavaSourceFromString(getClassName(), source);
//
//    Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
//    CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);



    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    List<File> sourceFileList = new ArrayList <File>();
    sourceFileList.add(sourceFile.getRawLocation().toFile());
    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFileList);
    CompilationTask task = compiler.getTask(null, fileManager, null, null, null, compilationUnits);
    boolean success = task.call();

    for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
      System.out.println(diagnostic.getCode());
      System.out.println(diagnostic.getKind());
      System.out.println(diagnostic.getPosition());
      System.out.println(diagnostic.getStartPosition());
      System.out.println(diagnostic.getEndPosition());
      System.out.println(diagnostic.getSource());
      System.out.println(diagnostic.getMessage(null));

    }
    System.out.println("\nSUCCESS: " + success);
  }

  class JavaSourceFromString extends SimpleJavaFileObject
  {
    final String code;

    JavaSourceFromString(String name, String code)
    {
      super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
      this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
    {
      return code;
    }
  }
}
