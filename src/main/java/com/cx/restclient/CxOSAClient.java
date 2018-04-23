package com.cx.restclient;

import com.cx.restclient.common.Waiter;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.Status;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.exception.CxOSAException;
import com.cx.restclient.exception.CxTokenExpiredException;
import com.cx.restclient.httpClient.CxHttpClient;
import com.cx.restclient.osa.dto.*;
import com.cx.restclient.osa.utils.OSAUtils;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.whitesource.fs.ComponentScan;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static com.cx.restclient.httpClient.utils.ContentType.CONTENT_TYPE_APPLICATION_JSON_V1;
import static com.cx.restclient.httpClient.utils.HttpClientHelper.convertToJson;
import static com.cx.restclient.osa.utils.OSAParam.*;
import static com.cx.restclient.osa.utils.OSAUtils.writeJsonToFile;

/**
 * Created by Galn on 05/02/2018.
 */
class CxOSAClient /**implements ICxOSAClient **/
{

    private CxHttpClient httpClient;
    private Logger log;
    private CxScanConfig config;
    private OSAResults osaResults = new OSAResults();
    private Waiter<OSAScanStatus> osaWaiter = new Waiter<OSAScanStatus>("CxOSA", 20) {
        @Override
        public OSAScanStatus getStatus(String id) throws CxClientException, IOException, CxTokenExpiredException {
            return getOSAScanStatus(id);
        }

        @Override
        public void printProgress(OSAScanStatus scanStatus) {
            printOSAProgress(scanStatus, getStartTimeSec());
        }

        @Override
        public OSAScanStatus resolveStatus(OSAScanStatus scanStatus) throws CxClientException {
            return resolveOSAStatus(scanStatus);
        }
    };

    public CxOSAClient(CxHttpClient client, Logger log, CxScanConfig config) {
        this.log = log;
        this.httpClient = client;
        this.config = config;
    }

    //API
    public String createOSAScan(long projectId) throws IOException, InterruptedException, CxClientException, CxTokenExpiredException {
        log.info("Creating OSA scan");
        String osaDependenciesJson = config.getOsaDependenciesJson();
        if (osaDependenciesJson == null) {
            osaDependenciesJson = resolveOSADependencies();
        }
        return sendOSAScan(osaDependenciesJson, projectId);
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
        log.info("-------------------------------------Get CxOSA Results:-----------------------------------");
        log.info("Waiting for OSA scan to finish");
        OSAScanStatus osaScanStatus = osaWaiter.waitForTaskToFinish(scanId, -1, log);
        log.info("OSA scan finished successfully. Retrieving OSA scan results");

        log.info("Creating OSA reports");
        retrieveOSAResults(osaResults, osaScanStatus);

        if (config.getReportsDir() != null) {
            writeJsonToFile(OSA_SUMMARY_NAME, osaResults.getResults(), config.getReportsDir(), log);
            writeJsonToFile(OSA_LIBRARIES_NAME, osaResults.getOsaLibraries(), config.getReportsDir(), log);
            writeJsonToFile(OSA_VULNERABILITIES_NAME, osaResults.getOsaVulnerabilities(), config.getReportsDir(), log);
        }

        OSAUtils.printOSAResultsToConsole(osaResults, log);
        return osaResults;
    }

    private OSAResults retrieveOSAResults(OSAResults results, OSAScanStatus osaScanStatus) throws CxClientException, IOException, CxTokenExpiredException {

        String scanId = results.getOsaScanId();
        OSASummaryResults osaSummaryResults = getOSAScanSummaryResults(scanId);
        List<Library> osaLibraries = getOSALibraries(scanId);
        List<CVE> osaVulnerabilities = getOSAVulnerabilities(scanId);

        results.setResults(osaSummaryResults, osaLibraries, osaVulnerabilities, osaScanStatus);
        return results;
    }

    public OSAResults getOSALastResults() throws CxClientException, IOException, CxOSAException, InterruptedException, CxTokenExpiredException {
        log.info("----------------------------------Get CxOSA Last Results:--------------------------------");
        OSAScanStatus osaScanStatus = getOSALastOSAStatus();
        String lastScanId = osaScanStatus.getBaseId();
        OSAResults lastResults = new OSAResults();
        lastResults.setOsaScanId(lastScanId);

        return retrieveOSAResults(lastResults, osaScanStatus);
    }

    //Private Methods
    private String sendOSAScan(String osaDependenciesJson, long projectId) throws CxClientException, IOException, CxTokenExpiredException {
        log.info("Sending OSA scan request");
        CreateOSAScanResponse osaScan = sendOSARequest(projectId, osaDependenciesJson);
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

    private OSAScanStatus getOSALastOSAStatus() throws CxClientException, IOException, CxTokenExpiredException {
        return httpClient.getRequest(OSA_LAST_SCAN, CONTENT_TYPE_APPLICATION_JSON_V1, OSAScanStatus.class, 200, " last OSA scan ID", false); //TODO
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
        long elapsedSec = System.currentTimeMillis() / 1000 - startTime;
        long hours = elapsedSec / 3600;
        long minutes = elapsedSec % 3600 / 60;
        long seconds = elapsedSec % 60;
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

