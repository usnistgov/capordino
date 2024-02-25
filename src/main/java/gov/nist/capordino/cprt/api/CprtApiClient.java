package gov.nist.capordino.cprt.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nist.capordino.cprt.pojo.CprtExportResponse;
import gov.nist.capordino.cprt.pojo.CprtMetadataResponse;

/**
 * Interact with the CPRT API
 */
public class CprtApiClient {
    protected final String baseUrl = "https://csrc.nist.gov/extensions/nudp/services/json/nudp";

    final HttpClient client = HttpClient.newHttpClient();
    final ObjectMapper objectMapper = new ObjectMapper();

    public CprtApiClient() {
        objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
        objectMapper.configure(Feature.AUTO_CLOSE_SOURCE, true);
    }

    public CprtMetadataResponse getMetadata() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/metadata"))
          .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), CprtMetadataResponse.class);
    }

    public CprtExportResponse exportCprt(String frameworkVersionIdentifier) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/framework/version/" + frameworkVersionIdentifier + "/export/json?element=all"))
          .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), CprtExportResponse.class);
    }
}
