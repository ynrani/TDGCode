/*
 * Object Name : TDMAdminController.java
 * Modification Block
 * ---------------------------------------------------------------------
 * S.No.	Name 			Date			Bug_Fix_No			Desc
 * ---------------------------------------------------------------------
 * 	1.	  vkrish14		Jun 15, 2015			NA             Created
 * ---------------------------------------------------------------------
 * Copyrights: 2015 Capgemini.com
 */
package com.tesda.controller;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tesda.model.DTO.BaseDTO;
import com.tesda.model.DTO.TdgDictionaryDTO;
import com.tesda.model.DTO.TdgGuiDetailsDTO;
import com.tesda.model.DTO.TdgSchemaDTO;
import com.tesda.model.DTO.TdmUserDTO;
import com.tesda.model.DTO.ValidationResponse;
import com.tesda.service.TDMAdminService;
import com.tesda.service.impl.TdgAsyncServiceImpl;
import com.tesda.util.CSVGenerator;
import com.tesda.util.PaginationUtil;
import com.tesda.util.TdgCentralConstant;
import com.tesda.util.TdgExcelOperationsUtil;

@Controller
public class TDMAdminController extends BaseController{
	private static Logger logger = Logger.getLogger(TDMAdminController.class);
	private static String strClassName = " [ TDMAdminController ] ";
	@Resource(name = "tDMAdminService")
	TDMAdminService tDMAdminService;
	@Resource(name = "tdgAsyncServiceImpl")
	TdgAsyncServiceImpl tdgAsyncServiceImpl;

	@RequestMapping(value = "/tesdaUserCreate", method = RequestMethod.POST)
	public String userDetails(@ModelAttribute("userdo") TdmUserDTO userdo, ModelMap model,
			HttpServletRequest request, HttpServletResponse response){
		String strMethodName = " [ userDetails() ]";
		
		if(TdgCentralConstant.SESSION_CONTINUE.equals(checkSession(request, response))){
		logger.info(strClassName + strMethodName + " inside of userDetails get method ");
		 String strReturnPage= "redirect:testdaAdmin";
		boolean bEdit = userdo.isCreated();
		String strMessage = tDMAdminService.saveUserDetails(userdo, bEdit);
		if (!"Success".equals(strMessage)) {
			if (logger.isDebugEnabled())
				logger.debug(strClassName + strMethodName + " Userid is already exist ");
			model.addAttribute("errors", "User Id is already exist");
			String button = "Create User";
			model.addAttribute("userdo", userdo);
			model.addAttribute("Button", button);
			strReturnPage = "createNewUser";
		}
		logger.info(strClassName + strMethodName + " return from userDetails get method ");
		return strReturnPage;
		}else{
			return TdgCentralConstant.LOGIN_PAGE;
		}
	}

	@RequestMapping(value = "/testdisplayAdmin", method = RequestMethod.GET)
	public String displayAdmin(){
		return "admin";
	}

	@RequestMapping(value = "/testdaAdmin", method = RequestMethod.GET)
	public String displayUser(@RequestParam(value = "page", required = false) String search,
			@ModelAttribute("userdo") TdmUserDTO userdo, ModelMap model,
			HttpServletRequest request, HttpServletResponse response){
		String strMethodName = " [ displayUser() ]";
		logger.info(strClassName + strMethodName + " inside of displayUser get method ");
		Long totalRecords = 0L;
		PaginationUtil pagenation = new PaginationUtil();
		int recordsperpage = 10;
		int offSet = pagenation.getOffset(request, recordsperpage);
		User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		userdo.setUserId(user.getUsername());
		totalRecords = tDMAdminService.searchUserRecordsCount(userdo);
		if (logger.isDebugEnabled())
			logger.debug(strClassName + strMethodName + " Total records found in server is : "
					+ totalRecords);
		List<TdmUserDTO> DiaplayUser = tDMAdminService.getAllUser(userdo, offSet, recordsperpage,
				true);
		pagenation.paginate(totalRecords, request, Double.valueOf(recordsperpage), recordsperpage);
		int noOfPages = (int) Math.ceil(totalRecords.doubleValue() / recordsperpage);
		request.setAttribute("noOfPages", noOfPages);
		model.addAttribute("displayUser", DiaplayUser);
		logger.info(strClassName + strMethodName + " return from displayUser get method ");
		return "displayUsers";
	}

	@RequestMapping(value = "/editUser", method = RequestMethod.GET)
	public String editUser(@ModelAttribute("userdo") TdmUserDTO userdo, ModelMap model,
			HttpServletRequest request, HttpServletResponse response){
		String strMethodName = " [ editUser() ]";
		logger.info(strClassName + strMethodName + " inside of editUser method ");
		String userId = request.getParameter("userId");
		String button = "Update User";
		userdo = tDMAdminService.getEditUser(userId);
		model.addAttribute("userdo", userdo);
		model.addAttribute("Button", button);
		logger.info(strClassName + strMethodName + " return from editUser method ");
		return "createNewUser";
	}

	@RequestMapping("/tesdaCreateNewUser")
	public String createNewUser(@ModelAttribute("userdo") TdmUserDTO userdo, ModelMap model,
			HttpServletRequest request, HttpServletResponse response){
		String strMethodName = " [ createNewUser() ]";
		logger.info(strClassName + strMethodName + " inside of createNewUser method ");
		String button = "Create User";
		userdo.setCreated(true);
		model.addAttribute("userdo", userdo);
		model.addAttribute("Button", button);
		logger.info(strClassName + strMethodName + " rturn from the createNewUser method ");
		return "createNewUser";
	}

