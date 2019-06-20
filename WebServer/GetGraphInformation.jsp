<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="org.json.simple.*, java.sql.*, java.net.URLEncoder, java.text.SimpleDateFormat, java.util.Date, java.util.Calendar"
    import="java.util.ArrayList, java.util.List, java.lang.StringBuilder, java.lang.Math" %>

<%
//초기 선언
	JSONObject jsonMain = new JSONObject();
	JSONArray jArray = new JSONArray();
	JSONObject jObject;

	String id = request.getParameter("ID");
	
	if(id.equals("")) {
		jsonMain.put("result", "0");
		// 0 송신
	} else {
		jsonMain.put("result", "0");
		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/smartcage", "root", "passwd");
			if (conn == null) {
				jsonMain.put("result", "0");
			}
			stmt = conn.createStatement();
			
			int i = 0;
			int j = 0;
			int[][] distance = new int[7][24];
			int[][] route = new int[32][24];
			int hourCounter = 0;
			
			for(i = 0; i<7; i++) {
				for(j = 0; j<24; j++) {
				
					Date today = new Date();
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					
					Calendar week = Calendar.getInstance();
					int minus = hourCounter * -1;
					week.add(Calendar.HOUR, minus);
					String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(week.getTime());
					
					week = Calendar.getInstance();
					minus = (hourCounter + 1) * -1;
					week.add(Calendar.HOUR, minus);
					String dateBefore = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(week.getTime());
					
					hourCounter++;
					
					String command = String.format("select id, route, date from route where date >= '%s' and date <= '%s' and id = '%s';", dateBefore, dateStr, id);
					//out.println(command);
					
					ResultSet rs = stmt.executeQuery(command);
					List routeList = new ArrayList();
					String[] splits;
					while(rs.next()) {
						jObject = new JSONObject();
						jsonMain.put("result", "1");
						//String gotId = URLEncoder.encode(rs.getString("id"), "UTF-8");
						//String route = URLEncoder.encode(rs.getString("route"), "UTF-8");
						String routeStr = rs.getString("route");
						splits = routeStr.split("-");
						for (String item : splits) {
							routeList.add(item);
						}
						//String date = URLEncoder.encode(rs.getString("date"), "UTF-8");
						//jObject.put("id", gotId);
						//jObject.put("route", route);
						//jObject.put("date", date);
						//jArray.add(cnt, jObject);
					}
					
					if(!(routeList.size() == 0)) {
					
						String firstLoc = routeList.get(0).toString();
						String[] locSplits = firstLoc.split(",");
						int prevX = new Integer(locSplits[0]);
						int prevY = new Integer(locSplits[1]);
						int whereRouteX = prevX / 20;
						int whereRouteY = prevY / 20;
						int nowX = 0, nowY = 0;
						int totalDistance = 0;
						
						for(int k=1; k<routeList.size(); k++) {
							String item = routeList.get(k).toString();
							locSplits = item.split(",");
							nowX = new Integer(locSplits[0]);
							nowY = new Integer(locSplits[1]);
							
							int distanceX = Math.abs(nowX - prevX);
							int distanceY = Math.abs(nowY - prevY);
							double distanceResult = Math.sqrt((distanceX*distanceX) + (distanceY*distanceY));
							int distanceInt = (int)distanceResult;
							totalDistance += distanceInt;
							
							whereRouteX = nowX / 20;
							whereRouteY = nowY / 20;
							if(whereRouteX >= 31) whereRouteX = 31;
							if(whereRouteY >= 23) whereRouteY = 23;
							route[whereRouteX][whereRouteY]++;
							
							//String tmpx = String.format("%d", whereRouteX);
							//String tmpy = String.format("%d", whereRouteY);
							//out.println(tmpx + "~" + tmpy);
							
							prevX = nowX;
							prevY = nowY;
						}
						distance[i][j] = totalDistance;
					}
				}
			}
			
			StringBuilder bd = new StringBuilder();
			//out.println("Distance");
			//out.println("<BR>");
			for(i = 0; i<7; i++) {
				for(j=0; j<24; j++) {
					String tmpx = String.format("%d", distance[i][j]);
					if(i == 0 && j == 0) {
						bd.append(tmpx);
					} else {
						bd.append("," + tmpx);
					}
					//out.println(tmpx + "  ");
				}
				//out.println("<BR>");
			}
			jsonMain.put("distance", bd.toString());
			
			//out.println("Route");
			//out.println("<BR>");
			bd = new StringBuilder();
			for(i = 0; i<32; i++) {
				for(j=0; j<24; j++) {
					String tmpx = String.format("%d", route[i][j]);
					if(i == 0 && j == 0) {
						bd.append(tmpx);
					} else {
						bd.append("," + tmpx);
					}
				}
				//out.println("<BR>");
			}
			jsonMain.put("route", bd.toString());
		}
		// catch(Exception e) {
		//	out.println(e.toString());
		//}
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
	//jsonMain.put("List", jArray);
	
        // 안드로이드에 보낼 데이터를 출력
	out.println(jsonMain.toJSONString());
%>