package lngs.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import lngs.lotus.LotusNotesCalendarEntry;
import lngs.util.StatusMessageCallback;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;

public class GoogleManager {

    public GoogleManager() {
    }


    /**
     * Login to Google and connect to the calendar.
     */
    public void connect() throws Exception {
        try {
            statusMessageCallback.statusAppendStart("Logging into Google");

            // Get the absolute path to this app
            String appPath = new java.io.File("").getAbsolutePath() + System.getProperty("file.separator");
            googleInRangeEntriesFullFilename = appPath + googleInRangeEntriesFilename;

            
            
            
statusMessageCallback.statusAppendLineDiag("Logging in loc1");
            // Initialize the transport
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();

statusMessageCallback.statusAppendLineDiag("Logging in loc2");
            // Initialize the data store factory
            dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

statusMessageCallback.statusAppendLineDiag("Logging in loc3");
            // Load client secrets
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(GoogleManager.class.getResourceAsStream("/resources/client_secret.json")));
            
            boolean doRetry = false;
            int retryCount = 0;
            do {
                try {
statusMessageCallback.statusAppendLineDiag("Logging in loc4");
                    // Set up authorization code flow
                    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport, JSON_FACTORY, clientSecrets,
                        Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory)
                        .build();
                    
statusMessageCallback.statusAppendLineDiag("Logging in loc5");
                    // Authorize with OAuth2
                    credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(googleUsername);

statusMessageCallback.statusAppendLineDiag("Logging in loc6");
                    // Set up global Calendar instance
                    client = new com.google.api.services.calendar.Calendar.Builder(
                        httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
statusMessageCallback.statusAppendLineDiag("Logging in loc7");
                } catch (Exception ex) {
                    if (++retryCount > maxRetryCount)
                        throw ex;
                    Thread.sleep(retryDelayMsecs);
                    doRetry = true;

                    statusMessageCallback.statusAppendLineDiag("Logging in Retry #" + retryCount + ". Encountered " + ex.toString());
                    
                    if (retryCount == 1) {
                        // Write out the stack trace
                        StringWriter sw = new StringWriter();
                        ex.printStackTrace(new PrintWriter(sw));
                        statusMessageCallback.statusAppendLineDiag(sw.toString());                        
                    }                    
                } catch (Throwable ex) {
                    throw new Exception("An unknown Throwable error occurred.", ex);
                }
            } while (doRetry);
            
            
            
            
//            String protocol = "http:";
//            if (useSSL) {
//                protocol = "https:";
//            }
//
//            mainCalendarFeedUrl = new URL(protocol + "//www.google.com/calendar/feeds/" + googleUsername + "/owncalendars/full");
//            privateCalendarFeedUrl = new URL(protocol + "//www.google.com/calendar/feeds/" + googleUsername + "/private/full");
//
//            service = new CalendarService("LotusNotes-Calendar-Sync");
//            
//            // I think (but I'm not positive) that the default for these two timeout values is 20000 msecs.
//            // Increase them because some users have encountered timeout problems.
//            service.setConnectTimeout(30000);
//            service.setReadTimeout(30000);
//
//            if (useSSL) {
//                service.useSsl();
//            }
//
//            int retryCount = 0;
//            boolean doRetry = false;
//
//            do {
//                try {
//                    doRetry = false;
//                    service.setUserCredentials(googleUsername, googlePassword);
//                } catch (InvalidCredentialsException ex) {
//                    throw new Exception("The username and/or password are invalid for signing into Google.", ex);
//                } catch (AuthenticationException ex) {
//                    throw new Exception("Unable to login to Google. Perhaps you need to use a proxy server.", ex);
//                } catch (Exception ex) {
//                    if (++retryCount > maxRetryCount)
//                        throw ex;
//                    Thread.sleep(retryDelayMsecs);
//                    doRetry = true;
//
//                    statusMessageCallback.statusAppendLineDiag("User Credentials Retry #" + retryCount + ". Encountered " + ex.toString());
//                    
//                    if (retryCount == 1) {
//                        // Write out the stack trace
//                        StringWriter sw = new StringWriter();
//                        ex.printStackTrace(new PrintWriter(sw));
//                        statusMessageCallback.statusAppendLineDiag(sw.toString());                        
//                    }                    
//                } catch (Throwable ex) {
//                    throw new Exception("An unknown Throwable error occurred.", ex);
//                }
//            } while (doRetry);

            createCalendar();

            if (diagnosticMode) {
                // Get this machine's current time zone
                TimeZone localTimeZone = TimeZone.getDefault();
                String timeZoneName = localTimeZone.getID();
                statusMessageCallback.statusAppendLineDiag("Local Machine Time Zone: " + timeZoneName);

                statusMessageCallback.statusAppendLineDiag("Dest Calendar Time Zone: " + getDestinationTimeZone());
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            statusMessageCallback.statusAppendFinished();
        }
    }

