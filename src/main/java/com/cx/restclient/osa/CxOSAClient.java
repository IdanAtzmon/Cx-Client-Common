package com.cx.restclient.osa;

import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.Status;
import com.cx.restclient.common.Waiter;
import com.cx.restclient.httpClient.CxHttpClient;
import com.cx.restclient.httpClient.exception.CxClientException;
import com.cx.restclient.httpClient.exception.CxTokenExpiredException;
import com.cx.restclient.osa.dto.*;
import com.cx.restclient.osa.exception.CxOSAException;
import com.cx.restclient.osa.utils.OSAUtils;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.whitesource.fs.ComponentScan;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static com.cx.restclient.httpClient.utils.ClientUtils.convertToJson;
import static com.cx.restclient.httpClient.utils.PARAM.CONTENT_TYPE_APPLICATION_JSON_V1;
import static com.cx.restclient.osa.utils.CxOSAParam.*;

/**
 * Created by Galn on 05/02/2018.
 */
public class CxOSAClient /**implements ICxOSAClient **/{

    private CxHttpClient httpClient;
    private Logger log;
    private Integer projectId;
    private CxScanConfig config;
    private OSAResults osaResults = new OSAResults();
    private Waiter<OSAScanStatus> osaWaiter = new Waiter<OSAScanStatus>("CxOSA", 20000) {
        @Override
        public OSAScanStatus getStatus(String id) throws CxClientException, IOException, CxTokenExpiredException {
            return getOSAScanStatus(id);
        }

        @Override
        public void printProgress(OSAScanStatus scanStatus) {
            printOSAProgress(scanStatus, getStartTime());
        }

        @Override
        public OSAScanStatus resolveStatus(OSAScanStatus scanStatus) throws CxClientException {
            return resolveOSAStatus(scanStatus);
        }
    };

    public CxOSAClient(CxHttpClient client, Logger log, CxScanConfig config, Integer projectId) {
        this.log = log;
        this.httpClient = client;
        this.config = config;
        this.projectId = projectId;
    }

    //API
    public String createOSAScan() throws IOException, InterruptedException, CxClientException, CxTokenExpiredException {
        log.info("Creating OSA scan");
        String osaDependenciesJson = config.getOsaDependenciesJson();
        if (osaDependenciesJson == null) {
            osaDependenciesJson = resolveOSADependencies();
        }
        return sendOSAScan(osaDependenciesJson);
    }

    private String resolveOSADependencies() {
        log.info("Scanning for CxOSA compatible files");
        Properties scannerProperties = OSAUtils.generateOSAScanConfiguration(config.getOsaFilterPattern(),
                config.getOsaArchiveIncludePatterns(), config.getSourceDir(), config.getOsaRunInstall());
        ComponentScan componentScan = new ComponentScan(scannerProperties);
        String osaDependenciesJson = componentScan.scan();
        OSAUtils.writeToOsaListToTemp(osaDependenciesJson, log);

        return osaDependenciesJson;
    }

    public OSAResults getOSAResults(String scanId) throws CxClientException, IOException, CxOSAException, InterruptedException, CxTokenExpiredException {

            log.info("Waiting for OSA scan to finish");
            OSAScanStatus osaScanStatus = osaWaiter.waitForTaskToFinish(scanId, -1, log);
            OSAScanStatus osaScanStatus = osaWaiter.waitToFinish(scanId, -1, log);
            log.info("OSA scan finished successfully");
            log.info("Creating OSA reports");
            OSASummaryResults osaSummaryResults = getOSAScanSummaryResults(scanId);
          /*  OSASummaryResults osaSummaryResults = new OSASummaryResults();
            osaSummaryResults.setTotalLibraries(15);
            osaSummaryResults.setHighVulnerabilityLibraries(10);
            osaSummaryResults.setMediumVulnerabilityLibraries(9);
            osaSummaryResults.setLowVulnerabilityLibraries(8);
            osaSummaryResults.setNonVulnerableLibraries(7);
            osaSummaryResults.setVulnerableAndUpdated(12);
            osaSummaryResults.setVulnerableAndOutdated(13);
            osaSummaryResults.setVulnerabilityScore("52");
            osaSummaryResults.setTotalHighVulnerabilities(21);
            osaSummaryResults.setTotalMediumVulnerabilities(22);
            osaSummaryResults.setTotalLowVulnerabilities(23);*/

            List<Library> osaLibraries = getOSALibraries(scanId);
            List<CVE> osaVulnerabilities = getOSAVulnerabilities(scanId);
            osaResults.setResults(osaSummaryResults, osaLibraries, osaVulnerabilities, osaScanStatus);

            OSAUtils.printOSAResultsToConsole(osaResults, log);


        return osaResults;
    }

