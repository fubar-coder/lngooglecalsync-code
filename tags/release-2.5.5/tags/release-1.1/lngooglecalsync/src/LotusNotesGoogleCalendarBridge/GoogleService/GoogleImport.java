package LotusNotesGoogleCalendarBridge.GoogleService;

import LotusNotesGoogleCalendarBridge.LotusNotesService.NotesCalendarEntry;
import com.google.gdata.client.GoogleService.*;
import com.google.gdata.client.calendar.*;
import com.google.gdata.data.*;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.batch.BatchStatus;
import com.google.gdata.data.calendar.*;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.extensions.*;
import com.google.gdata.util.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Calendar;

public class GoogleImport {

    public GoogleImport(String accountname, String password, boolean useSSL) throws Exception {
        try {
            String protocol = "https:";
            if (!useSSL) {
                protocol = "http:";
            }

            mainCalendarFeedUrl = new URL(protocol + "//www.google.com/calendar/feeds/" + accountname + "/owncalendars/full");
            privateCalendarFeedUrl = new URL(protocol + "//www.google.com/calendar/feeds/" + accountname + "/private/full");
            service = new CalendarService("Corporate-LotusNotes-Calendar");
            if (useSSL) {
                service.useSsl();
            }
            service.setUserCredentials(accountname, password);

            createCalendar();
        } catch (InvalidCredentialsException ex) {
            throw new Exception("The username and/or password are invalid for signing into the Google account.", ex);
        } catch (AuthenticationException ex) {
            throw new Exception("Unable to login to Google. Perhaps you need to use a proxy server.", ex);
        } catch (Exception ex) {
            throw ex;
        }
    }

    public GoogleImport() {
    }


    /**
     * Get the calendar feed URL for the calendar we want to update.
     * @return The calendar feed URL for the calendar we want to update.
     */
    protected URL getDestinationCalendarUrl() throws Exception {
        try {
            // If true, we already know our calendar URL
            if (destinationCalendarFeedUrl != null)
                return destinationCalendarFeedUrl;

            CalendarFeed calendars = service.getFeed(mainCalendarFeedUrl, CalendarFeed.class);

            for (int i = 0; i < calendars.getEntries().size(); i++) {
                CalendarEntry calendar = calendars.getEntries().get(i);

                // If true, we've found the name of the destination calendar
                if (calendar.getTitle().getPlainText().equals(destinationCalendarName)) {
                    destinationCalendarFeedUrl = new URL(calendar.getLink("alternate", "application/atom+xml").getHref());
                }
            }
        } catch (Exception ex) {
            throw ex;
        }

        return destinationCalendarFeedUrl;
    }




    /**
     * Creates a Google calendar for the desired name (if it doesn't already exist).
     * @throws IOException
     * @throws ServiceException
     */
    public void createCalendar() throws Exception, IOException, ServiceException {
        // If true, the calendar already exists
        if (getDestinationCalendarUrl() != null) {
            return;
        }

        CalendarEntry calendar = new CalendarEntry();
        calendar.setTitle(new PlainTextConstruct(destinationCalendarName));
//        calendar.setSummary(new PlainTextConstruct("Lotus Notes Calendar"));
        // Get this machine's current time zone when creating the new Google calendar
        TimeZone localTimeZone = TimeZone.getDefault();
        calendar.setTimeZone(new TimeZoneProperty(localTimeZone.getID()));
        
        calendar.setHidden(HiddenProperty.FALSE);
        calendar.setSelected(SelectedProperty.TRUE);
        calendar.setColor(new ColorProperty(COLOR));

        CalendarEntry returnedCalendar = service.insert(mainCalendarFeedUrl, calendar);
        returnedCalendar.update();

        // Get the feed url reference so that we can add events to the new calendar.
        destinationCalendarFeedUrl = new URL(returnedCalendar.getLink("alternate", "application/atom+xml").getHref());

        return;
    }


    /**
     * Delete all Google calendar entries for a specific date range.
     * @return The number of entries successfully deleted.
     */
    public int deleteCalendarEntries() throws Exception {
        try {
            URL feedUrl = getDestinationCalendarUrl();
            CalendarQuery myQuery = new CalendarQuery(feedUrl);

            // Get today - 7 days
            Calendar now = Calendar.getInstance();
            now.add(Calendar.DATE, -7);
            // Clear out the time portion
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);

            myQuery.setMinimumStartTime(new com.google.gdata.data.DateTime(now.getTime()));
            // Make the end time far into the future so we delete everything
            myQuery.setMaximumStartTime(com.google.gdata.data.DateTime.parseDateTime("2099-12-31T23:59:59"));

            // Set the maximum number of results to return for the query.
            // Note: A GData server may choose to provide fewer results, but will never provide
            // more than the requested maximum.
            myQuery.setMaxResults(5000);
            int startIndex = 1;
            int entriesReturned;

            List<CalendarEventEntry> allCalEntries = new ArrayList<CalendarEventEntry>();
            CalendarEventFeed resultFeed;

            // Run our query as many times as necessary to get all the
            // Google calendar entries we want
            while (true) {
                myQuery.setStartIndex(startIndex);

                // Execute the query and get the response
                resultFeed = service.query(myQuery, CalendarEventFeed.class);

                entriesReturned = resultFeed.getEntries().size();
                if (entriesReturned == 0)
                    // We've hit the end of the list
                    break;

                // Add the returned entries to our local list
                allCalEntries.addAll(resultFeed.getEntries());

                startIndex = startIndex + entriesReturned;
            }

            // Delete all the entries as a batch delete
            CalendarEventFeed batchRequest = new CalendarEventFeed();

            for (int i = 0; i < allCalEntries.size(); i++) {
                CalendarEventEntry entry = allCalEntries.get(i);

                BatchUtils.setBatchId(entry, Integer.toString(i));
                BatchUtils.setBatchOperationType(entry, BatchOperationType.DELETE);
                batchRequest.getEntries().add(entry);
            }

            // Get the batch link URL and send the batch request
            Link batchLink = resultFeed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM);
            CalendarEventFeed batchResponse = service.batch(new URL(batchLink.getHref()), batchRequest);

