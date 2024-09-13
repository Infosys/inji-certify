package io.mosip.certify.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.certify.api.dto.VCRequestDto;
import io.mosip.certify.api.exception.VCIExchangeException;
import io.mosip.certify.api.util.ErrorConstants;
import io.mosip.certify.entity.CredentialData;
import io.mosip.certify.repository.CredentialRepository;
import io.mosip.certify.service.DataModelService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.URLResourceLoader;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class DataModelServiceImpl implements DataModelService {
    private static final String CREDENTIAL_TYPE_PROPERTY_PREFIX ="mosip.certify.vciplugin.sunbird-rc.credential-type";

    private static final String LINKED_DATA_PROOF_VC_FORMAT ="ldp_vc";

    private static final String TEMPLATE_URL = "template-url";

    private static final String REGISTRY_GET_URL = "registry-get-url";

    private static final String REGISTRY_SEARCH_URL= "registry-search-url";

    private static final String CRED_SCHEMA_ID = "cred-schema-id";

    private static final String CRED_SCHEMA_VESRION = "cred-schema-version";

    private static final String STATIC_VALUE_MAP_ISSUER_ID = "static-value-map.issuerId";
    @Autowired
    CredentialRepository credentialRepository;
    @Autowired
    Environment env;

    @Autowired
    ObjectMapper mapper;

    private VelocityEngine velocityEngine;

    @Value("#{'${mosip.certify.vciplugin.sunbird-rc.supported-credential-types}'.split(',')}")
    List<String> supportedCredentialTypes;

    private final Map<String, Template> credentialTypeTemplates = new HashMap<>();
    private final Map<String,Map<String,String>> credentialTypeConfigMap = new HashMap<>();

    @PostConstruct
    public void initialize() throws VCIExchangeException {
        velocityEngine = new VelocityEngine();
        URLResourceLoader urlResourceLoader = new URLResourceLoader() {
            @Override
            public InputStream getResourceStream(String name) throws ResourceNotFoundException {
                try {
                    URL url = new URL(name);
                    URLConnection connection = url.openConnection();
                    return connection.getInputStream();
                } catch (IOException e) {
                    throw new ResourceNotFoundException("Unable to find resource '" + name + "'");
                }
            }
        };
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "url");
        velocityEngine.setProperty("url.resource.loader.instance", urlResourceLoader);
        velocityEngine.init();
        //Validate all the supported VC
    }

    @Override
    public Map<String, Object> fetchDataFromDataProvider(List<String> credentialType, List<String> contextUrl) throws Exception {

        Map<String, Object> cacheData = new HashMap<>();
        cacheData.put("credentialType", Arrays.asList("VerifiableCredential", "InsuranceCredential"));
        cacheData.put("context", "https://piyush7034.github.io/my-files/MembershipCredential.json");
        cacheData.put("validFrom", "01/01/2005");
        cacheData.put("validUntil", "01/01/2025");
        cacheData.put("id", "urn:uuid:3978344f-8596-4c3a-a978-8fcaba3903c5");
        cacheData.put("issuer", "did:kratos:123456789");
        cacheData.put("dob", "01/01/2000");
        cacheData.put("fullName", "abcdef");
        cacheData.put("gender", "Male");
        cacheData.put("mobile", "1234567890");
        cacheData.put("benefits", "1234567890");
        cacheData.put("email", "abcdef@example.com");
        cacheData.put("membershipName", "Premium");
        cacheData.put("membershipNumber", "12345");


        Map<String,Object> requestMap=new HashMap<>();
//        Template template = velocityEngine.getTemplate("schooltemplate.url");
        String templateString = getTemplate(credentialType, contextUrl);
        StringWriter writer = new StringWriter();
        VelocityContext velocityContext = new VelocityContext(cacheData);

        InputStream is = new ByteArrayInputStream(templateString.getBytes(StandardCharsets.UTF_8));
        velocityEngine.evaluate(velocityContext, writer, "templateMerge", new InputStreamReader(is));

        try{
            Map<String,Object> credentialObject = mapper.readValue(writer.toString(),Map.class);
            requestMap.put("credential", credentialObject);
        }catch (JsonProcessingException e){
            log.error("Error while parsing the template ",e);
            throw new VCIExchangeException(ErrorConstants.VCI_EXCHANGE_FAILED);
        }

        return requestMap;
    }

//    private VCRequestDto getVCRequest(List<String> contextUrl, List<String> credentialType, String format) {
//        VCRequestDto vcRequestDto = new VCRequestDto();
//        vcRequestDto.setContext(contextUrl);
//        vcRequestDto.setType(credentialType);
//        vcRequestDto.setFormat(format);
//    }

    private String getTemplate(List<String> credentialType, List<String> contextUrl) throws VCIExchangeException {
        Collections.sort(credentialType);
        Collections.sort(contextUrl);
        String credentialTypeString = String.join(",", credentialType);
        String contextUrlTypeString = String.join(",", contextUrl);
        Optional<CredentialData> optional =
                credentialRepository.findByCredentialTypeAndContext(credentialTypeString, contextUrlTypeString);

        CredentialData credentialData = optional.orElseThrow(() -> new VCIExchangeException("Credential not found."));
        try {
            Map<String, Object> cacheData = mapper.readValue(credentialData.getTemplate(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return credentialData.getTemplate();
    }
}
