package dh.newspaper.model.generated;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table PATH_TO_CONTENT.
 */
public class PathToContent {

    private Long id;
    /** Not-null value. */
    private String urlPattern;
    private String xpath;
    private String language;
    private Boolean enable;
    private java.util.Date lastUpdate;

    public PathToContent() {
    }

    public PathToContent(Long id) {
        this.id = id;
    }

    public PathToContent(Long id, String urlPattern, String xpath, String language, Boolean enable, java.util.Date lastUpdate) {
        this.id = id;
        this.urlPattern = urlPattern;
        this.xpath = xpath;
        this.language = language;
        this.enable = enable;
        this.lastUpdate = lastUpdate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** Not-null value. */
    public String getUrlPattern() {
        return urlPattern;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public java.util.Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(java.util.Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

}