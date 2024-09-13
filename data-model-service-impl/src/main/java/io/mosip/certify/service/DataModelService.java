package io.mosip.certify.service;

import java.util.List;
import java.util.Map;

public interface DataModelService {
    Map<String, Object> fetchDataFromDataProvider(List<String> credentialType, List<String> context) throws Exception;
}
