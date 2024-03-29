
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.jasypt.util.password.StrongPasswordEncryptor;


@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedbMaster");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");

        String username = request.getParameter("username");
        String password = request.getParameter("password");

       // String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        //System.out.println("gRecaptchaResponse=" + gRecaptchaResponse);
        try {
            int filler = 2;
            //RecaptchaVerifyUtils.verify(gRecaptchaResponse);
        } catch (Exception e) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT email, id, password FROM customers WHERE email = ?";

            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, username);
            //statement.setString(2, password);

            ResultSet rs = statement.executeQuery();
            JsonObject jsonObject = new JsonObject();

            boolean success = false;

            if (rs.next()) {
                String encryptedPassword = rs.getString("password");
                success = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
                if(success){
                    jsonObject.addProperty("status", "success");
                    jsonObject.addProperty("message", "Logged in successfully!");
                    String customerId = rs.getString("id");

                    request.getSession().setAttribute("user", username);
                    request.getSession().setAttribute("customerId", customerId);

                    jsonObject.addProperty("user", (new User(username,customerId )).toString());
                    response.setStatus(200);
                }
                else {
                    jsonObject.addProperty("status", "fail");
                    jsonObject.addProperty("message", "Invalid email or password!");
                    jsonObject.addProperty("input", username + " " + password);
                }

            } else {
                // User not found
                jsonObject.addProperty("status", "fail");
                jsonObject.addProperty("message", "Invalid email or password!");
                jsonObject.addProperty("input", username + " " + password);

                //response.setStatus(401);  // Unauthorized
            }

            rs.close();
            statement.close();


            jsonObject.addProperty("sessionId" , request.getSession().getId());
            response.getWriter().write(jsonObject.toString());

        } catch (Exception e) {

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            response.getWriter().write(jsonObject.toString());;

            request.getServletContext().log("Error:", e);
            response.setStatus(500);

        } finally {
            response.getWriter();
        }
    }
}
