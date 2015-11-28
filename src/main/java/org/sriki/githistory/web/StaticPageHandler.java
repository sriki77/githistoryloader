package org.sriki.githistory.web;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class StaticPageHandler extends ContextHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPageHandler.class);

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (target.equals("/")) {
            target = "/index.html";
        }
        InputStream targetResponse = this.getClass().getResourceAsStream("/dashboard" + target);
        if (targetResponse == null) {
            return404(response, target);
            return;
        }
        ServletOutputStream outputStream = response.getOutputStream();
        try {
            IOUtils.copy(targetResponse, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    private void return404(HttpServletResponse response, String target, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/plain");
        final PrintStream printStream = new PrintStream(response.getOutputStream());
        try {
            printStream.println("No response found for: " + target);
            printStream.println("Exception: ");
            e.printStackTrace(printStream);
        } finally {
            IOUtils.closeQuietly(printStream);
        }
    }

    private void return404(HttpServletResponse response, String target) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("text/plain");
        final ServletOutputStream outputStream = response.getOutputStream();
        try {
            IOUtils.write("No response found for: " + target, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
