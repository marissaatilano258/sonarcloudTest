package com.uml.parser.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.uml.parser.model.UMLClass;
import com.uml.parser.model.UMLMethod;
import com.uml.parser.model.UMLVariable;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;

public class ParseJava {
	private List<UMLClass> umlClasses;
	private Counselor counselor;
	
	public ParseJava() {
		umlClasses = new ArrayList<>();
		counselor = Counselor.getInstance();
	}
	
	public List<UMLClass> parseFiles(List<File> files){
		try{
			for(File file : files){
				System.out.println(file.getAbsolutePath());
				CompilationUnit compliationUnit = JavaParser.parse(file);
				createElements(compliationUnit);
			}
		}catch(FileNotFoundException ex){
			System.err.println("Error: File not found. Trace: "+ ex.getMessage());
		}catch(IOException ex){
			System.err.println("Error: IO Exception. Trace: "+ ex.getStackTrace());
		}catch(ParseException ex){
			System.err.println("Error: Parse exception. Trace: "+ ex.getStackTrace());
		}
		
		return umlClasses;
	}
	
	private void createElements(CompilationUnit compliationUnit){
		List<TypeDeclaration> types = compliationUnit.getTypes();
		for(TypeDeclaration type : types){
			UMLClass umlClass = new UMLClass();
			List<BodyDeclaration> bodyDeclarations = type.getMembers();
			boolean isInterface = ((ClassOrInterfaceDeclaration) type).isInterface();
			umlClass.setName(type.getName());
			umlClass.setInterface(isInterface);
			System.out.println("Name "+ umlClass.getName());
			
			counselor.checkForRelatives(umlClass, type);
			
			for(BodyDeclaration body : bodyDeclarations){
				if(body instanceof FieldDeclaration){
					createUMLVariables(umlClass, (FieldDeclaration) body);
				}else if(body instanceof MethodDeclaration){
					createUMLMethods(umlClass, (MethodDeclaration) body, false);
				}else if(body instanceof ConstructorDeclaration){
					createUMLMethods(umlClass, (ConstructorDeclaration) body, true);
				}
			}
			umlClasses.add(umlClass);
		}
	}
	
	private void createUMLVariables(UMLClass umlClass, FieldDeclaration field){
		List<VariableDeclarator> variables = field.getVariables();
		for(VariableDeclarator variable : variables){
			UMLVariable umlVariable = new UMLVariable();
			umlVariable.setModifier(field.getModifiers());
			umlVariable.setName(variable.getId().getName());
			umlVariable.setInitialValue(variable.getInit() == null ? "" : " = " + variable.getInit().toString());
			umlVariable.setUMLClassType(UMLHelper.isUMLClassType(field.getType()));
			umlVariable.setType(field.getType());
			umlClass.getUMLVariables().add(umlVariable);
		}
		counselor.checkForRelatives(umlClass, field);
	}
	
	private void createUMLMethods(UMLClass umlClass, BodyDeclaration body, boolean isConstructor){
		UMLMethod umlMethod = new UMLMethod();
		if(isConstructor){
			ConstructorDeclaration constructor = (ConstructorDeclaration) body;
			umlMethod.setConstructor(true);
			umlMethod.setModifier(constructor.getModifiers());
			umlMethod.setName(constructor.getName());
			umlMethod.setParameters(constructor.getParameters());
		}else {
			MethodDeclaration method = (MethodDeclaration) body;
			umlMethod.setConstructor(false);
			umlMethod.setModifier(method.getModifiers());
			umlMethod.setName(method.getName());
			umlMethod.setParameters(method.getParameters());
			umlMethod.setType(method.getType());
		}
		umlClass.getUMLMethods().add(umlMethod);
		counselor.checkForRelatives(umlClass, umlMethod);
	}
}