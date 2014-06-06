package org.oki.transmodel.hhtsanalysis;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Modifier;
import java.net.UnknownHostException;

import com.esri.arcgis.datasourcesGDB.FileGDBWorkspaceFactory;
import com.esri.arcgis.geodatabase.FeatureClassDescription;
import com.esri.arcgis.geodatabase.Fields;
import com.esri.arcgis.geodatabase.GeometryDef;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureClassDescription;
import com.esri.arcgis.geodatabase.IFeatureWorkspace;
import com.esri.arcgis.geodatabase.IFeatureWorkspaceProxy;
import com.esri.arcgis.geodatabase.IField;
import com.esri.arcgis.geodatabase.IFieldEdit;
import com.esri.arcgis.geodatabase.IFields;
import com.esri.arcgis.geodatabase.IFieldsEdit;
import com.esri.arcgis.geodatabase.IGeometryDef;
import com.esri.arcgis.geodatabase.IGeometryDefEdit;
import com.esri.arcgis.geodatabase.IObjectClassDescription;
import com.esri.arcgis.geodatabase.IWorkspace;
import com.esri.arcgis.geodatabase.IWorkspaceFactory;
import com.esri.arcgis.geodatabase.esriFeatureType;
import com.esri.arcgis.geodatabase.esriFieldType;
import com.esri.arcgis.geometry.IProjectedCoordinateSystem;
import com.esri.arcgis.geometry.ISpatialReference;
import com.esri.arcgis.geometry.ISpatialReferenceFactory;
import com.esri.arcgis.geometry.SpatialReferenceEnvironment;
import com.esri.arcgis.geometry.esriGeometryType;
import com.esri.arcgis.geometry.esriSRProjCSType;
import com.esri.arcgis.geoprocessing.GeoProcessor;
import com.esri.arcgis.system.AoInitialize;
import com.esri.arcgis.system.EngineInitializer;
import com.esri.arcgis.system.esriLicenseProductCode;
import com.esri.arcgis.system.esriLicenseStatus;

public class ExportLines {
	static AoInitialize aoInit;
	static IWorkspace workspace;