	@RequestMapping(value = "/deleteUser")
	public String daleteUser(@ModelAttribute("userdo") TdmUserDTO userdo, ModelMap model,
			HttpServletRequest request, HttpServletResponse response){
		String strMethodName = " [ daleteUser() ]";
		logger.info(strClassName + strMethodName + " inside of daleteUser method ");
		tDMAdminService.deleteUserByUserId(request.getParameter("userId"));
		logger.info(strClassName + strMethodName + " return from daleteUser method");
		return "redirect:testdaAdmin";
	}

	@RequestMapping(value = "/validateUserId", method = RequestMethod.POST)
	public @ResponseBody ValidationResponse validateUserId(
			@RequestParam(value = "userid", required = false) String userid, ModelMap model,
			HttpServletRequest request, HttpServletResponse response){
		String strMethodName = " [ validateUserId() ]";
		logger.info(strClassName + strMethodName + " inside of validateUserId method ");
		ValidationResponse validationResponse = new ValidationResponse();
		List<String> listResult = new ArrayList<String>();
		boolean bCheck = tDMAdminService.validateUserId(userid);
		if (!bCheck) {
			validationResponse.setStatus("FAILED");
			listResult.add(userid + " user id is already exist");
			validationResponse.setResult(listResult);
		} else {
			validationResponse.setStatus("SUCCESS");
		}
		logger.info(strClassName + strMethodName + " return from validateUserId method ");
		return validationResponse;
	}

	@RequestMapping(value = "/tesdaMasterDictionary", method = RequestMethod.GET)
	public String uploadMasterData(@ModelAttribute("tdgDictionaryDTO") TdgDictionaryDTO userdo){
		String strMethodName = " [ uploadMasterData() ]";
		logger.info(strClassName + strMethodName + " inside of uploadMasterData get method ");
		return "tdgMasterDictionary";
	}

	@RequestMapping(value = "/tdgMasterDictionaryDashboard", method = RequestMethod.GET)
	public String tdgMasterDictionaryDashboard(
			@RequestParam(value = "page", required = false) String page, ModelMap model,
			HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute("baseDTO") BaseDTO baseDTO){
		String strMethodName = " [ tdgMasterDictionaryDashboard() ]";
		logger.info(strClassName + strMethodName
				+ " inside of tdgMasterDictionaryDashboard get method ");
		try {
			Long totalRecords = 0L;
			PaginationUtil pagenation = new PaginationUtil();
			int recordsperpage = Integer.valueOf(10);
			int offSet = pagenation.getOffset(request, recordsperpage);
			totalRecords = tDMAdminService.getTdgMasterDictionaryRecordsCount();
			List<TdgSchemaDTO> tdgSchemaDTOList = tDMAdminService
					.getTdgMasterDictionaryRecordsForPagination(offSet, recordsperpage, true);
			pagenation.paginate(totalRecords, request, Double.valueOf(recordsperpage),
					recordsperpage);
			int noOfPages = (int) Math.ceil(totalRecords.doubleValue() / recordsperpage);
			request.setAttribute("noOfPages", noOfPages);
			model.addAttribute("tdgSchemaDTOList", tdgSchemaDTOList);
			model.addAttribute("baseDTO", baseDTO);
			return "tdgMasterDictionaryDashboard";
		} catch (Exception e) {
			logger.error(strClassName + " " + e.getMessage());
			return "tdgMasterDictionaryDashboard";
		}
	}

	@RequestMapping(value = "/deleteTdgMasterDictionaryByReqSchemaId")
	public String deleteTdgMasterDictionaryByReqSchemaId(
			@RequestParam(value = "reqSchemaId", required = false) String reqSchemaId,
			@RequestParam(value = "manualDictionaryId", required = false) String manualDictionaryId,
			ModelMap model, HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute("baseDTO") BaseDTO baseDTO){
		String strMethodName = " [ deleteTdgMasterDictionaryByReqSchemaId() ]";
		logger.info(strClassName + strMethodName
				+ " inside of deleteTdgMasterDictionaryByReqSchemaId method ");
		try {
			if (reqSchemaId != null) {
				tDMAdminService.deleteTdgMasterDictionaryByReqSchemaId(reqSchemaId,
						manualDictionaryId);
			}
		} catch (Exception ex) {
			baseDTO.setMessage(ex.getMessage());
			baseDTO.setMessageConstant(TdgCentralConstant.FAILED_MESSAGE);
			return "redirect:tdgMasterDictionaryDashboard";
		}
		logger.info(strClassName + strMethodName
				+ " return from deleteTdgMasterDictionaryByReqSchemaId method");
		return "redirect:tdgMasterDictionaryDashboard";
	}
	