    /**
     * Creates a Google calendar for the desired name (if it doesn't already exist).
     * @throws IOException
     * @throws ServiceException
     */
    public void createCalendar() throws Exception, IOException {
        // If true, we already have a reference to the calendar
        if (destCalendar != null) {
            return;
        }
//destinationCalendarName="DeanTest2";
        // Get a list of all calendars
        CalendarList feed = client.calendarList().list().execute();
        if (feed.getItems() != null) {
            for (CalendarListEntry entry : feed.getItems()) {
String tz = entry.getTimeZone();
                if (entry.getSummary().equals(destinationCalendarName)) {
                    // Get the Calendar object
                    destCalendar = client.calendars().get(entry.getId()).execute();
                    return;
                }
            }
        }

        // Get this machine's current time zone when creating the new Google calendar
        TimeZone localTimeZone = TimeZone.getDefault();
        String timeZoneName = localTimeZone.getID();
        
        // The calendar wasn't found, and will be created
        statusMessageCallback.statusAppendLineDiag("Creating calendar named '" + destinationCalendarName + "'");
        com.google.api.services.calendar.model.Calendar newCal = new com.google.api.services.calendar.model.Calendar();

        newCal.setSummary(destinationCalendarName);
        newCal.setTimeZone(timeZoneName);
        
        destCalendar = client.calendars().insert(newCal).execute();


// Try this update code to set background color        
//com.google.api.services.calendar.model.Calendar entry = new com.google.api.services.calendar.model.Calendar();
//entry.setSummary("Updated Calendar for Testing");
//com.google.api.services.calendar.model.Calendar result = client.calendars().patch(calendar.getId(), entry).execute();
    
        CalendarListEntry newCalEntry = client.calendarList().get(destCalendar.getId()).execute();
String tz = newCalEntry.getTimeZone();
String cid = newCalEntry.getBackgroundColor();
        newCalEntry.setHidden(false);
        newCalEntry.setSelected(true);
        newCalEntry.setBackgroundColor(DEST_CALENDAR_COLOR);
String bg = newCalEntry.getBackgroundColor();
        newCalEntry.setSelected(true);
        newCalEntry.setBackgroundColor(DEST_CALENDAR_COLOR);
//        client.calendarList().update(newCalEntry.getId(), newCalEntry).execute();
        client.calendarList().patch(newCalEntry.getId(), newCalEntry).execute();



//        // If true, the calendar already exists
//        if (getDestinationCalendarUrl() != null) {
//            return;
//        }
//
//        statusMessageCallback.statusAppendLineDiag("Creating calendar named '" + destinationCalendarName + "'");
//
//        CalendarEntry calendar = new CalendarEntry();
//        calendar.setTitle(new PlainTextConstruct(destinationCalendarName));


//        // Set the Google calendar time zone
//        TimeZoneProperty tzp = new TimeZoneProperty(timeZoneName);
//        calendar.setTimeZone(tzp);

//        calendar.setHidden(HiddenProperty.FALSE);
//        calendar.setSelected(SelectedProperty.TRUE);
//        calendar.setColor(new ColorProperty(DEST_CALENDAR_COLOR));
//
//        CalendarEntry returnedCalendar = service.insert(mainCalendarFeedUrl, calendar);
//        returnedCalendar.update();
//
//        // Get the feed url reference so that we can add events to the new calendar.
//        destinationCalendarFeedUrl = new URL(returnedCalendar.getLink("alternate", "application/atom+xml").getHref());
    }


