package io.github.valossa515.pricetracker.consent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.consent")
public class ConsentDocumentsProperties {

    private Map<ConsentDocumentType, DocumentSpec> documents =
            new EnumMap<>(ConsentDocumentType.class);

    public Map<ConsentDocumentType, DocumentSpec> getDocuments() {
        return documents;
    }

    public void setDocuments(Map<ConsentDocumentType, DocumentSpec> documents) {
        this.documents = documents;
    }

    public static class DocumentSpec {
        private String version;
        private String title;
        private String url;

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