	@RequestMapping(value = "/downloadTdgMasterDictionaryByReqSchemaId")
	public void downloadTdgMasterDictionaryByReqSchemaId(
			@RequestParam(value = "reqSchemaId", required = false) String reqSchemaId,
			@RequestParam(value = "manualDictionaryId", required = false) String manualDictionaryId,
			ModelMap model, HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute("baseDTO") BaseDTO baseDTO){
		String strMethodName = " [ downloadTdgMasterDictionaryByReqSchemaId() ]";
		logger.info(strClassName + strMethodName
				+ " inside of downloadTdgMasterDictionaryByReqSchemaId method ");
		try {
			if (reqSchemaId != null) {
				TdgSchemaDTO schemaDTO = tDMAdminService
						.getSchemaDetails(reqSchemaId);
				    /*Properties props = new Properties();
			        props.setProperty(TdgCentralConstant.SCHEMA_URL, schemaDTO.getUrl());
			        props.setProperty(TdgCentralConstant.SCHEMA_PASS, schemaDTO.getPassword());
			        props.setProperty(TdgCentralConstant.SCHEMA_DATE_FORMATE, schemaDTO.getDateformate());
			        props.setProperty(TdgCentralConstant.SCHEMA_BUSINESS_RULES, schemaDTO.getBusinessrules()!= null ? schemaDTO.getBusinessrules() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_DEPENDS_DBS, schemaDTO.getColumnsdepends()!= null ? schemaDTO.getColumnsdepends() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_MASTER_TABS, schemaDTO.getSchemamastertables()!= null ? schemaDTO.getSchemamastertables() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_PASSED_TABS, schemaDTO.getSchemapasstabs()!= null ? schemaDTO.getSchemapasstabs() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_REQUESTED_COLUMNS, schemaDTO.getRequiredcolumns()!= null ? schemaDTO.getRequiredcolumns() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_SEQUENCE_PREFIX_TABS, schemaDTO.getSeqtableprefix()!= null ? schemaDTO.getSeqtableprefix() : "");
			        //props.setProperty(TdgCentralConstant.SCHEMA_, schemaDTO.getUrl());
			        for(TdgGuiDetailsDTO dto : schemaDTO.getTdgGuiDetailsDTOs()){
			        	props.setProperty(dto.getColumnname(), dto.getColumnLabel()+";"+dto.getColumnType()+";"+(dto.getColumnValues() != null ? dto.getColumnValues() : "")+";");
			        }
			        File f = new File(schemaDTO.getSchemaname()+".properties");
			        OutputStream out = new FileOutputStream( f );
			        
			        
			        props.store(out,"");
			        
			        response.setContentType("text/properties");
					String disposition = "attachment; fileName="+schemaDTO.getSchemaname()+".properties";
					response.setHeader("Content-Disposition", disposition);
					
					
					byte[] arBytes = new byte[(int) f.length()];
					FileInputStream is = new FileInputStream(f);
					is.read(arBytes);*/
					/*ServletOutputStream op = response.getOutputStream();
					op.write(arBytes);*/
					//op.flush();
					//is.close();
					/*response.getOutputStream().write(arBytes);
					response.getOutputStream().flush();*/
					//is.close();
					
					
					
					/*response.getOutputStream().write(out.);Writer().append(
							CSVGenerator.getCSV(colvalMap, tdgRequestListDTO.getRequestCount(),
									tdgRequestListDTO.getListGeneratedData()));*/
				
				response.setContentType("text/text");
				String disposition = "attachment; fileName="+schemaDTO.getSchemaname()+".properties";
				response.setHeader("Content-Disposition", disposition);
				response.getWriter().append(
						CSVGenerator.getPropertiesFile(schemaDTO));
				response.getWriter().flush();
			}
		} catch (Exception ex) {
			baseDTO.setMessage(ex.getMessage());
			baseDTO.setMessageConstant(TdgCentralConstant.FAILED_MESSAGE);
			//return "redirect:tdgMasterDictionaryDashboard";
		}
		logger.info(strClassName + strMethodName
				+ " return from downloadTdgMasterDictionaryByReqSchemaId method");
		//return "redirect:tdgMasterDictionaryDashboard";
	}

