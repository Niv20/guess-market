package guessmarket.server;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Creates fresh in-memory state for each deployment; Tomcat never serializes this service. */
@WebListener
public final class ApplicationListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute(ApplicationService.CONTEXT_ATTRIBUTE, new ApplicationService());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(ApplicationService.CONTEXT_ATTRIBUTE);
    }
}
