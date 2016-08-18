package io.bootique.jetty;

import com.google.inject.Binder;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import io.bootique.jetty.unit.JettyApp;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class JettyModule_MappedContributionsIT {

    @ClassRule
    public static JettyApp JETTY_FACTORY = new JettyApp();

    private static WebTarget BASE;

    @BeforeClass
    public static void startServer() {
        JETTY_FACTORY.startServer(new Module(), "--server");
        BASE = ClientBuilder.newClient().target("http://localhost:8080");
    }

    @Test
    public void testAnnotatedMapping() {

        Response r1 = BASE.path("/s1").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("f1_s1r", r1.readEntity(String.class));
    }

    @Test
    public void testByTypeMapping1() {

        Response r1 = BASE.path("/s2").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("f2_s2r", r1.readEntity(String.class));
    }

    @Test
    public void testByTypeMapping2() {

        Response r1 = BASE.path("/s3").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("f3_s3r", r1.readEntity(String.class));
    }


    @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @BindingAnnotation
    public static @interface A1 {

    }

    public static class Module implements com.google.inject.Module {

        @Override
        public void configure(Binder binder) {
            JettyModule.contributeMappedServlets(binder).addBinding().to(Key.get(MappedServlet.class, A1.class));

            TypeLiteral<MappedServlet<Servlet2>> s2 = new TypeLiteral<MappedServlet<Servlet2>>() {
            };
            JettyModule.contributeMappedServlets(binder).addBinding().to(Key.get(s2));

            TypeLiteral<MappedServlet<Servlet3>> s3 = new TypeLiteral<MappedServlet<Servlet3>>() {
            };
            JettyModule.contributeMappedServlets(binder).addBinding().to(Key.get(s3));

            JettyModule.contributeMappedFilters(binder).addBinding().to(Key.get(MappedFilter.class, A1.class));

            TypeLiteral<MappedFilter<Filter2>> f2 = new TypeLiteral<MappedFilter<Filter2>>() {
            };
            JettyModule.contributeMappedFilters(binder).addBinding().to(Key.get(f2));

            TypeLiteral<MappedFilter<Filter3>> f3 = new TypeLiteral<MappedFilter<Filter3>>() {
            };
            JettyModule.contributeMappedFilters(binder).addBinding().to(Key.get(f3));
        }


        @A1
        @Singleton
        @Provides
        MappedServlet provideMappedAnnotated(Servlet1 servlet) {
            return new MappedServlet<>(servlet, Collections.singleton("/s1"), "s1");
        }

        @Singleton
        @Provides
        MappedServlet<Servlet2> provideType1(Servlet2 servlet) {
            return new MappedServlet<>(servlet, Collections.singleton("/s2"), "s2");
        }

        @Singleton
        @Provides
        MappedServlet<Servlet3> provideType2(Servlet3 servlet) {
            return new MappedServlet<>(servlet, Collections.singleton("/s3"), "s3");
        }

        @A1
        @Singleton
        @Provides
        MappedFilter provideMappedAnnotated(Filter1 filter) {
            return new MappedFilter<>(filter, Collections.singleton("/s1/*"), "f1", 1);
        }

        @Singleton
        @Provides
        MappedFilter<Filter2> provideType1(Filter2 filter) {
            return new MappedFilter<>(filter, Collections.singleton("/s2/*"), "f2", 1);
        }

        @Singleton
        @Provides
        MappedFilter<Filter3> provideType2(Filter3 filter) {
            return new MappedFilter<>(filter, Collections.singleton("/s3/*"), "f3", 1);
        }
    }

    public static class Servlet1 extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            resp.getWriter().print("s1r");
        }
    }

    public static class Servlet2 extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            resp.getWriter().print("s2r");
        }
    }

    public static class Servlet3 extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            resp.getWriter().print("s3r");
        }
    }

    public static class Filter1 implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            response.getWriter().print("f1_");
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {

        }
    }

    public static class Filter2 implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            response.getWriter().print("f2_");
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {

        }
    }

    public static class Filter3 implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            response.getWriter().print("f3_");
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {

        }
    }
}