package com.cx.restclient.sast.dto;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.cx.restclient.sast.utils.SASTParam.PROJECT_LINK_FORMAT;
import static com.cx.restclient.sast.utils.SASTParam.SCAN_LINK_FORMAT;


/**
 * Created by Galn on 05/02/2018.
 */
public class SASTResults {

    private long scanId;

    private boolean sastResultsReady = false;
    private int high = 0;
    private int medium = 0;
    private int low = 0;
    private int info = 0;

    private int newHigh = 0;
    private int newMedium = 0;
    private int newLow = 0;
    private int newInfo = 0;

    private String sastScanLink;
    private String sastProjectLink;
    private String sastPDFLink;

    private String scanStart;
    private String scanTime;
    private String scanStartTime;
    private String scanEndTime;

    private String filesScanned;
    private String LOC;
    private List<CxXMLResults.Query> queryList;

    private byte[] PDFReport;

    private boolean isSynchronous;

    private enum Severity {
        High, Medium, Low, Info;
    }

    public void setScanDetailedReport(CxXMLResults reportObj) {
        this.scanStart = reportObj.getScanStart();
        this.scanTime = reportObj.getScanTime();
        setScanStartEndDates(this.scanStart, this.scanTime);
        this.LOC = reportObj.getLinesOfCodeScanned();
        this.filesScanned = reportObj.getFilesScanned();

        for (CxXMLResults.Query q : reportObj.getQuery()) {
            List<CxXMLResults.Query.Result> qResult = q.getResult();
            for (int i = 0; i < qResult.size(); i++) {
                CxXMLResults.Query.Result result = qResult.get(i);
                if ("True".equals(result.getFalsePositive())) {
                    qResult.remove(i);
                } else if ("New".equals(result.getStatus())){
                    Severity sev = Severity.valueOf(result.getSeverity());
                    switch (sev) {
                        case High:
                            newHigh++;
                            break;
                        case Medium:
                            newMedium++;
                            break;
                        case Low:
                            newLow++;
                            break;
                        case Info:
                            newInfo++;
                            break;
                    }
                }
            }
        }
        this.queryList = reportObj.getQuery();//todo qa
    }

    public void setResults(long scanId, SASTStatisticsResponse statisticsResults, String url, long projectId) {
        setScanId(scanId);
        setHigh(statisticsResults.getHighSeverity());
        setMedium(statisticsResults.getMediumSeverity());
        setLow(statisticsResults.getLowSeverity());
        setInfo(statisticsResults.getInfoSeverity());
        setSastScanLink(url, scanId, projectId);
        setSastProjectLink(url, projectId);
        setSastResultsReady(true);
    }

    public long getScanId() {
        return scanId;
    }

    public void setScanId(long scanId) {
        this.scanId = scanId;
    }

    public int getHigh() {
        return high;
    }

    public void setHigh(int high) {
        this.high = high;
    }

    public int getMedium() {
        return medium;
    }

    public void setMedium(int medium) {
        this.medium = medium;
    }

    public int getLow() {
        return low;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public int getInfo() {
        return info;
    }

    public void setInfo(int info) {
        this.info = info;
    }

    public int getNewHigh() {
        return newHigh;
    }

    public void setNewHigh(int newHigh) {
        this.newHigh = newHigh;
    }

    public int getNewMedium() {
        return newMedium;
    }

    public void setNewMedium(int newMedium) {
        this.newMedium = newMedium;
    }

    public int getNewLow() {
        return newLow;
    }

    public void setNewLow(int newLow) {
        this.newLow = newLow;
    }

    public int getNewInfo() {
        return newInfo;
    }

    public void setNewInfo(int newInfo) {
        this.newInfo = newInfo;
    }

    public String getSastScanLink() {
        return sastScanLink;
    }

    public void setSastScanLink(String sastScanLink) {
        this.sastScanLink = sastScanLink;
    }

    public void setSastScanLink(String url, long scanId, long projectId) {
        this.sastScanLink = String.format(url + SCAN_LINK_FORMAT, scanId, projectId);
    }

    public String getSastProjectLink() {
        return sastProjectLink;
    }

    public void setSastProjectLink(String sastProjectLink) {
        this.sastProjectLink = sastProjectLink;
    }

    public void setSastProjectLink(String url, long projectId) {
        this.sastProjectLink = String.format(url + PROJECT_LINK_FORMAT, projectId);
    }

    public String getSastPDFLink() {
        return sastPDFLink;
    }

    public void setSastPDFLink(String sastPDFLink) {
        this.sastPDFLink = sastPDFLink;
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

    public String getScanStartTime() {
        return scanStartTime;
    }

    public void setScanStartTime(String scanStartTime) {
        this.scanStartTime = scanStartTime;
    }

    public String getScanEndTime() {
        return scanEndTime;
    }

    public void setScanEndTime(String scanEndTime) {
        this.scanEndTime = scanEndTime;
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

    public String getLOC() {
        return LOC;
    }

    public void setLOC(String LOC) {
        this.LOC = LOC;
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

    public boolean getSynchronous() {
        return isSynchronous;
    }

    public void setSynchronous(boolean synchronous) {
        isSynchronous = synchronous;
    }

    public boolean hasNewResults() {
        return newHigh + newMedium + newLow > 0;
    }

    private void setScanStartEndDates(String scanStart, String scanTime) {

        try {
            //turn strings to date objects
            Date scanStartDate = createStartDate(scanStart);
            Date scanTimeDate = createTimeDate(scanTime);
            Date scanEndDate = createEndDate(scanStartDate, scanTimeDate);

            //turn dates back to strings
            String scanStartDateFormatted = formatToDisplayDate(scanStartDate);
            String scanEndDateFormatted = formatToDisplayDate(scanEndDate);

            //set sast scan result object with formatted strings
            this.scanStartTime = scanStartDateFormatted;
            this.scanEndTime = scanEndDateFormatted;

        } catch (Exception ignored) {
            //ignored
        }

    }

    private String formatToDisplayDate(Date date) {
        //"26/2/17 12:17"
        String displayDatePattern = "dd/MM/yy HH:mm";
        Locale locale = Locale.ENGLISH;

        return new SimpleDateFormat(displayDatePattern, locale).format(date);
    }

    private Date createStartDate(String scanStart) throws ParseException {
        //"Sunday, February 26, 2017 12:17:09 PM"
        String oldPattern = "EEEE, MMMM dd, yyyy hh:mm:ss a";
        Locale locale = Locale.ENGLISH;

        DateFormat oldDateFormat = new SimpleDateFormat(oldPattern, locale);

        return oldDateFormat.parse(scanStart);
    }

    private Date createTimeDate(String scanTime) throws ParseException {
        //"00h:00m:30s"
        String oldPattern = "HH'h':mm'm':ss's'";

        DateFormat oldTimeFormat = new SimpleDateFormat(oldPattern);
        oldTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return oldTimeFormat.parse(scanTime);
    }

    private Date createEndDate(Date scanStartDate, Date scanTimeDate) {
        long time /*no c*/ = scanStartDate.getTime() + scanTimeDate.getTime();
        return new Date(time);
    }


}