	@RequestMapping(value = "/tesdaMasterDictionary", headers = ("content-type=multipart/*"), method = RequestMethod.POST)
	public String uploadMasterDictionaryData(
			@ModelAttribute("tdgDictionaryDTO") TdgDictionaryDTO tdgDictionaryDTO, ModelMap model){
		String strMethodName = " [ uploadMasterDictionaryData() ]";
		List<String> listValidation = new ArrayList<String>();
		String strMessage = "";
		MultipartFile file = tdgDictionaryDTO.getMaltiPartFile();
		logger.info(strClassName + strMethodName + " inside of uploadMasterData post method ");
		TdgDictionaryDTO tdgDictionaryDTOTemp = new TdgDictionaryDTO();
		try {		
		User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (!file.isEmpty()) {
			
				//Properties props = new TdgSequencedProperties();
			Map<String,String> props = new LinkedHashMap<String,String>();
			BufferedReader br = null;
	        String strLine = "";
	        try {
	            br = new BufferedReader(new InputStreamReader(file.getInputStream()));
	            while( (strLine = br.readLine()) != null){
	                if(strLine.contains("=")){
	                	props.put(strLine.substring(0,strLine.indexOf("=")), strLine.substring(strLine.indexOf("=")+1,strLine.length()));
	                }
	            }
	        } catch (FileNotFoundException e) {
	            System.err.println("Unable to find the file: fileName");
	        } catch (IOException e) {
	            System.err.println("Unable to read the file: fileName");
	        }
				//props.load(file.getInputStream());
				listValidation.addAll(validateProps(props));
				if (listValidation.isEmpty()) {
					/**
					 * Going for dump the properties file into DB
					 */
					TdgSchemaDTO tdgSchemaDTO = new TdgSchemaDTO();
					Set<TdgGuiDetailsDTO> setTdgGuiDetailsDTOs = new LinkedHashSet<TdgGuiDetailsDTO>();
					tdgSchemaDTO.setUrl(props.get(TdgCentralConstant.SCHEMA_URL));
					boolean bPostGresCheck = false;
					if(tdgSchemaDTO.getUrl().toLowerCase().contains(TdgCentralConstant.DB_TYPE_POSTGRES.toLowerCase()))
						bPostGresCheck = true;
					tdgSchemaDTO.setUsername(props.get(TdgCentralConstant.SCHEMA_NAME));
					tdgSchemaDTO.setPassword(props.get(TdgCentralConstant.SCHEMA_PASS));
					tdgSchemaDTO.setColumnsdepends(props
							.get(TdgCentralConstant.SCHEMA_DEPENDS_DBS));
					tdgSchemaDTO.setSchemamastertables(props
							.get(TdgCentralConstant.SCHEMA_MASTER_TABS));
					tdgSchemaDTO.setSeqtableprefix(props
							.get(TdgCentralConstant.SCHEMA_SEQUENCE_PREFIX_TABS));
					tdgSchemaDTO.setSchemapasstabs(props
							.get(TdgCentralConstant.SCHEMA_PASSED_TABS));
					tdgSchemaDTO.setUserid(user.getUsername());
					tdgSchemaDTO.setDateformate(props
							.get(TdgCentralConstant.SCHEMA_DATE_FORMATE));
					tdgSchemaDTO.setRequiredcolumns(props
							.get(TdgCentralConstant.SCHEMA_REQUESTED_COLUMNS));
					tdgSchemaDTO.setBusinessrules(props.get(TdgCentralConstant.SCHEMA_BUSINESS_RULES));
					tdgSchemaDTO.setSchemaname(file.getOriginalFilename().substring(0,file.getOriginalFilename().indexOf(".")));
					//going for check dbconnection on tdg 
					List<String> listDisplayNames = tDMAdminService.checkDataConnections(user.getUsername(),tdgSchemaDTO.getUrl(),tdgSchemaDTO.getUsername(),tdgSchemaDTO.getPassword());
					logger.info("Database connection succeed.....");
					StringBuffer strBuffer = new StringBuffer();
					for(String str: listDisplayNames){
						if(strBuffer.length()>0)
							strBuffer.append("#");
						strBuffer.append(str);
					}
					tdgSchemaDTO.setDataconnections(strBuffer.toString());
					//end of checking
					//Set<Entry<Object, Object>> keys = props.entrySet();
					for (Map.Entry<String, String> obj : props.entrySet()) {
						TdgGuiDetailsDTO tdgGuiDetailsDTO = new TdgGuiDetailsDTO();
						if (!String.valueOf(obj.getKey()).startsWith("SCHEMA_")) {
							if(!bPostGresCheck){
							tdgGuiDetailsDTO.setColumnname(String.valueOf(obj.getKey())
									.toUpperCase().trim());
							}else{
								tdgGuiDetailsDTO.setColumnname(String.valueOf(obj.getKey())
										.toLowerCase().trim());
							}
							Object objValue = props.get(obj.getKey());
							if (objValue != null && String.valueOf(objValue).contains(";")) {
								String[] arraySplit = String.valueOf(objValue).split(";");
								/**
								 * Going to set Column Label Name
								 */
								tdgGuiDetailsDTO.setColumnLabel(arraySplit[0]);
								/**
								 * Going to set GUI field Type
								 */
								if (arraySplit.length > 1
										&& (arraySplit[1] == null || "".equals(arraySplit[1]))) {
									tdgGuiDetailsDTO
											.setColumnLabel(TdgCentralConstant.GUI_TEXT_BOX);
								} else {
									tdgGuiDetailsDTO.setColumnType(arraySplit[1].toUpperCase());
								}
								/**
								 * Going to set Column Values
								 */
								if (arraySplit.length >= 3) {
									tdgGuiDetailsDTO.setColumnValues(arraySplit[2]);
								} else {
									tdgGuiDetailsDTO.setColumnValues("");
								}
								tdgGuiDetailsDTO.setTdgSchemaDTO(tdgSchemaDTO);
								setTdgGuiDetailsDTOs.add(tdgGuiDetailsDTO);
							}
						}
					}
					tdgSchemaDTO.setTdgGuiDetailsDTOs(setTdgGuiDetailsDTOs);
					strMessage = tDMAdminService.saveTdgSchemaDetails(tdgSchemaDTO);
				}
			
		} else {
			if (logger.isDebugEnabled())
				logger.debug(strClassName + strMethodName + " File is empty while upload the file");
			listValidation.add("You failed to upload " + file.getName()
					+ " because the file was empty.");
		}
		tdgDictionaryDTO.setErrors(listValidation);
		
		if (null != listValidation && !listValidation.isEmpty()) {
			tdgDictionaryDTOTemp.setErrors(listValidation);
		} else {
			tdgDictionaryDTOTemp.setErrors(null);
		}
		if (TdgCentralConstant.SUCCESS_MESSAGE.equals(strMessage)) {
			tdgDictionaryDTOTemp.setMessageConstant(TdgCentralConstant.SUCCESS_MESSAGE);
			tdgDictionaryDTOTemp.setMessage("Data dictionary uploaded successfully");
		} else {
			tdgDictionaryDTOTemp.setMessageConstant(TdgCentralConstant.FAILED_MESSAGE);
			StringBuffer strBuffer = new StringBuffer();
			for(String strValue : listValidation){
				strBuffer.append(strValue);
			}
			tdgDictionaryDTOTemp.setMessage(strBuffer.toString());
		}
		//model.addAttribute("tdgDictionaryDTO", tdgDictionaryDTOTemp);
		logger.info(strClassName + strMethodName + " return from uploadMasterData post method ");
		} catch (Exception e) {
			logger.error(strClassName + strMethodName + " " + e.getMessage());
			//listValidation.add("You failed to upload " + file.getName() + " => Cause of authentication problems");
			tdgDictionaryDTOTemp.setMessageConstant(TdgCentralConstant.FAILED_MESSAGE);
			tdgDictionaryDTOTemp.setMessage("You failed to upload " + file.getName() + " => Cause of authentication problems");
		}
		model.addAttribute("tdgDictionaryDTO", tdgDictionaryDTOTemp);
		return "tdgMasterDictionary";
		// return "";
	}

