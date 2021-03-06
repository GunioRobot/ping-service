package dmitrygusev.ping.pages.report;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.tapestry5.annotations.BeforeRenderTemplate;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.ping.services.dao.AccountDAO;
import dmitrygusev.ping.services.dao.JobDAO;
import dmitrygusev.ping.services.dao.ScheduleDAO;
import dmitrygusev.tapestry5.AbstractReadonlyPropertyConduit;

public class JobsReport {

    @Inject
    private ScheduleDAO scheduleDAO;
    @Property
    private Job job;
    @Inject
    private AccountDAO accountDAO;
    @Inject
    private JobDAO jobDAO;
    
    public List<Job> getUnbindedJobs() {
        List<Job> unbindedJobs = jobDAO.getAll();
        unbindedJobs.removeAll(getJobs());
        return unbindedJobs;
    }
    
    private List<Job> jobs;
    
    public List<Job> getJobs() {
        if (jobs == null) {
            List<Schedule> schedules = scheduleDAO.getAll();
            jobs = new ArrayList<Job>();
            for (Schedule schedule : schedules) {
                List<Job> scheduleJobs = schedule.getJobs();
                for (Job job : scheduleJobs) {
                    job.setSchedule(schedule);
                }
                jobs.addAll(scheduleJobs);
            }
        }
        return jobs;
    }
    
    private int counter = 0;
    
    @Component(id="grid")
    private Grid grid;
    
    @BeforeRenderTemplate
    void beforeRender() {
        if (grid.getSortModel().getSortConstraints().isEmpty()) {
            //  ascending
            grid.getSortModel().updateSort("createdAt");
            //  descending
            grid.getSortModel().updateSort("createdAt");
        }
    }
    
    @Inject private BeanModelSource beanModelSource;
    @Inject private Messages messages;
    
    public BeanModel<?> getModel() {
        BeanModel<?> beanModel = beanModelSource.createDisplayModel(Job.class, messages);

        beanModel.exclude("createdAt");
        beanModel.exclude("lastBackupTimestamp");
        beanModel.exclude("cronString");
        beanModel.exclude("recentAvailabilityPercent", "totalAvailabilityPercent");

        beanModel.add("recentAvailabilityPercent", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ((Job) instance).getRecentAvailabilityPercentFriendly().replace("%", "");
            }
        });
        beanModel.add("totalAvailabilityPercent", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ((Job) instance).getTotalAvailabilityPercentFriendly().replace("%", "");
            }
        });
        beanModel.add("cronString", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ((Job) instance).getCronString()
                            .replace("every ", "")
                            .replace(" hours", "h")
                            .replace(" minutes", "m");
            }
        });
        beanModel.add("lastBackupTimestamp", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                Date date = ((Job) instance).getLastBackupTimestamp();
                return date == null ? null : Application.DATETIME_FORMAT.format(date);
            }
        });
        beanModel.add("createdAt", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                Date date = ((Job) instance).getCreatedAt();
                return date == null ? null : Application.DATETIME_FORMAT.format(date);
            }
        });
        beanModel.add("userLastVisit", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                Date date = accountDAO.getAccount(((Job) instance).getSchedule().getName()).getLastVisitDate();
                return date == null ? null : Application.DATETIME_FORMAT.format(date);
            }
        });
        beanModel.add("SN", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ++counter;
            }
        }).sortable(false);

        beanModel.add("analytics", null);
        beanModel.add("details", null);
        
        beanModel.exclude(
                "lastPingDetails",
                "lastPingFailed",
                "lastPingResult",
                "usesValidatingRegexp",
                "usesValidatingHttpCode",
                "validatingRegexp",
                "validatingHttpCode",
                "responseEncoding",
                "shortenUrl",
                "title",
                "pingURL",
                "statusCounter",
                "statusCounterFriendlyShort",
                "upDownTimeInMinutes",
                "previousStatusCounter",
                "totalStatusCounter",
                "totalSuccessStatusCounter",
                "totalAvailabilityPercentFriendly",
                "recentAvailabilityPercentFriendly",
                "lastPingTimestamp",
                "validationSummary",
                "reportEmail",
                "statusCounterFriendly",
                "previousStatusCounterFriendly",
                "googleIOException",
                "totalStatusCounterFriendly",
                "totalSuccessStatusCounterFriendly");

        beanModel.reorder(
                        "SN", 
                        "cronString",
                        "titleFriendly", 
                        "lastPingSummary",
                        "recentAvailabilityPercent",
                        "totalAvailabilityPercent",
                        "scheduledBy",
                        "userLastVisit",
                        "createdAt",
                        "resultsCount");
        
        return beanModel;
    }
    
    public Long[] getJobContext() {
        return Utils.createJobContext(job);
    }
    
    public String getSummaryStatusCssClass() {
        if (job.isGoogleIOException()) {
            return "status-warning";
        }
        if (job.isLastPingFailed()) {
            return "status-error";
        }
        if (job.getTotalStatusCounter() == 0) {
            return "status-na";
        }
        return "status-okay";
    }

    public String getLastPingTimestamp() {
        return Utils.formatTime(job.getLastPingTimestamp());
    }

    public String getLastBackupTimestampFriendly() {
        return Utils.getTimeAgoUpToDays(job.getLastBackupTimestamp());
    }
    
    public String getCreatedAtFriendly() {
        return Utils.getTimeAgoUpToDays(job.getCreatedAt());
    }
    
    public String getUserLastVisitFriendly() {
        Schedule schedule = job.getSchedule();
        if (schedule == null) {
            return null;
        }
        return Utils.getTimeAgoUpToDays(accountDAO.getAccount(schedule.getName()).getLastVisitDate());
    }

    public String getLastBackupTimestamp() {
        return Utils.formatTime(job.getLastBackupTimestamp());
    }
    
    public String getCreatedAt() {
        return Utils.formatTime(job.getCreatedAt());
    }
    
    public String getUserLastVisit() {
        Schedule schedule = job.getSchedule();
        if (schedule == null) {
            return null;
        }
        return Utils.formatTime(accountDAO.getAccount(schedule.getName()).getLastVisitDate());
    }
    
    public void onActionFromDeleteJob(Long scheduleId, Long jobId) {
        jobDAO.delete(scheduleId, jobId);
    }
}