    /**
     * Delete the Google calendar entries in the provided list.
     * @return The number of entries successfully deleted.
     */
    public int deleteCalendarEntries(ArrayList<Event> googleCalEntries) throws Exception {
        try {
            if (googleCalEntries.size() == 0)
                return 0;

            int cntDeleted = googleCalEntries.size();
                    
// TODO: Try to do a batch delete.
            for (int i = 0; i < googleCalEntries.size(); i++) {
                statusMessageCallback.statusAppendLineDiag("Delete #" + (i+1) +
                        ". Subject: " + googleCalEntries.get(i).getSummary() +
                        "  Start Date: " + googleCalEntries.get(i).getStart());
                client.events().delete(destCalendar.getId(), googleCalEntries.get(i).getId()).execute();
            }
            
            return cntDeleted;
            
            
            
            
//            URL feedUrl = getDestinationCalendarUrl();
//
//            int retryCount = 0;
//
//            // Delete all the entries as a batch delete
//            CalendarEventFeed batchRequest = new CalendarEventFeed();
//
//            for (int i = 0; i < googleCalEntries.size(); i++) {
//                CalendarEventEntry entry = googleCalEntries.get(i);
//
//                BatchUtils.setBatchId(entry, Integer.toString(i));
//                BatchUtils.setBatchOperationType(entry, BatchOperationType.DELETE);
//                batchRequest.getEntries().add(entry);
//
//                String startDateStr;
//                startDateStr = "";
//                if (entry.getTimes().size() > 0)
//                    startDateStr = entry.getTimes().get(0).getStartTime().toString();
//
//                statusMessageCallback.statusAppendLineDiag("Delete #" + (i+1) +
//                        ". Subject: " + entry.getTitle().getPlainText() +
//                        "  Start Date: " + startDateStr);
//            }
//
//            CalendarEventFeed feed = null;
//            do {
//                try {
//                    feed = service.getFeed(feedUrl, CalendarEventFeed.class);
//                } catch (com.google.gdata.util.ServiceException ex) {
//                    feed = null;
//                    // If there is a network problem, retry a few times
//                    if (++retryCount > maxRetryCount)
//                        throw ex;
//                    Thread.sleep(retryDelayMsecs);
//                    
//                    statusMessageCallback.statusAppendLineDiag("Get Feed for Delete Retry #" + retryCount + ". Encountered " + ex.toString());
//                    
//                    if (retryCount == 1) {
//                        // Write out the stack trace
//                        StringWriter sw = new StringWriter();
//                        ex.printStackTrace(new PrintWriter(sw));
//                        statusMessageCallback.statusAppendLineDiag(sw.toString());                        
//                    }                    
//                }
//            } while (feed == null);
//
//            // Get the batch link URL
//            Link batchLink = feed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM);
//
//            // Send the batch request with retries
//            CalendarEventFeed batchResponse = null;
//            retryCount = 0;
//            do {
//                try {
//                    batchResponse = service.batch(new URL(batchLink.getHref()), batchRequest);
//                } catch (com.google.gdata.util.ServiceException ex) {
//                    batchResponse = null;
//                    // If there is a network problem, retry a few times
//                    if (++retryCount > maxRetryCount)
//                        throw ex;
//                    Thread.sleep(retryDelayMsecs);
//
//                    statusMessageCallback.statusAppendLineDiag("Get Batch Response for Delete Retry #" + retryCount + ". Encountered " + ex.toString());
//                    
//                    if (retryCount == 1) {
//                        // Write out the stack trace
//                        StringWriter sw = new StringWriter();
//                        ex.printStackTrace(new PrintWriter(sw));
//                        statusMessageCallback.statusAppendLineDiag(sw.toString());                        
//                    }                    
//                }
//            } while (batchResponse == null);
//
//            CheckBatchDeleteResults(batchResponse, googleCalEntries);
//
//            return batchRequest.getEntries().size();
        } catch (Exception ex) {
            throw ex;
        }
    }

    
    /**
     * Throw an exception if there were any errors in the batch delete.
     * @param batchResponse - The batch response.
     * @param googleCalEntries - The original list of items that we tried to delete.
     */
/*    protected void CheckBatchDeleteResults(CalendarEventFeed batchResponse, ArrayList<CalendarEventEntry> googleCalEntries) throws Exception {

        // Ensure that all the operations were successful
        boolean isSuccess = true;

        StringBuffer batchFailureMsg = new StringBuffer("These entries in the batch delete failed:");
        for (CalendarEventEntry entry : batchResponse.getEntries()) {
            String batchId = BatchUtils.getBatchId(entry);
            if (!BatchUtils.isSuccess(entry)) {
                isSuccess = false;
                BatchStatus status = BatchUtils.getBatchStatus(entry);

                CalendarEventEntry entryOrig = googleCalEntries.get(new Integer(batchId));

                batchFailureMsg.append("\nID: " + batchId + "  Reason: " + status.getReason() +
                        "  Subject: " + entryOrig.getTitle().getPlainText() +
                        "  Start Date: " + entryOrig.getTimes().get(0).getStartTime().toString());
            }
        }

        if (!isSuccess) {
            throw new Exception(batchFailureMsg.toString());
        }

    }
*/
    