    //Private Methods
    private String sendOSAScan(String osaDependenciesJson) throws CxClientException, IOException, CxTokenExpiredException {
        log.info("Sending OSA scan request");
        long projectId = config.getProject().getId();
        CreateOSAScanResponse osaScan = scanOSA(projectId, osaDependenciesJson);
        String osaProjectSummaryLink = OSAUtils.composeProjectOSASummaryLink(config.getUrl(), projectId);
        osaResults.setOsaProjectSummaryLink(osaProjectSummaryLink);
        log.info("OSA scan created successfully. Link to project state: " + osaProjectSummaryLink);

        return osaScan.getScanId();
    }

    private CreateOSAScanResponse sendOSARequest(long projectId, String osaDependenciesJson) throws IOException, CxClientException, CxTokenExpiredException {
        CreateOSAScanRequest req = new CreateOSAScanRequest(projectId, osaDependenciesJson);
        StringEntity entity = new StringEntity(convertToJson(req));
        return httpClient.postRequest(OSA_SCAN_PROJECT, CONTENT_TYPE_APPLICATION_JSON_V1, entity, CreateOSAScanResponse.class, 201, "create OSA scan");
    }

    private OSASummaryResults getOSAScanSummaryResults(String scanId) throws CxClientException, IOException, CxTokenExpiredException {
        String relativePath = OSA_SCAN_SUMMARY + SCAN_ID_QUERY_PARAM + scanId;
        return httpClient.getRequest(relativePath, CONTENT_TYPE_APPLICATION_JSON_V1, OSASummaryResults.class, 200, "OSA scan summary results", false);
    }

    private List<Library> getOSALibraries(String scanId) throws CxClientException, IOException, CxTokenExpiredException {
        String relPath = OSA_SCAN_LIBRARIES + SCAN_ID_QUERY_PARAM + scanId + ITEM_PER_PAGE_QUERY_PARAM + MAX_ITEMS;
        return (List<Library>) httpClient.getRequest(relPath, CONTENT_TYPE_APPLICATION_JSON_V1, Library.class, 200, "OSA libraries", true);
    }

    private List<CVE> getOSAVulnerabilities(String scanId) throws CxClientException, IOException, CxTokenExpiredException {
        String relPath = OSA_SCAN_VULNERABILITIES + SCAN_ID_QUERY_PARAM + scanId + ITEM_PER_PAGE_QUERY_PARAM + MAX_ITEMS;
        return (List<CVE>) httpClient.getRequest(relPath, CONTENT_TYPE_APPLICATION_JSON_V1, CVE.class, 200, "OSA vulnerabilities", true);
    }

    //Waiter - overload methods
    private OSAScanStatus getOSAScanStatus(String scanId) throws CxClientException, IOException, CxTokenExpiredException {
        String relPath = OSA_SCAN_STATUS.replace("{scanId}", scanId);
        OSAScanStatus scanStatus = httpClient.getRequest(relPath, CONTENT_TYPE_APPLICATION_JSON_V1, OSAScanStatus.class, 200, "OSA scan status", false);
        int stateId = scanStatus.getState().getId();

        if (OSAScanStatusEnum.SUCCEEDED.getNum() == stateId) {
            scanStatus.setBaseStatus(Status.SUCCEEDED);
        } else if (OSAScanStatusEnum.IN_PROGRESS.getNum() == stateId || OSAScanStatusEnum.NOT_STARTED.getNum() == stateId) {
            scanStatus.setBaseStatus(Status.IN_PROGRESS);
        } else {
            scanStatus.setBaseStatus(Status.FAILED);
        }
        return scanStatus;
    }

    private void printOSAProgress(OSAScanStatus scanStatus, long startTime) {
        long hours = (System.currentTimeMillis() - startTime) / 3600000;
        long minutes = ((System.currentTimeMillis() - startTime) % 3600000) / 60000;
        long seconds = ((System.currentTimeMillis() - startTime) % 60000) / 1000;
        String hoursStr = (hours < 10) ? ("0" + Long.toString(hours)) : (Long.toString(hours));
        String minutesStr = (minutes < 10) ? ("0" + Long.toString(minutes)) : (Long.toString(minutes));
        String secondsStr = (seconds < 10) ? ("0" + Long.toString(seconds)) : (Long.toString(seconds));
        log.info("Waiting for OSA scan results. Elapsed time: " + hoursStr + ":" + minutesStr + ":" + secondsStr + ". " +
                "Status: " + scanStatus.getState().getName());
    }

    private OSAScanStatus resolveOSAStatus(OSAScanStatus scanStatus) throws CxClientException {
        if (Status.FAILED == scanStatus.getBaseStatus()) {
            String failedMsg = scanStatus.getState() == null ? "" : "status [" + scanStatus.getState().getName() + "]. Reason: " + scanStatus.getState().getFailureReason();
            throw new CxClientException("OSA scan cannot be completed. " + failedMsg);
        }
        if (Status.SUCCEEDED == scanStatus.getBaseStatus()) {
            log.info("OSA scan finished.");
            return scanStatus;
        }
        return scanStatus;
    }
}