            // Ensure that all the operations were successful
            boolean isSuccess = true;
            StringBuffer batchFailureMsg = new StringBuffer("These entries in the batch delete failed:");
            for (CalendarEventEntry entry : batchResponse.getEntries()) {
                String batchId = BatchUtils.getBatchId(entry);
                if (!BatchUtils.isSuccess(entry)) {
                    isSuccess = false;
                    BatchStatus status = BatchUtils.getBatchStatus(entry);
                    batchFailureMsg.append("\nID: " + batchId + "  Reason: " + status.getReason());
                }
            }

            if (!isSuccess) {
                throw new Exception(batchFailureMsg.toString());
            }

            return batchRequest.getEntries().size();
        } catch (Exception ex) {
            throw ex;
        }
    }


    // Delete the destination calendar.
    //
    // TODO What if entries were created in the main calendar (instead of the Lotus Notes
    // calendar)? This code only deletes from the Lotus Notes calendar.
    // Because this isn't working quite yet, I've disabled the "sync to main calendar"
    // checkbox on the Advanced tab.
    public void deleteCalendar() throws Exception {

        try {
            CalendarFeed calendars = service.getFeed(mainCalendarFeedUrl, CalendarFeed.class);

            // Loop through each calendar
            for (int i = 0; i < calendars.getEntries().size(); i++) {
                CalendarEntry calendar = calendars.getEntries().get(i);

                // If this is the Lotus Notes calendar, delete it
                if (calendar.getTitle().getPlainText().equals(destinationCalendarName)) {
                    calendar.delete();
                }
            }
        } catch (Exception ex) {
            throw ex;
        }
    }


    /**
     * Create Lotus Notes calendar entries in the Google calendar.
     * @param cals - The list of Lotus Notes calendar entries.
     * @param inMainCalendar - If true, the entries are created in the main Google calendar.
     * @return The number of Google calendar entries successfully created.
     * @throws ServiceException
     * @throws IOException
     */
    public int createCalendarEntries(List cals, boolean inMainCalendar) throws Exception, ServiceException, IOException {
        int createdCount = 0;

        for (int i = 0; i < cals.size(); i++) {
            NotesCalendarEntry cal = (NotesCalendarEntry) cals.get(i);
            CalendarEventEntry event = new CalendarEventEntry();
            event.setTitle(new PlainTextConstruct(cal.getSubject()));

            Where location = new Where();
            location.setValueString(cal.getLocation());
            event.addLocation(location);

            DateTime startTime, endTime;
            if (cal.getEntryType() == NotesCalendarEntry.EntryType.TASK ||
                    cal.getAppointmentType() == NotesCalendarEntry.AppointmentType.ALL_DAY_EVENT ||
                    cal.getAppointmentType() == NotesCalendarEntry.AppointmentType.ANNIVERSARY )
            {
                // Create a Google all-day event. All-day events are created by
                // setting start/end dates the same and having no time portion.
                startTime = DateTime.parseDate(cal.getStartDateGoogle());
                endTime = DateTime.parseDate(cal.getStartDateGoogle());
            }
            else if (cal.getAppointmentType() == NotesCalendarEntry.AppointmentType.APPOINTMENT ||
                    cal.getAppointmentType() == NotesCalendarEntry.AppointmentType.MEETING)
            {
                // Create a standard event
                startTime = DateTime.parseDateTime(cal.getStartDateTimeGoogle());
                endTime = DateTime.parseDateTime(cal.getEndDateTimeGoogle());
            }
            else if (cal.getAppointmentType() == NotesCalendarEntry.AppointmentType.REMINDER)
            {
                // Create a standard event with the start and end times the same
                startTime = DateTime.parseDateTime(cal.getStartDateTimeGoogle());
                endTime = DateTime.parseDateTime(cal.getStartDateTimeGoogle());
            }
            else
            {
                throw new Exception("Couldn't determine Lotus Notes event type.\nEvent subject: " + cal.getSubject() +
                        "\nEntry Type: " + cal.getEntryType() +
                        "\nAppointment Type: " + cal.getAppointmentType());
            }

            When eventTimes = new When();
            eventTimes.setStartTime(startTime);
            eventTimes.setEndTime(endTime);
            event.addTime(eventTimes);

            try {
                if (inMainCalendar) {
                    service.insert(privateCalendarFeedUrl, event);
                } else {
                    service.insert(getDestinationCalendarUrl(), event);
                }

                createdCount++;
            } catch (Exception ex) {
                throw ex;
            }
        }

        return createdCount;
    }

    public void setCalendarColor(String color) {
        if (color.startsWith("#") && color.length() == 7) {
            COLOR = color;
        }
    }

    URL mainCalendarFeedUrl = null;
    URL privateCalendarFeedUrl = null;
    URL destinationCalendarFeedUrl = null;
    String destinationCalendarName = "Lotus Notes";
    CalendarService service;
//    String COLOR = "#2952A3";  //default cal color
    String COLOR = "#F2A640";  //default cal color
}
