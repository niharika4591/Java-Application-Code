package edu.uic.ids517.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.io.*;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.jsp.jstl.sql.Result;
import javax.servlet.jsp.jstl.sql.ResultSupport;

@ManagedBean
@SessionScoped
public class ActionBean implements Serializable, Cloneable {
	
	private static final long serialVersionUID = 1L;
	
	//Beans
	private UserBean userBean;
	private AssessmentBean assessmentBean;
	private AssessmentInfoBean assessmentInfoBean;
	private RosterBean rosterBean;
	private TrackBean trackBean;
	private CourseBean courseBean;
	private DbAccess dbAccess;
	private DataBaseBean dataBaseBean;
	private GradeBean gradeBean;
	private ChartBean chartBean;
	//ArrayLists
	private ArrayList<AssessmentBean> assessmentList = new ArrayList<AssessmentBean>();	
	private ArrayList<RosterBean> rosterList=new ArrayList<RosterBean>();
	private ArrayList<TrackBean> trackList = new ArrayList<TrackBean>();
	private ArrayList<String> arrayAssessTables=new ArrayList<String>();
	private ArrayList<String> arrayRosterTables=new ArrayList<String>();
	private ArrayList<String> arrayCourses=new ArrayList<String>();
	private ArrayList<String> arrayCRN=new ArrayList<String>();
	private ArrayList<String> arrayUsers=new ArrayList<String>();
	private ArrayList<QuizAttemptBean> quizAttemptList = new ArrayList<QuizAttemptBean>();
	private ArrayList<CourseBean> courseList=new ArrayList<CourseBean>();
	private ArrayList<GradeBean> gradeByUser=new ArrayList<GradeBean>();
	private ArrayList<GradeBean> gradeByAssess=new ArrayList<GradeBean>();
	private ArrayList<AssessmentBean> assBeanList = new ArrayList<AssessmentBean>();
	private ArrayList<StatsBean> statsList = new ArrayList<StatsBean>();
	private ArrayList<StatsBean> statsListUser = new ArrayList<StatsBean>();

	
	private boolean loggedIn=false;
	private String selectedAssess;
	private String selectedRoster;
	private String selectedUser;
	private String selectedCourse;
	private String selectedCRN=null;
	private String rosterName;
	private String applicationPath;
	private String errorMessage;
	private String query;
	private String option = "upload";
	private boolean okEnable = true;
	private boolean allEnable = true;
	private boolean uploadEnable = true;
	private boolean selectEnable=true;
	
	//render variables
	private boolean renderedAssessList=false;
	private boolean renderedRosterList=false;
	private boolean renderedCourseList=false;
	private boolean renderedQuizList=false;
	private boolean renderedTrackList=false;
	private boolean renderedSummary=false;
	private boolean renderAssessment=false;
	private boolean renderPrevious=false;
	private boolean renderGrade=false;
	private boolean renderError=false;
	private boolean renderedGradeUser=false;
	private boolean renderedGradeAssess=false;
	private boolean renderedSummaryUser=false;
	
	//file variables
	 private UploadedFile uploadedFile;
	 private String uploadedFileContents = null;
	 private String fileName;
	 private String fileLabel;
	 private String fileExtension;
	 private String fileType;
	 private String grade=null;
	 
	 //course info
	 private String courseId;
	 private String section;
	 
	 //Quiz variables
	 private int tries;
	 private String correctAnswer;
	 
	 //Default constructor
	 public ActionBean() {}
	
	 //clone method
	 public ActionBean clone() throws CloneNotSupportedException {
		ActionBean cloned = (ActionBean) super.clone();
		return cloned;
		}
	
	 @PostConstruct
	 public void init() 
	 {
		FacesContext context = FacesContext.getCurrentInstance();
		//getting application path
		applicationPath = context.getExternalContext().getRealPath("/content/");
		//instantiating DbAccess
		Map <String, Object> d = context.getExternalContext().getSessionMap();
		dbAccess = (DbAccess) d.get("dbAccess");
		//instantiating DataBaseBean
		Map <String, Object> db = context.getExternalContext().getApplicationMap();
		dataBaseBean = (DataBaseBean) db.get("dataBaseBean");
		//instantiating UserBean
		Map <String, Object> u = context.getExternalContext().getSessionMap();
		userBean = (UserBean) u.get("userBean");
		//instantiating AssessmentBean
		Map <String, Object> a = context.getExternalContext().getSessionMap();
		assessmentBean = (AssessmentBean) a.get("assessmentBean");
		//instantiating AssessmentInfoBean
		Map <String, Object> ai = context.getExternalContext().getSessionMap();
		assessmentInfoBean = (AssessmentInfoBean) ai.get("assessmentInfoBean");
		//instantiating TrackBean
		Map <String, Object> t = context.getExternalContext().getSessionMap();
		trackBean = (TrackBean) t.get("trackBean");
		//instantiating RosterBean
		Map <String, Object> r = context.getExternalContext().getSessionMap();
		rosterBean = (RosterBean) r.get("rosterBean");
		Map <String, Object> ch = context.getExternalContext().getSessionMap();
		chartBean = (ChartBean) ch.get("chartBean");
	}
	
	//Calling DbAccess to connect to the database
	public String connect()
	{	
		renderError=false;
		errorMessage=null;
		//checks whether connection is made
		if(dbAccess.dbConnect().equals("Login"))
		{
			//Checks for database login
			if (loggedIn==false)
			{
				//calls method to create tables
				if(dbAccess.createTables().equals("Success"))
				{
					loggedIn=true;
					return "Login";
				}
				else
				{
					renderError=true;
					errorMessage = "Cannot connect to the database";
					return "Failure";
				}
			}
			else
				return "Login";
		}
		else
		{
			renderError=true;
			errorMessage = "Cannot connect to the database";
			return "Failure";
		}
	}
		
	//Validate the user
	public String validateUser()
	{
		if(!loggedIn)
			//session ends on logout - need to connect to database again
			connect();
		
		//ensures that error message is not visible
		errorMessage=null;
		renderError=false;
		
		//flag to enable navigation if credentials are correct
		boolean flag=false;
		String userRole=null;
		
		query = "SELECT * FROM login_user";
		try
		{
			if (dbAccess.executeSelect(query).equals("Success"))
			{
				while(dbAccess.getResultSet().next())
				{
					if ((dbAccess.getResultSet().getString(1).equals(userBean.getUserName())) && (dbAccess.getResultSet().getString(2).equals(userBean.getPassword())) && (dbAccess.getResultSet().getString(5).equals(userBean.getRole())))
					{
						//Getting user details from database
						userRole=dbAccess.getResultSet().getString(5);
						userBean.setFirstName(dbAccess.getResultSet().getString(3));
						userBean.setLastName(dbAccess.getResultSet().getString(4));
						//Enable navigation to respective page
						flag=true;
						break;
					}
				}
				if (!flag)
				{
					query="SELECT * FROM roster_user";
					//Check the roster users if no record is found in default table
					if (dbAccess.executeSelect(query).equals("Success"))
					{
						while(dbAccess.getResultSet().next())
						{
							if ((dbAccess.getResultSet().getString(1).equals(userBean.getUserName())) && (dbAccess.getResultSet().getString(2).equals(userBean.getPassword())) && (dbAccess.getResultSet().getString(5).equals(userBean.getRole())))
							{
								//Getting user details from database
								userRole=dbAccess.getResultSet().getString(5);
								userBean.setFirstName(dbAccess.getResultSet().getString(3));
								userBean.setLastName(dbAccess.getResultSet().getString(4));
								//Enable navigation to respective page
								flag=true;
								break;
							}
						}
					}
				}
			}
			if (flag==true)
			{
				//Gets courses from database
				getCourses();
				//stores login timestamp in track_user table
				trackLogin();
				return userRole;
			}
			else
			{
				errorMessage="The entries do not match the records";
				renderError=true;
				return "Failure";
			}
		}
		catch(SQLException e)
		{
			errorMessage="The entries do not match the records";
			renderError=true;
			return "Failure";
		}
	}
				
	
	//stores login timestamp in track_user table
	public String trackLogin()
	{
		trackBean.generateInTime();
		trackBean.getUserIP();
		query = "INSERT INTO track_user VALUES (" + "'" + userBean.getUserName() + "'" + ","
					+ "'" + trackBean.getInTime() + "'" + ","
					+ "null,"
					+ "'" + trackBean.getIpAddress() + "'" + ","
					+ "null)";
		if(dbAccess.executeUpdate(query).equals("Success"))
			return "Success";
		else
			return "Failure";
	}
	
	//display tracking info
	public String displayTrack()
	{
		trackList.clear();
		query = "SELECT * FROM track_user";
		try
		{
			//getting records from DbAccess
			if(dbAccess.executeSelect(query).equals("Success"))
			{
				//inserting in arraylist
				while(dbAccess.getResultSet().next())
				{
					TrackBean track = new TrackBean();
					track.setUserid(dbAccess.getResultSet().getString(1));
					track.setInTime(dbAccess.getResultSet().getString(2));
					track.setOutTime(dbAccess.getResultSet().getString(3));
					track.setIpAddress(dbAccess.getResultSet().getString(4));
					trackList.add(track);
				}
				renderedTrackList=true;
			}
			return "Success";
		}
		catch(SQLException e)
		{
			return "Failure";
		}
	}

	//Uploads file at the application path
	public String uploadFile()
	{
		renderError=false;
		errorMessage = null;
		try 
		{
			//Ensures that the uploaded file is in csv format
			if(FilenameUtils.getExtension(uploadedFile.getName()).equals("csv"))
			{
				fileLabel = FilenameUtils.getBaseName(uploadedFile.getName());
				fileName = FilenameUtils.getName(uploadedFile.getName());
				uploadedFileContents = new String(uploadedFile.getBytes());
				File tempFile = new File(applicationPath + fileName);
				FileOutputStream fos = new FileOutputStream(tempFile);
				fos.write(uploadedFile.getBytes());
				fos.close();
				selectEnable = false;
				renderError=true;
				errorMessage = "File has been selected";
				return "Success";
			}
			else
			{
				renderError=true;
				errorMessage = "Please select a file in csv format";
				return "Failure";
			}
		} 
		catch (Exception e)
		{
				renderError=true;
				errorMessage = "No file Selected";
				return "Failure";
		}
	}
	
