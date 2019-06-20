<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="org.json.simple.*, java.sql.*, java.net.URLDecoder, java.net.URLEncoder" %>

<%
//초기 선언
	JSONObject jsonMain = new JSONObject();
	JSONArray jArray = new JSONArray();
	JSONObject jObject = new JSONObject();
	
	request.setCharacterEncoding("UTF-8");
	response.setContentType("text/html; charset=UTF-8");

	String id = request.getParameter("ID");

	if(id.equals("")) {
		jObject.put("RESULT", "0");
		// 0 송신
	} else {
		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/smartcage", "root", "passwd");
			if (conn == null) {
				jObject.put("RESULT", "0");
			}
			stmt = conn.createStatement();
			int n = stmt.executeUpdate("update cage set feedleft = 0 where id = '" + id + "';");
		
			if(n != 0) {
				jsonMain.put("RESULT", "1");
				//JSONObject jsonID = new JSONObject();
				//String id = rs.getString(1);
				//jsonMain.put("ID", id);
			}
			else {
				jsonMain.put("RESULT", "0");
			}
		}
		finally {
			try {
				stmt.close();
			} catch (Exception ignored) {
			}
			try {
				conn.close();
			} catch (Exception ignored) {
			}
		}
	}
	
        // 안드로이드에 보낼 데이터를 출력
	out.println(jsonMain.toJSONString());
%>