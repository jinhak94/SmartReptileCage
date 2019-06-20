<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="org.json.simple.*, java.sql.*, java.net.URLEncoder" %>

<%
//초기 선언
	JSONObject jsonMain = new JSONObject();
	JSONArray jArray = new JSONArray();
	JSONObject jObject = new JSONObject();

	String idxStr = request.getParameter("IDX");
	int idx = Integer.parseInt(idxStr);
	
	if(idxStr.equals("")) {
		jsonMain.put("RESULT", "0");
		// 0 송신
	} else {
		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/smartcage", "root", "passwd");
			if (conn == null) {
				jsonMain.put("RESULT", "0");
			}
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select id, title, memo, time, hit from board where num = " + idxStr + ";");
			if(rs.next()) {
				jsonMain.put("RESULT", "1");
				jObject = new JSONObject();
				String id = URLEncoder.encode(rs.getString("id"), "UTF-8");
				String title = URLEncoder.encode(rs.getString("title"), "UTF-8");
				String memo = URLEncoder.encode(rs.getString("memo"), "UTF-8");
				String time = URLEncoder.encode(rs.getString("time"), "UTF-8");
				String hit = URLEncoder.encode(rs.getString("hit"), "UTF-8");
				jsonMain.put("id", id);
				jsonMain.put("title", title);
				jsonMain.put("time", time);
				jsonMain.put("memo", memo);
				jsonMain.put("hit", hit);
			} else {
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

        // 최종적으로 배열을 하나로 묶음
        
        // 안드로이드에 보낼 데이터를 출력
	out.println(jsonMain.toJSONString());
%>