	public static void main(String[] args) {
		EngineInitializer.initializeEngine();
		initializeArcGISLicenses();
		try {
			aoInit = new AoInitialize();
			if(aoInit.isProductCodeAvailable(esriLicenseProductCode.esriLicenseProductCodeEngine)==esriLicenseStatus.esriLicenseAvailable)
				aoInit.initialize(esriLicenseProductCode.esriLicenseProductCodeEngine);
			else if(aoInit.isProductCodeAvailable(esriLicenseProductCode.esriLicenseProductCodeBasic)==esriLicenseStatus.esriLicenseAvailable)
				aoInit.initialize(esriLicenseProductCode.esriLicenseProductCodeBasic);
			
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if(args.length!=2){
			System.out.println("USAGE: ExportData infile outfile\n");
			System.out.println("Where:");
			System.out.println("infile = filename of object file");
			System.out.println("outfile = output gdb path and layername");
			return;
		}
		try {
			String fileGdbPath=args[1];
			String featClassName="f"+args[1].substring(args[0].lastIndexOf("\\")+1, args[0].length()-4);
			FileInputStream fin=new FileInputStream(args[0]);
			ObjectInputStream ois=new ObjectInputStream(fin);
			Object inObj=ois.readObject();
			ois.close();
			if(inObj instanceof GPSList){
				GPSList GPS=(GPSList)inObj;
				GeoProcessor gp=new GeoProcessor();
				gp.setOverwriteOutput(true);
				
				IFields fields=new Fields();
				IFieldsEdit fieldsEdit=(IFieldsEdit) fields;
				
				int fieldCount=0;
				for(java.lang.reflect.Field f:GPSData.class.getDeclaredFields())
					if(Modifier.isPublic(f.getModifiers()))
						fieldCount++;
				fieldsEdit.setFieldCount(2+fieldCount);
				
				IField field=new com.esri.arcgis.geodatabase.Field();
				IFieldEdit fieldEdit= (IFieldEdit) field;
				
				// Geometry Definition
				IGeometryDef geometryDef = new GeometryDef();
			    IGeometryDefEdit geometryDefEdit = (IGeometryDefEdit)geometryDef;
			    geometryDefEdit.setGeometryType(esriGeometryType.esriGeometryPoint);
			    geometryDefEdit.setGridCount(1);
			    geometryDefEdit.setGridSize(0, 0);
			    geometryDefEdit.setHasM(false);
			    geometryDefEdit.setHasZ(false);
			    
			    // Spatial Reference
			    ISpatialReferenceFactory isrf = new SpatialReferenceEnvironment();
			    IProjectedCoordinateSystem pcs=isrf.createProjectedCoordinateSystem(esriSRProjCSType.esriSRProjCS_NAD1983SPCS_OHSouthFT); 
			    ISpatialReference spatialReference = pcs;
			    if (spatialReference != null){
			        geometryDefEdit.setSpatialReferenceByRef(spatialReference);
			    }
			    
			    // Shape Field
			    fieldEdit.setName("SHAPE");
			    fieldEdit.setType(esriFieldType.esriFieldTypeGeometry);
			    fieldEdit.setGeometryDefByRef(geometryDef);
			    fieldEdit.setIsNullable(true);
			    fieldEdit.setRequired(true);
			    fieldsEdit.setFieldByRef(0, field);
			    
				// ObjectID field
			    field=new com.esri.arcgis.geodatabase.Field();
			    fieldEdit= (IFieldEdit) field;
				fieldEdit.setName("ObjectID");
				fieldEdit.setType(esriFieldType.esriFieldTypeOID);
				fieldsEdit.setFieldByRef(1, field);
				
				// Other fields
				int fieldCnt=2;
				for(java.lang.reflect.Field ff:GPSData.class.getDeclaredFields()){
					if(Modifier.isPublic(ff.getModifiers())){
						field=new com.esri.arcgis.geodatabase.Field();
					    fieldEdit= (IFieldEdit) field;
					    fieldEdit.setName(ff.getName());
						if(ff.getType().toString().toLowerCase().matches("double")){
							fieldEdit.setType(esriFieldType.esriFieldTypeDouble);
							fieldEdit.setScale(20);
							fieldEdit.setPrecision(10);							
						}else if(ff.getType().toString().toLowerCase().matches("int")){
							fieldEdit.setType(esriFieldType.esriFieldTypeInteger);
							
						}else if(ff.getType().toString().toLowerCase().contains("date")){
							fieldEdit.setType(esriFieldType.esriFieldTypeDate);
						}else{ 
							fieldEdit.setType(esriFieldType.esriFieldTypeString);
							fieldEdit.setLength(40);
						}
						fieldsEdit.setFieldByRef(fieldCnt, field);
						fieldCnt++;
					}
				}
		
				workspace=null;
			
				IWorkspaceFactory wsF=new FileGDBWorkspaceFactory();
				try{
					workspace=wsF.openFromFile(fileGdbPath, 0);
				}catch(Exception ex){
					ex.printStackTrace();
				}
				
				IFeatureClassDescription fcDesc=new FeatureClassDescription();
				IObjectClassDescription ocDesc= (IObjectClassDescription) fcDesc;
				IFeatureWorkspace featWs = new IFeatureWorkspaceProxy(workspace);
				IFeatureClass featClass=featWs.createFeatureClass(featClassName, fields, ocDesc.getInstanceCLSID(), ocDesc.getClassExtensionCLSID(), esriFeatureType.esriFTSimple, fcDesc.getShapeFieldName(), "");
				
				int counter=0;
				
			} //if instanceof GPSList
		}catch(Exception ex){
			
		}
	} //void main
	
	static void initializeArcGISLicenses() {
		try {
			com.esri.arcgis.system.AoInitialize ao = new com.esri.arcgis.system.AoInitialize();
			if (ao.isProductCodeAvailable(com.esri.arcgis.system.esriLicenseProductCode.esriLicenseProductCodeEngine) == com.esri.arcgis.system.esriLicenseStatus.esriLicenseAvailable)
				ao.initialize(com.esri.arcgis.system.esriLicenseProductCode.esriLicenseProductCodeEngine);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
