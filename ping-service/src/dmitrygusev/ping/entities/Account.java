package dmitrygusev.ping.entities;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class Account implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = -3714340195981301403L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Transient
    private Ref ref;
    
    @Column(nullable=false)
    private String email;
    private String timeZoneCity;
    
    // Since 13.05.2010
    private Date lastVisitDate;
    private Date creationDate;

    public Account() {
    }
    public String getEmail() {
        return email == null ? null : email.toLowerCase();
    }
    public void setEmail(String email) {
        this.email = email == null ? null : email.toLowerCase();
    }
    public Long getId() {
        return id;
    }
    public void setRef(Ref ref) {
        this.ref = ref;
    }
    public Ref getRef() {
        return ref;
    }
    public String getRefAccessType() {
        return ref.getAccessTypeFriendly();
    }
    public void setTimeZoneCity(String timeZoneCity) {
        this.timeZoneCity = timeZoneCity;
    }
    public String getTimeZoneCity() {
        return timeZoneCity;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Date getLastVisitDate() {
        return lastVisitDate;
    }
    public void setLastVisitDate(Date lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }
}