	private List<String> validateProps(Properties props){
		List<String> listInvalidEntries = new ArrayList<String>();
		List<String> listColNames = new ArrayList<String>();
		Set<Entry<Object, Object>> keys = props.entrySet();
		StringBuffer strCheckValues = new StringBuffer();
		String strDepends = props.getProperty(TdgCentralConstant.SCHEMA_DEPENDS_DBS);
		if (strDepends != null && strDepends.contains(";")) {
			String strArrays[] = strDepends.split(";");
			for (int i = 0; i < strArrays.length; i++) {
				if (strArrays[i] != null && strArrays[i].contains("#")) {
					String strColsArrays[] = strArrays[i].split("#");
					listColNames.add(strColsArrays[0]);
					listColNames.add(strColsArrays[1]);
				}
			}
		}
		for (Entry<Object, Object> obj : keys) {
			if (!String.valueOf(obj.getKey()).startsWith("SCHEMA_")) {
				listColNames.add(String.valueOf(obj.getKey()).toUpperCase());
				if (StringUtils.countMatches(String.valueOf(props.get(obj.getKey())), ";") != 3) {
					strCheckValues.append(String.valueOf(obj.getKey()));
					strCheckValues.append(',');
				}
			}
		}
		String strUrl = props.getProperty(TdgCentralConstant.SCHEMA_URL);
		String strUserName = props.getProperty(TdgCentralConstant.SCHEMA_NAME);
		String strPassword = props.getProperty(TdgCentralConstant.SCHEMA_PASS);
		String strDateFormate = props.getProperty(TdgCentralConstant.SCHEMA_DATE_FORMATE);
		if (strUrl == null || "".equals(strUrl.trim()) ) {
			listInvalidEntries.add(TdgCentralConstant.SCHEMA_URL + " is invalid");
		}
		if (strUserName == null || "".equals(strUserName.trim()) || strUserName.contains(";")) {
			listInvalidEntries.add(TdgCentralConstant.SCHEMA_NAME + " is invalid");
		}
		if (strPassword == null || "".equals(strPassword.trim()) || strPassword.contains(";")) {
			listInvalidEntries.add(TdgCentralConstant.SCHEMA_PASS + " is invalid");
		}
		if (strDateFormate == null || "".equals(strDateFormate.trim())
				|| strDateFormate.contains(";")) {
			listInvalidEntries.add(TdgCentralConstant.SCHEMA_DATE_FORMATE + " is invalid");
		}
		String strPassedTabs = props.getProperty(TdgCentralConstant.SCHEMA_PASSED_TABS);
		List<String> listPassedTabs = new ArrayList<String>();
		if (StringUtils.isNotEmpty(strPassedTabs)) {
			if (strPassedTabs.contains(";")) {
				String strArrays[] = strPassedTabs.split(";");
				if (strArrays.length >= 1) {
					if (strArrays[0].contains(",")) {
						String[] strMasterTabs = strArrays[0].split(",");
						for (String strVal : strMasterTabs) {
							listPassedTabs.add(strVal.toUpperCase().trim());
						}
					} else {
						listPassedTabs.add(strArrays[0].toUpperCase().trim());
					}
				}
			}
		}
		if (listInvalidEntries.isEmpty()) {
			List<String> listCols = tDMAdminService.getColsByTabs(strUrl.trim(),
					strUserName.trim(), strPassword.trim(), listPassedTabs);
			StringBuffer strBuffer = new StringBuffer();
			for (String strPassCol : listColNames) {
				boolean bCheck = false;
				if (strPassCol.contains("#")) {
					String[] strArray = strPassCol.split("#");
					int iCheckForAll = 0;
					for (int i = 0; i < strArray.length; i++) {
						for (String strExistCol : listCols) {
							if (strArray[i].equals(strExistCol)) {
								bCheck = true;
								iCheckForAll++;
								break;
							}
						}
					}
					if (iCheckForAll != strArray.length) {
						bCheck = false;
					}
				} else {
					for (String strExistCol : listCols) {
						if (strPassCol.equals(strExistCol)) {
							bCheck = true;
							break;
						}
					}
				}
				if (!bCheck) {
					if(StringUtils.isNotEmpty(strBuffer.toString())){
						strBuffer.append(",");
					}
					strBuffer.append(strPassCol);
				}
			}
			if (strBuffer.toString().contains(",") && strBuffer.toString().endsWith(",")) {
				listInvalidEntries.add(strBuffer.substring(0, strBuffer.length() - 1)
						+ " are invalid Column names ");
			} else if (!strBuffer.toString().isEmpty()) {
				listInvalidEntries.add(strBuffer + " are invalid Column names ");
			}
		}
		if (strCheckValues.toString().contains(",") && strCheckValues.toString().endsWith(",")) {
			listInvalidEntries.add(strCheckValues.substring(0, strCheckValues.length() - 1)
					+ " related values are not valid ");
		} else if (!strCheckValues.toString().isEmpty()) {
			listInvalidEntries.add(strCheckValues + " related values are not valid ");
		}
		return listInvalidEntries;
	}
	