    /**
     * Get all the Google calendar entries for a specific date range.
     * @return The found entries.
     */
    public ArrayList<Event> getCalendarEntries() throws Exception {
        try {
            statusMessageCallback.statusAppendStart("Getting Google calendar entries");

            // Get all events within our date range
            com.google.api.client.util.DateTime minDate = new com.google.api.client.util.DateTime(minStartDate);
            com.google.api.client.util.DateTime maxDate = new com.google.api.client.util.DateTime(maxEndDate);
            
//            Events feed = client.events().list(destCalendar.getId())
//                    .setTimeZone(destCalendar.getTimeZone())
//                    .setTimeMin(minDate)
//                    .setTimeMax(maxDate)
//                    .execute();
                
            ArrayList<Event> allCalEntries =  new ArrayList<Event>();
            Events events = new Events();


            String pageToken = null;
            int retryCount = 0;
            int queryCount = 0;
            int entriesReturned = 0;
            // Run our query as many times as necessary to get all the
            // Google calendar entries we want
            do {
                try {
                    // Execute the query and get the response
// Set the maximum number of results to return for the query.
// Note: The server may choose to provide fewer results, but will never provide
// more than the requested maximum.
                    events = client.events().list(destCalendar.getId())
                            .setTimeZone(destCalendar.getTimeZone())
                            .setTimeMin(minDate)
                            .setTimeMax(maxDate)
                            .setMaxResults(1000)
                            .setPageToken(pageToken)
                            .execute();
                } catch (Exception ex) {
                    // If there is a network problem while connecting to Google, retry a few times
                    if (++retryCount > maxRetryCount) {
                        throw ex;
                    }
                    Thread.sleep(retryDelayMsecs);

                    statusMessageCallback.statusAppendLineDiag("Query Retry #" + retryCount + ". Encountered " + ex.toString());
                    
                    if (retryCount == 1) {
                        // Write out the stack trace
                        StringWriter sw = new StringWriter();
                        ex.printStackTrace(new PrintWriter(sw));
                        statusMessageCallback.statusAppendLineDiag(sw.toString());                        
                    }
                    
                    continue;
                }

                queryCount++;

                if (events.getItems() != null) {
                    statusMessageCallback.statusAppendLineDiag(events.getItems().size() + " entries returned by query #" + queryCount);

                    // Add the returned entries to our local list
                    allCalEntries.addAll(events.getItems());
                }

                pageToken = events.getNextPageToken();
            } while (pageToken != null);
            
//            ArrayList<Event> allCalEntries =  new ArrayList<Event>(feed.getItems());
            
            
//            statusMessageCallback.statusAppendLineDiag(allCalEntries.size() + " entries returned by query.");

            // Remove all entries marked canceled. Canceled entries aren't visible
            // in Google calendar, and trying to delete them programatically will
            // cause an exception.
            for (int i = 0; i < allCalEntries.size(); i++) {
                Event evt = allCalEntries.get(i);

                if (evt.getStatus().equals("cancelled")) {
                    allCalEntries.remove(evt);
                    i--;
                }
            }
            
            
/*            
            ArrayList<CalendarEventEntry> allCalEntries = new ArrayList<CalendarEventEntry>();

            URL feedUrl = getDestinationCalendarUrl();

            CalendarQuery myQuery = new CalendarQuery(feedUrl);

            myQuery.setMinimumStartTime(new com.google.gdata.data.DateTime(minStartDate.getTime()));
            // Make the end time far into the future so we delete everything we aren't syncing
            myQuery.setMaximumStartTime(com.google.gdata.data.DateTime.parseDateTime("2099-12-31T23:59:59"));

            // Set the maximum number of results to return for the query.
            // Note: A GData server may choose to provide fewer results, but will never provide
            // more than the requested maximum.
            myQuery.setMaxResults(5000);
            int startIndex = 1;
            int entriesReturned = 0;
            int queryCount = 0;
            int retryCount = 0;

            CalendarEventFeed resultFeed;

            // Run our query as many times as necessary to get all the
            // Google calendar entries we want
            while (true) {
                myQuery.setStartIndex(startIndex);

                try {
                    // Execute the query and get the response
                    resultFeed = service.query(myQuery, CalendarEventFeed.class);
                } catch (com.google.gdata.util.ServiceException ex) {
                    // If there is a network problem while connecting to Google, retry a few times
                    if (++retryCount > maxRetryCount) {
                        throw ex;
                    }
                    Thread.sleep(retryDelayMsecs);

                    statusMessageCallback.statusAppendLineDiag("Query Retry #" + retryCount + ". Encountered " + ex.toString());
                    
                    if (retryCount == 1) {
                        // Write out the stack trace
                        StringWriter sw = new StringWriter();
                        ex.printStackTrace(new PrintWriter(sw));
                        statusMessageCallback.statusAppendLineDiag(sw.toString());                        
                    }
                    
                    continue;
                }

                queryCount++;
                entriesReturned = resultFeed.getEntries().size();
                statusMessageCallback.statusAppendLineDiag(entriesReturned + " entries returned by query #" + queryCount + ".");
                if (entriesReturned == 0)
                    // We've hit the end of the list
                    break;

                // Add the returned entries to our local list
                allCalEntries.addAll(resultFeed.getEntries());

                startIndex = startIndex + entriesReturned;
            }

            // Remove all entries marked canceled. Canceled entries aren't visible
            // in Google calendar, and trying to delete them programatically will
            // cause an exception.
            for (int i = 0; i < allCalEntries.size(); i++) {
                CalendarEventEntry entry = allCalEntries.get(i);

                if (entry.getStatus().equals(BaseEventEntry.EventStatus.CANCELED)) {
                    allCalEntries.remove(entry);
                    i--;
                }
            }
*/
            if (diagnosticMode)
                writeInRangeEntriesToFile(allCalEntries);

            return allCalEntries;
        } catch (Exception ex) {
            throw ex;
        } finally {
            statusMessageCallback.statusAppendFinished();
        }
    }


    /**
     * Write key parts of the Google calendar entries to a text file.
     * @param calendarEntries - The calendar entries to process.
     */
    public void writeInRangeEntriesToFile(ArrayList<Event> calendarEntries) throws Exception {
        try {
            // Open the output file if it is not open
            if (googleInRangeEntriesWriter == null) {
                googleInRangeEntriesFile = new File(googleInRangeEntriesFullFilename);
                googleInRangeEntriesWriter = new BufferedWriter(new FileWriter(googleInRangeEntriesFile));
            }

            if (calendarEntries == null)
                googleInRangeEntriesWriter.write("The calendar entries list is empty.\n");
            else
                googleInRangeEntriesWriter.write("Total entries: " + calendarEntries.size() + "\n\n");                
            
                for (Event calEntry : calendarEntries) {
                    googleInRangeEntriesWriter.write("=== Calendar Entry ===\n");

                    googleInRangeEntriesWriter.write("  Title: " + calEntry.getSummary() + "\n");

                    googleInRangeEntriesWriter.write("  Where: " + calEntry.getLocation() + "\n");

                    googleInRangeEntriesWriter.write("  IcalUID: " + calEntry.getICalUID() + "\n");
                    googleInRangeEntriesWriter.write("  Start DateTime:   " + calEntry.getStart().getDateTime() + "\n");
                    googleInRangeEntriesWriter.write("  End DateTime:     " + calEntry.getEnd().getDateTime() + "\n");
                    googleInRangeEntriesWriter.write("  Start Date:       " + calEntry.getStart().getDate() + "\n");
                    googleInRangeEntriesWriter.write("  End Date:         " + calEntry.getEnd().getDate() + "\n");
                    googleInRangeEntriesWriter.write("  Updated Date: " + calEntry.getUpdated() + "\n");
                    googleInRangeEntriesWriter.write("  Alarm: " + (calEntry.getReminders() != null ? calEntry.getReminders().toPrettyString() : "none") + "\n");

                    googleInRangeEntriesWriter.write("\n\n");
                }
        } catch (Exception ex) {
            throw ex;
        }
        finally {
            if (googleInRangeEntriesWriter != null) {
                googleInRangeEntriesWriter.close();
                googleInRangeEntriesWriter = null;
            }
        }
    }


