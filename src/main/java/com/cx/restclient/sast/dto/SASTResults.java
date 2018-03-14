package com.cx.restclient.sast.dto;

import java.util.List;

import static com.cx.restclient.sast.utils.SASTParam.LINK_FORMAT;

/**
 * Created by Galn on 05/02/2018.
 */
public class SASTResults {

    private long scanId;//TODO

    private boolean sastResultsReady = false;
    private String highResults;
    private String mediumResults;
    private String lowResults;
    private String infoResults;

    private String sastScanLink;
    private String sastProjectLink;

    private String scanStart;
    private String scanTime;
    private String filesScanned;
    private String linesOfCodeScanned;
    private List<CxXMLResults.Query> queryList;

    private byte[] PDFReport;

    public void setScanDetailedReport(CxXMLResults reportObj) {
        this.scanStart = reportObj.getScanStart();
        this.scanTime = reportObj.getScanTime();
        this.linesOfCodeScanned = reportObj.getLinesOfCodeScanned();
        this.filesScanned = reportObj.getFilesScanned();
        this.queryList = reportObj.getQuery();
    }


    public void setResults(ProjectScannedData projectScannedData, String url) {
        setScanId(projectScannedData.getLastScanID());   //TODO!!!!! Need it??
        setHighResults(Integer.toString(projectScannedData.getHighVulnerabilities())); //TODO what if null??
        setMediumResults(Integer.toString(projectScannedData.getMediumVulnerabilities())); //TODO what if null??
        setLowResults(Integer.toString(projectScannedData.getLowVulnerabilities())); //TODO what if null??
        setInfoResults(Integer.toString(projectScannedData.getInfoVulnerabilities())); //TODO what if null??
        setSastScanLink(url, projectScannedData.getLastScanID(), projectScannedData.getProjectID());
        setSastResultsReady(true);
    }

    public long getScanId() {
        return scanId;
    }

    public void setScanId(long scanId) {
        this.scanId = scanId;
    }

    public String getHighResults() {
        return highResults;
    }

    public void setHighResults(String highResults) {
        this.highResults = highResults;
    }

    public String getMediumResults() {
        return mediumResults;
    }

    public void setMediumResults(String mediumResults) {
        this.mediumResults = mediumResults;
    }

    public String getLowResults() {
        return lowResults;
    }

    public void setLowResults(String lowResults) {
        this.lowResults = lowResults;
    }

    public String getInfoResults() {
        return infoResults;
    }

    public void setInfoResults(String infoResults) {
        this.infoResults = infoResults;
    }


    public String getSastScanLink() {
        return sastScanLink;
    }

    public void setSastScanLink(String sastScanLink) {
        this.sastScanLink = sastScanLink;
    }

    public void setSastScanLink(String url, long scanId, long projectId) {
        this.sastScanLink = String.format(url + "/CxWebClient/ViewerMain.aspx?scanId=%s&ProjectID=%s", scanId, projectId);
    }

    public String getSastProjectLink() {
        return sastProjectLink;
    }

    public void setSastProjectLink(String sastProjectLink) {
        this.sastProjectLink = sastProjectLink;
    }

    public void setSastProjectLink(String url, long projectId) {
        this.sastProjectLink = String.format(url + LINK_FORMAT, projectId);
    }

    public String getScanStart() {
        return scanStart;
    }

    public void setScanStart(String scanStart) {
        this.scanStart = scanStart;
    }

    public String getScanTime() {
        return scanTime;
    }

    public void setScanTime(String scanTime) {
        this.scanTime = scanTime;
    }

    public String getFilesScanned() {
        return filesScanned;
    }

    public void setFilesScanned(String filesScanned) {
        this.filesScanned = filesScanned;
    }

    public boolean isSastResultsReady() {
        return sastResultsReady;
    }

    public void setSastResultsReady(boolean sastResultsReady) {
        this.sastResultsReady = sastResultsReady;
    }

    public String getLinesOfCodeScanned() {
        return linesOfCodeScanned;
    }

    public void setLinesOfCodeScanned(String linesOfCodeScanned) {
        this.linesOfCodeScanned = linesOfCodeScanned;
    }

    public void setQueryList(List<CxXMLResults.Query> queryList) {
        this.queryList = queryList;
    }

    public List<CxXMLResults.Query> getQueryList() {
        return queryList;
    }

    public byte[] getPDFReport() {
        return PDFReport;
    }

    public void setPDFReport(byte[] PDFReport) {
        this.PDFReport = PDFReport;
    }
}