	//displays fields required according to the file type - assessment/roster 
	public String chooseFileType()
	{
		renderError = false;
		errorMessage =null;
			if (fileType.equals("assessment"))
		{
			//check if file name exists already
			query = "SELECT table_name FROM assessment_record where course_id = " + "'" + selectedCourse + "'";
			dbAccess.executeSelect(query);
			
			try
			{
					//number of rows
					dbAccess.getResultSet().last();
					int size = dbAccess.getResultSet().getRow();
					dbAccess.getResultSet().beforeFirst();
		    
					int j=0;
					String[] tableName = new String[size];
			
					while(dbAccess.getResultSet().next())
					{
						tableName[j]=dbAccess.getResultSet().getString(1);
						j++;
					}
			
					String tempName = selectedCourse + "_" + selectedCRN + "_" + fileLabel.toLowerCase();
					for(int i=0;i<size;i++)
					{
						if (tempName.equals(tableName[i]))
						{
							renderError = true;
							errorMessage = "A file exists with the entered name. If you choose to proceed, it will be replaced";
						}
					}
			}catch(SQLException e)
			{}
			renderAssessment=true;
			uploadEnable=false;
		}
		else
		{
			if(checkRoster())
			{
				renderError = true;
				errorMessage = "Roster exists for this course. If you choose to proceed, it will be replaced";
			}
			uploadEnable=false;
			renderAssessment = false;
		}
			renderError = true;
			errorMessage = "Click upload to save file";
			return "Success";
	}
		

	//choose import options
	public String importFile()
	{
		renderError = false;
		errorMessage = null;
		if (fileType.equals("assessment"))
		{
			importAssessment();
			return "Success";
		}
		else if (fileType.equals("roster"))
		{
			//a combination of course and CRN can have only one roster
			checkRoster();
			importRoster();
			return "Success";
		}
		else
		{
			return "Failure";
		}
	}
	