    /**
     * Compare the Lotus and Google entries based on the Lotus modified timestamp
     * and other items.
     * On exit, lotusCalEntries will only contain the entries we want created and
     * googleCalEntries will only contain the entries we want deleted.
     */
    public void compareCalendarEntries(ArrayList<LotusNotesCalendarEntry> lotusCalEntries, ArrayList<Event> googleCalEntries) {
        // Loop through all Lotus entries
        for (int i = 0; i < lotusCalEntries.size(); i++) {
            LotusNotesCalendarEntry lotusEntry = lotusCalEntries.get(i);

            // Loop through all Google entries for each Lotus entry.  This isn't
            // very efficient, but we have small lists (probably less than 300).
            for (int j = 0; j < googleCalEntries.size(); j++) {
                if ( ! LotusNotesCalendarEntry.isLNGSUID(googleCalEntries.get(j).getICalUID())) {
                    // The Google entry was NOT created by LNGS, so we want to remove it from
                    // our processing list (i.e. we will leave it alone).
                    googleCalEntries.remove(j--);
//statusMessageCallback.statusAppendLineDiag("Compare: Google entry NOT created by LNGS: " + googleCalEntries.get(j).getSummary());
                }
                else if ( ! hasEntryChanged(lotusEntry, googleCalEntries.get(j))) {
                    // The Lotus and Google entries are identical, so remove them from out lists.
                    // They don't need created or deleted.
                    lotusCalEntries.remove(i--);
                    googleCalEntries.remove(j--);
                    break;
                }
                else {
//statusMessageCallback.statusAppendLineDiag("Compare: Lotus entry needs created in GCal: " + lotusEntry.getSubject());
                }
            }
        }
    }


    /**
     * Compare a Lotus and Google entry
     * Return true if the Lotus entry has changed since the last sync.
     * Return false if the two entries are equivalent.
     */
    public boolean hasEntryChanged(LotusNotesCalendarEntry lotusEntry, Event googleEntry) {
        final int googleUIDIdx = 33;

        String syncUID = lotusEntry.getSyncUID();

        // The Google IcalUID has the format: GoogleUID:SyncUID. Strip off the 
        // "GoogleUID:" part and do a compare of the SyncUID.
        // The SyncUID contains several pieces of info, including the Lotus modified
        // timestamp. Most changes to a Lotus entry will update this timestamp. Therefore,
        // this compare will catch the vast majority of the changes between Lotus/Google.
        if (googleEntry.getICalUID().substring(googleUIDIdx).equals(syncUID)) {
            // The Google and Lotus entries match on our first test, but we have to compare
            // other values. Why? Say a sync is performed with the "sync alarms"
            // option enabled, but then "sync alarms" is turned off. When the
            // second sync happens, we want to delete all the Google entries created
            // the first time (with alarms) and re-create them without alarms.
statusMessageCallback.statusAppendLineDiag("Compare: UIDs match. Subj: " + googleEntry.getSummary() + "  Start: " + googleEntry.getStart().getDateTime());

            // Compare the title/subject
            String lotusSubject = createSubjectText(lotusEntry);
            if (googleEntry.getSummary() == null || !googleEntry.getSummary().equals(lotusSubject)) {
statusMessageCallback.statusAppendLineDiag("Compare: Subjects differ");
                return true;
            }

            // If true, we want location/where info in our Google entries and the Lotus
            // entry has location info to add.
            if (syncWhere && lotusEntry.getGoogleWhereString() != null) {
                // If true, the Google entry doesn't contain location info, so the entries don't match.
                if (googleEntry.getLocation() == null || googleEntry.getLocation().isEmpty()) {
statusMessageCallback.statusAppendLineDiag("Compare: No Google location info");;
                    return true;
                }
            }
            else {
                // If true, the Google entry has location info (which we don't want), so the entries don't match.
                if (googleEntry.getLocation() != null && ! googleEntry.getLocation().isEmpty()) {
statusMessageCallback.statusAppendLineDiag("Compare: No Lotus location info. GCal Location: " + googleEntry.getLocation());
                    return true;
                }
            }

            boolean useDefaultReminder = true;
            if (googleEntry.getReminders() != null) {
                useDefaultReminder = false;
            }
            if (syncAlarms && lotusEntry.getAlarm()) {   
                // We are syncing alarms, so make sure the Google entry has an alarm.
                // LNGS always sets alarms as UseDefault=false, so UseDefault=true means
                // we want to update the entry.
                // Note: If there is an alarm set, we'll assume the alarm offset is correct.
                if (useDefaultReminder) {
statusMessageCallback.statusAppendLineDiag("Compare: No GCal reminder, but has Lotus reminder");
                    return true;
                }
            }
            else {
                // We aren't syncing alarms, so make sure the Google entry doesn't
                // have an alarm specified
                if (! useDefaultReminder) {
statusMessageCallback.statusAppendLineDiag("Compare: No Lotus reminder, but has GCal reminder");
                    return true;
                }
            }

            // Compare the Description field of Google entry to what we would build it as
            if (googleEntry.getDescription() == null || ! googleEntry.getDescription().equals(createDescriptionText(lotusEntry))) {
statusMessageCallback.statusAppendLineDiag("Compare: Descriptions differ");
                return true;
            }

            // The Lotus and Google entries are identical
            return false;
        }

        return true;
    }