	//convert properties into map 
	private List<String> validateProps(Map<String,String> props){
		List<String> listInvalidEntries = new ArrayList<String>();
		List<String> listColNames = new ArrayList<String>();
		//Set<Entry<Object, Object>> keys = props.entrySet();
		StringBuffer strCheckValues = new StringBuffer();
		String strDepends = props.get(TdgCentralConstant.SCHEMA_DEPENDS_DBS);
		if (strDepends != null && strDepends.contains(";")) {
			String strArrays[] = strDepends.split(";");
			for (int i = 0; i < strArrays.length; i++) {
				if (strArrays[i] != null && strArrays[i].contains("#")) {
					String strColsArrays[] = strArrays[i].split("#");
					listColNames.add(strColsArrays[0]);
					listColNames.add(strColsArrays[1]);
				}
			}
		}
		for (Map.Entry<String, String> obj : props.entrySet()) {
			if (!obj.getKey().startsWith("SCHEMA_")) {
				listColNames.add(obj.getKey().toUpperCase());
				if (StringUtils.countMatches(props.get(obj.getKey()), ";") != 3) {
					strCheckValues.append(obj.getKey());
					strCheckValues.append(',');
				}
			}
		}
		String strUrl = props.get(TdgCentralConstant.SCHEMA_URL);
		String strUserName = props.get(TdgCentralConstant.SCHEMA_NAME);
		String strPassword = props.get(TdgCentralConstant.SCHEMA_PASS);
		String strDateFormate = props.get(TdgCentralConstant.SCHEMA_DATE_FORMATE);
		if (strUrl == null || "".equals(strUrl.trim()) ) {
			listInvalidEntries.add(TdgCentralConstant.SCHEMA_URL + " is invalid");
		}
		if (strUserName == null || "".equals(strUserName.trim()) || strUserName.contains(";")) {
			listInvalidEntries.add(TdgCentralConstant.SCHEMA_NAME + " is invalid");
		}
		if (strPassword == null || "".equals(strPassword.trim()) || strPassword.contains(";")) {
			listInvalidEntries.add(TdgCentralConstant.SCHEMA_PASS + " is invalid");
		}
		if (strDateFormate == null || "".equals(strDateFormate.trim())
				|| strDateFormate.contains(";")) {
			listInvalidEntries.add(TdgCentralConstant.SCHEMA_DATE_FORMATE + " is invalid");
		}
		String strPassedTabs = props.get(TdgCentralConstant.SCHEMA_PASSED_TABS);
		List<String> listPassedTabs = new ArrayList<String>();
		if (StringUtils.isNotEmpty(strPassedTabs)) {
			if (strPassedTabs.contains(";")) {
				String strArrays[] = strPassedTabs.split(";");
				if (strArrays.length >= 1) {
					if (strArrays[0].contains(",")) {
						String[] strMasterTabs = strArrays[0].split(",");
						for (String strVal : strMasterTabs) {
							listPassedTabs.add(strVal.toUpperCase().trim());
						}
					} else {
						listPassedTabs.add(strArrays[0].toUpperCase().trim());
					}
				}
			}
		}
		if (listInvalidEntries.isEmpty()) {
			List<String> listCols = tDMAdminService.getColsByTabs(strUrl.trim(),
					strUserName.trim(), strPassword.trim(), listPassedTabs);
			StringBuffer strBuffer = new StringBuffer();
			for (String strPassCol : listColNames) {
				boolean bCheck = false;
				if (strPassCol.contains("#")) {
					String[] strArray = strPassCol.split("#");
					int iCheckForAll = 0;
					for (int i = 0; i < strArray.length; i++) {
						for (String strExistCol : listCols) {
							if (strArray[i].equals(strExistCol)) {
								bCheck = true;
								iCheckForAll++;
								break;
							}
						}
					}
					if (iCheckForAll != strArray.length) {
						bCheck = false;
					}
				} else {
					for (String strExistCol : listCols) {
						if (strPassCol.equals(strExistCol)) {
							bCheck = true;
							break;
						}
					}
				}
				if (!bCheck) {
					if(StringUtils.isNotEmpty(strBuffer.toString())){
						strBuffer.append(",");
					}
					strBuffer.append(strPassCol);
				}
			}
			if (strBuffer.toString().contains(",") && strBuffer.toString().endsWith(",")) {
				listInvalidEntries.add(strBuffer.substring(0, strBuffer.length() - 1)
						+ " are invalid Column names ");
			} else if (!strBuffer.toString().isEmpty()) {
				listInvalidEntries.add(strBuffer + " are invalid Column names ");
			}
		}
		if (strCheckValues.toString().contains(",") && strCheckValues.toString().endsWith(",")) {
			listInvalidEntries.add(strCheckValues.substring(0, strCheckValues.length() - 1)
					+ " related values are not valid ");
		} else if (!strCheckValues.toString().isEmpty()) {
			listInvalidEntries.add(strCheckValues + " related values are not valid ");
		}
		return listInvalidEntries;
	}

