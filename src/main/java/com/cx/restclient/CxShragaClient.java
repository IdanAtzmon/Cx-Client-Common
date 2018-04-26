package com.cx.restclient;

import com.cx.restclient.common.summary.SummaryUtils;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.Team;
import com.cx.restclient.dto.ThresholdResult;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.exception.CxSASTException;
import com.cx.restclient.exception.CxTokenExpiredException;
import com.cx.restclient.httpClient.CxHttpClient;
import com.cx.restclient.osa.dto.OSAResults;
import com.cx.restclient.sast.dto.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import static com.cx.restclient.common.CxPARAM.*;
import static com.cx.restclient.common.ShragaUtils.isThresholdExceeded;
import static com.cx.restclient.httpClient.utils.ContentType.CONTENT_TYPE_APPLICATION_JSON_V1;
import static com.cx.restclient.httpClient.utils.HttpClientHelper.convertToJson;
import static com.cx.restclient.sast.utils.SASTParam.SAST_ENGINE_CONFIG;
import static com.cx.restclient.sast.utils.SASTParam.SAST_GET_PROJECT;
import static com.cx.restclient.sast.utils.SASTParam.SAST_GET_All_PROJECTS;

/**
 * Created by Galn on 05/02/2018.
 */
public class CxShragaClient /*implements ICxShragaClient*/ {
    private CxHttpClient httpClient;
    private Logger log;
    private CxScanConfig config;
    private long projectId;

    private CxSASTClient sastClient;
    private CxOSAClient osaClient;
    private long sastScanId;
    private String osaScanId;
    private SASTResults sastResults;
    private OSAResults osaResults;


    public CxShragaClient(CxScanConfig config, Logger log) throws MalformedURLException {
        this.config = config;
        this.log = log;
        this.httpClient = new CxHttpClient(config.getUrl(), config.getUsername(), config.getPassword(), config.getCxOrigin(), log);
        sastClient = new CxSASTClient(httpClient, log, config);
        osaClient = new CxOSAClient(httpClient, log, config);
    }

    public CxShragaClient(String serverUrl, String username, String password, String origin, Logger log) throws MalformedURLException {
        this(new CxScanConfig(serverUrl, username, password, origin),log);
    }

    //API Scans methods
    public void init() throws CxClientException, IOException, CxTokenExpiredException {
        log.info("Initializing Cx client");
        login();
        if (config.getSastEnabled()) {
            resolvePreset();
        }
        resolveTeam();
        resolveProject();
    }

    public long createSASTScan() throws CxSASTException, InterruptedException, IOException {
        sastScanId = sastClient.createSASTScan(projectId);
        return sastScanId;
    }

    public String createOSAScan() throws IOException, InterruptedException, CxClientException, CxTokenExpiredException {
        osaScanId = osaClient.createOSAScan(projectId);
        return osaScanId;
    }

    public void cancelSASTScan() throws IOException, CxClientException, CxTokenExpiredException {
        sastClient.cancelSASTScan(sastResults.getScanId());
    }

    public SASTResults waitForSASTResults() throws Exception {
        sastResults = sastClient.waitForSASTResults(sastScanId, projectId);
        return sastResults;
    }

    public SASTResults getLatestSASTResults() throws Exception {
        sastResults = sastClient.getLatestSASTResults(projectId);
        return sastResults;
    }

    public OSAResults waitForOSAResults() throws Exception {
        osaResults = osaClient.getOSAResults(osaScanId, projectId);
        return osaResults;
    }

    public OSAResults getLatestOSAResults() throws Exception {
        osaResults = osaClient.getLatestOSAResults(projectId);
        return osaResults;
    }

    public ThresholdResult getThresholdResult() {
        StringBuilder res = new StringBuilder("");
        boolean isFail = isThresholdExceeded(sastResults, osaResults, res, config);
        return new ThresholdResult(isFail, res.toString());
    }

    public String generateHTMLSummary() throws Exception {
        return SummaryUtils.generateSummary(sastResults, osaResults, config);
    }

    public String generateHTMLSummary(SASTResults sastResults, OSAResults osaResults) throws Exception {
        return SummaryUtils.generateSummary(sastResults, osaResults, config);
    }


    public void close() {
        httpClient.close();
    }

    //HELP config  Methods
    public void login() throws IOException, CxClientException, CxTokenExpiredException {
        // perform login to server
        log.info("Logging into the Checkmarx service.");
        //loginToServer();
        httpClient.login();
    }