    // This method is for testing purposes.
    public void createSampleCalEntry() {
        LotusNotesCalendarEntry cal = new LotusNotesCalendarEntry();
        cal.setSubject("DeanRepeatTest");
        cal.setEntryType(LotusNotesCalendarEntry.EntryType.APPOINTMENT);
        cal.setAppointmentType("3");
        cal.setLocation("nolocation");
        cal.setRoom("noroom");

        Date dstartDate, dendDate;
        Calendar now = Calendar.getInstance();
        now.set(Calendar.YEAR, 2010);
        now.set(Calendar.MONTH, 7);  // Month is relative zero
        now.set(Calendar.DAY_OF_MONTH, 2);
        now.set(Calendar.HOUR_OF_DAY, 10);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        dstartDate = now.getTime();
        cal.setStartDateTime(dstartDate);

        now.set(Calendar.HOUR_OF_DAY, 11);
        dendDate = now.getTime();
        cal.setEndDateTime(dendDate);

//        DateTime startTime, endTime;
//
//        CalendarEventEntry event = new CalendarEventEntry();
//        event.setTitle(new PlainTextConstruct(cal.getSubject()));
//
//        String whereStr = cal.getGoogleWhereString();
//        if (whereStr != null) {
//            Where location = new Where();
//            location.setValueString(whereStr);
//            event.addLocation(location);
//        }
//
//        try {
//            When eventTime = new When();
//            eventTime.setStartTime(DateTime.parseDateTime(cal.getStartDateTimeGoogle()));
//            eventTime.setEndTime(DateTime.parseDateTime(cal.getEndDateTimeGoogle()));
//            event.addTime(eventTime);
//
//            Date dstartDate2, dendDate2;
//            now.set(Calendar.DAY_OF_MONTH, 3);
//            now.set(Calendar.HOUR_OF_DAY, 10);
//            dstartDate2 = now.getTime();
//            cal.setStartDateTime(dstartDate2);
//            now.set(Calendar.HOUR_OF_DAY, 11);
//            dendDate2 = now.getTime();
//            cal.setEndDateTime(dendDate2);
//
//            eventTime = new When();
//            eventTime.setStartTime(DateTime.parseDateTime(cal.getStartDateTimeGoogle()));
//            eventTime.setEndTime(DateTime.parseDateTime(cal.getEndDateTimeGoogle()));
//            event.addTime(eventTime);
//            int j = event.getTimes().size();
//            j++;
//
//            service.insert(getDestinationCalendarUrl(), event);
//        } catch (Exception e) {
//        }
    }