	@RequestMapping(value = "/uploadManualDictionary", method = RequestMethod.GET)
	public String uploadManualDictionaryGet(
			@RequestParam(value = "reqSchemaId", required = false) String reqSchemaId,
			@ModelAttribute("tdgDictionaryDTO") TdgDictionaryDTO tdgDictionaryDTO, ModelMap model,
			HttpServletRequest request, HttpServletResponse response){
		String strMethodName = " [ uploadManualDictionaryGet() ]";
		logger.info(strClassName + strMethodName
				+ " inside of uploadManualDictionaryGet get method ");
		List<String> listDictionaries = tDMAdminService.fetchAllManualDictionaries();
		tdgDictionaryDTO.setListManualDictionaries(listDictionaries);
		request.setAttribute("reqSchemaId", reqSchemaId);
		tdgDictionaryDTO.setSchemaid(reqSchemaId);
		model.addAttribute("tdgDictionaryDTO", tdgDictionaryDTO);
		logger.info(strClassName + strMethodName
				+ " return from schemaDetails method for schemaid : " + reqSchemaId);
		return "manualDictionary";
	}

	@RequestMapping(value = "/uploadManualDictionary", method = RequestMethod.POST)
	public ModelAndView uploadManualDictionaryPost(
			@RequestParam(value = "reqSchemaId", required = false) String reqSchemaId,
			@ModelAttribute("tdgDictionaryDTO") TdgDictionaryDTO tdgDictionaryDTO, ModelMap model,
			HttpServletRequest request, HttpServletResponse response, RedirectAttributes redir){
		String strMethodName = " [ uploadManualDictionaryGet() ]";
		BaseDTO baseDTO = new BaseDTO();
		logger.info(strClassName + strMethodName
				+ " inside of uploadManualDictionaryGet get method "
				+ request.getAttribute("reqSchemaId"));
		ModelAndView modelAndView = new ModelAndView();
		MultipartFile file = tdgDictionaryDTO.getMaltiPartFile();
		TdgExcelOperationsUtil tdgExcelValues = new TdgExcelOperationsUtil();
		Map<String, List<String>> mapResult = null;
		if(file != null){
		try {
			mapResult = tdgExcelValues.readFile(file.getOriginalFilename(), file.getInputStream());
		} catch (IOException e) {
			logger.error("Error occured while reading file", e);
		}
		}
		if (mapResult != null && !mapResult.isEmpty()) {
			String strResponse = tDMAdminService.saveManualDictionaryDetails(
					file.getOriginalFilename(), mapResult, tdgDictionaryDTO.getSchemaid());
			String tabName = file.getOriginalFilename().toUpperCase();
			if (tabName.toUpperCase().contains(".")) {
				tabName = tabName.substring(0, tabName.indexOf(".")).toUpperCase();
			}
			tdgAsyncServiceImpl.doDumpManualDictionaryValues(tabName, mapResult);
			if (!StringUtils.isEmpty(strResponse)
					&& TdgCentralConstant.SUCCESS_MESSAGE.equals(strResponse)) {
				baseDTO.setMessage("Manual dictionary uploaded sucessfully for schemaid : "
						+ tdgDictionaryDTO.getSchemaid());
				baseDTO.setMessageConstant(TdgCentralConstant.SUCCESS_MESSAGE);
			} else {
				baseDTO.setMessage("Manual dictionary failed to upload schemaid : "
						+ tdgDictionaryDTO.getSchemaid());
				baseDTO.setMessageConstant(TdgCentralConstant.FAILED_MESSAGE);
			}
		}else if(StringUtils.isNotEmpty(tdgDictionaryDTO.getManualDictionary())){
			String strResponse = tDMAdminService.saveManualDictionaryDetails(
					tdgDictionaryDTO.getManualDictionary(), null, tdgDictionaryDTO.getSchemaid());
			if (!StringUtils.isEmpty(strResponse)
					&& TdgCentralConstant.SUCCESS_MESSAGE.equals(strResponse)) {
				baseDTO.setMessage("Manual Dictionary table is attached sucessfully for schemaid : "
						+ tdgDictionaryDTO.getSchemaid());
				baseDTO.setMessageConstant(TdgCentralConstant.SUCCESS_MESSAGE);
			} else {
				baseDTO.setMessage("Manual dictionary attachement is failed to upload schemaid : "
						+ tdgDictionaryDTO.getSchemaid());
				baseDTO.setMessageConstant(TdgCentralConstant.FAILED_MESSAGE);
			}
		} else {
			baseDTO.setMessage("Manual dictionary failed to upload schemaid : "
					+ tdgDictionaryDTO.getSchemaid());
			baseDTO.setMessageConstant(TdgCentralConstant.FAILED_MESSAGE);
		}
		logger.info(strClassName + strMethodName + " return from schemaDetails method ");
		modelAndView.setViewName("redirect:tdgMasterDictionaryDashboard");
		redir.addFlashAttribute("baseDTO", baseDTO);
		return modelAndView;
	}
	
	
	
