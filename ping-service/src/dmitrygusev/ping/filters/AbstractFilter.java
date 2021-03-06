package dmitrygusev.ping.filters;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.cache.Cache;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.internal.services.BaseURLSourceImpl;
import org.apache.tapestry5.internal.services.DefaultSessionPersistedObjectAnalyzer;
import org.apache.tapestry5.internal.services.LinkImpl;
import org.apache.tapestry5.internal.services.LinkSecurity;
import org.apache.tapestry5.internal.services.RequestGlobalsImpl;
import org.apache.tapestry5.internal.services.RequestImpl;
import org.apache.tapestry5.internal.services.ResponseImpl;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.services.AppModule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.JobExecutor;
import dmitrygusev.ping.services.LocalMemorySoftCache;
import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.impl.cache.JobDAOImplCache;

public abstract class AbstractFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFilter.class);

    protected EntityManagerFactory emf;

    protected Application application;
    
    protected JobDAO jobDAO;

    protected final RequestGlobals globals = new RequestGlobalsImpl();

    protected Cache cache; 
    
    public AbstractFilter() {
        super();
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @SuppressWarnings("rawtypes")
    protected void lazyInit() {
        //  One instance might already be created by tapestry-jpa
        System.setProperty("appengine.orm.disable.duplicate.emf.exception", "");
        
        emf = Persistence.createEntityManagerFactory("transactions-optional");
        
        cache = AppModule.buildCache(logger, null);
        
        jobDAO = new JobDAOImplCache(cache);
        
        application = new Application(null, 
                                      jobDAO, 
                                      null, 
                                      null, 
                                      null, 
                                      new JobExecutor(), 
                                      new Mailer(), 
                                      new PageRenderLinkSource()
                                      {
                                          @Override public Link createPageRenderLinkWithContext(Class pageClass, EventContext eventContext) { return null; }
                                          @Override public Link createPageRenderLinkWithContext(String pageName, EventContext eventContext) { return null; }
                                          @Override public Link createPageRenderLinkWithContext(String pageName, Object... context) { return null; }
                                          @Override public Link createPageRenderLink(String pageName) { return null; }
                                          @Override public Link createPageRenderLink(Class pageClass) 
                                          {
                                              String pageAddress = pageClass.getName().substring("dmitrygusev.ping.pages".length()).replaceAll("\\.", "/");
                                              
                                              return new LinkImpl(pageAddress, 
                                                      false, 
                                                      LinkSecurity.INSECURE, 
                                                      new ResponseImpl(globals.getHTTPServletRequest(), globals.getHTTPServletResponse()), 
                                                      null,
                                                      new BaseURLSourceImpl(globals.getRequest()));
                                          }
                                          @Override public Link createPageRenderLinkWithContext(Class pageClass, Object... context)
                                          {
                                              StringBuilder pageAddress = new StringBuilder(pageClass.getName().substring("dmitrygusev.ping.pages".length()).replaceAll("\\.", "/"));
                                              
                                              for (Object object : context) {
                                                  pageAddress.append("/");
                                                  pageAddress.append(object);
                                              }
                                              
                                              return new LinkImpl(pageAddress.toString(), 
                                                                  false, 
                                                                  LinkSecurity.INSECURE, 
                                                                  new ResponseImpl(globals.getHTTPServletRequest(), globals.getHTTPServletResponse()), 
                                                                  null,
                                                                  new BaseURLSourceImpl(globals.getRequest()));
                                          }
                                      }, 
                                      globals,
                                      null,
                                      null);
    }

    public void setApplication(Application application) {
        this.application = application;
    }
    
    @Override
    public void destroy() {
        
    }

    protected abstract void processRequest() throws Exception;

    @Override
    public synchronized void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        long startTime = System.currentTimeMillis();
        
        if (emf == null) {
            lazyInit();
        }
        
        EntityManager em = emf.createEntityManager();
        
        EntityTransaction tx = null;
        
        try {
            
            tx = em.getTransaction();
            
            tx.begin();
            
            jobDAO.setEntityManager(em);
            
            HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            HttpServletResponse httpServletResponse = (HttpServletResponse)response;
            globals.storeServletRequestResponse(httpServletRequest, httpServletResponse);
            globals.storeRequestResponse(new RequestImpl(httpServletRequest, 
                                                         httpServletRequest.getCharacterEncoding(), 
                                                         new DefaultSessionPersistedObjectAnalyzer()), 
                                         new ResponseImpl(httpServletRequest, httpServletResponse));
            
            processRequest();
            
            if (tx.isActive()) {
                tx.commit();
            }
        }
        catch (Exception e)
        {
            logger.error("Error processing request", e);
        }
        finally
        {
            if (cache instanceof LocalMemorySoftCache) {
                ((LocalMemorySoftCache) cache).reset();
            }
            
            if (tx != null && tx.isActive())
            {
                tx.rollback();
            }
            
            em.close();
        }
        
        long endTime = System.currentTimeMillis();
        
        logger.debug("Total time: " + (endTime - startTime));
    }

}