    /**
     * Create Lotus Notes calendar entries in the Google calendar.
     * @param lotusCalEntries - The list of Lotus Notes calendar entries.
     * @return The number of Google calendar entries successfully created.
     * @throws ServiceException
     * @throws IOException
     */
    public int createCalendarEntries(ArrayList<LotusNotesCalendarEntry> lotusCalEntries) throws Exception, IOException {
        int retryCount = 0;
        int createdCount = 0;

        for (int i = 0; i < lotusCalEntries.size(); i++) {
            LotusNotesCalendarEntry lotusEntry = lotusCalEntries.get(i);
            Event event = new Event();
            // Set the subject/title
            event.setSummary(createSubjectText(lotusEntry));

            // The Google IcalUID must be unique or we'll get a
            // VersionConflictException during the insert. So start the IcalUID string
            // with a newly generate UUID (with the '-' chars removed).  Then add the values
            // we really want to remember (referred to as the SyncUID).
            event.setICalUID(UUID.randomUUID().toString().replaceAll("-", "") + ":" + lotusEntry.getSyncUID());

            StringBuffer sb = new StringBuffer();

            // Set the body/description
            event.setDescription(createDescriptionText(lotusEntry));

            if (syncWhere) {
                String whereStr = lotusEntry.getGoogleWhereString();
                if (whereStr != null) {
                    // Remove all control/non-printing characters from the Where string. If present, such
                    // characters will cause the GCal create to fail.
                    event.setLocation(whereStr.replaceAll("\\p{Cntrl}", ""));
                }
            }

            Date startTime, endTime;
            if (lotusEntry.getEntryType() == LotusNotesCalendarEntry.EntryType.TASK ||
                    lotusEntry.getAppointmentType() == LotusNotesCalendarEntry.AppointmentType.ALL_DAY_EVENT ||
                    lotusEntry.getAppointmentType() == LotusNotesCalendarEntry.AppointmentType.ANNIVERSARY)
            {
                // Create an all-day event by setting start/end dates with no time portion
                startTime = lotusEntry.getStartDate(0);
                // IMPORTANT: For Google to properly create an all-day event, we must add
                // one day to the end date
                if (lotusEntry.getEndDateTime() == null)
                    // Use start date since the end date is null
                    endTime = lotusEntry.getStartDate(1);
                else
                    endTime = lotusEntry.getEndDate(1);
            }
            else if (lotusEntry.getAppointmentType() == LotusNotesCalendarEntry.AppointmentType.APPOINTMENT ||
                    lotusEntry.getAppointmentType() == LotusNotesCalendarEntry.AppointmentType.MEETING)
            {
                // Create a standard event
                startTime = lotusEntry.getStartDateTime();
                if (lotusEntry.getEndDateTime() == null) {
                    // Use start date since the end date is null
                    endTime = lotusEntry.getStartDateTime();
                }
                else {
                    endTime = lotusEntry.getEndDateTime();
                }
            }
            else if (lotusEntry.getAppointmentType() == LotusNotesCalendarEntry.AppointmentType.REMINDER)
            {
                // Create a standard event with the start and end times the same
                startTime = lotusEntry.getStartDateTime();
                endTime = lotusEntry.getStartDateTime();
            }
            else
            {
                throw new Exception("Couldn't determine Lotus Notes event type.\nEvent subject: " + lotusEntry.getSubject() +
                        "\nEntry Type: " + lotusEntry.getEntryType() +
                        "\nAppointment Type: " + lotusEntry.getAppointmentType());
            }

            com.google.api.client.util.DateTime googleStartTime = new com.google.api.client.util.DateTime(startTime);
            EventDateTime startEdt = new EventDateTime();
            startEdt.setDateTime(googleStartTime);
            event.setStart(startEdt);

            com.google.api.client.util.DateTime googleEndTime = new com.google.api.client.util.DateTime(endTime);
            EventDateTime endEdt = new EventDateTime();
            endEdt.setDateTime(googleEndTime);
            event.setEnd(endEdt);
            
            
//statusMessageCallback.statusAppendLineDiag("Subject: " + event.getSummary() +
//        "  Start Date: " + event.getStart() +
//        "  start1: " + event.getStart().getDateTime() +
//        "  end: " + event.getEnd().getDateTime());
            

            if (syncAlarms && lotusEntry.getAlarm()) {
                Event.Reminders reminders = new Event.Reminders();
                
                com.google.api.services.calendar.model.EventReminder reminder = new com.google.api.services.calendar.model.EventReminder();

                reminder.setMinutes(lotusEntry.getAlarmOffsetMinsGoogle());
                reminder.setMethod("popup");
                ArrayList<com.google.api.services.calendar.model.EventReminder> over = new ArrayList<com.google.api.services.calendar.model.EventReminder>();
                over.add(reminder);
                reminders.setOverrides(over);
                reminders.setUseDefault(false);
                event.setReminders(reminders);
            }

            // If the Lotus Notes entry has the Mark Private checkbox checked, then
            // mark the entry private in Google
            if (lotusEntry.getPrivate())
                event.setVisibility("private");

            retryCount = 0;
            do {
                try {
                    createdCount++;
                    statusMessageCallback.statusAppendLineDiag("Create #" + createdCount +
                            ". Subject: " + event.getSummary() +
                            "  Start Date: " + event.getStart().getDateTime() +
                            "  Type: " + lotusEntry.getAppointmentType());
                    client.events().insert(destCalendar.getId(), event).execute();

                    break;
                } catch (Exception ex) {
                    // If there is a network problem (a ServiceException) while connecting to Google, retry a few times
                    // before throwing an exception.
//                    if (ex instanceof com.google.gdata.util.ServiceException && ++retryCount <= maxRetryCount)
//                        Thread.sleep(retryDelayMsecs);
//                    else
                        throw new Exception("Couldn't create Google entry.\nSubject: " + event.getSummary() +
                            "\nStart Date: " + event.getStart().getDateTime() +
                            "\nType: " + lotusEntry.getAppointmentType(), ex);
                }
            } while (true);
        }

        return createdCount;
    }

    /**
     * Build the GCal subject text from the Lotus Notes calendar entry.
     * @param lotusEntry - The source Lotus Notes calendar entry.
     * @return The GCal subject text.
     */
    protected String createSubjectText(LotusNotesCalendarEntry lotusEntry) {
        String subjectText = "";
        
        if (syncAllSubjectsToValue) {
            subjectText = syncAllSubjectsToThisValue;
        } else {
            // Remove carriage returns
            subjectText = lotusEntry.getSubject().trim().replace("\r", "");            
        }
        
        if (subjectText.length() > maxSubjectChars) {
            // Truncate to a max size
            subjectText = subjectText.substring(0, maxSubjectChars);
        }
        
        return subjectText;
    }
    