	//Validates assessment information entered
	public boolean validateInput(AssessmentInfoBean assessInfo)
	{
		if(assessInfo.getAssessmentNumber().equals(null) || assessInfo.getQuestionScore()==0 || assessInfo.getEndDate().equals(null) || assessInfo.getAssessmentTime()==0 )
		{
			return false;
		}
		else
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try 
			{
					sdf.setLenient(false);
					sdf.parse(assessInfo.getEndDate());
					return true;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				return false;
			}
		}
	}
	
	
	//Imports the uploaded assessment into database
	public String importAssessment() 
	{
		renderError=false;
		errorMessage=null;
		
		//name for the database file
		String tempName = selectedCourse + "_" + selectedCRN + "_" + fileLabel.toLowerCase();
		
		//files can only be imported in the group database schema
		if ((dataBaseBean.getDbUsername().equals("f15g118")) && (dataBaseBean.getDbPassword().equals("f15g118XRcdP")))
		{
			try
			{
				AssessmentInfoBean assessInfo = assessmentInfoBean.clone();
				
				//validate input
				if(validateInput(assessInfo))
				{
				boolean flag=true;
				
				//check for duplicate assessment_number value
				query = "SELECT table_name, assessment_number FROM assessment_record where course_id = " + "'" + selectedCourse + "'";
				dbAccess.executeSelect(query);
				
					//number of rows
					dbAccess.getResultSet().last();
					int size = dbAccess.getResultSet().getRow();
					dbAccess.getResultSet().beforeFirst();
			    
					int j=0;
					String[] tableName = new String[size];
					String[] assessNumber = new String[size];
				
					while(dbAccess.getResultSet().next())
					{
						tableName[j]=dbAccess.getResultSet().getString(1);
						assessNumber[j] = dbAccess.getResultSet().getString(2);
						j++;
					}
					for(int i=0;i<size;i++)
					{
						if(assessInfo.getAssessmentNumber().equals(assessNumber[i]))
						{
						//assessment_number entered in the form already exists for that course
							flag=false;
							break;
						}
					}	
				if (flag)
				{
					//deletes file with same name
					query = "DROP TABLE IF EXISTS " + tempName;
					dbAccess.executeUpdate(query);
					
					//Read uploaded csv file
					String csvFile = applicationPath+ fileName;
					FileReader file= new FileReader(csvFile);
					BufferedReader br = new BufferedReader(file);
					String line = "";
					line = br.readLine();
					String arrayHeader [] = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				
					//Create assessment table
					query = "create table "+ tempName + " (";
					for(int i=0;i<arrayHeader.length;i++)
					{	
						if (i != arrayHeader.length -1)
							query = query + arrayHeader[i].replace("'","\\'").replace("\"", "").replace(" ", "").replace("(+/-)", "").replace("#", "_number") + " varchar(250),";	
						else
							query = query + arrayHeader[i].replace("'", "\\'").replace("\"", "").replace(" ", "").replace("(+/-)", "").replace("#", "_number") + " varchar(250))";					
					}
					dbAccess.executeUpdate(query);
			
					//Inserting data
					while ((line=br.readLine()) != null) 
					{
						String arrayValue[] = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
						query = "insert into "+ tempName + " values(";
						for(int i=0;i<arrayValue.length;i++)
						{	
							if(i!= arrayValue.length-1)	
								query = query + "'"+arrayValue[i].replace("'", "\\'").replace("\"", "")+"'" + ",";	
							else
								query = query + "'"+arrayValue[i].replace("'", "\\'").replace("\"", "")+"'" + ")";
						}
						dbAccess.executeUpdate(query);
					}
				
					//updating assessment record
					for(int i=0;i<size;i++)
					{
						if (tempName.equals(tableName[i]))
						{
							query = "DELETE FROM assessment_record where table_name = " + "'" + tableName[i] + "'" + " and course_id = " + "'" + selectedCourse + "'";
							dbAccess.executeUpdate(query);
						}
					}
				
					query = "INSERT INTO assessment_record VALUES ("
						+ "'" + tempName + "'" + ","
						+ "'" + assessInfo.getAssessmentNumber() + "'" + ","
						+ assessInfo.getAssessmentTime() + ","
						+ "'" + assessInfo.getEndDate() + "'" + ","
						+ assessInfo.getQuestionScore() + ","
						+ "'" + selectedCourse + "'" + ","
						+ "'" + selectedCRN + "'" + ")";
					dbAccess.executeUpdate(query);
								
					//Getting the column and row count of uploaded table
					query = "SELECT * FROM " + tempName;
					dbAccess.executeSelect(query);
					dbAccess.getResultSet().last();
					int rowsCount = dbAccess.getResultSet().getRow();
					int columnCount = dbAccess.getResultSetMetaData().getColumnCount();
					
					errorMessage = "Your file has been uploaded with " + rowsCount + " rows and " + columnCount + " columns.";
					renderError = true;
					
					//Adding a column in corresponding roster for the uploaded assessment
					String selectRoster = "select table_name from roster_record where course_id = '" + selectedCourse +"' and section = '" + selectedCRN +"'";
					dbAccess.executeSelect(selectRoster);
					String rosterName = null;
					while(dbAccess.getResultSet().next())
					{
						rosterName = dbAccess.getResultSet().getString(1);
						break;
					}
					if(rosterName!=null)
					{
						String rosterData = "select * from " + rosterName;
						dbAccess.executeSelect(rosterData);
						int numCols = dbAccess.getResultSetMetaData().getColumnCount();
						boolean assessColumn = false;
						for(int i=0;i<numCols;i++)
						{
							if (assessInfo.getAssessmentNumber().equals(dbAccess.getResultSet().getMetaData().getColumnName(i+1)))
							{
								//column with the same name exists
								assessColumn=true;
								break;
							}
						}
						//drops the same name column and add a new one
						if(assessColumn)
						{
							String dropColumn = "ALTER TABLE " + rosterName + " " + "DROP COLUMN " + assessInfo.getAssessmentNumber();
							dbAccess.executeUpdate(dropColumn);
						}
						String updateRoster = "ALTER TABLE " + rosterName + " " + "ADD (" + assessInfo.getAssessmentNumber() + " varchar(30))";
						dbAccess.executeUpdate(updateRoster);
						String updateValues = "UPDATE " + rosterName + " SET " + assessInfo.getAssessmentNumber() + " = 'Not yet taken'";
						dbAccess.executeUpdate(updateValues);
					}
					assessInfo.clearValues();
					return "Success";
				}
				else
				{
					errorMessage = "Assessment Number already exists";
					renderError = true;
					return "Failure";
				}
				}
				else
				{
					errorMessage = "Enter correct values for all fields";
					renderError = true;
					return "Failure";
				}
			}	
			catch(SQLException e)
			{
				errorMessage = "Error in uploading file";
				renderError = true;
				return "Failure";
			}
			catch(FileNotFoundException e)
			{
				errorMessage = "Error in uploading file";
				renderError = true;
				return "Failure";
			}
			catch(IOException e)
			{
				errorMessage = "Error in uploading file";
				renderError = true;
				return "Failure";
			}
			catch(CloneNotSupportedException e)
			{
				errorMessage = "Error in uploading file";
				renderError = true;
				return "Failure";
			}
		}
		else
		{
			errorMessage = "File cannot be imported to the connected database";
			renderError = true;
			return "Failure";
		}
	}	
	
	//Checks whether a roster exists for a particular class(course+section)
	public boolean checkRoster()
	{
		query = "SELECT table_name from roster_record where course_id = " + "'" + selectedCourse + "' and section = '" + selectedCRN + "'";
		try
		{
		dbAccess.executeSelect(query);
		if(!dbAccess.getResultSet().next())
			return false;
		else
			return true;
		}catch(SQLException e)
		{
			return true;
		}
	}
	
	public boolean checkUsers()
	{
		query = "SELECT NetID FROM " + selectedCourse + "_" + selectedCRN + "_" + fileLabel.toLowerCase();
		dbAccess.executeSelect(query);
		ArrayList<String> userIds = new ArrayList<String>();
		try
		{
		while(dbAccess.getResultSet().next())
		{
			userIds.add(dbAccess.getResultSet().getString(1));
		}
		boolean flag=true;;
		//check whether student exists in the came course with different crn
		String getEnrollUsers = "SELECT net_id,course_id FROM enroll where type='roster'";
		dbAccess.executeSelect(getEnrollUsers);
		while(dbAccess.getResultSet().next())
		{
			for (int count=0;count<userIds.size();count++)
			{
			if(userIds.get(count).equals(dbAccess.getResultSet().getString(1))&& selectedCourse.equals(dbAccess.getResultSet().getString(2)))
			{
				renderError=true;
				errorMessage = "A student cannot be enrolled in two sections of the same course";
				flag=false;
				break;
			}
			if(!flag)
				break;
			}
		}
		if(!flag)
		{
			String dropQuery = "DROP TABLE IF EXISTS " +selectedCourse + "_" + selectedCRN + "_" + fileLabel.toLowerCase();
			dbAccess.executeUpdate(dropQuery);
		}
		return flag;
		}catch(SQLException e)
		{
			return false;
		}
	}
	
	//imports the uploaded roster into database
	public String importRoster() 
	{
		if ((dataBaseBean.getDbUsername().equals("f15g118")) && (dataBaseBean.getDbPassword().equals("f15g118XRcdP")))
		{
			try
			{
				String selectQuery = "SELECT table_name FROM roster_record where course_Id = " + "'" + selectedCourse + "' and section = '" + selectedCRN + "'";
				dbAccess.executeSelect(selectQuery);
				String tableName=null;
				while(dbAccess.getResultSet().next())
				{
					tableName = dbAccess.getResultSet().getString(1);
					break;
				}
			    String tempName = selectedCourse + "_" + selectedCRN + "_" + fileLabel.toLowerCase();
				
				if(checkRoster())
				{
					//drop existing roster
					String dropRoster = "DROP TABLE IF EXISTS " + tableName;
					dbAccess.executeUpdate(dropRoster);
					query = "DELETE FROM roster_record where table_name = " + "'" + tableName + "'" + " and course_id = " + "'" + selectedCourse + "'";
					dbAccess.executeUpdate(query);
				}
				//Read csv
				String csvFile = applicationPath+ fileName;
			    FileReader file= new FileReader(csvFile);
				BufferedReader br = new BufferedReader(file);
				String line = "";
				line = br.readLine();
				String arrayHeader [] = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

				//Create roster table
				String createTable = "create table "+ tempName + "(";
				for(int i=0;i<arrayHeader.length;i++)
				{	
					if (i != arrayHeader.length -1)
						createTable = createTable + arrayHeader[i].replace("'", "\\'").replace("\"", "") + " varchar(50),";	
					else
						createTable = createTable + arrayHeader[i].replace("'", "\\'").replace("\"", "") + " varchar(50))";					
				}
				dbAccess.executeUpdate(createTable);
				
				//Inserting data
				while ((line=br.readLine()) != null) 
				{
					String arrayValue[] = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
					String insertInto = "insert into "+ tempName + " values(";
					for(int i=0;i<arrayValue.length;i++)
					{	
							if(i!= arrayValue.length-1)	
							insertInto = insertInto + "'"+arrayValue[i].replace("'", "\\'").replace("\"", "")+"'" + ",";	
							else 
							insertInto = insertInto + "'"+arrayValue[i].replace("'", "\\'").replace("\"", "")+"'" + ")";
					}
					dbAccess.executeUpdate(insertInto);
				}
				br.close();
				
				if(checkUsers())
				{
				//Updating the roser_record table	
				String updateRecord = "INSERT INTO roster_record VALUES (" + "'" + tempName + "'" + "," + "'" + selectedCourse + "'" + "," + "'" + selectedCRN + "')";
				dbAccess.executeUpdate(updateRecord);
				
				//inserting users in the uploaded roster into roster_user to allow login for them
				rosterUsers();
				
				//Getting the column and row count of uploaded table
				query = "SELECT * FROM " + tempName;
				dbAccess.executeSelect(query);
				dbAccess.getResultSet().last();
			    int rowsCount = dbAccess.getResultSet().getRow();
			    int columnCount = dbAccess.getResultSetMetaData().getColumnCount();
				errorMessage = "Your file has been uploaded with " + rowsCount + " rows and " + columnCount + " columns.";
				renderError = true;
				return "Success";
				}
				else
				{
					return "Failure";
				}
			}
			catch(SQLException e)
			{
				errorMessage = "Error in uploading file";
				renderError = true;
				return "Failure";
			}
			catch(FileNotFoundException e)
			{
				errorMessage = "Error in uploading file";
				renderError = true;
				return "Failure";
			}
			catch(IOException e)
			{
				errorMessage = "Error in uploading file";
				renderError = true;
				return "Failure";
			}
			}
			else
			{
				errorMessage = "File cannot be imported to the connected database";
				renderError = true;
				return "Failure";
			}

		}	
		
		//Create roster Login
		public String rosterUsers()
		{
			try
			{	
				query = "SELECT * FROM " + selectedCourse + "_" + selectedCRN + "_" + fileLabel.toLowerCase();
				dbAccess.executeSelect(query);
				int columns = dbAccess.getResultSetMetaData().getColumnCount();
				String [] header = new String[columns];
				for(int i=0;i<columns;i++)
				{
					header[i]=dbAccess.getResultSetMetaData().getColumnName(i+1);
				}
				String firstName = null;
				String lastName = null;
				String userId = null;
				String role = null;
				
				for(int i=0;i<header.length;i++)
				{
					if (header[i].toLowerCase().contains("first"))
					{
						firstName = header[i];
					}
					else if (header[i].toLowerCase().contains("last"))
					{
						lastName = header[i];
					}
					else if (header[i].toLowerCase().contains("net"))
					{
						userId = header[i];
					}
					else if (header[i].toLowerCase().contains("role"))
					{
						role = header[i];
					}
				}
				String selectUser = "SELECT " + userId + " FROM " + selectedCourse + "_" + selectedCRN + "_" + fileLabel.toLowerCase();
				dbAccess.executeSelect(selectUser);
				
				//getting row count
				dbAccess.getResultSet().last();
			    int size = dbAccess.getResultSet().getRow();
			    dbAccess.getResultSet().beforeFirst();
				
				String [] userIds = new String[size];
				int j=0;
				
				while (dbAccess.getResultSet().next())
				{
					userIds[j] = dbAccess.getResultSet().getString(1);
					j++;
				}
				
				//delete duplicate values from enroll and enter new ones
				for(int i=0;i<size;i++)
				{
					String enrollDelete = "DELETE FROM enroll where net_id = " + "'" + userIds[i] + "' and course_id = '" + selectedCourse + "' and crn = '" + selectedCRN +"' and type = 'roster'";
					dbAccess.executeUpdate(enrollDelete);
					String enterEnroll = "INSERT INTO enroll VALUES ('" + userIds[i] + "','" + selectedCourse + "','" + selectedCRN + "','roster')";
					dbAccess.executeUpdate(enterEnroll);
				}
				//delete from roster_user
				for(int i=0;i<size;i++)
				{
					String deleteQuery = "DELETE FROM roster_user WHERE net_id in ('" + userIds[i] + "')";
					dbAccess.executeUpdate(deleteQuery);
				}
				
				String selectQuery = "SELECT " + firstName + "," + lastName + "," + userId + "," + role + " FROM " + selectedCourse + "_" + selectedCRN + "_" + fileLabel.toLowerCase();
				dbAccess.executeSelect(selectQuery);
				
				
				ArrayList<UserBean> userList = new ArrayList<UserBean>();
				//insert into login_user
				dbAccess.getResultSet().beforeFirst();
				while (dbAccess.getResultSet().next())
				{	
					UserBean users = new UserBean();
					users.setFirstName(dbAccess.getResultSet().getString(1));
					users.setLastName(dbAccess.getResultSet().getString(2));
					users.setPassword(dbAccess.getResultSet().getString(1).toLowerCase());
					users.setRole(dbAccess.getResultSet().getString(4).toLowerCase());
					users.setUserName(dbAccess.getResultSet().getString(3).toLowerCase());
					userList.add(users);
				}
				
				for(int i=0;i<userList.size();i++)
				{
					query = "INSERT INTO roster_user VALUES ("
							+ "'" + userList.get(i).getUserName().toLowerCase() + "'" + ","
							+ "'" + userList.get(i).getFirstName().toLowerCase() + "'" + ","
							+ "'" + userList.get(i).getFirstName() + "'" + ","
							+ "'" + userList.get(i).getLastName() + "'" + ","
							+ "'" + userList.get(i).getRole().toLowerCase() + "')";
					dbAccess.executeUpdate(query);
				}
				return "Success";
			}
			catch(SQLException e)
			{
				return "Failure";
			}
		}
	
		//get tables in arraylist
		public String getAssessTables()
		{
			arrayAssessTables.clear();
			try
			{
				query = "select distinct assessment_number from assessment_record where course_id = " + "'" + selectedCourse + "'";
				dbAccess.executeSelect(query);
				//ArrayList<String> arrayAssessTables=new ArrayList<String>();
				while (dbAccess.getResultSet().next()) 
				{
					arrayAssessTables.add(dbAccess.getResultSet().getString(1));
				}
				return "Success";
			}
			catch(SQLException e)
			{
				return "Failure";
			}
		}
		
		//get courses for ta and instructor
		public String getCourses()
		{
			arrayAssessTables.clear();
			arrayRosterTables.clear();
			arrayCourses.clear();
			try
			{
			if(userBean.getRole().equals("ta"))
				query = "select distinct course_id from courses where teaching_assistant = " + "'" + userBean.getFirstName() + " " + userBean.getLastName() + "'";
			if(userBean.getRole().equals("instructor"))
				query = "select distinct course_id from courses where instructor = " + "'" + userBean.getFirstName() + " " + userBean.getLastName() + "'";
			if(userBean.getRole().equals("student"))
			{
				query = "select distinct course_id from enroll where net_id = '" + userBean.getUserName() +"' and type = 'roster'";
				dbAccess.executeSelect(query);
				if(!dbAccess.getResultSet().next())
					query = "select distinct course_id from enroll where net_id = '" + userBean.getUserName() +"' and type = 'default'";
				}
				dbAccess.executeSelect(query);
				while(dbAccess.getResultSet().next())
				{
					arrayCourses.add(dbAccess.getResultSet().getString(1));
				}
				return "Success";
			}catch(SQLException e)
			{
				return "Failure";
			}
		}
		
		//get Users from roster
		public String getUsers()
		{
			arrayUsers.clear();
			query = "select distinct net_id from enroll where course_id = '" + selectedCourse + "' and crn = '" + selectedCRN + "' and type = 'roster'";
			dbAccess.executeSelect(query);
			try
			{
			if(!dbAccess.getResultSet().next())
			{	
				query = "select distinct net_id from enroll where course_id = '" + selectedCourse + "' and crn = '" + selectedCRN + "' and type = 'default'";
				dbAccess.executeSelect(query);
			}
			while(dbAccess.getResultSet().next())
			{
				arrayUsers.add(dbAccess.getResultSet().getString(1));
			}
			getAssessTables();
			allEnable=false;
			return "Success";
			}catch(SQLException e)
			{
				return "Failure";
			}
			
		}
		
		//get CRN list
				public String getCRN()
				{
					arrayCRN.clear();
					query = "SELECT distinct CRN from courses where course_id = " + "'" + selectedCourse + "'";
					try
					{
					dbAccess.executeSelect(query);
					while(dbAccess.getResultSet().next())
					{
						arrayCRN.add(dbAccess.getResultSet().getString(1));
					}
					okEnable=false;
					selectEnable=false;
					return "Success";
					}catch(SQLException e)
					{
						return "Failure";
					}
				}
				
		public String getRosterTables()
		{
			arrayRosterTables.clear();
			try
			{
				query = "select distinct table_name from roster_record where course_id = " + "'" + selectedCourse + "'";
				dbAccess.executeSelect(query);
				//ArrayList<String> arrayAssessTables=new ArrayList<String>();
				while (dbAccess.getResultSet().next()) 
				{
					arrayRosterTables.add(dbAccess.getResultSet().getString(1));
				}
				return "Success";
			}
			catch(SQLException e)
			{
				return "Failure";
			}
		}
		
		//Get assessments and rosters for courses selected
		public String getTables()
		{
			getAssessTables();
			getRosterTables();
			selectEnable=false;
			return "Success";
		}
		
	
		//Export Roster
		public String downloadRoster()
		{
			errorMessage = null;
			renderError = false;
			
			FileOutputStream fos = null; 
			String fileName = selectedRoster+"."+fileExtension;//applicationPath + "\\" + "Assessment.csv";	
			File f = new File(fileName);
	
			//getting data from table
			String exportQuery = "select * from " + selectedRoster;
			dbAccess.executeSelect(exportQuery);
			try
			{
			if(!dbAccess.getResultSet().next())
			{
				renderError = true;
				errorMessage = "No file selected for download";
				return "Failure";
			}
			else
			{
			Result result = ResultSupport.toResult(dbAccess.getResultSet());

			Object [][] sData = result.getRowsByIndex();
			StringBuffer sb = new StringBuffer();
			  
			try
			{
				fos = new FileOutputStream(fileName);
				ArrayList<String> columnNames = new ArrayList<String>();
				int noOfCols = dbAccess.getResultSetMetaData().getColumnCount();
				 
				for(int i=0;i<noOfCols;i++) 
				{
					columnNames.add(dbAccess.getResultSetMetaData().getColumnName(i+1));   
				}
				  
				for(int i=0; i<columnNames.size(); i++) {
				  
				sb.append(columnNames.get(i)+ ",");
				  
				}
				  
				sb.append("\n");
				  
				fos.write(sb.toString().getBytes());
				for(int i = 0; i < sData.length; i++) 
				{  
					sb = new StringBuffer();
					for(int j=0; j<sData[0].length; j++) 
					{
						sb.append(sData[i][j].toString() + ",");
					}
					sb.append("\n");
					fos.write(sb.toString().getBytes());
				}
				fos.flush();
				fos.close();
			} 
			catch (Exception e) { }
			
			//downloading file
			FacesContext fc = FacesContext.getCurrentInstance();
			ExternalContext ec = fc.getExternalContext();
			String mimeType = ec.getMimeType(fileName);
			FileInputStream in = null;
			byte b;
			ec.responseReset();
			ec.setResponseContentType(mimeType);
			ec.setResponseContentLength((int) f.length());
			ec.setResponseHeader("Content-Disposition","attachment; filename=\"" +fileName + "\"");
			  
			try 
			{  
				in = new FileInputStream(f); 
				OutputStream output = ec.getResponseOutputStream();
				while(true) 
				{ 
					b = (byte) in.read();
					if(b < 0)
						break;
					output.write(b);
				}
			}
			  catch (Exception e) {} 
			finally 
			{  
				try 
				{
					in.close();
				}
				catch (Exception e) { }
			} 
			fc.responseComplete();			  
			return "Success";
			}
			}
			catch(SQLException e)
			{
				renderError = true;
				errorMessage = "Error in downloading file";
				return "Failure";
			}
		}

		//Export Assessment
				public String downloadAssessment()
				{
					errorMessage = null;
					renderError = false;
			
					//getting data from table
					String getAssess = "select table_name from assessment_record where assessment_number = " + "'" + selectedAssess + "' and course_id = '" + selectedCourse + "'" ;
					dbAccess.executeSelect(getAssess);
					try
					{
						if(!dbAccess.getResultSet().next())
						{
							renderError = true;
							errorMessage = "No file selected for download";
							return "Failure";
						}
						else
						{
						dbAccess.getResultSet().beforeFirst();
						dbAccess.getResultSet().next();
						String assessName = dbAccess.getResultSet().getString(1);
						String exportQuery = "select * from " + assessName;
						dbAccess.executeSelect(exportQuery);
					
						FileOutputStream fos = null; 
						String fileName = assessName +"."+fileExtension;
						File f = new File(fileName);
					Result result = ResultSupport.toResult(dbAccess.getResultSet());

					Object [][] sData = result.getRowsByIndex();
					StringBuffer sb = new StringBuffer();
					  
					try
					{
						fos = new FileOutputStream(fileName);
						ArrayList<String> columnNames = new ArrayList<String>();
						int noOfCols = dbAccess.getResultSetMetaData().getColumnCount();
						 
						for(int i=0;i<noOfCols;i++) 
						{
							columnNames.add(dbAccess.getResultSetMetaData().getColumnName(i+1));   
						}
						  
						for(int i=0; i<columnNames.size(); i++) {
						  
						sb.append(columnNames.get(i)+ ",");
						  
						}
						  
						sb.append("\n");
						  
						fos.write(sb.toString().getBytes());
						for(int i = 0; i < sData.length; i++) 
						{  
							sb = new StringBuffer();
							for(int j=0; j<sData[0].length; j++) 
							{
								sb.append(sData[i][j].toString() + ",");
							}
							sb.append("\n");
							fos.write(sb.toString().getBytes());
						}
						fos.flush();
						fos.close();
					} 
					catch (Exception e) { }
					
					//downloading file
					FacesContext fc = FacesContext.getCurrentInstance();
					ExternalContext ec = fc.getExternalContext();
					String mimeType = ec.getMimeType(fileName);
					FileInputStream in = null;
					byte b;
					ec.responseReset();
					ec.setResponseContentType(mimeType);
					ec.setResponseContentLength((int) f.length());
					ec.setResponseHeader("Content-Disposition","attachment; filename=\"" +fileName + "\"");
					  
					try 
					{  
						in = new FileInputStream(f); 
						OutputStream output = ec.getResponseOutputStream();
						while(true) 
						{ 
							b = (byte) in.read();
							if(b < 0)
								break;
							output.write(b);
						}
					}
					  catch (Exception e) {} 
					finally 
					{  
						try 
						{
							in.close();
						}
						catch (Exception e) { }
					} 
					fc.responseComplete();			  
					return "Success";	
				}
					}	
				catch(SQLException e)
				{
					renderError = true;
					errorMessage = "Error in downloading file";
					return "Failure";
				}
				}

		
		//Display Assessment table
		public String displayAssessment()
		{
			assessmentList.clear();
			errorMessage = null;
			renderError = false;
			try
			{
				//getting data from table
				String getAssess = "select table_name from assessment_record where assessment_number = " + "'" + selectedAssess + "' and course_id = '" + selectedCourse + "'" ;
				dbAccess.executeSelect(getAssess);
					if(!dbAccess.getResultSet().next())
					{
						renderError = true;
						errorMessage = "No file selected for display";
						return "Failure";
					}
					else
					{
					String assessName = dbAccess.getResultSet().getString(1);
					String selectQuery = "select * from " + assessName;
				dbAccess.executeSelect(selectQuery);
				int columns = dbAccess.getResultSetMetaData().getColumnCount();
				//inserting header into an array
				String[] header = new String[columns];
				for(int i=0;i<columns;i++)
				{
					header[i]=dbAccess.getResultSetMetaData().getColumnName(i+1);
				}
				while(dbAccess.getResultSet().next())
				{
					AssessmentBean assess = new AssessmentBean();
					for(int i=0;i<columns;i++)
					{
						if(header[i].toLowerCase().contains("question") && (header[i].toLowerCase().contains("number") || header[i].toLowerCase().contains("#")))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								assess.setQuestionNumber("NA");
							else
							{
								assess.setQuestionNumber(dbAccess.getResultSet().getString(header[i]));
							}
						}
						else if (header[i].toLowerCase().equals("question") || (header[i].toLowerCase().contains("actual") && header[i].toLowerCase().contains("question")))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								assess.setActualQuestion("NA");
							else
								assess.setActualQuestion(dbAccess.getResultSet().getString(header[i]));
						}
						else if (header[i].toLowerCase().contains("answer"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								assess.setAnswer("NA");
							else
							{
								assess.setAnswer(dbAccess.getResultSet().getString(header[i]));
							}
						}
						else if (header[i].toLowerCase().contains("error") || header[i].toLowerCase().contains("tolerance"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								assess.setTolerance(0);
							else
								assess.setTolerance(Double.valueOf(dbAccess.getResultSet().getString(header[i])));
						}
						else if (header[i].toLowerCase().contains("type"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								assess.setQuestionType("NA");
							else
								assess.setQuestionType(dbAccess.getResultSet().getString(header[i]));
						}
						else {}
					}
					assessmentList.add(assess);
				}
				
				//Getting assessment information
				query = "SELECT * FROM assessment_record WHERE assessment_number = " + "'" + selectedAssess + "'" + " and course_id = " + "'" + selectedCourse + "'";
				dbAccess.executeSelect(query);
				dbAccess.getResultSet().next();
				//AssessmentInfoBean assessInfo = new AssessmentInfoBean();
				assessmentInfoBean.setAssessmentNumber(dbAccess.getResultSet().getString("assessment_number"));
				assessmentInfoBean.setAssessmentTime(dbAccess.getResultSet().getInt("assessment_time"));
				assessmentInfoBean.setEndDate(dbAccess.getResultSet().getString("end_date"));
				assessmentInfoBean.setQuestionScore(dbAccess.getResultSet().getDouble("question_score"));
				courseId = dbAccess.getResultSet().getString("course_Id");
				section = dbAccess.getResultSet().getString("section");
				
				renderedAssessList = true;
				return "Success";
			}
			}
			catch(SQLException e)
			{
				errorMessage = "Error in displaying assessment table";
				renderError = true;
				return "Failure";
			}
		}
		
		//Display roster table
		public String displayRoster()
		{
			rosterList.clear();
			errorMessage = null;
			renderError = false;
			try
			{
				
				String selectQuery = "Select * from " + selectedRoster;
				dbAccess.executeSelect(selectQuery);
				if(!dbAccess.getResultSet().next())
				{
					renderError = true;
					errorMessage = "No file selected for display";
					return "Failure";
				}
				else
				{

				int columns = dbAccess.getResultSetMetaData().getColumnCount();
				//inserting header into an array
				String[] header = new String[columns];
				for(int i=0;i<columns;i++)
				{
					header[i]=dbAccess.getResultSetMetaData().getColumnName(i+1);
				}
				while(dbAccess.getResultSet().next())
				{
					RosterBean roster = new RosterBean();
					for(int i=0;i<columns;i++)
					{
						if(header[i].toLowerCase().contains("last"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								roster.setLastName("NA");
							else
								roster.setLastName(dbAccess.getResultSet().getString(header[i]));
						}
						else if (header[i].toLowerCase().contains("first"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								roster.setFirstName("NA");
							else
								roster.setFirstName(dbAccess.getResultSet().getString(header[i]));
						}
						else if (header[i].toLowerCase().contains("net"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								roster.setNetID("NA");
							else
								roster.setNetID(dbAccess.getResultSet().getString(header[i]));
						}
						else if (header[i].toLowerCase().contains("role"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								roster.setRole("NA");
							else
								roster.setRole(dbAccess.getResultSet().getString(header[i]));
						}
						else if (header[i].toLowerCase().contains("status"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								roster.setStatus("NA");
							else
								roster.setStatus(dbAccess.getResultSet().getString(header[i]));
						}
						else if (header[i].toLowerCase().contains("uin"))
						{
							if(dbAccess.getResultSet().getString(header[i]).length()==0)
								roster.setUIN("NA");
							else
								roster.setUIN(dbAccess.getResultSet().getString(header[i]));
						}
						else {}
						
					}
					rosterList.add(roster);
				}
				
				//getting roster info
				query = "SELECT * FROM roster_record WHERE table_name = " + "'" + selectedRoster + "'" + " and course_id = " + "'" + selectedCourse + "'";
				dbAccess.executeSelect(query);
				dbAccess.getResultSet().next();
				courseId = dbAccess.getResultSet().getString("course_Id");
				section = dbAccess.getResultSet().getString("section");
				
				renderedRosterList=true;
				return "Success";
			}
			}
			catch(Exception e)
			{
				errorMessage = "Error in displaying roster table";
				renderError = true;
				return "Failure";
			}
		}
	
		//Drop Assessment
		public String dropAssessment()
		{
			renderError = false;
			errorMessage = null;
			if ((dataBaseBean.getDbUsername().equals("f15g118")) && (dataBaseBean.getDbPassword().equals("f15g118XRcdP")))
			{
				//getting data from table
				String getAssess = "select table_name from assessment_record where assessment_number = " + "'" + selectedAssess + "' and course_id = '" + selectedCourse + "'" ;
				dbAccess.executeSelect(getAssess);
				try
				{
					if(!dbAccess.getResultSet().next())
					{
						renderError = true;
						errorMessage = "No file selected";
						return "Failure";
					}
					else
					{
					dbAccess.getResultSet().beforeFirst();
					dbAccess.getResultSet().next();
					String assessName = dbAccess.getResultSet().getString(1);

				String dropQuery = "DROP TABLE IF EXISTS " + assessName;
				dbAccess.executeUpdate(dropQuery);
				
				//delete table from record
				String dropRecord = "DELETE FROM assessment_record where assessment_number = '" + selectedAssess + "'" + " and course_id = " + "'" + selectedCourse + "'";
				dbAccess.executeUpdate(dropRecord);
				
				//fetch updated list
				getAssessTables();
				getRosterTables();
				
				//Hide datatable
				renderedAssessList=false;
				
				return "Success";
			}
				}catch(SQLException e)
				{
					renderError = true;
					errorMessage = "Error in deleting table";
					return "Failure";
				}
			}
			else
			{
				renderError = true;
				errorMessage = "Not authorized to drop table";
				return "Failure";
			}
		}
		
		//Drop Roster
				public String dropRoster()
				{
					renderError = false;
					errorMessage = null;
					if ((dataBaseBean.getDbUsername().equals("f15g118")) && (dataBaseBean.getDbPassword().equals("f15g118XRcdP")))
					{
					try
					{
						//drop users from login table
						dbAccess.executeSelect("SELECT * FROM " + selectedRoster);
						int columns = dbAccess.getResultSetMetaData().getColumnCount();
						String [] header = new String[8];
						for(int i=0;i<columns;i++)
						{
							header[i]=dbAccess.getResultSetMetaData().getColumnName(i+1);
						}
						String userId = null;
						for(int i=0;i<header.length;i++)
						{
							if  (header[i].toLowerCase().contains("net"))
							{
								userId = header[i];
								break;
							}
						}
						String selectUser = "SELECT " + userId + " FROM " + selectedRoster;
						dbAccess.executeSelect(selectUser);
						
						//getting row count
						dbAccess.getResultSet().last();
					    int size = dbAccess.getResultSet().getRow();
					    dbAccess.getResultSet().beforeFirst();
						
						String [] userIds = new String[size];
						int j=0;
						
						while (dbAccess.getResultSet().next())
						{
								userIds[j] = dbAccess.getResultSet().getString(1);
								j++;
						}
			
						//delete from enroll
						for(int i=0;i<size;i++)
						{
							String deleteEnroll = "DELETE FROM enroll where net_id = '" + userIds[i] + "' and course_id = '" + selectedCourse + "' and crn = '" + selectedRoster.substring(7,12) + "' and type = 'roster'";
							dbAccess.executeUpdate(deleteEnroll);
						}
						//delete from login_user
						for(int i=0;i<size;i++)
						{
							String deleteQuery = "DELETE FROM roster_user WHERE UserId in ('" + userIds[i] + "')";
							dbAccess.executeUpdate(deleteQuery);
						} 
						
						//delete table
						String dropQuery = "DROP TABLE IF EXISTS " + selectedRoster;
						dbAccess.executeUpdate(dropQuery);
						
						//delete table from record
						String dropRecord = "DELETE FROM roster_record where table_name = '" + selectedRoster + "'" + " and course_id = " + "'" + selectedCourse + "'";
						dbAccess.executeUpdate(dropRecord);
						
						//fetch updated list
						getAssessTables();
						getRosterTables();
						
						//Hide datatable
						renderedRosterList=false;
						
						return "Success";
					}
					catch(SQLException e)
					{
						renderError = true;
						errorMessage = "Table not dropped";
						return "Failure";
					}
					}
					else
					{
						renderError = true;
						errorMessage = "Not authorized to delete table";
						return "Failure";
					}
				}
				
		//hide Track
		public String hideTrack()
		{
			errorMessage=null;
			renderError=false;
			renderedTrackList = false;
			return "Success";
		}
		
		//hide assessment
		public String hideAssessment()
		{
			errorMessage=null;
			renderError=false;
			renderedAssessList = false;
			return "Success";
		}
		
		//hide assessment
		public String hideRoster()
		{
			errorMessage=null;
			renderError=false;
			renderedRosterList = false;
			return "Success";
		}

		//import Navigate
		public String navigate()
		{	
			errorMessage=null;
			renderError=false;
			renderedAssessList=false;
			renderedRosterList=false;
			renderedTrackList=false;
			renderedSummary=false;
			renderedGradeUser=false;
			renderedGradeAssess=false;
			renderedSummaryUser=false;
			if(option.equals("display"))
			{
				if(userBean.getRole().equals("ta"))
					option = "tadisplay";
				if(userBean.getRole().equals("instructor"))
					option = "instructordisplay";
			}
			getCourses();
			okEnable = true;
			uploadEnable=true;
			selectEnable=true;
			return option;
		}
		
		//back from upload
		public String backHome()
		{
			errorMessage=null;
			renderError=false;
			return "Success";
		}
	
		//---------------Student Functions------------------------
		
		//Navigate to quiz page
		public String takeQuiz()
		{
			renderError = false;
			errorMessage = null;
			try
			{
				dbAccess.executeSelect("select end_date from assessment_record where assessment_number = " + "'" + selectedAssess + "'" + " and course_id = " + "'" + selectedCourse + "'");
				String returnValue=null;
				//checking end date of the quiz
				java.util.Date dt = new java.util.Date();
				dbAccess.getResultSet().beforeFirst();
				dbAccess.getResultSet().next();
				//comparing quiz end date with current date
				if(dt.compareTo(dbAccess.getResultSet().getTimestamp("end_date"))>0 )
				{
					errorMessage = "The quiz cannot be taken after " + dbAccess.getResultSet().getTimestamp("end_date").toString();
					returnValue="Failure";
				}
				else 
				{
						//returnValue= "Success";
				dbAccess.executeSelect("select userID, assessment_number, course_id from quiz_attempt");
				
				if(!dbAccess.getResultSet().next())
				{
					returnValue="Success";
					Quiz();
				}
				else
				{
					//checking whether user has taken the quiz earlier also
				while(dbAccess.getResultSet().next())
				{
					if (userBean.getUserName().equals(dbAccess.getResultSet().getString(1)) && selectedAssess.equals(dbAccess.getResultSet().getString(2)) && selectedCourse.equals(dbAccess.getResultSet().getString(3)))
					{
						errorMessage = "You have already taken this quiz";
						returnValue = "Failure";
						break;
					}
					else
					{
						returnValue = "Success";
					}
				}
				}
				}
				renderError = true;
				Quiz();
				return returnValue;
			}
			catch(SQLException e)
			{
				return "Failure";
			} 
		}
		
		//Populate the quiz arraylist 
		public String Quiz()
		{
			assBeanList.clear();
			//getting data from table
			String getAssess = "select table_name from assessment_record where assessment_number = " + "'" + selectedAssess + "' and course_id = '" + selectedCourse + "'" ;
			dbAccess.executeSelect(getAssess);
			try
			{
				if(!dbAccess.getResultSet().next())
				{
					renderError = true;
					errorMessage = "No file selected for download";
					return "Failure";
				}
				else
				{
				dbAccess.getResultSet().beforeFirst();
				dbAccess.getResultSet().next();
				String assessName = dbAccess.getResultSet().getString(1);
			String selectQuery = "select * from " + assessName;	
				dbAccess.executeSelect(selectQuery);
				
				dbAccess.getResultSet().beforeFirst();
				while (dbAccess.getResultSet().next())
				{	
					AssessmentBean assess = new AssessmentBean();
					assess.setQuestionNumber(dbAccess.getResultSet().getString(1));
					assess.setActualQuestion(dbAccess.getResultSet().getString(2));
					assess.setAnswer(dbAccess.getResultSet().getString(3));
					assess.setTolerance(dbAccess.getResultSet().getDouble("error"));
					assBeanList.add(assess);
				}
				//renderPrevious=false;
				dbAccess.getResultSet().beforeFirst();
				
				//getting assessment info
				dbAccess.executeSelect("select question_score from assessment_record where assessment_number = " + "'" + selectedAssess + "'" + " and course_id = " + "'" + selectedCourse + "'");
				dbAccess.getResultSet().next();
				assessmentInfoBean.setAssessmentNumber(selectedAssess);
				assessmentInfoBean.setQuestionScore(dbAccess.getResultSet().getDouble("question_score"));
			}} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "Success";
		
		}
	

		   //validate and insert answers into quiz_attempt table
		 	public String answerAction() 
		 	{
		 		renderError = false;
				errorMessage = null;
				try
				{
					dbAccess.executeSelect("select table_name, assessment_number,section from assessment_record where assessment_number = " + "'" + selectedAssess + "'" + " and course_id = " + "'" + selectedCourse + "'");
					String selectedAssessment = selectedAssess;
					String assessTable = null;
					while(dbAccess.getResultSet().next())
					{
						section = dbAccess.getResultSet().getString("section");
						assessTable = dbAccess.getResultSet().getString("table_name");
						break;
					}
					String returnValue=null;
					//check for re-take
					dbAccess.executeSelect("select userID, assessment_number, course_id from quiz_attempt");
					
					if(!dbAccess.getResultSet().next())
					{
						returnValue="Success";
					}
					else
					{
						//checking whether user has taken the quiz earlier also
					while(dbAccess.getResultSet().next())
					{
						if (userBean.getUserName().equals(dbAccess.getResultSet().getString(1)) && selectedAssessment.equals(dbAccess.getResultSet().getString(2)) && selectedCourse.equals(dbAccess.getResultSet().getString(3)))
						{
							errorMessage = "You have already taken this quiz. You will not be evaluated";
							returnValue = "Failure";
							break;
						}
						else
						{
							returnValue = "Success";
						}
					}
					}
					renderError = true;
					if(returnValue.equals("Success"))
					{
				   boolean flag = false;
				   dbAccess.executeSelect("select userID, assessment_number, question_number from quiz_attempt");
				   if(!dbAccess.getResultSet().next())
					   flag=false;
				   else
				   {
	        		 for(int i=0;i<assBeanList.size();i++)
	        		 {
	        			 while(dbAccess.getResultSet().next())
	        			 {
	        				 if(userBean.getUserName().equals(dbAccess.getResultSet().getString(1)) && assessmentInfoBean.getAssessmentNumber().equals(dbAccess.getResultSet().getString(2)) && assBeanList.get(i).questionNumber.equals(dbAccess.getResultSet().getString(3)))
	        				 {
	        			 		 flag=true;
	        				 }
	        			 }
	        			 if(flag)
	        			 {
	        				 String deleteQuery = "DELETE FROM quiz_attempt WHERE userID = " + "'" + userBean.getUserName() + "'"
	        				       + " and assessment_number = " + "'" + assessmentInfoBean.getAssessmentNumber() + "'"
	        				       + " and question_number = " + "'" + assBeanList.get(i).questionNumber + "'"
	        				       + " and course_id = " + "'" + selectedCourse + "'";
	        				 dbAccess.executeUpdate(deleteQuery);
	        			 }
	        		 }
				   }
				   for(int i=0;i<assBeanList.size();i++)
				   {
					   if(assBeanList.get(i).getAnswer().matches("-?[0-9]*\\.?[0-9]*$")&& assBeanList.get(i).getStudentResponse().matches("-?[0-9]*\\.?[0-9]*$")&&!assBeanList.get(i).getStudentResponse().isEmpty())
					   {
						   if (Math.abs(Double.parseDouble(assBeanList.get(i).getAnswer())-Double.parseDouble(assBeanList.get(i).getStudentResponse()))<= assBeanList.get(i).getTolerance())
						   {    
							   String insertQuery = "insert into quiz_attempt VALUES(" + "'"+userBean.getUserName()+"'" +","+ "'"+assessmentInfoBean.getAssessmentNumber()+"'"+","+"'" + selectedCourse + "'"+","+
			        			     "'"+assBeanList.get(i).getQuestionNumber()+"'" +","+ "'"+assBeanList.get(i).getActualQuestion().replace("'", "\\'").replace("\"", "")+"'" +"," + "'"+ assBeanList.get(i).getAnswer()+ "'" +","+ "'"+ assBeanList.get(i).getStudentResponse()+"'"+","+assessmentInfoBean.getQuestionScore()+")";	
							   dbAccess.executeUpdate(insertQuery);	    
						   }
		                else 
		                {
				        	 String insertQuery = "insert into quiz_attempt VALUES(" + "'"+userBean.getUserName()+"'" +","+"'"+assessmentInfoBean.getAssessmentNumber()+"'"+","+"'" + selectedCourse + "'"+","+
				        			     "'"+assBeanList.get(i).getQuestionNumber()+"'" +","+ "'"+assBeanList.get(i).getActualQuestion().replace("'", "\\'").replace("\"", "")+"'" +"," + "'"+ assBeanList.get(i).getAnswer()+ "'" +","+ "'"+ assBeanList.get(i).getStudentResponse()+"'"+","+0+")";	
			        		 dbAccess.executeUpdate(insertQuery);	
				 		} 
					  }
	        	else
	        	{
	        		if (assBeanList.get(i).getAnswer().equalsIgnoreCase(assBeanList.get(i).getStudentResponse()))
	        		{    
	        			String insertQuery = "insert into quiz_attempt VALUES(" + "'"+userBean.getUserName()+"'" +","+ "'"+assessmentInfoBean.getAssessmentNumber()+"'"+","+"'" + selectedCourse + "'"+","+
		        			     "'"+assBeanList.get(i).getQuestionNumber()+"'" +","+ "'"+assBeanList.get(i).getActualQuestion().replace("'", "\\'").replace("\"", "")+"'" +"," + "'"+ assBeanList.get(i).getAnswer()+ "'" +","+ "'"+ assBeanList.get(i).getStudentResponse()+"'"+","+assessmentInfoBean.getQuestionScore()+")";	
						dbAccess.executeUpdate(insertQuery);	    
	        		}
			      else 
			      	{
			        	String insertQuery = "insert into quiz_attempt VALUES(" + "'"+userBean.getUserName()+"'" +","+"'"+assessmentInfoBean.getAssessmentNumber()+"'"+","+"'" + selectedCourse + "'"+","+
			        			     "'"+assBeanList.get(i).getQuestionNumber()+"'" +","+ "'"+assBeanList.get(i).getActualQuestion().replace("'", "\\'").replace("\"", "")+"'" +"," + "'"+ assBeanList.get(i).getAnswer()+ "'" +","+ "'"+ assBeanList.get(i).getStudentResponse()+"'"+","+0+")";
						dbAccess.executeUpdate(insertQuery);	
			 		} 
	        	}
				
			}
				 //Calculate grade
					String gradeQuery = "select sum(score) as total from quiz_attempt where userID = "+"'"+ userBean.getUserName()+"'"+" and assessment_number = " + "'"+assessmentInfoBean.getAssessmentNumber()+"' and course_id = '" + selectedCourse + "'" +" group by userID,assessment_number,course_id ;";
					dbAccess.executeSelect(gradeQuery);
					dbAccess.getResultSet().next();
					double scoreObtained = dbAccess.getResultSet().getDouble(1);
					double totalScore = assessmentInfoBean.getQuestionScore()*assBeanList.size();
					
					//insert into grade table
					String insertGrade = "INSERT INTO grade VALUES ('" + userBean.getUserName() + "'" + ",'" + assessmentInfoBean.getAssessmentNumber() + "'" + ",'" + selectedCourse + "','" + section + "'," + scoreObtained + "," + totalScore + ")";
					dbAccess.executeUpdate(insertGrade);
					
					//check for roster users
					String selectUsers = "select UserId from roster_user";
					dbAccess.executeSelect(selectUsers);
					while(dbAccess.getResultSet().next())
					{
						if(userBean.getUserName().equals(dbAccess.getResultSet().getString(1)))
						{
							updateRoster(scoreObtained, totalScore, assessTable);
							break;
						}
					}
	        
					}
					renderGrade=false;
					return "Success";
	     } catch (SQLException e) {
					// TODO Auto-generated catch block
					renderError = true;
					errorMessage = "Error in submitting your quiz";
					return "Failure";
				}
		         
	}
		 	//Update roster with student grade
		 	public String updateRoster(double score, double total, String assessTable)
		 	{
		 		double percent = (score/total)*100;
		 		String percentString = new Double(percent).toString();
		 		String selectRoster = "select table_name from roster_record where course_Id = '" + selectedCourse + "' and section = '" + section + "'";
		 		try
		 		{
		 		dbAccess.executeSelect(selectRoster);
		 		String tableName = null;
		 		//ArrayList<String> userList = new ArrayList<String>();
		 		while(dbAccess.getResultSet().next())
		 		{
		 			tableName = dbAccess.getResultSet().getString(1);
		 		}
		 			String getUsers = "select NetID from " + tableName;
		 			dbAccess.executeSelect(getUsers);
		 			boolean flag = false;
		 			while(dbAccess.getResultSet().next())
		 			{
		 				if (userBean.getUserName().equals(dbAccess.getResultSet().getString(1)))
		 				{
		 				flag = true;
		 				break;
		 				}
		 			}
		 			if(flag)
		 			{
		 			String updateRoster = "UPDATE " + tableName + " SET " + assessTable + " = " + percentString + " where NetId = '" + userBean.getUserName() + "'";
		 			dbAccess.executeUpdate(updateRoster);
		 			}
		 		return "Success";
		 		}catch(SQLException e)
		 		{
		 			return "Failure";
		 		}
		 	}
		   
		   //view grades
		   public String viewGrades()
		   {
			   renderError = false;
			   errorMessage=null;
				try 
				{
					dbAccess.executeSelect("select score_obtained, total_score from grade where user_id = " + "'" + userBean.getUserName() + "' and assessment_number = " + "'" + assessmentInfoBean.getAssessmentNumber() + "' and course_id = " + "'" + selectedCourse + "'" );
					dbAccess.getResultSet().next();
					grade = "Your score is "+dbAccess.getResultSet().getDouble(1)+" out of " + dbAccess.getResultSet().getDouble(2);
					renderGrade=true;
					return "Success";
				} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return "Error";
					}
		   }
		   
		   //display Quiz attempt
		   public String displayQuizAttempt()
		   {
			   renderError=false;
			   errorMessage = null;
			   quizAttemptList.clear();
			   String selectedUser = userBean.getUserName();
			   try
			   {
					/*dbAccess.executeSelect("select assessment_number from assessment_record where table_name = " + "'" + selectedAssess + "'" + " and course_id = " + "'" + selectedCourse + "'");
					String selectedAssessment = null;
					while(dbAccess.getResultSet().next())
					{
						selectedAssessment = dbAccess.getResultSet().getString("assessment_number");
						break;
					}*/
					
			   dbAccess.executeSelect("select * from quiz_attempt where userID = " + "'" + selectedUser + "'"
					   + " and assessment_number = " + "'" + selectedAssess + "' and course_id = '" + selectedCourse + "'");
			   
			   while(dbAccess.getResultSet().next())
			   {
				   QuizAttemptBean quizAttempt = new QuizAttemptBean();
				   quizAttempt.setUserId(dbAccess.getResultSet().getString(1));
				   quizAttempt.setAssessmentNumber(dbAccess.getResultSet().getString(2));
				   quizAttempt.setQuestionNumber(dbAccess.getResultSet().getString(4));
				   quizAttempt.setActualQuestion(dbAccess.getResultSet().getString(5));
				   quizAttempt.setActualAnswer(dbAccess.getResultSet().getString(6));
				   quizAttempt.setStudentResponse(dbAccess.getResultSet().getString(7));
				   quizAttempt.setScore(dbAccess.getResultSet().getDouble(8));
				   quizAttemptList.add(quizAttempt);
				   
			   }
			   renderedQuizList=true;
			   return "Success";
			   }
			   catch(SQLException e)
			   {
				   renderError = true;
				   errorMessage = "Error in displaying the quiz attempt";
				   return "Failure";
			   }
		   }
		   
		 //display Quiz attempt
		   public String displayCourses()
		   {
			   renderError=false;
			   errorMessage = null;
			   courseList.clear();
			   try
			   {	
				   dbAccess.executeSelect("select * from courses");
			   
			   while(dbAccess.getResultSet().next())
			   {
				   CourseBean course = new CourseBean();
				   course.setCourseId(dbAccess.getResultSet().getString(1));
				   course.setCrn(dbAccess.getResultSet().getString(2));
				   course.setTitle(dbAccess.getResultSet().getString(3));
				   course.setInstructor(dbAccess.getResultSet().getString(4));
				   course.setTa(dbAccess.getResultSet().getString(5));
				   courseList.add(course);
				   
			   }
			   renderedCourseList=true;
			   return "Success";
			   }
			   catch(SQLException e)
			   {
				   renderError = true;
				   errorMessage = "Error in displaying the courses";
				   return "Failure";
			   }
		   }
		
		   //hide attempt
		   public String hideAttempt()
		   {
			   renderedQuizList=false;
			   return "Success";
		   }
		   
		   //hide attempt
		   public String hideCourses()
		   {
			   renderedCourseList=false;
			   return "Success";
		   }
		   //--------------------Exit Student Functions------------------------------------
	
		   /**********************************************************************************************************************/
			
		 //display grade student wise
			public String displayGradeByUser()
			{
				gradeByUser.clear();
				query = "select table_name from roster_record where course_id = '" + selectedCourse + "' and section = '" + selectedCRN + "'";
				dbAccess.executeSelect(query);
				try
				{
				if(!dbAccess.getResultSet().next())
				{
					query = "SELECT last_name, first_name, net_id FROM login_user where net_id = '" + selectedUser + "'";
					dbAccess.executeSelect(query);
				}
				else
				{
				String rosterName = dbAccess.getResultSet().getString(1);
				query = "SELECT * FROM " + rosterName + " where NetID = '" + selectedUser + "'";
				dbAccess.executeSelect(query);
				}
				
				if(dbAccess.getResultSet().next())
				{
					rosterBean.setFirstName(dbAccess.getResultSet().getString(2));
					rosterBean.setLastName(dbAccess.getResultSet().getString(1));
					rosterBean.setNetID(dbAccess.getResultSet().getString(3));
				}
				
				query = "SELECT * FROM grade where user_id = '" + selectedUser + "' and course_id = '" + selectedCourse + "'";
				dbAccess.executeSelect(query);
				
				while(dbAccess.getResultSet().next())
				{
					GradeBean grade = new GradeBean();
					grade.setAssessmentNumber(dbAccess.getResultSet().getString(2));
					grade.setScoreObtained(dbAccess.getResultSet().getDouble(5));
					grade.setTotalScore(dbAccess.getResultSet().getDouble(6));
					gradeByUser.add(grade);
				}
				renderedGradeUser = true;
				return "Success";
				}catch(SQLException e){
					return "Failure";
				}
			}
			
			//hide grade by user
			public void hideGradeByUser()
			{
				renderedGradeUser=false;
			}
			
			//hide grade by user
			public void hideGradeByAssess()
			{
				renderedGradeAssess=false;
			}
			
			//display grade by assessment
			public String displayGradeByAssessment()
			{
				gradeByAssess.clear();
				query = "select * from assessment_record where assessment_number = '" + selectedAssess + "' and course_id = '" + selectedCourse + "'";
				dbAccess.executeSelect(query);
				try
				{
				
				if(dbAccess.getResultSet().next())
				{
					assessmentInfoBean.setAssessmentNumber(dbAccess.getResultSet().getString(2));
					assessmentInfoBean.setAssessmentTime(dbAccess.getResultSet().getInt(3));
					assessmentInfoBean.setEndDate(dbAccess.getResultSet().getString(4));
					assessmentInfoBean.setQuestionScore(dbAccess.getResultSet().getDouble(5));
				}
				
				query = "SELECT * FROM grade where assessment_number = '" + assessmentInfoBean.getAssessmentNumber() + "' and course_id = '" + selectedCourse + "'";
				dbAccess.executeSelect(query);
				while(dbAccess.getResultSet().next())
				{
					GradeBean grade = new GradeBean();
					grade.setNetId(dbAccess.getResultSet().getString(1));
					grade.setScoreObtained(dbAccess.getResultSet().getDouble(5));
					grade.setTotalScore(dbAccess.getResultSet().getDouble(6));
					gradeByAssess.add(grade);
				}
				renderedGradeAssess = true;
				return "Success";
				}catch(SQLException e){
					return "Failure";
				}
			}
			
			//evaluates and displays five-number summary
			public String displayStatsSummary()
			   {
				   statsList.clear();
				   try
				   {
					   /*dbAccess.executeSelect("select assessment_number from assessment_record where table_name = " + "'" + selectedAssess + "'");
						
						String selectedAssessment = null;
						while(dbAccess.getResultSet().next())
						{
							selectedAssessment = dbAccess.getResultSet().getString("assessment_number");
							break;
						}*/
						
				   String sqlQuery = "select sum(score) from quiz_attempt where assessment_number = " + "'" + selectedAssess + "' " + "group by userID,assessment_number ";
				   dbAccess.executeSelect(sqlQuery);
				   
				   dbAccess.getResultSet().last();
					int nRows = dbAccess.getResultSet().getRow();
					
					dbAccess.getResultSet().beforeFirst();
				   
				   double[] list = new double[nRows];
				   int i=0;
				   while(dbAccess.getResultSet().next())
				   {
					   list[i] = dbAccess.getResultSet().getDouble(1);
					   i++;
					   
				   }
				   
				   StatsBean statsBean = new StatsBean();
				   
				   statsBean.setMaxValue(StatUtils.max(list));
				   statsBean.setMinValue(StatUtils.min(list));
				   statsBean.setMean(StatUtils.mean(list));
				   statsBean.setVariance(StatUtils.variance(list));
				   statsBean.setStd(Math.sqrt(StatUtils.variance(list)));
				   statsBean.setMedian(StatUtils.percentile(list,50.0));
				   statsBean.setQ1(StatUtils.percentile(list,25.0));
				   statsBean.setQ3(StatUtils.percentile(list,75.0));
				   statsBean.setIqr(StatUtils.percentile(list,75.0)-StatUtils.percentile(list,25.0));
				   statsBean.setRange(StatUtils.max(list)-StatUtils.min(list));
				  
				   statsList.add(statsBean);
				   
				   renderedSummary =true;
				   return "Success";
				   }
				   catch(SQLException e)
				   {
					   return "Failure";
				   }
			   }
				
				
			
			   //hide attempt
			   public String hideSummary()
			   {
				   renderedSummary=false;
				   return "Success";
			   }
			   
/*---------------------------------- Summary by user for All assessments-----------------------------------------------------------*/
			   
			   
			   public String displayStatsSummaryUser()
			   {
				   statsListUser.clear();
				   try
				   {
					 
						
				   String sqlQuery = "select (score_obtained/total_score)*100 from grade where user_id = " + "'" + selectedUser + "' " + "and course_id = '"+selectedCourse+"'";
				   dbAccess.executeSelect(sqlQuery);
				   
				   dbAccess.getResultSet().last();
					int nRows = dbAccess.getResultSet().getRow();
					
					dbAccess.getResultSet().beforeFirst();
				   
				   double[] list = new double[nRows];
				   int i=0;
				   while(dbAccess.getResultSet().next())
				   {
					   list[i] = dbAccess.getResultSet().getDouble(1);
					   i++;
					   
				   }
				  
				   StatsBean statsBean = new StatsBean();
				   
				   statsBean.setMaxValue(StatUtils.max(list));
				   statsBean.setMinValue(StatUtils.min(list));
				   statsBean.setMean(StatUtils.mean(list));
				   statsBean.setVariance(StatUtils.variance(list));
				   statsBean.setStd(Math.sqrt(StatUtils.variance(list)));
				   statsBean.setMedian(StatUtils.percentile(list,50.0));
				   statsBean.setQ1(StatUtils.percentile(list,25.0));
				   statsBean.setQ3(StatUtils.percentile(list,75.0));
				   statsBean.setIqr(StatUtils.percentile(list,75.0)-StatUtils.percentile(list,25.0));
				   statsBean.setRange(StatUtils.max(list)-StatUtils.min(list));
				   
				   statsListUser.add(statsBean);
				   
				   renderedSummaryUser =true;
				   return "Success";
				   }
				   catch(SQLException e)
				   {
					   return "Failure";
				   }
			   }
				
				
			
			   //hide attempt
			   public String hideSummaryUser()
			   {
				   renderedSummaryUser=false;
				   return "Success";
			   }
			  			   		   
			  

			  

ArrayList<GradeBean> gradeBeanListMean = new ArrayList<GradeBean>();
	ArrayList<GradeBean> gradeBeanListGradeCategory = new ArrayList<GradeBean>();
	
	ArrayList<Double> meanArray = new ArrayList<Double>();
	
	public void gradeDataMean(){
		
		gradeBeanListMean.clear();
		
		String sqlQuery = "select assessment_number,avg((score_obtained/total_score)*100) from grade group by assessment_number";
		
		dbAccess.executeSelect(sqlQuery);
		
		try {
			while(dbAccess.getResultSet().next()){
				
				GradeBean gradeBean = new GradeBean();
				
				//gradeBean.setNetId(dbAccess.getResultSet().getDouble(1));
				gradeBean.setAssessmentNumber(dbAccess.getResultSet().getString(1));
				//gradeBean.setCourseId(dbAccess.getResultSet().getString(3));
				gradeBean.setScoreObtained(dbAccess.getResultSet().getDouble(2));
				//gradeBean.setTotalScore(dbAccess.getResultSet().getDouble(5));
				
				gradeBeanListMean.add(gradeBean);
				
				
				
			}
			
			chartBean.setGradeBeanListMean(gradeBeanListMean);
			
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}
	
	//////////////////////////////////////////////////////////////////
	
public void gradeDataGradeCategory(){
		
		String sqlQuery = "select user_Id,sum(score_obtained)/sum(total_score)*100 from grade group by user_Id";
		
		
		
		dbAccess.executeSelect(sqlQuery);
		
		try {
			while(dbAccess.getResultSet().next()){
				
				GradeBean gradeBean = new GradeBean();
				
				//gradeBean.setNetId(dbAccess.getResultSet().getDouble(1));
				gradeBean.setNetId(dbAccess.getResultSet().getString(1));
				//gradeBean.setCourseId(dbAccess.getResultSet().getString(3));
				gradeBean.setScoreObtained(dbAccess.getResultSet().getDouble(2));
				//gradeBean.setTotalScore(dbAccess.getResultSet().getDouble(5));
				
				gradeBeanListGradeCategory.add(gradeBean);
				
			}
			
			chartBean.setGradeBeanListGradeCategory(gradeBeanListGradeCategory);
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}

public ArrayList<GradeBean> getGradeBeanListMean() {
	return gradeBeanListMean;
}

public void setGradeBeanListMean(ArrayList<GradeBean> gradeBeanListMean) {
	this.gradeBeanListMean = gradeBeanListMean;
}

public ArrayList<GradeBean> getGradeBeanListGradeCategory() {
	return gradeBeanListGradeCategory;
}

public void setGradeBeanListGradeCategory(ArrayList<GradeBean> gradeBeanListGradeCategory) {
	this.gradeBeanListGradeCategory = gradeBeanListGradeCategory;
}
			   
			public boolean isRenderedSummary() {
				return renderedSummary;
			}

			public void setRenderedSummary(boolean renderedSummary) {
				this.renderedSummary = renderedSummary;
			}

			public ArrayList<StatsBean> getStatsList() {
				return statsList;
			}

			public void setStatsList(ArrayList<StatsBean> statsList) {
				this.statsList = statsList;
			}
			
			
			public String showChart(){
				
				
				return "Success";
				
				
			}
			
	

	//User logout	
	public String logout()
	{
		try 
		{
			loggedIn=false;
			trackLogout();
			ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
			externalContext.invalidateSession();
			String url = externalContext.getRequestScheme()
				  + "://" + externalContext.getRequestServerName()
				  + ":" + externalContext.getRequestServerPort()
				  + externalContext.getRequestContextPath();
			dbAccess.closeConnection();
			externalContext.redirect(url + "/faces/home.jsp");
			return "Logout"; 
		}
		catch (IOException e)
		{
			return "Failure";
		}	
	}

	
	//track logout
	public String trackLogout()
	{
		trackBean.generateOutTime();
			String updateTrack = "UPDATE track_user SET out_time = " + "'" + trackBean.getOutTime() + "'" + "where user_id = " + "'" + userBean.getUserName() + "' and out_flag is null";
			dbAccess.executeUpdate(updateTrack);
			return "Success";
	}
	
	//Exit Application
	public String exitApplication()
	{
		try
		{
			ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
			ec.invalidateSession();
			String url = ec.getRequestScheme()
				  + "://" + ec.getRequestServerName()
				  + ":" + ec.getRequestServerPort()
				  + ec.getRequestContextPath();
			dbAccess.closeConnection();
			ec.redirect(url + "/faces/index.jsp");
			return "Logout"; 
		}
		catch (IOException e)
		{
			return "Failure";
		}
	}
	
	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public AssessmentBean getAssessmentBean() {
		return assessmentBean;
	}

	public void setAssessmentBean(AssessmentBean assessmentBean) {
		this.assessmentBean = assessmentBean;
	}


	public boolean isRenderedAssessList() {
		return renderedAssessList;
	}

	public void setRenderedAssessList(boolean renderedAssessList) {
		this.renderedAssessList = renderedAssessList;
	}

	public String getRosterName() {
		return rosterName;
	}

	public void setRosterName(String rosterName) {
		this.rosterName = rosterName;
	}

	public RosterBean getRosterBean() {
		return rosterBean;
	}

	public void setRosterBean(RosterBean rosterBean) {
		this.rosterBean = rosterBean;
	}

	public boolean isRenderedRosterList() {
		return renderedRosterList;
	}

	public void setRenderedRosterList(boolean renderedRosterList) {
		this.renderedRosterList = renderedRosterList;
	}

	public String getApplicationPath() {
		return applicationPath;
	}

	public void setApplicationPath(String applicationPath) {
		this.applicationPath = applicationPath;
	}

	public ArrayList<String> getArrayAssessTables() {
		return arrayAssessTables;
	}

	public void setArrayAssessTables(ArrayList<String> arrayAssessTables) {
		this.arrayAssessTables = arrayAssessTables;
	}

	public ArrayList<String> getArrayRosterTables() {
		return arrayRosterTables;
	}

	public void setArrayRosterTables(ArrayList<String> arrayRosterTables) {
		this.arrayRosterTables = arrayRosterTables;
	}

	public String getSelectedAssess() {
		return selectedAssess;
	}

	public void setSelectedAssess(String selectedAssess) {
		this.selectedAssess = selectedAssess;
	}

	public String getSelectedRoster() {
		return selectedRoster;
	}

	public void setSelectedRoster(String selectedRoster) {
		this.selectedRoster = selectedRoster;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public String getUploadedFileContents() {
		return uploadedFileContents;
	}

	public void setUploadedFileContents(String uploadedFileContents) {
		this.uploadedFileContents = uploadedFileContents;
	}

	public String getFileLabel() {
		return fileLabel;
	}

	public void setFileLabel(String fileLabel) {
		this.fileLabel = fileLabel;
	}

	public TrackBean getTrackBean() {
		return trackBean;
	}

	public void setTrackBean(TrackBean trackBean) {
		this.trackBean = trackBean;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public ArrayList<AssessmentBean> getAssessmentList() {
		return assessmentList;
	}

	public void setAssessmentList(ArrayList<AssessmentBean> assessmentList) {
		this.assessmentList = assessmentList;
	}

	public ArrayList<RosterBean> getRosterList() {
		return rosterList;
	}

	public void setRosterList(ArrayList<RosterBean> rosterList) {
		this.rosterList = rosterList;
	}

	public ArrayList<TrackBean> getTrackList() {
		return trackList;
	}

	public void setTrackList(ArrayList<TrackBean> trackList) {
		this.trackList = trackList;
	}

	public boolean isRenderedTrackList() {
		return renderedTrackList;
	}

	public void setRenderedTrackList(boolean renderedTrackList) {
		this.renderedTrackList = renderedTrackList;
	}

	public String getCourseId() {
		return courseId;
	}

	public void setCourseId(String courseId) {
		this.courseId = courseId;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public boolean isRenderAssessment() {
		return renderAssessment;
	}

	public void setRenderAssessment(boolean renderAssessment) {
		this.renderAssessment = renderAssessment;
	}

	public int getTries() {
		return tries;
	}

	public void setTries(int tries) {
		this.tries = tries;
	}

	public String getCorrectAnswer() {
		return correctAnswer;
	}

	public void setCorrectAnswer(String correctAnswer) {
		this.correctAnswer = correctAnswer;
	}

	public ArrayList<AssessmentBean> getAssBeanList() {
		return assBeanList;
	}

	public void setAssBeanList(ArrayList<AssessmentBean> assBeanList) {
		this.assBeanList = assBeanList;
	}

	public boolean isRenderPrevious() {
		return renderPrevious;
	}

	public void setRenderPrevious(boolean renderPrevious) {
		this.renderPrevious = renderPrevious;
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	public AssessmentInfoBean getAssessmentInfoBean() {
		return assessmentInfoBean;
	}

	public void setAssessmentInfoBean(AssessmentInfoBean assessmentInfoBean) {
		this.assessmentInfoBean = assessmentInfoBean;
	}

	public ArrayList<QuizAttemptBean> getQuizAttemptList() {
		return quizAttemptList;
	}

	public void setQuizAttemptList(ArrayList<QuizAttemptBean> quizAttemptList) {
		this.quizAttemptList = quizAttemptList;
	}

	public String getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedUser(String selectedUser) {
		this.selectedUser = selectedUser;
	}

	public boolean isRenderedQuizList() {
		return renderedQuizList;
	}

	public void setRenderedQuizList(boolean renderedQuizList) {
		this.renderedQuizList = renderedQuizList;
	}

	public boolean isRenderGrade() {
		return renderGrade;
	}

	public void setRenderGrade(boolean renderGrade) {
		this.renderGrade = renderGrade;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public boolean isRenderError() {
		return renderError;
	}

	public void setRenderError(boolean renderError) {
		this.renderError = renderError;
	}

	public DbAccess getDbAccess() {
		return dbAccess;
	}

	public void setDbAccess(DbAccess dbAccess) {
		this.dbAccess = dbAccess;
	}

	public DataBaseBean getDataBaseBean() {
		return dataBaseBean;
	}

	public void setDataBaseBean(DataBaseBean dataBaseBean) {
		this.dataBaseBean = dataBaseBean;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getOption() {
		return option;
	}

	public void setOption(String option) {
		this.option = option;
	}

	public CourseBean getCourseBean() {
		return courseBean;
	}

	public void setCourseBean(CourseBean courseBean) {
		this.courseBean = courseBean;
	}

	public ArrayList<CourseBean> getCourseList() {
		return courseList;
	}

	public void setCourseList(ArrayList<CourseBean> courseList) {
		this.courseList = courseList;
	}

	public String getSelectedCourse() {
		return selectedCourse;
	}

	public void setSelectedCourse(String selectedCourse) {
		this.selectedCourse = selectedCourse;
	}

	public boolean isRenderedCourseList() {
		return renderedCourseList;
	}

	public void setRenderedCourseList(boolean renderedCourseList) {
		this.renderedCourseList = renderedCourseList;
	}

	public ArrayList<String> getArrayCourses() {
		return arrayCourses;
	}

	public void setArrayCourses(ArrayList<String> arrayCourses) {
		this.arrayCourses = arrayCourses;
	}

	public ArrayList<String> getArrayCRN() {
		return arrayCRN;
	}

	public void setArrayCRN(ArrayList<String> arrayCRN) {
		this.arrayCRN = arrayCRN;
	}

	public String getSelectedCRN() {
		return selectedCRN;
	}

	public void setSelectedCRN(String selectedCRN) {
		this.selectedCRN = selectedCRN;
	}

	public ArrayList<String> getArrayUsers() {
		return arrayUsers;
	}

	public void setArrayUsers(ArrayList<String> arrayUsers) {
		this.arrayUsers = arrayUsers;
	}

	public GradeBean getGradeBean() {
		return gradeBean;
	}

	public void setGradeBean(GradeBean gradeBean) {
		this.gradeBean = gradeBean;
	}

	public ArrayList<GradeBean> getGradeByUser() {
		return gradeByUser;
	}

	public void setGradeByUser(ArrayList<GradeBean> gradeByUser) {
		this.gradeByUser = gradeByUser;
	}

	public ArrayList<GradeBean> getGradeByAssess() {
		return gradeByAssess;
	}

	public void setGradeByAssess(ArrayList<GradeBean> gradeByAssess) {
		this.gradeByAssess = gradeByAssess;
	}

	public boolean isRenderedGradeUser() {
		return renderedGradeUser;
	}

	public void setRenderedGradeUser(boolean renderedGradeUser) {
		this.renderedGradeUser = renderedGradeUser;
	}

	public boolean isRenderedGradeAssess() {
		return renderedGradeAssess;
	}

	public void setRenderedGradeAssess(boolean renderedGradeAssess) {
		this.renderedGradeAssess = renderedGradeAssess;
	}

	public boolean isOkEnable() {
		return okEnable;
	}

	public void setOkEnable(boolean okEnable) {
		this.okEnable = okEnable;
	}

	public boolean isUploadEnable() {
		return uploadEnable;
	}

	public void setUploadEnable(boolean uploadEnable) {
		this.uploadEnable = uploadEnable;
	}

	public ArrayList<Double> getMeanArray() {
		return meanArray;
	}

	public void setMeanArray(ArrayList<Double> meanArray) {
		this.meanArray = meanArray;
	}

	public ArrayList<StatsBean> getStatsListUser() {
		return statsListUser;
	}

	public void setStatsListUser(ArrayList<StatsBean> statsListUser) {
		this.statsListUser = statsListUser;
	}

	public boolean isRenderedSummaryUser() {
		return renderedSummaryUser;
	}

	public void setRenderedSummaryUser(boolean renderedSummaryUser) {
		this.renderedSummaryUser = renderedSummaryUser;
	}

	public boolean isSelectEnable() {
		return selectEnable;
	}

	public void setSelectEnable(boolean selectEnable) {
		this.selectEnable = selectEnable;
	}

	public boolean isAllEnable() {
		return allEnable;
	}

	public void setAllEnable(boolean allEnable) {
		this.allEnable = allEnable;
	}

	public ChartBean getChartBean() {
		return chartBean;
	}

	public void setChartBean(ChartBean chartBean) {
		this.chartBean = chartBean;
	}
	
	
	
}