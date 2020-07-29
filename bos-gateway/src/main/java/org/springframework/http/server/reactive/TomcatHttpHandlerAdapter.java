package org.springframework.http.server.reactive;

import org.apache.catalina.connector.*;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * <p></p >
 *
 * @author zxl
 * @email xiaoliang.zhang@payby.com
 * @date TomcatHttpHandlerAdapter.java v1.0  2020/7/29 1:59 PM
 */
public class TomcatHttpHandlerAdapter extends ServletHttpHandlerAdapter {
    public TomcatHttpHandlerAdapter(HttpHandler httpHandler) {
        super(httpHandler);
    }

    @Override
    protected ServletServerHttpRequest createRequest(HttpServletRequest request, AsyncContext asyncContext) throws IOException, URISyntaxException {
        Assert.notNull(this.getServletPath(), "Servlet path is not initialized");
        return new TomcatHttpHandlerAdapter.TomcatServerHttpRequest(request, asyncContext, this.getServletPath(), this.getDataBufferFactory(), this.getBufferSize());
    }

    @Override
    protected ServletServerHttpResponse createResponse(HttpServletResponse response, AsyncContext asyncContext, ServletServerHttpRequest request) throws IOException {
        return new TomcatHttpHandlerAdapter.TomcatServerHttpResponse(response, asyncContext, this.getDataBufferFactory(), this.getBufferSize(), request);
    }

    private static final class TomcatServerHttpResponse extends ServletServerHttpResponse {
        private static final Field COYOTE_RESPONSE_FIELD;

        TomcatServerHttpResponse(HttpServletResponse response, AsyncContext context, DataBufferFactory factory, int bufferSize, ServletServerHttpRequest request) throws IOException {
            super(createTomcatHttpHeaders(response), response, context, factory, bufferSize, request);
        }

        private static HttpHeaders createTomcatHttpHeaders(HttpServletResponse response) {
            ResponseFacade responseFacade = getResponseFacade(response);
            Response connectorResponse = (Response)ReflectionUtils.getField(COYOTE_RESPONSE_FIELD, responseFacade);
            Assert.state(connectorResponse != null, "No Tomcat connector response");
            org.apache.coyote.Response tomcatResponse = connectorResponse.getCoyoteResponse();
            TomcatHeadersAdapter headers = new TomcatHeadersAdapter(tomcatResponse.getMimeHeaders());
            return new HttpHeaders(headers);
        }

        private static ResponseFacade getResponseFacade(HttpServletResponse response) {
            if (response instanceof ResponseFacade) {
                return (ResponseFacade)response;
            } else if (response instanceof HttpServletResponseWrapper) {
                HttpServletResponseWrapper wrapper = (HttpServletResponseWrapper)response;
                HttpServletResponse wrappedResponse = (HttpServletResponse)wrapper.getResponse();
                return getResponseFacade(wrappedResponse);
            } else {
                throw new IllegalArgumentException("Cannot convert [" + response.getClass() + "] to org.apache.catalina.connector.ResponseFacade");
            }
        }

        @Override
        protected void applyHeaders() {
            HttpServletResponse response = (HttpServletResponse)this.getNativeResponse();
            MediaType contentType = null;

            try {
                contentType = this.getHeaders().getContentType();
            } catch (Exception var6) {
                String rawContentType = this.getHeaders().getFirst("Content-Type");
                response.setContentType(rawContentType);
            }

            if (response.getContentType() == null && contentType != null) {
                response.setContentType(contentType.toString());
            }

            this.getHeaders().remove("Content-Type");
            Charset charset = contentType != null ? contentType.getCharset() : null;
            if (response.getCharacterEncoding() == null && charset != null) {
                response.setCharacterEncoding(charset.name());
            }

            long contentLength = this.getHeaders().getContentLength();
            if (contentLength != -1L) {
                response.setContentLengthLong(contentLength);
            }

            this.getHeaders().remove("Content-Length");
        }

        @Override
        protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
            ByteBuffer input = dataBuffer.asByteBuffer();
            int len = input.remaining();
            ServletResponse response = (ServletResponse)this.getNativeResponse();
            ((CoyoteOutputStream)response.getOutputStream()).write(input);
            return len;
        }

        static {
            Field field = ReflectionUtils.findField(ResponseFacade.class, "response");
            Assert.state(field != null, "Incompatible Tomcat implementation");
            ReflectionUtils.makeAccessible(field);
            COYOTE_RESPONSE_FIELD = field;
        }
    }

    public static final class TomcatServerHttpRequest extends ServletServerHttpRequest {
        private static final Field COYOTE_REQUEST_FIELD;
        private final int bufferSize;
        private final DataBufferFactory factory;
        private HttpServletRequest request;

        TomcatServerHttpRequest(HttpServletRequest request, AsyncContext context, String servletPath, DataBufferFactory factory, int bufferSize) throws IOException, URISyntaxException {
            super(createTomcatHttpHeaders(request), request, context, servletPath, factory, bufferSize);
            this.factory = factory;
            this.bufferSize = bufferSize;
            this.request = request;
        }



        private static HttpHeaders createTomcatHttpHeaders(HttpServletRequest request) {
            RequestFacade requestFacade = getRequestFacade(request);
            Request connectorRequest = (Request)ReflectionUtils.getField(COYOTE_REQUEST_FIELD, requestFacade);
            Assert.state(connectorRequest != null, "No Tomcat connector request");
            org.apache.coyote.Request tomcatRequest = connectorRequest.getCoyoteRequest();
            TomcatHeadersAdapter headers = new TomcatHeadersAdapter(tomcatRequest.getMimeHeaders());
            return new HttpHeaders(headers);
        }

        private static RequestFacade getRequestFacade(HttpServletRequest request) {
            if (request instanceof RequestFacade) {
                return (RequestFacade)request;
            } else if (request instanceof HttpServletRequestWrapper) {
                HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper)request;
                HttpServletRequest wrappedRequest = (HttpServletRequest)wrapper.getRequest();
                return getRequestFacade(wrappedRequest);
            } else {
                throw new IllegalArgumentException("Cannot convert [" + request.getClass() + "] to org.apache.catalina.connector.RequestFacade");
            }
        }

        @Override
        protected DataBuffer readFromInputStream() throws IOException {
            ServletInputStream inputStream = ((ServletRequest)this.getNativeRequest()).getInputStream();
            if (!(inputStream instanceof CoyoteInputStream)) {
                return super.readFromInputStream();
            } else {
                boolean release = true;
                int capacity = this.bufferSize;
                DataBuffer dataBuffer = this.factory.allocateBuffer(capacity);

                DataBuffer var7;
                try {
                    ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, capacity);
                    int read = ((CoyoteInputStream)inputStream).read(byteBuffer);
                    this.logBytesRead(read);
                    if (read <= 0) {
                        if (read == -1) {
                            var7 = EOF_BUFFER;
                            return var7;
                        }

                        var7 = null;
                        return var7;
                    }

                    dataBuffer.writePosition(read);
                    release = false;
                    var7 = dataBuffer;
                } finally {
                    if (release) {
                        DataBufferUtils.release(dataBuffer);
                    }

                }

                return var7;
            }
        }

        static {
            Field field = ReflectionUtils.findField(RequestFacade.class, "request");
            Assert.state(field != null, "Incompatible Tomcat implementation");
            ReflectionUtils.makeAccessible(field);
            COYOTE_REQUEST_FIELD = field;
        }

        public HttpServletRequest getRequest() {
            return request;
        }
    }
}