    /**
     * Build the GCal description text from the Lotus Notes calendar entry. The
     * output includes the LN description and optional info like the invitees.
     * @param lotusEntry - The source Lotus Notes calendar entry.
     * @return The GCal description text.
     */
    protected String createDescriptionText(LotusNotesCalendarEntry lotusEntry) {
        StringBuffer sb = new StringBuffer();

        if (syncMeetingAttendees) {
            if (lotusEntry.getChairpersonPlain() != null) {
                //chair comes out in format: CN=Jonathan Marshall/OU=UK/O=IBM, leaving like that at the moment
                sb.append("Chairperson: "); sb.append(lotusEntry.getChairpersonPlain());
            }

            if (lotusEntry.getRequiredAttendeesPlain() != null) {
                if (sb.length() > 0)
                    sb.append("\n");
                sb.append("Required: "); sb.append(lotusEntry.getRequiredAttendeesPlain());
            }

            if (lotusEntry.getOptionalAttendees() != null){
                if (sb.length() > 0)
                    sb.append("\n");
                sb.append("Optional: "); sb.append(lotusEntry.getOptionalAttendees());
            }
        }

        if (syncDescription && lotusEntry.getBody() != null) {
            if (sb.length() > 0)
                // Put blank lines between attendees and the description
                sb.append("\n\n\n");

            // Lotus ends each description line with \r\n.  Remove all
            // carriage returns (\r) because they aren't needed and they prevent the
            // Lotus description from matching the description in Google.
            String s = lotusEntry.getBody().replace("\r", "");
            sb.append(s.trim());
        }

        // Return a string truncated to a max size
        return sb.toString().substring(0, sb.length() < maxDescriptionChars ? sb.length() : maxDescriptionChars);
    }

    public String getDestinationTimeZone() throws Exception {
        if (destCalendar != null) {
            return destCalendar.getTimeZone();
        }
        
        return "unknown";
    }

    public void setUsername(String value) {
        googleUsername = value;
    }

    public void setPassword(String value) {
        googlePassword = value;
    }

    public void setCalendarName(String value) {
        destinationCalendarName = value;
        
        // Set our object to null to force a reconnect to the new calendar name
        destCalendar = null;
    }

    public void setUseSSL(boolean value) {
        useSSL = value;
    }

    public void setSyncDescription(boolean value) {
        syncDescription = value;
    }

    public void setSyncAlarms(boolean value) {
        syncAlarms = value;
    }

    public void setSyncWhere(boolean value) {
        syncWhere = value;
    }

    public void setSyncAllSubjectsToValue(boolean value) {
        syncAllSubjectsToValue = value;
    }

    public void setSyncAllSubjectsToThisValue(String value) {
        syncAllSubjectsToThisValue = value;
    }

    public void setSyncMeetingAttendees(boolean value){
    	syncMeetingAttendees = value;
    }

    public void setMinStartDate(Date minStartDate) {
        this.minStartDate = minStartDate;
    }

    public void setMaxEndDate(Date maxEndDate) {
        this.maxEndDate = maxEndDate;
    }

    public void setDiagnosticMode(boolean value) {
        diagnosticMode = value;
    }

    public void setStatusMessageCallback(StatusMessageCallback value) {
        statusMessageCallback = value;
    }

    private static final String APPLICATION_NAME = "LNGS";

    // Directory to store user credentials.
    private static final java.io.File DATA_STORE_DIR =
        new java.io.File(System.getProperty("user.home"), ".store/LNGS");

    // Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
    // globally shared instance across your application.
    private static FileDataStoreFactory dataStoreFactory;

    // Global instance of the HTTP transport.
    private static HttpTransport httpTransport;

    // Global instance of the JSON factory.
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    
    // OAuth2 credential
    Credential credential;

    private static com.google.api.services.calendar.Calendar client;
    
    com.google.api.services.calendar.model.Calendar destCalendar;    
    
    
    
    
    
    protected StatusMessageCallback statusMessageCallback = null;

    protected URL mainCalendarFeedUrl = null;
    protected URL privateCalendarFeedUrl = null;
    protected URL destinationCalendarFeedUrl = null;
//    protected CalendarService service;

    protected String googleUsername;
    protected String googlePassword;

    protected String destinationCalendarName;
    protected String DEST_CALENDAR_COLOR = "#FFAD40";

    // Debug file info
    protected BufferedWriter googleInRangeEntriesWriter = null;
    protected File googleInRangeEntriesFile = null;
    protected final String googleInRangeEntriesFilename = "GoogleInRangeEntries.txt";
    // Filename with full path
    protected String googleInRangeEntriesFullFilename;

    protected boolean useSSL = true;
    protected boolean diagnosticMode = false;

    protected boolean syncDescription = false;
    protected boolean syncWhere = false;
    protected boolean syncAllSubjectsToValue = false;
    protected String syncAllSubjectsToThisValue = "";
    protected boolean syncAlarms = false;
    protected boolean syncMeetingAttendees = false;
    // Our min and max dates for entries we will process.
    // If the calendar entry is outside this range, it is ignored.
    protected Date minStartDate = null;
    protected Date maxEndDate = null;

    protected final int maxRetryCount = 10;
    protected final int retryDelayMsecs = 600;

    // Google has a maximum limit of around 1600 chars for subject/title lines.
    // I don't know the Lotus limit, but 1000 should be plenty.
    protected final int maxSubjectChars = 1000;
    // The maximum number of chars allowed in a calendar description. Google has some
    // limit around 8100 chars. Lotus has a limit greater than that, so choose 8000.
    protected final int maxDescriptionChars = 8000;
}
