<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="org.json.simple.*, java.sql.*, java.net.URLEncoder" %>

<%
//초기 선언
	int articleCnt = 0;

	JSONObject jsonMain = new JSONObject();
	JSONArray jArray = new JSONArray();
	JSONObject jObject = new JSONObject();

	String startIdxStr = request.getParameter("STARTIDX");
	String endIdxStr = request.getParameter("ENDIDX");
	int startIdx = Integer.parseInt(startIdxStr);
	int endIdx = Integer.parseInt(endIdxStr);
	
	if(startIdxStr.equals("")) {
		jObject.put("RESULT", "0");
		// 0 송신
	} else if(endIdxStr.equals("")) {
		jObject.put("RESULT", "0");
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
			ResultSet rs = stmt.executeQuery("select id, title, time, hit from board where num >= " + startIdxStr + " and num <= " + endIdxStr + ";");
			while(rs.next()) {
				jObject = new JSONObject();
				String id = URLEncoder.encode(rs.getString("id"), "UTF-8");
				String title = URLEncoder.encode(rs.getString("title"), "UTF-8");
				String time = URLEncoder.encode(rs.getString("time"), "UTF-8");
				String hit = URLEncoder.encode(rs.getString("hit"), "UTF-8");
				jObject.put("id", id);
				jObject.put("title", title);
				jObject.put("time", time);
				jObject.put("hit", hit);
				jArray.add(articleCnt++, jObject);
			}
			
			rs = stmt.executeQuery("SELECT COUNT(*) FROM board");
			if(rs.next()) {
				jObject = new JSONObject();
				String cnt = URLEncoder.encode(rs.getString("COUNT(*)"));
				jsonMain.put("COUNT", cnt);
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
	jsonMain.put("List", jArray);
    String cntStr = String.format("%d", articleCnt);
	jsonMain.put("RESULT", cntStr);
        
        // 안드로이드에 보낼 데이터를 출력
	out.println(jsonMain.toJSONString());
%>