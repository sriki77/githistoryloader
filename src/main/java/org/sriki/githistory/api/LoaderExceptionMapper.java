package org.sriki.githistory.api;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;

@Provider
public class LoaderExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderExceptionMapper.class);
    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.debug("Converting Exception to response",exception);
        if (exception instanceof NotFoundException) {
            return handle404(exception);
        }
        return handle500(exception);

    }

    public Response handle500(Throwable exception) {
        final StringWriter msg = new StringWriter();
        final PrintWriter out = new PrintWriter(msg);
        out.println("Request Processing Failed. Reason: " + exception.getMessage());
        writeStackTrace(exception, out);
        out.close();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(msg.toString()).type("text/plain").build();
    }

    public Response handle404(Throwable exception) {
        final StringWriter msg = new StringWriter();
        final PrintWriter out = new PrintWriter(msg);
        out.println("Request Resource Not found. Reason: " + exception.getMessage());
        writeStackTrace(exception, out);
        out.close();
        return Response.status(Response.Status.NOT_FOUND)
                .entity(msg.toString()).type("text/plain").build();
    }

    public void writeStackTrace(Throwable exception, PrintWriter out) {
        out.println("--------------  STACK TRACE ------------- ");
        exception.printStackTrace(out);
    }
}