	@RequestMapping(value = "/editTdgMasterDictionary")
	public String editTdgMasterDictionary(
			@RequestParam(value = "reqSchemaId", required = false) String reqSchemaId,
			ModelMap model, HttpServletRequest request, HttpServletResponse response,RedirectAttributes redAttr
			){
		String strMethodName = " [ editTdgMasterDictionary() ]";
		logger.info(strClassName + strMethodName
				+ " inside of editTdgMasterDictionary method ");
		try {
			if (reqSchemaId != null) {
				/*TdgSchemaDTO schemaDTO = tDMAdminService
						.getSchemaDetails(reqSchemaId);*/
				    /*Properties props = new Properties();
			        props.setProperty(TdgCentralConstant.SCHEMA_URL, schemaDTO.getUrl());
			        props.setProperty(TdgCentralConstant.SCHEMA_PASS, schemaDTO.getPassword());
			        props.setProperty(TdgCentralConstant.SCHEMA_DATE_FORMATE, schemaDTO.getDateformate());
			        props.setProperty(TdgCentralConstant.SCHEMA_BUSINESS_RULES, schemaDTO.getBusinessrules()!= null ? schemaDTO.getBusinessrules() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_DEPENDS_DBS, schemaDTO.getColumnsdepends()!= null ? schemaDTO.getColumnsdepends() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_MASTER_TABS, schemaDTO.getSchemamastertables()!= null ? schemaDTO.getSchemamastertables() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_PASSED_TABS, schemaDTO.getSchemapasstabs()!= null ? schemaDTO.getSchemapasstabs() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_REQUESTED_COLUMNS, schemaDTO.getRequiredcolumns()!= null ? schemaDTO.getRequiredcolumns() : "");
			        props.setProperty(TdgCentralConstant.SCHEMA_SEQUENCE_PREFIX_TABS, schemaDTO.getSeqtableprefix()!= null ? schemaDTO.getSeqtableprefix() : "");
			        //props.setProperty(TdgCentralConstant.SCHEMA_, schemaDTO.getUrl());
			        for(TdgGuiDetailsDTO dto : schemaDTO.getTdgGuiDetailsDTOs()){
			        	props.setProperty(dto.getColumnname(), dto.getColumnLabel()+";"+dto.getColumnType()+";"+(dto.getColumnValues() != null ? dto.getColumnValues() : "")+";");
			        }
			        File f = new File(schemaDTO.getSchemaname()+".properties");
			        OutputStream out = new FileOutputStream( f );
			        
			        
			        props.store(out,"");
			        
			        response.setContentType("text/properties");
					String disposition = "attachment; fileName="+schemaDTO.getSchemaname()+".properties";
					response.setHeader("Content-Disposition", disposition);
					
					
					byte[] arBytes = new byte[(int) f.length()];
					FileInputStream is = new FileInputStream(f);
					is.read(arBytes);*/
					/*ServletOutputStream op = response.getOutputStream();
					op.write(arBytes);*/
					//op.flush();
					//is.close();
					/*response.getOutputStream().write(arBytes);
					response.getOutputStream().flush();*/
					//is.close();
					
					
					
					/*response.getOutputStream().write(out.);Writer().append(
							CSVGenerator.getCSV(colvalMap, tdgRequestListDTO.getRequestCount(),
									tdgRequestListDTO.getListGeneratedData()));*/
				
				//TdgMasterDictionaryDTO tdgMasterDictionaryDTO = new TdgMasterDictionaryDTO();
				redAttr.addFlashAttribute("schemaId", reqSchemaId);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			//return "redirect:tdgMasterDictionaryDashboard";
		}
		logger.info(strClassName + strMethodName
				+ " return from downloadTdgMasterDictionaryByReqSchemaId method");
		return "redirect:tdgaCreateMasterDictionary";
	}
	
	@RequestMapping(value = "/tdgaManualDictionaryDashboard", method = RequestMethod.GET)
	public String deleteManualDictionaryGet(
			@RequestParam(value = "dictionaryname", required = false) String dictionaryname,
			@ModelAttribute("baseDTO") BaseDTO dto, ModelMap model,
			HttpServletRequest request, HttpServletResponse response){
		String strMethodName = " [ deleteManualDictionaryGet() ]";
		logger.info(strClassName + strMethodName
				+ " inside of deleteManualDictionaryGet get method ");
		if(StringUtils.isNotEmpty(dictionaryname)){
			tDMAdminService.dropManualDictionary(dictionaryname);
		}
		List<String> listDictionaries = tDMAdminService.fetchAllManualDictionaries();
		List<TdgSchemaDTO> lstDTO = tDMAdminService.fetchAllTdgSchemaDetails();
		for(TdgSchemaDTO dtos :lstDTO)
			if(StringUtils.isNotEmpty(dtos.getManualdictionary())){
			listDictionaries.remove(dtos.getManualdictionary());
			listDictionaries.add(dtos.getManualdictionary()+"EXIST");
			}
		model.addAttribute("baseDTO", dto);
		model.addAttribute("listDictionaries", listDictionaries);
		logger.info(strClassName + strMethodName
				+ " return from deleteManualDictionaryGet method ");
		return "tdgManualDictionaryDashboard";
	}
}
