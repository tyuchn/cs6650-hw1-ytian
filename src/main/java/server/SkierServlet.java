package server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@javax.servlet.annotation.WebServlet(name = "server.SkierServlet")
public class SkierServlet extends javax.servlet.http.HttpServlet {
    protected void doPost(javax.servlet.http.HttpServletRequest request, HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing paramterers");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)

        if (!isPostUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setStatus(HttpServletResponse.SC_CREATED);
            // do any sophisticated processing with urlParts which contains all the url params
            response.getWriter().write("Post " + urlPath);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing paramterers");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)

        if (!isGetUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            response.getWriter().write("Get " + urlPath);
        }
    }


    private boolean isGetUrlValid(String[] urlPath) {
        // validate the request url path according to the API spec
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
        if (urlPath == null) {
            return false;
        }
        if (urlPath.length == 8) {
            return isValidSkierLiftUrl(urlPath);
        }
        else if (urlPath.length == 3) {
            if (urlPath[1].equals("vertical")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isPostUrlValid(String[] urlPath) {
        if (urlPath == null) {
            return false;
        }
        if (urlPath.length == 8) {
            return isValidSkierLiftUrl(urlPath);
        } else {
            return false;
        }
    }

    private boolean isValidSkierLiftUrl(String[] urlPath) {
        if (urlPath[2].equals("seasons") && urlPath[4].equals("days") && urlPath[6].equals("skiers")) {
            return true;
        } else {
            return false;
        }
    }

}
