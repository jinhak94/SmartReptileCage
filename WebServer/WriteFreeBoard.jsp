<%@ page language="java" contentType="text/html; charset=EUC-KR"
    pageEncoding="EUC-KR" import="org.json.simple.*, java.sql.*, java.text.SimpleDateFormat, java.util.Date, java.net.URLEncoder, java.net.URLDecoder" %>

<%
//초기 선언
	request.setCharacterEncoding("EUC-KR");

	JSONObject jsonMain = new JSONObject();
	JSONArray jArray = new JSONArray();
	JSONObject jObject = new JSONObject();

	String id = request.getParameter("ID");
	String time = request.getParameter("TIME");
	String memo = URLDecoder.decode(request.getParameter("MEMO"), "utf-8");
	String title = URLDecoder.decode(request.getParameter("TITLE"), "utf-8");
	
	if(id.equals("") || title.equals("") || memo.equals("")) {
		jsonMain.put("RESULT", "0");
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
			// ResultSet rs = stmt.executeQuery("select id, password from userinfo where id = '" + id + "' && password = '" + password + "';");
			
			//SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
			//String dateStr = date.format(new Date());
			String query = String.format("insert into board(id, title, memo, time) values ('%s', '%s', '%s', '%s');", id, title, memo, time);
			int n = stmt.executeUpdate(query);
			if (n > 0) {
				jsonMain.put("RESULT", "1");
			} else {
				jsonMain.put("RESULT", "0");
			}
		}
		catch (Exception e) {
			jsonMain.put("RESULT", "0");
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