    public String getTeamIdByName(String teamName) throws CxClientException, IOException, CxTokenExpiredException {
        List<Team> allTeams = getTeamList();
        for (Team team : allTeams) {
            if ((team.getFullName()).equalsIgnoreCase(teamName)) { //TODO caseSenesitive- checkkk and REMOVE The WA "\"
                return team.getId();
            }
        }
        throw new CxClientException("Could not resolve team ID from team name: " + teamName);
    }

    public int getPresetIdByName(String presetName) throws CxClientException, IOException, CxTokenExpiredException {
        List<Preset> allPresets = getPresetList();
        for (Preset preset : allPresets) {
            if (preset.getName().equalsIgnoreCase(presetName)) { //TODO caseSenesitive- checkkk
                return preset.getId();
            }
        }

        throw new CxClientException("Could not resolve preset ID from preset name: " + presetName);
    }

    public List<Team> getTeamList() throws IOException, CxClientException, CxTokenExpiredException {
        return (List<Team>) httpClient.getRequest(CXTEAMS, CONTENT_TYPE_APPLICATION_JSON_V1, Team.class, 200, "team list", true);
    }

    public List<Preset> getPresetList() throws IOException, CxClientException, CxTokenExpiredException {
        return (List<Preset>) httpClient.getRequest(CXPRESETS, CONTENT_TYPE_APPLICATION_JSON_V1, Preset.class, 200, "preset list", true);
    }

    public List<CxNameObj> getConfigurationSetList() throws IOException, CxClientException, CxTokenExpiredException {
        return (List<CxNameObj>) httpClient.getRequest(SAST_ENGINE_CONFIG, CONTENT_TYPE_APPLICATION_JSON_V1, CxNameObj.class, 200, "engine configurations", true);
    }

    //Private methods
    private void resolveTeam() throws CxClientException, IOException, CxTokenExpiredException {
        if (config.getTeamId() == null) {
            config.setTeamId(getTeamIdByName(config.getTeamPath()));
        }
    }

    private void resolvePreset() throws CxClientException, IOException, CxTokenExpiredException {
        if (config.getPresetId() == null) {
            config.setPresetId(getPresetIdByName(config.getPresetName()));
        }
    }

    private void resolveProject() throws IOException, CxClientException, CxTokenExpiredException {
        List<Project> projects = getProjectByName(config.getProjectName(), config.getTeamId());
        if (projects == null) { // Project is new
            if (config.getDenyProject()) {
                String errMsg = "Creation of the new project [" + config.getProjectName() + "] is not authorized. " +
                        "Please use an existing project. \nYou can enable the creation of new projects by disabling" + "" +
                        " the Deny new Checkmarx projects creation checkbox in the Checkmarx plugin global settings.\n";
                throw new CxClientException(errMsg);
            }
            //Create newProject
            CreateProjectRequest request = new CreateProjectRequest(config.getProjectName(), config.getTeamId(), config.getPublic());
            projectId = createNewProject(request).getId();

        } else {
            projectId = projects.get(0).getId();
        }
    }

    private List<Project> getProjectByName(String projectName, String teamId) throws IOException, CxClientException, CxTokenExpiredException {
        String projectNamePath = SAST_GET_PROJECT.replace("{name}", projectName).replace("{teamId}", teamId);
        List<Project> projects = null;
        try {
            projects = (List<Project>) httpClient.getRequest(projectNamePath, CONTENT_TYPE_APPLICATION_JSON_V1, Project.class, 200, "project by name: " + projectName, true);
        } catch (HttpResponseException ex) {
            if (ex.getStatusCode() != 404) {
                throw ex;
            }
        }
        return projects;
    }

    public List<Project> getAllProjects() throws IOException, CxClientException, CxTokenExpiredException {
        List<Project> projects = null;
        try {
            projects = (List<Project>) httpClient.getRequest(SAST_GET_All_PROJECTS, CONTENT_TYPE_APPLICATION_JSON_V1, Project.class, 200, "all projects", true);
        } catch (HttpResponseException ex) {
            if (ex.getStatusCode() != 404) {
                throw ex;
            }
        }
        return projects;
    }

    private Project createNewProject(CreateProjectRequest request) throws CxClientException, IOException, CxTokenExpiredException {
        String json = convertToJson(request);
        StringEntity entity = new StringEntity(json);
        return httpClient.postRequest(CREATE_PROJECT, CONTENT_TYPE_APPLICATION_JSON_V1, entity, Project.class, 201, "create new project: " + request.getName());
    }
 }

